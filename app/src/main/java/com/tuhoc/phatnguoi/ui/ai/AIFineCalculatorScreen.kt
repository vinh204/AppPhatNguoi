package com.tuhoc.phatnguoi.ui.ai

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.navigationBarsPadding
import com.tuhoc.phatnguoi.ui.theme.RedPrimary
import com.tuhoc.phatnguoi.ui.theme.TextPrimary
import com.tuhoc.phatnguoi.ui.theme.TextSub
import com.tuhoc.phatnguoi.ui.theme.WarningRed
import com.tuhoc.phatnguoi.utils.AIFineCalculator
import com.tuhoc.phatnguoi.utils.FineDetails
import com.tuhoc.phatnguoi.utils.FineAnalysisResult
import com.tuhoc.phatnguoi.utils.ViolationUtils.filterUnresolvedViolations
import kotlinx.coroutines.delay

/**
 * Helper function: Tạo danh sách ViolationAnalysis từ FineAnalysisResult
 * Sử dụng originalIndex để map chính xác vi phạm với kết quả AI
 */
private fun createViolationAnalysisList(
    violations: List<Map<String, Any>>,
    analysisResult: FineAnalysisResult
): List<ViolationAnalysis> {
    // Tạo map từ originalIndex -> ViolationFineDetail để map chính xác
    val analysisMap = analysisResult.violations.associateBy { it.originalIndex }
    
    return violations.mapIndexed { index, violation ->
        // Lấy originalIndex từ violation (đã được thêm khi filter)
        val originalIndex = violation["_originalIndex"] as? Int ?: index
        val violationDetail = analysisMap[originalIndex]?.toFineDetails() ?: FineDetails(
            fineRange = com.tuhoc.phatnguoi.utils.FineAmountRange.empty(),
            moTa = "",
            mucPhatTien = "",
            hinhPhatBoSung = "",
            ghiChu = ""
        )
        
        ViolationAnalysis(
            index = index + 1,
            violation = violation,
            fineDetails = violationDetail
        )
    }
}

/**
 * Màn hình AI phân tích chi tiết vi phạm và tính số tiền phạt
 */
