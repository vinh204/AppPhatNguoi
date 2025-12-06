# BÁO CÁO SỬ DỤNG CÁC FILE SECURITY TRONG ỨNG DỤNG

## Tổng quan

Tài liệu này liệt kê tất cả các nơi trong ứng dụng sử dụng các file security và cách chúng được tích hợp.

---

## 📁 CÁC FILE SECURITY

### 1. `SessionManager.kt`
- **Mục đích:** Quản lý session tokens
- **Vị trí:** `app/src/main/java/com/tuhoc/phatnguoi/security/SessionManager.kt`

### 2. `SecurityAuditLogger.kt`
- **Mục đích:** Ghi log các hoạt động bảo mật
- **Vị trí:** `app/src/main/java/com/tuhoc/phatnguoi/security/SecurityAuditLogger.kt`

### 3. `PinStrengthChecker.kt`
- **Mục đích:** Kiểm tra độ mạnh PIN 6 chữ số
- **Vị trí:** `app/src/main/java/com/tuhoc/phatnguoi/security/PinStrengthChecker.kt`

### 4. `PasswordStrengthChecker.kt`
- **Mục đích:** Kiểm tra độ mạnh mật khẩu (không được sử dụng, chỉ dành cho tương lai)
- **Vị trí:** `app/src/main/java/com/tuhoc/phatnguoi/security/PasswordStrengthChecker.kt`

### 5. `SecurityConfig.kt`
- **Mục đích:** Cấu hình bảo mật tập trung
- **Vị trí:** `app/src/main/java/com/tuhoc/phatnguoi/security/SecurityConfig.kt`

---

## 📍 CÁC NƠI SỬ DỤNG

### 1. AuthManager.kt

**File:** `app/src/main/java/com/tuhoc/phatnguoi/data/local/AuthManager.kt`

**Import:**
```kotlin
import com.tuhoc.phatnguoi.security.SecurityAuditLogger
import com.tuhoc.phatnguoi.security.SessionManager
import com.tuhoc.phatnguoi.security.PinStrengthChecker
import com.tuhoc.phatnguoi.security.SecurityConfig
```

**Sử dụng:**

#### 1.1. SessionManager
```kotlin
private val sessionManager = SessionManager(context)

// Tạo session khi đăng nhập thành công
val sessionToken = sessionManager.createSession(normalizedPhone)

// Kiểm tra session khi isLoggedIn()
if (!sessionManager.isValidSession()) {
    // Session không hợp lệ, logout
}

// Xóa session khi logout
sessionManager.clearSession()
```

**Vị trí trong code:**
- Dòng 17: Khởi tạo `SessionManager`
- Dòng 177: Tạo session khi đăng nhập thành công
- Dòng 284: Xóa session khi logout
- Dòng 290-299: Kiểm tra session khi `isLoggedIn()`

#### 1.2. SecurityAuditLogger
```kotlin
// Ghi log đăng nhập thành công
SecurityAuditLogger.logLoginSuccess(normalizedPhone, SecurityAuditLogger.getDefaultMetadata())

// Ghi log đăng nhập thất bại
SecurityAuditLogger.logLoginFailed(normalizedPhone, "Invalid password")

// Ghi log tạo tài khoản
SecurityAuditLogger.logAccountCreated(normalizedPhone, SecurityAuditLogger.getDefaultMetadata())

// Ghi log đổi mật khẩu
SecurityAuditLogger.logPasswordChanged(normalizedPhone, SecurityAuditLogger.getDefaultMetadata())

// Ghi log đăng xuất
SecurityAuditLogger.logLogout(it, SecurityAuditLogger.getDefaultMetadata())

// Ghi log hoạt động đáng ngờ
SecurityAuditLogger.logSuspiciousActivity(normalizedPhone, "Attempt to create account with weak PIN")

// Ghi log rate limit
SecurityAuditLogger.logRateLimitTriggered(normalizedPhone, "Phone exists check rate limit exceeded")

// Ghi log session expired
SecurityAuditLogger.logEvent(
    eventType = SecurityAuditLogger.EventType.SESSION_EXPIRED,
    phoneNumber = it,
    details = "Session expired",
    severity = SecurityAuditLogger.Severity.INFO
)
```

