package com.tuhoc.phatnguoi.data.remote

import android.content.Context
import android.util.Base64
import com.google.gson.Gson
import com.tuhoc.phatnguoi.BuildConfig
import com.tuhoc.phatnguoi.security.SecureErrorHandler
import com.tuhoc.phatnguoi.security.SecureLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.net.ssl.*
import kotlinx.coroutines.delay

class PhatNguoiRepository(private val context: Context? = null) {

    private val client: OkHttpClient = createOkHttpClient()
    private val gson = Gson()
    private val secureErrorHandler = context?.let { SecureErrorHandler(it) }

    // API key AutoCaptcha được đọc từ BuildConfig (từ local.properties)
    // Không còn hardcode trong source code
    private val AUTOCAPTCHA_API_KEY = BuildConfig.AUTOCAPTCHA_API_KEY
    
    private val AUTOCAPTCHA_API_URL = "https://autocaptcha.pro/apiv3/process"
    private val CSGT_CAPTCHA_URL = "https://www.csgt.vn/lib/captcha/captcha.class.php"
    private val CSGT_SEARCH_URL = "https://www.csgt.vn/tra-cuu-phuong-tien-vi-pham.html"
    private val CSGT_AJAX_URL = "https://www.csgt.vn/?mod=contact&task=tracuu_post&ajax"
    private val CSGT_BASE_URL = "https://www.csgt.vn"

