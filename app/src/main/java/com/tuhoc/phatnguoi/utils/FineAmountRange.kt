package com.tuhoc.phatnguoi.utils

/**
 * Khoảng số tiền phạt (từ min đến max)
 * 
 * Data class này đại diện cho khoảng số tiền phạt từ mức tối thiểu đến mức tối đa.
 * Được sử dụng để hiển thị số tiền phạt ước tính từ AI.
 * 
 * @param min Số tiền phạt tối thiểu (VNĐ)
 * @param max Số tiền phạt tối đa (VNĐ)
 * 
 * @property average Số tiền phạt trung bình (để hiển thị ước tính)
 * @property isValid Kiểm tra xem có giá trị hợp lệ không (min > 0 && max >= min)
 * 
 * @sample
 * ```
 * val range = FineAmountRange(1000000, 2000000)
 * println(range.average) // 1500000
 * println(range.formatRange()) // "1.000.000 VNĐ - 2.000.000 VNĐ"
 * ```
 */
data class FineAmountRange(
    val min: Long,
    val max: Long
) {
    /**
     * Trả về số tiền trung bình (để hiển thị ước tính)
     */
    val average: Long
        get() = (min + max) / 2
    
    /**
     * Kiểm tra xem có giá trị hợp lệ không
     */
    val isValid: Boolean
        get() = min > 0 && max >= min
    
    /**
     * Format hiển thị khoảng số tiền
     * 
     * @return Chuỗi đã format, ví dụ: "1.000.000 VNĐ - 2.000.000 VNĐ" hoặc "1.000.000 VNĐ" nếu min == max
     */
    fun formatRange(): String {
        return if (isValid) {
            if (min == max) {
                formatMoney(min)
            } else {
                "${formatMoney(min)} - ${formatMoney(max)}"
            }
        } else {
            "0 VNĐ"
        }
    }
    
    /**
     * Format số tiền đơn lẻ
     */
    private fun formatMoney(amount: Long): String {
        val formatted = String.format("%,d", amount).replace(",", ".")
        return "$formatted VNĐ"
    }
    
    companion object {
        /**
         * Tạo range từ một số tiền cố định (min = max = amount)
         */
        fun fromSingleAmount(amount: Long): FineAmountRange {
            return FineAmountRange(amount, amount)
        }
        
        /**
         * Tạo range rỗng (0, 0)
         */
        fun empty(): FineAmountRange {
            return FineAmountRange(0, 0)
        }
    }
}

/**
 * Chi tiết mức phạt cho một vi phạm (bao gồm hình phạt bổ sung)
 * 
 * Data class này chứa thông tin chi tiết về mức phạt cho một vi phạm giao thông,
 * bao gồm mô tả, mức phạt tiền, hình phạt bổ sung và ghi chú.
 * 
 * @param fineRange Khoảng số tiền phạt (min-max)
 * @param moTa Mô tả về mức phạt, bao gồm loại phương tiện, hành vi vi phạm, nghị định áp dụng
 * @param mucPhatTien Mức phạt tiền với tham chiếu chính xác (ví dụ: "Từ 18.000.000 đồng đến 20.000.000 đồng (theo Khoản 9 Điều 6 Nghị định 168/2024/NĐ-CP)")
 * @param hinhPhatBoSung Hình phạt bổ sung với tham chiếu chính xác (ví dụ: "Bị trừ 04 điểm trên Giấy phép lái xe")
 * @param ghiChu Ghi chú về trường hợp đặc biệt (ví dụ: "Nếu gây tai nạn giao thông: ...")
 * @param nghidinh [Deprecated] Sử dụng moTa thay thế
 * @param dieu [Deprecated] Sử dụng mucPhatTien thay thế
 */
data class FineDetails(
    val fineRange: FineAmountRange,
    val moTa: String = "",
    val mucPhatTien: String = "",
    val hinhPhatBoSung: String = "",
    val ghiChu: String = "",
    // Backward compatibility - deprecated fields
    @Deprecated("Sử dụng moTa thay thế")
    val nghidinh: String = "",
    @Deprecated("Sử dụng mucPhatTien thay thế")
    val dieu: String = ""
)