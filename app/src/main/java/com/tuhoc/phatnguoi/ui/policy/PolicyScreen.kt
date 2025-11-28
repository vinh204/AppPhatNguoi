package com.tuhoc.phatnguoi.ui.policy

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tuhoc.phatnguoi.Divider
import com.tuhoc.phatnguoi.ui.theme.RedPrimary
import com.tuhoc.phatnguoi.ui.theme.TextPrimary
import com.tuhoc.phatnguoi.ui.theme.TextSub

@Composable
fun PolicyScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp)
        ) {
            Spacer(Modifier.height(16.dp))
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Chính sách bảo mật",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = RedPrimary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Mục 1
                    PolicyItem(
                        number = 1,
                        title = "Thu thập thông tin",
                        content = """
                            Ứng dụng Tra Cứu Phạt Nguội chỉ thu thập các thông tin cần thiết để cung cấp dịch vụ:
                            
                            - Số điện thoại: Để đăng nhập và quản lý tài khoản
                            - Lịch sử tra cứu: Để lưu lại các biển số đã tra cứu (chỉ lưu trên thiết bị)
                            - Thông tin biển số: Khi bạn tra cứu phạt nguội
                            
                            Chúng tôi KHÔNG thu thập:
                            - Vị trí địa lý
                            - Danh bạ liên hệ
                            - Ảnh hoặc file cá nhân
                            - Thông tin thanh toán
                        """.trimIndent()
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))
                    
                    // Mục 2
                    PolicyItem(
                        number = 2,
                        title = "Sử dụng thông tin",
                        content = """
                            Thông tin thu thập được sử dụng cho các mục đích:
                            
                            - Cung cấp dịch vụ tra cứu phạt nguội
                            - Xác thực và bảo mật tài khoản
                            - Lưu trữ lịch sử tra cứu trên thiết bị
                            - Cải thiện chất lượng dịch vụ
                            
                            Chúng tôi CAM KẾT không:
                            - Chia sẻ thông tin với bên thứ ba
                            - Sử dụng thông tin cho mục đích quảng cáo
                            - Bán thông tin cá nhân
                        """.trimIndent()
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))
                    
                    // Mục 3
                    PolicyItem(
                        number = 3,
                        title = "Bảo mật dữ liệu",
                        content = """
                            Chúng tôi áp dụng các biện pháp bảo mật:
                            
                            - Mã hóa thông tin đăng nhập
                            - Lưu trữ dữ liệu cục bộ trên thiết bị
                            - Không truyền dữ liệu nhạy cảm qua mạng không bảo mật
                            - Kiểm tra và cập nhật bảo mật thường xuyên
                            
                            Lưu ý: 
                            - Thông tin tra cứu chỉ được lưu trên thiết bị của bạn
                            - Khi xóa ứng dụng, tất cả dữ liệu sẽ bị xóa
                        """.trimIndent()
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))
                    
                    // Mục 4
                    PolicyItem(
                        number = 4,
                        title = "Quyền của người dùng",
                        content = """
                            Bạn có các quyền sau:
                            
                            - Quyền truy cập: Xem thông tin tài khoản của mình
                            - Quyền chỉnh sửa: Thay đổi mật khẩu
                            - Quyền xóa: Xóa lịch sử tra cứu
                            - Quyền xóa tài khoản: Xóa hoàn toàn tài khoản và dữ liệu
                            
                            Để thực hiện các quyền trên, vui lòng liên hệ qua email:
                            tracuuphatnguoi@gmail.com
                        """.trimIndent()
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))
                    
                    // Mục 5
                    PolicyItem(
                        number = 5,
                        title = "Thay đổi chính sách",
                        content = """
                            Chúng tôi có thể cập nhật chính sách bảo mật này theo thời gian.
                            
                            Khi có thay đổi quan trọng:
                            - Thông báo sẽ được gửi qua ứng dụng
                            - Phiên bản mới của chính sách sẽ được công bố
                            - Ngày cập nhật sẽ được ghi rõ
                            
                            Việc bạn tiếp tục sử dụng ứng dụng sau khi có thay đổi đồng nghĩa với việc bạn chấp nhận chính sách mới.
                        """.trimIndent()
                    )
                    
                    Spacer(Modifier.height(8.dp))
                }
            }
            
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun PolicyItem(
    number: Int,
    title: String,
    content: String
) {
    Column {
        Row(
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            // Số thứ tự
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = RedPrimary.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text(
                    text = number.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = RedPrimary
                )
            }
            
            Spacer(Modifier.width(12.dp))
            
            // Tiêu đề
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        
        // Nội dung
        Text(
            text = content,
            fontSize = 14.sp,
            color = TextPrimary,
            lineHeight = 22.sp,
            textAlign = TextAlign.Justify,
            modifier = Modifier.padding(start = 44.dp)
        )
    }
}

