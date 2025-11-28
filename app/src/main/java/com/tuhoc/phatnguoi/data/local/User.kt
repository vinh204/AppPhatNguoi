package com.tuhoc.phatnguoi.data.local

/**
 * User data class - Thay thế UserEntity từ Room Database
 * Dữ liệu được lưu trong Firestore
 */
data class User(
    val phoneNumber: String,
    val password: String? = null,
    val isLoggedIn: Boolean = false
) {
    /**
     * Convert từ Map<String, Any> (từ Firestore) sang User
     */
    companion object {
        fun fromMap(data: Map<String, Any>): User {
            return User(
                phoneNumber = data["phoneNumber"] as? String ?: "",
                password = data["password"] as? String,
                isLoggedIn = (data["isLoggedIn"] as? Boolean) ?: false
            )
        }
    }
    
    /**
     * Convert sang Map<String, Any> để lưu vào Firestore
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "phoneNumber" to phoneNumber,
            "password" to (password ?: ""),
            "isLoggedIn" to isLoggedIn
        )
    }
}

