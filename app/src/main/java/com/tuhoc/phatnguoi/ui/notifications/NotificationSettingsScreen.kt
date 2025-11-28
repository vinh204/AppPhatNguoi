package com.tuhoc.phatnguoi.ui.notifications

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuhoc.phatnguoi.data.local.AuthManager
import com.tuhoc.phatnguoi.data.local.NotificationSettingsManager
import com.tuhoc.phatnguoi.ui.theme.RedPrimary
import com.tuhoc.phatnguoi.ui.theme.TextPrimary
import com.tuhoc.phatnguoi.ui.theme.TextSub
import com.tuhoc.phatnguoi.utils.AutoCheckScheduler

@Composable
fun NotificationSettingsScreen(
    modifier: Modifier = Modifier,
    onLoginClick: () -> Unit = {},
    isLoggedInInitial: Boolean = false, // Trạng thái đăng nhập ban đầu (từ MainActivity)
    key: Any? = null // Key để reload khi đăng nhập/đăng xuất
) {
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    val settingsManager = remember { NotificationSettingsManager(context) }
    val scheduler = remember { AutoCheckScheduler(context) }
    
    // Kiểm tra trạng thái đăng nhập - khởi tạo với giá trị từ MainActivity để tránh flicker
    var isLoggedIn by remember { mutableStateOf(isLoggedInInitial) }
    
    // Cập nhật khi key thay đổi (đăng nhập/đăng xuất) hoặc khi isLoggedInInitial thay đổi
    LaunchedEffect(key, isLoggedInInitial) {
        isLoggedIn = isLoggedInInitial
        // Đảm bảo đồng bộ với database
        val actualStatus = authManager.isLoggedIn()
        if (actualStatus != isLoggedIn) {
            isLoggedIn = actualStatus
        }
    }
    
    // States for notification settings - load from preferences
    var notifyApp by remember { mutableStateOf(settingsManager.getNotifyApp()) }
    var notifySMS by remember { mutableStateOf(settingsManager.getNotifySMS()) }
    
    // Đảm bảo SMS tắt khi chưa đăng nhập
    LaunchedEffect(isLoggedIn) {
        if (!isLoggedIn) {
            notifySMS = false // Tắt SMS khi chưa đăng nhập
        }
    }
    
    // Frequency states - load from preferences
    var frequency by remember { mutableStateOf(settingsManager.getFrequency()) }
    var showFrequencyDialog by remember { mutableStateOf(false) }
    val frequencyOptions = listOf("Hàng ngày", "Hàng tuần", "Hàng tháng")
    
    // Time states - load from preferences
    var selectedHour by remember { mutableStateOf(settingsManager.getHour()) }
    var selectedMinute by remember { mutableStateOf(settingsManager.getMinute()) }
    var showTimeDialog by remember { mutableStateOf(false) }
    
    // Save settings when changed and reschedule alarm (chỉ khi đã đăng nhập)
    LaunchedEffect(notifyApp, notifySMS, frequency, selectedHour, selectedMinute, isLoggedIn) {
        if (isLoggedIn) {
            settingsManager.saveSettings(
                notifyApp = notifyApp,
                notifySMS = notifySMS,
                frequency = frequency,
                hour = selectedHour,
                minute = selectedMinute
            )
            
            // Reschedule alarm khi cài đặt thay đổi
            scheduler.scheduleNextAutoCheck()
        }
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
            .verticalScroll(rememberScrollState())
            .padding(bottom = 80.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        
        // Cảnh báo nếu chưa đăng nhập
        if (!isLoggedIn) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Vui lòng đăng nhập để sử dụng tính năng thông báo tự động",
                        fontSize = 14.sp,
                        color = TextPrimary,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = onLoginClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RedPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Đăng nhập ngay", fontSize = 14.sp)
                    }
                }
            }
        }
        
        // Section: Tùy chọn thông báo
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Tùy chọn thông báo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Thông báo qua ứng dụng
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = null,
                            tint = if (isLoggedIn) RedPrimary else TextSub.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Qua ứng dụng",
                                fontSize = 16.sp,
                                color = if (isLoggedIn) TextPrimary else TextSub,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isLoggedIn) "Thông báo trong ứng dụng" else "Cần đăng nhập để sử dụng",
                                fontSize = 13.sp,
                                color = TextSub
                            )
                        }
                    }
                    Switch(
                        checked = notifyApp,
                        onCheckedChange = { if (isLoggedIn) notifyApp = it },
                        enabled = isLoggedIn,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = RedPrimary,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFCCCCCC),
                            disabledCheckedThumbColor = Color.White,
                            disabledCheckedTrackColor = Color(0xFFCCCCCC).copy(alpha = 0.5f),
                            disabledUncheckedThumbColor = Color.White,
                            disabledUncheckedTrackColor = Color(0xFFCCCCCC).copy(alpha = 0.5f)
                        )
                    )
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Thông báo qua SMS - luôn hiển thị nhưng disable khi chưa đăng nhập
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Message,
                            contentDescription = null,
                            tint = if (isLoggedIn) RedPrimary else TextSub.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Qua SMS",
                                fontSize = 16.sp,
                                color = if (isLoggedIn) TextPrimary else TextSub,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isLoggedIn) "Nhận thông báo qua tin nhắn" else "Cần đăng nhập để sử dụng",
                                fontSize = 13.sp,
                                color = TextSub
                            )
                        }
                    }
                    Switch(
                        checked = notifySMS,
                        onCheckedChange = { if (isLoggedIn) notifySMS = it },
                        enabled = isLoggedIn,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = RedPrimary,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFCCCCCC),
                            disabledCheckedThumbColor = Color.White,
                            disabledCheckedTrackColor = Color(0xFFCCCCCC).copy(alpha = 0.5f),
                            disabledUncheckedThumbColor = Color.White,
                            disabledUncheckedTrackColor = Color(0xFFCCCCCC).copy(alpha = 0.5f)
                        )
                    )
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
        
        // Section: Tần suất và thời gian thông báo
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Tần suất và thời gian thông báo",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Tần suất thông báo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isLoggedIn) { if (isLoggedIn) showFrequencyDialog = true }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            tint = RedPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Tần suất thông báo",
                                fontSize = 16.sp,
                                color = if (isLoggedIn) TextPrimary else TextSub,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isLoggedIn) frequency else "Cần đăng nhập",
                                fontSize = 14.sp,
                                color = TextSub,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = TextSub
                    )
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Thời gian thông báo
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = isLoggedIn) { if (isLoggedIn) showTimeDialog = true }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = RedPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Thời gian thông báo",
                                fontSize = 16.sp,
                                color = if (isLoggedIn) TextPrimary else TextSub,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = if (isLoggedIn) String.format("%02d:%02d", selectedHour, selectedMinute) else "Cần đăng nhập",
                                fontSize = 14.sp,
                                color = TextSub,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = null,
                        tint = TextSub
                    )
                }
            }
        }
        
        Spacer(Modifier.height(16.dp))
    }
    
    // Frequency selection dialog
    if (showFrequencyDialog) {
        AlertDialog(
            onDismissRequest = { showFrequencyDialog = false },
            title = {
                Text(
                    text = "Chọn tần suất thông báo",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    frequencyOptions.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    frequency = option
                                    showFrequencyDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = frequency == option,
                                onClick = {
                                    frequency = option
                                    showFrequencyDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = RedPrimary
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = option,
                                fontSize = 16.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showFrequencyDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = RedPrimary
                    )
                ) {
                    Text("Đóng")
                }
            }
        )
    }
    
    // Time picker dialog
    if (showTimeDialog) {
        var tempHour by remember { mutableStateOf(selectedHour) }
        var tempMinute by remember { mutableStateOf(selectedMinute) }
        
        AlertDialog(
            onDismissRequest = { showTimeDialog = false },
            title = {
                Text(
                    text = "Chọn thời gian",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 24.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hour selector
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(80.dp)
                        ) {
                            Text(
                                text = "Giờ",
                                fontSize = 14.sp,
                                color = TextSub,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                IconButton(
                                    onClick = {
                                        tempHour = if (tempHour <= 0) 23 else tempHour - 1
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Tăng",
                                        tint = RedPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = String.format("%02d", tempHour),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    modifier = Modifier.width(56.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(Modifier.height(4.dp))
                                IconButton(
                                    onClick = {
                                        tempHour = if (tempHour >= 23) 0 else tempHour + 1
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Giảm",
                                        tint = RedPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                        
                        Text(
                            text = ":",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(top = 28.dp)
                        )
                        
                        // Minute selector
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(80.dp)
                        ) {
                            Text(
                                text = "Phút",
                                fontSize = 14.sp,
                                color = TextSub,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                IconButton(
                                    onClick = {
                                        tempMinute = if (tempMinute <= 0) 55 else tempMinute - 5
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowUpward,
                                        contentDescription = "Tăng",
                                        tint = RedPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = String.format("%02d", tempMinute),
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    modifier = Modifier.width(56.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(Modifier.height(4.dp))
                                IconButton(
                                    onClick = {
                                        tempMinute = if (tempMinute >= 55) 0 else tempMinute + 5
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDownward,
                                        contentDescription = "Giảm",
                                        tint = RedPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedHour = tempHour
                        selectedMinute = tempMinute
                        showTimeDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = RedPrimary
                    )
                ) {
                    Text("Xác nhận", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTimeDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = TextSub
                    )
                ) {
                    Text("Hủy")
                }
            }
        )
    }
}

