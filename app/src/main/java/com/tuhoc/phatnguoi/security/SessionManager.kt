package com.tuhoc.phatnguoi.security

import android.content.Context
import com.tuhoc.phatnguoi.security.EncryptedPreferencesHelper
import java.util.UUID
import java.util.concurrent.TimeUnit
import android.content.SharedPreferences

/**
 * SessionManager - Quản lý session tokens cho người dùng
 * 
 * Tính năng:
 * - Tạo session token khi đăng nhập
 * - Kiểm tra session token hợp lệ
 * - Tự động expire session sau thời gian nhất định
 * - Refresh session token
 * - Revoke session (đăng xuất)
 */
class SessionManager(private val context: Context) {
    private val prefs: SharedPreferences = EncryptedPreferencesHelper.create(
        context,
        "session_manager"
    )
    
    companion object {
        private const val KEY_SESSION_TOKEN = "session_token"
        private const val KEY_SESSION_EXPIRY = "session_expiry"
        private const val KEY_PHONE_NUMBER = "session_phone_number"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        
        // Session expires sau 7 ngày
        private const val SESSION_DURATION_DAYS = 7L
        // Refresh token expires sau 30 ngày
        private const val REFRESH_TOKEN_DURATION_DAYS = 30L
        // Session sẽ được refresh tự động nếu còn < 1 ngày
        private const val AUTO_REFRESH_THRESHOLD_DAYS = 1L
    }
    
    /**
     * Tạo session mới khi đăng nhập thành công
     * @param phoneNumber Số điện thoại của user
     * @return Session token
     */
    fun createSession(phoneNumber: String): String {
        val sessionToken = generateSecureToken()
        val refreshToken = generateSecureToken()
        val expiryTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(SESSION_DURATION_DAYS)
        val refreshExpiryTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(REFRESH_TOKEN_DURATION_DAYS)
        
        prefs.edit().apply {
            putString(KEY_SESSION_TOKEN, sessionToken)
            putLong(KEY_SESSION_EXPIRY, expiryTime)
            putString(KEY_PHONE_NUMBER, phoneNumber)
            putString(KEY_REFRESH_TOKEN, refreshToken)
            apply()
        }
        
        return sessionToken
    }
    
    /**
     * Kiểm tra session có hợp lệ không
     * @return true nếu session hợp lệ, false nếu không
     */
    fun isValidSession(): Boolean {
        val sessionToken = prefs.getString(KEY_SESSION_TOKEN, null)
        val expiryTime = prefs.getLong(KEY_SESSION_EXPIRY, 0)
        
        if (sessionToken.isNullOrBlank() || expiryTime == 0L) {
            return false
        }
        
        // Kiểm tra session đã hết hạn chưa
        if (System.currentTimeMillis() >= expiryTime) {
            // Thử refresh session bằng refresh token
            return tryRefreshSession()
        }
        
        return true
    }
    
    /**
     * Lấy session token hiện tại
     * @return Session token hoặc null nếu không có
     */
    fun getSessionToken(): String? {
        if (!isValidSession()) {
            return null
        }
        
        // Tự động refresh nếu gần hết hạn
        autoRefreshIfNeeded()
        
        return prefs.getString(KEY_SESSION_TOKEN, null)
    }
    
    /**
     * Lấy số điện thoại từ session
     * @return Số điện thoại hoặc null nếu không có session hợp lệ
     */
    fun getPhoneNumber(): String? {
        if (!isValidSession()) {
            return null
        }
        return prefs.getString(KEY_PHONE_NUMBER, null)
    }
    
    /**
     * Refresh session token (tạo token mới)
     * @return true nếu refresh thành công, false nếu không
     */
    fun refreshSession(): Boolean {
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        val phoneNumber = prefs.getString(KEY_PHONE_NUMBER, null)
        
        if (refreshToken.isNullOrBlank() || phoneNumber.isNullOrBlank()) {
            return false
        }
        
        // Tạo session mới
        createSession(phoneNumber)
        return true
    }
    
    /**
     * Tự động refresh session nếu gần hết hạn
     */
    private fun autoRefreshIfNeeded() {
        val expiryTime = prefs.getLong(KEY_SESSION_EXPIRY, 0)
        if (expiryTime == 0L) return
        
        val timeUntilExpiry = expiryTime - System.currentTimeMillis()
        val threshold = TimeUnit.DAYS.toMillis(AUTO_REFRESH_THRESHOLD_DAYS)
        
        if (timeUntilExpiry > 0 && timeUntilExpiry < threshold) {
            // Tự động refresh
            refreshSession()
        }
    }
    
    /**
     * Thử refresh session bằng refresh token
     */
    private fun tryRefreshSession(): Boolean {
        val refreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        val phoneNumber = prefs.getString(KEY_PHONE_NUMBER, null)
        
        if (refreshToken.isNullOrBlank() || phoneNumber.isNullOrBlank()) {
            clearSession()
            return false
        }
        
        // Refresh token vẫn còn hiệu lực, tạo session mới
        createSession(phoneNumber)
        return true
    }
    
    /**
     * Xóa session (đăng xuất)
     */
    fun clearSession() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Kiểm tra session còn bao nhiêu thời gian
     * @return Số milliseconds còn lại, hoặc 0 nếu đã hết hạn
     */
    fun getRemainingTime(): Long {
        val expiryTime = prefs.getLong(KEY_SESSION_EXPIRY, 0)
        if (expiryTime == 0L) return 0
        
        val remaining = expiryTime - System.currentTimeMillis()
        return if (remaining > 0) remaining else 0
    }
    
    /**
     * Tạo token ngẫu nhiên an toàn
     */
    private fun generateSecureToken(): String {
        // Sử dụng UUID kết hợp với timestamp và random
        return UUID.randomUUID().toString() + 
               "-" + 
               System.currentTimeMillis().toString(36) + 
               "-" + 
               (Math.random() * 1000000).toInt().toString(36)
    }
}



