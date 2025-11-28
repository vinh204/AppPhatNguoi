package com.tuhoc.phatnguoi.data.local

/**
 * AutoCheck data class - Thay thế AutoCheckEntity từ Room Database
 * Dữ liệu được lưu trong Firestore
 */
data class AutoCheck(
    val bienSo: String,
    val loaiXe: Int, // 1 = Ô tô, 2 = Xe máy, 3 = Xe máy điện
    val enabled: Boolean = true,
    val documentId: String? = null // Document ID trong Firestore để update/delete
) {
    /**
     * Convert từ Map<String, Any> (từ Firestore) sang AutoCheck
     */
    companion object {
        fun fromMap(data: Map<String, Any>, documentId: String? = null): AutoCheck {
            return AutoCheck(
                bienSo = data["bienSo"] as? String ?: "",
                loaiXe = (data["loaiXe"] as? Long)?.toInt() ?: (data["loaiXe"] as? Int) ?: 1,
                enabled = (data["enabled"] as? Boolean) ?: true,
                documentId = documentId
            )
        }
    }
    
    /**
     * Convert sang Map<String, Any> để lưu vào Firestore
     */
    fun toMap(): Map<String, Any> {
        return mapOf(
            "bienSo" to bienSo,
            "loaiXe" to loaiXe,
            "enabled" to enabled
        )
    }
}

