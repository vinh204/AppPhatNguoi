package com.tuhoc.phatnguoi.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * FirebaseUserService - Service để quản lý users trong Firestore
 */
class FirebaseUserService {
    private val db = FirebaseFirestore.getInstance()
    private val repository = FirebaseRepository()

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
     */
    suspend fun createAccount(phoneNumber: String, password: String): Result<String> {
        // Logout tất cả user trước
        logoutAll()
        
        val userData = mapOf(
            "phoneNumber" to phoneNumber,
            "password" to password,
            "isLoggedIn" to true,
            "createdAt" to Timestamp.now(),
            "updatedAt" to Timestamp.now()
        )
        
        return repository.saveDocument("users", userData, phoneNumber)
    }

    /**
     * Đăng nhập
     */
    suspend fun login(phoneNumber: String, password: String): Boolean {
        val user = getUserByPhone(phoneNumber)
        
        return if (user != null && user["password"] == password) {
            // Logout tất cả user trước
            logoutAll()
            // Login user này
            repository.updateDocument("users", phoneNumber, mapOf(
                "isLoggedIn" to true,
                "updatedAt" to Timestamp.now()
            ))
            true
        } else {
            false
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
     */
    suspend fun updatePassword(phoneNumber: String, newPassword: String): Result<Boolean> {
        return repository.updateDocument("users", phoneNumber, mapOf(
            "password" to newPassword,
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

