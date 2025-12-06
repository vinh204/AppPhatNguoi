package com.tuhoc.phatnguoi.ui.settings

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import com.tuhoc.phatnguoi.Divider
import com.tuhoc.phatnguoi.data.local.AuthManager
import com.tuhoc.phatnguoi.ui.theme.RedPrimary
import com.tuhoc.phatnguoi.ui.theme.TextPrimary
import com.tuhoc.phatnguoi.ui.theme.TextSub

@Composable
fun SettingsScreen(
    authManager: AuthManager? = null,
    onLogout: () -> Unit = {},
    onLoginClick: () -> Unit = {},
    onItemClick: (String) -> Unit = {},
    key: Any? = null // Key để trigger reload
) {
    val scope = rememberCoroutineScope()
    var isLoggedIn by remember { mutableStateOf(authManager?.isLoggedInSync() ?: false) }
    var phoneNumber by remember { mutableStateOf("") }
    var hasPassword by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    // Load từ Firestore lần đầu
    LaunchedEffect(Unit) {
        if (authManager != null) {
            isLoggedIn = authManager.isLoggedIn()
            val user = authManager.getCurrentUser()
            if (user != null) {
                phoneNumber = user.phoneNumber
                hasPassword = user.password != null
            } else {
                phoneNumber = ""
                hasPassword = false
            }
        }
    }
    
    // Reload khi key thay đổi (sau khi đăng nhập/đăng xuất)
    LaunchedEffect(key) {
        if (authManager != null) {
            isLoggedIn = authManager.isLoggedIn()
            val user = authManager.getCurrentUser()
            if (user != null) {
                phoneNumber = user.phoneNumber
                hasPassword = user.password != null
            } else {
                phoneNumber = ""
                hasPassword = false
            }
        }
    }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background) // dùng background từ theme
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
        // Thông tin đăng nhập hoặc nút đăng nhập
        if (isLoggedIn && authManager != null) {
            // Đã đăng nhập - hiển thị thông tin
            SettingSection(
                items = listOf(
                    SettingItem(
                        title = "Số điện thoại: $phoneNumber",
                        icon = Icons.Default.AccountCircle
                    ),
                    SettingItem(
                        title = "Đổi mật khẩu",
                        icon = Icons.Default.Lock
                    ),
                    SettingItem(
                        title = "Cài đặt thông báo",
                        icon = Icons.Default.Notifications
                    ),
                    SettingItem("Đăng xuất", Icons.Default.Logout)
                ),
                onItemClick = { title ->
                    when (title) {
                        "Đăng xuất" -> {
                            showLogoutDialog = true
                        }
                        "Đổi mật khẩu" -> {
                            onItemClick("Đổi mật khẩu")
                        }
                        "Cài đặt thông báo" -> {
                            onItemClick("Cài đặt thông báo")
                        }
                        else -> {
                        onItemClick(title)
                        }
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
        } else {
            // Chưa đăng nhập - hiển thị nút đăng nhập
            SettingSection(
                items = listOf(
                    SettingItem("Đăng nhập", Icons.Default.Person)
                ),
                onItemClick = { title ->
                    if (title == "Đăng nhập") {
                        onLoginClick()
                    } else {
                        onItemClick(title)
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
        }

        SettingSection(
            items = listOf(
                SettingItem("Câu hỏi thường gặp", Icons.Default.Help),
                SettingItem("Nộp phạt trực tuyến", Icons.Default.Payment),
                SettingItem("Liên hệ", Icons.Default.Email),
                SettingItem("Đánh giá ứng dụng", Icons.Default.Star),
                SettingItem("Chia sẻ ứng dụng", Icons.Default.Share),
                SettingItem("Chính sách bảo mật", Icons.Default.Security),
                SettingItem("Điều khoản sử dụng", Icons.Default.Description),
            ),
            onItemClick = onItemClick
        )
        
        // Dòng phiên bản ứng dụng
        AppVersionText()
        Spacer(Modifier.height(16.dp))
        }
    
    // Dialog xác nhận đăng xuất
    if (showLogoutDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { showLogoutDialog = false },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Xác nhận đăng xuất",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Text(
                        text = "Bạn có chắc chắn muốn đăng xuất?",
                        fontSize = 16.sp,
                        color = TextPrimary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Nút Hủy
                        Button(
                            onClick = { showLogoutDialog = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TextSub,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Hủy",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        // Nút Đăng xuất
                        Button(
                            onClick = {
                                scope.launch {
                                    authManager?.logout()
                                    isLoggedIn = false
                                    phoneNumber = ""
                                    showLogoutDialog = false
                                    onLogout()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = RedPrimary,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "Đăng xuất",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
    } // Đóng Box
}

@Composable
fun AppVersionText() {
    val context = LocalContext.current
    var versionName by remember { mutableStateOf("1.0.1") }
    
    LaunchedEffect(Unit) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            versionName = packageInfo.versionName ?: "1.0.1"
        } catch (e: PackageManager.NameNotFoundException) {
            versionName = "1.0.1"
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Phiên bản ứng dụng $versionName",
            fontSize = 12.sp,
            color = TextSub.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Normal
        )
    }
}

data class SettingItem(
    val title: String,
    val icon: ImageVector
)

@Composable
fun SettingSection(
    items: List<SettingItem>,
    onItemClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .background(
                color = MaterialTheme.colorScheme.surface, // card màu surface
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(item.title) }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = RedPrimary   // icon trái dùng màu brand
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextSub
                )
            }

            // Divider mờ mờ giữa các item, trừ item cuối
            if (index != items.lastIndex) {
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}
