package com.tuhoc.phatnguoi.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.tuhoc.phatnguoi.MainActivity

/**
 * Helper class để hiển thị thông báo trong ứng dụng
 */
class NotificationHelper(private val context: Context) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    
    companion object {
        private const val CHANNEL_ID = "tra_cuu_notification_channel"
        private const val CHANNEL_NAME = "Tra cứu phạt nguội"
        private const val CHANNEL_DESCRIPTION = "Thông báo về kết quả tra cứu phạt nguội"
        const val EXTRA_BIEN_SO = "extra_bien_so"
        const val EXTRA_LOAI_XE = "extra_loai_xe"
        const val EXTRA_FROM_NOTIFICATION = "extra_from_notification"
    }
    
    init {
        createNotificationChannel()
    }
    
    /**
     * Tạo notification channel cho Android 8.0+
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH // High importance để thông báo hiển thị rõ ràng
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true) // Rung khi có thông báo
                enableLights(true) // Bật đèn LED
            }
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Hiển thị thông báo tra cứu dạng hệ thống Android
     * @param bienSo Biển số
     * @param loaiXe Loại xe: 1 = Ô tô, 2 = Xe máy, 3 = Xe đạp điện
     * @param coViPham Có vi phạm hay không
     * @param soLoi Số lỗi vi phạm (nếu có)
     * @param daNop Số vi phạm đã nộp
     * @param chuaNop Số vi phạm chưa nộp
     * @param notificationId ID của notification (để update hoặc dismiss)
     */
    fun showTraCuuNotification(
        bienSo: String,
        loaiXe: Int,
        coViPham: Boolean,
        soLoi: Int? = null,
        daXuPhat: Int = 0,
        chuaXuPhat: Int = 0,
        notificationId: Int = System.currentTimeMillis().toInt()
    ) {
        // Title: "Thông báo xe [biển số]"
        val title = "Thông báo xe $bienSo"
        
        // Content: "Vi phạm: X | Đã nộp: Y | Chưa nộp: Z"
        val content = if (coViPham && soLoi != null && soLoi > 0) {
            "Vi phạm: $soLoi | Đã xử phạt: $daXuPhat | Chưa xử phạt: $chuaXuPhat"
        } else {
            "Không có vi phạm"
        }
        
        // Intent để mở app và tự động tra cứu khi click vào notification
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_FROM_NOTIFICATION, true)
            putExtra(EXTRA_BIEN_SO, bienSo)
            putExtra(EXTRA_LOAI_XE, loaiXe)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Tạo notification với style giống Messenger
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Có thể thay bằng icon tùy chỉnh sau
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(content)) // Không set summary text để chỉ hiển thị tên app
            .setPriority(NotificationCompat.PRIORITY_HIGH) // High priority để hiển thị rõ ràng
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // Tự động đóng khi click
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true) // Hiển thị thời gian "bây giờ"
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Sử dụng default sound, vibration, lights
            .build()
        
        notificationManager.notify(notificationId, notification)
    }
}

