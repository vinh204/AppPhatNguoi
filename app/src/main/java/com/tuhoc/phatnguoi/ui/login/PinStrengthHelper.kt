package com.tuhoc.phatnguoi.ui.login

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuhoc.phatnguoi.security.PinStrengthChecker

/**
 * Helper composable để hiển thị PIN strength warning/error
 * Tái sử dụng cho tất cả các màn hình login
 */
@Composable
fun PinStrengthMessages(
    warning: String?,
    error: String?,
    modifier: Modifier = Modifier
) {
    // Hiển thị cảnh báo mật khẩu yếu
    warning?.let {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = it,
                fontSize = 12.sp,
                color = Color(0xFFFF9800), // Màu cam cảnh báo
                textAlign = TextAlign.Start
            )
        }
    }
    
    // Hiển thị lỗi mật khẩu không hợp lệ
    error?.let {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = it,
                fontSize = 12.sp,
                color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Start
            )
        }
    }
}

/**
 * Helper function để kiểm tra PIN strength real-time
 * Trả về Pair<warning, error>
 */
fun checkPinStrengthRealTime(
    pin: String,
    phoneNumber: String,
    pinLength: Int = 6
): Pair<String?, String?> {
    if (pin.length != pinLength) {
        return Pair(null, null)
    }
    
    val pinCheck = PinStrengthChecker.checkPin(pin, phoneNumber)
    return when {
        !pinCheck.isValid -> Pair(null, pinCheck.feedback)
        pinCheck.strength == PinStrengthChecker.Strength.WEAK -> 
            Pair(pinCheck.feedback ?: "Mật khẩu này khá yếu, nên chọn mật khẩu khác", null)
        else -> Pair(null, null)
    }
}

/**
 * Helper function để validate PIN khi submit
 * Trả về error message nếu không hợp lệ, null nếu hợp lệ
 */
fun validatePinOnSubmit(
    password: String,
    phoneNumber: String,
    pinLength: Int = 6
): String? {
    if (password.length != pinLength) {
        return null // Sẽ được validate bởi validatePassword
    }
    
    val pinCheck = PinStrengthChecker.checkPin(password, phoneNumber)
    return if (!pinCheck.isValid) {
        pinCheck.feedback ?: "Mật khẩu không hợp lệ"
    } else {
        null
    }
}