**Vị trí trong code:**
- Dòng 29-33: Log suspicious activity khi phone number không hợp lệ
- Dòng 40-44: Log rate limit khi phone exists check
- Dòng 71-75: Log suspicious activity khi PIN yếu
- Dòng 86-90: Log suspicious activity khi tạo tài khoản thất bại
- Dòng 99: Log account created khi tạo tài khoản thành công
- Dòng 102-106: Log suspicious activity khi tạo tài khoản thất bại
- Dòng 122-126: Log login failed khi phone number không hợp lệ
- Dòng 135-139: Log login failed khi password không hợp lệ
- Dòng 152-156: Log rate limit khi login rate limit exceeded
- Dòng 177: Log login success khi đăng nhập thành công
- Dòng 186-190: Log login failed khi password sai
- Dòng 197-201: Log suspicious activity khi account bị lockout
- Dòng 260: Log password changed khi đổi mật khẩu thành công
- Dòng 284: Log logout khi đăng xuất
- Dòng 295-299: Log session expired khi session hết hạn

#### 1.3. PinStrengthChecker
```kotlin
// Kiểm tra PIN strength khi tạo tài khoản
val pinCheck = PinStrengthChecker.checkPin(password, normalizedPhone)
if (!pinCheck.isValid) {
    return Result.failure(IllegalArgumentException(pinCheck.feedback ?: "PIN không đủ mạnh"))
}

// Kiểm tra PIN strength khi đổi mật khẩu
val pinCheck = PinStrengthChecker.checkPin(newPassword, normalizedPhone)
if (!pinCheck.isValid) {
    return Result.failure(IllegalArgumentException(pinCheck.feedback ?: "PIN không đủ mạnh"))
}
```

**Vị trí trong code:**
- Dòng 84-90: Kiểm tra PIN khi tạo tài khoản
- Dòng 250-254: Kiểm tra PIN khi đổi mật khẩu

#### 1.4. SecurityConfig
```kotlin
// Sử dụng cấu hình PIN length
PinStrengthChecker.checkPin(password, normalizedPhone, SecurityConfig.Password.MIN_LENGTH)
```

**Vị trí trong code:**
- Dòng 84: Sử dụng `SecurityConfig.Password.MIN_LENGTH` (gián tiếp qua PinStrengthChecker)

---

### 2. LoginScreen.kt

**File:** `app/src/main/java/com/tuhoc/phatnguoi/ui/login/LoginScreen.kt`

**Import:**
```kotlin
import com.tuhoc.phatnguoi.security.PinStrengthChecker
```

**Sử dụng:**

#### 2.1. PinStrengthChecker
```kotlin
// Kiểm tra PIN strength real-time khi người dùng nhập
if (it.length == PIN_LENGTH) {
    val pinCheck = PinStrengthChecker.checkPin(it, phoneNumber)
    if (!pinCheck.isValid) {
        pinStrengthError = pinCheck.feedback
    } else if (pinCheck.strength == PinStrengthChecker.Strength.WEAK) {
        pinStrengthWarning = pinCheck.feedback ?: "Mật khẩu này khá yếu, nên chọn mật khẩu khác"
    }
}

// Kiểm tra PIN strength khi submit tạo tài khoản
val pinCheck = if (password.length == PIN_LENGTH) {
    PinStrengthChecker.checkPin(password, phoneNumber)
} else {
    null
}
if (pinCheck != null && !pinCheck.isValid) {
    errorMessage = pinCheck.feedback ?: "Mật khẩu không hợp lệ"
    pinStrengthError = pinCheck.feedback
}
```

**Vị trí trong code:**
- Dòng 817-822: Kiểm tra PIN strength real-time khi nhập mật khẩu mới
- Dòng 1161-1165: Kiểm tra PIN strength khi submit tạo tài khoản

---

### 3. ChangePasswordScreen.kt

**File:** `app/src/main/java/com/tuhoc/phatnguoi/ui/login/ChangePasswordScreen.kt`

**Import:**
```kotlin
import com.tuhoc.phatnguoi.security.PinStrengthChecker
```

