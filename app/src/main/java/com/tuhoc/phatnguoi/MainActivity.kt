package com.tuhoc.phatnguoi

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.Icons.Default
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import com.tuhoc.phatnguoi.data.local.AuthManager
import com.tuhoc.phatnguoi.data.local.HistoryManager
import com.tuhoc.phatnguoi.data.local.NewsManager
import com.tuhoc.phatnguoi.data.remote.OtpService
import com.tuhoc.phatnguoi.data.local.TraCuuHistoryItem
import com.tuhoc.phatnguoi.ui.faq.FAQScreen
import com.tuhoc.phatnguoi.ui.history.HistoryScreen
import com.tuhoc.phatnguoi.ui.login.LoginScreen
import com.tuhoc.phatnguoi.ui.login.ChangePasswordScreen
import com.tuhoc.phatnguoi.ui.login.ForgotPasswordScreen
import com.tuhoc.phatnguoi.ui.news.NewsScreen
import com.tuhoc.phatnguoi.ui.notifications.NotificationSettingsScreen
import com.tuhoc.phatnguoi.ui.policy.PolicyScreen
import com.tuhoc.phatnguoi.ui.result.ResultScreen
import com.tuhoc.phatnguoi.ui.result.ResultScreenData
import com.tuhoc.phatnguoi.ui.settings.SettingsScreen
import com.tuhoc.phatnguoi.ui.terms.TermsScreen
import com.tuhoc.phatnguoi.ui.theme.PhatNguoiTheme
import com.tuhoc.phatnguoi.ui.theme.RedPrimary
import com.tuhoc.phatnguoi.ui.theme.RedAccent
import com.tuhoc.phatnguoi.ui.theme.TextPrimary
import com.tuhoc.phatnguoi.ui.theme.TextSub
import com.tuhoc.phatnguoi.ui.theme.WarningRed
import com.tuhoc.phatnguoi.utils.AutoCheckScheduler
import com.tuhoc.phatnguoi.utils.NotificationHelper
import com.tuhoc.phatnguoi.utils.PermissionHelper
import com.tuhoc.phatnguoi.viewmodel.TraCuuViewModel
import com.tuhoc.phatnguoi.viewmodel.UiState as TraCuuUiState
import com.tuhoc.phatnguoi.data.firebase.FirebaseInitHelper
import kotlinx.coroutines.delay

/* ===========================================================
 * Main Activity
 * =========================================================== */

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Tự động kiểm tra và khởi tạo Firestore database khi app khởi động
        FirebaseInitHelper.checkDatabaseInitialized { isInitialized ->
            if (!isInitialized) {
                FirebaseInitHelper.initDatabase(this) { success, error ->
                    if (success) {
                        android.util.Log.d("MainActivity", "✅ Firestore database đã được khởi tạo tự động!")
                    } else {
                        android.util.Log.w("MainActivity", "⚠️ Chưa thể khởi tạo Firestore database: $error")
                        // Không hiển thị lỗi cho user, chỉ log - app vẫn hoạt động bình thường
                    }
                }
            } else {
                android.util.Log.d("MainActivity", "✅ Firestore database đã được khởi tạo từ trước")
            }
        }

        setContent {
            PhatNguoiTheme {
                AppNav(
                    initialIntent = intent
                )
            }
        }
    }
}

/* ===========================================================
 * Navigation
 * =========================================================== */

@Composable
fun AppNav(
    initialIntent: Intent? = null
) {
    val nav = rememberNavController()

    // Kiểm tra Intent từ notification
    val fromNotification = initialIntent?.getBooleanExtra(NotificationHelper.EXTRA_FROM_NOTIFICATION, false) ?: false
    val bienSoFromNotification = initialIntent?.getStringExtra(NotificationHelper.EXTRA_BIEN_SO)
    val loaiXeFromNotification = initialIntent?.getIntExtra(NotificationHelper.EXTRA_LOAI_XE, -1)?.takeIf { it != -1 }

    NavHost(
        navController = nav,
        startDestination = "home"
    ) {
        composable("home") {
            MainTabsScreen(
                fromNotification = fromNotification,
                bienSoFromNotification = bienSoFromNotification,
                loaiXeFromNotification = loaiXeFromNotification
            )
        }
    }
}

/* ===========================================================
 * Bottom tabs
 * =========================================================== */

sealed class BottomItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    object TraCuu : BottomItem("tab_tracuu", "Tra cứu", Icons.Filled.Search)
    object LichSu : BottomItem("tab_lichsu", "Lịch sử", Icons.Filled.History)
    object TinTuc : BottomItem("tab_tintuc", "Tin tức", Icons.Filled.Article)
    object CaiDat : BottomItem("tab_caidat", "Cài đặt", Icons.Filled.Settings)
}

