package com.tuhoc.phatnguoi.data.local

import android.content.Context
import android.util.Log
import com.tuhoc.phatnguoi.data.firebase.FirebaseUserService

class AuthManager(context: Context) {
    private val firebaseUserService = FirebaseUserService()
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
     * Lưu vào Firestore
     */
    suspend fun createAccount(phoneNumber: String, password: String): Result<String> {
        return firebaseUserService.createAccount(phoneNumber, password)
    }
    
    /**
     * Đăng nhập với số điện thoại và mật khẩu
     * Kiểm tra và cập nhật trạng thái đăng nhập trong Firestore
     */
    suspend fun login(phoneNumber: String, password: String): Boolean {
        return firebaseUserService.login(phoneNumber, password)
    }
    
    /**
     * Cập nhật mật khẩu cho user trong Firestore
     */
    suspend fun updatePassword(phoneNumber: String, newPassword: String): Result<Boolean> {
        return firebaseUserService.updatePassword(phoneNumber, newPassword)
    }
    
    /**
     * Đặt lại mật khẩu (dùng cho quên mật khẩu)
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

