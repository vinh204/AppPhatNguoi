package com.tuhoc.phatnguoi.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuhoc.phatnguoi.data.local.AutoCheckManager
import com.tuhoc.phatnguoi.data.local.HistoryManager
import com.tuhoc.phatnguoi.data.local.TraCuuHistoryItem
import com.tuhoc.phatnguoi.ui.theme.RedPrimary
import com.tuhoc.phatnguoi.ui.theme.TextPrimary
import com.tuhoc.phatnguoi.ui.theme.TextSub
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HistoryScreen(
    historyManager: HistoryManager,
    onItemClick: (String, Int) -> Unit = { _, _ -> },
    key: Int = 0
) {
    val context = LocalContext.current
    val autoCheckManager = remember { AutoCheckManager(context) }
    val scope = rememberCoroutineScope()
    
    var history by remember { mutableStateOf<List<TraCuuHistoryItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Function để reload lịch sử
    fun reloadHistory() {
        scope.launch {
            isLoading = true
            // Đợi một chút để hiển thị loading
            kotlinx.coroutines.delay(100)
            history = historyManager.getHistory()
            isLoading = false
        }
    }
    
    // Reload dữ liệu mỗi lần screen được hiển thị hoặc key thay đổi
    LaunchedEffect(key) {
        reloadHistory()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        when {
            isLoading -> {
                // Không hiển thị gì khi đang load để tránh flash empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = RedPrimary
                    )
                }
            }
            history.isEmpty() -> {
                EmptyHistoryView()
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(history) { item ->
                        HistoryItemCard(
                            item = item,
                            autoCheckManager = autoCheckManager,
                            scope = scope,
                            historyManager = historyManager,
                            onSearchClick = {
                                val loaiXe = when (item.loaiXe) {
                                    "Ô tô" -> 1
                                    "Xe máy" -> 2
                                    "Xe máy điện" -> 3
                                    else -> 1
                                }
                                onItemClick(item.bienSo, loaiXe)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyHistoryView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.History,
                contentDescription = null,
                tint = TextSub,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Chưa có lịch sử tra cứu",
                fontSize = 16.sp,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Các biển số bạn tra cứu sẽ được lưu ở đây",
                fontSize = 14.sp,
                color = TextSub,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

@Composable
fun HistoryItemCard(
    item: TraCuuHistoryItem,
    autoCheckManager: AutoCheckManager,
    scope: kotlinx.coroutines.CoroutineScope,
    historyManager: HistoryManager,
    onSearchClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val dateStr = dateFormat.format(Date(item.thoiGian))
    
    // Icon theo loại xe
    val vehicleIcon = when (item.loaiXe) {
        "Ô tô" -> Icons.Filled.DirectionsCar
        "Xe máy" -> Icons.Filled.TwoWheeler
        "Xe máy điện" -> Icons.Filled.ElectricBike
        else -> Icons.Filled.DirectionsCar
    }
    
    // Lấy loại xe dạng số
    val loaiXe = when (item.loaiXe) {
        "Ô tô" -> 1
        "Xe máy" -> 2
        "Xe máy điện" -> 3
        else -> 1
    }
    
    // Load trạng thái tự động tra cứu từ database
    var toggleChecked by remember { mutableStateOf(false) }
    
    LaunchedEffect(item.bienSo) {
        toggleChecked = autoCheckManager.isAutoCheckEnabled(item.bienSo)
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Hàng trên: Icon, Biển số, Nút search
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon xe trong vòng tròn đỏ
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            color = RedPrimary,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = vehicleIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(Modifier.width(12.dp))
                
                // Biển số và thông tin
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.bienSo,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (item.coViPham) {
                            "Có vi phạm"
                        } else {
                            "Chưa phát hiện lỗi vi phạm"
                        },
                        fontSize = 14.sp,
                        color = TextSub
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Lần tra cứu gần nhất: $dateStr",
                        fontSize = 13.sp,
                        color = TextSub
                    )
                }
                
                // Nút search
                IconButton(
                    onClick = onSearchClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Color(0xFFF5F5F5),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "Tra cứu",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(Modifier.height(12.dp))
            
            Divider(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFE0E0E0),
                thickness = 1.dp
            )
            
            Spacer(Modifier.height(8.dp))
            
            // Toggle tự động check
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Tự động tra cứu hàng ngày và thông báo",
                    fontSize = 13.sp,
                    color = TextSub
                )
                
                Switch(
                    checked = toggleChecked,
                    onCheckedChange = { enabled ->
                        toggleChecked = enabled
                        scope.launch {
                            autoCheckManager.addOrUpdateAutoCheck(
                                bienSo = item.bienSo,
                                loaiXe = loaiXe,
                                enabled = enabled
                            )
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = RedPrimary,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = Color(0xFFCCCCCC)
                    )
                )
            }
        }
    }
}

