package com.tuhoc.phatnguoi.data.remote

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.delay
import java.util.Random

/**
 * Service để xử lý OTP verification
 * Tạm thời dùng mock implementation, có thể thay thế bằng API thật sau
 */
class OtpService(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "otp_cache",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_OTP = "otp_"
        private const val KEY_OTP_EXPIRY = "otp_expiry_"
        private const val OTP_VALIDITY_MINUTES = 5L
        private const val OTP_LENGTH = 4
    }
    
    /**
     * Gửi OTP đến số điện thoại
     * @return true nếu gửi thành công, false nếu lỗi
     */
    suspend fun sendOtp(phoneNumber: String): Boolean {
        return try {
            // TODO: Thay thế bằng API thật khi có
            // val api = NetworkModule.phatNguoiApi
            // val response = api.sendOtp(phoneNumber)
            // return response.isSuccessful
            
            // Mock: Tạo OTP ngẫu nhiên
            val otp = generateOtp()
            val expiryTime = System.currentTimeMillis() + (OTP_VALIDITY_MINUTES * 60 * 1000)
            
            // Lưu OTP vào SharedPreferences
            prefs.edit()
                .putString(KEY_OTP + phoneNumber, otp)
                .putLong(KEY_OTP_EXPIRY + phoneNumber, expiryTime)
                .apply()
            
            // Mock delay để giống gọi API thật
            delay(1000)
            
            // Log để test (trong thực tế sẽ gửi qua SMS)
            Log.d("OtpService", "OTP gửi đến $phoneNumber: $otp (hết hạn sau ${OTP_VALIDITY_MINUTES} phút)")
            
            true
        } catch (e: Exception) {
            Log.e("OtpService", "Lỗi khi gửi OTP", e)
            false
        }
    }
    
    /**
     * Xác thực OTP
     * @return true nếu OTP hợp lệ, false nếu không hợp lệ hoặc hết hạn
     */
    fun verifyOtp(phoneNumber: String, otp: String): Boolean {
        val storedOtp = prefs.getString(KEY_OTP + phoneNumber, null)
        val expiryTime = prefs.getLong(KEY_OTP_EXPIRY + phoneNumber, 0)
        
        // Kiểm tra OTP có tồn tại không
        if (storedOtp == null) {
            Log.d("OtpService", "Không tìm thấy OTP cho số điện thoại: $phoneNumber")
            return false
        }
        
        // Kiểm tra OTP đã hết hạn chưa
        if (System.currentTimeMillis() > expiryTime) {
            Log.d("OtpService", "OTP đã hết hạn cho số điện thoại: $phoneNumber")
            // Xóa OTP hết hạn
            prefs.edit()
                .remove(KEY_OTP + phoneNumber)
                .remove(KEY_OTP_EXPIRY + phoneNumber)
                .apply()
            return false
        }
        
        // Kiểm tra OTP có khớp không
        val isValid = storedOtp == otp
        if (isValid) {
            // Xóa OTP sau khi xác thực thành công
            prefs.edit()
                .remove(KEY_OTP + phoneNumber)
                .remove(KEY_OTP_EXPIRY + phoneNumber)
                .apply()
            Log.d("OtpService", "OTP xác thực thành công cho số điện thoại: $phoneNumber")
        } else {
            Log.d("OtpService", "OTP không khớp cho số điện thoại: $phoneNumber")
        }
        
        return isValid
    }
    
    /**
     * Xóa OTP (dùng khi cần reset)
     */
    fun clearOtp(phoneNumber: String) {
        prefs.edit()
            .remove(KEY_OTP + phoneNumber)
            .remove(KEY_OTP_EXPIRY + phoneNumber)
            .apply()
    }
    
    /**
     * Tạo OTP ngẫu nhiên
     */
    private fun generateOtp(): String {
        val random = Random()
        return (1..OTP_LENGTH)
            .map { random.nextInt(10) }
            .joinToString("")
    }
}

