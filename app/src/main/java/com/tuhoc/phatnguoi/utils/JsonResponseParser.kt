package com.tuhoc.phatnguoi.utils

import org.json.JSONArray
import org.json.JSONObject
import com.tuhoc.phatnguoi.security.SecureLogger

/**
 * Parser cho JSON response từ AI
 */
object JsonResponseParser {
    
    /**
     * Parse JSON response từ AI
     * Xử lý cả trường hợp response có markdown code block (```json ... ```)
     * 
     * @param responseText Text response từ AI
     * @param expectedViolationCount Số lượng vi phạm mong đợi
     * @return FineAnalysisResult hoặc null nếu parse thất bại
     */
    fun parseJsonResponse(responseText: String, expectedViolationCount: Int): FineAnalysisResult? {
        try {
            // Bước 1: Tìm JSON trong markdown code block (```json ... ```)
            val jsonBlockRegex = Regex("```json\\s*([\\s\\S]*?)```", RegexOption.IGNORE_CASE)
            val jsonBlockMatch = jsonBlockRegex.find(responseText)
            val jsonString = if (jsonBlockMatch != null) {
                // Lấy JSON từ code block
                jsonBlockMatch.groupValues[1].trim()
            } else {
                // Bước 2: Nếu không có code block, tìm JSON object trực tiếp
                val jsonStart = responseText.indexOf('{')
                val jsonEnd = responseText.lastIndexOf('}') + 1
                
                if (jsonStart < 0 || jsonEnd <= jsonStart) {
                    SecureLogger.e("Không tìm thấy JSON trong response")
                    return null
                }
                
                responseText.substring(jsonStart, jsonEnd)
            }
            
            if (jsonString.isBlank()) {
                SecureLogger.e("JSON string rỗng")
                return null
            }
            
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
            
            // Đảm bảo số lượng vi phạm khớp
            while (violations.size < expectedViolationCount) {
                violations.add(
                    ViolationFineDetail(
                        fineRange = FineAmountRange.empty(),
                        originalIndex = -1, // Index không hợp lệ cho vi phạm thiếu
                        moTa = "",
                        mucPhatTien = "",
                        hinhPhatBoSung = "",
                        ghiChu = ""
                    )
                )
            }
            
            return FineAnalysisResult(totalFineRange, violations.take(expectedViolationCount))
        } catch (e: Exception) {
            SecureLogger.e("Lỗi khi parse JSON response", e)
            return null
        }
    }
}

