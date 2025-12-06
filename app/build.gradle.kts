plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

import java.util.Properties

android {
    namespace = "com.tuhoc.phatnguoi"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.tuhoc.phatnguoi"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Đọc API keys từ local.properties
        val localPropertiesFile = rootProject.file("local.properties")
        val properties = if (localPropertiesFile.exists()) {
            val props = Properties()
            props.load(localPropertiesFile.inputStream())
            props
        } else {
            Properties()
        }
        
        val geminiApiKey = properties.getProperty("GEMINI_API_KEY") ?: ""
        val autocaptchaApiKey = properties.getProperty("AUTOCAPTCHA_API_KEY") ?: ""
        
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
        buildConfigField("String", "AUTOCAPTCHA_API_KEY", "\"$autocaptchaApiKey\"")
    }

    buildTypes {
        debug {
            buildConfigField("Boolean", "DEBUG", "true")
        }
        release {
            buildConfigField("Boolean", "DEBUG", "false")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("androidx.compose.material:material-icons-extended:1.6.8")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.11.0")
    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    // Jsoup: parse HTML giống BeautifulSoup
    implementation("org.jsoup:jsoup:1.17.2")
    // Gson: để lưu lịch sử tra cứu
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Coil: để load hình ảnh từ URL
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // Google Gemini AI
    implementation("com.google.ai.client.generativeai:generativeai:0.8.0")
    
    // BCrypt for password hashing
    implementation("org.mindrot:jbcrypt:0.4")
    
    // EncryptedSharedPreferences for secure local storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    
    // Firebase BOM - quản lý version của tất cả Firebase libraries
    implementation(platform(libs.firebase.bom))
    
    // Firebase services
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.realtime.database)
    implementation(libs.firebase.analytics)
}