    private val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    /**
     * Tra cứu vi phạm giao thông
     * @param plate Biển số xe (đã được chuẩn hóa và validate ở ViewModel)
     * @param vehicleType Loại xe (1 = ô tô, 2 = xe máy, 3 = Xe đạp điện)
     */
    suspend fun checkPhatNguoi(
        plate: String,
        vehicleType: Int
    ): PhatNguoiResult = withContext(Dispatchers.IO) {
        try {
            // Tạo session với CookieJar để giữ cookies
            val cookieJar = CookieJarImpl()
            val sessionClient = client.newBuilder()
                .cookieJar(cookieJar)
                .build()

            // 1. GET trang tra cứu để lấy cookie/session (với retry cho SSL errors)
            val searchPageRequest = Request.Builder()
                .url(CSGT_SEARCH_URL)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                .header("Accept-Language", "vi-VN,vi;q=0.9,en-US;q=0.8,en;q=0.7")
                .header("Connection", "keep-alive")
                .build()

            val searchPageResponse = executeWithRetry(sessionClient, searchPageRequest, maxRetries = 3)
            if (!searchPageResponse.isSuccessful) {
                throw IOException("Không thể kết nối tới CSGT: ${searchPageResponse.code}")
            }
            searchPageResponse.close()

            // 2. Giải captcha và tra cứu
            val captchaText = solveCaptcha(sessionClient, AUTOCAPTCHA_API_KEY)

            // Gửi AJAX POST
            SecureLogger.d("=== BƯỚC 3: Gửi AJAX tra cứu ===")
            val formBody = FormBody.Builder()
                .add("BienKS", plate)
                .add("Xe", vehicleType.toString())
                .add("captcha", captchaText)
                .add("ipClient", "127.0.0.1")
                .add("cUrl", vehicleType.toString())
                .build()

            val ajaxRequest = Request.Builder()
                .url(CSGT_AJAX_URL)
                .header("User-Agent", USER_AGENT)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", CSGT_SEARCH_URL)
                .header("Origin", CSGT_BASE_URL)
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .post(formBody)
                .build()

            val ajaxResponse = executeWithRetry(sessionClient, ajaxRequest, maxRetries = 3)
            // Tối ưu: Sử dụng use để tự động close và trim ngay
            val ajaxText = ajaxResponse.body?.use { it.string() }?.trim() ?: ""
            ajaxResponse.close()

            if (ajaxText == "404") {
                return@withContext PhatNguoiResult(
                    error = true,
                    message = "Captcha sai. Vui lòng thử lại."
                )
            }

            // Parse JSON response
            val ajaxJson: Map<String, Any> = try {
                val cleanText = ajaxText.replace(Regex("\\s+"), "")
                gson.fromJson(cleanText, Map::class.java) as Map<String, Any>
            } catch (e: Exception) {
                try {
                    gson.fromJson(ajaxText, Map::class.java) as Map<String, Any>
                } catch (e2: Exception) {
                    SecureLogger.e("Lỗi parse JSON", e2)
                    val errorMessage = secureErrorHandler?.handleError(e2)?.message 
                        ?: "Server CSGT trả về dữ liệu không hợp lệ"
                    return@withContext PhatNguoiResult(
                        error = true,
                        message = errorMessage
                    )
                }
            }

            // Xử lý success có thể là String "true"/"false" hoặc Boolean true/false
            val successValue = ajaxJson["success"]
            val success = when (successValue) {
                is Boolean -> successValue
                is String -> successValue.equals("true", ignoreCase = true)
                is Number -> successValue.toInt() != 0
                else -> false
            }

            if (!success) {
                val errorMsg = ajaxJson["message"] as? String ?: "Tra cứu thất bại"
                SecureLogger.e("Tra cứu thất bại: $errorMsg")
                return@withContext PhatNguoiResult(
                    error = true,
                    message = "Tra cứu thất bại: $errorMsg"
                )
            }

            // 3. Lấy href từ response và GET trang kết quả
            val href = ajaxJson["href"] as? String
            if (href.isNullOrEmpty()) {
                return@withContext PhatNguoiResult(
                    error = true,
                    message = "Không nhận được link kết quả từ CSGT"
                )
            }

            val resultUrl = if (href.startsWith("/")) {
                CSGT_BASE_URL + href
            } else {
                href
            }
            val resultRequest = Request.Builder()
                .url(resultUrl)
                .header("User-Agent", USER_AGENT)
                .header("Referer", CSGT_SEARCH_URL)
                .build()

            val resultResponse = executeWithRetry(sessionClient, resultRequest, maxRetries = 3)
            if (!resultResponse.isSuccessful) {
                throw IOException("Lỗi khi lấy trang kết quả: ${resultResponse.code}")
            }

            // Tối ưu: Đọc response với buffer size lớn hơn để nhanh hơn
            val htmlText = resultResponse.body?.use { it.string() } ?: ""
            resultResponse.close()

            // 4. Kiểm tra nếu có message "Không tìm thấy kết quả"
            val doc: Document = Jsoup.parse(htmlText)
            val bodyPrint = doc.selectFirst("div#bodyPrint123")
            
            // Kiểm tra message "Không tìm thấy kết quả"
            val noResultMessage = bodyPrint?.selectFirst("div[style*='color: red'], div[style*='color:red']")
            if (noResultMessage != null) {
                val messageText = noResultMessage.text().trim()
                if (messageText.contains("Không tìm thấy", ignoreCase = true)) {
                    return@withContext PhatNguoiResult(
                        error = false,
                        viPham = false,
                        message = "Không tìm thấy vi phạm giao thông cho biển số này"
                    )
                }
            }

            // 5. Parse HTML để lấy thông tin vi phạm
            val violations = parseViolationHtml(htmlText)

            if (violations.isEmpty()) {
                return@withContext if (bodyPrint != null) {
                    PhatNguoiResult(
                        error = false,
                        viPham = false,
                        message = "Không tìm thấy vi phạm giao thông cho biển số này"
                    )
                } else {
                    PhatNguoiResult(
                        error = true,
                        message = "Không thể lấy thông tin kết quả. Vui lòng thử lại."
                    )
                }
            }

            // Log số vi phạm parse được
            SecureLogger.d("Số vi phạm parse được: ${violations.size}")
            
            // Đếm số vi phạm đã xử phạt và chưa xử phạt dựa trên trạng thái
            var daXuPhat = 0
            var chuaXuPhat = 0
            
            violations.forEachIndexed { index, violation ->
                val trangThai = (violation["Trạng thái"] as? String)?.lowercase() ?: ""
                SecureLogger.d("Vi phạm ${index + 1}: Trạng thái = $trangThai")
                when {
                    trangThai.contains("đã xử phạt") || 
                    trangThai.contains("đã xử") || 
                    trangThai.contains("đã nộp") ||
                    trangThai.contains("đã thanh toán") -> {
                        daXuPhat++
                    }
                    trangThai.contains("chưa xử phạt") || 
                    trangThai.contains("chưa xử") || 
                    trangThai.contains("chưa nộp") ||
                    trangThai.contains("chưa thanh toán") -> {
                        chuaXuPhat++
                    }
                    else -> {
                        // Nếu không rõ trạng thái, mặc định là chưa xử phạt
                        chuaXuPhat++
                    }
                }
            }
            
            val soLoi = violations.size
            val violation = violations[0] // Lấy vi phạm đầu tiên để hiển thị chi tiết (backward compatibility)

            return@withContext PhatNguoiResult(
                error = false,
                viPham = true,
                message = "Có vi phạm",
                bienSo = violation["Biển kiểm soát"] as? String,
                mauBien = violation["Màu biển"] as? String,
                loaiPhuongTien = violation["Loại phương tiện"] as? String,
                thoiGianViPham = violation["Thời gian vi phạm"] as? String,
                diaDiemViPham = violation["Địa điểm vi phạm"] as? String,
                hanhViViPham = violation["Hành vi vi phạm"] as? String,
                trangThai = violation["Trạng thái"] as? String,
                donViPhatHien = violation["Đơn vị phát hiện vi phạm"] as? String,
                noiGiaiQuyet = (violation["Nơi giải quyết vụ việc"] as? List<String>) ?: emptyList(),
                soLoiViPham = soLoi,
                soDaXuPhat = daXuPhat,
                soChuaXuPhat = chuaXuPhat,
                allViolations = violations // Trả về tất cả vi phạm
            )

        } catch (e: Exception) {
            SecureLogger.e("Lỗi tra cứu", e)
            val errorMessage = secureErrorHandler?.handleError(e)?.message 
                ?: "Có lỗi xảy ra khi tra cứu"
            return@withContext PhatNguoiResult(
                error = true,
                message = errorMessage
            )
        }
    }

