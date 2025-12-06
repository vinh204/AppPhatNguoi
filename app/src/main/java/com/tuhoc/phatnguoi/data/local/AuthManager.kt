package com.tuhoc.phatnguoi.data.local

import android.content.Context
import android.util.Log
import com.tuhoc.phatnguoi.data.firebase.FirebaseUserService
import com.tuhoc.phatnguoi.security.SecurityAuditLogger
import com.tuhoc.phatnguoi.security.SessionManager
import com.tuhoc.phatnguoi.security.PinStrengthChecker
import com.tuhoc.phatnguoi.security.SecurityConfig
import com.tuhoc.phatnguoi.security.AdvancedRateLimiter
import com.tuhoc.phatnguoi.security.RateLimitResult
import com.tuhoc.phatnguoi.security.InputValidator

class AuthManager(context: Context) {
    private val firebaseUserService = FirebaseUserService()
    private val rateLimiter = AdvancedRateLimiter(context)
    private val sessionManager = SessionManager(context)
    private val TAG = "AuthManager"

    /**
     * Kiểm tra số điện thoại có tồn tại trong Firestore không
     * Có rate limiting để chống enumeration attack
     */
    suspend fun phoneExists(phoneNumber: String): Boolean {
        // Validate input
        val normalizedPhone = InputValidator.normalizePhoneNumber(phoneNumber)
        val validationResult = InputValidator.validatePhoneNumber(normalizedPhone)
        if (!validationResult.isSuccess()) {
            SecurityAuditLogger.logSuspiciousActivity(
                phoneNumber = null,
                description = "Attempt to check phone existence with invalid format: $phoneNumber"
            )
            return false
        }
        
        // Rate limiting cho phone exists để chống enumeration
        val key = "phone_exists_$normalizedPhone"
        val rateLimitResult = rateLimiter.canProceed(key)
        if (!rateLimitResult.canProceed) {
            SecurityAuditLogger.logRateLimitTriggered(
                phoneNumber = normalizedPhone,
                reason = "Phone exists check rate limit exceeded"
            )
            return false // Trả về false để không tiết lộ số điện thoại có tồn tại hay không
        }
        
        // Ghi nhận attempt
        rateLimiter.recordFailedAttempt(key)
        
        return firebaseUserService.phoneExists(normalizedPhone)
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
     * Có validation và kiểm tra độ mạnh mật khẩu
     */
    suspend fun createAccount(phoneNumber: String, password: String): Result<String> {
        // Validate input
        val normalizedPhone = InputValidator.normalizePhoneNumber(phoneNumber)
        val phoneValidation = InputValidator.validatePhoneNumber(normalizedPhone)
        if (!phoneValidation.isSuccess()) {
            SecurityAuditLogger.logSuspiciousActivity(
                phoneNumber = null,
                description = "Attempt to create account with invalid phone: $phoneNumber"
            )
            return Result.failure(IllegalArgumentException(phoneValidation.getErrorMessage()))
        }
        
        val passwordValidation = InputValidator.validatePassword(password)
        if (!passwordValidation.isSuccess()) {
            return Result.failure(IllegalArgumentException(passwordValidation.getErrorMessage()))
        }
        
        // Kiểm tra độ mạnh PIN (6 chữ số)
        val pinCheck = PinStrengthChecker.checkPin(password, normalizedPhone)
        if (!pinCheck.isValid) {
            SecurityAuditLogger.logSuspiciousActivity(
                phoneNumber = normalizedPhone,
                description = "Attempt to create account with weak PIN: ${pinCheck.feedback}"
            )
            return Result.failure(IllegalArgumentException(pinCheck.feedback ?: "PIN không đủ mạnh"))
        }
        
        // Tạo tài khoản
        val result = firebaseUserService.createAccount(normalizedPhone, password)
        
        // Nếu thành công, tạo session và ghi audit log
        result.onSuccess {
            val sessionToken = sessionManager.createSession(normalizedPhone)
            SecurityAuditLogger.logAccountCreated(normalizedPhone, SecurityAuditLogger.getDefaultMetadata())
            Log.d(TAG, "Tài khoản được tạo thành công: $normalizedPhone")
        }.onFailure { exception ->
            SecurityAuditLogger.logSuspiciousActivity(
                phoneNumber = normalizedPhone,
                description = "Failed to create account: ${exception.message}"
            )
        }
        
        return result
    }

    /**
     * Đăng nhập với số điện thoại và mật khẩu
     * Kiểm tra và cập nhật trạng thái đăng nhập trong Firestore
     * Có tích hợp rate limiting để chống brute force
     * Có validation input và audit logging
     */
    suspend fun login(phoneNumber: String, password: String): LoginResult {
        // Validate input
        val normalizedPhone = InputValidator.normalizePhoneNumber(phoneNumber)
        val phoneValidation = InputValidator.validatePhoneNumber(normalizedPhone)
        if (!phoneValidation.isSuccess()) {
            SecurityAuditLogger.logLoginFailed(
                phoneNumber = phoneNumber,
                reason = "Invalid phone number format"
            )
            return LoginResult.Failed(
                isRateLimited = false,
                rateLimitResult = null,
                remainingAttempts = 0
            )
        }
        
        val passwordValidation = InputValidator.validatePassword(password)
        if (!passwordValidation.isSuccess()) {
            SecurityAuditLogger.logLoginFailed(
                phoneNumber = normalizedPhone,
                reason = "Invalid password format"
            )
            return LoginResult.Failed(
                isRateLimited = false,
                rateLimitResult = null,
                remainingAttempts = 0
            )
        }
        
        val key = "login_$normalizedPhone"
        
        // Kiểm tra rate limit trước
        val rateLimitResult = rateLimiter.canProceed(key)
        if (!rateLimitResult.canProceed) {
            Log.w(TAG, "Rate limit: ${rateLimitResult.message}")
            SecurityAuditLogger.logRateLimitTriggered(
                phoneNumber = normalizedPhone,
                reason = "Login rate limit exceeded"
            )
            return LoginResult.Failed(
                isRateLimited = true,
                rateLimitResult = rateLimitResult,
                remainingAttempts = 0
            )
        }
        
        // Lấy số lần thử còn lại trước khi thử đăng nhập
        val remainingAttemptsBefore = rateLimiter.getRemainingAttempts(key)
        
        // Thử đăng nhập
        val success = firebaseUserService.login(normalizedPhone, password)
        
        if (success) {
            // Reset rate limiter khi đăng nhập thành công
            rateLimiter.reset(key)
            
            // Tạo session token
            val sessionToken = sessionManager.createSession(normalizedPhone)
            
            // Ghi audit log
            SecurityAuditLogger.logLoginSuccess(normalizedPhone, SecurityAuditLogger.getDefaultMetadata())
            
            Log.d(TAG, "Đăng nhập thành công cho $normalizedPhone")
            return LoginResult.Success
        } else {
            // Ghi nhận lần thử thất bại
            rateLimiter.recordFailedAttempt(key)
            
            // Ghi audit log
            SecurityAuditLogger.logLoginFailed(
                phoneNumber = normalizedPhone,
                reason = "Invalid password"
            )
            
            // Kiểm tra lại rate limit sau khi ghi nhận attempt (có thể đã bị lockout)
            val rateLimitResultAfter = rateLimiter.canProceed(key)
            
            if (!rateLimitResultAfter.canProceed) {
                // Đã bị lockout sau khi ghi nhận attempt
                Log.w(TAG, "Đã bị rate limit sau khi nhập sai: ${rateLimitResultAfter.message}")
                SecurityAuditLogger.logSuspiciousActivity(
                    phoneNumber = normalizedPhone,
                    description = "Account locked due to multiple failed login attempts"
                )
                return LoginResult.Failed(
                    isRateLimited = true,
                    rateLimitResult = rateLimitResultAfter,
                    remainingAttempts = 0
                )
            }
            
            // Lấy số lần thử còn lại sau khi ghi nhận attempt
            val remainingAttemptsAfter = rateLimiter.getRemainingAttempts(key)
            
            Log.d(TAG, "Mật khẩu không đúng cho $normalizedPhone, còn $remainingAttemptsAfter lần thử")
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
     * Có validation và kiểm tra độ mạnh mật khẩu
     */
    suspend fun updatePassword(phoneNumber: String, newPassword: String): Result<Boolean> {
        // Validate input
        val normalizedPhone = InputValidator.normalizePhoneNumber(phoneNumber)
        val passwordValidation = InputValidator.validatePassword(newPassword)
        if (!passwordValidation.isSuccess()) {
            return Result.failure(IllegalArgumentException(passwordValidation.getErrorMessage()))
        }
        
        // Kiểm tra độ mạnh PIN (6 chữ số)
        val pinCheck = PinStrengthChecker.checkPin(newPassword, normalizedPhone)
        if (!pinCheck.isValid) {
            return Result.failure(IllegalArgumentException(pinCheck.feedback ?: "PIN không đủ mạnh"))
        }
        
        // Cập nhật mật khẩu
        val result = firebaseUserService.updatePassword(normalizedPhone, newPassword)
        
        // Ghi audit log
        result.onSuccess {
            SecurityAuditLogger.logPasswordChanged(normalizedPhone, SecurityAuditLogger.getDefaultMetadata())
        }
        
        return result
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
            // Xóa session
            sessionManager.clearSession()
            
            // Logout từ Firestore
            firebaseUserService.logout(it)
            
            // Ghi audit log
            SecurityAuditLogger.logLogout(it, SecurityAuditLogger.getDefaultMetadata())
        }
    }

    suspend fun isLoggedIn(): Boolean {
        // Kiểm tra session token trước
        if (!sessionManager.isValidSession()) {
            // Session không hợp lệ, logout
            val phoneNumber = getPhoneNumber()
            phoneNumber?.let {
                firebaseUserService.logout(it)
                SecurityAuditLogger.logEvent(
                    eventType = SecurityAuditLogger.EventType.SESSION_EXPIRED,
                    phoneNumber = it,
                    details = "Session expired",
                    severity = SecurityAuditLogger.Severity.INFO
                )
            }
            return false
        }
        
        // Kiểm tra Firestore
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
