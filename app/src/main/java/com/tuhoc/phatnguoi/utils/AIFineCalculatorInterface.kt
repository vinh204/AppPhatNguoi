package com.tuhoc.phatnguoi.utils

/**
 * Interface cho AI Fine Calculator để dễ dàng test và mock
 */
interface AIFineCalculatorInterface {
    /**
     * Tính toán chi tiết mức phạt cho tất cả vi phạm bằng một lần gọi AI
     * 
     * @param violations Danh sách vi phạm
     * @return FineAnalysisResult chứa tổng tiền phạt và chi tiết từng vi phạm, hoặc null nếu có lỗi
     */
    suspend fun calculateFineAnalysis(violations: List<Map<String, Any>>): FineAnalysisResult?
}




