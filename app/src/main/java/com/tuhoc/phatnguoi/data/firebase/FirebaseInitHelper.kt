package com.tuhoc.phatnguoi.data.firebase

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * FirebaseInitHelper - Helper để khởi tạo database Firebase
 * Tự động được gọi khi app khởi động trong MainActivity
 */
object FirebaseInitHelper {
    private const val TAG = "FirebaseInitHelper"
    private val databaseService = FirebaseDatabaseService()

    /**
     * Khởi tạo database với dữ liệu mẫu
     * Chỉ nên gọi một lần khi setup lần đầu
     */
    fun initDatabase(context: Context, onComplete: (Boolean, String?) -> Unit = { _, _ -> }) {
        CoroutineScope(Dispatchers.IO).launch {
            databaseService.initializeDatabase().fold(
                onSuccess = {
                    Log.d(TAG, "Khởi tạo Firebase database thành công!")
                    onComplete(true, null)
                },
                onFailure = { error ->
                    Log.e(TAG, "Lỗi khi khởi tạo database: ${error.message}", error)
                    onComplete(false, error.message)
                }
            )
        }
    }

    /**
     * Kiểm tra xem database đã được khởi tạo chưa
     */
    fun checkDatabaseInitialized(
        onResult: (Boolean) -> Unit = {}
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val usersExists = databaseService.collectionExists("users")
            val historyExists = databaseService.collectionExists("history")
            val autoCheckExists = databaseService.collectionExists("auto_check")
            
            val isInitialized = usersExists && historyExists && autoCheckExists
            onResult(isInitialized)
        }
    }
}