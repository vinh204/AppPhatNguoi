package com.tuhoc.phatnguoi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tuhoc.phatnguoi.ui.theme.PhatNguoiTheme
import com.tuhoc.phatnguoi.ui.theme.TextSub   // #757575
import com.tuhoc.phatnguoi.ui.theme.WarningRed // #D32F2F (nếu cần cảnh báo)
import kotlinx.coroutines.delay
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Search
import androidx.compose.foundation.shape.RoundedCornerShape



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhatNguoiTheme {
                AppNav()
            }
        }
    }
}

/* ---------------- Nav graph ---------------- */

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "splash") {
        composable("splash") {
            SplashScreen(
                onDone = {
                    nav.navigate("home") {
                        popUpTo("splash") { inclusive = true } // xoá splash khỏi back stack
                    }
                }
            )
        }
        composable("home") { MainTabsScreen() }
    }
}

/* ---------------- Splash ---------------- */

@Composable
fun SplashScreen(onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(2000) // 2 giây
        onDone()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary), // 🔴 #E53935
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E0E0)), // vòng tròn xám nhạt
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = "Camera",
                    tint = MaterialTheme.colorScheme.primary, // icon đỏ đồng bộ
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Tra Cứu Phạt Nguội",
                color = MaterialTheme.colorScheme.onPrimary, // trắng trên nền đỏ
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

/* ---------------- Home + 5 tab ---------------- */

sealed class BottomItem(val route: String, val label: String, val icon: @Composable () -> Unit) {
    object TraCuu : BottomItem("tab_tracuu", "Tra cứu", { Icon(Icons.Filled.Home, null) })
    object LichSu : BottomItem("tab_lichsu", "Lịch sử", { Icon(Icons.Filled.History, null) })
    object Camera : BottomItem("tab_camera", "Camera", { Icon(Icons.Filled.Camera, null) })
    object TinTuc : BottomItem("tab_tintuc", "Tin tức", { Icon(Icons.Filled.Article, null) })
    object CaiDat : BottomItem("tab_caidat", "Cài đặt", { Icon(Icons.Filled.Settings, null) })
}

@Composable
fun MainTabsScreen() {
    val tabs = listOf(
        BottomItem.TraCuu, BottomItem.LichSu, BottomItem.Camera, BottomItem.TinTuc, BottomItem.CaiDat
    )
    var current by remember { mutableStateOf<BottomItem>(BottomItem.TraCuu) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { item ->
                    NavigationBarItem(
                        selected = current.route == item.route,
                        onClick = { current = item },
                        icon = { item.icon() },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { inner ->
        Box(Modifier.padding(inner)) {
            when (current) {
                is BottomItem.TraCuu -> TraCuuScreen()
                is BottomItem.LichSu -> Placeholder("Lịch sử")
                is BottomItem.Camera -> Placeholder("Camera")
                is BottomItem.TinTuc -> Placeholder("Tin tức")
                is BottomItem.CaiDat -> Placeholder("Cài đặt")
            }
        }
    }
}

/* --------- Màn hình Tra cứu --------- */

@Composable
fun TraCuuScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background), // ⚪ #FFFFFF
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))

        // Header đỏ tràn viền
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(MaterialTheme.colorScheme.primary), // 🔴 #E53935
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary // icon đỏ đồng bộ
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "Tra Cứu Phạt Nguội",
                    color = MaterialTheme.colorScheme.onPrimary, // trắng
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Nguồn: Từ Cục Cảnh sát giao thông",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(20.dp))


        var plate by remember { mutableStateOf("") }
        val plateOk = remember(plate) { plate.matches(Regex("^[A-Z0-9]{5,10}$")) }

// ------- Card: Nhập biển số + nút Tra ngay -------
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(Modifier.padding(16.dp)) {

                OutlinedTextField(
                    value = plate,
                    onValueChange = { plate = it.uppercase().replace(" ", "") },
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center
                    ),
                    placeholder = {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Nhập biển số xe",
                                color = TextSub,
                                textAlign = TextAlign.Center
                            )
                        }
                    },

                    supportingText = {
                        Text(
                            "Nhập liền mạch, không ký tự đặc biệt (Ví dụ: 20C11771)",
                            color = TextSub,
                            fontSize = 12.sp,
                            maxLines = 2,                // ⬅️ cho phép 2 dòng
                            textAlign = TextAlign.Center // nếu muốn căn giữa
                        )
                    },
                    singleLine = true,
                    shape = CircleShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = TextSub,
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.secondary,
                        unfocusedLabelColor = TextSub,
                        focusedPlaceholderColor = TextSub,
                        unfocusedPlaceholderColor = TextSub,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                )

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { /* TODO: gọi API tra cứu */ },
                    enabled = true, // luôn đỏ
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = CircleShape
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Tra ngay")
                }
            }
        }



        Spacer(Modifier.height(24.dp))

        // Hướng dẫn: chỉ phần này có padding 2 bên
        Text(
            "Hướng dẫn tra cứu:",
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
        )

// Card nội dung
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
                    .copy(alpha = 0.6f) // nền xám nhạt như ảnh
            )
        ) {
            Column(Modifier.padding(16.dp)) {
                StepItem(number = 1, text = "Nhập vào biển số xe, ví dụ 20C11771, lưu ý nhập liền mạch và không cần ký tự đặc biệt.")
                Spacer(Modifier.height(10.dp))
                StepItem(number = 2, text = "Nhấn vào nút Tra ngay và đợi kết quả trả về.")
            }
        }

    }
}

@Composable
fun Placeholder(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title)
    }
}

@Composable
fun StepItem(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color(0xFFF0F0F0)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                number.toString(),
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

