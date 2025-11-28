package com.tuhoc.phatnguoi.data.local

import android.content.Context
import android.util.Log
import com.tuhoc.phatnguoi.data.firebase.FirebaseAutoCheckService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "AutoCheckManager"

/**
 * Quản lý danh sách biển số cần tự động tra cứu
 * Dữ liệu được lưu trong Firestore
 */
class AutoCheckManager(context: Context) {
    private val authManager = AuthManager(context)
    private val firebaseAutoCheckService = FirebaseAutoCheckService()
    
    /**
     * Thêm hoặc cập nhật biển số vào danh sách tự động tra cứu
     * Nếu biển số đã có, cập nhật bản ghi cũ thay vì tạo mới
     * @param bienSo Biển số cần tra cứu
     * @param loaiXe Loại xe: 1 = Ô tô, 2 = Xe máy, 3 = Xe máy điện
     * @param enabled Trạng thái bật/tắt
     */
    suspend fun addOrUpdateAutoCheck(bienSo: String, loaiXe: Int, enabled: Boolean) {
        withContext(Dispatchers.IO) {
            try {
                val currentUser = authManager.getCurrentUser()
                if (currentUser == null) {
                    Log.w(TAG, "⚠️ Không có user đăng nhập, không thể lưu auto check")
                    return@withContext
                }
                
                val phoneNumber = currentUser.phoneNumber
                Log.d(TAG, "💾 Đang lưu/cập nhật auto check: bienSo=$bienSo, loaiXe=$loaiXe, enabled=$enabled")
                firebaseAutoCheckService.addAutoCheck(phoneNumber, bienSo, loaiXe, enabled)
                    .fold(
                        onSuccess = { autoCheckId ->
                            Log.d(TAG, "✅ Đã lưu/cập nhật auto check lên Firestore: $autoCheckId")
                        },
                        onFailure = { error ->
                            Log.e(TAG, "❌ Không thể lưu auto check lên Firestore: ${error.message}", error)
                        }
                    )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception khi lưu auto check: ${e.message}", e)
            }
        }
    }
    
    /**
     * Xóa biển số khỏi danh sách tự động tra cứu
     * Xóa khỏi Firestore
     */
    suspend fun removeAutoCheck(bienSo: String) {
        withContext(Dispatchers.IO) {
            try {
                val currentUser = authManager.getCurrentUser()
                if (currentUser == null) {
                    Log.w(TAG, "⚠️ Không có user đăng nhập, không thể xóa auto check")
                    return@withContext
                }
                
                val phoneNumber = currentUser.phoneNumber
                
                // Tìm document ID
                val autoChecks = firebaseAutoCheckService.getAutoChecksByUser(phoneNumber)
                    .getOrNull() ?: emptyList()
                
                val autoCheck = autoChecks.find { it["bienSo"] == bienSo }
                val documentId = autoCheck?.get("_documentId") as? String
                
                if (documentId != null) {
                    firebaseAutoCheckService.deleteAutoCheck(documentId).fold(
                        onSuccess = {
                            Log.d(TAG, "✅ Đã xóa auto check: $documentId")
                        },
                        onFailure = { error ->
                            Log.e(TAG, "❌ Không thể xóa auto check: ${error.message}", error)
                        }
                    )
                } else {
                    Log.w(TAG, "⚠️ Không tìm thấy document ID để xóa auto check: $bienSo")
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Exception khi xóa auto check: ${e.message}", e)
            }
        }
    }
    
    /**
     * Lấy tất cả biển số cần tự động tra cứu của user hiện tại từ Firestore
     */
    suspend fun getAllAutoCheck(): List<AutoCheck> {
        return withContext(Dispatchers.IO) {
            val currentUser = authManager.getCurrentUser()
            if (currentUser == null) {
                return@withContext emptyList()
            }
            
            val phoneNumber = currentUser.phoneNumber
            
            firebaseAutoCheckService.getAutoChecksByUser(phoneNumber)
                .getOrNull()
                ?.map { 
                    val documentId = it.get("_documentId") as? String
                    AutoCheck.fromMap(it, documentId = documentId)
                } ?: emptyList()
        }
    }
    
    /**
     * Lấy tất cả biển số đang bật tự động tra cứu từ Firestore
     */
    suspend fun getEnabledAutoCheck(): List<AutoCheck> {
        return withContext(Dispatchers.IO) {
            val currentUser = authManager.getCurrentUser()
            if (currentUser == null) {
                return@withContext emptyList()
            }
            
            val phoneNumber = currentUser.phoneNumber
            
            firebaseAutoCheckService.getEnabledAutoChecksByUser(phoneNumber)
                .getOrNull()
                ?.map { 
                    val documentId = it.get("_documentId") as? String
                    AutoCheck.fromMap(it, documentId = documentId)
                } ?: emptyList()
        }
    }
    
    /**
     * Kiểm tra biển số có đang bật tự động tra cứu không
     */
    suspend fun isAutoCheckEnabled(bienSo: String): Boolean {
        return withContext(Dispatchers.IO) {
            val currentUser = authManager.getCurrentUser()
            if (currentUser == null) {
                return@withContext false
            }
            
            val phoneNumber = currentUser.phoneNumber
            val autoChecks = firebaseAutoCheckService.getEnabledAutoChecksByUser(phoneNumber)
                .getOrNull() ?: emptyList()
            
            autoChecks.any { it["bienSo"] == bienSo }
        }
    }
}
