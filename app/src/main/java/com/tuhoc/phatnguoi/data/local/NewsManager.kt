package com.tuhoc.phatnguoi.data.local

import com.tuhoc.phatnguoi.data.remote.NewsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class NewsItem(
    val id: Long = 0,
    val title: String,
    val imageUrl: String,
    val articleUrl: String,
    val publishedDate: Long,
    val description: String = ""
)

class NewsManager {
    private val newsService = NewsService()
    
    /**
     * Lấy tất cả tin tức từ CSGT (fetch trực tiếp, không lưu vào database)
     */
    suspend fun getAllNews(): List<NewsItem> {
        return withContext(Dispatchers.IO) {
            try {
                val newsList = newsService.fetchNewsFromCSGT(page = 1)
                // Loại bỏ duplicate dựa trên articleUrl
                newsList.distinctBy { it.articleUrl }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    /**
     * Lấy tin tức mới nhất (fetch trực tiếp từ CSGT)
     */
    suspend fun getLatestNews(limit: Int = 50): List<NewsItem> {
        return withContext(Dispatchers.IO) {
            try {
                val newsList = newsService.fetchNewsFromCSGT(page = 1)
                // Loại bỏ duplicate và giới hạn số lượng
                newsList.distinctBy { it.articleUrl }.take(limit)
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    /**
     * Tìm kiếm tin tức (filter từ kết quả fetch)
     */
    suspend fun searchNews(query: String): List<NewsItem> {
        return withContext(Dispatchers.IO) {
            try {
                val newsList = newsService.fetchNewsFromCSGT(page = 1)
                val lowerQuery = query.lowercase()
                // Tìm kiếm trong title và description
                newsList.filter { 
                    it.title.lowercase().contains(lowerQuery) || 
                    it.description.lowercase().contains(lowerQuery)
                }.distinctBy { it.articleUrl }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
    
    /**
     * Đồng bộ tin tức từ website CSGT (fetch trực tiếp)
     * @return Danh sách tin tức mới nhất
     */
    suspend fun syncNewsFromCSGT(): List<NewsItem> {
        return withContext(Dispatchers.IO) {
            try {
                val newsList = newsService.fetchNewsFromCSGT(page = 1)
                newsList.distinctBy { it.articleUrl }
            } catch (e: Exception) {
                e.printStackTrace()
                emptyList()
            }
        }
    }
}

