package com.tuhoc.phatnguoi.ui.news

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import com.tuhoc.phatnguoi.data.local.NewsItem
import com.tuhoc.phatnguoi.data.local.NewsManager
import com.tuhoc.phatnguoi.ui.theme.RedPrimary
import com.tuhoc.phatnguoi.ui.theme.TextPrimary
import com.tuhoc.phatnguoi.ui.theme.TextSub

@Composable
fun NewsScreen(newsManager: NewsManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var newsList by remember { mutableStateOf<List<NewsItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    
    // Function để load news từ CSGT (fetch trực tiếp)
    fun loadNews() {
        scope.launch {
            isLoading = true
            newsList = newsManager.getAllNews()
            isLoading = false
        }
    }
    
    // Function để refresh news từ CSGT
    fun syncNews() {
        scope.launch {
            isRefreshing = true
            val fetchedNews = newsManager.syncNewsFromCSGT()
            if (fetchedNews.isNotEmpty()) {
                newsList = fetchedNews
            }
            isRefreshing = false
        }
    }
    
    // Load news lần đầu
    LaunchedEffect(Unit) {
        loadNews()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = RedPrimary)
            }
        } else if (newsList.isEmpty()) {
            EmptyNewsView(
                onRefresh = { syncNews() },
                isRefreshing = isRefreshing
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 0.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(newsList) { news ->
                    NewsItemCard(
                        news = news,
                        onClick = {
                            // Mở link trong browser
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(news.articleUrl))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    )
                }
            }
            
            // FloatingActionButton để refresh
            FloatingActionButton(
                onClick = { syncNews() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = RedPrimary,
                contentColor = Color.White
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "Làm mới"
                    )
                }
            }
        }
    }
}

@Composable
fun NewsItemCard(
    news: NewsItem,
    onClick: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    val dateStr = try {
        dateFormat.format(Date(news.publishedDate))
    } catch (e: Exception) {
        ""
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Hình ảnh thumbnail
                if (news.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = news.imageUrl,
                        contentDescription = news.title,
                        modifier = Modifier
                            .width(120.dp)
                            .height(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F5F5)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Placeholder nếu không có hình ảnh
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF5F5F5))
                    )
                }
                
                // Tiêu đề và thông tin
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = news.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 20.sp
                    )
                    
                    if (dateStr.isNotEmpty()) {
                        Text(
                            text = dateStr,
                            fontSize = 12.sp,
                            color = TextSub
                        )
                    }
                }
            }
            
            // Mô tả (nếu có)
            if (news.description.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = news.description,
                    fontSize = 13.sp,
                    color = TextSub,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun EmptyNewsView(
    onRefresh: () -> Unit = {},
    isRefreshing: Boolean = false
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Chưa có tin tức",
                fontSize = 16.sp,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onRefresh,
                enabled = !isRefreshing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = RedPrimary,
                    contentColor = Color.White
                )
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Làm mới")
                }
            }
        }
    }
}

