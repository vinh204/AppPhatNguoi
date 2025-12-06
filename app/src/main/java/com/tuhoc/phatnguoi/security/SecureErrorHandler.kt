package com.tuhoc.phatnguoi.security

import android.content.Context
import com.tuhoc.phatnguoi.BuildConfig
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException
import javax.net.ssl.SSLHandshakeException

/**
 * Secure Error Handler để xử lý lỗi an toàn
 * 
 * Không expose thông tin chi tiết về lỗi hệ thống cho user
 * Chỉ hiển thị thông báo user-friendly
 */
class SecureErrorHandler(private val context: Context) {
    
    /**
     * Xử lý exception và trả về thông báo user-friendly
     */
    fun handleError(throwable: Throwable): UserFriendlyError {
        // Log chi tiết chỉ trong debug mode
        if (BuildConfig.DEBUG) {
            SecureLogger.e("Error occurred: ${throwable.javaClass.simpleName}", throwable)
        } else {
            // Trong production, chỉ log generic error
            SecureLogger.e("An error occurred", null)
        }
        
        // Phân loại và trả về thông báo phù hợp
        return when (throwable) {
            // Network errors
            is UnknownHostException -> 
                UserFriendlyError(
                    "Không thể kết nối đến server. Vui lòng kiểm tra kết nối mạng của bạn.",
                    ErrorType.NETWORK_ERROR
                )
            
            is SocketTimeoutException -> 
                UserFriendlyError(
                    "Kết nối quá thời gian. Vui lòng thử lại sau.",
                    ErrorType.NETWORK_ERROR
                )
            
            is ConnectException -> 
                UserFriendlyError(
                    "Không thể kết nối đến server. Vui lòng kiểm tra kết nối mạng.",
                    ErrorType.NETWORK_ERROR
                )
            
            is IOException -> 
                UserFriendlyError(
                    "Lỗi kết nối. Vui lòng thử lại sau.",
                    ErrorType.NETWORK_ERROR
                )
            
            // SSL errors
            is SSLException, is SSLHandshakeException -> 
                UserFriendlyError(
                    "Lỗi bảo mật kết nối. Vui lòng kiểm tra cài đặt mạng.",
                    ErrorType.SECURITY_ERROR
                )
            
            // Security errors
            is SecurityException -> 
                UserFriendlyError(
                    "Không có quyền truy cập. Vui lòng kiểm tra cài đặt ứng dụng.",
                    ErrorType.SECURITY_ERROR
                )
            
            // Null pointer
            is NullPointerException -> 
                UserFriendlyError(
                    "Đã xảy ra lỗi không mong muốn. Vui lòng thử lại.",
                    ErrorType.UNKNOWN_ERROR
                )
            
            // Illegal argument
            is IllegalArgumentException -> 
                UserFriendlyError(
                    "Dữ liệu không hợp lệ. Vui lòng kiểm tra lại thông tin nhập vào.",
                    ErrorType.VALIDATION_ERROR
                )
            
            // Generic error
            else -> 
                UserFriendlyError(
                    "Đã xảy ra lỗi. Vui lòng thử lại sau.",
                    ErrorType.UNKNOWN_ERROR
                )
        }
    }
    
    /**
     * Xử lý error message string
     */
    fun handleErrorMessage(message: String?): UserFriendlyError {
        if (message.isNullOrBlank()) {
            return UserFriendlyError(
                "Đã xảy ra lỗi. Vui lòng thử lại sau.",
                ErrorType.UNKNOWN_ERROR
            )
        }
        
        // Kiểm tra nếu message chứa thông tin nhạy cảm
        val sanitizedMessage = sanitizeErrorMessage(message)
        
        return UserFriendlyError(
            sanitizedMessage,
            ErrorType.UNKNOWN_ERROR
        )
    }
    
    /**
     * Sanitize error message để loại bỏ thông tin nhạy cảm
     */
    private fun sanitizeErrorMessage(message: String): String {
        // Loại bỏ stack traces
        if (message.contains("Exception") || message.contains("at ")) {
            return "Đã xảy ra lỗi. Vui lòng thử lại sau."
        }
        
        // Loại bỏ file paths
        if (message.contains("/") || message.contains("\\")) {
            return "Đã xảy ra lỗi. Vui lòng thử lại sau."
        }
        
        // Loại bỏ class names
        if (message.contains("java.") || message.contains("android.")) {
            return "Đã xảy ra lỗi. Vui lòng thử lại sau."
        }
        
        return message
    }
    
    /**
     * User-friendly error message
     */
    data class UserFriendlyError(
        val message: String,
        val type: ErrorType = ErrorType.UNKNOWN_ERROR
    )
    
    /**
     * Error types
     */
    enum class ErrorType {
        NETWORK_ERROR,
        SECURITY_ERROR,
        VALIDATION_ERROR,
        UNKNOWN_ERROR
    }
}