@Composable
fun AIFineCalculatorScreen(
    totalViolations: Int,
    unresolvedViolations: Int,
    violations: List<Map<String, Any>>,
    preCalculatedAnalysis: com.tuhoc.phatnguoi.utils.FineAnalysisResult? = null, // Kết quả đã tính sẵn từ MainActivity
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val fineCalculator = remember { AIFineCalculator(context) }
    
    // State cho việc tính toán
    var isLoading by remember { mutableStateOf(true) }
    var totalFineRange by remember { mutableStateOf<com.tuhoc.phatnguoi.utils.FineAmountRange?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var analysisResults by remember { mutableStateOf<List<ViolationAnalysis>>(emptyList()) }
    
    // Tính toán khi màn hình được hiển thị
    // Kiểm tra preCalculatedAnalysis trước - nếu có thì dùng luôn, không gọi AI
    LaunchedEffect(violations, preCalculatedAnalysis) {
        isLoading = true
        errorMessage = null
        
        try {
            // Lọc chỉ các vi phạm chưa xử phạt
            val unresolvedViolationsList = filterUnresolvedViolations(violations)
            
            if (unresolvedViolationsList.isEmpty()) {
                totalFineRange = com.tuhoc.phatnguoi.utils.FineAmountRange.empty()
                analysisResults = emptyList()
                // Thêm delay 1 giây cho loading
                delay(1000)
                isLoading = false
                return@LaunchedEffect
            }
            
            // Nếu đã có kết quả tính sẵn từ MainActivity, dùng luôn (KHÔNG gọi AI)
            if (preCalculatedAnalysis != null) {
                android.util.Log.d("AIFineCalculatorScreen", "Sử dụng kết quả đã tính sẵn (KHÔNG gọi AI)")
                totalFineRange = preCalculatedAnalysis.totalFineRange
                analysisResults = createViolationAnalysisList(unresolvedViolationsList, preCalculatedAnalysis)
                // Thêm delay 1 giây cho loading khi xem chi tiết
                delay(1000)
                isLoading = false
                return@LaunchedEffect
            }
            
            // Chỉ gọi AI nếu KHÔNG có kết quả sẵn
            android.util.Log.d("AIFineCalculatorScreen", "Không có kết quả sẵn, đang gọi AI...")
            val analysisResult = fineCalculator.calculateFineAnalysis(unresolvedViolationsList)
            
            if (analysisResult != null) {
                totalFineRange = analysisResult.totalFineRange
                analysisResults = createViolationAnalysisList(unresolvedViolationsList, analysisResult)
            } else {
                errorMessage = "Không thể phân tích vi phạm. Vui lòng thử lại."
                totalFineRange = com.tuhoc.phatnguoi.utils.FineAmountRange.empty()
                analysisResults = emptyList()
            }
            
        } catch (e: Exception) {
            errorMessage = "Không thể tính toán số tiền phạt: ${e.message}"
            totalFineRange = com.tuhoc.phatnguoi.utils.FineAmountRange.empty()
            analysisResults = emptyList()
        } finally {
            // Khi gọi AI, không cần delay thêm vì AI đã mất thời gian
            // Chỉ delay khi có dữ liệu sẵn (đã xử lý ở trên)
            isLoading = false
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        when {
            isLoading -> {
                // Loading state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = RedPrimary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Đang phân tích vi phạm bằng AI...",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Vui lòng đợi trong giây lát",
                            color = TextSub,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            errorMessage != null -> {
                // Error state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = WarningRed,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = "Có lỗi xảy ra",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "Không thể tính toán số tiền phạt",
                            color = TextSub,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            else -> {
                // Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding(), // Tự động thêm padding để tránh navigation bar
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Chi tiết mức phạt
                    if (analysisResults.isNotEmpty()) {
                        itemsIndexed(analysisResults) { index, analysis ->
                            ViolationAnalysisCard(
                                analysis = analysis,
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            if (index < analysisResults.size - 1) {
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    } else {
                        item {
                            EmptyStateCard(
                                message = "Không có vi phạm chưa xử phạt để phân tích"
                            )
                        }
                    }
                    
                    // Thông tin bổ sung
                    item {
                        InfoCard()
                    }
                }
            }
        }
    }
}

/**
 * Card phân tích từng vi phạm
 */
@Composable
private fun ViolationAnalysisCard(
    analysis: ViolationAnalysis,
    modifier: Modifier = Modifier
) {
    val violation = analysis.violation
    
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Hiển thị 4 nội dung theo thứ tự: Loại phương tiện, Thời gian, Địa điểm, Hành vi vi phạm
            // Format: Title (bold) - Nội dung (normal), không có trạng thái
            
            // 1. Loại phương tiện
            violation["Loại phương tiện"]?.let { loaiXe ->
                val mauBien = violation["Màu biển"]?.toString()?.trim()
                Column {
                    Text(
                        text = "Loại phương tiện",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = buildString {
                            append(loaiXe.toString().trim())
                            if (mauBien != null && mauBien.isNotBlank()) {
                                append(" - $mauBien")
                            }
                        },
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
            
            // 2. Thời gian vi phạm
            violation["Thời gian vi phạm"]?.let { thoiGian ->
                Column {
                    Text(
                        text = "Thời gian vi phạm",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = thoiGian.toString().trim(),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
            
            // 3. Địa điểm vi phạm
            violation["Địa điểm vi phạm"]?.let { diaDiem ->
                Column {
                    Text(
                        text = "Địa điểm vi phạm",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = diaDiem.toString().trim(),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
            
            // 4. Hành vi vi phạm
            violation["Hành vi vi phạm"]?.let { hanhVi ->
                Column {
                    Text(
                        text = "Hành vi vi phạm",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = hanhVi.toString().trim(),
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
            
            // Mức phạt - Gom tất cả thông tin mức phạt vào đây (style giống Hình phạt bổ sung trước)
            if (analysis.fineDetails.fineRange.isValid || 
                analysis.fineDetails.moTa.isNotBlank() ||
                analysis.fineDetails.mucPhatTien.isNotBlank() ||
                analysis.fineDetails.hinhPhatBoSung.isNotBlank() ||
                analysis.fineDetails.ghiChu.isNotBlank() ||
                // Backward compatibility
                analysis.fineDetails.nghidinh.isNotBlank() ||
                analysis.fineDetails.dieu.isNotBlank()) {
                
                // Kiểm tra xem có ít nhất một trong 4 field được hiển thị không
                val hasBasicInfo = violation["Loại phương tiện"] != null ||
                                   violation["Thời gian vi phạm"] != null ||
                                   violation["Địa điểm vi phạm"] != null ||
                                   violation["Hành vi vi phạm"] != null
                
                if (hasBasicInfo) {
                    Spacer(Modifier.height(6.dp))
                }
                
                // Card chứa thông tin mức phạt (style giống Hình phạt bổ sung)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = WarningRed.copy(alpha = 0.1f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(
                        1.dp,
                        WarningRed.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // Header với icon và title
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = WarningRed,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Mức phạt:",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        
                        // Nội dung mức phạt - Hiển thị 4 trường: moTa, mucPhatTien, hinhPhatBoSung, ghiChu
                        Column {
                            // 1. Mô tả (moTa)
                            if (analysis.fineDetails.moTa.isNotBlank()) {
                                Text(
                                    text = analysis.fineDetails.moTa,
                                    color = WarningRed,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 20.sp
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                            
                            // 2. Mức phạt tiền (mucPhatTien)
                            if (analysis.fineDetails.mucPhatTien.isNotBlank()) {
                                Text(
                                    text = "- Mức phạt tiền: ${analysis.fineDetails.mucPhatTien}",
                                    color = WarningRed,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 20.sp
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                            
                            // 3. Hình thức xử phạt bổ sung (hinhPhatBoSung)
                            if (analysis.fineDetails.hinhPhatBoSung.isNotBlank()) {
                                Text(
                                    text = "- Hình thức xử phạt bổ sung: ${analysis.fineDetails.hinhPhatBoSung}",
                                    color = WarningRed,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 20.sp
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                            
                            // 4. Ghi chú (ghiChu)
                            if (analysis.fineDetails.ghiChu.isNotBlank()) {
                                Text(
                                    text = analysis.fineDetails.ghiChu,
                                    color = WarningRed,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Card trạng thái rỗng
 */
@Composable
private fun EmptyStateCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = TextSub,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = message,
                color = TextSub,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Card thông tin bổ sung
 */
@Composable
private fun InfoCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Số tiền phạt được tính toán bằng AI dựa trên Nghị định 100/2019/NĐ-CP (cho vi phạm trước 01/01/2025) hoặc Nghị định 168/2024/NĐ-CP (cho vi phạm từ 01/01/2025). Đây là số tiền ước tính, số tiền thực tế có thể khác.",
                color = TextPrimary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}


/**
 * Data class cho phân tích vi phạm
 */
private data class ViolationAnalysis(
    val index: Int,
    val violation: Map<String, Any>,
    val fineDetails: FineDetails
)
