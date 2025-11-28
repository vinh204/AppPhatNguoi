package com.tuhoc.phatnguoi.utils

/**
 * Utility functions cho xử lý vi phạm giao thông
 * 
 * Object này cung cấp các hàm tiện ích để xử lý dữ liệu vi phạm giao thông,
 * bao gồm lọc các vi phạm chưa xử phạt.
 */
object ViolationUtils {
    /**
     * Lọc các vi phạm chưa xử phạt và thêm index gốc vào mỗi violation
     * 
     * Hàm này sẽ:
     * 1. Kiểm tra trạng thái của mỗi vi phạm
     * 2. Lọc các vi phạm có trạng thái chứa "chưa xử phạt", "chưa xử", "chưa nộp", hoặc "chưa thanh toán"
     * 3. Thêm field "_originalIndex" vào mỗi violation để map lại sau này
     * 
     * @param violations Danh sách vi phạm cần lọc (Map với key "Trạng thái")
     * @return Danh sách vi phạm chưa xử phạt với _originalIndex được thêm vào
     * 
     * @sample
     * ```
     * val violations = listOf(
     *     mapOf("Trạng thái" to "Chưa xử phạt", "Hành vi vi phạm" to "Vượt đèn đỏ"),
     *     mapOf("Trạng thái" to "Đã xử phạt", "Hành vi vi phạm" to "Không đội mũ bảo hiểm")
     * )
     * val unresolved = ViolationUtils.filterUnresolvedViolations(violations)
     * // Kết quả: chỉ có vi phạm đầu tiên, với _originalIndex = 0
     * ```
     */
    fun filterUnresolvedViolations(violations: List<Map<String, Any>>): List<Map<String, Any>> {
        return violations.mapIndexedNotNull { index, violation ->
            val trangThai = violation["Trạng thái"]?.toString()?.lowercase() ?: ""
            if (trangThai.contains("chưa xử phạt") || 
                trangThai.contains("chưa xử") || 
                trangThai.contains("chưa nộp") ||
                trangThai.contains("chưa thanh toán")) {
                // Thêm index gốc vào violation để map lại sau này
                violation.toMutableMap().apply {
                    put("_originalIndex", index)
                }
            } else {
                null
            }
        }
    }
}