@Composable
fun MainTabsScreen(
    fromNotification: Boolean = false,
    bienSoFromNotification: String? = null,
    loaiXeFromNotification: Int? = null
) {
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    val historyManager = remember { HistoryManager(context) }
    val newsManager = remember { NewsManager() }

    val scope = rememberCoroutineScope()
    var current by remember { mutableStateOf<BottomItem>(BottomItem.TraCuu) }
    var showLoginDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var forgotPasswordPhoneNumber by remember { mutableStateOf("") }
    var forgotPasswordInitialStep by remember { mutableStateOf(1) }
    var forgotPasswordFromChangePassword by remember { mutableStateOf(false) } // Track xem quên mật khẩu được mở từ đổi mật khẩu không
    var showFAQDialog by remember { mutableStateOf(false) }
    var showContactDialog by remember { mutableStateOf(false) }
    var showPolicyDialog by remember { mutableStateOf(false) }
    var showTermsDialog by remember { mutableStateOf(false) }
    var showNotificationSettingsDialog by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf(authManager.isLoggedInSync()) }

    // State để hiển thị AI screen
    var showAIScreen by remember { mutableStateOf(false) }
    var aiScreenData by remember { mutableStateOf<AIScreenData?>(null) }
    var cachedAnalysisResult by remember { mutableStateOf<com.tuhoc.phatnguoi.utils.FineAnalysisResult?>(null) }
    var aiScreenFromResult by remember { mutableStateOf(false) } // Track xem AI screen được mở từ ResultScreen không

    // State để hiển thị Result screen
    var showResultDialog by remember { mutableStateOf(false) }
    var resultScreenData by remember { mutableStateOf<com.tuhoc.phatnguoi.ui.result.ResultScreenData?>(null) }
    var isResultLoading by remember { mutableStateOf(false) }
    var resetTraCuuCallback by remember { mutableStateOf<(() -> Unit)?>(null) }

    // State để tự động tra cứu khi click vào item lịch sử hoặc từ notification
    var autoTraCuuBienSo by remember { mutableStateOf<String?>(null) }
    var autoTraCuuLoaiXe by remember { mutableStateOf<Int?>(null) }

    // Xử lý notification khi app mở từ notification
    LaunchedEffect(fromNotification, bienSoFromNotification, loaiXeFromNotification) {
        if (fromNotification && !bienSoFromNotification.isNullOrEmpty() && loaiXeFromNotification != null) {
            // Chuyển sang tab Tra cứu
            current = BottomItem.TraCuu

            // Đợi một chút để UI render xong
            delay(500)

            // Set biển số và loại xe để tự động tra cứu
            autoTraCuuBienSo = bienSoFromNotification
            autoTraCuuLoaiXe = loaiXeFromNotification
        }
    }

    // Key để reset TraCuuScreen khi chuyển tab
    var traCuuKey by remember { mutableStateOf(0) }
    var previousTab by remember { mutableStateOf<BottomItem>(BottomItem.TraCuu) }
    
    // Key để reload HistoryScreen khi chuyển tab
    var historyKey by remember { mutableStateOf(0) }

    // Snackbar state
    val snackbarHostState = remember { SnackbarHostState() }

    // Key để trigger reload SettingsScreen
    var settingsReloadKey by remember { mutableStateOf(0) }

    // Request permissions
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, schedule alarm
            val scheduler = AutoCheckScheduler(context)
            scheduler.scheduleNextAutoCheck()
        } else {
            // Permission denied, show message
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Cần cấp quyền thông báo để nhận kết quả tra cứu tự động",
                    duration = SnackbarDuration.Long
                )
            }
        }
    }

    // Load login state from database và check permissions
    LaunchedEffect(Unit) {
        isLoggedIn = authManager.isLoggedIn()

        // Check và request notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!PermissionHelper.hasNotificationPermission(context)) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                return@LaunchedEffect
            }
        }

        // Check SCHEDULE_EXACT_ALARM permission (Android 12+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!PermissionHelper.hasScheduleExactAlarmPermission(context)) {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Cần cấp quyền đặt lịch chính xác trong Settings",
                        duration = SnackbarDuration.Long,
                        actionLabel = "Mở Settings"
                    )
                    // Có thể thêm action để mở Settings
                    PermissionHelper.openScheduleExactAlarmSettings(context)
                }
                return@LaunchedEffect
            }
        }

        // Schedule auto check alarm khi app khởi động và có đủ permissions
        val scheduler = AutoCheckScheduler(context)
        scheduler.scheduleNextAutoCheck()
    }

    // Reset kết quả tra cứu khi chuyển sang tab khác
    LaunchedEffect(current) {
        if (previousTab == BottomItem.TraCuu && current != BottomItem.TraCuu) {
            // Đã chuyển từ tab Tra cứu sang tab khác -> tăng key để reset khi quay lại
            traCuuKey++
        }
        if (current == BottomItem.LichSu && previousTab != BottomItem.LichSu) {
            // Đã chuyển sang tab Lịch sử -> tăng key để reload lịch sử
            historyKey++
        }
        previousTab = current
    }

    val tabs = listOf(
        BottomItem.TraCuu,
        BottomItem.LichSu,
        BottomItem.TinTuc,
        BottomItem.CaiDat
    )

    Scaffold(
        topBar = {
            if (!showAIScreen) {
                AppHeader(current)
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        bottomBar = {
            NavigationBar {
                tabs.forEach { item ->
                    NavigationBarItem(
                        selected = current.route == item.route,
                        onClick = {
                            // Cho phép chuyển tab, tab Lịch sử sẽ tự hiển thị thông báo nếu chưa đăng nhập
                            current = item
                        },
                        icon = {
                            Icon(
                                item.icon,
                                contentDescription = item.label
                            )
                        },
                        label = {
                            Text(
                                item.label,
                                color = if (current.route == item.route)
                                    RedPrimary       // tab đang chọn = đỏ brand
                                else
                                    TextSub
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = RedPrimary,
                            selectedTextColor = RedPrimary,
                            indicatorColor = RedPrimary.copy(alpha = 0.08f)
                        )
                    )
                }
            }
        }
    ) { inner ->
        Box(Modifier.padding(inner)) {
            when (current) {
                is BottomItem.TraCuu -> TraCuuScreen(
                    historyManager = historyManager,
                    autoBienSo = autoTraCuuBienSo,
                    autoLoaiXe = autoTraCuuLoaiXe,
                    onAutoTraCuuDone = {
                        // Reset sau khi tra cứu xong
                        autoTraCuuBienSo = null
                        autoTraCuuLoaiXe = null
                    },
                    key = traCuuKey,
                    showAIScreen = showAIScreen,
                    aiScreenData = aiScreenData,
                    onShowAIScreen = { data ->
                        showAIScreen = true
                        aiScreenData = data
                    },
                    onHideAIScreen = {
                        showAIScreen = false
                        aiScreenData = null
                        aiScreenFromResult = false
                    },
                    onShowResult = { info, pairs, violationsForCalculation ->
                        // Hiển thị kết quả ngay (không đợi AI)
                        val onShowAIScreenCallback: (com.tuhoc.phatnguoi.AIScreenData) -> Unit = { data ->
                            showResultDialog = false
                            showAIScreen = true
                            // Truyền kết quả AI đã tính sẵn vào AIScreenData (không gọi AI lại)
                            val analysisResult = cachedAnalysisResult
                            android.util.Log.d("MainActivity", "Mở AI screen với analysisResult: ${if (analysisResult != null) "CÓ - KHÔNG gọi AI lại" else "KHÔNG CÓ - sẽ gọi AI"}")
                            aiScreenData = data.copy(analysisResult = analysisResult)
                            aiScreenFromResult = true // Đánh dấu AI screen được mở từ ResultScreen
                        }
                        
                        resultScreenData = ResultScreenData(
                            info = info,
                            pairs = pairs,
                            onShowAIScreen = onShowAIScreenCallback
                        )
                        isResultLoading = false
                        showResultDialog = true
                        
                        // Bắt đầu tính toán AI ở background nếu có vi phạm chưa xử lý
                        // Chỉ dùng 1 lệnh calculateFineAnalysis để lấy JSON đầy đủ
                        // Lưu kết quả để truyền vào AIFineCalculatorScreen (không gọi AI lại)
                        if (info?.chuaxuphat ?: 0 > 0 && violationsForCalculation.isNotEmpty()) {
                            scope.launch {
                                try {
                                    val fineCalculator = com.tuhoc.phatnguoi.utils.AIFineCalculator()
                                    // Gọi AI một lần duy nhất, trả về JSON đầy đủ
                                    val analysisResult = fineCalculator.calculateFineAnalysis(violationsForCalculation)
                                    
                                    if (analysisResult != null) {
                                        // Lưu kết quả vào state để truyền vào AIFineCalculatorScreen
                                        cachedAnalysisResult = analysisResult
                                        
                                        val tongTienPhatRange = analysisResult.totalFineRange
                                        val tongTienPhat = tongTienPhatRange.average
                                        
                                        // Cập nhật resultScreenData với kết quả AI
                                        val updatedInfo = info?.let {
                                            com.tuhoc.phatnguoi.data.remote.DataInfo(
                                                total = it.total,
                                                chuaxuphat = it.chuaxuphat,
                                                daxuphat = it.daxuphat,
                                                tongTienPhat = tongTienPhat,
                                                tongTienPhatRange = tongTienPhatRange,
                                                latest = it.latest
                                            )
                                        }
                                        android.util.Log.d("PhatNguoi", "AI tính toán xong: $tongTienPhatRange")
                                        resultScreenData = ResultScreenData(
                                            info = updatedInfo,
                                            pairs = pairs,
                                            onShowAIScreen = onShowAIScreenCallback
                                        )
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("PhatNguoi", "Lỗi khi tính tiền phạt với Gemini AI", e)
                                    // Không hiển thị lỗi, chỉ log
                                }
                            }
                        }
                    },
                    onStartSearch = {
                        // Mở dialog ngay khi bắt đầu tra cứu
                        isResultLoading = true
                        showResultDialog = true
                        resultScreenData = null
                    },
                    isResultDialogOpen = showResultDialog,
                    onCloseResultDialog = {
                        // Reset màn hình tra cứu trước
                        resetTraCuuCallback?.invoke()
                        // Đợi một chút để UI cập nhật (hiển thị hướng dẫn)
                        scope.launch {
                            kotlinx.coroutines.delay(200)
                            // Sau đó mới đóng dialog
                            showResultDialog = false
                            isResultLoading = false
                            resultScreenData = null
                        }
                    },
                    onRegisterResetCallback = { callback ->
                        resetTraCuuCallback = callback
                    }
                )
                is BottomItem.LichSu -> {
                    if (isLoggedIn) {
                        HistoryScreen(
                            historyManager = historyManager,
                            key = historyKey, // Trigger reload khi key thay đổi
                            onItemClick = { bienSo, loaiXe ->
                                // Mở dialog kết quả ngay
                                isResultLoading = true
                                showResultDialog = true
                                resultScreenData = null
                                
                                // Lưu biển số và loại xe để tự động tra cứu
                                autoTraCuuBienSo = bienSo
                                autoTraCuuLoaiXe = loaiXe
                                // Chuyển sang tab Tra cứu
                                current = BottomItem.TraCuu
                            }
                        )
                    } else {
                        // Màn hình yêu cầu đăng nhập đơn giản
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFF5F5F5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Vui lòng đăng nhập",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )

                                Spacer(Modifier.height(12.dp))

                                Text(
                                    text = "Đăng nhập để xem lịch sử tra cứu và quản lý các biển số đã tra",
                                    fontSize = 14.sp,
                                    color = TextSub,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
                is BottomItem.TinTuc -> NewsScreen(newsManager = newsManager)
                is BottomItem.CaiDat -> SettingsScreen(
                    authManager = authManager,
                    onLogout = {
                        isLoggedIn = false
                        settingsReloadKey++ // Trigger reload
                        if (current == BottomItem.LichSu) {
                            current = BottomItem.TraCuu
                        }
                        // Hiển thị thông báo đăng xuất thành công
                        scope.launch {
                            snackbarHostState.showSnackbar("Bạn đã đăng xuất!")
                        }
                    },
                    onLoginClick = {
                        showLoginDialog = true
                    },
                    onItemClick = { title ->
                        when (title) {
                            "Câu hỏi thường gặp" -> {
                                showFAQDialog = true
                            }
                            "Đổi mật khẩu" -> {
                                showChangePasswordDialog = true
                            }
                            "Nộp phạt trực tuyến" -> {
                                val url = "https://dichvucong.gov.vn/p/home/dvc-thanh-toan-vi-pham-giao-thong.html"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            }
                            "Cài đặt thông báo" -> {
                                showNotificationSettingsDialog = true
                            }
                            "Chia sẻ ứng dụng" -> {
                                try {
                                    val packageManager = context.packageManager
                                    val appName = packageManager.getApplicationLabel(
                                        packageManager.getApplicationInfo(context.packageName, 0)
                                    ).toString()

                                    val shareText = """
                                        $appName
                                        
                                        Ứng dụng tra cứu phạt nguội giao thông tiện lợi.
                                        Tra cứu nhanh chóng, chính xác thông tin vi phạm giao thông từ Cục Cảnh sát giao thông.
                                        
                                        Tải ngay tại Google Play Store!
                                    """.trimIndent()

                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, appName)
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }

                                    val chooser = Intent.createChooser(intent, "Chia sẻ ứng dụng")
                                    context.startActivity(chooser)
                                } catch (e: Exception) {
                                    // Fallback nếu có lỗi
                                    val shareText = """
                                        Tra Cứu Phạt Nguội
                                        
                                        Ứng dụng tra cứu phạt nguội giao thông tiện lợi.
                                        Tra cứu nhanh chóng, chính xác thông tin vi phạm giao thông từ Cục Cảnh sát giao thông.
                                        
                                        Tải ngay tại Google Play Store!
                                    """.trimIndent()

                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Tra Cứu Phạt Nguội")
                                        putExtra(Intent.EXTRA_TEXT, shareText)
                                    }

                                    val chooser = Intent.createChooser(intent, "Chia sẻ ứng dụng")
                                    context.startActivity(chooser)
                                }
                            }
                            "Liên hệ" -> {
                                showContactDialog = true
                            }
                            "Chính sách bảo mật" -> {
                                showPolicyDialog = true
                            }
                            "Điều khoản sử dụng" -> {
                                showTermsDialog = true
                            }
                        }
                    },
                    key = settingsReloadKey
                )
            }
        }
    }

    // Dialog đăng nhập
    if (showLoginDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { showLoginDialog = false },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                LoginScreen(
                    onLoginSuccess = { isNewAccount ->
                        showLoginDialog = false
                        // Update login state in background
                        scope.launch {
                            isLoggedIn = authManager.isLoggedIn()
                            // Trigger reload SettingsScreen
                            settingsReloadKey++
                            // Hiển thị thông báo thành công
                            val message = if (isNewAccount) {
                                "Đăng ký thành công!"
                            } else {
                                "Đăng nhập thành công!"
                            }
                            snackbarHostState.showSnackbar(message)
                            // Giữ ở tab cài đặt (không chuyển tab)
                        }
                    },
                    onCancel = { showLoginDialog = false },
                    onForgotPassword = { phone ->
                        showLoginDialog = false
                        // Mở màn hình quên mật khẩu ngay, OTP sẽ được gửi tự động trong ForgotPasswordScreen
                        forgotPasswordPhoneNumber = phone
                        forgotPasswordInitialStep = 2 // Bắt đầu từ bước nhập OTP
                        forgotPasswordFromChangePassword = false // Mở từ đăng nhập
                        showForgotPasswordDialog = true
                    }
                )
            }
        }
    }

    // Dialog đổi mật khẩu
    if (showChangePasswordDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { showChangePasswordDialog = false },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                ChangePasswordScreen(
                    onSuccess = {
                        showChangePasswordDialog = false
                        scope.launch {
                            // Trigger reload SettingsScreen
                            settingsReloadKey++
                            snackbarHostState.showSnackbar("Đổi mật khẩu thành công!")
                        }
                    },
                    onCancel = { showChangePasswordDialog = false },
                    onForgotPassword = { phone ->
                        showChangePasswordDialog = false
                        forgotPasswordPhoneNumber = phone
                        forgotPasswordInitialStep = 2
                        forgotPasswordFromChangePassword = true // Đánh dấu là mở từ đổi mật khẩu
                        showForgotPasswordDialog = true
                    }
                )
            }
        }
    }

    // Dialog quên mật khẩu
    if (showForgotPasswordDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { 
                    showForgotPasswordDialog = false
                    forgotPasswordPhoneNumber = ""
                    forgotPasswordInitialStep = 1
                    forgotPasswordFromChangePassword = false
                },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                ForgotPasswordScreen(
                    onSuccess = {
                        showForgotPasswordDialog = false
                        forgotPasswordPhoneNumber = ""
                        forgotPasswordInitialStep = 1
                        forgotPasswordFromChangePassword = false
                        scope.launch {
                            snackbarHostState.showSnackbar("Đặt lại mật khẩu thành công!")
                        }
                    },
                    onCancel = {
                        showForgotPasswordDialog = false
                        forgotPasswordPhoneNumber = ""
                        forgotPasswordInitialStep = 1
                        // Kiểm tra xem được mở từ đâu để quay lại đúng dialog
                        if (forgotPasswordFromChangePassword) {
                            // Quay lại dialog đổi mật khẩu
                            forgotPasswordFromChangePassword = false
                            showChangePasswordDialog = true
                        } else {
                            // Quay lại dialog đăng nhập
                            showLoginDialog = true
                        }
                    },
                    initialPhoneNumber = forgotPasswordPhoneNumber,
                    initialStep = forgotPasswordInitialStep
                )
            }
        }
    }

    // Dialog Câu hỏi thường gặp
    if (showFAQDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { showFAQDialog = false },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    // Header với nút đóng
                    DialogHeader(
                        title = "Hỏi đáp",
                        onClose = { showFAQDialog = false }
                    )

                    // Nội dung FAQ
                    FAQScreen()
                }
            }
        }
    }

    // Dialog Liên hệ
    if (showContactDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { showContactDialog = false },
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
                        text = "Liên hệ với chúng tôi qua email",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = "tracuuphatnguoi@gmail.com",
                        fontSize = 16.sp,
                        color = RedPrimary,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Nút Đóng
                        Button(
                            onClick = { showContactDialog = false },
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
                                "Đóng",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Nút Gửi email
                        Button(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = Uri.parse("mailto:")
                                        putExtra(Intent.EXTRA_EMAIL, arrayOf("tracuuphatnguoi@gmail.com"))
                                        putExtra(Intent.EXTRA_SUBJECT, "Liên hệ từ ứng dụng Tra Cứu Phạt Nguội")
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Gửi email"))
                                    showContactDialog = false
                                } catch (e: Exception) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Không tìm thấy ứng dụng email")
                                    }
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
                                "Gửi email",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialog Chính sách bảo mật
    if (showPolicyDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { showPolicyDialog = false },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    // Header với nút đóng
                    DialogHeader(
                        title = "Chính sách bảo mật",
                        onClose = { showPolicyDialog = false }
                    )

                    // Nội dung Policy
                    PolicyScreen()
                }
            }
        }
    }

    // Dialog Điều khoản sử dụng
    if (showTermsDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { showTermsDialog = false },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    // Header với nút đóng
                    DialogHeader(
                        title = "Điều khoản sử dụng",
                        onClose = { showTermsDialog = false }
                    )

                    // Nội dung Terms
                    TermsScreen()
                }
            }
        }
    }

    // Dialog Cài đặt thông báo
    if (showNotificationSettingsDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { showNotificationSettingsDialog = false },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    // Header với nút đóng
                    DialogHeader(
                        title = "Cài đặt thông báo",
                        onClose = { showNotificationSettingsDialog = false }
                    )

                    // Nội dung Notification Settings
                    NotificationSettingsScreen(
                        onLoginClick = {
                            showNotificationSettingsDialog = false
                            showLoginDialog = true
                        },
                        isLoggedInInitial = isLoggedIn, // Truyền trạng thái đăng nhập hiện tại để tránh flicker
                        key = settingsReloadKey // Reload khi đăng nhập/đăng xuất
                    )
                }
            }
        }
    }

    // Dialog Kết quả tra cứu
    if (showResultDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { 
                    // Reset màn hình tra cứu trước
                    resetTraCuuCallback?.invoke()
                    // Đợi một chút để UI cập nhật (hiển thị hướng dẫn)
                    scope.launch {
                        kotlinx.coroutines.delay(200)
                        // Sau đó mới đóng dialog
                        showResultDialog = false
                        isResultLoading = false
                        resultScreenData = null
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    // Header với nút đóng
                    DialogHeader(
                        title = "Kết quả tra cứu vi phạm",
                        subtitle = null,
                        onClose = { 
                            // Reset màn hình tra cứu trước
                            resetTraCuuCallback?.invoke()
                            // Đợi một chút để UI cập nhật (hiển thị hướng dẫn)
                            scope.launch {
                                kotlinx.coroutines.delay(200)
                                // Sau đó mới đóng dialog
                                showResultDialog = false
                                isResultLoading = false
                                resultScreenData = null
                            }
                        }
                    )

                    // Hiển thị loading hoặc kết quả
                    if (isResultLoading || resultScreenData == null) {
                        // Hiển thị loading trong dialog
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = RedPrimary)
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Đang tra cứu…",
                                    color = TextPrimary,
                                    fontSize = 16.sp
                                )
                            }
                        }
                    } else {
                        // Nội dung Result
                        resultScreenData?.let { data ->
                            ResultScreen(data = data)
                        }
                    }
                }
            }
        }
    }

    // Dialog AI phân tích
    if (showAIScreen && aiScreenData != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { 
                    showAIScreen = false
                    aiScreenData = null
                    // Nếu AI screen được mở từ ResultScreen, mở lại ResultScreen
                    if (aiScreenFromResult) {
                        showResultDialog = true
                        aiScreenFromResult = false
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(0.dp)
                    .clickable(enabled = false) {},
                shape = RoundedCornerShape(0.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column {
                    // Header với nút đóng
                    DialogHeader(
                        title = "Chi tiết mức phạt",
                        onClose = { 
                            showAIScreen = false
                            aiScreenData = null
                            // Nếu AI screen được mở từ ResultScreen, mở lại ResultScreen
                            if (aiScreenFromResult) {
                                showResultDialog = true
                                aiScreenFromResult = false
                            }
                        }
                    )

                    // Nội dung AI Fine Calculator - chỉ hiển thị nội dung, không có header
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        com.tuhoc.phatnguoi.ui.ai.AIFineCalculatorScreen(
                            totalViolations = aiScreenData!!.totalViolations,
                            unresolvedViolations = aiScreenData!!.unresolvedViolations,
                            violations = aiScreenData!!.violations,
                            preCalculatedAnalysis = aiScreenData!!.analysisResult, // Truyền kết quả đã tính sẵn
                            onBack = {
                                showAIScreen = false
                                aiScreenData = null
                                cachedAnalysisResult = null // Xóa cache khi đóng màn hình
                                // Nếu AI screen được mở từ ResultScreen, mở lại ResultScreen
                                if (aiScreenFromResult) {
                                    showResultDialog = true
                                    aiScreenFromResult = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/* ===========================================================
 * Header chung cho dialog
 * =========================================================== */

@Composable
fun DialogHeader(
    title: String,
    onClose: () -> Unit,
    subtitle: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(RedPrimary)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onClose,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Đóng",
                    tint = Color.White
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
            // Spacer để căn giữa
            Spacer(modifier = Modifier.width(40.dp))
        }
    }
}

/* ===========================================================
 * Header chung cho mọi tab
 * =========================================================== */

@Composable
fun AppHeader(current: BottomItem) {
    val (title, subtitle) = when (current) {
        is BottomItem.TraCuu ->
            "Tra Cứu Phạt Nguội" to "Nguồn tra: Từ Cục Cảnh sát giao thông"

        is BottomItem.LichSu ->
            "Lịch sử tra cứu" to "Các biển số đã tra gần đây"

        is BottomItem.TinTuc ->
            "Tin tức giao thông" to "Cập nhật thông tin mới"

        is BottomItem.CaiDat ->
            "Cài đặt" to "Tùy chỉnh ứng dụng"
    }

    Surface(
        color = RedPrimary,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

/* ===========================================================
 * Loại xe
 * =========================================================== */

enum class VehicleType(val apiValue: Int, val label: String) {
    OTO(1, "Ô tô"),
    XE_MAY(2, "Xe máy"),
    XE_MAY_DIEN(3, "Xe đạp điện")
}

@Composable
fun VehicleTypeSelector(
    selected: VehicleType,
    onSelect: (VehicleType) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
        ) {
            VehicleType.values().forEach { type ->
                val isSelected = type == selected

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (isSelected) RedPrimary.copy(alpha = 0.08f)
                            else Color.Transparent
                        )
                        .clickable { onSelect(type) }
                ) {
                    Text(
                        text = type.label,
                        fontSize = 14.sp,
                        color = if (isSelected) RedPrimary else TextSub,
                        modifier = Modifier.align(Alignment.Center),
                        fontWeight = FontWeight.Bold
                    )

                    // gạch đỏ dưới chân tab đang chọn
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .height(2.dp)
                            .fillMaxWidth()
                            .background(
                                if (isSelected) RedPrimary
                                else Color.Transparent
                            )
                    )
                }
            }
        }

        // đường kẻ mờ dưới hàng tab
        Divider(
            color = Color(0x11000000),
            thickness = 1.dp
        )
    }
}

/* ===========================================================
 * Màn Tra cứu
 * =========================================================== */

@Composable
fun TraCuuScreen(
    historyManager: HistoryManager? = null,
    autoBienSo: String? = null,
    autoLoaiXe: Int? = null,
    onAutoTraCuuDone: () -> Unit = {},
    key: Int = 0,
    showAIScreen: Boolean = false,
    aiScreenData: AIScreenData? = null,
    onShowAIScreen: (AIScreenData) -> Unit = {},
    onHideAIScreen: () -> Unit = {},
    onShowResult: (com.tuhoc.phatnguoi.data.remote.DataInfo?, List<Pair<String, String>>, List<Map<String, Any>>) -> Unit = { _, _, _ -> },
    onStartSearch: () -> Unit = {},
    isResultDialogOpen: Boolean = false,
    onCloseResultDialog: () -> Unit = {},
    onRegisterResetCallback: ((() -> Unit) -> Unit)? = null
) {
    val vm: TraCuuViewModel = viewModel()
    val uiState by vm.state.collectAsState()

    var plate by remember { mutableStateOf("") }
    var vehicleType by remember { mutableStateOf(VehicleType.OTO) }
    
    // Đăng ký callback để reset khi đóng dialog
    LaunchedEffect(Unit) {
        onRegisterResetCallback?.invoke {
            vm.reset()
            plate = ""
            vehicleType = VehicleType.OTO
        }
    }

    // Track xem đã lưu lịch sử cho kết quả hiện tại chưa
    var lastSavedKey by remember { mutableStateOf<String?>(null) }

    // Reset kết quả tra cứu khi key thay đổi (chuyển tab)
    LaunchedEffect(key) {
        if (key > 0) {
            // Chỉ reset khi key > 0 (không reset lần đầu)
            vm.reset()
            plate = ""
            vehicleType = VehicleType.OTO
            lastSavedKey = null
        }
    }

    // Tự động tra cứu khi có giá trị từ lịch sử
    LaunchedEffect(autoBienSo, autoLoaiXe) {
        if (autoBienSo != null && autoLoaiXe != null) {
            // Set biển số và loại xe vào input
            plate = autoBienSo
            vehicleType = when (autoLoaiXe) {
                1 -> VehicleType.OTO
                2 -> VehicleType.XE_MAY
                3 -> VehicleType.XE_MAY_DIEN
                else -> VehicleType.OTO
            }

            // Đợi một chút để UI cập nhật và dialog mở
            kotlinx.coroutines.delay(300)

            // Tự động tra cứu (dialog đã được mở từ HistoryScreen)
            vm.traCuuBienSo(
                bienSoRaw = autoBienSo,
                loaiXe = autoLoaiXe
            )

            // Gọi callback khi xong
            onAutoTraCuuDone()
        }
    }

    // Lưu lịch sử khi tra cứu thành công (chỉ lưu một lần cho mỗi kết quả)
    LaunchedEffect(uiState, plate, vehicleType) {
        if (uiState is TraCuuUiState.Success && historyManager != null && plate.isNotEmpty()) {
            val successState = uiState as TraCuuUiState.Success
            val loaiXeLabel = when (vehicleType) {
                VehicleType.OTO -> "Ô tô"
                VehicleType.XE_MAY -> "Xe máy"
                VehicleType.XE_MAY_DIEN -> "Xe đạp điện"
            }

            // Tạo key duy nhất cho kết quả này (biển số + loại xe + số lỗi)
            val currentKey = "${plate}_${vehicleType}_${successState.info?.total}_success"

            // Chỉ lưu nếu chưa lưu cho key này
            if (currentKey != lastSavedKey) {
                historyManager.addHistory(
                    TraCuuHistoryItem(
                        bienSo = plate,
                        loaiXe = loaiXeLabel,
                        thoiGian = System.currentTimeMillis(),
                        coViPham = true,
                        soLoi = successState.info?.total
                    )
                )
                lastSavedKey = currentKey
            }
        } else if (uiState is TraCuuUiState.Error && historyManager != null) {
            // Kiểm tra xem có phải là "Không tìm thấy vi phạm" không
            val errorState = uiState as TraCuuUiState.Error
            if (errorState.message.contains("Không tìm thấy", ignoreCase = true) && plate.isNotEmpty()) {
                val loaiXeLabel = when (vehicleType) {
                    VehicleType.OTO -> "Ô tô"
                    VehicleType.XE_MAY -> "Xe máy"
                    VehicleType.XE_MAY_DIEN -> "Xe đạp điện"
                }

                // Tạo key duy nhất cho kết quả này
                val currentKey = "${plate}_${vehicleType}_no_violation"

                // Chỉ lưu nếu chưa lưu cho key này
                if (currentKey != lastSavedKey) {
                    historyManager.addHistory(
                        TraCuuHistoryItem(
                            bienSo = plate,
                            loaiXe = loaiXeLabel,
                            thoiGian = System.currentTimeMillis(),
                            coViPham = false
                        )
                    )
                    lastSavedKey = currentKey
                }
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
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(32.dp))

            // ------- Card nhập & nút -------

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                VehicleTypeSelector(
                    selected = vehicleType,
                    onSelect = { vehicleType = it }
                )

                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier.padding(
                        start = 16.dp,
                        end = 16.dp,
                        top = 4.dp,
                        bottom = 20.dp
                    )
                ) {
                    OutlinedTextField(
                        value = plate,
                        onValueChange = {
                            plate = it.uppercase().replace(" ", "")
                        },
                        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
                        placeholder = {
                            Box(
                                Modifier.fillMaxWidth(),
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
                                "Lưu ý: Nhập biển số liền mạch, không chứa ký tự đặc biệt",
                                color = TextSub,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedAccent,
                            unfocusedBorderColor = TextSub,
                            cursorColor = RedPrimary,
                            focusedLabelColor = RedAccent,
                            unfocusedLabelColor = TextSub,
                            focusedPlaceholderColor = TextSub,
                            unfocusedPlaceholderColor = TextSub,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            // Reset key để cho phép lưu lịch sử cho lần tra cứu mới
                            lastSavedKey = null
                            
                            // Chuẩn hoá biển số (giống ViewModel)
                            val bienSo = plate.uppercase().replace(" ", "")
                            
                            // Kiểm tra biển số trước khi mở dialog
                            val isValid = when {
                                bienSo.isBlank() -> false
                                bienSo.any { !it.isLetterOrDigit() } -> false
                                bienSo.length !in 5..10 -> false
                                else -> true
                            }
                            
                            if (isValid) {
                                // Chỉ mở dialog khi biển số hợp lệ
                                onStartSearch()
                            }
                            
                            // Gọi ViewModel để tra cứu (ViewModel sẽ xử lý validation và hiển thị lỗi nếu cần)
                            vm.traCuuBienSo(
                                bienSoRaw = plate,
                                loaiXe = vehicleType.apiValue  // 1 / 2 / 3
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RedPrimary,       // 🔴 nút chính
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Tra ngay",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // ------- Khu vực kết quả -------

            when (val s = uiState) {
                TraCuuUiState.Idle -> {
                    Text(
                        "Hướng dẫn tra cứu:",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            StepItem(
                                1,
                                "Nhập biển số liền mạch, không ký tự đặc biệt. Ví dụ: 20C11771"
                            )
                            Spacer(Modifier.height(10.dp))
                            StepItem(
                                2,
                                "Nhấn 'Tra ngay' và đợi kết quả."
                            )
                        }
                    }
                }

                TraCuuUiState.Loading -> {
                    // Loading sẽ hiển thị trong dialog, không hiển thị ở trang tra cứu
                    Spacer(Modifier.height(0.dp))
                }

                is TraCuuUiState.Error -> {
                    // Kiểm tra xem có phải là "Không tìm thấy vi phạm" không
                    val isNoViolation = s.message.contains("Không tìm thấy vi phạm", ignoreCase = true) ||
                                       s.message.contains("không tìm thấy", ignoreCase = true)
                    
                    if (isNoViolation) {
                        // Nếu là "không tìm thấy vi phạm", hiển thị trong ResultScreen
                        var hasShownNoViolation by remember { mutableStateOf(false) }
                        LaunchedEffect(s.message, plate) {
                            if (!hasShownNoViolation && plate.isNotEmpty()) {
                                // Tạo ResultScreenData với thông báo không có vi phạm
                                // Thêm biển số vào pairs để hiển thị trong thông báo
                                val pairsWithBienSo = listOf(
                                    "Biển kiểm soát" to plate
                                )
                                onShowResult(
                                    com.tuhoc.phatnguoi.data.remote.DataInfo(
                                        total = 0,
                                        chuaxuphat = 0,
                                        daxuphat = 0
                                    ),
                                    pairsWithBienSo, // Thêm biển số để hiển thị trong thông báo
                                    emptyList() // Không có vi phạm để tính toán
                                )
                                hasShownNoViolation = true
                            }
                        }
                        
                        // Reset flag khi state thay đổi
                        LaunchedEffect(key) {
                            hasShownNoViolation = false
                        }
                        
                        // Không hiển thị gì ở đây vì đã mở dialog
                        Spacer(Modifier.height(0.dp))
                    } else {
                        // Các lỗi khác (validation, network, etc.) - đóng dialog và hiển thị ở màn hình tra cứu
                        LaunchedEffect(s.message) {
                            if (isResultDialogOpen) {
                                onCloseResultDialog()
                            }
                        }
                        
                        // Hiển thị thông báo lỗi ở màn hình tra cứu
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
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = s.message,
                                    color = WarningRed,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                is TraCuuUiState.Success -> {
                    val info = s.info
                    val rows = s.pairs

                    // Cập nhật dialog kết quả khi có kết quả thành công (chỉ một lần)
                    var hasUpdatedResult by remember { mutableStateOf(false) }
                    LaunchedEffect(s) {
                        if (!hasUpdatedResult) {
                            onShowResult(info, rows, s.violationsForCalculation)
                            hasUpdatedResult = true
                        }
                    }

                    // Reset flag khi state thay đổi
                    LaunchedEffect(key) {
                        hasUpdatedResult = false
                    }

                    // Không hiển thị gì ở đây vì đã mở dialog
                    Spacer(Modifier.height(0.dp))
                }
            }
        }
    }
}

/* ===========================================================
 * Filter Type
 * =========================================================== */

enum class FilterType {
    DaXuPhat,   // Đã xử phạt
    ChuaXuPhat  // Chưa xử phạt
}

/* ===========================================================
 * AI Screen Data
 * =========================================================== */

data class AIScreenData(
    val totalViolations: Int,
    val unresolvedViolations: Int,
    val violations: List<Map<String, Any>>,
    val analysisResult: com.tuhoc.phatnguoi.utils.FineAnalysisResult? = null // Kết quả AI đã tính sẵn
)

/* ===========================================================
 * Helpers
 * =========================================================== */

/**
 * Card tóm tắt vi phạm - Hiển thị số vi phạm và tổng tiền phạt
 */
@Composable
fun ViolationSummaryCard(
    bienSo: String,
    totalViolations: Int,
    unresolvedViolations: Int,
    tongTienPhat: Long,
    tongTienPhatRange: com.tuhoc.phatnguoi.utils.FineAmountRange? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Dòng 1: Tóm tắt vi phạm
            val resolvedViolations = totalViolations - unresolvedViolations
            val (textBeforeBienSo, textAfterBienSo) = when {
                unresolvedViolations > 0 && resolvedViolations > 0 -> {
                    "Biển số " to " đang có $totalViolations vi phạm trong đó $unresolvedViolations vi phạm chưa xử phạt và $resolvedViolations vi phạm đã xử phạt"
                }
                unresolvedViolations == 0 && resolvedViolations > 0 -> {
                    "Biển số " to " đang có $totalViolations vi phạm đã xử phạt"
                }
                unresolvedViolations > 0 && resolvedViolations == 0 -> {
                    "Biển số " to " đang có $totalViolations vi phạm chưa xử phạt"
                }
                else -> {
                    "Biển số " to " đang có $totalViolations vi phạm"
                }
            }
            Text(
                text = buildAnnotatedString {
                    append(textBeforeBienSo)
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(bienSo)
                    }
                    append(textAfterBienSo)
                },
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(8.dp))
            
            // Dòng 2: AI đang tính toán hoặc hiển thị kết quả
            if (unresolvedViolations > 0) {
                if (tongTienPhatRange != null && tongTienPhatRange.isValid) {
                    // Hiển thị kết quả khi AI đã tính xong
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Dòng 1: "Số tiền dự kiến đóng phạt khoảng"
                        Text(
                            text = "Số tiền dự kiến đóng phạt khoảng",
                            color = TextSub,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Spacer(Modifier.height(4.dp))
                        // Dòng 2: "xxx -- xxx"
                        Text(
                            text = tongTienPhatRange.formatRange(),
                            color = RedPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // Hiển thị loading khi AI đang tính toán
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = RedPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "AI đang tính toán số tiền đóng phạt dự kiến",
                            color = TextSub,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
            
            // Nút "Xem chi tiết mức phạt" - chỉ hiển thị khi AI tính xong
            if (tongTienPhatRange != null && tongTienPhatRange.isValid) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onClick,
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
                        text = "Xem chi tiết mức phạt",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun FilterTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(50.dp)
            .background(
                if (isSelected) RedPrimary.copy(alpha = 0.08f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            color = if (isSelected) RedPrimary else TextSub,
            modifier = Modifier.align(Alignment.Center),
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        // gạch đỏ dưới chân tab đang chọn
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .height(2.dp)
                .fillMaxWidth()
                .background(
                    if (isSelected) RedPrimary
                    else Color.Transparent
                )
        )
    }
}

@Composable
fun Placeholder(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(title, color = TextPrimary)
    }
}

@Composable
fun StepItem(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(RedPrimary.copy(alpha = 0.06f)),  // nhẹ tone đỏ
            contentAlignment = Alignment.Center
        ) {
            Text(
                number.toString(),
                color = RedPrimary,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(text, color = TextPrimary)
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text,
        fontWeight = FontWeight.SemiBold,
        color = TextPrimary,
        modifier = Modifier.padding(top = 12.dp)
    )
}

@Composable
fun KeyValueRow(key: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            key,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            modifier = Modifier.weight(1.2f),
            textAlign = TextAlign.End,
            softWrap = true,
            color = TextPrimary
        )
    }
}

@Composable
fun StatusBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    val norm = text.trim().uppercase()
    val isDa = "ĐÃ" in norm || "DA" in norm

    val bg = if (isDa)
        RedPrimary.copy(alpha = 0.12f)
    else
        WarningRed.copy(alpha = 0.10f)

    val fg = if (isDa)
        RedPrimary
    else
        WarningRed

    Surface(
        modifier = modifier,
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, fg)
    ) {
        Text(
            text = norm,
            modifier = Modifier.padding(horizontal =6.dp, vertical = 2.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Hiển thị "Nơi giải quyết vụ việc" */
@Composable
fun ResolvePlaceList(raw: String) {
    val items = remember(raw) {
        val parts = raw.split(Regex("(?=\\d+\\.\\s)"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val out = mutableListOf<Pair<String, String?>>()
        var lastIdx = -1

        parts.forEach { p ->
            if (p.startsWith("Địa chỉ:", ignoreCase = true)) {
                if (lastIdx >= 0) {
                    val (t, _) = out[lastIdx]
                    out[lastIdx] = t to p.removePrefix("Địa chỉ:").trim()
                } else {
                    out += "Địa chỉ" to p.removePrefix("Địa chỉ:").trim()
                }
            } else {
                out += p to null
                lastIdx = out.lastIndex
            }
        }
        out
    }

    Column {
        if (items.isEmpty()) {
            Text(
                text = raw, 
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        } else {
            items.forEach { (title, addr) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                ) {
                    Text(
                        text = title, 
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
                addr?.let {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, bottom = 6.dp)
                    ) {
                        Text(
                            text = "Địa chỉ: ", 
                            color = TextSub,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                        Text(
                            text = it, 
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusRow(key: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            key,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(12.dp))
        Box(
            modifier = Modifier.weight(1.2f),
            contentAlignment = Alignment.CenterEnd
        ) {
            StatusBadge(value)
        }
    }
}

/* ===========================================================
 * Divider custom
 * =========================================================== */

@Composable
fun Divider(
    modifier: Modifier = Modifier,
    color: Color = Color(0x22000000),
    thickness: Dp = 0.7.dp
) {
    val targetThickness =
        if (thickness == Dp.Hairline)
            (1f / LocalDensity.current.density).dp
        else thickness

    Box(
        Modifier
            .then(modifier)
            .fillMaxWidth()
            .height(targetThickness)
            .background(color)
    )
}
