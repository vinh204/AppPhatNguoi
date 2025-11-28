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

/**
 * Validation functions
 */
private fun validatePhoneNumber(phone: String): String? {
    return when {
        phone.isEmpty() -> "Vui lòng nhập số điện thoại"
        phone.length !in MIN_PHONE_LENGTH..MAX_PHONE_LENGTH -> "Số điện thoại không hợp lệ"
        !phone.startsWith("0") -> "Số điện thoại phải bắt đầu bằng số 0"
        else -> null
    }
}

private fun validatePassword(password: String): String? {
    return if (password.length != PIN_LENGTH) {
        "Mật khẩu phải có $PIN_LENGTH số"
    } else {
        null
    }
}

private fun validateConfirmPassword(password: String, confirmPassword: String): String? {
    return if (password != confirmPassword) {
        "Mật khẩu xác nhận không khớp"
    } else {
        null
    }
}

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
    
    // Tự động gửi OTP khi mở màn hình (luôn ở bước 2)
    LaunchedEffect(initialPhoneNumber) {
        if (initialPhoneNumber.isNotEmpty()) {
            // Gửi OTP trong background, không block UI
            scope.launch {
                isLoading = true
                val success = otpService.sendOtp(initialPhoneNumber)
                isLoading = false
                if (success) {
                    isOtpSent = true
                    countdown = 60
                } else {
                    errorMessage = "Không thể gửi OTP. Vui lòng thử lại."
                }
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
                                            val success = otpService.sendOtp(phoneNumber)
                                            isLoading = false
                                            if (success) {
                                                isOtpSent = true
                                                countdown = 60
                                                clearError()
                                            } else {
                                                errorMessage =
                                                    "Không thể gửi OTP. Vui lòng thử lại."
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
                    }

                    // Step 3: Tạo mật khẩu mới
                    if (step == 3) {
                        PasswordTextFieldWithDots(
                            value = password,
                            onValueChange = {
                                password = it
                                clearError()
                            },
                            label = "Nhập mật khẩu mới",
                            placeholder = "Nhập mật khẩu mới",
                            enabled = !isLoading,
                            modifier = Modifier.fillMaxWidth(),
                            focusRequester = passwordFocusRequester
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
                                        val passwordError = validatePassword(password)
                                        val confirmError =
                                            validateConfirmPassword(password, confirmPassword)
                                        when {
                                            passwordError != null -> errorMessage = passwordError
                                            confirmError != null -> errorMessage = confirmError
                                            else -> {
                                                clearError()
                                                isLoading = true
                                                scope.launch {
                                                    authManager.resetPassword(phoneNumber, password)
                                                    isLoading = false
                                                    onSuccess()
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

