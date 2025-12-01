# HƯỚNG DẪN SỬA CÁC LỖ HỔNG BẢO MẬT

## 🔴 1. SỬA LỖI LƯU MẬT KHẨU PLAIN TEXT

### Vấn đề
Mật khẩu đang được lưu trực tiếp vào Firestore không qua hash.

### Giải pháp: Sử dụng BCrypt

**Bước 1:** Thêm dependency vào `build.gradle.kts`:

```kotlin
dependencies {
    // BCrypt for password hashing
    implementation("org.mindrot:jbcrypt:0.4")
}
```

**Bước 2:** Tạo utility class `PasswordHasher.kt`:

```kotlin
package com.tuhoc.phatnguoi.utils

import org.mindrot.jbcrypt.BCrypt

object PasswordHasher {
    /**
     * Hash mật khẩu với salt tự động
     */
    fun hashPassword(password: String): String {
        return BCrypt.hashpw(password, BCrypt.gensalt())
    }
    
    /**
     * Kiểm tra mật khẩu có khớp với hash không
     */
    fun verifyPassword(password: String, hash: String): Boolean {
        return try {
            BCrypt.checkpw(password, hash)
        } catch (e: Exception) {
            false
        }
    }
}
```

**Bước 3:** Sửa `FirebaseUserService.kt`:

```kotlin
import com.tuhoc.phatnguoi.utils.PasswordHasher

suspend fun createAccount(phoneNumber: String, password: String): Result<String> {
    logoutAll()
    
    // ✅ Hash mật khẩu trước khi lưu
    val hashedPassword = PasswordHasher.hashPassword(password)
    
    val userData = mapOf(
        "phoneNumber" to phoneNumber,
        "password" to hashedPassword,  // ✅ Lưu hash thay vì plain text
        "isLoggedIn" to true,
        "createdAt" to Timestamp.now(),
        "updatedAt" to Timestamp.now()
    )
    
    return repository.saveDocument("users", userData, phoneNumber)
}

suspend fun login(phoneNumber: String, password: String): Boolean {
    val user = getUserByPhone(phoneNumber)
    
    if (user == null) return false
    
    // ✅ So sánh mật khẩu với hash
    val storedHash = user["password"] as? String ?: return false
    val isValid = PasswordHasher.verifyPassword(password, storedHash)
    
    if (isValid) {
        logoutAll()
        repository.updateDocument("users", phoneNumber, mapOf(
            "isLoggedIn" to true,
            "updatedAt" to Timestamp.now()
        ))
        return true
    }
    
    return false
}

suspend fun updatePassword(phoneNumber: String, newPassword: String): Result<Boolean> {
    // ✅ Hash mật khẩu mới
    val hashedPassword = PasswordHasher.hashPassword(newPassword)
    
    return repository.updateDocument("users", phoneNumber, mapOf(
        "password" to hashedPassword,
        "updatedAt" to Timestamp.now()
    ))
}
```

---

## 🔴 2. DI CHUYỂN API KEY RA KHỎI SOURCE CODE

### Giải pháp: Sử dụng local.properties

**Bước 1:** Thêm vào `local.properties`:

```properties
AUTOCAPTCHA_API_KEY=your_api_key_here
```

**Bước 2:** Sửa `build.gradle.kts`:

```kotlin
defaultConfig {
    // ... existing code ...
    
    // Đọc API key từ local.properties
    val localPropertiesFile = rootProject.file("local.properties")
    val autocaptchaApiKey = if (localPropertiesFile.exists()) {
        val properties = Properties()
        properties.load(localPropertiesFile.inputStream())
        properties.getProperty("AUTOCAPTCHA_API_KEY") ?: ""
    } else {
        ""
    }
    buildConfigField("String", "AUTOCAPTCHA_API_KEY", "\"$autocaptchaApiKey\"")
}
```

**Bước 3:** Sửa `PhatNguoiRepository.kt`:

```kotlin
import com.tuhoc.phatnguoi.BuildConfig

class PhatNguoiRepository {
    // ✅ Đọc từ BuildConfig thay vì hardcode
    private val AUTOCAPTCHA_API_KEY = BuildConfig.AUTOCAPTCHA_API_KEY
    
    // ... rest of code ...
}
```

**Lưu ý:** Thêm `local.properties` vào `.gitignore` để không commit API key lên Git.

---

## 🟠 3. SỬA LỖI SSL VERIFICATION

### Giải pháp: Chỉ disable SSL trong debug mode

**Sửa `PhatNguoiRepository.kt`:**

```kotlin
import com.tuhoc.phatnguoi.BuildConfig

private fun createUnsafeOkHttpClient(): OkHttpClient {
    // ✅ Chỉ disable SSL trong debug mode
    if (BuildConfig.DEBUG) {
        // Debug mode: có thể disable SSL để test
        try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory

            return OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        } catch (e: Exception) {
            Log.e("PhatNguoi", "Lỗi tạo unsafe OkHttpClient", e)
            // Fallback to default
        }
    }
    
    // ✅ Production: Sử dụng SSL verification bình thường
    return OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
}
```

