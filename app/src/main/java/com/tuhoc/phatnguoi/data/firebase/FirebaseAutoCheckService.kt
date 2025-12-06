package com.tuhoc.phatnguoi.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import android.util.Log

/**
 * FirebaseAutoCheckService - Service để quản lý auto check trong Firestore
 * 
 * Loại xe:
 * 1 = Ô tô
 * 2 = Xe máy
 * 3 = Xe đạp điện
 */
class FirebaseAutoCheckService {
    private val repository = FirebaseRepository()
    private val db = FirebaseFirestore.getInstance()
    private val TAG = "FirebaseAutoCheckService"

    /**
     * Thêm hoặc cập nhật biển số vào danh sách tự động tra cứu
     * Nếu biển số đã có, cập nhật bản ghi cũ thay vì tạo mới
     */
    suspend fun addAutoCheck(
        userId: String,
        bienSo: String,
        loaiXe: Int,
        enabled: Boolean = true
    ): Result<String> {
        return try {
            Log.d(TAG, "Đang lưu/cập nhật auto check: bienSo=$bienSo, userId=$userId")
            
            // Tìm document có cùng userId và bienSo để cập nhật hoặc tạo mới
            val snapshot = db.collection("auto_check")
                .whereEqualTo("userId", userId)
                .whereEqualTo("bienSo", bienSo)
                .limit(1)
                .get()
                .await()
            
            val data = mapOf(
                "userId" to userId,
                "bienSo" to bienSo,
                "loaiXe" to loaiXe,
                "enabled" to enabled,
                "updatedAt" to Timestamp.now()
            )
            
            if (snapshot.documents.isNotEmpty()) {
                val documentId = snapshot.documents[0].id
                Log.d(TAG, "Cập nhật auto check cũ: documentId=$documentId")
                
                repository.updateDocument("auto_check", documentId, data).fold(
                    onSuccess = { 
                        Log.d(TAG, "Đã cập nhật auto check: $documentId")
                        Result.success(documentId) 
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Lỗi khi cập nhật: ${error.message}", error)
                        Result.failure(error)
                    }
                )
            } else {
                Log.d(TAG, "Tạo auto check mới")
                val newData = data + ("createdAt" to Timestamp.now())
                repository.saveDocument("auto_check", newData)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception khi lưu auto check: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Lấy danh sách auto check của một user
     */
    suspend fun getAutoChecksByUser(userId: String): Result<List<Map<String, Any>>> {
        return repository.getDocumentsWhere("auto_check", "userId", userId)
    }

    /**
     * Lấy danh sách auto check đang bật của một user
     */
    suspend fun getEnabledAutoChecksByUser(userId: String): Result<List<Map<String, Any>>> {
        return try {
            val allAutoChecks = repository.getDocumentsWhere("auto_check", "userId", userId)
                .getOrNull() ?: emptyList()
            val enabled = allAutoChecks.filter { 
                (it["enabled"] as? Boolean) == true 
            }
            Result.success(enabled)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Xóa auto check
     */
    suspend fun deleteAutoCheck(autoCheckId: String): Result<Boolean> {
        return repository.deleteDocument("auto_check", autoCheckId)
    }
}

