# Hướng dẫn Setup Firebase

## ✅ Đã hoàn thành:
- ✅ Firebase dependencies đã được thêm vào project
- ✅ Google Services plugin đã được cấu hình
- ✅ File `google-services.json` đã có trong project
- ✅ Các service Firebase đã được tạo sẵn

## 📋 Các bước cần làm để sử dụng Firestore:

### 1. Tạo Firestore Database trong Firebase Console:
1. Vào [Firebase Console](https://console.firebase.google.com/)
2. Chọn project `phat-nguoi-bc6cf`
3. Vào **Firestore Database** (bên trái menu)
4. Click **Create database**
5. Chọn chế độ:
   - **Test mode** (cho development) - Cho phép đọc/ghi trong 30 ngày
   - **Production mode** (cho production) - Cần cấu hình rules
6. Chọn region: **asia-southeast1** (Singapore) hoặc region gần nhất
7. Click **Enable**

### 2. (Tùy chọn) Khởi tạo database với dữ liệu mẫu:
Sau khi tạo Firestore database, bạn có thể gọi:
```kotlin
FirebaseInitHelper.initDatabase(context) { success, error ->
    if (success) {
        Log.d("TAG", "Database đã được khởi tạo!")
    }
}
```

### 3. Cấu hình Firestore Rules (Quan trọng cho production):
Vào Firebase Console → Firestore Database → Rules

**Test mode (tạm thời):**
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.time < timestamp.date(2025, 12, 31);
    }
  }
}
```

**Production mode (an toàn hơn):**
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Users collection - chỉ user đó mới đọc/ghi được data của mình
    match /users/{userId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // History collection - chỉ user đó mới đọc/ghi được
    match /history/{historyId} {
      allow read, write: if request.auth != null && 
        resource.data.userId == request.auth.uid;
    }
    
    // Auto check collection - chỉ user đó mới đọc/ghi được
    match /auto_check/{autoCheckId} {
      allow read, write: if request.auth != null && 
        resource.data.userId == request.auth.uid;
    }
  }
}
```

## 🚀 App có thể chạy ngay:
App **có thể khởi động được** ngay cả khi chưa tạo Firestore database. 
Firebase sẽ chỉ báo lỗi khi bạn cố gắng sử dụng Firestore services.

## 📝 Sử dụng Firebase trong code:

```kotlin
// Quản lý users
val userService = FirebaseUserService()
userService.createAccount("0912345678", "1234")

// Quản lý lịch sử
val historyService = FirebaseHistoryService()
historyService.saveHistory("0912345678", "30A-12345", "Xe máy", true, 2)

// Quản lý auto check
val autoCheckService = FirebaseAutoCheckService()
autoCheckService.addAutoCheck("0912345678", "30A-12345", 2, true)
```

## ⚠️ Lưu ý:
- App sẽ không crash nếu chưa có Firestore database
- Chỉ khi bạn gọi các hàm Firebase thì mới cần database đã được tạo
- Nên tạo database trước khi sử dụng các tính năng Firebase

