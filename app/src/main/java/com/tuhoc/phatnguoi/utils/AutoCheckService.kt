package com.tuhoc.phatnguoi.utils

import android.content.Context
import com.tuhoc.phatnguoi.data.local.AutoCheckManager
import com.tuhoc.phatnguoi.data.local.AuthManager
import com.tuhoc.phatnguoi.data.local.HistoryManager
import com.tuhoc.phatnguoi.data.local.NotificationSettingsManager
import com.tuhoc.phatnguoi.data.remote.PhatNguoiRepository
import com.tuhoc.phatnguoi.data.remote.SmsGatewayServiceFactory
import com.tuhoc.phatnguoi.utils.PermissionHelper
import com.tuhoc.phatnguoi.security.InputValidator
import com.tuhoc.phatnguoi.security.SecureErrorHandler
import com.tuhoc.phatnguoi.security.SecureLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.os.Build
import java.util.Calendar

/**
 * Service để tự động tra cứu các biển số đã đăng ký
 * 
 * Đã tích hợp các tính năng bảo mật:
 * - Secure logging để ẩn thông tin nhạy cảm
 * - Input validation để kiểm tra dữ liệu đầu vào
 * - Secure error handling để xử lý lỗi an toàn
 */
class AutoCheckService(private val context: Context) {
    private val autoCheckManager = AutoCheckManager(context)
    private val authManager = AuthManager(context)
    private val historyManager = HistoryManager(context)
    private val settingsManager = NotificationSettingsManager(context)
    private val notificationHelper = NotificationHelper(context)
    private val repository = PhatNguoiRepository(context)
    private val scope = CoroutineScope(Dispatchers.IO)
    
    // Security components
    private val inputValidator = InputValidator
    private val secureErrorHandler = SecureErrorHandler(context)
    
