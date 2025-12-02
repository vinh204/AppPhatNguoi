package com.tuhoc.phatnguoi.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.util.concurrent.TimeUnit

/**
 * Rate Limiter cho tra cứu (chỉ áp dụng cho user chưa đăng nhập)
 * 
 * Giới hạn: 3 lần tra cứu trong 24 giờ
 * 
 * Sử dụng EncryptedSharedPreferences để mã hóa dữ liệu rate limiting
 */
class TraCuuRateLimiter(private val context: Context) {
    private val prefs: SharedPreferences = EncryptedPreferencesHelper.create(
        context,
        "tra_cuu_rate_limiter"
    )
    
    private val TAG = "TraCuuRateLimiter"
    
    companion object {
        private const val MAX_ATTEMPTS_24_HOURS = 3
        private const val TIME_WINDOW_24_HOURS = 24L
        private const val KEY_ATTEMPTS_24_HOURS = "attempts_24hours_"
        private const val KEY_TIMESTAMP_24_HOURS = "timestamp_24hours_"
    }
    
    /**
     * Kiểm tra có thể tra cứu không
     */
    fun canTraCuu(): TraCuuRateLimitResult {
        val key = "guest_user" // Key cố định cho user chưa đăng nhập
        val currentTime = System.currentTimeMillis()
        
        var attempts = prefs.getInt(KEY_ATTEMPTS_24_HOURS + key, 0)
        var firstAttemptTime = prefs.getLong(KEY_TIMESTAMP_24_HOURS + key, 0)
        
        // Nếu chưa có timestamp hoặc đã hết thời gian window (24 giờ) → reset
        if (firstAttemptTime == 0L || currentTime - firstAttemptTime > TimeUnit.HOURS.toMillis(TIME_WINDOW_24_HOURS)) {
            attempts = 0
            firstAttemptTime = currentTime
            prefs.edit()
                .putInt(KEY_ATTEMPTS_24_HOURS + key, 0)
                .putLong(KEY_TIMESTAMP_24_HOURS + key, firstAttemptTime)
                .apply()
        }
        
        val canTraCuu = attempts < MAX_ATTEMPTS_24_HOURS
        
        if (canTraCuu) {
            return TraCuuRateLimitResult(
                canTraCuu = true,
                remainingAttempts = MAX_ATTEMPTS_24_HOURS - attempts,
                remainingSeconds = 0,
                message = null
            )
        } else {
            // Đã hết số lần tra cứu
            val remainingSeconds = TimeUnit.HOURS.toMillis(TIME_WINDOW_24_HOURS) - (currentTime - firstAttemptTime)
            
            return TraCuuRateLimitResult(
                canTraCuu = false,
                remainingAttempts = 0,
                remainingSeconds = remainingSeconds.toInt(),
                message = "Bạn đã tra cứu $MAX_ATTEMPTS_24_HOURS lần trong ngày. Vui lòng đăng nhập để tra cứu không giới hạn"
            )
        }
    }
    
    /**
     * Ghi nhận một lần tra cứu
     */
    fun recordTraCuu() {
        val key = "guest_user"
        val currentTime = System.currentTimeMillis()
        
        var attempts = prefs.getInt(KEY_ATTEMPTS_24_HOURS + key, 0)
        var firstAttemptTime = prefs.getLong(KEY_TIMESTAMP_24_HOURS + key, 0)
        
        // Nếu chưa có timestamp hoặc đã hết thời gian window (24 giờ) → reset
        if (firstAttemptTime == 0L || currentTime - firstAttemptTime > TimeUnit.HOURS.toMillis(TIME_WINDOW_24_HOURS)) {
            attempts = 0
            firstAttemptTime = currentTime
        }
        
        attempts++
        prefs.edit()
            .putInt(KEY_ATTEMPTS_24_HOURS + key, attempts)
            .putLong(KEY_TIMESTAMP_24_HOURS + key, firstAttemptTime)
            .apply()
        
        Log.d(TAG, "Đã ghi nhận tra cứu: $attempts/$MAX_ATTEMPTS_24_HOURS lần trong 24 giờ")
    }
    
    /**
     * Lấy thông tin rate limit hiện tại
     */
    fun getRateLimitInfo(): TraCuuRateLimitResult {
        return canTraCuu()
    }
    
    /**
     * Reset rate limiter (dùng khi user đăng nhập)
     */
    fun reset() {
        val key = "guest_user"
        prefs.edit()
            .remove(KEY_ATTEMPTS_24_HOURS + key)
            .remove(KEY_TIMESTAMP_24_HOURS + key)
            .apply()
        Log.d(TAG, "Đã reset rate limiter cho tra cứu")
    }
}

/**
 * Kết quả kiểm tra rate limit cho tra cứu
 */
data class TraCuuRateLimitResult(
    val canTraCuu: Boolean,
    val remainingAttempts: Int,
    val remainingSeconds: Int,
    val message: String?
)

