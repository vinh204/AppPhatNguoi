package com.tuhoc.phatnguoi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.History
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tuhoc.phatnguoi.ui.theme.PhatNguoiTheme
import kotlinx.coroutines.delay

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
                        popUpTo("splash") { inclusive = true } // xóa splash khỏi back stack
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
            .background(Color(0xFFE53935)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = "Camera",
                    tint = Color(0xFFB71C1C),
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Tra Cứu Phạt Nguội",
                color = Color.White,
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


@Composable
fun TraCuuScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(12.dp))
        // Header đỏ
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(MaterialTheme.colorScheme.primary),
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
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color(0xFFB71C1C))
                }
                Spacer(Modifier.height(8.dp))
                Text("Tra Cứu Phạt Nguội", color = Color.White, fontWeight = FontWeight.Bold)
                Text("Nguồn: Từ Cục Cảnh sát giao thông", color = Color.White.copy(.9f), fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(20.dp))

        var plate by remember { mutableStateOf("") }

        OutlinedTextField(
            value = plate,
            onValueChange = { plate = it.uppercase().replace(" ", "") },
            placeholder = { Text("Nhập biển số xe") },
            supportingText = {
                Text(
                    "Nhập liền mạch, không kí tự đặc biệt (Ví dụ: 30L88253)",
                    color = Color(0xFF757575)
                )
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        )

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { /* TODO: gọi API tra cứu */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .height(48.dp),
            shape = CircleShape
        ) {
            Text("Tra ngay")
        }

        Spacer(Modifier.height(24.dp))

        Text("Hướng dẫn tra cứu:", fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        Text(
            "1. Nhập biển số xe ví dụ: 38K119735. Lưu ý nhập liền mạch không kí tự đặc biệt\n\n" +
                    "2. Nhấn vào nút Tra ngay và đợi kết quả trả về",
            color = Color(0xFFFF6F61) // subtext/hướng dẫn
        )
    }
}

@Composable
fun Placeholder(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title)
    }
}
