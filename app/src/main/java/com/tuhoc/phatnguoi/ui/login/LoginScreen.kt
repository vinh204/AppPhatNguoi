package com.tuhoc.phatnguoi.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuhoc.phatnguoi.data.local.AuthManager
import com.tuhoc.phatnguoi.data.remote.OtpService
import com.tuhoc.phatnguoi.security.PinStrengthChecker
import com.tuhoc.phatnguoi.security.SecurityConfig
import com.tuhoc.phatnguoi.ui.login.PinStrengthMessages
import com.tuhoc.phatnguoi.ui.login.checkPinStrengthRealTime
import com.tuhoc.phatnguoi.ui.login.validatePinOnSubmit
import com.tuhoc.phatnguoi.security.InputValidator
import com.tuhoc.phatnguoi.ui.theme.RedPrimary
import com.tuhoc.phatnguoi.ui.theme.TextPrimary
import com.tuhoc.phatnguoi.ui.theme.TextSub
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Constants - Sử dụng config từ SecurityConfig
private val PIN_LENGTH = SecurityConfig.Password.PIN_LENGTH
private val OTP_LENGTH = SecurityConfig.OTP.LENGTH
private val MIN_PHONE_LENGTH = SecurityConfig.PhoneNumber.MIN_LENGTH
private val MAX_PHONE_LENGTH = SecurityConfig.PhoneNumber.MAX_LENGTH
private const val FOCUS_DELAY_MS = 100L // UI config, không liên quan security

/**
 * Format message rate limit cho OTP verification
 */
private fun formatOtpVerifyRateLimitMessage(remainingSeconds: Int): String {
    val timeString = if (remainingSeconds < 60) {
        "$remainingSeconds giây"
    } else {
        val minutes = remainingSeconds / 60
        val seconds = remainingSeconds % 60
        if (seconds == 0) {
            "$minutes phút"
        } else {
            "$minutes phút $seconds giây"
        }
    }
    return "Xác thực sai nhiều lần. Vui lòng thử lại sau $timeString"
}

/**
 * Custom OTP Input với 4 ô vuông
 */
@Composable
fun OtpInputField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    autoFocus: Boolean = true,
    onComplete: (() -> Unit)? = null,
    length: Int = OTP_LENGTH
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(length) { index ->
                val isFilled = index < value.length
                val isFocused = index == value.length
                
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .border(
                            width = if (isFocused) 2.dp else 1.5.dp,
                            color = if (isFocused) RedPrimary else if (isFilled) TextSub else TextSub.copy(alpha = 0.3f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .background(
                            Color.White,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isFilled) {
                        Text(
                            text = value[index].toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
        
        // BasicTextField ẩn để nhận input
        BasicTextField(
            value = value,
            onValueChange = { newValue ->
                val filteredValue = newValue.filter { it.isDigit() }.take(length)
                onValueChange(filteredValue)
                
                // Tự động chuyển focus khi đủ số
                if (filteredValue.length == length) {
                    onComplete?.invoke()
                }
            },
            modifier = Modifier
                .size(1.dp)
                .alpha(0f)
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            enabled = enabled,
            singleLine = true
        )
    }
    
    // Tự động focus khi component được hiển thị
    LaunchedEffect(enabled, autoFocus) {
        if (enabled && autoFocus) {
            delay(FOCUS_DELAY_MS)
        focusRequester.requestFocus()
        }
    }
}

/**
 * Custom PIN Input với 6 vòng tròn (dùng cho mật khẩu)
 */
@Composable
fun PinInputField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    autoFocus: Boolean = true,
    onComplete: (() -> Unit)? = null,
    length: Int = PIN_LENGTH
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        // Ô chữ nhật chứa các chấm tròn
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.5.dp,
                    color = RedPrimary,
                    shape = RoundedCornerShape(8.dp)
                )
                .background(
                    Color.White,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(length) { index ->
                    val isFilled = index < value.length
                    
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) Color(0xFF757575) else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isFilled) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF757575))
                            )
                        }
                    }
                }
            }
        }
        
        // BasicTextField ẩn để nhận input
        BasicTextField(
            value = value,
            onValueChange = { newValue ->
                val filteredValue = newValue.filter { it.isDigit() }.take(length)
                onValueChange(filteredValue)
                
                // Tự động chuyển focus khi đủ số
                if (filteredValue.length == length) {
                    onComplete?.invoke()
                }
            },
            modifier = Modifier
                .size(1.dp)
                .alpha(0f)
                .focusRequester(focusRequester),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            enabled = enabled,
            singleLine = true
        )
    }
    
    // Tự động focus khi component được hiển thị
    LaunchedEffect(enabled, autoFocus) {
        if (enabled && autoFocus) {
            delay(FOCUS_DELAY_MS)
        focusRequester.requestFocus()
        }
    }
}

