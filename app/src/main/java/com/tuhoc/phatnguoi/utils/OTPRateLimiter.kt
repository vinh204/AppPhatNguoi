package com.tuhoc.phatnguoi.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.util.concurrent.TimeUnit

/**
 * Rate Limiter cho OTP với nhiều cấp độ
 * 
 * Sử dụng EncryptedSharedPreferences để mã hóa dữ liệu rate limiting
 */
class OTPRateLimiter(private val context: Context) {
    private val prefs: SharedPreferences = EncryptedPreferencesHelper.create(
        context,
        "otp_rate_limiter"
    )
    
    private val TAG = "OTPRateLimiter"
    
    companion object {
        private const val MAX_ATTEMPTS_10_MIN = 3
        private const val MAX_ATTEMPTS_1_HOUR = 5
        private const val MAX_ATTEMPTS_24_HOURS = 10
        private const val TIME_WINDOW_10_MIN = 10L
        private const val TIME_WINDOW_1_HOUR = 60L
        private const val TIME_WINDOW_24_HOURS = 24L
        private const val LOCKOUT_MINUTES = 30L
        private const val KEY_LAST_OTP_TIME = "last_otp_time_"
        private const val KEY_ATTEMPTS_10_MIN = "attempts_10min_"
        private const val KEY_TIMESTAMP_10_MIN = "timestamp_10min_"
        private const val KEY_ATTEMPTS_1_HOUR = "attempts_1hour_"
        private const val KEY_TIMESTAMP_1_HOUR = "timestamp_1hour_"
        private const val KEY_ATTEMPTS_24_HOURS = "attempts_24hours_"
        private const val KEY_TIMESTAMP_24_HOURS = "timestamp_24hours_"
        private const val KEY_LOCKOUT_UNTIL = "lockout_until_"
    }
    
    fun canSendOTP(phoneNumber: String): OTPRateLimitResult {
        val key = phoneNumber
        val currentTime = System.currentTimeMillis()
        
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL + key, 0)
        if (currentTime < lockoutUntil) {
            val remainingSeconds = TimeUnit.MILLISECONDS.toSeconds(lockoutUntil - currentTime)
            Log.w(TAG, "OTP bị lockout cho $phoneNumber, còn lại $remainingSeconds giây")
            return OTPRateLimitResult(
                canSend = false,
                remainingSeconds = remainingSeconds.toInt(),
                message = "Bạn đã yêu cầu quá nhiều OTP. Vui lòng thử lại sau ${formatTime(remainingSeconds.toInt())}"
            )
        }
        
        if (!checkLimit(key, currentTime, MAX_ATTEMPTS_10_MIN, TIME_WINDOW_10_MIN, 
            KEY_ATTEMPTS_10_MIN, KEY_TIMESTAMP_10_MIN)) {
            return triggerLockout(key, currentTime, "Bạn đã yêu cầu quá nhiều OTP. Vui lòng thử lại sau")
        }
        
        if (!checkLimit(key, currentTime, MAX_ATTEMPTS_1_HOUR, TIME_WINDOW_1_HOUR,
            KEY_ATTEMPTS_1_HOUR, KEY_TIMESTAMP_1_HOUR)) {
            return triggerLockout(key, currentTime, "Bạn đã yêu cầu quá nhiều OTP. Vui lòng thử lại sau")
        }
        
        if (!checkLimit(key, currentTime, MAX_ATTEMPTS_24_HOURS, TIME_WINDOW_24_HOURS,
            KEY_ATTEMPTS_24_HOURS, KEY_TIMESTAMP_24_HOURS)) {
            return triggerLockout(key, currentTime, "Bạn đã yêu cầu quá nhiều OTP. Vui lòng thử lại sau")
        }
        
        return OTPRateLimitResult(canSend = true, remainingSeconds = 0, message = null)
    }
    
    private fun checkLimit(
        key: String,
        currentTime: Long,
        maxAttempts: Int,
        timeWindowMinutes: Long,
        attemptsKey: String,
        timestampKey: String
    ): Boolean {
        var attempts = prefs.getInt(attemptsKey + key, 0)
        var firstAttemptTime = prefs.getLong(timestampKey + key, 0)
        
        if (firstAttemptTime == 0L || currentTime - firstAttemptTime > TimeUnit.MINUTES.toMillis(timeWindowMinutes)) {
            attempts = 0
            firstAttemptTime = currentTime
            prefs.edit()
                .putInt(attemptsKey + key, 0)
                .putLong(timestampKey + key, firstAttemptTime)
                .apply()
        }
        
        return attempts < maxAttempts
    }
    
    private fun triggerLockout(key: String, currentTime: Long, message: String): OTPRateLimitResult {
        val lockoutUntil = currentTime + TimeUnit.MINUTES.toMillis(LOCKOUT_MINUTES)
        prefs.edit().putLong(KEY_LOCKOUT_UNTIL + key, lockoutUntil).apply()
        
        Log.w(TAG, "OTP lockout cho $key trong ${LOCKOUT_MINUTES} phút")
        
        return OTPRateLimitResult(
            canSend = false,
            remainingSeconds = TimeUnit.MINUTES.toSeconds(LOCKOUT_MINUTES).toInt(),
            message = message
        )
    }
    
    fun recordOTPSent(phoneNumber: String) {
        val key = phoneNumber
        val currentTime = System.currentTimeMillis()
        
        prefs.edit().putLong(KEY_LAST_OTP_TIME + key, currentTime).apply()
        
        incrementAttempts(key, currentTime, TIME_WINDOW_10_MIN, KEY_ATTEMPTS_10_MIN, KEY_TIMESTAMP_10_MIN)
        incrementAttempts(key, currentTime, TIME_WINDOW_1_HOUR, KEY_ATTEMPTS_1_HOUR, KEY_TIMESTAMP_1_HOUR)
        incrementAttempts(key, currentTime, TIME_WINDOW_24_HOURS, KEY_ATTEMPTS_24_HOURS, KEY_TIMESTAMP_24_HOURS)
        
        Log.d(TAG, "Đã ghi nhận gửi OTP cho $phoneNumber")
    }
    
    private fun incrementAttempts(
        key: String,
        currentTime: Long,
        timeWindowMinutes: Long,
        attemptsKey: String,
        timestampKey: String
    ) {
        var attempts = prefs.getInt(attemptsKey + key, 0)
        var firstAttemptTime = prefs.getLong(timestampKey + key, 0)
        
        if (firstAttemptTime == 0L || currentTime - firstAttemptTime > TimeUnit.MINUTES.toMillis(timeWindowMinutes)) {
            attempts = 0
            firstAttemptTime = currentTime
        }
        
        attempts++
        prefs.edit()
            .putInt(attemptsKey + key, attempts)
            .putLong(timestampKey + key, firstAttemptTime)
            .apply()
    }
    
    private fun formatTime(seconds: Int): String {
        if (seconds < 60) {
            return "$seconds giây"
        }
        
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        
        return if (remainingSeconds == 0) {
            "$minutes phút"
        } else {
            "$minutes phút $remainingSeconds giây"
        }
    }
    
    fun getRateLimitInfo(phoneNumber: String): OTPRateLimitResult {
        return canSendOTP(phoneNumber)
    }
}

data class OTPRateLimitResult(
    val canSend: Boolean,
    val remainingSeconds: Int,
    val message: String?
)
