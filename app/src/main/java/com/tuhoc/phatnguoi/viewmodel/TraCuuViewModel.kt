package com.tuhoc.phatnguoi.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tuhoc.phatnguoi.data.remote.DataInfo
import com.tuhoc.phatnguoi.data.remote.PhatNguoiRepository
import com.tuhoc.phatnguoi.data.remote.PhatNguoiResult
import com.tuhoc.phatnguoi.utils.AIFineCalculator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException


/* ===========================================================
 * UI State
 * =========================================================== */

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(
        val pairs: List<Pair<String, String>>,
        val info: DataInfo?,
        val violationsForCalculation: List<Map<String, Any>> = emptyList()
    ) : UiState()
    data class Error(val message: String) : UiState()
}

/* ===========================================================
 * ViewModel
 * =========================================================== */

class TraCuuViewModel : ViewModel() {

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state

    private val repository = PhatNguoiRepository()
    private val fineCalculator = AIFineCalculator()

    /* -----------------------------------------------------------
     * API tra cứu biển số
     * ----------------------------------------------------------- */
    fun traCuuBienSo(
        bienSoRaw: String,
        loaiXe: Int   // 1 = ô tô, 2 = xe máy, 3 = Xe đạp điện
    ) {
        // Chuẩn hoá giống UI (upper + bỏ space)
        val bienSo = bienSoRaw.uppercase().replace(" ", "")

        // 1) Không nhập
        if (bienSo.isBlank()) {
            _state.value = UiState.Error("Hãy nhập biển số xe để tra cứu")
            return
        }

        // 2) Có ký tự đặc biệt (không phải chữ hoặc số)
        if (bienSo.any { !it.isLetterOrDigit() }) {
            _state.value = UiState.Error("Biển số không được chứa ký tự đặc biệt, chỉ gồm chữ cái và số.")
            return
        }

        // 3) Sai độ dài
        if (bienSo.length !in 5..10) {
            _state.value = UiState.Error("Vui lòng điền đúng định dạng biển số xe!")
            return
        }

        _state.value = UiState.Loading

        viewModelScope.launch {
            try {
                // Retry tối đa 2 lần với backoff 0.8s -> 1.6s (chỉ retry lỗi network)
                val result = retry(times = 2, initialDelayMs = 800L) {
                    repository.checkPhatNguoi(
                        plate = bienSo,
                        vehicleType = loaiXe
                    )
                }

                // Xử lý kết quả từ Repository
                when {
                    // Trường hợp 1: Có lỗi thật sự (network, server, captcha sai, etc.)
                    result.error -> {
                        _state.value = UiState.Error(
                            result.message ?: "Có lỗi xảy ra khi tra cứu"
                        )
                    }
                    
                    // Trường hợp 2: Tra cứu thành công nhưng KHÔNG có vi phạm
                    !result.viPham -> {
                        // Hiển thị message từ server hoặc message mặc định
                        val message = result.message?.takeIf { it.isNotBlank() } 
                            ?: "Không tìm thấy vi phạm giao thông cho biển số này"
                        _state.value = UiState.Error(message)
                    }
                    
                    // Trường hợp 3: Tra cứu thành công và CÓ vi phạm
                    else -> {
                        
                        // Map tất cả vi phạm sang UI state
                        val pairs = if (result.allViolations.isNotEmpty()) {
                            // Map tất cả vi phạm
                            val mappedPairs = mapAllViolationsToPairs(result.allViolations)
                            Log.d("PhatNguoi", "Số pairs sau khi map: ${mappedPairs.size}")
                            mappedPairs
                        } else {
                            // Fallback: map vi phạm đầu tiên (backward compatibility)
                            mapResultToPairs(result)
                        }
                        
                        // Nếu không có dữ liệu để hiển thị (parse HTML lỗi hoặc thiếu)
                        if (pairs.isEmpty()) {
                            _state.value = UiState.Error(
                                "Không thể lấy thông tin vi phạm. Vui lòng thử lại."
                            )
                        } else {
                            // Tính số lỗi từ dữ liệu parse được hoặc từ result
                            val soChuaXuPhat = result.soChuaXuPhat ?: 0
                            
                            // Tính tổng tiền phạt sử dụng Gemini AI
                            val violationsForCalculation = if (result.allViolations.isNotEmpty()) {
                                result.allViolations
                            } else {
                                // Fallback: tạo violation từ result cũ
                                mutableMapOf<String, Any>().apply {
                                    result.bienSo?.let { put("Biển kiểm soát", it) }
                                    result.mauBien?.let { put("Màu biển", it) }
                                    result.loaiPhuongTien?.let { put("Loại phương tiện", it) }
                                    result.thoiGianViPham?.let { put("Thời gian vi phạm", it) }
                                    result.diaDiemViPham?.let { put("Địa điểm vi phạm", it) }
                                    result.hanhViViPham?.let { put("Hành vi vi phạm", it) }
                                    result.trangThai?.let { put("Trạng thái", it) }
                                }.let { listOf(it) }
                            }
                            
                            // Hiển thị kết quả ngay, không đợi AI tính toán
                            // AI sẽ được tính toán ở background sau khi hiển thị kết quả
                            val info = DataInfo(
                                total = result.soLoiViPham ?: result.allViolations.size,
                                chuaxuphat = soChuaXuPhat,
                                daxuphat = result.soDaXuPhat ?: 0,
                                tongTienPhat = 0L,
                                tongTienPhatRange = null
                            )
                            
                            _state.value = UiState.Success(pairs, info, violationsForCalculation)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PhatNguoi", "Tra cứu lỗi", e)
                _state.value = UiState.Error(mapError(e))
            }
        }
    }


    fun reset() {
        _state.value = UiState.Idle
    }

    /* ===========================================================
     * Helpers
     * =========================================================== */

    /** Retry helper với backoff mũ cho một khối suspend */
    private suspend fun <T> retry(
        times: Int,
        initialDelayMs: Long,
        block: suspend () -> T
    ): T {
        var delayMs = initialDelayMs
        repeat(times) {
            try {
                return block()
            } catch (e: SocketTimeoutException) {
                // thử lại
            } catch (e: ConnectException) {
                // thử lại
            } catch (e: UnknownHostException) {
                // thử lại
            }
            delay(delayMs)
            delayMs *= 2
        }
        // lần cuối, nếu lỗi sẽ ném ra ngoài
        return block()
    }

    /** Chuyển Exception -> thông điệp thân thiện cho người dùng */
    private fun mapError(e: Exception): String = when (e) {
        is SocketTimeoutException -> "Máy chủ phản hồi chậm (timeout). Vui lòng thử lại."
        is UnknownHostException   -> "Không thể kết nối máy chủ. Kiểm tra kết nối Internet."
        is ConnectException       -> "Không kết nối được tới máy chủ."
        is SSLHandshakeException  -> "Lỗi bảo mật kết nối (SSL)."
        is IOException            -> e.message ?: "Lỗi mạng."
        else -> e.message ?: "Có lỗi xảy ra."
    }

    /** Map tất cả vi phạm sang danh sách pairs để hiển thị */
    private fun mapAllViolationsToPairs(violations: List<Map<String, Any>>): List<Pair<String, String>> {
        val allPairs = mutableListOf<Pair<String, String>>()
        
        violations.forEachIndexed { index, violation ->
            // Thêm separator cho vi phạm (trừ vi phạm đầu tiên)
            if (index > 0) {
                allPairs.add("---SEPARATOR---" to "---SEPARATOR---")
            }
            
            // Map từng vi phạm
            violation["Biển kiểm soát"]?.let { allPairs.add("Biển kiểm soát" to it.toString()) }
            violation["Màu biển"]?.let { allPairs.add("Màu biển" to it.toString()) }
            violation["Loại phương tiện"]?.let { allPairs.add("Loại phương tiện" to it.toString()) }
            violation["Thời gian vi phạm"]?.let { allPairs.add("Thời gian vi phạm" to it.toString()) }
            violation["Địa điểm vi phạm"]?.let { allPairs.add("Địa điểm vi phạm" to it.toString()) }
            violation["Hành vi vi phạm"]?.let { allPairs.add("Hành vi vi phạm" to it.toString()) }
            violation["Trạng thái"]?.let { allPairs.add("Trạng thái" to it.toString()) }
            violation["Đơn vị phát hiện vi phạm"]?.let { allPairs.add("Đơn vị phát hiện vi phạm" to it.toString()) }
            
            // Nơi giải quyết vụ việc
            val noiGiaiQuyet = violation["Nơi giải quyết vụ việc"]
            if (noiGiaiQuyet is List<*> && noiGiaiQuyet.isNotEmpty()) {
                val noiGiaiQuyetText = noiGiaiQuyet.joinToString("\n") { it.toString() }
                allPairs.add("Nơi giải quyết vụ việc" to noiGiaiQuyetText)
            }
        }
        
        return allPairs
    }
    
    /** Map PhatNguoiResult sang danh sách pairs để hiển thị (backward compatibility) */
    private fun mapResultToPairs(result: PhatNguoiResult): List<Pair<String, String>> {
        val pairs = mutableListOf<Pair<String, String>>()

        result.bienSo?.let { pairs.add("Biển kiểm soát" to it) }
        result.mauBien?.let { pairs.add("Màu biển" to it) }
        result.loaiPhuongTien?.let { pairs.add("Loại phương tiện" to it) }
        result.thoiGianViPham?.let { pairs.add("Thời gian vi phạm" to it) }
        result.diaDiemViPham?.let { pairs.add("Địa điểm vi phạm" to it) }
        result.hanhViViPham?.let { pairs.add("Hành vi vi phạm" to it) }
        result.trangThai?.let { pairs.add("Trạng thái" to it) }
        result.donViPhatHien?.let { pairs.add("Đơn vị phát hiện vi phạm" to it) }
        
        // Nơi giải quyết vụ việc
        if (result.noiGiaiQuyet.isNotEmpty()) {
            val noiGiaiQuyetText = result.noiGiaiQuyet.joinToString("\n") { it }
            pairs.add("Nơi giải quyết vụ việc" to noiGiaiQuyetText)
        }

        return pairs
    }
}
