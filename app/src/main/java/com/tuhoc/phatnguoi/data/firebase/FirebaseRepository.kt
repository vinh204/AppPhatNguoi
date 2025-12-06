package com.tuhoc.phatnguoi.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * FirebaseRepository - Quản lý việc lưu và đọc data từ Firestore
 * 
 * Firestore là NoSQL database của Firebase, lưu data dưới dạng collections và documents
 * 
 * Cấu trúc:
 * - Collection: Tương đương với table trong SQL
 * - Document: Tương đương với row trong SQL
 * - Field: Tương đương với column trong SQL
 * 
 * Ví dụ cấu trúc:
 * users/
 *   {userId}/
 *     name: "Nguyễn Văn A"
 *     email: "nguyenvana@example.com"
 *     createdAt: Timestamp
 * 
 * history/
 *   {historyId}/
 *     userId: "user123"
 *     bienSo: "30A-12345"
 *     loaiXe: "Xe máy"
 *     thoiGian: Timestamp
 *     coViPham: true
 *     soLoi: 2
 */
class FirebaseRepository {
    private val db = FirebaseFirestore.getInstance()

    /**
     * Lưu một document vào collection
     * @param collection Tên collection (ví dụ: "users", "history")
     * @param data Map chứa dữ liệu cần lưu
     * @param documentId ID của document (nếu null, Firestore sẽ tự tạo ID)
     * @return ID của document đã lưu
     */
    suspend fun saveDocument(
        collection: String,
        data: Map<String, Any>,
        documentId: String? = null
    ): Result<String> {
        return try {
            val docRef = if (documentId != null) {
                db.collection(collection).document(documentId)
            } else {
                db.collection(collection).document()
            }
            
            docRef.set(data).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Đọc một document theo ID
     * @param collection Tên collection
     * @param documentId ID của document
     * @return Map chứa dữ liệu hoặc null nếu không tìm thấy
     */
    suspend fun getDocument(
        collection: String,
        documentId: String
    ): Result<Map<String, Any>?> {
        return try {
            val document = db.collection(collection).document(documentId).get().await()
            if (document.exists()) {
                Result.success(document.data)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Đọc tất cả documents trong một collection
     * @param collection Tên collection
     * @return List các Map chứa dữ liệu
     */
    suspend fun getAllDocuments(collection: String): Result<List<Map<String, Any>>> {
        return try {
            val snapshot = db.collection(collection).get().await()
            val documents = snapshot.documents.map { it.data ?: emptyMap() }
            Result.success(documents)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Đọc documents với điều kiện
     * @param collection Tên collection
     * @param field Tên field để filter
     * @param value Giá trị cần tìm
     * @return List các Map chứa dữ liệu
     */
    suspend fun getDocumentsWhere(
        collection: String,
        field: String,
        value: Any
    ): Result<List<Map<String, Any>>> {
        return try {
            val snapshot = db.collection(collection)
                .whereEqualTo(field, value)
                .get()
                .await()
            val documents = snapshot.documents.mapNotNull { documentSnapshot ->
                try {
                    val data = documentSnapshot.data?.toMutableMap() ?: mutableMapOf()
                    // Thêm document ID vào data để có thể update/delete sau
                    data["_documentId"] = documentSnapshot.id
                    data
                } catch (e: Exception) {
                    null
                }
            }
            Result.success(documents)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Đọc documents với sắp xếp và giới hạn
     * @param collection Tên collection
     * @param orderBy Field để sắp xếp
     * @param limit Số lượng documents tối đa
     * @param descending Sắp xếp giảm dần (true) hay tăng dần (false)
     * @return List các Map chứa dữ liệu
     */
    suspend fun getDocumentsOrdered(
        collection: String,
        orderBy: String,
        limit: Int = 10,
        descending: Boolean = true
    ): Result<List<Map<String, Any>>> {
        return try {
            val query = db.collection(collection)
                .orderBy(orderBy, if (descending) Query.Direction.DESCENDING else Query.Direction.ASCENDING)
                .limit(limit.toLong())
                .get()
                .await()
            val documents = query.documents.map { it.data ?: emptyMap() }
            Result.success(documents)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cập nhật một document
     * @param collection Tên collection
     * @param documentId ID của document
     * @param data Map chứa các field cần cập nhật
     * @return true nếu thành công
     */
    suspend fun updateDocument(
        collection: String,
        documentId: String,
        data: Map<String, Any>
    ): Result<Boolean> {
        return try {
            db.collection(collection).document(documentId).update(data).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Xóa một document
     * @param collection Tên collection
     * @param documentId ID của document
     * @return true nếu thành công
     */
    suspend fun deleteDocument(
        collection: String,
        documentId: String
    ): Result<Boolean> {
        return try {
            db.collection(collection).document(documentId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Lưu lịch sử tra cứu vào Firestore
     * @param userId ID của user
     * @param bienSo Biển số xe
     * @param loaiXe Loại xe
     * @param coViPham Có vi phạm hay không
     * @param soLoi Số lỗi vi phạm
     * @return ID của document đã lưu
     */
    suspend fun saveHistory(
        userId: String,
        bienSo: String,
        loaiXe: String,
        coViPham: Boolean,
        soLoi: Int? = null
    ): Result<String> {
        val data = mapOf(
            "userId" to userId,
            "bienSo" to bienSo,
            "loaiXe" to loaiXe,
            "thoiGian" to com.google.firebase.Timestamp.now(),
            "coViPham" to coViPham,
            "soLoi" to (soLoi ?: 0)
        )
        return saveDocument("history", data)
    }

    /**
     * Lấy lịch sử tra cứu của một user
     * @param userId ID của user
     * @param limit Số lượng kết quả tối đa
     * @return List các Map chứa lịch sử
     */
    suspend fun getHistoryByUser(
        userId: String,
        limit: Int = 50
    ): Result<List<Map<String, Any>>> {
        return try {
            android.util.Log.d("FirebaseRepository", "Querying history for userId: $userId, limit: $limit")
            
            // Thử query với orderBy trước (cần index)
            try {
                val snapshot = db.collection("history")
                    .whereEqualTo("userId", userId)
                    .orderBy("thoiGian", Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .get()
                    .await()
                
                android.util.Log.d("FirebaseRepository", "Query thành công, tìm thấy ${snapshot.documents.size} documents")
                
                val documents = snapshot.documents.mapNotNull { documentSnapshot ->
                    try {
                        val data = documentSnapshot.data?.toMutableMap() ?: mutableMapOf()
                        // Thêm document ID vào data để có thể update sau
                        data["_documentId"] = documentSnapshot.id
                        android.util.Log.d("FirebaseRepository", "Document ID: ${documentSnapshot.id}, data: $data")
                        data
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseRepository", "Lỗi khi đọc document: ${e.message}", e)
                        null
                    }
                }
                
                android.util.Log.d("FirebaseRepository", "Trả về ${documents.size} documents")
                Result.success(documents)
            } catch (indexError: Exception) {
                // Nếu lỗi do thiếu index, thử query đơn giản hơn (không orderBy)
                if (indexError.message?.contains("index") == true || indexError.message?.contains("indexes") == true) {
                    android.util.Log.w("FirebaseRepository", "Index chưa sẵn sàng, dùng query đơn giản (không sort)")
                    
                    val snapshot = db.collection("history")
                        .whereEqualTo("userId", userId)
                        .limit(limit.toLong())
                        .get()
                        .await()
                    
                    val documents = snapshot.documents.mapNotNull { documentSnapshot ->
                        try {
                            val data = documentSnapshot.data ?: emptyMap()
                            data
                        } catch (e: Exception) {
                            null
                        }
                    }
                    
                    // Sort trong memory
                    val sorted = documents.sortedByDescending { 
                        val timestamp = it["thoiGian"] as? com.google.firebase.Timestamp
                        timestamp?.toDate()?.time ?: 0L
                    }.take(limit)
                    
                    android.util.Log.d("FirebaseRepository", "Query đơn giản thành công, trả về ${sorted.size} documents (đã sort trong memory)")
                    Result.success(sorted)
                } else {
                    throw indexError
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseRepository", "Lỗi khi query history: ${e.message}", e)
            Result.failure(e)
        }
    }
}