/**
 * Password Input Field với OutlinedTextField style nhưng hiển thị chấm tròn bên trong
 */
@Composable
fun PasswordTextFieldWithDots(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "Mật khẩu",
    placeholder: String = "Nhập mật khẩu",
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    length: Int = PIN_LENGTH
) {
    var isFocused by remember { mutableStateOf(false) }
    
    Box(modifier = modifier) {
        // OutlinedTextField với text ẩn và cursor ẩn
        OutlinedTextField(
            value = value,
            onValueChange = { newValue ->
                val filteredValue = newValue.filter { it.isDigit() }.take(length)
                onValueChange(filteredValue)
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = {
                Icon(Icons.Filled.Lock, contentDescription = null)
            },
            singleLine = true,
            enabled = enabled,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { isFocused = it.isFocused },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RedPrimary,
                unfocusedBorderColor = TextSub,
                cursorColor = Color.Transparent, // Ẩn cursor
                focusedTextColor = Color.Transparent,
                unfocusedTextColor = Color.Transparent,
                focusedPlaceholderColor = Color.Transparent, // Ẩn placeholder khi focus
                unfocusedPlaceholderColor = TextSub.copy(alpha = 0.6f) // Hiển thị placeholder khi chưa focus
            )
        )
        
        // Overlay các chấm tròn bên trong - chỉ hiển thị khi có focus hoặc đã có giá trị
        if (isFocused || value.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 52.dp, top = 30.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(length) { index ->
                        val isFilled = index < value.length
                        
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) Color(0xFF757575) else TextSub.copy(alpha = 0.3f)
                                )
                        )
                    }
                }
            }
        }
    }
}

// Validation sử dụng InputValidator trực tiếp cho toàn bộ hệ thống

