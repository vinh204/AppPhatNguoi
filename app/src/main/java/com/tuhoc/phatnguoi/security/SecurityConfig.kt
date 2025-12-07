package com.tuhoc.phatnguoi.security

/**
 * SecurityConfig - Cấu hình bảo mật tập trung
 * 
 * Quản lý tất cả các cấu hình bảo mật của ứng dụng
 */
object SecurityConfig {
    
    // ============================================================
    // PASSWORD CONFIGURATION (PIN 6 chữ số)
    // ============================================================
    object Password {
        const val PIN_LENGTH = 6 // PIN 6 chữ số
        const val MIN_LENGTH = PIN_LENGTH
        const val MAX_LENGTH = PIN_LENGTH
        const val REQUIRE_DIGITS_ONLY = true // PIN chỉ chứa chữ số
        
        // Các PIN bị cấm (dễ đoán)
        val FORBIDDEN_PINS = listOf(
            "000000", "111111", "222222", "333333", "444444", "555555",
            "666666", "777777", "888888", "999999",
            "123456", "654321", "012345", "543210",
            "123123", "321321", "456456", "654654",
            "111222", "222333", "333444", "444555", "555666",
            "666777", "777888", "888999"
        )
    }
    
    // ============================================================
    // SESSION CONFIGURATION
    // ============================================================
    object Session {
        const val DURATION_DAYS = 7L
        const val REFRESH_TOKEN_DURATION_DAYS = 30L
        const val AUTO_REFRESH_THRESHOLD_DAYS = 1L
    }
    
    // ============================================================
    // RATE LIMITING CONFIGURATION
    // ============================================================
    object RateLimit {
        // Login rate limiting
        const val LOGIN_MAX_ATTEMPTS_LEVEL1 = 3
        const val LOGIN_TIME_WINDOW_MINUTES_LEVEL1 = 5L
        const val LOGIN_LOCKOUT_SECONDS_LEVEL1 = 6L
        
        const val LOGIN_MAX_ATTEMPTS_LEVEL2 = 3
        const val LOGIN_TIME_WINDOW_MINUTES_LEVEL2 = 5L
        const val LOGIN_LOCKOUT_MINUTES_LEVEL2 = 5L
        
        const val LOGIN_LOCKOUT_MINUTES_LEVEL3 = 60L
        
        // Phone exists rate limiting
        const val PHONE_EXISTS_MAX_ATTEMPTS = 10
        const val PHONE_EXISTS_TIME_WINDOW_MINUTES = 15L
        const val PHONE_EXISTS_LOCKOUT_MINUTES = 30L
        
        // Password reset rate limiting
        const val PASSWORD_RESET_MAX_ATTEMPTS = 5
        const val PASSWORD_RESET_TIME_WINDOW_MINUTES = 60L
        const val PASSWORD_RESET_LOCKOUT_MINUTES = 60L
        
        // OTP send rate limiting (multi-tier)
        const val OTP_SEND_MAX_ATTEMPTS_10_MIN = 3
        const val OTP_SEND_TIME_WINDOW_10_MIN = 10L
        const val OTP_SEND_MAX_ATTEMPTS_1_HOUR = 5
        const val OTP_SEND_TIME_WINDOW_1_HOUR = 60L
        const val OTP_SEND_MAX_ATTEMPTS_24_HOURS = 10
        const val OTP_SEND_TIME_WINDOW_24_HOURS = 24L
        const val OTP_SEND_LOCKOUT_MINUTES = 30L
        
        // OTP verify rate limiting (sử dụng AdvancedRateLimiter, dùng config login)
        // OTP verify sử dụng cùng config với login rate limiting
        
        // Tra cứu rate limiting (cho user chưa đăng nhập)
        const val TRACUU_MAX_ATTEMPTS_24_HOURS = 3
        const val TRACUU_TIME_WINDOW_24_HOURS = 24L
        
        // Advanced Rate Limiter reset configuration
        const val RESET_SMALL_FAIL_COUNT_MINUTES = 30L // Reset số lần thử sau 30 phút không thử
        const val RESET_ALL_HOURS = 24L // Reset toàn bộ sau 24 giờ không thử
    }
    
    // ============================================================
    // NETWORK SECURITY CONFIGURATION
    // ============================================================
    object Network {
        const val REQUIRE_HTTPS = true
        const val ALLOW_CLEARTEXT = false // Chỉ cho phép trong development
        const val TLS_MIN_VERSION = "1.2"
    }
    
    // ============================================================
    // AUDIT LOGGING CONFIGURATION
    // ============================================================
    object AuditLog {
        const val ENABLED = true
        const val LOG_RETENTION_DAYS = 90L // Giữ log trong 90 ngày
        const val LOG_SUSPICIOUS_ACTIVITIES = true
    }
    
    // ============================================================
    // ENCRYPTION CONFIGURATION
    // ============================================================
    object Encryption {
        const val USE_ENCRYPTED_PREFERENCES = true
        const val KEY_ALGORITHM = "AES256_GCM"
        const val PREF_KEY_ENCRYPTION = "AES256_SIV"
        const val PREF_VALUE_ENCRYPTION = "AES256_GCM"
    }
    
    // ============================================================
    // INPUT VALIDATION CONFIGURATION
    // ============================================================
    object InputValidation {
        const val ENABLE_SQL_INJECTION_CHECK = true
        const val ENABLE_XSS_CHECK = true
        const val ENABLE_COMMAND_INJECTION_CHECK = true
        const val ENABLE_PATH_TRAVERSAL_CHECK = true
        const val MAX_INPUT_LENGTH = 1000
    }
    
    // ============================================================
    // OTP CONFIGURATION
    // ============================================================
    object OTP {
        const val LENGTH = 4 // Độ dài OTP (4 chữ số)
        const val VALIDITY_MINUTES = 5L // Thời gian hết hạn OTP (5 phút)
        const val RESEND_COUNTDOWN_SECONDS = 60L // Thời gian countdown trước khi có thể gửi lại OTP (60 giây)
    }
    
    // ============================================================
    // PHONE NUMBER VALIDATION
    // ============================================================
    object PhoneNumber {
        const val MIN_LENGTH = 10
        const val MAX_LENGTH = 11
        val VALID_PREFIXES = listOf("0", "+84")
    }
    
    // ============================================================
    // BIEN SO VALIDATION
    // ============================================================
    object BienSo {
        const val MIN_LENGTH = 5
        const val MAX_LENGTH = 10
        val VALID_PATTERN = Regex("^[A-Z0-9]{5,10}$")
    }
    
    // ============================================================
    // SECURITY FEATURES FLAGS
    // ============================================================
    object Features {
        const val ENABLE_2FA = false // Chưa triển khai
        const val ENABLE_BIOMETRIC = false // Chưa triển khai
        const val ENABLE_DEVICE_FINGERPRINTING = false // Chưa triển khai
        const val ENABLE_IP_WHITELIST = false // Chưa triển khai
    }
}

