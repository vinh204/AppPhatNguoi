package com.tuhoc.phatnguoi.data.firebase

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * FirebaseHistoryService - Service để quản lý lịch sử tra cứu trong Firestore
 */
class FirebaseHistoryService {
    private val repository = FirebaseRepository()
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "FirebaseHistoryService"

    /**
     * Lưu lịch sử tra cứu
     * Nếu biển số đã có trong lịch sử, cập nhật bản ghi cũ thay vì tạo mới
     */
    suspend fun saveHistory(
        userId: String,
        bienSo: String,
        loaiXe: String,
        coViPham: Boolean,
        soLoi: Int? = null
    ): Result<String> {
        return try {
            Log.d(TAG, "💾 Đang lưu/cập nhật lịch sử: bienSo=$bienSo, userId=$userId")
            
            val snapshot = db.collection("history")
                .whereEqualTo("userId", userId)
                .whereEqualTo("bienSo", bienSo)
                .limit(1)
                .get()
                .await()
            
            val data = mapOf(
                "userId" to userId,
                "bienSo" to bienSo,
                "loaiXe" to loaiXe,
                "thoiGian" to Timestamp.now(),
                "coViPham" to coViPham,
                "soLoi" to (soLoi ?: 0),
                "updatedAt" to Timestamp.now()
            )
            
            if (snapshot.documents.isNotEmpty()) {
                val documentId = snapshot.documents[0].id
                Log.d(TAG, "🔄 Cập nhật lịch sử cũ: documentId=$documentId")
                
                repository.updateDocument("history", documentId, data).fold(
                    onSuccess = { 
                        Log.d(TAG, "✅ Đã cập nhật lịch sử: $documentId")
                        Result.success(documentId) 
                    },
                    onFailure = { error ->
                        Log.e(TAG, "❌ Lỗi khi cập nhật: ${error.message}", error)
                        Result.failure(error)
                    }
                )
            } else {
                Log.d(TAG, "➕ Tạo lịch sử mới")
                val newData = data + ("createdAt" to Timestamp.now())
                repository.saveDocument("history", newData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Exception khi lưu lịch sử: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Lấy lịch sử của một user
     */
    suspend fun getHistoryByUser(
        userId: String,
        limit: Int = 50
    ): Result<List<Map<String, Any>>> {
        return repository.getHistoryByUser(userId, limit)
    }

    /**
     * Xóa một lịch sử
     */
    suspend fun deleteHistory(historyId: String): Result<Boolean> {
        return repository.deleteDocument("history", historyId)
    }
}

