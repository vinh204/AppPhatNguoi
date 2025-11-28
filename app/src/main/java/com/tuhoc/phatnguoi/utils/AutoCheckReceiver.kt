package com.tuhoc.phatnguoi.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver để nhận alarm và thực hiện tự động tra cứu
 */
class AutoCheckReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "com.tuhoc.phatnguoi.AUTO_CHECK_ACTION") {
            // Chạy tra cứu tự động
            val autoCheckService = AutoCheckService(context)
            CoroutineScope(Dispatchers.IO).launch {
                autoCheckService.performAutoCheck()
            }
            
            // Schedule alarm tiếp theo
            val scheduler = AutoCheckScheduler(context)
            scheduler.scheduleNextAutoCheck()
        }
    }
}