@Composable
fun LoginScreen(
    onLoginSuccess: (isNewAccount: Boolean) -> Unit,
    onCancel: (() -> Unit)? = null,
    onForgotPassword: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    val otpService = remember { OtpService(context) }
    val scope = rememberCoroutineScope()
    
    // State management
    // Step 1 = nhập số điện thoại
    // Step 2 = xác thực OTP (chỉ cho user mới)
    // Step 3 = nhập/tạo mật khẩu
    var step by remember { mutableStateOf(1) }
    var phoneNumber by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") } // Cho bước tạo mật khẩu
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isExistingUser by remember { mutableStateOf(false) }
    var isOtpSent by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(0) } // Đếm ngược để gửi lại OTP
    var rateLimitMessage by remember { mutableStateOf<String?>(null) } // Message rate limit cho login
    var rateLimitCountdown by remember { mutableStateOf(0) } // Countdown cho rate limit login
    var otpRateLimitMessage by remember { mutableStateOf<String?>(null) } // Message rate limit cho OTP send
    var otpRateLimitCountdown by remember { mutableStateOf(0) } // Countdown cho rate limit OTP send
    var otpVerifyRateLimitMessage by remember { mutableStateOf<String?>(null) } // Message rate limit cho OTP verify
    var otpVerifyRateLimitCountdown by remember { mutableStateOf(0) } // Countdown cho rate limit OTP verify
    var remainingVerifyAttempts by remember { mutableStateOf(3) } // Số lần thử còn lại cho verify OTP
    var remainingLoginAttempts by remember { mutableStateOf<Int?>(null) } // Số lần thử còn lại cho login
    var pinStrengthWarning by remember { mutableStateOf<String?>(null) } // Cảnh báo PIN yếu
    var pinStrengthError by remember { mutableStateOf<String?>(null) } // Lỗi PIN không hợp lệ
    
    // Focus requesters cho PIN/OTP fields
    val otpFocusRequester = remember { FocusRequester() }
    val passwordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }
    
    // Helper function to clear error
    val clearError = { errorMessage = null }
    
    // Countdown timer để gửi lại OTP
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        }
    }
    
    // Countdown timer cho rate limit login
    LaunchedEffect(rateLimitCountdown) {
        if (rateLimitCountdown > 0) {
            delay(1000)
            rateLimitCountdown--
            
            // Cập nhật message nếu vẫn còn countdown
            if (rateLimitCountdown > 0) {
                val rateLimitInfo = authManager.getRateLimitInfo(phoneNumber)
                if (!rateLimitInfo.canProceed) {
                    rateLimitMessage = rateLimitInfo.message
                } else {
                    rateLimitMessage = null
                    rateLimitCountdown = 0
                    // Cập nhật lại số lần thử còn lại khi hết countdown
                    remainingLoginAttempts = authManager.getRemainingAttempts(phoneNumber)
                }
            } else {
                rateLimitMessage = null
                // Cập nhật lại số lần thử còn lại khi hết countdown
                remainingLoginAttempts = authManager.getRemainingAttempts(phoneNumber)
            }
        }
    }
    
    // Countdown timer cho rate limit OTP send
    LaunchedEffect(otpRateLimitCountdown) {
        if (otpRateLimitCountdown > 0) {
            delay(1000)
            otpRateLimitCountdown--
            
            // Cập nhật message nếu vẫn còn countdown
            if (otpRateLimitCountdown > 0) {
                val otpRateLimitInfo = otpService.getRateLimitInfo(phoneNumber)
                if (!otpRateLimitInfo.canSend) {
                    otpRateLimitMessage = otpRateLimitInfo.message
                } else {
                    otpRateLimitMessage = null
                    otpRateLimitCountdown = 0
                }
            } else {
                otpRateLimitMessage = null
            }
        }
    }
    
    // Countdown timer cho rate limit OTP verify
    LaunchedEffect(otpVerifyRateLimitCountdown) {
        if (otpVerifyRateLimitCountdown > 0) {
            delay(1000)
            otpVerifyRateLimitCountdown--
            
            // Cập nhật message nếu vẫn còn countdown
            if (otpVerifyRateLimitCountdown > 0) {
                val verifyRateLimitInfo = otpService.getVerifyRateLimitInfo(phoneNumber)
                if (!verifyRateLimitInfo.canProceed) {
                    // Override message cho OTP verification
                    otpVerifyRateLimitMessage = formatOtpVerifyRateLimitMessage(otpVerifyRateLimitCountdown)
                } else {
                    otpVerifyRateLimitMessage = null
                    otpVerifyRateLimitCountdown = 0
                    remainingVerifyAttempts = otpService.getRemainingVerifyAttempts(phoneNumber)
                }
            } else {
                otpVerifyRateLimitMessage = null
                remainingVerifyAttempts = otpService.getRemainingVerifyAttempts(phoneNumber)
            }
        }
    }
    
    // Kiểm tra rate limit khi phoneNumber thay đổi
    LaunchedEffect(phoneNumber, step) {
        if (phoneNumber.isNotEmpty()) {
            when (step) {
                1 -> {
                    // Kiểm tra OTP rate limit ở step 1
                    val otpRateLimitInfo = otpService.getRateLimitInfo(phoneNumber)
                    if (!otpRateLimitInfo.canSend) {
                        otpRateLimitMessage = otpRateLimitInfo.message
                        otpRateLimitCountdown = otpRateLimitInfo.remainingSeconds
                    } else {
                        otpRateLimitMessage = null
                        otpRateLimitCountdown = 0
                    }
                }
                2 -> {
                    // Kiểm tra OTP verify rate limit ở step 2
                    val verifyRateLimitInfo = otpService.getVerifyRateLimitInfo(phoneNumber)
                    if (!verifyRateLimitInfo.canProceed) {
                        // Override message cho OTP verification
                        otpVerifyRateLimitMessage = formatOtpVerifyRateLimitMessage(verifyRateLimitInfo.remainingSeconds)
                        otpVerifyRateLimitCountdown = verifyRateLimitInfo.remainingSeconds
                    } else {
                        otpVerifyRateLimitMessage = null
                        otpVerifyRateLimitCountdown = 0
                        remainingVerifyAttempts = otpService.getRemainingVerifyAttempts(phoneNumber)
                    }
                }
                3 -> {
                    // Kiểm tra login rate limit ở step 3
                    val rateLimitInfo = authManager.getRateLimitInfo(phoneNumber)
                    if (!rateLimitInfo.canProceed) {
                        rateLimitMessage = rateLimitInfo.message
                        rateLimitCountdown = rateLimitInfo.remainingSeconds
                        remainingLoginAttempts = null
                    } else {
                        rateLimitMessage = null
                        rateLimitCountdown = 0
                        // Cập nhật số lần thử còn lại
                        remainingLoginAttempts = authManager.getRemainingAttempts(phoneNumber)
                    }
                }
            }
        }
    }
    
    // Tự động focus vào field tương ứng khi chuyển step
    LaunchedEffect(step) {
        delay(FOCUS_DELAY_MS)
        when (step) {
            2 -> otpFocusRequester.requestFocus()
            3 -> passwordFocusRequester.requestFocus()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        // Top bar với nút quay lại (hiển thị khi ở bước OTP hoặc nhập mật khẩu)
        if (step == 2 || step == 3) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(
                            width = 1.dp,
                            color = TextSub.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                        .clickable(
                            enabled = !isLoading,
                            onClick = {
                                when (step) {
                                    2 -> {
                                        step = 1
                                        otp = ""
                                        isOtpSent = false
                                        countdown = 0
                                        otpService.clearOtp(phoneNumber)
                                    }
                                    3 -> {
                                        if (!isExistingUser) {
                                            step = 2
                                        } else {
                                            step = 1
                                        }
                                        password = ""
                                        confirmPassword = ""
                                    }
                                }
                                clearError()
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Quay lại",
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Text(
                    text = when (step) {
                        2 -> "Xác thực OTP"
                        3 -> "Nhập mật khẩu"
                        else -> ""
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
        }
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        // Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(RedPrimary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when {
                    step == 1 -> Icons.Filled.Phone
                    step == 2 -> Icons.Filled.Lock
                    else -> Icons.Filled.Lock
                },
                contentDescription = null,
                tint = RedPrimary,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(Modifier.height(24.dp))
        
        Text(
            text = when {
                step == 1 -> "Đăng nhập"
                step == 2 -> "Xác thực OTP"
                step == 3 && isExistingUser -> "Nhập mật khẩu"
                else -> "Tạo mật khẩu"
            },
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        
        Spacer(Modifier.height(8.dp))
        
        // Hiển thị mô tả
        when (step) {
            2 -> {
                Text(
                    text = "Mã OTP đã được gửi đến số điện thoại $phoneNumber",
                    fontSize = 14.sp,
                    color = TextSub,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
            }
            3 -> {
                Text(
                    text = if (isExistingUser) "Nhập mật khẩu để đăng nhập" else "Tạo mật khẩu để bảo vệ tài khoản",
                    fontSize = 14.sp,
                    color = TextSub,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        
        Spacer(Modifier.height(32.dp))
        
        // Form input
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Step 1: Nhập số điện thoại
                if (step == 1) {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { 
                            phoneNumber = it.filter { it.isDigit() }
                            clearError()
                        },
                        label = { Text("Số điện thoại") },
                        placeholder = { Text("Số điện thoại") },
                        leadingIcon = {
                            Icon(Icons.Filled.Phone, contentDescription = null)
                        },
                        singleLine = true,
                        enabled = !isLoading,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RedPrimary,
                            unfocusedBorderColor = TextSub,
                            cursorColor = RedPrimary
                        )
                    )
                    
                    // Hiển thị OTP rate limit message ở step 1
                    otpRateLimitMessage?.let { message ->
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = message,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                
                // Step 2: Xác thực OTP (chỉ cho user mới)
                if (step == 2) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Nhập mã OTP",
                            fontSize = 14.sp,
                            color = TextSub,
                            modifier = Modifier.padding(bottom = 18.dp)
                        )
                        OtpInputField(
                            value = otp,
                            onValueChange = { 
                                otp = it
                                clearError()
                            },
                            enabled = !isLoading && otpVerifyRateLimitCountdown == 0, // Disable khi bị rate limit
                            focusRequester = otpFocusRequester,
                            autoFocus = true,
                            length = OTP_LENGTH
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        // Nút gửi lại OTP (ẩn khi bị rate limit verify)
                        if (otpVerifyRateLimitCountdown == 0) {
                            if (countdown > 0) {
                                Text(
                                    text = "Gửi lại mã sau $countdown giây",
                                    fontSize = 12.sp,
                                    color = TextSub
                                )
                            } else {
                                TextButton(
                                    onClick = {
                                        isLoading = true
                                        scope.launch {
                                            val result = otpService.sendOtp(phoneNumber)
                                            isLoading = false
                                            when (result) {
                                                is com.tuhoc.phatnguoi.data.remote.OTPResult.Success -> {
                                                    isOtpSent = true
                                                    countdown = SecurityConfig.OTP.RESEND_COUNTDOWN_SECONDS.toInt()
                                                    clearError()
                                                    otpRateLimitMessage = null
                                                    otpRateLimitCountdown = 0
                                                }
                                                is com.tuhoc.phatnguoi.data.remote.OTPResult.RateLimited -> {
                                                    // Hiển thị rate limit message
                                                    otpRateLimitMessage = result.rateLimitResult.message
                                                    otpRateLimitCountdown = result.rateLimitResult.remainingSeconds
                                                    errorMessage = null
                                                }
                                                is com.tuhoc.phatnguoi.data.remote.OTPResult.Error -> {
                                                    errorMessage = result.message
                                                    otpRateLimitMessage = null
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isLoading
                                ) {
                                    Text(
                                        "Gửi lại mã OTP",
                                        fontSize = 12.sp,
                                        color = RedPrimary
                                    )
                                }
                            }
                        }
                        
                        // Hiển thị OTP send rate limit message
                        otpRateLimitMessage?.let { message ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = message,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        
                        // Hiển thị OTP verify rate limit message
                        otpVerifyRateLimitMessage?.let { message ->
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = message,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                
                // Step 3: Nhập/Tạo mật khẩu
                if (step == 3) {
                    if (!isExistingUser) {
                        // Tạo mật khẩu mới - có 2 trường
                        PasswordTextFieldWithDots(
                            value = password,
                            onValueChange = { 
                                password = it
                                clearError()
                                
                                // Kiểm tra PIN strength khi đủ 6 chữ số
                                val (warning, error) = checkPinStrengthRealTime(it, phoneNumber, PIN_LENGTH)
                                pinStrengthWarning = warning
                                pinStrengthError = error
                            },
                            label = "Nhập mật khẩu mới",
                            placeholder = "Nhập mật khẩu mới",
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            focusRequester = passwordFocusRequester
                        )
                        
                        // Hiển thị cảnh báo/lỗi PIN strength
                        PinStrengthMessages(
                            warning = pinStrengthWarning,
                            error = pinStrengthError
                        )
                        
                        Spacer(Modifier.height(16.dp))
                        
                        PasswordTextFieldWithDots(
                            value = confirmPassword,
                            onValueChange = { 
                                confirmPassword = it
                                clearError()
                            },
                            label = "Xác nhận mật khẩu",
                            placeholder = "Xác nhận mật khẩu",
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            focusRequester = confirmPasswordFocusRequester
                        )
                    } else {
                        // Nhập mật khẩu đã có - dùng OutlinedTextField với chấm tròn bên trong
                        PasswordTextFieldWithDots(
                            value = password,
                            onValueChange = { 
                                password = it
                                clearError()
                            },
                            label = "Mật khẩu",
                            placeholder = "Nhập mật khẩu",
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            focusRequester = passwordFocusRequester
                        )
                        
                        // Text "Quên mật khẩu?"
                        if (onForgotPassword != null) {
                            Spacer(Modifier.height(0.dp))
                            TextButton(
                                onClick = {
                                    onForgotPassword(phoneNumber)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isLoading,
                                contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Text(
                                        text = "Quên mật khẩu?",
                                        fontSize = 14.sp,
                                        color = Color(0xFF2196F3), // Màu xanh
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Hiển thị rate limit message (không có background)
                rateLimitMessage?.let { message ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = message,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                errorMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                Spacer(Modifier.height(16.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Chỉ hiển thị nút quay lại khi không phải step 2 và step 3 (vì đã có nút mũi tên ở trên)
                    if (step > 1 && step != 2 && step != 3) {
                        // Nút quay lại
                        Button(
                            onClick = {
                                when (step) {
                                    2 -> {
                                        step = 1
                                        otp = ""
                                        isOtpSent = false
                                        countdown = 0
                                        otpService.clearOtp(phoneNumber)
                                    }
                                    3 -> {
                                        if (!isExistingUser) {
                                            step = 2
                                        } else {
                                            step = 1
                                        }
                                        password = ""
                                        confirmPassword = ""
                                    }
                                }
                                clearError()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = TextSub,
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isLoading
                        ) {
                            Text("Quay lại", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    
                    Button(
                        onClick = {
                            when (step) {
                                1 -> {
                                    // Bước 1: Kiểm tra số điện thoại và gửi OTP nếu chưa đăng ký
                                    val phone = phoneNumber.trim()
                                    val normalizedPhone = InputValidator.normalizePhoneNumber(phone)
                                    val validationResult = InputValidator.validatePhoneNumber(normalizedPhone)
                                    val validationError = if (validationResult.isError()) validationResult.getErrorMessage() else null
                                    if (validationError != null) {
                                        errorMessage = validationError
                                    } else {
                                        clearError()
                                        
                                        // Kiểm tra rate limit OTP trước
                                        val otpRateLimitInfo = otpService.getRateLimitInfo(phone)
                                        if (!otpRateLimitInfo.canSend) {
                                            // Bị rate limit -> hiển thị thông báo
                                            otpRateLimitMessage = otpRateLimitInfo.message
                                            otpRateLimitCountdown = otpRateLimitInfo.remainingSeconds
                                            errorMessage = null
                                        } else {
                                            // Không bị rate limit -> tiếp tục logic
                                            otpRateLimitMessage = null
                                            otpRateLimitCountdown = 0
                                            isLoading = true
                                            scope.launch {
                                                val exists = authManager.phoneExists(phone)
                                                isExistingUser = exists
                                                
                                                if (exists) {
                                                    // User đã đăng ký -> chuyển thẳng sang bước nhập mật khẩu
                                                    step = 3
                                                    isLoading = false
                                                } else {
                                                    // User chưa đăng ký -> gửi OTP
                                                    scope.launch {
                                                        val result = otpService.sendOtp(phone)
                                                        when (result) {
                                                            is com.tuhoc.phatnguoi.data.remote.OTPResult.Success -> {
                                                                isOtpSent = true
                                                                countdown = SecurityConfig.OTP.RESEND_COUNTDOWN_SECONDS.toInt()
                                                                step = 2
                                                                isLoading = false
                                                                otpRateLimitMessage = null
                                                                otpRateLimitCountdown = 0
                                                            }
                                                            is com.tuhoc.phatnguoi.data.remote.OTPResult.RateLimited -> {
                                                                // Hiển thị rate limit message
                                                                otpRateLimitMessage = result.rateLimitResult.message
                                                                otpRateLimitCountdown = result.rateLimitResult.remainingSeconds
                                                                errorMessage = null
                                                                isLoading = false
                                                            }
                                                            is com.tuhoc.phatnguoi.data.remote.OTPResult.Error -> {
                                                                errorMessage = result.message
                                                                otpRateLimitMessage = null
                                                                isLoading = false
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                2 -> {
                                    // Bước 2: Xác thực OTP (chỉ cho user mới)
                                    // Kiểm tra rate limit trước
                                    if (otpVerifyRateLimitCountdown > 0) {
                                        // Đang bị rate limit, không cho phép verify
                                        return@Button
                                    }
                                    
                                    if (otp.length != OTP_LENGTH) {
                                        errorMessage = "Vui lòng nhập đầy đủ $OTP_LENGTH số OTP"
                                    } else {
                                        clearError()
                                        isLoading = true
                                        scope.launch {
                                            val isValid = otpService.verifyOtp(phoneNumber, otp)
                                            if (isValid) {
                                                // OTP hợp lệ -> reset rate limit và chuyển sang bước tạo mật khẩu
                                                otpVerifyRateLimitMessage = null
                                                otpVerifyRateLimitCountdown = 0
                                                remainingVerifyAttempts = 3
                                                step = 3
                                                isLoading = false
                                            } else {
                                                // OTP sai - kiểm tra rate limit
                                                val verifyRateLimitInfo = otpService.getVerifyRateLimitInfo(phoneNumber)
                                                if (!verifyRateLimitInfo.canProceed) {
                                                    // Đã bị rate limit
                                                    otpVerifyRateLimitMessage = verifyRateLimitInfo.message
                                                    otpVerifyRateLimitCountdown = verifyRateLimitInfo.remainingSeconds
                                                    errorMessage = null
                                                } else {
                                                    // Còn lần thử - hiển thị số lần thử còn lại
                                                    remainingVerifyAttempts = otpService.getRemainingVerifyAttempts(phoneNumber)
                                                    if (remainingVerifyAttempts > 0) {
                                                        errorMessage = "Mã OTP không đúng hoặc đã hết hạn"
                                                    } else {
                                                        // Hết lần thử - chuyển sang rate limit
                                                        val rateLimitInfo = otpService.getVerifyRateLimitInfo(phoneNumber)
                                                        // Override message cho OTP verification
                                                        otpVerifyRateLimitMessage = formatOtpVerifyRateLimitMessage(rateLimitInfo.remainingSeconds)
                                                        otpVerifyRateLimitCountdown = rateLimitInfo.remainingSeconds
                                                        errorMessage = null
                                                    }
                                                }
                                                isLoading = false
                                            }
                                        }
                                    }
                                }
                                3 -> {
                                    // Bước 3: Xử lý mật khẩu
                                    if (isExistingUser) {
                                        // Đăng nhập với mật khẩu
                                        val passwordValidationResult = InputValidator.validatePassword(password)
                                        val validationError = if (passwordValidationResult.isError()) passwordValidationResult.getErrorMessage() else null
                                        if (validationError != null) {
                                            errorMessage = validationError
                                        } else {
                                            clearError()
                                            isLoading = true
                                            scope.launch {
                                                val result = authManager.login(phoneNumber, password)
                                                when (result) {
                                                    is com.tuhoc.phatnguoi.data.local.LoginResult.Success -> {
                                                        onLoginSuccess(false) // false = đăng nhập
                                                    }
                                                    is com.tuhoc.phatnguoi.data.local.LoginResult.Failed -> {
                                                        if (result.isRateLimited) {
                                                            // Đang bị rate limit → hiển thị rate limit message với countdown
                                                            result.rateLimitResult?.let { rateLimitInfo ->
                                                                rateLimitMessage = rateLimitInfo.message
                                                                rateLimitCountdown = rateLimitInfo.remainingSeconds
                                                            }
                                                            errorMessage = null
                                                        } else {
                                                            // Kiểm tra số lần thử còn lại
                                                            // Cập nhật số lần thử còn lại từ kết quả
                                                            remainingLoginAttempts = result.remainingAttempts
                                                            
                                                            if (result.remainingAttempts > 0) {
                                                                // Còn lần thử → hiển thị số lần thử còn lại
                                                                errorMessage = "Mật khẩu không đúng. Còn ${result.remainingAttempts} lần thử"
                                                                rateLimitMessage = null
                                                                rateLimitCountdown = 0
                                                            } else {
                                                                // Hết lần thử → chuyển sang rate limit message
                                                                // Lấy thông tin rate limit mới nhất
                                                                val rateLimitInfo = authManager.getRateLimitInfo(phoneNumber)
                                                                if (!rateLimitInfo.canProceed) {
                                                                    rateLimitMessage = rateLimitInfo.message
                                                                    rateLimitCountdown = rateLimitInfo.remainingSeconds
                                                                    errorMessage = null
                                                                    remainingLoginAttempts = null
                                                                } else {
                                                                    errorMessage = "Mật khẩu không đúng"
                                                                    rateLimitMessage = null
                                                                    remainingLoginAttempts = 0
                                                                }
                                                            }
                                                        }
                                                        isLoading = false
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        // Tạo tài khoản mới
                                        val passwordValidationResult = InputValidator.validatePassword(password)
                                        val passwordError = if (passwordValidationResult.isError()) passwordValidationResult.getErrorMessage() else null
                                        val confirmValidationResult = InputValidator.validateConfirmPassword(password, confirmPassword)
                                        val confirmError = if (confirmValidationResult.isError()) confirmValidationResult.getErrorMessage() else null
                                        val pinError = validatePinOnSubmit(password, phoneNumber, PIN_LENGTH)
                                        
                                        when {
                                            passwordError != null -> errorMessage = passwordError
                                            confirmError != null -> errorMessage = confirmError
                                            pinError != null -> {
                                                errorMessage = pinError
                                                pinStrengthError = pinError
                                            }
                                            else -> {
                                                clearError()
                                                pinStrengthError = null
                                                isLoading = true
                                                scope.launch {
                                                    val result = authManager.createAccount(phoneNumber, password)
                                                    result.onSuccess {
                                                    onLoginSuccess(true) // true = đăng ký mới
                                                    }.onFailure { exception ->
                                                        errorMessage = exception.message ?: "Không thể tạo tài khoản"
                                                        isLoading = false
                                                    }
                                                }
                                            }
                                        }
                                    }
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
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                when (step) {
                                    1 -> "Tiếp tục"
                                    2 -> "Xác thực"
                                    3 -> if (isExistingUser) "Đăng nhập" else "Tạo tài khoản"
                                    else -> "Tiếp tục"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
                
                if (onCancel != null && step == 1) {
                    Spacer(Modifier.height(12.dp))
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    ) {
                        Text("Hủy", color = TextSub)
                    }
                }
            }
        }
        }
    }
}

