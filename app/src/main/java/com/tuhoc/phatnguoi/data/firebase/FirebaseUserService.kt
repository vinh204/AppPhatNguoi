package com.tuhoc.phatnguoi.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.tuhoc.phatnguoi.security.PasswordHasher
import kotlinx.coroutines.tasks.await
import android.util.Log

/**
 * FirebaseUserService - Service để quản lý users trong Firestore
 */
class FirebaseUserService {
    private val db = FirebaseFirestore.getInstance()
    private val repository = FirebaseRepository()
    private val TAG = "FirebaseUserService"

    /**
     * Kiểm tra số điện thoại có tồn tại không
     */
    suspend fun phoneExists(phoneNumber: String): Boolean {
        return repository.getDocumentsWhere("users", "phoneNumber", phoneNumber)
            .getOrNull()?.isNotEmpty() ?: false
    }

    /**
     * Lấy thông tin user theo số điện thoại
     */
    suspend fun getUserByPhone(phoneNumber: String): Map<String, Any>? {
        val result = repository.getDocumentsWhere("users", "phoneNumber", phoneNumber)
        return result.getOrNull()?.firstOrNull()
    }

    /**
     * Tạo tài khoản mới
     * Mật khẩu sẽ được hash bằng BCrypt trước khi lưu vào Firestore
     */
    suspend fun createAccount(phoneNumber: String, password: String): Result<String> {
        // Logout tất cả user trước
        logoutAll()
        
        // Hash mật khẩu trước khi lưu
        val hashedPassword = PasswordHasher.hashPassword(password)
        Log.d(TAG, "Đã hash mật khẩu cho user: $phoneNumber")
        
        val userData = mapOf(
            "phoneNumber" to phoneNumber,
            "password" to hashedPassword,  // Lưu hash thay vì plain text
            "isLoggedIn" to true,
            "createdAt" to Timestamp.now(),
            "updatedAt" to Timestamp.now()
        )
        
        return repository.saveDocument("users", userData, phoneNumber)
    }

    /**
     * Đăng nhập
     * So sánh mật khẩu với hash đã lưu bằng BCrypt
     */
    suspend fun login(phoneNumber: String, password: String): Boolean {
        val user = getUserByPhone(phoneNumber)
        
        if (user == null) {
            Log.d(TAG, "Không tìm thấy user: $phoneNumber")
            return false
        }
        
        val storedHash = user["password"] as? String
        if (storedHash == null) {
            Log.w(TAG, "User $phoneNumber không có mật khẩu trong database")
            return false
        }
        
        // Verify mật khẩu với hash đã lưu bằng BCrypt
        val isValid = PasswordHasher.verifyPassword(password, storedHash)
        
        if (isValid) {
            Log.d(TAG, "Đăng nhập thành công cho user: $phoneNumber")
            // Logout tất cả user trước
            logoutAll()
            // Login user này
            repository.updateDocument("users", phoneNumber, mapOf(
                "isLoggedIn" to true,
                "updatedAt" to Timestamp.now()
            ))
            return true
        } else {
            Log.d(TAG, "Mật khẩu không đúng cho user: $phoneNumber")
            return false
        }
    }

    /**
     * Đăng xuất tất cả users
     */
    suspend fun logoutAll() {
        try {
            val users = repository.getAllDocuments("users").getOrNull() ?: return
            
            users.forEach { user ->
                val phoneNumber = user["phoneNumber"] as? String ?: return@forEach
                repository.updateDocument("users", phoneNumber, mapOf(
                    "isLoggedIn" to false,
                    "updatedAt" to Timestamp.now()
                ))
            }
        } catch (e: Exception) {
            // Ignore errors
        }
    }

    /**
     * Đăng xuất một user cụ thể
     */
    suspend fun logout(phoneNumber: String) {
        repository.updateDocument("users", phoneNumber, mapOf(
            "isLoggedIn" to false,
            "updatedAt" to Timestamp.now()
        ))
    }

    /**
     * Cập nhật mật khẩu
     * Mật khẩu mới sẽ được hash bằng BCrypt trước khi lưu
     */
    suspend fun updatePassword(phoneNumber: String, newPassword: String): Result<Boolean> {
        // Hash mật khẩu mới trước khi lưu
        val hashedPassword = PasswordHasher.hashPassword(newPassword)
        Log.d(TAG, "Đã hash mật khẩu mới cho user: $phoneNumber")
        
        return repository.updateDocument("users", phoneNumber, mapOf(
            "password" to hashedPassword,  // Lưu hash thay vì plain text
            "updatedAt" to Timestamp.now()
        ))
    }

    /**
     * Lấy user đang đăng nhập
     */
    suspend fun getCurrentUser(): Map<String, Any>? {
        val result = repository.getDocumentsWhere("users", "isLoggedIn", true)
        return result.getOrNull()?.firstOrNull()
    }

    /**
     * Kiểm tra user có đang đăng nhập không
     */
    suspend fun isLoggedIn(): Boolean {
        return getCurrentUser() != null
    }
}

