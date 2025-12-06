package com.tuhoc.phatnguoi.security

import org.mindrot.jbcrypt.BCrypt

/**
 * Utility class để hash và verify mật khẩu sử dụng BCrypt
 * BCrypt tự động tạo salt và có thể điều chỉnh cost factor
 */
object PasswordHasher {
    /**
     * Hash mật khẩu với salt tự động
     * BCrypt sẽ tự động tạo salt và hash mật khẩu
     * 
     * @param password Mật khẩu plain text cần hash
     * @return Hash string (bao gồm salt và hash)
     */
    fun hashPassword(password: String): String {
        return try {
            BCrypt.hashpw(password, BCrypt.gensalt())
        } catch (e: Exception) {
            SecureLogger.e("Lỗi khi hash mật khẩu", e)
            throw RuntimeException("Không thể hash mật khẩu", e)
        }
    }
    
    /**
     * Kiểm tra mật khẩu có khớp với hash không
     * 
     * @param password Mật khẩu plain text cần kiểm tra
     * @param hash Hash string đã lưu trong database
     * @return true nếu mật khẩu khớp, false nếu không khớp hoặc lỗi
     */
    fun verifyPassword(password: String, hash: String): Boolean {
        return try {
            // BCrypt.checkpw sẽ extract salt từ hash và so sánh
            BCrypt.checkpw(password, hash)
        } catch (e: Exception) {
            SecureLogger.e("Lỗi khi verify mật khẩu", e)
            false
        }
    }
    
    /**
     * Kiểm tra xem một string có phải là BCrypt hash hợp lệ không
     * 
     * @param hash String cần kiểm tra
     * @return true nếu là BCrypt hash hợp lệ
     */
    fun isValidHash(hash: String): Boolean {
        // BCrypt hash thường bắt đầu bằng $2a$, $2b$, hoặc $2y$
        return hash.startsWith("\$2a\$") || 
               hash.startsWith("\$2b\$") || 
               hash.startsWith("\$2y\$")
    }
}

