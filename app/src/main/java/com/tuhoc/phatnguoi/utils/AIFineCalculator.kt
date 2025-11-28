package com.tuhoc.phatnguoi.utils

import android.content.Context
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.tuhoc.phatnguoi.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import com.tuhoc.phatnguoi.utils.ViolationUtils.filterUnresolvedViolations as filterViolations

/**
 * Service để tính số tiền phạt dựa trên thông tin vi phạm sử dụng AI Gemini
 * 
 * Service này sử dụng Google Gemini AI để phân tích các vi phạm giao thông và tính toán
 * mức phạt dựa trên Nghị định 100/2019/NĐ-CP (cho vi phạm trước 01/01/2025) hoặc
 * Nghị định 168/2024/NĐ-CP (cho vi phạm từ 01/01/2025).
 * 
 * @param context Context của ứng dụng (optional, hiện tại không sử dụng)
 * 
 * @see AIFineCalculatorInterface
 * @see FineAnalysisResult
 * @see ViolationUtils
 */
class AIFineCalculator(private val context: Context? = null) : AIFineCalculatorInterface {
    
    private val apiKey: String = BuildConfig.GEMINI_API_KEY
    
    // Cache GenerativeModel instance để tránh tạo lại mỗi lần gọi
    private val model: GenerativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-pro",
            apiKey = apiKey
        )
    }
    
    companion object {
        private const val TAG = "AIFineCalculator"
        private const val API_TIMEOUT_MS = 60_000L // 60 giây
        private const val MAX_RETRIES = 2
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        
        /**
         * Log debug message chỉ trong debug mode
         */
        private fun logD(message: String) {
            if (BuildConfig.DEBUG) Log.d(TAG, message)
        }
        
        /**
         * Log warning message
         */
        private fun logW(message: String) {
            Log.w(TAG, message)
        }
        
        /**
         * Log error message
         */
        private fun logE(message: String, throwable: Throwable? = null) {
            if (throwable != null) {
                Log.e(TAG, message, throwable)
            } else {
                Log.e(TAG, message)
            }
        }
    }
    
    /**
     * Tính toán chi tiết mức phạt cho tất cả vi phạm bằng một lần gọi AI
     * 
     * Hàm này sẽ:
     * 1. Lọc các vi phạm chưa xử phạt
     * 2. Gọi AI với retry mechanism (tối đa 2 lần) và timeout (60 giây)
     * 3. Parse JSON response từ AI
     * 4. Trả về kết quả phân tích chi tiết
     * 
     * Kết quả chỉ lưu tạm thời trong memory, không lưu vào cache.
     * Khi quay về màn hình tra cứu, dữ liệu sẽ mất và sẽ gọi AI lại.
     * 
     * @param violations Danh sách vi phạm cần phân tích (Map với các key như "Hành vi vi phạm", "Loại phương tiện", "Thời gian vi phạm", "Trạng thái")
     * @return FineAnalysisResult chứa tổng tiền phạt và chi tiết từng vi phạm, hoặc null nếu có lỗi
     * 
     * @see FineAnalysisResult
     * @see ViolationUtils.filterUnresolvedViolations
     */
    override suspend fun calculateFineAnalysis(violations: List<Map<String, Any>>): FineAnalysisResult? {
        if (apiKey.isBlank()) {
            logW("API key chưa được cấu hình")
            return null
        }
        
        // Lọc chỉ các vi phạm chưa xử phạt
        val unresolvedViolations = filterViolations(violations)
        
        if (unresolvedViolations.isEmpty()) {
            logD("Không có vi phạm chưa xử phạt để phân tích")
            return FineAnalysisResult(
                totalFineRange = FineAmountRange.empty(),
                violations = emptyList()
            )
        }
        
        // Gọi AI mỗi lần (không dùng cache, chỉ lưu tạm trong memory)
        logD("Đang gọi AI (kết quả chỉ lưu tạm trong memory)...")
        return try {
            withContext(Dispatchers.IO) {
                val result = callAIWithRetry(unresolvedViolations)
                // KHÔNG lưu vào cache - chỉ lưu tạm trong memory (state của màn hình)
                // Khi quay về màn hình tra cứu, state sẽ reset và dữ liệu sẽ mất
                result?.let { 
                    logD("Đã nhận kết quả từ AI (lưu tạm trong memory)")
                }
                result
            }
        } catch (e: TimeoutCancellationException) {
            logE("Timeout khi gọi AI (quá ${API_TIMEOUT_MS}ms)", e)
            null
        } catch (e: Exception) {
            logE("Lỗi khi tính toán phân tích phạt với AI", e)
            null
        }
    }
    
    /**
     * Gọi AI với retry mechanism và timeout
     */
    private suspend fun callAIWithRetry(violations: List<Map<String, Any>>): FineAnalysisResult? {
        var lastException: Exception? = null
        
        repeat(MAX_RETRIES) { attempt ->
            try {
                logD("Thử gọi AI lần ${attempt + 1}/$MAX_RETRIES")
                return callAIWithTimeout(violations)
            } catch (e: TimeoutCancellationException) {
                lastException = e
                if (attempt < MAX_RETRIES - 1) {
                    val delayMs = INITIAL_RETRY_DELAY_MS * (attempt + 1) // Exponential backoff
                    logW("Timeout, thử lại sau ${delayMs}ms...")
                    delay(delayMs)
                }
            } catch (e: Exception) {
                lastException = e
                if (attempt < MAX_RETRIES - 1) {
                    val delayMs = INITIAL_RETRY_DELAY_MS * (attempt + 1) // Exponential backoff
                    logW("Lỗi khi gọi AI, thử lại sau ${delayMs}ms: ${e.message}")
                    delay(delayMs)
                }
            }
        }
        
        logE("Không thể gọi AI sau $MAX_RETRIES lần thử", lastException)
        return null
    }
    
    /**
     * Gọi AI với timeout
     */
    private suspend fun callAIWithTimeout(violations: List<Map<String, Any>>): FineAnalysisResult? {
        return withTimeout(API_TIMEOUT_MS) {
            calculateFineAnalysisWithAI(violations)
        }
    }
    
    /**
     * Gọi AI một lần để lấy tất cả thông tin (tổng tiền phạt + chi tiết từng vi phạm)
     */
    private suspend fun calculateFineAnalysisWithAI(violations: List<Map<String, Any>>): FineAnalysisResult? {
        // Tạo prompt yêu cầu trả về JSON
        val prompt = buildJsonPrompt(violations)
        
        logD("=== PROMPT JSON GỬI CHO AI ===")
        logD(prompt)
        
        // Gọi API
        val response = model.generateContent(prompt)
        val responseText = response.text
        
        logD("=== RESPONSE JSON TỪ AI ===")
        logD(responseText ?: "Không có response")
        
        // Parse JSON từ response
        return JsonResponseParser.parseJsonResponse(responseText ?: "", violations.size)
    }
    
    /**
     * Tạo prompt yêu cầu AI trả về JSON
     */
    private fun buildJsonPrompt(violations: List<Map<String, Any>>): String {
        val violationsText = violations.mapIndexed { index, violation ->
            val originalIndex = violation["_originalIndex"] as? Int ?: index
            buildString {
                append("Vi phạm ${index + 1} (index: $originalIndex):\n")
                violation["Hành vi vi phạm"]?.let { append("- Hành vi vi phạm: $it\n") }
                violation["Loại phương tiện"]?.let { append("- Loại phương tiện: $it\n") }
                violation["Thời gian vi phạm"]?.let { append("- Thời gian vi phạm: $it\n") }
            }
        }.joinToString("\n\n")
        
        return """
Bạn là chuyên gia về luật giao thông đường bộ Việt Nam. Nhiệm vụ của bạn là phân tích chi tiết mức phạt cho các vi phạm giao thông.

Danh sách vi phạm:
$violationsText

YÊU CẦU BẮT BUỘC:

1. Xác định Nghị định dựa vào "Thời gian vi phạm":
   - Trước 01/01/2025: Áp dụng Nghị định 100/2019/NĐ-CP (đã được sửa đổi, bổ sung bởi Nghị định 123/2021/NĐ-CP)
   - Từ 01/01/2025 trở đi: Áp dụng Nghị định 168/2024/NĐ-CP (Nghị định mới, có hiệu lực từ 01/01/2025)

2. TRA CỨU CHÍNH XÁC mức phạt:
   - PHẢI tra cứu từ Nghị định chính thức (168/2024/NĐ-CP hoặc 100/2019/NĐ-CP)
   - Dựa vào: Loại phương tiện (Ô tô/Xe máy/Xe đạp điện) + Hành vi vi phạm + Thời gian vi phạm
   - Tra cứu ĐÚNG điều, khoản, điểm trong Nghị định
   - Đối với Nghị định 168/2024/NĐ-CP: Mức phạt đã được cập nhật mới, PHẢI tra cứu đúng theo nghị định này

3. Mức phạt tiền (MIN và MAX):
   - Tra cứu chính xác số tiền phạt tối thiểu và tối đa
   - Ghi rõ điều, khoản, điểm tham chiếu kèm tên nghị định

4. Hình phạt bổ sung:
   - Tra cứu các hình phạt bổ sung (nếu có): tước GPLX, trừ điểm, tịch thu phương tiện...
   - Ghi rõ điều, khoản, điểm tham chiếu kèm tên nghị định
   - Lưu ý: Nghị định 168/2024/NĐ-CP có thay đổi về hình phạt bổ sung (ví dụ: trừ điểm thay vì tước GPLX)

5. Ghi chú đặc biệt:
   - Trường hợp gây tai nạn giao thông
   - Các trường hợp tăng nặng mức phạt
   - Lưu ý về nghị định mới (nếu áp dụng)

LƯU Ý QUAN TRỌNG:
- PHẢI tra cứu CHÍNH XÁC từ Nghị định chính thức, KHÔNG được dùng thông tin cũ hoặc không chính xác
- Đối với vi phạm từ 01/01/2025: Mức phạt theo Nghị định 168/2024/NĐ-CP đã thay đổi đáng kể so với Nghị định 100/2019
- Ví dụ: Ô tô không chấp hành tín hiệu đèn giao thông từ 01/01/2025:
  * Mức phạt tiền: Từ 18.000.000 đồng đến 20.000.000 đồng (theo Khoản 9 Điều 6 Nghị định 168/2024/NĐ-CP)
  * Hình phạt bổ sung: Bị trừ 04 điểm trên Giấy phép lái xe (theo Điểm c Khoản 11 Điều 2 Nghị định 168/2024/NĐ-CP)
  * Nếu gây tai nạn: Phạt tiền từ 20.000.000 đồng đến 22.000.000 đồng và bị trừ 10 điểm trên GPLX (theo Điểm c Khoản 11 Điều 2 Nghị định 168/2024/NĐ-CP)

TRẢ VỀ KẾT QUẢ DƯỚI DẠNG JSON (chỉ trả về JSON, không có text thừa):

{
  "totalMin": [tổng số tiền phạt tối thiểu của tất cả vi phạm],
  "totalMax": [tổng số tiền phạt tối đa của tất cả vi phạm],
  "violations": [
    {
      "index": [index gốc của vi phạm trong danh sách ban đầu, bắt đầu từ 0],
      "tienPhatMin": [số tiền phạt tối thiểu],
      "tienPhatMax": [số tiền phạt tối đa],
      "moTa": "[mô tả về mức phạt, bao gồm loại phương tiện, hành vi vi phạm, nghị định áp dụng và thời gian có hiệu lực]",
      "mucPhatTien": "[mức phạt tiền với tham chiếu CHÍNH XÁC, ví dụ: 'Từ 18.000.000 đồng đến 20.000.000 đồng (theo Khoản 9 Điều 6 Nghị định 168/2024/NĐ-CP)']",
      "hinhPhatBoSung": "[hình phạt bổ sung với tham chiếu CHÍNH XÁC, ví dụ: 'Bị trừ 04 điểm trên Giấy phép lái xe (theo Điểm c Khoản 11 Điều 2 Nghị định 168/2024/NĐ-CP)'. Nếu không có thì để trống]",
      "ghiChu": "[ghi chú về trường hợp đặc biệt với tham chiếu CHÍNH XÁC, ví dụ: 'Nếu gây tai nạn giao thông: Phạt tiền từ 20.000.000 đồng đến 22.000.000 đồng và bị trừ 10 điểm trên GPLX (theo Điểm c Khoản 11 Điều 2 Nghị định 168/2024/NĐ-CP)'. Nếu không có thì để trống]"
    }
  ]
}

Ví dụ cho Nghị định 168/2024/NĐ-CP (từ 01/01/2025):
{
  "totalMin": 18000000,
  "totalMax": 20000000,
  "violations": [
    {
      "index": 0,
      "tienPhatMin": 18000000,
      "tienPhatMax": 20000000,
      "moTa": "Mức phạt ô tô không chấp hành hiệu lệnh của đèn tín hiệu giao thông từ ngày 01/01/2025 được quy định tại Nghị định 168/2024/NĐ-CP",
      "mucPhatTien": "Từ 18.000.000 đồng đến 20.000.000 đồng (theo Khoản 9 Điều 6 Nghị định 168/2024/NĐ-CP)",
      "hinhPhatBoSung": "Bị trừ 04 điểm trên Giấy phép lái xe (theo Điểm c Khoản 11 Điều 2 Nghị định 168/2024/NĐ-CP)",
      "ghiChu": "Nếu gây tai nạn giao thông: Phạt tiền từ 20.000.000 đồng đến 22.000.000 đồng và bị trừ 10 điểm trên GPLX (theo Điểm c Khoản 11 Điều 2 Nghị định 168/2024/NĐ-CP)"
    }
  ]
}

Ví dụ cho Nghị định 100/2019/NĐ-CP (trước 01/01/2025):
{
  "totalMin": 4000000,
  "totalMax": 6000000,
  "violations": [
    {
      "index": 0,
      "tienPhatMin": 4000000,
      "tienPhatMax": 6000000,
      "moTa": "Mức phạt ô tô không chấp hành tín hiệu đèn giao thông trước ngày 1/1/2025 được quy định tại Nghị định 100/2019/NĐ-CP (đã được sửa đổi, bổ sung bởi Nghị định 123/2021/NĐ-CP)",
      "mucPhatTien": "Từ 4.000.000 đồng đến 6.000.000 đồng (theo Khoản 5 Điều 5 Nghị định 100/2019/NĐ-CP)",
      "hinhPhatBoSung": "Bị tước quyền sử dụng Giấy phép lái xe từ 01 tháng đến 03 tháng (theo Điểm b Khoản 11 Điều 5 Nghị định 100/2019/NĐ-CP)",
      "ghiChu": "Nếu gây tai nạn giao thông: Bị tước quyền sử dụng Giấy phép lái xe từ 02 tháng đến 04 tháng (theo Điểm c Khoản 11 Điều 5 Nghị định 100/2019/NĐ-CP)"
    }
  ]
}
        """.trimIndent()
    }
}

