package com.tuhoc.phatnguoi.security

import android.util.Log
import com.tuhoc.phatnguoi.BuildConfig
import java.util.regex.Pattern

/**
 * Secure Logger để ẩn thông tin nhạy cảm trong logs
 * 
 * Tự động sanitize các thông tin như:
 * - Số điện thoại
 * - Biển số xe
 * - Mật khẩu
 * - API keys
 */
object SecureLogger {
    private const val TAG = "PhatNguoi"
    
    // Các pattern để phát hiện thông tin nhạy cảm
    private val SENSITIVE_PATTERNS = listOf(
        // Số điện thoại (10-11 chữ số)
        Pattern.compile("\\b(\\+84|0)?[0-9]{9,10}\\b"),
        // Biển số xe (5-10 ký tự chữ và số)
        Pattern.compile("\\b[A-Z0-9]{5,10}\\b"),
        // Mật khẩu trong log
        Pattern.compile("password\\s*[:=]\\s*[^\\s]+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("pass\\s*[:=]\\s*[^\\s]+", Pattern.CASE_INSENSITIVE),
        // API keys
        Pattern.compile("api[_-]?key\\s*[:=]\\s*[^\\s]+", Pattern.CASE_INSENSITIVE),
        Pattern.compile("apikey\\s*[:=]\\s*[^\\s]+", Pattern.CASE_INSENSITIVE),
        // Token
        Pattern.compile("token\\s*[:=]\\s*[^\\s]+", Pattern.CASE_INSENSITIVE),
        // OTP
        Pattern.compile("otp\\s*[:=]\\s*[0-9]{4,6}", Pattern.CASE_INSENSITIVE)
    )
    
    /**
     * Log debug message (chỉ trong debug mode)
     */
    fun d(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, sanitize(message))
        }
    }
    
    /**
     * Log error message
     * Trong production, chỉ log thông báo generic
     */
    fun e(message: String, throwable: Throwable? = null) {
        val sanitized = sanitize(message)
        if (BuildConfig.DEBUG) {
            Log.e(TAG, sanitized, throwable)
        } else {
            // Trong production, chỉ log lỗi generic để tránh leak thông tin
            Log.e(TAG, "An error occurred", null)
        }
    }
    
    /**
     * Log warning message
     */
    fun w(message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            Log.w(TAG, sanitize(message), throwable)
        }
    }
    
    /**
     * Log info message
     */
    fun i(message: String) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, sanitize(message))
        }
    }
    
    /**
     * Log verbose message (chỉ trong debug mode)
     */
    fun v(message: String) {
        if (BuildConfig.DEBUG) {
            Log.v(TAG, sanitize(message))
        }
    }
    
    /**
     * Sanitize message để loại bỏ thông tin nhạy cảm
     */
    private fun sanitize(message: String): String {
        var sanitized = message
        
        // Áp dụng các pattern để thay thế thông tin nhạy cảm
        SENSITIVE_PATTERNS.forEach { pattern ->
            sanitized = pattern.matcher(sanitized).replaceAll("[REDACTED]")
        }
        
        return sanitized
    }
}

