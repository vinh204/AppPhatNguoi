package com.tuhoc.phatnguoi.data.local

import android.content.Context
import android.util.Log
import com.google.firebase.Timestamp
import com.tuhoc.phatnguoi.data.firebase.FirebaseHistoryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "HistoryManager"

data class TraCuuHistoryItem(
    val bienSo: String,
    val loaiXe: String,
    val thoiGian: Long,
    val coViPham: Boolean,
    val soLoi: Int? = null,
    val documentId: String? = null // Document ID trong Firestore để delete
) {
    /**
     * Convert từ Map<String, Any> (từ Firestore) sang TraCuuHistoryItem
     */
    companion object {
        fun fromMap(data: Map<String, Any>, documentId: String? = null): TraCuuHistoryItem {
            val timestamp = data["thoiGian"] as? Timestamp
            val thoiGian = timestamp?.toDate()?.time ?: System.currentTimeMillis()
            
            return TraCuuHistoryItem(
                bienSo = data["bienSo"] as? String ?: "",
                loaiXe = data["loaiXe"] as? String ?: "",
                thoiGian = thoiGian,
                coViPham = (data["coViPham"] as? Boolean) ?: false,
                soLoi = (data["soLoi"] as? Long)?.toInt() ?: (data["soLoi"] as? Int),
                documentId = documentId
            )
        }
    }
}

class HistoryManager(context: Context) {
    private val authManager = AuthManager(context)
    private val firebaseHistoryService = FirebaseHistoryService()
    
    /**
     * Thêm hoặc cập nhật lịch sử tra cứu cho user hiện tại đang đăng nhập
     * Nếu biển số đã có, cập nhật bản ghi cũ thay vì tạo mới
     */
    suspend fun addHistory(item: TraCuuHistoryItem) {
        withContext(Dispatchers.IO) {
            try {
                val currentUser = authManager.getCurrentUser()
                if (currentUser == null) {
                    Log.w(TAG, "⚠️ Không có user đăng nhập, không thể lưu lịch sử")
                    return@withContext
                }
                
                val phoneNumber = currentUser.phoneNumber
                Log.d(TAG, "💾 Đang lưu lịch sử: bienSo=${item.bienSo}, loaiXe=${item.loaiXe}, userId=$phoneNumber")
                
                // Lưu vào Firestore
                firebaseHistoryService.saveHistory(
                    userId = phoneNumber,
                    bienSo = item.bienSo,
                    loaiXe = item.loaiXe,
                    coViPham = item.coViPham,
                    soLoi = item.soLoi
                ).fold(
                    onSuccess = { historyId ->
                        Log.d(TAG, "✅ Đã lưu lịch sử lên Firestore: $historyId")
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Không thể lưu lịch sử lên Firestore: ${error.message}", error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception khi lưu lịch sử: ${e.message}", e)
            }
        }
    }
    
    /**
     * Lấy lịch sử tra cứu của user hiện tại đang đăng nhập từ Firestore
     */
    suspend fun getHistory(): List<TraCuuHistoryItem> {
        return withContext(Dispatchers.IO) {
            try {
                val currentUser = authManager.getCurrentUser()
                if (currentUser == null) {
                    Log.w(TAG, "⚠️ Không có user đăng nhập, không thể lấy lịch sử")
                    return@withContext emptyList()
                }
                
                val phoneNumber = currentUser.phoneNumber
                Log.d(TAG, "🔍 Đang lấy lịch sử cho user: $phoneNumber")
                
                val result = firebaseHistoryService.getHistoryByUser(phoneNumber, limit = 100)
                
                result.fold(
                    onSuccess = { historyList ->
                        Log.d(TAG, "✅ Lấy được ${historyList.size} lịch sử từ Firestore")
                        val items = historyList.mapIndexed { index, data ->
                            try {
                                // Lấy document ID từ data (đã được thêm vào trong FirebaseRepository)
                                val documentId = data["_documentId"] as? String
                                TraCuuHistoryItem.fromMap(data, documentId = documentId)
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Lỗi khi convert history item: ${e.message}", e)
                                null
                            }
                        }.filterNotNull()
                        Log.d(TAG, "✅ Convert thành công ${items.size} items")
                        items
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Lỗi khi lấy lịch sử từ Firestore: ${error.message}", error)
                        emptyList()
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception khi lấy lịch sử: ${e.message}", e)
                emptyList()
            }
        }
    }
    
    /**
     * Xóa một item lịch sử cụ thể theo document ID
     */
    suspend fun deleteHistoryItem(documentId: String) {
        withContext(Dispatchers.IO) {
            firebaseHistoryService.deleteHistory(documentId).fold(
                onSuccess = {
                    Log.d(TAG, "✅ Đã xóa lịch sử: $documentId")
                },
                onFailure = { error ->
                    Log.w(TAG, "⚠️ Không thể xóa lịch sử: ${error.message}")
                }
            )
        }
    }
    
    // Synchronous version for compatibility
    fun getHistorySync(): List<TraCuuHistoryItem> {
        return try {
            kotlinx.coroutines.runBlocking {
                getHistory()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}

