package com.tuhoc.phatnguoi.utils

import android.content.Context
import com.tuhoc.phatnguoi.data.local.AutoCheckManager
import com.tuhoc.phatnguoi.data.local.AuthManager
import com.tuhoc.phatnguoi.data.local.HistoryManager
import com.tuhoc.phatnguoi.data.local.NotificationSettingsManager
import com.tuhoc.phatnguoi.data.remote.PhatNguoiRepository
import com.tuhoc.phatnguoi.utils.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.os.Build
import java.util.Calendar

/**
 * Service để tự động tra cứu các biển số đã đăng ký
 */
class AutoCheckService(private val context: Context) {
    private val autoCheckManager = AutoCheckManager(context)
    private val authManager = AuthManager(context)
    private val historyManager = HistoryManager(context)
    private val settingsManager = NotificationSettingsManager(context)
    private val notificationHelper = NotificationHelper(context)
    private val repository = PhatNguoiRepository()
    private val scope = CoroutineScope(Dispatchers.IO)
    
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
            
            // Kiểm tra có bật thông báo qua ứng dụng không
            val notifyApp = settingsManager.getNotifyApp()
            if (!notifyApp) {
                return@withContext
            }
            
            // Kiểm tra permission thông báo (Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (!PermissionHelper.hasNotificationPermission(context)) {
                    android.util.Log.w("AutoCheckService", "No notification permission")
                    return@withContext
                }
            }
            
            // Lấy danh sách biển số cần tra cứu (đã bật)
            val autoCheckList = autoCheckManager.getEnabledAutoCheck()
            
            // Tra cứu từng biển số
            autoCheckList.forEach { autoCheck ->
                try {
                    // Gọi API tra cứu qua repository
                    val result = repository.checkPhatNguoi(
                        plate = autoCheck.bienSo,
                        vehicleType = autoCheck.loaiXe
                    )
                    
                    val coViPham = result.viPham == true && !result.error
                    val soLoi = result.soLoiViPham ?: 0
                    val daXuPhat = result.soDaXuPhat ?: 0
                    val chuaXuPhat = result.soChuaXuPhat ?: 0
                    
                    // Lưu vào lịch sử
                    val historyItem = com.tuhoc.phatnguoi.data.local.TraCuuHistoryItem(
                        bienSo = autoCheck.bienSo,
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
                            bienSo = autoCheck.bienSo,
                            loaiXe = autoCheck.loaiXe,
                            coViPham = coViPham,
                            soLoi = if (coViPham) soLoi else null,
                            daXuPhat = daXuPhat,
                            chuaXuPhat = chuaXuPhat
                        )
                    }
                } catch (e: Exception) {
                    // Log lỗi nhưng tiếp tục với biển số tiếp theo
                    android.util.Log.e("AutoCheckService", "Lỗi tra cứu ${autoCheck.bienSo}: ${e.message}")
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

