package com.tuhoc.phatnguoi.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.tuhoc.phatnguoi.utils.EncryptedPreferencesHelper
import java.util.concurrent.TimeUnit

/**
 * Advanced Rate Limiter với nhiều cấp độ lockout
 * 
 * Cấp 1: 3 lần sai trong 5 phút → khóa 60 giây
 * Cấp 2: Sau khi hết 60s, nếu sai tiếp 3 lần trong 5 phút → khóa 5 phút
 * Cấp 3: Nếu vẫn sai → khóa 60 phút
 * 
 * Reset 2 tầng:
 * ✅ Tầng 1: 30 phút không thử → reset small-fail-count (Level 1 attempts)
 *    - small-fail-count = số lần sai liên tiếp trong 1 session / một thời gian ngắn
 *    - Tạo cảm giác "phiên đăng nhập mới" cho user
 * 🔥 Tầng 2: 24 giờ không thử → reset toàn bộ thống kê (tất cả levels)
 * 
 * Sử dụng EncryptedSharedPreferences để mã hóa dữ liệu rate limiting
 */
class AdvancedRateLimiter(private val context: Context) {
    private val prefs: SharedPreferences = EncryptedPreferencesHelper.create(
        context,
        "advanced_rate_limiter"
    )
    
    private val TAG = "AdvancedRateLimiter"
    
    companion object {
        // Cấp 1: 3 lần sai trong 5 phút → khóa 60 giây
        private const val LEVEL1_MAX_ATTEMPTS = 3
        private const val LEVEL1_TIME_WINDOW_MINUTES = 5L
        private const val LEVEL1_LOCKOUT_SECONDS = 60L
        
        // Cấp 2: 3 lần sai tiếp trong 5 phút → khóa 5 phút
        private const val LEVEL2_MAX_ATTEMPTS = 3
        private const val LEVEL2_TIME_WINDOW_MINUTES = 5L
        private const val LEVEL2_LOCKOUT_MINUTES = 5L
        
        // Cấp 3: Khóa 60 phút
        private const val LEVEL3_LOCKOUT_MINUTES = 60L
        
        // Reset
        private const val RESET_SMALL_FAIL_COUNT_MINUTES = 30L
        private const val RESET_ALL_HOURS = 24L
        
        // Keys
        private const val KEY_LEVEL1_ATTEMPTS = "level1_attempts_"
        private const val KEY_LEVEL1_TIMESTAMP = "level1_timestamp_"
        private const val KEY_LEVEL1_LOCKOUT_UNTIL = "level1_lockout_until_"
        
        private const val KEY_LEVEL2_ATTEMPTS = "level2_attempts_"
        private const val KEY_LEVEL2_TIMESTAMP = "level2_timestamp_"
        private const val KEY_LEVEL2_LOCKOUT_UNTIL = "level2_lockout_until_"
        
        private const val KEY_LEVEL3_LOCKOUT_UNTIL = "level3_lockout_until_"
        
        private const val KEY_LAST_ATTEMPT_TIME = "last_attempt_time_"
    }
    
