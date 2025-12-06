package com.tuhoc.phatnguoi.security

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.tuhoc.phatnguoi.security.SecureLogger
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * SecurityAuditLogger - Ghi log các hoạt động bảo mật quan trọng
 * 
 * Ghi log các sự kiện:
 * - Đăng nhập thành công/thất bại
 * - Đăng xuất
 * - Đổi mật khẩu
 * - Tạo tài khoản mới
 * - Các hoạt động đáng ngờ (nhiều lần đăng nhập thất bại, v.v.)
 */
object SecurityAuditLogger {
    private val db = FirebaseFirestore.getInstance()
    private const val COLLECTION_NAME = "security_audit_logs"
    private val TAG = "SecurityAuditLogger"
    
    /**
     * Loại sự kiện bảo mật
     */
    enum class EventType {
        LOGIN_SUCCESS,
        LOGIN_FAILED,
        LOGOUT,
        PASSWORD_CHANGED,
        PASSWORD_RESET,
        ACCOUNT_CREATED,
        SESSION_EXPIRED,
        SUSPICIOUS_ACTIVITY,
        RATE_LIMIT_TRIGGERED,
        UNAUTHORIZED_ACCESS_ATTEMPT
    }
    
    /**
     * Mức độ nghiêm trọng
     */
    enum class Severity {
        INFO,      // Thông tin thông thường
        WARNING,   // Cảnh báo
        CRITICAL   // Nghiêm trọng
    }
    
    /**
     * Ghi log sự kiện bảo mật
     * 
     * @param eventType Loại sự kiện
     * @param phoneNumber Số điện thoại (có thể null)
     * @param details Chi tiết sự kiện
     * @param severity Mức độ nghiêm trọng
     * @param metadata Metadata bổ sung (IP address, device info, v.v.)
     */
    suspend fun logEvent(
        eventType: EventType,
        phoneNumber: String? = null,
        details: String = "",
        severity: Severity = Severity.INFO,
        metadata: Map<String, Any> = emptyMap()
    ) {
        try {
            val logData = mapOf(
                "eventType" to eventType.name,
                "phoneNumber" to (phoneNumber ?: "unknown"),
                "details" to details,
                "severity" to severity.name,
                "timestamp" to Timestamp.now(),
                "metadata" to metadata
            )
            
            // Lưu vào Firestore
            db.collection(COLLECTION_NAME)
                .add(logData)
                .await()
            
            // Log local để debug
            SecureLogger.d("Security audit log: ${eventType.name} - $details")
            
        } catch (e: Exception) {
            // Không throw exception để không ảnh hưởng đến flow chính
            SecureLogger.e("Lỗi khi ghi security audit log: ${e.message}", e)
        }
    }
    
    /**
     * Ghi log đăng nhập thành công
     */
    suspend fun logLoginSuccess(phoneNumber: String, metadata: Map<String, Any> = emptyMap()) {
        logEvent(
            eventType = EventType.LOGIN_SUCCESS,
            phoneNumber = phoneNumber,
            details = "User đăng nhập thành công",
            severity = Severity.INFO,
            metadata = metadata
        )
    }
    
    /**
     * Ghi log đăng nhập thất bại
     */
    suspend fun logLoginFailed(phoneNumber: String, reason: String, metadata: Map<String, Any> = emptyMap()) {
        logEvent(
            eventType = EventType.LOGIN_FAILED,
            phoneNumber = phoneNumber,
            details = "Đăng nhập thất bại: $reason",
            severity = Severity.WARNING,
            metadata = metadata
        )
    }
    
    /**
     * Ghi log đăng xuất
     */
    suspend fun logLogout(phoneNumber: String, metadata: Map<String, Any> = emptyMap()) {
        logEvent(
            eventType = EventType.LOGOUT,
            phoneNumber = phoneNumber,
            details = "User đăng xuất",
            severity = Severity.INFO,
            metadata = metadata
        )
    }
    
    /**
     * Ghi log đổi mật khẩu
     */
    suspend fun logPasswordChanged(phoneNumber: String, metadata: Map<String, Any> = emptyMap()) {
        logEvent(
            eventType = EventType.PASSWORD_CHANGED,
            phoneNumber = phoneNumber,
            details = "User đã đổi mật khẩu",
            severity = Severity.INFO,
            metadata = metadata
        )
    }
    
    /**
     * Ghi log reset mật khẩu
     */
    suspend fun logPasswordReset(phoneNumber: String, metadata: Map<String, Any> = emptyMap()) {
        logEvent(
            eventType = EventType.PASSWORD_RESET,
            phoneNumber = phoneNumber,
            details = "User đã reset mật khẩu",
            severity = Severity.WARNING,
            metadata = metadata
        )
    }
    
    /**
     * Ghi log tạo tài khoản mới
     */
    suspend fun logAccountCreated(phoneNumber: String, metadata: Map<String, Any> = emptyMap()) {
        logEvent(
            eventType = EventType.ACCOUNT_CREATED,
            phoneNumber = phoneNumber,
            details = "Tài khoản mới được tạo",
            severity = Severity.INFO,
            metadata = metadata
        )
    }
    
    /**
     * Ghi log hoạt động đáng ngờ
     */
    suspend fun logSuspiciousActivity(
        phoneNumber: String?,
        description: String,
        metadata: Map<String, Any> = emptyMap()
    ) {
        logEvent(
            eventType = EventType.SUSPICIOUS_ACTIVITY,
            phoneNumber = phoneNumber,
            details = description,
            severity = Severity.CRITICAL,
            metadata = metadata
        )
    }
    
    /**
     * Ghi log rate limit được kích hoạt
     */
    suspend fun logRateLimitTriggered(phoneNumber: String, reason: String, metadata: Map<String, Any> = emptyMap()) {
        logEvent(
            eventType = EventType.RATE_LIMIT_TRIGGERED,
            phoneNumber = phoneNumber,
            details = "Rate limit được kích hoạt: $reason",
            severity = Severity.WARNING,
            metadata = metadata
        )
    }
    
    /**
     * Ghi log cố gắng truy cập trái phép
     */
    suspend fun logUnauthorizedAccessAttempt(
        phoneNumber: String?,
        description: String,
        metadata: Map<String, Any> = emptyMap()
    ) {
        logEvent(
            eventType = EventType.UNAUTHORIZED_ACCESS_ATTEMPT,
            phoneNumber = phoneNumber,
            details = description,
            severity = Severity.CRITICAL,
            metadata = metadata
        )
    }
    
    /**
     * Lấy metadata mặc định (device info, timestamp, v.v.)
     */
    fun getDefaultMetadata(): Map<String, Any> {
        return mapOf(
            "timestamp" to System.currentTimeMillis(),
            "date" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        )
    }
}



