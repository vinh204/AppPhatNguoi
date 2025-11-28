package com.tuhoc.phatnguoi.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver để nhận sự kiện boot completed và schedule lại alarm
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || 
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            // Schedule lại alarm sau khi khởi động lại
            val scheduler = AutoCheckScheduler(context)
            scheduler.scheduleNextAutoCheck()
        }
    }
}



