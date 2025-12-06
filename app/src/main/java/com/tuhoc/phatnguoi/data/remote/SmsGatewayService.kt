package com.tuhoc.phatnguoi.data.remote

import android.util.Log
import kotlinx.coroutines.delay

/**
 * Interface cho SMS Gateway Service
 * Cho phép dễ dàng thay thế mock implementation bằng API thật
 */
interface ISmsGatewayService {
    /**
     * Gửi SMS qua gateway
     * @param phoneNumber Số điện thoại nhận (format: 84xxxxxxxxx hoặc 0xxxxxxxxx)
     * @param message Nội dung tin nhắn
     * @return Result chứa success/error
     */
    suspend fun sendSms(phoneNumber: String, message: String): SmsResult
}

/**
 * Kết quả gửi SMS
 */
sealed class SmsResult {
    data class Success(val messageId: String? = null) : SmsResult()
    data class Error(val message: String) : SmsResult()
}

/**
 * Mock implementation của SMS Gateway Service
 * Sẽ được thay thế bằng API thật sau này
 */
class MockSmsGatewayService : ISmsGatewayService {
    private val TAG = "MockSmsGatewayService"
    
    override suspend fun sendSms(phoneNumber: String, message: String): SmsResult {
        return try {
            // Simulate network delay
            delay(500)
            
            // Validate phone number
            val normalizedPhone = normalizePhoneNumber(phoneNumber)
            if (!isValidPhoneNumber(normalizedPhone)) {
                Log.w(TAG, "Số điện thoại không hợp lệ: $phoneNumber")
                return SmsResult.Error("Số điện thoại không hợp lệ")
            }
            
            // Mock: Log thay vì gửi thật
            val messageId = "mock_${System.currentTimeMillis()}"
            Log.d(TAG, "📱 [MOCK SMS] Gửi đến $normalizedPhone:")
            Log.d(TAG, "   Message ID: $messageId")
            Log.d(TAG, "   Nội dung: $message")
            
            // Simulate 95% success rate (để test error handling)
            val random = (0..100).random()
            if (random < 95) {
                SmsResult.Success(messageId)
            } else {
                Log.w(TAG, "   [MOCK] Simulate lỗi gửi SMS")
                SmsResult.Error("Lỗi mạng tạm thời (mock)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi gửi SMS", e)
            SmsResult.Error(e.message ?: "Có lỗi xảy ra khi gửi SMS")
        }
    }
    
    /**
     * Chuẩn hóa số điện thoại về format 84xxxxxxxxx
     */
    private fun normalizePhoneNumber(phone: String): String {
        // Loại bỏ khoảng trắng và ký tự đặc biệt
        var normalized = phone.replace(Regex("[^0-9]"), "")
        
        // Nếu bắt đầu bằng 0, thay bằng 84
        if (normalized.startsWith("0")) {
            normalized = "84" + normalized.substring(1)
        }
        // Nếu không bắt đầu bằng 84, thêm 84
        else if (!normalized.startsWith("84")) {
            normalized = "84$normalized"
        }
        
        return normalized
    }
    
    /**
     * Kiểm tra số điện thoại có hợp lệ không (Vietnam format)
     */
    private fun isValidPhoneNumber(phone: String): Boolean {
        // Format: 84xxxxxxxxx (10-11 số sau 84)
        val pattern = Regex("^84[0-9]{9,10}$")
        return pattern.matches(phone)
    }
}

/**
 * Real implementation của SMS Gateway Service (sẽ triển khai sau)
 * Sử dụng Retrofit để gọi API thật
 */
class RealSmsGatewayService(
    private val apiUrl: String,
    private val apiKey: String
) : ISmsGatewayService {
    private val TAG = "RealSmsGatewayService"
    
    // TODO: Implement với Retrofit khi có API thật
    // private val apiService: SmsGatewayApi = ...
    
    override suspend fun sendSms(phoneNumber: String, message: String): SmsResult {
        // TODO: Implement API call thật
        // return try {
        //     val response = apiService.sendSms(
        //         phone = normalizePhoneNumber(phoneNumber),
        //         message = message,
        //         apiKey = apiKey
        //     )
        //     if (response.isSuccessful) {
        //         SmsResult.Success(response.body()?.messageId)
        //     } else {
        //         SmsResult.Error(response.message())
        //     }
        // } catch (e: Exception) {
        //     SmsResult.Error(e.message ?: "Có lỗi xảy ra")
        // }
        
        // Tạm thời throw để nhắc implement
        return SmsResult.Error("RealSmsGatewayService chưa được triển khai. Vui lòng sử dụng MockSmsGatewayService.")
    }
    
    private fun normalizePhoneNumber(phone: String): String {
        var normalized = phone.replace(Regex("[^0-9]"), "")
        if (normalized.startsWith("0")) {
            normalized = "84" + normalized.substring(1)
        } else if (!normalized.startsWith("84")) {
            normalized = "84$normalized"
        }
        return normalized
    }
}

/**
 * Factory để tạo SMS Gateway Service
 * Cho phép dễ dàng switch giữa mock và real implementation
 */
object SmsGatewayServiceFactory {
    /**
     * Tạo SMS Gateway Service
     * @param useMock Nếu true thì dùng mock, false thì dùng real API
     * @param apiUrl API URL (chỉ cần khi useMock = false)
     * @param apiKey API Key (chỉ cần khi useMock = false)
     */
    fun create(
        useMock: Boolean = true,
        apiUrl: String? = null,
        apiKey: String? = null
    ): ISmsGatewayService {
        return if (useMock) {
            MockSmsGatewayService()
        } else {
            requireNotNull(apiUrl) { "API URL không được null khi dùng real service" }
            requireNotNull(apiKey) { "API Key không được null khi dùng real service" }
            RealSmsGatewayService(apiUrl, apiKey)
        }
    }
}