    /**
     * Kiểm tra xem có thể thực hiện login không
     * @param key Key để identify user (ví dụ: phoneNumber)
     * @return RateLimitResult với thông tin về trạng thái
     */
    fun canProceed(key: String): RateLimitResult {
        val currentTime = System.currentTimeMillis()
        
        // Kiểm tra reset tự động
        checkAndResetIfNeeded(key, currentTime)
        
        // Kiểm tra Level 3 (khóa 60 phút)
        val level3LockoutUntil = prefs.getLong(KEY_LEVEL3_LOCKOUT_UNTIL + key, 0)
        if (currentTime < level3LockoutUntil) {
            val remainingSeconds = TimeUnit.MILLISECONDS.toSeconds(level3LockoutUntil - currentTime)
            Log.w(TAG, "Level 3 lockout cho $key, còn lại $remainingSeconds giây")
            return RateLimitResult(
                canProceed = false,
                level = 3,
                remainingSeconds = remainingSeconds.toInt(),
                message = "Bạn đã nhập sai mật khẩu quá nhiều lần. Vui lòng thử lại sau ${formatTime(remainingSeconds.toInt())}"
            )
        }
        
        // Kiểm tra Level 2 (khóa 5 phút)
        val level2LockoutUntil = prefs.getLong(KEY_LEVEL2_LOCKOUT_UNTIL + key, 0)
        if (currentTime < level2LockoutUntil) {
            val remainingSeconds = TimeUnit.MILLISECONDS.toSeconds(level2LockoutUntil - currentTime)
            Log.w(TAG, "Level 2 lockout cho $key, còn lại $remainingSeconds giây")
            return RateLimitResult(
                canProceed = false,
                level = 2,
                remainingSeconds = remainingSeconds.toInt(),
                message = "Bạn đã nhập sai mật khẩu nhiều lần. Vui lòng thử lại sau ${formatTime(remainingSeconds.toInt())}"
            )
        }
        
        // Kiểm tra Level 1 (khóa 60 giây)
        val level1LockoutUntil = prefs.getLong(KEY_LEVEL1_LOCKOUT_UNTIL + key, 0)
        if (currentTime < level1LockoutUntil) {
            val remainingSeconds = TimeUnit.MILLISECONDS.toSeconds(level1LockoutUntil - currentTime)
            Log.w(TAG, "Level 1 lockout cho $key, còn lại $remainingSeconds giây")
            return RateLimitResult(
                canProceed = false,
                level = 1,
                remainingSeconds = remainingSeconds.toInt(),
                message = "Bạn đã nhập sai mật khẩu nhiều lần. Vui lòng thử lại sau ${formatTime(remainingSeconds.toInt())}"
            )
        }
        
        // Có thể thử
        return RateLimitResult(canProceed = true, level = 0, remainingSeconds = 0, message = null)
    }
    
    /**
     * Ghi nhận một lần thử thất bại
     */
    fun recordFailedAttempt(key: String) {
        val currentTime = System.currentTimeMillis()
        
        // Cập nhật last attempt time
        prefs.edit().putLong(KEY_LAST_ATTEMPT_TIME + key, currentTime).apply()
        
        // Kiểm tra và reset nếu cần
        checkAndResetIfNeeded(key, currentTime)
        
        // Kiểm tra các lockout hiện tại
        val level1LockoutUntil = prefs.getLong(KEY_LEVEL1_LOCKOUT_UNTIL + key, 0)
        val level2LockoutUntil = prefs.getLong(KEY_LEVEL2_LOCKOUT_UNTIL + key, 0)
        val level3LockoutUntil = prefs.getLong(KEY_LEVEL3_LOCKOUT_UNTIL + key, 0)
        
        // Xử lý theo thứ tự ưu tiên
        if (level3LockoutUntil > 0 && currentTime < level3LockoutUntil) {
            // Đang bị lockout Level 3, không làm gì
            return
        }
        
        if (level2LockoutUntil > 0 && currentTime < level2LockoutUntil) {
            // Đang bị lockout Level 2, không làm gì
            return
        }
        
        if (level1LockoutUntil > 0 && currentTime < level1LockoutUntil) {
            // Đang bị lockout Level 1, không làm gì
            return
        }
        
        // Không bị lockout nào → xử lý Level 1 hoặc Level 2
        if (level1LockoutUntil > 0 && currentTime >= level1LockoutUntil) {
            // Đã hết lockout Level 1 → xử lý Level 2
            handleLevel2(key, currentTime)
        } else {
            // Chưa có lockout Level 1 hoặc chưa từng bị → xử lý Level 1
            handleLevel1(key, currentTime)
        }
        
        // Sau khi xử lý Level 2, kiểm tra Level 3
        if (level2LockoutUntil > 0 && currentTime >= level2LockoutUntil) {
            handleLevel3(key, currentTime)
        }
    }
    