    /**
     * Thực hiện tự động tra cứu cho tất cả biển số đã đăng ký
     */
    suspend fun performAutoCheck() {
        withContext(Dispatchers.IO) {
            // Kiểm tra user đã đăng nhập chưa
            val isLoggedIn = authManager.isLoggedIn()
            if (!isLoggedIn) {
                return@withContext
            }
            
            // Kiểm tra có bật thông báo qua ứng dụng hoặc SMS không
            val notifyApp = settingsManager.getNotifyApp()
            val notifySMS = settingsManager.getNotifySMS()
            if (!notifyApp && !notifySMS) {
                // Không có phương thức thông báo nào được bật
                return@withContext
            }
            
            // Kiểm tra permission thông báo (Android 13+) nếu bật thông báo qua app
            if (notifyApp && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (!PermissionHelper.hasNotificationPermission(context)) {
                    SecureLogger.w("No notification permission")
                    // Vẫn tiếp tục nếu có bật SMS
                    if (!notifySMS) {
                        return@withContext
                    }
                }
            }
            
            // Lấy danh sách biển số cần tra cứu (đã bật)
            val autoCheckList = autoCheckManager.getEnabledAutoCheck()
            
            // Tra cứu từng biển số
            autoCheckList.forEach { autoCheck ->
                try {
                    // Validate biển số trước khi tra cứu
                    val normalizedBienSo = inputValidator.normalizeBienSo(autoCheck.bienSo)
                    val validationResult = inputValidator.validateBienSo(normalizedBienSo)
                    
                    if (validationResult is com.tuhoc.phatnguoi.security.InputValidator.ValidationResult.Error) {
                        SecureLogger.e("Invalid biển số: ${validationResult.message}")
                        return@forEach
                    }
                    
                    // Gọi API tra cứu qua repository với biển số đã được normalize
                    val result = repository.checkPhatNguoi(
                        plate = normalizedBienSo,
                        vehicleType = autoCheck.loaiXe
                    )
                    
                    val coViPham = result.viPham == true && !result.error
                    val soLoi = result.soLoiViPham ?: 0
                    val daXuPhat = result.soDaXuPhat ?: 0
                    val chuaXuPhat = result.soChuaXuPhat ?: 0
                    
                    // Lưu vào lịch sử (sử dụng biển số đã normalize)
                    val historyItem = com.tuhoc.phatnguoi.data.local.TraCuuHistoryItem(
                        bienSo = normalizedBienSo,
                        loaiXe = when (autoCheck.loaiXe) {
                            1 -> "Ô tô"
                            2 -> "Xe máy"
                            3 -> "Xe đạp điện"
                            else -> "Ô tô"
                        },
                        thoiGian = System.currentTimeMillis(),
                        coViPham = coViPham,
                        soLoi = if (coViPham) soLoi else null
                    )
                    historyManager.addHistory(historyItem)
                    
                    // Hiển thị thông báo hệ thống Android (chỉ khi bật thông báo qua ứng dụng)
                    if (settingsManager.getNotifyApp()) {
                        notificationHelper.showTraCuuNotification(
                            bienSo = normalizedBienSo,
                            loaiXe = autoCheck.loaiXe,
                            coViPham = coViPham,
                            soLoi = if (coViPham) soLoi else null,
                            daXuPhat = daXuPhat,
                            chuaXuPhat = chuaXuPhat
                        )
                    }
                    
                    // Gửi SMS nếu bật thông báo qua SMS và có vi phạm
                    if (settingsManager.getNotifySMS() && coViPham && soLoi > 0) {
                        try {
                            val phoneNumber = authManager.getPhoneNumber()
                            if (phoneNumber != null) {
                                // Validate số điện thoại
                                val normalizedPhone = inputValidator.normalizePhoneNumber(phoneNumber)
                                val phoneValidation = inputValidator.validatePhoneNumber(normalizedPhone)
                                
                                if (phoneValidation is com.tuhoc.phatnguoi.security.InputValidator.ValidationResult.Error) {
                                    SecureLogger.e("Invalid phone number for SMS: ${phoneValidation.message}")
                                    return@forEach
                                }
                                
                                // Tạo SMS Gateway Service dựa trên cấu hình
                                val smsService = SmsGatewayServiceFactory.create(
                                    useMock = settingsManager.getSmsUseMock(),
                                    apiUrl = settingsManager.getSmsApiUrl().takeIf { it.isNotEmpty() },
                                    apiKey = settingsManager.getSmsApiKey().takeIf { it.isNotEmpty() }
                                )
                                
                                // Tạo nội dung tin nhắn
                                val loaiXeText = when (autoCheck.loaiXe) {
                                    1 -> "Ô tô"
                                    2 -> "Xe máy"
                                    3 -> "Xe đạp điện"
                                    else -> "Ô tô"
                                }
                                
                                val message = buildString {
                                    append("Thông báo vi phạm giao thông\n")
                                    append("Biển số: $normalizedBienSo\n")
                                    append("Loại xe: $loaiXeText\n")
                                    append("Số lỗi: $soLoi\n")
                                    append("Đã xử phạt: $daXuPhat\n")
                                    append("Chưa xử phạt: $chuaXuPhat\n")
                                    append("\nVui lòng kiểm tra chi tiết trong ứng dụng.")
                                }
                                
                                // Gửi SMS
                                val smsResult = smsService.sendSms(normalizedPhone, message)
                                when (smsResult) {
                                    is com.tuhoc.phatnguoi.data.remote.SmsResult.Success -> {
                                        SecureLogger.d("SMS sent successfully")
                                    }
                                    is com.tuhoc.phatnguoi.data.remote.SmsResult.Error -> {
                                        SecureLogger.e("SMS sending failed: ${smsResult.message}")
                                    }
                                }
                            } else {
                                SecureLogger.w("Phone number not found for SMS")
                            }
                        } catch (e: Exception) {
                            // Sử dụng SecureErrorHandler để xử lý lỗi an toàn
                            val userError = secureErrorHandler.handleError(e)
                            SecureLogger.e("Error sending SMS: ${userError.message}", e)
                        }
                    }
                } catch (e: Exception) {
                    // Sử dụng SecureErrorHandler để xử lý lỗi an toàn
                    val userError = secureErrorHandler.handleError(e)
                    SecureLogger.e("Error during auto check: ${userError.message}", e)
                }
            }
        }
    }
    
    /**
     * Kiểm tra có nên thực hiện auto check không dựa vào tần suất và thời gian
     */
    fun shouldPerformAutoCheck(): Boolean {
        val frequency = settingsManager.getFrequency()
        val hour = settingsManager.getHour()
        val minute = settingsManager.getMinute()
        
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK)
        val currentDayOfMonth = now.get(Calendar.DAY_OF_MONTH)
        
        // Kiểm tra thời gian
        if (currentHour != hour || currentMinute != minute) {
            return false
        }
        
        // Kiểm tra tần suất
        return when (frequency) {
            "Hàng ngày" -> true
            "Hàng tuần" -> {
                // Thực hiện vào ngày đầu tuần (Chủ nhật = 1, Thứ 2 = 2, ...)
                currentDayOfWeek == Calendar.SUNDAY
            }
            "Hàng tháng" -> {
                // Thực hiện vào ngày đầu tháng
                currentDayOfMonth == 1
            }
            else -> true
        }
    }
    
    /**
     * Thực hiện auto check nếu điều kiện thỏa mãn
     */
    fun performAutoCheckIfNeeded() {
        if (shouldPerformAutoCheck()) {
            scope.launch {
                performAutoCheck()
            }
        }
    }
}

