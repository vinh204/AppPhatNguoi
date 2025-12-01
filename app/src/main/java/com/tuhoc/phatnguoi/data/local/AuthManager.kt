package com.tuhoc.phatnguoi.data.local

import android.content.Context
import android.util.Log
import com.tuhoc.phatnguoi.data.firebase.FirebaseUserService
import com.tuhoc.phatnguoi.utils.AdvancedRateLimiter
import com.tuhoc.phatnguoi.utils.RateLimitResult

class AuthManager(context: Context) {
    private val firebaseUserService = FirebaseUserService()
    private val rateLimiter = AdvancedRateLimiter(context)
    private val TAG = "AuthManager"

    /**
     * Kiểm tra số điện thoại có tồn tại trong Firestore không
     */
    suspend fun phoneExists(phoneNumber: String): Boolean {
        return firebaseUserService.phoneExists(phoneNumber)
    }

    /**
     * Lấy thông tin user theo số điện thoại từ Firestore
     */
    suspend fun getUserByPhone(phoneNumber: String): User? {
        val userData = firebaseUserService.getUserByPhone(phoneNumber)
        return userData?.let { User.fromMap(it) }
    }

    /**
     * Tạo tài khoản mới với số điện thoại và mật khẩu
     * Mật khẩu sẽ được hash bằng BCrypt trước khi lưu vào Firestore
     */
    suspend fun createAccount(phoneNumber: String, password: String): Result<String> {
        return firebaseUserService.createAccount(phoneNumber, password)
    }

    /**
     * Đăng nhập với số điện thoại và mật khẩu
     * Kiểm tra và cập nhật trạng thái đăng nhập trong Firestore
     * Có tích hợp rate limiting để chống brute force
     */
    suspend fun login(phoneNumber: String, password: String): LoginResult {
        val key = "login_$phoneNumber"
        
        // Kiểm tra rate limit trước
        val rateLimitResult = rateLimiter.canProceed(key)
        if (!rateLimitResult.canProceed) {
            Log.w(TAG, "Rate limit: ${rateLimitResult.message}")
            return LoginResult.Failed(
                isRateLimited = true,
                rateLimitResult = rateLimitResult,
                remainingAttempts = 0
            )
        }
        
        // Lấy số lần thử còn lại trước khi thử đăng nhập
        val remainingAttemptsBefore = rateLimiter.getRemainingAttempts(key)
        
        // Thử đăng nhập
        val success = firebaseUserService.login(phoneNumber, password)
        
        if (success) {
            // Reset rate limiter khi đăng nhập thành công
            rateLimiter.reset(key)
            Log.d(TAG, "Đăng nhập thành công cho $phoneNumber")
            return LoginResult.Success
        } else {
            // Ghi nhận lần thử thất bại
            rateLimiter.recordFailedAttempt(key)
            
            // Kiểm tra lại rate limit sau khi ghi nhận attempt (có thể đã bị lockout)
            val rateLimitResultAfter = rateLimiter.canProceed(key)
            
            if (!rateLimitResultAfter.canProceed) {
                // Đã bị lockout sau khi ghi nhận attempt
                Log.w(TAG, "Đã bị rate limit sau khi nhập sai: ${rateLimitResultAfter.message}")
                return LoginResult.Failed(
                    isRateLimited = true,
                    rateLimitResult = rateLimitResultAfter,
                    remainingAttempts = 0
                )
            }
            
            // Lấy số lần thử còn lại sau khi ghi nhận attempt
            val remainingAttemptsAfter = rateLimiter.getRemainingAttempts(key)
            
            Log.d(TAG, "Mật khẩu không đúng cho $phoneNumber, còn $remainingAttemptsAfter lần thử")
            return LoginResult.Failed(
                isRateLimited = false,
                rateLimitResult = null,
                remainingAttempts = remainingAttemptsAfter
            )
        }
    }
    
    /**
     * Lấy thông tin rate limit hiện tại (để hiển thị trên UI)
     */
    fun getRateLimitInfo(phoneNumber: String): RateLimitResult {
        val key = "login_$phoneNumber"
        return rateLimiter.getLockoutInfo(key)
    }
    
    /**
     * Lấy số lần thử còn lại trước khi bị lockout
     */
    fun getRemainingAttempts(phoneNumber: String): Int {
        val key = "login_$phoneNumber"
        return rateLimiter.getRemainingAttempts(key)
    }

    /**
     * Cập nhật mật khẩu cho user trong Firestore
     * Mật khẩu mới sẽ được hash bằng BCrypt
     */
    suspend fun updatePassword(phoneNumber: String, newPassword: String): Result<Boolean> {
        return firebaseUserService.updatePassword(phoneNumber, newPassword)
    }

    /**
     * Đặt lại mật khẩu (dùng cho quên mật khẩu)
     * Mật khẩu mới sẽ được hash bằng BCrypt
     */
    suspend fun resetPassword(phoneNumber: String, newPassword: String): Result<Boolean> {
        return firebaseUserService.updatePassword(phoneNumber, newPassword)
    }

    suspend fun logout() {
        val phoneNumber = getPhoneNumber()
        phoneNumber?.let {
            firebaseUserService.logout(it)
        }
    }

    suspend fun isLoggedIn(): Boolean {
        return firebaseUserService.isLoggedIn()
    }

    suspend fun getPhoneNumber(): String? {
        val user = getCurrentUser()
        return user?.phoneNumber
    }

    /**
     * Lấy thông tin user hiện tại đang đăng nhập từ Firestore
     */
    suspend fun getCurrentUser(): User? {
        val userData = firebaseUserService.getCurrentUser()
        return userData?.let { User.fromMap(it) }
    }

    // Synchronous version for compatibility (sử dụng runBlocking trong background)
    fun isLoggedInSync(): Boolean {
        return try {
            kotlinx.coroutines.runBlocking {
                firebaseUserService.isLoggedIn()
            }
        } catch (e: Exception) {
            false
        }
    }

    fun getPhoneNumberSync(): String? {
        return try {
            kotlinx.coroutines.runBlocking {
                val user = getCurrentUser()
                user?.phoneNumber
            }
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Kết quả đăng nhập
 */
sealed class LoginResult {
    object Success : LoginResult()
    data class Failed(
        val isRateLimited: Boolean,
        val rateLimitResult: RateLimitResult?,
        val remainingAttempts: Int = 0
    ) : LoginResult()
}