    /**
     * Reset rate limiter khi đăng nhập thành công
     */
    fun reset(key: String) {
        Log.d(TAG, "Reset rate limiter cho $key")
        prefs.edit()
            .remove(KEY_LEVEL1_ATTEMPTS + key)
            .remove(KEY_LEVEL1_TIMESTAMP + key)
            .remove(KEY_LEVEL1_LOCKOUT_UNTIL + key)
            .remove(KEY_LEVEL2_ATTEMPTS + key)
            .remove(KEY_LEVEL2_TIMESTAMP + key)
            .remove(KEY_LEVEL2_LOCKOUT_UNTIL + key)
            .remove(KEY_LEVEL3_LOCKOUT_UNTIL + key)
            .remove(KEY_LAST_ATTEMPT_TIME + key)
            .apply()
    }
    
    /**
     * Xử lý Level 1: 3 lần sai trong 5 phút → khóa 60 giây
     */
    private fun handleLevel1(key: String, currentTime: Long) {
        val attemptsKey = KEY_LEVEL1_ATTEMPTS + key
        val timestampKey = KEY_LEVEL1_TIMESTAMP + key
        val lockoutKey = KEY_LEVEL1_LOCKOUT_UNTIL + key
        
        var attempts = prefs.getInt(attemptsKey, 0)
        var firstAttemptTime = prefs.getLong(timestampKey, 0)
        
        // Nếu chưa có timestamp hoặc đã hết thời gian window (5 phút) → reset
        if (firstAttemptTime == 0L || currentTime - firstAttemptTime > TimeUnit.MINUTES.toMillis(LEVEL1_TIME_WINDOW_MINUTES)) {
            attempts = 0
            firstAttemptTime = currentTime
            prefs.edit().putLong(timestampKey, firstAttemptTime).apply()
        }
        
        attempts++
        prefs.edit().putInt(attemptsKey, attempts).apply()
        
        // Nếu đạt 3 lần sai trong 5 phút → khóa 60 giây
        if (attempts >= LEVEL1_MAX_ATTEMPTS) {
            val lockoutUntil = currentTime + TimeUnit.SECONDS.toMillis(LEVEL1_LOCKOUT_SECONDS)
            prefs.edit()
                .putLong(lockoutKey, lockoutUntil)
                .putInt(attemptsKey, 0) // Reset attempts
                .putLong(timestampKey, 0)
                .apply()
            
            Log.w(TAG, "Level 1 lockout cho $key trong ${LEVEL1_LOCKOUT_SECONDS} giây")
        }
    }
    
    /**
     * Xử lý Level 2: Sau khi hết lockout Level 1, nếu sai tiếp 3 lần trong 5 phút → khóa 5 phút
     */
    private fun handleLevel2(key: String, currentTime: Long) {
        val level1LockoutUntil = prefs.getLong(KEY_LEVEL1_LOCKOUT_UNTIL + key, 0)
        
        // Chỉ xử lý Level 2 nếu đã hết lockout Level 1
        if (level1LockoutUntil > 0 && currentTime >= level1LockoutUntil) {
            val level2AttemptsKey = KEY_LEVEL2_ATTEMPTS + key
            val level2TimestampKey = KEY_LEVEL2_TIMESTAMP + key
            val level2LockoutKey = KEY_LEVEL2_LOCKOUT_UNTIL + key
            
            var level2Attempts = prefs.getInt(level2AttemptsKey, 0)
            var level2FirstAttemptTime = prefs.getLong(level2TimestampKey, 0)
            
            // Nếu chưa có timestamp hoặc đã hết thời gian window (5 phút) → reset
            if (level2FirstAttemptTime == 0L || currentTime - level2FirstAttemptTime > TimeUnit.MINUTES.toMillis(LEVEL2_TIME_WINDOW_MINUTES)) {
                level2Attempts = 0
                level2FirstAttemptTime = currentTime
                prefs.edit().putLong(level2TimestampKey, level2FirstAttemptTime).apply()
            }
            
            level2Attempts++
            prefs.edit().putInt(level2AttemptsKey, level2Attempts).apply()
            
            // Nếu đạt 3 lần sai tiếp trong 5 phút → khóa 5 phút
            if (level2Attempts >= LEVEL2_MAX_ATTEMPTS) {
                val lockoutUntil = currentTime + TimeUnit.MINUTES.toMillis(LEVEL2_LOCKOUT_MINUTES)
                prefs.edit()
                    .putLong(level2LockoutKey, lockoutUntil)
                    .putInt(level2AttemptsKey, 0)
                    .putLong(level2TimestampKey, 0)
                    .apply()
                
                Log.w(TAG, "Level 2 lockout cho $key trong ${LEVEL2_LOCKOUT_MINUTES} phút")
            }
        }
    }
    
