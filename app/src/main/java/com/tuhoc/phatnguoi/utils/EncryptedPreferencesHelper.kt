package com.tuhoc.phatnguoi.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import android.util.Log

/**
 * Helper class để tạo EncryptedSharedPreferences
 * Sử dụng Android Keystore để mã hóa dữ liệu
 */
object EncryptedPreferencesHelper {
    private const val TAG = "EncryptedPreferencesHelper"
    
    /**
     * Tạo EncryptedSharedPreferences với master key từ Android Keystore
     * 
     * @param context Application context
     * @param fileName Tên file SharedPreferences
     * @return EncryptedSharedPreferences instance
     */
    fun create(context: Context, fileName: String): SharedPreferences {
        return try {
            // Tạo master key từ Android Keystore
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            // Tạo EncryptedSharedPreferences
            EncryptedSharedPreferences.create(
                context,
                fileName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(TAG, "Lỗi khi tạo EncryptedSharedPreferences: ${e.message}", e)
            // Fallback về SharedPreferences thông thường nếu có lỗi
            // (có thể xảy ra trên emulator hoặc device không hỗ trợ Android Keystore)
            Log.w(TAG, "Fallback về SharedPreferences thông thường")
            context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
        }
    }
}

