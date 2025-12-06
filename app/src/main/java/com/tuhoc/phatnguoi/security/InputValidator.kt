package com.tuhoc.phatnguoi.security

/**
 * Input Validator để validate input từ user
 * 
 * Bảo vệ chống:
 * - SQL Injection (kiểm tra trong validation)
 * - XSS (Cross-Site Scripting) (kiểm tra trong validation)
 * - Command Injection (kiểm tra trong validation)
 * - Path Traversal (kiểm tra trong validation)
 */
object InputValidator {
    // Pattern cho biển số xe: 5-10 ký tự chữ và số
    private val BIEN_SO_PATTERN = Regex("^[A-Z0-9]{5,10}$")
    
    // Pattern cho số điện thoại Việt Nam
    private val PHONE_PATTERN = Regex("^(\\+84|0)[0-9]{9,10}$")
    
    // Pattern phát hiện SQL Injection
    private val SQL_INJECTION_PATTERN = Regex(
        "([';]|(--)|(\\*)|(\\|)|(;)|(union)|(select)|(drop)|(delete)|(insert)|(update))",
        RegexOption.IGNORE_CASE
    )
    
    // Pattern phát hiện XSS
    private val XSS_PATTERN = Regex(
        "<script|javascript:|onerror=|onload=|onclick=|eval\\(|document\\.cookie",
        RegexOption.IGNORE_CASE
    )
    
    // Pattern phát hiện Command Injection
    private val COMMAND_INJECTION_PATTERN = Regex(
        "[;&|`$(){}]|(\\|\\|)|(&&)|(\\$\\{)|(\\$\\(\\$)",
        RegexOption.IGNORE_CASE
    )
    
    // Pattern phát hiện Path Traversal
    private val PATH_TRAVERSAL_PATTERN = Regex("\\.\\./|\\.\\.\\\\|/etc/|/proc/|/sys/")
    
    /**
     * Validate biển số xe
     */
    fun validateBienSo(bienSo: String): ValidationResult {
        return when {
            bienSo.isBlank() -> 
                ValidationResult.Error("Biển số không được để trống")
            
            !BIEN_SO_PATTERN.matches(bienSo.uppercase()) -> 
                ValidationResult.Error("Biển số không hợp lệ. Vui lòng nhập 5-10 ký tự chữ và số.")
            
            containsInjection(bienSo) -> 
                ValidationResult.Error("Biển số chứa ký tự không hợp lệ")
            
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Validate số điện thoại
     */
    fun validatePhoneNumber(phone: String): ValidationResult {
        return when {
            phone.isBlank() -> 
                ValidationResult.Error("Số điện thoại không được để trống")
            
            !PHONE_PATTERN.matches(phone.trim()) -> 
                ValidationResult.Error("Số điện thoại không hợp lệ. Vui lòng nhập số điện thoại Việt Nam (10-11 chữ số).")
            
            containsInjection(phone) -> 
                ValidationResult.Error("Số điện thoại chứa ký tự không hợp lệ")
            
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Validate mật khẩu (PIN 6 chữ số)
     */
    fun validatePassword(password: String): ValidationResult {
        return when {
            password.isBlank() -> 
                ValidationResult.Error("Mật khẩu không được để trống")
            
            password.length != 6 -> 
                ValidationResult.Error("Mật khẩu phải có đúng 6 chữ số")
            
            !password.all { it.isDigit() } -> 
                ValidationResult.Error("Mật khẩu chỉ được chứa chữ số")
            
            containsInjection(password) -> 
                ValidationResult.Error("Mật khẩu chứa ký tự không hợp lệ")
            
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Validate xác nhận mật khẩu
     */
    fun validateConfirmPassword(password: String, confirmPassword: String): ValidationResult {
        return when {
            confirmPassword.isBlank() -> 
                ValidationResult.Error("Mật khẩu xác nhận không được để trống")
            
            password != confirmPassword -> 
                ValidationResult.Error("Mật khẩu xác nhận không khớp")
            
            else -> ValidationResult.Success
        }
    }
    
    /**
     * Kiểm tra input có chứa injection pattern không
     */
    private fun containsInjection(input: String): Boolean {
        return SQL_INJECTION_PATTERN.containsMatchIn(input) || 
               XSS_PATTERN.containsMatchIn(input) ||
               COMMAND_INJECTION_PATTERN.containsMatchIn(input) ||
               PATH_TRAVERSAL_PATTERN.containsMatchIn(input)
    }
    
    /**
     * Normalize biển số (uppercase, loại bỏ khoảng trắng)
     */
    fun normalizeBienSo(bienSo: String): String {
        return bienSo.trim().uppercase().replace("\\s".toRegex(), "")
    }
    
    /**
     * Normalize số điện thoại (loại bỏ khoảng trắng, dấu gạch ngang)
     */
    fun normalizePhoneNumber(phone: String): String {
        return phone.trim().replace("[\\s-]".toRegex(), "")
    }
    
    /**
     * Kết quả validation
     */
    sealed class ValidationResult {
        object Success : ValidationResult()
        data class Error(val message: String) : ValidationResult()
        
        fun isSuccess(): Boolean = this is Success
        fun isError(): Boolean = this is Error
        
        fun getErrorMessage(): String {
            return when (this) {
                is Success -> ""
                is Error -> this.message
            }
        }
    }
}