    /**
     * Xử lý Level 3: Sau khi hết lockout Level 2, nếu vẫn sai → khóa 60 phút
     */
    private fun handleLevel3(key: String, currentTime: Long) {
        val level2LockoutUntil = prefs.getLong(KEY_LEVEL2_LOCKOUT_UNTIL + key, 0)
        
        // Chỉ xử lý Level 3 nếu đã hết lockout Level 2
        if (level2LockoutUntil > 0 && currentTime >= level2LockoutUntil) {
            val level2Attempts = prefs.getInt(KEY_LEVEL2_ATTEMPTS + key, 0)
            
            // Nếu vẫn có attempts trong Level 2 (tức là đã sai tiếp) → khóa Level 3
            if (level2Attempts > 0) {
                val lockoutUntil = currentTime + TimeUnit.MINUTES.toMillis(LEVEL3_LOCKOUT_MINUTES)
                prefs.edit()
                    .putLong(KEY_LEVEL3_LOCKOUT_UNTIL + key, lockoutUntil)
                    .putInt(KEY_LEVEL2_ATTEMPTS + key, 0)
                    .putLong(KEY_LEVEL2_TIMESTAMP + key, 0)
                    .putLong(KEY_LEVEL2_LOCKOUT_UNTIL + key, 0)
                    .apply()
                
                Log.w(TAG, "Level 3 lockout cho $key trong ${LEVEL3_LOCKOUT_MINUTES} phút")
            }
        }
    }
    
    /**
     * Kiểm tra và reset nếu cần
     * 
     * Tầng 1: 30 phút không thử → reset small-fail-count (Level 1 attempts)
     * Tầng 2: 24 giờ không thử → reset toàn bộ thống kê (tất cả levels)
     */
    private fun checkAndResetIfNeeded(key: String, currentTime: Long) {
        val lastAttemptTime = prefs.getLong(KEY_LAST_ATTEMPT_TIME + key, 0)
        
        if (lastAttemptTime == 0L) return
        
        val timeSinceLastAttempt = currentTime - lastAttemptTime
        
        // 🔥 Tầng 2: Reset toàn bộ sau 24 giờ không thử
        if (timeSinceLastAttempt > TimeUnit.HOURS.toMillis(RESET_ALL_HOURS)) {
            Log.d(TAG, "🔥 Tầng 2: Reset toàn bộ cho $key (24 giờ không thử)")
            reset(key)
            return
        }
        
        // ✅ Tầng 1: Reset small-fail-count sau 30 phút không thử
        // small-fail-count = số lần sai liên tiếp trong 1 session / một thời gian ngắn
        // Tạo cảm giác "phiên đăng nhập mới" cho user
        if (timeSinceLastAttempt > TimeUnit.MINUTES.toMillis(RESET_SMALL_FAIL_COUNT_MINUTES)) {
            Log.d(TAG, "✅ Tầng 1: Reset small-fail-count cho $key (30 phút không thử)")
            
            // Reset Level 1 (small-fail-count)
            prefs.edit()
                .remove(KEY_LEVEL1_ATTEMPTS + key)
                .remove(KEY_LEVEL1_TIMESTAMP + key)
                .remove(KEY_LEVEL1_LOCKOUT_UNTIL + key)
                .apply()
            
            // Nếu không có lockout Level 2 hoặc Level 3 đang active, reset cả Level 2
            val level2LockoutUntil = prefs.getLong(KEY_LEVEL2_LOCKOUT_UNTIL + key, 0)
            val level3LockoutUntil = prefs.getLong(KEY_LEVEL3_LOCKOUT_UNTIL + key, 0)
            
            // Chỉ reset Level 2 nếu không có lockout đang active
            if ((level2LockoutUntil == 0L || currentTime >= level2LockoutUntil) &&
                (level3LockoutUntil == 0L || currentTime >= level3LockoutUntil)) {
                prefs.edit()
                    .remove(KEY_LEVEL2_ATTEMPTS + key)
                    .remove(KEY_LEVEL2_TIMESTAMP + key)
                    .apply()
            }
        }
    }
    
