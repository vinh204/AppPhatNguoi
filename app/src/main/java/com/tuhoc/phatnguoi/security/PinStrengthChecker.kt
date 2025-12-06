package com.tuhoc.phatnguoi.security

/**
 * PinStrengthChecker - Kiểm tra độ mạnh của PIN 6 chữ số
 * 
 * Vì PIN chỉ có 6 chữ số, không thể áp dụng các tiêu chí như mật khẩu dài.
 * Thay vào đó, kiểm tra:
 * - Không phải PIN dễ đoán (123456, 000000, v.v.)
 * - Không phải pattern đơn giản (111111, 123123, v.v.)
 * - Không chứa thông tin cá nhân (số điện thoại)
 */
object PinStrengthChecker {
    
    // PIN bị cấm (dễ đoán)
    private val FORBIDDEN_PINS = SecurityConfig.Password.FORBIDDEN_PINS.toSet()
    
    /**
     * Kết quả kiểm tra độ mạnh PIN
     */
    enum class Strength {
        VERY_WEAK,    // Rất yếu (PIN bị cấm)
        WEAK,         // Yếu (pattern đơn giản)
        FAIR,         // Trung bình (có thể chấp nhận)
        GOOD,         // Tốt
        STRONG        // Mạnh (khó đoán)
    }
    
    /**
     * Kết quả kiểm tra chi tiết
     */
    data class CheckResult(
        val strength: Strength,
        val isValid: Boolean, // Có đủ mạnh để chấp nhận không
        val feedback: String? // Gợi ý (nếu có)
    )
    
    /**
     * Kiểm tra độ mạnh PIN
     * 
     * @param pin PIN 6 chữ số cần kiểm tra
     * @param phoneNumber Số điện thoại (để kiểm tra không chứa trong PIN)
     * @return CheckResult với thông tin chi tiết
     */
    fun checkPin(
        pin: String,
        phoneNumber: String? = null
    ): CheckResult {
        // Kiểm tra độ dài
        if (pin.length != 6) {
            return CheckResult(
                strength = Strength.VERY_WEAK,
                isValid = false,
                feedback = "Mật khẩu phải có đúng 6 chữ số"
            )
        }
        
        // Kiểm tra chỉ chứa chữ số
        if (!pin.all { it.isDigit() }) {
            return CheckResult(
                strength = Strength.VERY_WEAK,
                isValid = false,
                feedback = "Mật khẩu chỉ được chứa chữ số"
            )
        }
        
        // Kiểm tra PIN bị cấm
        if (FORBIDDEN_PINS.contains(pin)) {
            return CheckResult(
                strength = Strength.VERY_WEAK,
                isValid = false,
                feedback = "Mật khẩu này quá dễ đoán. Vui lòng chọn mật khẩu khác."
            )
        }
        
        // Kiểm tra pattern đơn giản
        val patternCheck = checkSimplePatterns(pin)
        if (!patternCheck.isValid) {
            return patternCheck
        }
        
        // Kiểm tra chứa thông tin cá nhân
        val personalInfoCheck = checkPersonalInfo(pin, phoneNumber)
        if (!personalInfoCheck.isValid) {
            return personalInfoCheck
        }
        
        // Kiểm tra độ đa dạng
        val diversityCheck = checkDiversity(pin)
        
        return diversityCheck
    }
    