/**
 * Kết quả phân tích từ AI (dạng JSON)
 */
data class FineAnalysisResult(
    val totalFineRange: FineAmountRange,
    val violations: List<ViolationFineDetail>
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("totalMin", totalFineRange.min)
        json.put("totalMax", totalFineRange.max)
        
        val violationsArray = JSONArray()
        violations.forEach { violation ->
            val violationJson = JSONObject()
            violationJson.put("index", violation.originalIndex)
            violationJson.put("tienPhatMin", violation.fineRange.min)
            violationJson.put("tienPhatMax", violation.fineRange.max)
            violationJson.put("moTa", violation.moTa)
            violationJson.put("mucPhatTien", violation.mucPhatTien)
            violationJson.put("hinhPhatBoSung", violation.hinhPhatBoSung)
            violationJson.put("ghiChu", violation.ghiChu)
            // Backward compatibility
            violationJson.put("nghidinh", violation.nghidinh)
            violationJson.put("dieu", violation.dieu)
            violationsArray.put(violationJson)
        }
        json.put("violations", violationsArray)
        
        return json.toString()
    }
    
    companion object {
        fun fromJson(jsonString: String): FineAnalysisResult {
            val json = JSONObject(jsonString)
            val totalMin = json.getLong("totalMin")
            val totalMax = json.getLong("totalMax")
            val totalFineRange = FineAmountRange(totalMin, totalMax)
            
            val violationsArray = json.getJSONArray("violations")
            val violations = mutableListOf<ViolationFineDetail>()
            for (i in 0 until violationsArray.length()) {
                val violationJson = violationsArray.getJSONObject(i)
                val originalIndex = violationJson.optInt("index", i) // Đọc index từ JSON, mặc định là i
                val fineRange = FineAmountRange(
                    violationJson.getLong("tienPhatMin"),
                    violationJson.getLong("tienPhatMax")
                )
                violations.add(
                    ViolationFineDetail(
                        fineRange = fineRange,
                        originalIndex = originalIndex, // Lưu index gốc
                        moTa = violationJson.optString("moTa", ""),
                        mucPhatTien = violationJson.optString("mucPhatTien", ""),
                        hinhPhatBoSung = violationJson.optString("hinhPhatBoSung", ""),
                        ghiChu = violationJson.optString("ghiChu", ""),
                        // Backward compatibility
                        nghidinh = violationJson.optString("nghidinh", ""),
                        dieu = violationJson.optString("dieu", "")
                    )
                )
            }
            
            return FineAnalysisResult(totalFineRange, violations)
        }
    }
}