    /**
     * Format thời gian thành chuỗi dễ đọc
     */
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
    
    /**
     * Lấy thông tin lockout hiện tại (để hiển thị trên UI)
     */
    fun getLockoutInfo(key: String): RateLimitResult {
        return canProceed(key)
    }
    
    /**
     * Lấy số lần thử còn lại trước khi bị lockout
     */
    fun getRemainingAttempts(key: String): Int {
        val currentTime = System.currentTimeMillis()
        checkAndResetIfNeeded(key, currentTime)
        
        // Kiểm tra các lockout hiện tại
        val level1LockoutUntil = prefs.getLong(KEY_LEVEL1_LOCKOUT_UNTIL + key, 0)
        val level2LockoutUntil = prefs.getLong(KEY_LEVEL2_LOCKOUT_UNTIL + key, 0)
        val level3LockoutUntil = prefs.getLong(KEY_LEVEL3_LOCKOUT_UNTIL + key, 0)
        
        // Nếu đang bị lockout → không còn lần thử
        if ((level1LockoutUntil > 0 && currentTime < level1LockoutUntil) ||
            (level2LockoutUntil > 0 && currentTime < level2LockoutUntil) ||
            (level3LockoutUntil > 0 && currentTime < level3LockoutUntil)) {
            return 0
        }
        
        // Kiểm tra Level 1
        if (level1LockoutUntil == 0L || currentTime >= level1LockoutUntil) {
            val attemptsKey = KEY_LEVEL1_ATTEMPTS + key
            val timestampKey = KEY_LEVEL1_TIMESTAMP + key
            val attempts = prefs.getInt(attemptsKey, 0)
            val firstAttemptTime = prefs.getLong(timestampKey, 0)
            
            // Nếu chưa có attempts hoặc đã hết window → còn đủ 3 lần
            if (attempts == 0 || firstAttemptTime == 0L) {
                return LEVEL1_MAX_ATTEMPTS
            }
            
            // Kiểm tra xem có trong window không
            if (currentTime - firstAttemptTime <= TimeUnit.MINUTES.toMillis(LEVEL1_TIME_WINDOW_MINUTES)) {
                return LEVEL1_MAX_ATTEMPTS - attempts
            } else {
                // Đã hết window → reset
                return LEVEL1_MAX_ATTEMPTS
            }
        }
        
        // Kiểm tra Level 2
        if (level1LockoutUntil > 0 && currentTime >= level1LockoutUntil) {
            val level2AttemptsKey = KEY_LEVEL2_ATTEMPTS + key
            val level2TimestampKey = KEY_LEVEL2_TIMESTAMP + key
            val level2Attempts = prefs.getInt(level2AttemptsKey, 0)
            val level2FirstAttemptTime = prefs.getLong(level2TimestampKey, 0)
            
            // Nếu chưa có attempts hoặc đã hết window → còn đủ 3 lần
            if (level2Attempts == 0 || level2FirstAttemptTime == 0L) {
                return LEVEL2_MAX_ATTEMPTS
            }
            
            // Kiểm tra xem có trong window không
            if (currentTime - level2FirstAttemptTime <= TimeUnit.MINUTES.toMillis(LEVEL2_TIME_WINDOW_MINUTES)) {
                return LEVEL2_MAX_ATTEMPTS - level2Attempts
            } else {
                // Đã hết window → reset
                return LEVEL2_MAX_ATTEMPTS
            }
        }
        
        return LEVEL1_MAX_ATTEMPTS
    }
}

/**
 * Kết quả kiểm tra rate limit
 */
data class RateLimitResult(
    val canProceed: Boolean,
    val level: Int, // 0 = không lockout, 1/2/3 = level lockout
    val remainingSeconds: Int,
    val message: String? // Message để hiển thị cho user
)