    /**
     * Lấy ảnh captcha và giải bằng AutoCaptcha
     */
    private suspend fun solveCaptcha(client: OkHttpClient, apiKey: String): String {
        return withContext(Dispatchers.IO) {
            // Lấy ảnh captcha
            val captchaRequest = Request.Builder()
                .url(CSGT_CAPTCHA_URL)
                .header("User-Agent", USER_AGENT)
                .build()
            val captchaResponse = executeWithRetry(client, captchaRequest, maxRetries = 3)
            if (!captchaResponse.isSuccessful) {
                throw IOException("Không thể lấy ảnh captcha: ${captchaResponse.code}")
            }
            val imageBytes = captchaResponse.body?.use { it.bytes() } ?: throw IOException("Không có dữ liệu ảnh")
            captchaResponse.close()

            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            val imageDataUri = "data:image/jpeg;base64,$base64Image"

            val payload = mapOf(
                "key" to apiKey,
                "type" to "imagetotext",
                "img" to imageDataUri,
                "casesensitive" to false
            )

            val jsonBody = gson.toJson(payload).toRequestBody("application/json".toMediaType())

            val autoCaptchaRequest = Request.Builder()
                .url(AUTOCAPTCHA_API_URL)
                .header("Content-Type", "application/json")
                .post(jsonBody)
                .build()

            val autoCaptchaResponse = executeWithRetry(client, autoCaptchaRequest, maxRetries = 3)
            val responseText = autoCaptchaResponse.body?.use { it.string() } ?: throw IOException("Không có response từ AutoCaptcha")
            autoCaptchaResponse.close()

            val responseJson = try {
                gson.fromJson(responseText, Map::class.java) as Map<String, Any>
            } catch (e: Exception) {
                throw IOException("Lỗi parse JSON từ AutoCaptcha: ${e.message}")
            }

            val success = responseJson["success"] as? Boolean ?: false
            if (!success) {
                val errorMsg = responseJson["message"] as? String ?: "Unknown error"
                throw IOException("Lỗi giải captcha: $errorMsg")
            }

            val captchaText = responseJson["captcha"] as? String
            if (captchaText.isNullOrEmpty()) {
                throw IOException("AutoCaptcha không trả về captcha")
            }

            SecureLogger.d("Captcha giải được")
            captchaText
        }
    }