    /**
     * Kiểm tra pattern đơn giản
     */
    private fun checkSimplePatterns(pin: String): CheckResult {
        // Tất cả số giống nhau (000000, 111111, v.v.)
        if (pin.all { it == pin[0] }) {
            return CheckResult(
                strength = Strength.VERY_WEAK,
                isValid = false,
                feedback = "Mật khẩu không được chứa tất cả số giống nhau"
            )
        }
        
        // Chuỗi tăng dần (123456, 234567, v.v.)
        var isAscending = true
        for (i in 1 until pin.length) {
            if (pin[i].digitToInt() != pin[i-1].digitToInt() + 1) {
                isAscending = false
                break
            }
        }
        if (isAscending) {
            return CheckResult(
                strength = Strength.VERY_WEAK,
                isValid = false,
                feedback = "Mật khẩu không được là chuỗi số tăng dần"
            )
        }
        
        // Chuỗi giảm dần (654321, 543210, v.v.)
        var isDescending = true
        for (i in 1 until pin.length) {
            if (pin[i].digitToInt() != pin[i-1].digitToInt() - 1) {
                isDescending = false
                break
            }
        }
        if (isDescending) {
            return CheckResult(
                strength = Strength.VERY_WEAK,
                isValid = false,
                feedback = "Mật khẩu không được là chuỗi số giảm dần"
            )
        }
        
        // Pattern lặp lại (123123, 456456, v.v.)
        if (pin.length == 6 && pin.substring(0, 3) == pin.substring(3, 6)) {
            return CheckResult(
                strength = Strength.WEAK,
                isValid = false,
                feedback = "Mật khẩu không được có pattern lặp lại"
            )
        }
        
        // Pattern đối xứng (123321, 456654, v.v.)
        if (pin == pin.reversed()) {
            return CheckResult(
                strength = Strength.WEAK,
                isValid = false,
                feedback = "Mật khẩu không được đối xứng"
            )
        }
        
        return CheckResult(
            strength = Strength.FAIR,
            isValid = true,
            feedback = null
        )
    }
    
    /**
     * Kiểm tra chứa thông tin cá nhân
     */
    private fun checkPersonalInfo(pin: String, phoneNumber: String?): CheckResult {
        if (phoneNumber != null) {
            // Loại bỏ các ký tự không phải số
            val phoneDigits = phoneNumber.filter { it.isDigit() }
            
            // Kiểm tra PIN chứa 4 số cuối của số điện thoại
            if (phoneDigits.length >= 4) {
                val last4Digits = phoneDigits.takeLast(4)
                if (pin.contains(last4Digits)) {
                    return CheckResult(
                        strength = Strength.WEAK,
                        isValid = false,
                        feedback = "Mật khẩu không nên chứa số điện thoại của bạn"
                    )
                }
            }
            
            // Kiểm tra PIN chứa 4 số đầu của số điện thoại
            if (phoneDigits.length >= 4) {
                val first4Digits = phoneDigits.take(4)
                if (pin.contains(first4Digits)) {
                    return CheckResult(
                        strength = Strength.WEAK,
                        isValid = false,
                        feedback = "Mật khẩu không nên chứa số điện thoại của bạn"
                    )
                }
            }
        }
        
        return CheckResult(
            strength = Strength.FAIR,
            isValid = true,
            feedback = null
        )
    }
    
    /**
     * Kiểm tra độ đa dạng của PIN
     */
    private fun checkDiversity(pin: String): CheckResult {
        val uniqueDigits = pin.toSet().size
        
        return when {
            uniqueDigits == 1 -> {
                // Đã được kiểm tra ở checkSimplePatterns
                CheckResult(Strength.VERY_WEAK, false, null)
            }
            uniqueDigits == 2 -> {
                // Chỉ có 2 số khác nhau (ví dụ: 112233)
                CheckResult(
                    strength = Strength.WEAK,
                    isValid = true, // Vẫn chấp nhận nhưng cảnh báo
                    feedback = "Mật khẩu chỉ có 2 số khác nhau, nên chọn mật khẩu đa dạng hơn"
                )
            }
            uniqueDigits == 3 -> {
                CheckResult(
                    strength = Strength.FAIR,
                    isValid = true,
                    feedback = null
                )
            }
            uniqueDigits >= 4 -> {
                CheckResult(
                    strength = Strength.STRONG,
                    isValid = true,
                    feedback = null
                )
            }
            else -> {
                CheckResult(
                    strength = Strength.FAIR,
                    isValid = true,
                    feedback = null
                )
            }
        }
    }
    
    /**
     * Lấy message mô tả độ mạnh
     */
    fun getStrengthMessage(strength: Strength): String {
        return when (strength) {
            Strength.VERY_WEAK -> "Rất yếu - PIN này rất dễ đoán"
            Strength.WEAK -> "Yếu - Nên chọn PIN khác"
            Strength.FAIR -> "Trung bình - PIN có thể chấp nhận được"
            Strength.GOOD -> "Tốt - PIN khá mạnh"
            Strength.STRONG -> "Mạnh - PIN rất an toàn"
        }
    }
}