---

## 🟠 4. IMPLEMENT RATE LIMITING

### Giải pháp: Tạo RateLimiter class

**Tạo `RateLimiter.kt`:**

```kotlin
package com.tuhoc.phatnguoi.utils

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.TimeUnit

class RateLimiter(
    private val context: Context,
    private val maxAttempts: Int = 5,
    private val timeWindowMinutes: Long = 15
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "rate_limiter",
        Context.MODE_PRIVATE
    )
    
    /**
     * Kiểm tra xem có thể thực hiện action không
     * @param key Key để identify action (ví dụ: "login_$phoneNumber")
     * @return true nếu có thể thực hiện, false nếu bị block
     */
    fun canProceed(key: String): Boolean {
        val attemptsKey = "${key}_attempts"
        val timestampKey = "${key}_timestamp"
        
        val attempts = prefs.getInt(attemptsKey, 0)
        val lastAttemptTime = prefs.getLong(timestampKey, 0)
        val currentTime = System.currentTimeMillis()
        
        // Reset nếu đã hết thời gian window
        if (currentTime - lastAttemptTime > TimeUnit.MINUTES.toMillis(timeWindowMinutes)) {
            prefs.edit()
                .putInt(attemptsKey, 0)
                .putLong(timestampKey, currentTime)
                .apply()
            return true
        }
        
        // Kiểm tra số lần thử
        if (attempts >= maxAttempts) {
            return false
        }
        
        return true
    }
    
    /**
     * Ghi nhận một lần thử
     */
    fun recordAttempt(key: String) {
        val attemptsKey = "${key}_attempts"
        val timestampKey = "${key}_timestamp"
        
        val attempts = prefs.getInt(attemptsKey, 0) + 1
        val currentTime = System.currentTimeMillis()
        
        prefs.edit()
            .putInt(attemptsKey, attempts)
            .putLong(timestampKey, currentTime)
            .apply()
    }
    
    /**
     * Reset rate limiter cho một key
     */
    fun reset(key: String) {
        val attemptsKey = "${key}_attempts"
        val timestampKey = "${key}_timestamp"
        
        prefs.edit()
            .remove(attemptsKey)
            .remove(timestampKey)
            .apply()
    }
    
    /**
     * Lấy số lần thử còn lại
     */
    fun getRemainingAttempts(key: String): Int {
        val attemptsKey = "${key}_attempts"
        val attempts = prefs.getInt(attemptsKey, 0)
        return maxOf(0, maxAttempts - attempts)
    }
    
    /**
     * Lấy thời gian còn lại (seconds) trước khi reset
     */
    fun getTimeRemaining(key: String): Long {
        val timestampKey = "${key}_timestamp"
        val lastAttemptTime = prefs.getLong(timestampKey, 0)
        val currentTime = System.currentTimeMillis()
        val elapsed = currentTime - lastAttemptTime
        val windowMillis = TimeUnit.MINUTES.toMillis(timeWindowMinutes)
        
        return if (elapsed >= windowMillis) {
            0
        } else {
            TimeUnit.MILLISECONDS.toSeconds(windowMillis - elapsed)
        }
    }
}
```

**Sửa `FirebaseUserService.kt`:**

```kotlin
import com.tuhoc.phatnguoi.utils.RateLimiter

class FirebaseUserService {
    private val db = FirebaseFirestore.getInstance()
    private val repository = FirebaseRepository()
    
    // ✅ Thêm rate limiter
    private val rateLimiter = RateLimiter(
        context = /* inject context hoặc dùng Application context */,
        maxAttempts = 5,
        timeWindowMinutes = 15
    )
    
    suspend fun login(phoneNumber: String, password: String): Boolean {
        val key = "login_$phoneNumber"
        
        // ✅ Kiểm tra rate limit
        if (!rateLimiter.canProceed(key)) {
            Log.w("FirebaseUserService", "Quá nhiều lần thử đăng nhập cho $phoneNumber")
            return false
        }
        
        val user = getUserByPhone(phoneNumber)
        
        return if (user != null && PasswordHasher.verifyPassword(password, user["password"] as String)) {
            // ✅ Reset rate limiter khi đăng nhập thành công
            rateLimiter.reset(key)
            
            logoutAll()
            repository.updateDocument("users", phoneNumber, mapOf(
                "isLoggedIn" to true,
                "updatedAt" to Timestamp.now()
            ))
            true
        } else {
            // ✅ Ghi nhận lần thử thất bại
            rateLimiter.recordAttempt(key)
            false
        }
    }
}
```

---

## 🟡 5. CẢI THIỆN OTP GENERATION

### Sửa `OtpService.kt`:

```kotlin
import java.security.SecureRandom

class OtpService(private val context: Context) {
    companion object {
        private const val OTP_LENGTH = 6  // ✅ Tăng từ 4 lên 6 chữ số
        // ... other constants ...
    }
    
    /**
     * Tạo OTP ngẫu nhiên an toàn
     */
    private fun generateOtp(): String {
        // ✅ Sử dụng SecureRandom thay vì Random
        val secureRandom = SecureRandom()
        return (1..OTP_LENGTH)
            .map { secureRandom.nextInt(10) }
            .joinToString("")
    }
}
```