**Sử dụng:**

#### 3.1. PinStrengthChecker
```kotlin
// Kiểm tra PIN strength real-time khi người dùng nhập mật khẩu mới
if (it.length == PIN_LENGTH) {
    val pinCheck = PinStrengthChecker.checkPin(it, phoneNumber)
    if (!pinCheck.isValid) {
        pinStrengthError = pinCheck.feedback
    } else if (pinCheck.strength == PinStrengthChecker.Strength.WEAK) {
        pinStrengthWarning = pinCheck.feedback ?: "Mật khẩu này khá yếu, nên chọn mật khẩu khác"
    }
}

// Kiểm tra PIN strength khi submit đổi mật khẩu
val pinCheck = if (newPassword.length == PIN_LENGTH) {
    PinStrengthChecker.checkPin(newPassword, phoneNumber)
} else {
    null
}
if (pinCheck != null && !pinCheck.isValid) {
    errorMessage = pinCheck.feedback ?: "Mật khẩu không hợp lệ"
    pinStrengthError = pinCheck.feedback
}
```

**Vị trí trong code:**
- Dòng 280-285: Kiểm tra PIN strength real-time khi nhập mật khẩu mới
- Dòng 389-393: Kiểm tra PIN strength khi submit đổi mật khẩu

---

### 4. ForgotPasswordScreen.kt

**File:** `app/src/main/java/com/tuhoc/phatnguoi/ui/login/ForgotPasswordScreen.kt`

**Import:**
```kotlin
import com.tuhoc.phatnguoi.security.PinStrengthChecker
```

**Sử dụng:**

#### 4.1. PinStrengthChecker
```kotlin
// Kiểm tra PIN strength real-time khi người dùng nhập mật khẩu mới
if (it.length == PIN_LENGTH) {
    val pinCheck = PinStrengthChecker.checkPin(it, phoneNumber)
    if (!pinCheck.isValid) {
        pinStrengthError = pinCheck.feedback
    } else if (pinCheck.strength == PinStrengthChecker.Strength.WEAK) {
        pinStrengthWarning = pinCheck.feedback ?: "Mật khẩu này khá yếu, nên chọn mật khẩu khác"
    }
}

// Kiểm tra PIN strength khi submit reset mật khẩu
val pinCheck = if (password.length == PIN_LENGTH) {
    PinStrengthChecker.checkPin(password, phoneNumber)
} else {
    null
}
if (pinCheck != null && !pinCheck.isValid) {
    errorMessage = pinCheck.feedback ?: "Mật khẩu không hợp lệ"
    pinStrengthError = pinCheck.feedback
}
```

**Vị trí trong code:**
- Dòng 417-422: Kiểm tra PIN strength real-time khi nhập mật khẩu mới
- Dòng 528-532: Kiểm tra PIN strength khi submit reset mật khẩu

---

### 5. PinStrengthChecker.kt (sử dụng SecurityConfig)

**File:** `app/src/main/java/com/tuhoc/phatnguoi/security/PinStrengthChecker.kt`

**Sử dụng:**

#### 5.1. SecurityConfig
```kotlin
// Sử dụng danh sách PIN bị cấm từ SecurityConfig
private val FORBIDDEN_PINS = SecurityConfig.Password.FORBIDDEN_PINS.toSet()
```

**Vị trí trong code:**
- Dòng 15: Sử dụng `SecurityConfig.Password.FORBIDDEN_PINS`

---

## 📊 TỔNG KẾT SỬ DỤNG

### SessionManager
- **Sử dụng trong:** `AuthManager.kt`
- **Số lần sử dụng:** 4 lần
  - Khởi tạo: 1 lần
  - Tạo session: 1 lần
  - Kiểm tra session: 1 lần
  - Xóa session: 1 lần

### SecurityAuditLogger
- **Sử dụng trong:** `AuthManager.kt`
- **Số lần sử dụng:** 15 lần
  - Log login success: 1 lần
  - Log login failed: 3 lần
  - Log account created: 1 lần
  - Log password changed: 1 lần
  - Log logout: 1 lần
  - Log suspicious activity: 4 lần
  - Log rate limit: 2 lần
  - Log session expired: 1 lần
  - Log event (generic): 1 lần

