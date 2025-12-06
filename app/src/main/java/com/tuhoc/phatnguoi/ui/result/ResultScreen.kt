package com.tuhoc.phatnguoi.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.foundation.BorderStroke
import com.tuhoc.phatnguoi.ViolationSummaryCard
import com.tuhoc.phatnguoi.StatusRow
import com.tuhoc.phatnguoi.StatusBadge
import com.tuhoc.phatnguoi.SectionLabel
import com.tuhoc.phatnguoi.ResolvePlaceList
import com.tuhoc.phatnguoi.KeyValueRow
import com.tuhoc.phatnguoi.data.remote.DataInfo
import com.tuhoc.phatnguoi.ui.theme.RedPrimary
import com.tuhoc.phatnguoi.ui.theme.TextPrimary
import com.tuhoc.phatnguoi.ui.theme.TextSub
import androidx.compose.ui.draw.rotate

data class ResultScreenData(
    val info: DataInfo?,
    val pairs: List<Pair<String, String>>,
    val onShowAIScreen: (com.tuhoc.phatnguoi.AIScreenData) -> Unit,
    val onLogin: (() -> Unit)? = null
)

@Composable
fun ResultScreen(
    data: ResultScreenData
) {
    val info = data.info
    val rows = data.pairs

    val desiredOrder = listOf(
        "Màu biển",
        "Loại phương tiện",
        "Thời gian vi phạm",
        "Địa điểm vi phạm",
        "Hành vi vi phạm",
        "Trạng thái",
        "Đơn vị phát hiện vi phạm",
        "Nơi giải quyết vụ việc"
    )

    // Tách các vi phạm thành danh sách riêng biệt
    val violations = remember(rows) {
        val violationsList = mutableListOf<List<Pair<String, String>>>()
        var currentViolation = mutableListOf<Pair<String, String>>()

        rows.forEach { pair ->
            if (pair.first == "---SEPARATOR---") {
                if (currentViolation.isNotEmpty()) {
                    violationsList.add(currentViolation.toList())
                    currentViolation.clear()
                }
            } else {
                currentViolation.add(pair)
            }
        }

        // Thêm vi phạm cuối cùng
        if (currentViolation.isNotEmpty()) {
            violationsList.add(currentViolation.toList())
        }
        violationsList
    }

    // Sort các field trong mỗi vi phạm
    val sortedViolations = remember(violations, desiredOrder) {
        violations.map { violation ->
            violation.sortedBy { p ->
                val keyNormalized = p.first.trim().trimEnd(':')
                val i = desiredOrder.indexOf(keyNormalized)
                if (i == -1) Int.MAX_VALUE else i
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp)
        ) {
            Spacer(Modifier.height(16.dp))

            // Kiểm tra nếu không có vi phạm
            val hasNoViolations = info?.total == 0 || (info == null && rows.isEmpty())
            
            // ✅ Kiểm tra xem có phải là thông báo rate limit không
            val isRateLimitMessage = rows.any { 
                it.first == "Thông báo" && (
                    it.second.contains("tra cứu 3 lần", ignoreCase = true) ||
                    it.second.contains("đăng nhập để tra cứu", ignoreCase = true) ||
                    it.second.contains("vượt quá giới hạn tra cứu", ignoreCase = true) ||
                    it.second.contains("đăng nhập để tiếp tục", ignoreCase = true)
                )
            }
            
            // Lấy biển số từ rows để hiển thị trong thông báo
            val bienSo = rows.find { 
                it.first == "Biển kiểm soát" || 
                it.first == "Biển số" ||
                it.first.contains("Biển", ignoreCase = true)
            }?.second?.trim() ?: "N/A"
            
            // ✅ Hiển thị thông báo rate limit
            if (isRateLimitMessage) {
                val rateLimitMessage = rows.find { it.first == "Thông báo" }?.second 
                    ?: "Đã vượt quá giới hạn tra cứu trong ngày. Vui lòng đăng nhập để tiếp tục tra cứu"
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = rateLimitMessage,
                            color = RedPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        
                        // ✅ Nút đăng nhập ngay - style giống "Xem chi tiết mức phạt"
                        if (data.onLogin != null) {
                            Spacer(Modifier.height(24.dp))
                            OutlinedButton(
                                onClick = { data.onLogin?.invoke() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(40.dp),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = TextPrimary
                                ),
                                border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                            ) {
                                Text(
                                    text = "Đăng nhập ngay",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            } else if (hasNoViolations) {
                // Hiển thị thông báo không có vi phạm với biển số
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (bienSo != "N/A") {
                                "Biển số $bienSo không tìm thấy vi phạm giao thông nào!"
                            } else {
                                "Không tìm thấy vi phạm giao thông nào!"
                            },
                            color = RedPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Summary Card - Tóm tắt vi phạm
                info?.let { violationInfo ->
                    if (violationInfo.total > 0) {
                        // Lấy biển số từ violations hoặc từ rows
                        val bienSo = rows.find { it.first == "Biển kiểm soát" }?.second 
                            ?: violations.firstOrNull()?.find { it.first == "Biển kiểm soát" }?.second
                            ?: "N/A"
                        
                        ViolationSummaryCard(
                            bienSo = bienSo,
                            totalViolations = violationInfo.total,
                            unresolvedViolations = violationInfo.chuaxuphat,
                            tongTienPhat = violationInfo.tongTienPhat,
                            tongTienPhatRange = violationInfo.tongTienPhatRange,
                            onClick = {
                                // Convert violations từ List<List<Pair<String, String>>> sang List<Map<String, Any>>
                                val violationsMap = violations.map { violation ->
                                    violation.associate { it.first to it.second }
                                }

                                // Set data và hiển thị AI screen
                                data.onShowAIScreen(
                                    com.tuhoc.phatnguoi.AIScreenData(
                                        totalViolations = violationInfo.total,
                                        unresolvedViolations = violationInfo.chuaxuphat,
                                        violations = violationsMap
                                    )
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            // Chỉ hiển thị card chi tiết nếu có vi phạm và không phải rate limit message
            if (!hasNoViolations && !isRateLimitMessage) {
                // Hiển thị thông tin cập nhật
                info?.let {
                    it.latest?.let { latest ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Text(
                                text = "Cập nhật: $latest",
                                color = TextSub,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }

                // Hiển thị từng vi phạm trong Card riêng
                if (sortedViolations.isEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Text(
                            "Không có vi phạm nào",
                            color = TextSub,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    sortedViolations.forEachIndexed { violationIndex, violation ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                // Tách trạng thái, loại phương tiện và màu biển để hiển thị đặc biệt
                                val trangThai = violation.find { 
                                    it.first.trim().trimEnd(':') == "Trạng thái" 
                                }?.second?.trim()
                                
                                val loaiPhuongTien = violation.find { 
                                    it.first.trim().trimEnd(':') == "Loại phương tiện" 
                                }?.second?.trim()
                                
                                val mauBien = violation.find { 
                                    it.first.trim().trimEnd(':') == "Màu biển" 
                                }?.second?.trim()
                                
                                // Hiển thị Loại phương tiện với title và status badge ở góc trên bên phải
                                Box(modifier = Modifier.fillMaxWidth()) {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        // Title "Loại phương tiện"
                                        Text(
                                            text = "Loại phương tiện",
                                            color = TextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        // Nội dung: giá trị Loại phương tiện và Màu biển
                                        Spacer(Modifier.height(2.dp))
                                        val content = buildString {
                                            if (loaiPhuongTien != null) {
                                                append(loaiPhuongTien)
                                            }
                                            if (mauBien != null) {
                                                if (loaiPhuongTien != null) {
                                                    append(" - ")
                                                }
                                                append(mauBien)
                                            }
                                        }
                                        if (content.isNotEmpty()) {
                                            Text(
                                                text = content,
                                                color = TextPrimary,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Normal
                                            )
                                        }
                                    }
                                    if (trangThai != null) {
                                        StatusBadge(
                                            text = trangThai,
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .offset(x = (5).dp, y = (-5).dp)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                
                                // Hiển thị các thông tin khác theo format title - nội dung
                                violation
                                    .filter { item ->
                                        val key = item.first.trim().trimEnd(':')
                                        key != "Biển kiểm soát" && 
                                        key != "Loại phương tiện" && 
                                        key != "Màu biển" &&
                                        key != "Trạng thái"
                                    }
                                    .forEachIndexed { idx, item ->
                                        val (kRaw, vRaw) = item
                                        val key = kRaw.trim().trimEnd(':')
                                        val value = vRaw.trim()

                                        when (key) {
                                            "Nơi giải quyết vụ việc" -> {
                                                // Title
                                                Text(
                                                    text = key,
                                                    color = TextPrimary,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Spacer(Modifier.height(2.dp))
                                                // Nội dung
                                                ResolvePlaceList(value)
                                                if (idx < violation.filter { 
                                                    val k = it.first.trim().trimEnd(':')
                                                    k != "Biển kiểm soát" && 
                                                    k != "Loại phương tiện" && 
                                                    k != "Màu biển" &&
                                                    k != "Trạng thái"
                                                }.size - 1) {
                                                    Spacer(Modifier.height(6.dp))
                                                }
                                            }

                                            else -> {
                                                Column {
                                                    // Title
                                                    Text(
                                                        text = key,
                                                        color = TextPrimary,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Spacer(Modifier.height(2.dp))
                                                    // Nội dung
                                                    Text(
                                                        text = value,
                                                        color = TextPrimary,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Normal
                                                    )
                                                }
                                                if (idx < violation.filter { 
                                                    val k = it.first.trim().trimEnd(':')
                                                    k != "Biển kiểm soát" && 
                                                    k != "Loại phương tiện" && 
                                                    k != "Màu biển" &&
                                                    k != "Trạng thái"
                                                }.size - 1) {
                                                    Spacer(Modifier.height(6.dp))
                                                }
                                            }
                                        }
                                    }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