---

## 🟡 6. MÃ HÓA DỮ LIỆU LOCAL

### Giải pháp: Sử dụng EncryptedSharedPreferences

**Bước 1:** Thêm dependency:

```kotlin
dependencies {
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
}
```

**Bước 2:** Tạo `SecurePreferences.kt`:

```kotlin
package com.tuhoc.phatnguoi.utils

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurePreferences(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val prefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }
    
    fun getString(key: String, defaultValue: String? = null): String? {
        return prefs.getString(key, defaultValue)
    }
    
    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }
}
```

**Bước 3:** Sửa `OtpService.kt` để sử dụng SecurePreferences:

```kotlin
import com.tuhoc.phatnguoi.utils.SecurePreferences

class OtpService(private val context: Context) {
    // ✅ Sử dụng EncryptedSharedPreferences
    private val securePrefs = SecurePreferences(context)
    
    suspend fun sendOtp(phoneNumber: String): Boolean {
        val otp = generateOtp()
        val expiryTime = System.currentTimeMillis() + (OTP_VALIDITY_MINUTES * 60 * 1000)
        
        // ✅ Lưu vào encrypted storage
        securePrefs.putString(KEY_OTP + phoneNumber, otp)
        securePrefs.putString(KEY_OTP_EXPIRY + phoneNumber, expiryTime.toString())
        
        // ... rest of code ...
    }
    
    fun verifyOtp(phoneNumber: String, otp: String): Boolean {
        val storedOtp = securePrefs.getString(KEY_OTP + phoneNumber)
        val expiryTimeStr = securePrefs.getString(KEY_OTP_EXPIRY + phoneNumber)
        
        // ... validation logic ...
    }
}
```

---

## 🔵 7. BẬT PROGUARD

### Sửa `build.gradle.kts`:

```kotlin
buildTypes {
    release {
        isMinifyEnabled = true  // ✅ Bật obfuscation
        isShrinkResources = true  // ✅ Xóa resources không dùng
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

**Sửa `proguard-rules.pro`:**

```proguard
# Giữ lại các class cần thiết cho Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Giữ lại các model class
-keep class com.tuhoc.phatnguoi.data.local.** { *; }
-keep class com.tuhoc.phatnguoi.data.remote.** { *; }

# Giữ lại BuildConfig
-keep class com.tuhoc.phatnguoi.BuildConfig { *; }

# Giữ lại các class được sử dụng bởi reflection
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer
```

---

## 🟠 8. TẠO FIRESTORE SECURITY RULES

**Tạo file `firestore.rules` trong thư mục `app/`:**

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper function để kiểm tra user đang đăng nhập
    function isAuthenticated() {
      return request.auth != null;
    }
    
    // Helper function để kiểm tra user có phải owner không
    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }
    
    // Users collection
    match /users/{userId} {
      // Chỉ cho phép đọc/ghi dữ liệu của chính mình
      allow read, write: if isOwner(userId);
      
      // Không cho phép đọc password hash của user khác
      allow read: if isAuthenticated() && 
        !('password' in resource.data);
    }
    
    // History collection
    match /history/{historyId} {
      // Chỉ cho phép đọc/ghi lịch sử của chính mình
      allow read, write: if isOwner(resource.data.userId);
      
      // Validate dữ liệu khi tạo mới
      allow create: if isOwner(request.resource.data.userId) &&
        request.resource.data.keys().hasAll(['bienSo', 'loaiXe', 'coViPham', 'thoiGian', 'userId']) &&
        request.resource.data.bienSo is string &&
        request.resource.data.loaiXe is string &&
        request.resource.data.coViPham is bool &&
        request.resource.data.thoiGian is timestamp;
    }
    
    // AutoCheck collection
    match /autoCheck/{checkId} {
      // Chỉ cho phép đọc/ghi auto check của chính mình
      allow read, write: if isOwner(resource.data.userId);
    }
  }
}
```

**Lưu ý:** Cần deploy rules lên Firebase Console hoặc sử dụng Firebase CLI.

---

## 📝 CHECKLIST TRIỂN KHAI

Sau khi sửa các lỗ hổng, kiểm tra lại:

- [ ] Mật khẩu được hash trước khi lưu
- [ ] API keys không còn hardcode
- [ ] SSL verification được bật cho production
- [ ] Rate limiting hoạt động đúng
- [ ] OTP sử dụng SecureRandom và có 6 chữ số
- [ ] Dữ liệu local được mã hóa
- [ ] ProGuard được bật và test kỹ
- [ ] Firestore security rules được deploy
- [ ] Test lại tất cả chức năng sau khi sửa

---

**Lưu ý quan trọng:**
- Test kỹ sau mỗi thay đổi
- Backup code trước khi sửa
- Deploy từng bước một
- Monitor logs để phát hiện lỗi



