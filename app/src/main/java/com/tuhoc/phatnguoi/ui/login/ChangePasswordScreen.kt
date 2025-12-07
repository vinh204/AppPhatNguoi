package com.tuhoc.phatnguoi.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

// Constants
// Constants - Sử dụng config từ SecurityConfig
private val PIN_LENGTH = SecurityConfig.Password.PIN_LENGTH
private const val FOCUS_DELAY_MS = 100L // UI config, không liên quan security

// Validation sử dụng InputValidator trực tiếp cho toàn bộ hệ thống

@Composable
fun ChangePasswordScreen(
    onSuccess: () -> Unit,
    onCancel: () -> Unit,
    onForgotPassword: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    val scope = rememberCoroutineScope()
    
    // State management
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(1) } // 1 = mật khẩu cũ, 2 = mật khẩu mới, 3 = xác nhận
    var phoneNumber by remember { mutableStateOf("") }
    var pinStrengthWarning by remember { mutableStateOf<String?>(null) } // Cảnh báo PIN yếu
    var pinStrengthError by remember { mutableStateOf<String?>(null) } // Lỗi PIN không hợp lệ
    
    // Focus requesters
    val currentPasswordFocusRequester = remember { FocusRequester() }
    val newPasswordFocusRequester = remember { FocusRequester() }
    val confirmPasswordFocusRequester = remember { FocusRequester() }
    
    // Helper function to clear error
    val clearError = { errorMessage = null }
    
    // Lấy số điện thoại của user hiện tại
    LaunchedEffect(Unit) {
        val user = authManager.getCurrentUser()
        if (user != null) {
            phoneNumber = user.phoneNumber
        }
    }
    
    // Tự động focus vào trường đầu tiên
    LaunchedEffect(Unit) {
        delay(FOCUS_DELAY_MS)
        currentPasswordFocusRequester.requestFocus()
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
    ) {
        // Top bar with back button
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
                            when (currentStep) {
                                1 -> onCancel()
                                2 -> {
                                    currentStep = 1
                                    newPassword = ""
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
                text = when (currentStep) {
                    1 -> "Nhập mật khẩu cũ"
                    2 -> "Nhập mật khẩu mới"
                    else -> ""
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
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
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = RedPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Đổi mật khẩu",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Nhập mật khẩu cũ và mật khẩu mới",
                fontSize = 14.sp,
                color = TextSub,
                textAlign = TextAlign.Center
            )

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
                    // Step 1: Nhập mật khẩu cũ
                    if (currentStep == 1) {
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            PasswordTextFieldWithDots(
                                value = currentPassword,
                                onValueChange = {
                                    currentPassword = it
                                    clearError()
                                },
                                label = "Nhập mật khẩu cũ",
                                placeholder = "Nhập mật khẩu cũ",
                                enabled = !isLoading,
                                modifier = Modifier.fillMaxWidth(),
                                focusRequester = currentPasswordFocusRequester
                            )

                            // Text "Quên mật khẩu?"
                            if (onForgotPassword != null && phoneNumber.isNotEmpty()) {
                                Spacer(Modifier.height(0.dp))
                                TextButton(
                                    onClick = {
                                        onForgotPassword(phoneNumber)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isLoading,
                                    contentPadding = PaddingValues(
                                        horizontal = 0.dp,
                                        vertical = 4.dp
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Text(
                                            text = "Quên mật khẩu?",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2196F3)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Step 2: Nhập mật khẩu mới và xác nhận mật khẩu
                    if (currentStep == 2) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            PasswordTextFieldWithDots(
                                value = newPassword,
                                onValueChange = {
                                    newPassword = it
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
                                focusRequester = newPasswordFocusRequester
                            )
                            
                            // Hiển thị cảnh báo/lỗi PIN strength
                            PinStrengthMessages(
                                warning = pinStrengthWarning,
                                error = pinStrengthError
                            )
                            
                            PasswordTextFieldWithDots(
                                value = confirmPassword,
                                onValueChange = {
                                    confirmPassword = it
                                    clearError()
                                },
                                label = "Xác nhận mật khẩu mới",
                                placeholder = "Xác nhận mật khẩu mới",
                                enabled = !isLoading,
                                modifier = Modifier.fillMaxWidth(),
                                focusRequester = confirmPasswordFocusRequester
                            )
                        }
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

                    // Button
                    Button(
                        onClick = {
                            when (currentStep) {
                                1 -> {
                                    // Kiểm tra mật khẩu cũ
                                    if (currentPassword.length != PIN_LENGTH) {
                                        errorMessage = "Mật khẩu phải có $PIN_LENGTH số"
                                    } else {
                                        clearError()
                                        isLoading = true
                                        scope.launch {
                                            val user = authManager.getCurrentUser()
                                            if (user != null && user.password == currentPassword) {
                                                currentStep = 2
                                                isLoading = false
                                            } else {
                                                errorMessage = "Mật khẩu cũ không đúng"
                                                isLoading = false
                                            }
                                        }
                                    }
                                }

                                2 -> {
                                    // Xác nhận và đổi mật khẩu
                                    val passwordValidationResult = InputValidator.validatePassword(newPassword)
                                    val passwordError = if (passwordValidationResult.isError()) passwordValidationResult.getErrorMessage() else null
                                    val confirmValidationResult = InputValidator.validateConfirmPassword(newPassword, confirmPassword)
                                    val confirmError = if (confirmValidationResult.isError()) confirmValidationResult.getErrorMessage() else null
                                    val pinError = validatePinOnSubmit(newPassword, phoneNumber, PIN_LENGTH)
                                    
                                    when {
                                        passwordError != null -> errorMessage = passwordError
                                        confirmError != null -> errorMessage = confirmError
                                        newPassword == currentPassword -> {
                                            errorMessage = "Mật khẩu mới phải khác mật khẩu cũ"
                                        }
                                        pinError != null -> {
                                            errorMessage = pinError
                                            pinStrengthError = pinError
                                        }
                                        else -> {
                                            clearError()
                                            pinStrengthError = null
                                            isLoading = true
                                            scope.launch {
                                                val user = authManager.getCurrentUser()
                                                if (user != null) {
                                                    val result = authManager.updatePassword(
                                                        user.phoneNumber,
                                                        newPassword
                                                    )
                                                    result.onSuccess {
                                                    onSuccess()
                                                    }.onFailure { exception ->
                                                        errorMessage = exception.message ?: "Không thể đổi mật khẩu"
                                                        isLoading = false
                                                    }
                                                } else {
                                                    errorMessage =
                                                        "Không tìm thấy thông tin người dùng"
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
                                when (currentStep) {
                                    1 -> "Tiếp tục"
                                    2 -> "Đổi mật khẩu"
                                    else -> "Đổi mật khẩu"
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

        // Tự động focus vào trường tiếp theo khi chuyển step
        LaunchedEffect(currentStep) {
            delay(FOCUS_DELAY_MS)
            when (currentStep) {
                2 -> newPasswordFocusRequester.requestFocus()
            }
        }
    }
}

