package com.tuhoc.phatnguoi.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.tuhoc.phatnguoi.data.local.NotificationSettingsManager
import com.tuhoc.phatnguoi.utils.PermissionHelper
import com.tuhoc.phatnguoi.security.SecureLogger
import java.util.Calendar

/**
 * Scheduler để schedule alarm cho tự động tra cứu
 */
class AutoCheckScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val settingsManager = NotificationSettingsManager(context)
    
    companion object {
        private const val REQUEST_CODE = 1001
        private const val ACTION_AUTO_CHECK = "com.tuhoc.phatnguoi.AUTO_CHECK_ACTION"
    }
    
    /**
     * Schedule alarm cho lần tra cứu tiếp theo dựa vào cài đặt
     */
    fun scheduleNextAutoCheck() {
        // Kiểm tra permission trước
        if (!PermissionHelper.hasScheduleExactAlarmPermission(context)) {
            SecureLogger.w("No SCHEDULE_EXACT_ALARM permission")
            return
        }
        
        val frequency = settingsManager.getFrequency()
        val hour = settingsManager.getHour()
        val minute = settingsManager.getMinute()
        
        SecureLogger.d("Scheduling: frequency=$frequency, time=$hour:$minute")
        
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        // Nếu thời gian đã qua trong ngày hôm nay, set cho ngày mai
        val now = Calendar.getInstance()
        if (calendar.before(now)) {
            calendar.add(Calendar.DAY_OF_MONTH, 1)
        }
        
        // Điều chỉnh theo tần suất
        when (frequency) {
            "Hàng tuần" -> {
                // Set cho Chủ nhật tiếp theo
                while (calendar.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
                    calendar.add(Calendar.DAY_OF_MONTH, 1)
                }
            }
            "Hàng tháng" -> {
                // Set cho ngày 1 của tháng tiếp theo
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.add(Calendar.MONTH, 1)
            }
            // "Hàng ngày" -> giữ nguyên (đã set ngày mai nếu thời gian đã qua)
        }
        
        // Tạo Intent
        val intent = Intent(context, AutoCheckReceiver::class.java).apply {
            action = ACTION_AUTO_CHECK
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Schedule alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
        
        SecureLogger.d("Scheduled auto check for: ${calendar.time}")
    }
    
    /**
     * Cancel alarm hiện tại
     */
    fun cancelAutoCheck() {
        val intent = Intent(context, AutoCheckReceiver::class.java).apply {
            action = ACTION_AUTO_CHECK
        }
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        alarmManager.cancel(pendingIntent)
        SecureLogger.d("Cancelled auto check")
    }
}