    /**
     * Parse HTML để lấy thông tin vi phạm (hỗ trợ nhiều vi phạm được phân tách bởi <hr>)
     * Tối ưu: Chỉ parse những phần cần thiết, sử dụng selector hiệu quả hơn
     */
    private fun parseViolationHtml(htmlText: String): List<Map<String, Any>> {
        try {
            // Tối ưu: Chỉ parse phần bodyPrint123 thay vì toàn bộ HTML
            val bodyPrintStart = htmlText.indexOf("<div id=\"bodyPrint123\"")
            val bodyPrintEnd = htmlText.indexOf("</div>", bodyPrintStart + 1000) // Tìm closing tag gần nhất
            val relevantHtml = if (bodyPrintStart >= 0 && bodyPrintEnd > bodyPrintStart) {
                htmlText.substring(bodyPrintStart, bodyPrintEnd + 6) // +6 cho "</div>"
            } else {
                htmlText // Fallback: parse toàn bộ nếu không tìm thấy
            }
            
            val doc: Document = Jsoup.parse(relevantHtml)
            val form = doc.selectFirst("div#bodyPrint123")

            if (form == null) {
                SecureLogger.w("Không tìm thấy form bodyPrint123 trong HTML")
                return emptyList()
            }

            val violations = mutableListOf<Map<String, Any>>()
            // Tối ưu: Chỉ select form-group có chứa label (bỏ qua các div không có data)
            val formGroups = form.select("div.form-group:has(label.control-label)")
            
            SecureLogger.d("Tổng số formGroups: ${formGroups.size}")
            
            // Tìm các vị trí bắt đầu của mỗi vi phạm (dựa vào "Biển kiểm soát")
            // Mỗi vi phạm bắt đầu bằng một form-group có label "Biển kiểm soát"
            val violationStarts = mutableListOf<Int>()
            
            formGroups.forEachIndexed { index, group ->
                val row = group.selectFirst("div.row")
                val labelElem = row?.selectFirst("label.control-label")
                val label = labelElem?.text()?.replace(":", "")?.trim() ?: ""
                
                if (label.contains("Biển kiểm soát", ignoreCase = true)) {
                    violationStarts.add(index)
                    SecureLogger.d("Tìm thấy bắt đầu vi phạm tại index: $index")
                }
            }
            
            SecureLogger.d("Tổng số vi phạm tìm được: ${violationStarts.size}")
            
            // Nếu không tìm thấy "Biển kiểm soát" nào, parse toàn bộ như 1 vi phạm
            if (violationStarts.isEmpty()) {
                SecureLogger.d("Không tìm thấy 'Biển kiểm soát', parse toàn bộ như 1 vi phạm")
                val violation = parseSingleViolation(formGroups, 0, formGroups.size)
                if (violation.isNotEmpty()) {
                    violations.add(violation)
                }
                return violations
            }
            
            // Parse từng vi phạm
            violationStarts.forEachIndexed { violationIndex, startIndex ->
                val endIndex = if (violationIndex < violationStarts.size - 1) {
                    violationStarts[violationIndex + 1] // Kết thúc ở vi phạm tiếp theo
                } else {
                    formGroups.size // Vi phạm cuối cùng
                }
                
                val violation = parseSingleViolation(formGroups, startIndex, endIndex)
                if (violation.isNotEmpty()) {
                    violations.add(violation)
                    SecureLogger.d("Parse vi phạm ${violationIndex + 1} thành công ($startIndex đến $endIndex)")
                }
            }

            return violations

        } catch (e: Exception) {
            SecureLogger.e("Lỗi parse HTML", e)
            return emptyList()
        }
    }
    
    /**
     * Parse một vi phạm đơn lẻ từ formGroups trong khoảng [startIndex, endIndex)
     */
    private fun parseSingleViolation(
        formGroups: Elements,
        startIndex: Int,
        endIndex: Int
    ): Map<String, Any> {
        val violationData = mutableMapOf<String, Any>()
        var noiGiaiQuyetIndex = -1
        var donViPhatHien: String? = null

        for (i in startIndex until endIndex) {
            val group = formGroups[i]
            val row = group.selectFirst("div.row")

            if (row != null) {
                val labelElem = row.selectFirst("label.control-label")
                val valueElem = row.selectFirst("div.col-md-9")

                if (labelElem != null && valueElem != null) {
                    val label = labelElem.text().replace(":", "").trim()
                    var value = valueElem.text().trim()

                    when (label) {
                        "Biển kiểm soát" -> violationData["Biển kiểm soát"] = value
                        "Màu biển" -> violationData["Màu biển"] = value
                        "Loại phương tiện" -> violationData["Loại phương tiện"] = value
                        "Thời gian vi phạm" -> violationData["Thời gian vi phạm"] = value
                        "Địa điểm vi phạm" -> violationData["Địa điểm vi phạm"] = value
                        "Hành vi vi phạm" -> violationData["Hành vi vi phạm"] = value
                        "Trạng thái" -> {
                            val badge = valueElem.selectFirst("span.badge")
                            if (badge != null) {
                                value = badge.text().trim()
                            }
                            violationData["Trạng thái"] = value
                        }
                        "Đơn vị phát hiện vi phạm" -> {
                            donViPhatHien = value
                            violationData["Đơn vị phát hiện vi phạm"] = value
                        }
                        "Nơi giải quyết vụ việc" -> noiGiaiQuyetIndex = i
                    }
                }
            }
        }

        // Thu thập thông tin "Nơi giải quyết vụ việc"
        val noiGiaiQuyetList = mutableListOf<String>()
        if (noiGiaiQuyetIndex >= 0) {
            // Thêm "Đơn vị phát hiện vi phạm" vào đầu nếu có
            if (donViPhatHien != null) {
                noiGiaiQuyetList.add("Đơn vị phát hiện vi phạm:$donViPhatHien")
            }

            // Thu thập các form-group sau "Nơi giải quyết vụ việc" (trước <hr> hoặc hết formGroups)
            for (j in (noiGiaiQuyetIndex + 1) until endIndex) {
                val nextGroup = formGroups[j]
                val nextRow = nextGroup.selectFirst("div.row")

                // Nếu có row với label, có thể là field mới -> dừng
                if (nextRow != null) {
                    val labelElemNext = nextRow.selectFirst("label.control-label")
                    if (labelElemNext != null) {
                        break
                    }
                }

                // Kiểm tra nếu có <hr> thì dừng
                if (nextGroup.selectFirst("hr") != null) {
                    break
                }

                val text = nextGroup.text().trim()
                if (text.isNotEmpty()) {
                    noiGiaiQuyetList.add(text)
                }
            }

            if (noiGiaiQuyetList.isNotEmpty()) {
                violationData["Nơi giải quyết vụ việc"] = noiGiaiQuyetList
            }
        }

        return violationData
    }

