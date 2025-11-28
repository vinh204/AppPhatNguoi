package com.tuhoc.phatnguoi.data.local

import android.content.Context
import android.content.SharedPreferences

/**
 * Quản lý cài đặt thông báo
 * Lưu các tùy chọn: thông báo qua ứng dụng, SMS, tần suất, thời gian
 */
class NotificationSettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "notification_settings",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_NOTIFY_APP = "notify_app"
        private const val KEY_NOTIFY_SMS = "notify_sms"
        private const val KEY_FREQUENCY = "frequency"
        private const val KEY_HOUR = "hour"
        private const val KEY_MINUTE = "minute"
        
        // Default values
        private const val DEFAULT_NOTIFY_APP = true
        private const val DEFAULT_NOTIFY_SMS = false
        private const val DEFAULT_FREQUENCY = "Hàng ngày"
        private const val DEFAULT_HOUR = 8
        private const val DEFAULT_MINUTE = 0
    }
    
    /**
     * Lưu cài đặt thông báo qua ứng dụng
     */
    fun setNotifyApp(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFY_APP, enabled).apply()
    }
    
    /**
     * Lấy cài đặt thông báo qua ứng dụng
     */
    fun getNotifyApp(): Boolean {
        return prefs.getBoolean(KEY_NOTIFY_APP, DEFAULT_NOTIFY_APP)
    }
    
    /**
     * Lưu cài đặt thông báo qua SMS
     */
    fun setNotifySMS(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFY_SMS, enabled).apply()
    }
    
    /**
     * Lấy cài đặt thông báo qua SMS
     */
    fun getNotifySMS(): Boolean {
        return prefs.getBoolean(KEY_NOTIFY_SMS, DEFAULT_NOTIFY_SMS)
    }
    
    /**
     * Lưu tần suất thông báo (Hàng ngày, Hàng tuần, Hàng tháng)
     */
    fun setFrequency(frequency: String) {
        prefs.edit().putString(KEY_FREQUENCY, frequency).apply()
    }
    
    /**
     * Lấy tần suất thông báo
     */
    fun getFrequency(): String {
        return prefs.getString(KEY_FREQUENCY, DEFAULT_FREQUENCY) ?: DEFAULT_FREQUENCY
    }
    
    /**
     * Lưu thời gian thông báo (giờ)
     */
    fun setHour(hour: Int) {
        prefs.edit().putInt(KEY_HOUR, hour).apply()
    }
    
    /**
     * Lấy thời gian thông báo (giờ)
     */
    fun getHour(): Int {
        return prefs.getInt(KEY_HOUR, DEFAULT_HOUR)
    }
    
    /**
     * Lưu thời gian thông báo (phút)
     */
    fun setMinute(minute: Int) {
        prefs.edit().putInt(KEY_MINUTE, minute).apply()
    }
    
    /**
     * Lấy thời gian thông báo (phút)
     */
    fun getMinute(): Int {
        return prefs.getInt(KEY_MINUTE, DEFAULT_MINUTE)
    }
    
    /**
     * Lưu tất cả cài đặt
     */
    fun saveSettings(
        notifyApp: Boolean,
        notifySMS: Boolean,
        frequency: String,
        hour: Int,
        minute: Int
    ) {
        prefs.edit().apply {
            putBoolean(KEY_NOTIFY_APP, notifyApp)
            putBoolean(KEY_NOTIFY_SMS, notifySMS)
            putString(KEY_FREQUENCY, frequency)
            putInt(KEY_HOUR, hour)
            putInt(KEY_MINUTE, minute)
            apply()
        }
    }
}



