package com.tuhoc.phatnguoi.data.remote

data class PhatNguoiResult(
    val error: Boolean = false,
    val viPham: Boolean = false,
    val message: String? = null,
    val bienSo: String? = null,
    val mauBien: String? = null,
    val loaiPhuongTien: String? = null,
    val thoiGianViPham: String? = null,
    val diaDiemViPham: String? = null,
    val hanhViViPham: String? = null,
    val trangThai: String? = null,
    val donViPhatHien: String? = null,
    val noiGiaiQuyet: List<String> = emptyList(),
    val soLoiViPham: Int? = null,
    val soDaXuPhat: Int? = null,
    val soChuaXuPhat: Int? = null,
    val allViolations: List<Map<String, Any>> = emptyList() // Danh sách tất cả vi phạm
)