### PinStrengthChecker
- **Sử dụng trong:** 
  - `AuthManager.kt` (2 lần)
  - `LoginScreen.kt` (2 lần)
  - `ChangePasswordScreen.kt` (2 lần)
  - `ForgotPasswordScreen.kt` (2 lần)
- **Tổng số lần sử dụng:** 8 lần
  - Kiểm tra khi tạo tài khoản: 2 lần (AuthManager + LoginScreen)
  - Kiểm tra khi đổi mật khẩu: 2 lần (AuthManager + ChangePasswordScreen)
  - Kiểm tra khi reset mật khẩu: 2 lần (AuthManager + ForgotPasswordScreen)
  - Kiểm tra real-time trong UI: 3 lần (LoginScreen, ChangePasswordScreen, ForgotPasswordScreen)

### SecurityConfig
- **Sử dụng trong:**
  - `AuthManager.kt` (1 lần - gián tiếp)
  - `PinStrengthChecker.kt` (1 lần)
- **Tổng số lần sử dụng:** 2 lần

### PasswordStrengthChecker
- **Sử dụng trong:** Không được sử dụng (dành cho tương lai)

---

## 🔄 LUỒNG HOẠT ĐỘNG

### 1. Đăng nhập
```
User nhập thông tin
    ↓
AuthManager.login()
    ↓
SecurityAuditLogger.logLoginFailed() (nếu sai)
    ↓
SessionManager.createSession() (nếu thành công)
    ↓
SecurityAuditLogger.logLoginSuccess()
```

### 2. Tạo tài khoản
```
User nhập PIN
    ↓
LoginScreen: PinStrengthChecker.checkPin() (real-time)
    ↓
User submit
    ↓
AuthManager.createAccount()
    ↓
PinStrengthChecker.checkPin() (validation)
    ↓
SecurityAuditLogger.logAccountCreated() (nếu thành công)
    ↓
SessionManager.createSession()
```

### 3. Đổi mật khẩu
```
User nhập PIN mới
    ↓
ChangePasswordScreen: PinStrengthChecker.checkPin() (real-time)
    ↓
User submit
    ↓
AuthManager.updatePassword()
    ↓
PinStrengthChecker.checkPin() (validation)
    ↓
SecurityAuditLogger.logPasswordChanged() (nếu thành công)
```

### 4. Kiểm tra session
```
App khởi động / Kiểm tra đăng nhập
    ↓
AuthManager.isLoggedIn()
    ↓
SessionManager.isValidSession()
    ↓
SecurityAuditLogger.logEvent(SESSION_EXPIRED) (nếu hết hạn)
```

---

## 📝 GHI CHÚ

1. **SessionManager** chỉ được sử dụng trong `AuthManager`, không được gọi trực tiếp từ UI
2. **SecurityAuditLogger** chỉ được sử dụng trong `AuthManager`, tất cả các hoạt động bảo mật đều được log tự động
3. **PinStrengthChecker** được sử dụng ở cả backend (AuthManager) và frontend (UI screens) để:
   - Validation ở backend (bắt buộc)
   - Hiển thị feedback real-time ở frontend (UX tốt hơn)
4. **SecurityConfig** được sử dụng gián tiếp thông qua các class khác
5. **PasswordStrengthChecker** không được sử dụng vì ứng dụng dùng PIN 6 chữ số

---

## ✅ KẾT LUẬN

Tất cả các file security đã được tích hợp đầy đủ vào ứng dụng:
- ✅ SessionManager: Quản lý session tokens
- ✅ SecurityAuditLogger: Ghi log tất cả hoạt động bảo mật
- ✅ PinStrengthChecker: Kiểm tra độ mạnh PIN ở cả backend và frontend
- ✅ SecurityConfig: Cấu hình bảo mật tập trung
- ⚠️ PasswordStrengthChecker: Chưa sử dụng (dành cho tương lai nếu cần)

Hệ thống bảo mật hoạt động tốt và được tích hợp một cách nhất quán trong toàn bộ ứng dụng.



