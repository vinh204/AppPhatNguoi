package com.tuhoc.phatnguoi.data.firebase

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * FirebaseDatabaseService - Service để khởi tạo và quản lý cấu trúc database trong Firestore
 * 
 * Cấu trúc database:
 * 
 * users/
 *   {userId}/
 *     phoneNumber: String
 *     password: String? (nullable)
 *     isLoggedIn: Boolean
 *     createdAt: Timestamp
 *     updatedAt: Timestamp
 * 
 * history/
 *   {historyId}/
 *     userId: String
 *     bienSo: String
 *     loaiXe: String
 *     thoiGian: Timestamp
 *     coViPham: Boolean
 *     soLoi: Int
 *     createdAt: Timestamp
 * 
 * auto_check/
 *   {autoCheckId}/
 *     userId: String
 *     bienSo: String
 *     loaiXe: Int (1 = Ô tô, 2 = Xe máy, 3 = Xe máy điện)
 *     enabled: Boolean
 *     createdAt: Timestamp
 *     updatedAt: Timestamp
 */
class FirebaseDatabaseService {
    private val db = FirebaseFirestore.getInstance()
    private val repository = FirebaseRepository()
    private val TAG = "FirebaseDatabaseService"

    /**
     * Khởi tạo cấu trúc database với dữ liệu mẫu
     * Gọi hàm này một lần để tạo collections và documents mẫu
     */
    suspend fun initializeDatabase(): Result<Boolean> {
        return try {
            // Tạo users collection với dữ liệu mẫu
            createSampleUsers()
            
            // Tạo history collection với dữ liệu mẫu
            createSampleHistory()
            
            // Tạo auto_check collection với dữ liệu mẫu
            createSampleAutoCheck()
            
            Log.d(TAG, "Khởi tạo database thành công!")
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi khởi tạo database: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Tạo users collection với dữ liệu mẫu
     */
    private suspend fun createSampleUsers() {
        val sampleUsers = listOf(
            mapOf(
                "phoneNumber" to "0912345678",
                "password" to "1234",
                "isLoggedIn" to false,
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            ),
            mapOf(
                "phoneNumber" to "0987654321",
                "password" to "5678",
                "isLoggedIn" to false,
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )
        )

        for (userData in sampleUsers) {
            val phoneNumber = userData["phoneNumber"] as String
            repository.saveDocument("users", userData, phoneNumber)
        }
        
        Log.d(TAG, "Đã tạo ${sampleUsers.size} users mẫu")
    }

    /**
     * Tạo history collection với dữ liệu mẫu
     */
    private suspend fun createSampleHistory() {
        val sampleHistory = listOf(
            mapOf(
                "userId" to "0912345678",
                "bienSo" to "30A-12345",
                "loaiXe" to "Xe máy",
                "thoiGian" to Timestamp.now(),
                "coViPham" to true,
                "soLoi" to 2,
                "createdAt" to Timestamp.now()
            ),
            mapOf(
                "userId" to "0912345678",
                "bienSo" to "30B-67890",
                "loaiXe" to "Ô tô",
                "thoiGian" to Timestamp.now(),
                "coViPham" to false,
                "soLoi" to 0,
                "createdAt" to Timestamp.now()
            ),
            mapOf(
                "userId" to "0987654321",
                "bienSo" to "29A-11111",
                "loaiXe" to "Xe máy",
                "thoiGian" to Timestamp.now(),
                "coViPham" to true,
                "soLoi" to 1,
                "createdAt" to Timestamp.now()
            )
        )

        for (historyData in sampleHistory) {
            repository.saveDocument("history", historyData)
        }
        
        Log.d(TAG, "Đã tạo ${sampleHistory.size} lịch sử mẫu")
    }

    /**
     * Tạo auto_check collection với dữ liệu mẫu
     */
    private suspend fun createSampleAutoCheck() {
        val sampleAutoCheck = listOf(
            mapOf(
                "userId" to "0912345678",
                "bienSo" to "30A-12345",
                "loaiXe" to 2, // Xe máy
                "enabled" to true,
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            ),
            mapOf(
                "userId" to "0912345678",
                "bienSo" to "30B-67890",
                "loaiXe" to 1, // Ô tô
                "enabled" to true,
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            ),
            mapOf(
                "userId" to "0987654321",
                "bienSo" to "29A-11111",
                "loaiXe" to 2, // Xe máy
                "enabled" to false,
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )
        )

        for (autoCheckData in sampleAutoCheck) {
            repository.saveDocument("auto_check", autoCheckData)
        }
        
        Log.d(TAG, "Đã tạo ${sampleAutoCheck.size} auto_check mẫu")
    }

    /**
     * Kiểm tra xem collection đã tồn tại chưa
     */
    suspend fun collectionExists(collectionName: String): Boolean {
        return try {
            val snapshot = db.collection(collectionName).limit(1).get().await()
            snapshot.documents.isNotEmpty() || true // Collection tồn tại nếu có thể query
        } catch (e: Exception) {
            false
        }
    }

}