/**
 * Chi tiết mức phạt cho một vi phạm
 */
data class ViolationFineDetail(
    val fineRange: FineAmountRange,
    val originalIndex: Int = -1, // Index gốc của vi phạm trong danh sách ban đầu
    val moTa: String = "",
    val mucPhatTien: String = "",
    val hinhPhatBoSung: String = "",
    val ghiChu: String = "",
    // Backward compatibility - deprecated fields
    @Deprecated("Sử dụng moTa và mucPhatTien thay thế")
    val nghidinh: String = "",
    @Deprecated("Sử dụng mucPhatTien thay thế")
    val dieu: String = ""
) {
    fun toFineDetails(): FineDetails {
        return FineDetails(
            fineRange = fineRange,
            moTa = moTa,
            mucPhatTien = mucPhatTien,
            hinhPhatBoSung = hinhPhatBoSung,
            ghiChu = ghiChu,
            // Backward compatibility
            nghidinh = nghidinh.ifBlank { extractNghidinhFromMoTa(moTa) },
            dieu = dieu.ifBlank { extractDieuFromMucPhatTien(mucPhatTien) }
        )
    }
    
    private fun extractNghidinhFromMoTa(moTa: String): String {
        // Tìm nghị định trong mô tả
        val regex = Regex("Nghị định\\s+[\\d/]+/NĐ-CP[^.]*")
        return regex.find(moTa)?.value ?: ""
    }
    
    private fun extractDieuFromMucPhatTien(mucPhatTien: String): String {
        // Tìm điều khoản trong mức phạt tiền
        val regex = Regex("\\(theo\\s+([^)]+)\\)")
        return regex.find(mucPhatTien)?.groupValues?.get(1) ?: ""
    }
}

