package com.tuhoc.phatnguoi.data.remote

import com.tuhoc.phatnguoi.data.local.NewsItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.*

class NewsService {
    private val baseUrl = "https://www.csgt.vn"
    private val newsUrl = "$baseUrl/tintuc/"
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN"))
    
    suspend fun fetchNewsFromCSGT(page: Int = 1): List<NewsItem> {
        return withContext(Dispatchers.IO) {
            try {
                val url = if (page == 1) newsUrl else "$newsUrl/$page/"
                val doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                    .timeout(15000)
                    .get()
                
                val newsList = mutableListOf<NewsItem>()
                val seenUrls = mutableSetOf<String>() // Để tránh trùng lặp
                
                // Parse các bài viết từ HTML
                val listItems = doc.select("div.list")
                
                listItems.forEachIndexed { index, item ->
                    try {
                        // Lấy hình ảnh
                        val imgElement = item.selectFirst("img")
                        val imageUrl = imgElement?.attr("src")?.let {
                            if (it.startsWith("http")) it else {
                                if (it.startsWith("//")) "https:$it" else "$baseUrl$it"
                            }
                        } ?: ""
                        
                        // Lấy tiêu đề và link
                        val linkElement = item.selectFirst("a[href]")
                        val title = linkElement?.text()?.trim() ?: ""
                        val articleUrl = linkElement?.attr("href")?.let {
                            if (it.startsWith("http")) it else {
                                if (it.startsWith("//")) "https:$it" else "$baseUrl$it"
                            }
                        } ?: ""
                        
                        // Lấy ngày đăng
                        val dateElement = item.selectFirst("font i")
                        val dateText = dateElement?.text()?.replace("Ngày đăng:", "")?.replace("&nbsp;", " ")?.trim() ?: ""
                        val publishedDate = try {
                            if (dateText.isNotEmpty()) {
                                dateFormat.parse(dateText)?.time ?: System.currentTimeMillis()
                            } else {
                                System.currentTimeMillis()
                            }
                        } catch (e: Exception) {
                            System.currentTimeMillis()
                        }
                        
                        // Lấy mô tả
                        val description = item.selectFirst("span")?.text()?.trim() ?: ""
                        
                        // Chỉ thêm tin tức có mô tả và chưa tồn tại (dựa trên articleUrl)
                        if (title.isNotEmpty() && articleUrl.isNotEmpty() && description.isNotEmpty() && !seenUrls.contains(articleUrl)) {
                            seenUrls.add(articleUrl)
                            newsList.add(
                                NewsItem(
                                    id = 0, // Sẽ được auto-generate khi insert vào DB
                                    title = title,
                                    imageUrl = imageUrl,
                                    articleUrl = articleUrl,
                                    publishedDate = publishedDate,
                                    description = description
                                )
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                
                newsList
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}

