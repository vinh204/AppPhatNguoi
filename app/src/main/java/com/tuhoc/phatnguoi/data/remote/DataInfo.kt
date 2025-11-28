package com.tuhoc.phatnguoi.data.remote

import com.tuhoc.phatnguoi.utils.FineAmountRange

data class DataInfo(
    val total: Int,
    val chuaxuphat: Int,
    val daxuphat: Int,
    val latest: String? = null,
    val tongTienPhat: Long = 0L,  // Tổng tiền phạt (backward compatibility - dùng average)
    val tongTienPhatRange: FineAmountRange? = null  // Khoảng số tiền phạt (min-max)
) {
    /**
     * Lấy khoảng số tiền phạt, fallback về average nếu không có range
     */
    fun getFineRange(): FineAmountRange {
        return tongTienPhatRange ?: FineAmountRange.fromSingleAmount(tongTienPhat)
    }
}








