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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuhoc.phatnguoi.data.local.AuthManager
import com.tuhoc.phatnguoi.data.remote.OtpService
import com.tuhoc.phatnguoi.security.PinStrengthChecker
import com.tuhoc.phatnguoi.ui.login.PinStrengthMessages
import com.tuhoc.phatnguoi.ui.login.checkPinStrengthRealTime
import com.tuhoc.phatnguoi.ui.login.validatePinOnSubmit
import com.tuhoc.phatnguoi.security.InputValidator
import com.tuhoc.phatnguoi.ui.theme.RedPrimary
import com.tuhoc.phatnguoi.ui.theme.TextPrimary
import com.tuhoc.phatnguoi.ui.theme.TextSub
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Constants
private const val PIN_LENGTH = 6
private const val OTP_LENGTH = 4
private const val MIN_PHONE_LENGTH = 10
private const val MAX_PHONE_LENGTH = 11
private const val FOCUS_DELAY_MS = 100L

// Validation sử dụng InputValidator trực tiếp cho toàn bộ hệ thống

@Composable
fun ForgotPasswordScreen(
    onSuccess: () -> Unit,
    onCancel: (() -> Unit)? = null,
    initialPhoneNumber: String = "",
    initialStep: Int = 2 // Luôn bắt đầu từ bước OTP
) {
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    val otpService = remember { OtpService(context) }
    val scope = rememberCoroutineScope()

    // State management
    // Step 2 = xác thực OTP
    // Step 3 = tạo mật khẩu mới
    var step by remember { mutableStateOf(initialStep) }
    var phoneNumber by remember { mutableStateOf(initialPhoneNumber) }
    var otp by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isOtpSent by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(0) }
    var pinStrengthWarning by remember { mutableStateOf<String?>(null) } // Cảnh báo PIN yếu
    var pinStrengthError by remember { mutableStateOf<String?>(null) } // Lỗi PIN không hợp lệ
    
    // State cho OTP rate limiting
    var otpRateLimitMessage by remember { mutableStateOf<String?>(null) }
    var otpRateLimitCountdown by remember { mutableStateOf(0) }
    
    // Tự động gửi OTP khi mở màn hình (luôn ở bước 2)
    LaunchedEffect(initialPhoneNumber) {
        if (initialPhoneNumber.isNotEmpty()) {
            // Gửi OTP trong background, không block UI
            scope.launch {
                isLoading = true
                val result = otpService.sendOtp(initialPhoneNumber)
                isLoading = false
                when (result) {
                    is com.tuhoc.phatnguoi.data.remote.OTPResult.Success -> {
                        isOtpSent = true
                        countdown = 60
                        otpRateLimitMessage = null
                        otpRateLimitCountdown = 0
                    }
                    is com.tuhoc.phatnguoi.data.remote.OTPResult.RateLimited -> {
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
        }
    }
    
    // Countdown timer cho rate limit OTP
    LaunchedEffect(otpRateLimitCountdown) {
        if (otpRateLimitCountdown > 0) {
            delay(1000)
            otpRateLimitCountdown--
            
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
        // Top bar với nút quay lại (hiển thị khi ở bước OTP hoặc tạo mật khẩu)
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
                                        // Quay về màn hình nhập mật khẩu (LoginScreen)
                                        onCancel?.invoke()
                                    }

                                    3 -> {
                                        step = 2
                                        password = ""
                                        confirmPassword = ""
                                        clearError()
                                    }
                                }
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
                        3 -> "Tạo mật khẩu mới"
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
                    step == 2 -> "Xác thực OTP"
                    else -> "Tạo mật khẩu mới"
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
                        text = "Tạo mật khẩu mới để bảo vệ tài khoản",
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
                    // Step 2: Xác thực OTP
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
                                enabled = !isLoading,
                                focusRequester = otpFocusRequester,
                                autoFocus = true,
                                length = OTP_LENGTH
                            )

                            Spacer(Modifier.height(16.dp))

                            // Nút gửi lại OTP
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
                                                    countdown = 60
                                                    clearError()
                                                    otpRateLimitMessage = null
                                                    otpRateLimitCountdown = 0
                                                }
                                                is com.tuhoc.phatnguoi.data.remote.OTPResult.RateLimited -> {
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
                            
                            // Hiển thị OTP rate limit message
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
                    }

                    // Step 3: Tạo mật khẩu mới
                    if (step == 3) {
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
                    Button(
                        onClick = {
                            when (step) {
                                2 -> {
                                        // Bước 2: Xác thực OTP
                                        if (otp.length != OTP_LENGTH) {
                                            errorMessage = "Vui lòng nhập đầy đủ $OTP_LENGTH số OTP"
                                        } else {
                                            clearError()
                                            isLoading = true
                                            scope.launch {
                                                val isValid = otpService.verifyOtp(phoneNumber, otp)
                                                if (isValid) {
                                                    step = 3
                                                    isLoading = false
                                                } else {
                                                    errorMessage =
                                                        "Mã OTP không đúng hoặc đã hết hạn"
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    }

                                    3 -> {
                                        // Bước 3: Đặt lại mật khẩu
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
                                                    val result = authManager.resetPassword(phoneNumber, password)
                                                    result.onSuccess {
                                                        onSuccess()
                                                    }.onFailure { exception ->
                                                        errorMessage = exception.message ?: "Không thể đặt lại mật khẩu"
                                                    isLoading = false
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
                            .fillMaxWidth()
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
                                    2 -> "Xác thực"
                                    3 -> "Đặt lại mật khẩu"
                                    else -> "Tiếp tục"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