    /**
     * Simple CookieJar implementation để giữ session
     */
    private class CookieJarImpl : CookieJar {
        private val cookies = mutableListOf<Cookie>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            this.cookies.addAll(cookies)
            // Giữ tối đa 100 cookies để tránh memory leak
            if (this.cookies.size > 100) {
                this.cookies.removeAt(0)
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookies.filter { it.matches(url) }
        }
    }

    /**
     * Thực thi request với retry logic cho SSL errors
     */
    private suspend fun executeWithRetry(
        client: OkHttpClient,
        request: Request,
        maxRetries: Int = 3
    ): Response {
        var lastException: Exception? = null
        
        for (attempt in 1..maxRetries) {
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful || attempt == maxRetries) {
                    return response
                }
                response.close()
            } catch (e: SSLException) {
                lastException = e
                SecureLogger.w("SSL error lần thử $attempt/$maxRetries: ${e.message}")
                if (attempt < maxRetries) {
                    // Exponential backoff: 1s, 2s, 4s
                    val delayMs = (1000L * (1 shl (attempt - 1)))
                    delay(delayMs)
                    continue
                }
            } catch (e: IOException) {
                lastException = e
                SecureLogger.w("IO error lần thử $attempt/$maxRetries: ${e.message}")
                if (attempt < maxRetries && (e.message?.contains("Connection reset", ignoreCase = true) == true ||
                        e.message?.contains("timeout", ignoreCase = true) == true)) {
                    val delayMs = (1000L * (1 shl (attempt - 1)))
                    delay(delayMs)
                    continue
                }
            }
        }
        
        throw lastException ?: IOException("Request failed after $maxRetries attempts")
    }

    /**
     * Tạo OkHttpClient với SSL verification phù hợp
     * - DEBUG mode: Disable SSL verification (để test với server có vấn đề SSL)
     * - RELEASE mode: Bật SSL verification đầy đủ (an toàn cho production)
     */
    private fun createOkHttpClient(): OkHttpClient {
        // Chỉ disable SSL verification trong debug mode
        if (BuildConfig.DEBUG) {
            SecureLogger.w("DEBUG MODE: SSL verification đã bị disable - CHỈ DÙNG CHO TEST")
            return createUnsafeOkHttpClientForDebug()
        }
        
        // Production: Sử dụng SSL verification bình thường với TLS protocols
        SecureLogger.d("RELEASE MODE: SSL verification đã được bật")
        return createSecureOkHttpClient()
    }
    
    /**
     * Tạo OkHttpClient an toàn với TLS protocols được chỉ định
     */
    private fun createSecureOkHttpClient(): OkHttpClient {
        return try {
            // Tạo SSL context với TLS 1.2 và 1.3
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, null, java.security.SecureRandom())
            
            // Tạo socket factory với TLS protocols
            val sslSocketFactory = object : SSLSocketFactory() {
                private val delegate = sslContext.socketFactory
                
                override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites
                override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites
                
                override fun createSocket(s: java.net.Socket?, host: String?, port: Int, autoClose: Boolean): java.net.Socket {
                    val socket = delegate.createSocket(s, host, port, autoClose) as SSLSocket
                    configureSocket(socket)
                    return socket
                }
                
                override fun createSocket(host: String?, port: Int): java.net.Socket {
                    val socket = delegate.createSocket(host, port) as SSLSocket
                    configureSocket(socket)
                    return socket
                }
                
                override fun createSocket(host: String?, port: Int, localHost: java.net.InetAddress?, localPort: Int): java.net.Socket {
                    val socket = delegate.createSocket(host, port, localHost, localPort) as SSLSocket
                    configureSocket(socket)
                    return socket
                }
                
                override fun createSocket(address: java.net.InetAddress?, port: Int): java.net.Socket {
                    val socket = delegate.createSocket(address, port) as SSLSocket
                    configureSocket(socket)
                    return socket
                }
                
                override fun createSocket(address: java.net.InetAddress?, port: Int, localAddress: java.net.InetAddress?, localPort: Int): java.net.Socket {
                    val socket = delegate.createSocket(address, port, localAddress, localPort) as SSLSocket
                    configureSocket(socket)
                    return socket
                }
                
                private fun configureSocket(socket: SSLSocket) {
                    // Chỉ định TLS protocols được hỗ trợ
                    val protocols = arrayOf("TLSv1.2", "TLSv1.3")
                    try {
                        socket.enabledProtocols = socket.supportedProtocols.filter { it in protocols }.toTypedArray()
                    } catch (e: Exception) {
                        SecureLogger.w("Không thể cấu hình TLS protocols: ${e.message}")
                    }
                }
            }
            
            OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, getDefaultTrustManager())
                .connectTimeout(20, TimeUnit.SECONDS) // Tăng timeout cho SSL handshake
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .connectionPool(okhttp3.ConnectionPool(10, 5, TimeUnit.MINUTES))
                .retryOnConnectionFailure(true)
                .build()
        } catch (e: Exception) {
            SecureLogger.e("Lỗi tạo secure OkHttpClient, dùng default", e)
            // Fallback to default
            OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .connectionPool(okhttp3.ConnectionPool(10, 5, TimeUnit.MINUTES))
                .retryOnConnectionFailure(true)
                .build()
        }
    }
    
    /**
     * Lấy default trust manager từ system
     */
    private fun getDefaultTrustManager(): X509TrustManager {
        val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        trustManagerFactory.init(null as java.security.KeyStore?)
        val trustManagers = trustManagerFactory.trustManagers
        return trustManagers.first { it is X509TrustManager } as X509TrustManager
    }
    
    /**
     * Tạo OkHttpClient không verify SSL - CHỈ DÙNG CHO DEBUG
     * ⚠️ CẢNH BÁO: Không an toàn, chỉ dùng để test
     */
    private fun createUnsafeOkHttpClientForDebug(): OkHttpClient {
        return try {
            // Tạo trust manager chấp nhận tất cả certificates
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
            })

            // Tạo SSL context với TLS
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            // Tạo ssl socket factory
            val sslSocketFactory: SSLSocketFactory = sslContext.socketFactory

            OkHttpClient.Builder()
                .sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true } // Bỏ qua hostname verification
                .connectTimeout(15, TimeUnit.SECONDS) // Giảm từ 30s xuống 15s
                .readTimeout(20, TimeUnit.SECONDS) // Giảm từ 30s xuống 20s
                .writeTimeout(15, TimeUnit.SECONDS) // Giảm từ 30s xuống 15s
                .connectionPool(okhttp3.ConnectionPool(10, 5, TimeUnit.MINUTES)) // Connection pooling tốt hơn
                .retryOnConnectionFailure(true) // Tự động retry khi connection fail
                .build()
        } catch (e: Exception) {
            SecureLogger.e("Lỗi tạo unsafe OkHttpClient cho debug", e)
            // Fallback: vẫn disable SSL verification
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS) // Giảm từ 30s xuống 15s
                .readTimeout(20, TimeUnit.SECONDS) // Giảm từ 30s xuống 20s
                .writeTimeout(15, TimeUnit.SECONDS) // Giảm từ 30s xuống 15s
                .connectionPool(okhttp3.ConnectionPool(10, 5, TimeUnit.MINUTES)) // Connection pooling tốt hơn
                .retryOnConnectionFailure(true) // Tự động retry khi connection fail
                .build()
        }
    }
}

