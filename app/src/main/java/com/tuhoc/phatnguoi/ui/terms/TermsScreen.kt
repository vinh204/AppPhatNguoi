package com.tuhoc.phatnguoi.ui.terms

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
fun TermsScreen() {
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
                        text = "Điều khoản sử dụng",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = RedPrimary,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Mục 1
                    TermsItem(
                        number = 1,
                        title = "Chấp nhận điều khoản",
                        content = """
                            Bằng việc tải, cài đặt và sử dụng ứng dụng Tra Cứu Phạt Nguội, bạn đồng ý tuân thủ các điều khoản sử dụng sau đây.
                            
                            Nếu không đồng ý với bất kỳ điều khoản nào, vui lòng ngừng sử dụng ứng dụng và gỡ bỏ ứng dụng khỏi thiết bị của bạn.
                            
                            Việc bạn tiếp tục sử dụng ứng dụng sau khi các điều khoản được cập nhật đồng nghĩa với việc bạn chấp nhận các điều khoản mới.
                        """.trimIndent()
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))
                    
                    // Mục 2
                    TermsItem(
                        number = 2,
                        title = "Mục đích sử dụng",
                        content = """
                            Ứng dụng Tra Cứu Phạt Nguội được cung cấp để:
                            
                            - Tra cứu thông tin vi phạm giao thông từ Cục Cảnh sát giao thông
                            - Hỗ trợ người dân kiểm tra phạt nguội một cách nhanh chóng và tiện lợi
                            - Lưu trữ lịch sử tra cứu trên thiết bị cá nhân
                            
                            Bạn chỉ được sử dụng ứng dụng cho mục đích hợp pháp và phù hợp với các quy định pháp luật hiện hành.
                            
                            KHÔNG được sử dụng ứng dụng cho các mục đích:
                            - Vi phạm pháp luật
                            - Lừa đảo hoặc gian lận
                            - Gây hại cho người khác
                        """.trimIndent()
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))
                    
                    // Mục 3
                    TermsItem(
                        number = 3,
                        title = "Trách nhiệm người dùng",
                        content = """
                            Khi sử dụng ứng dụng, bạn có trách nhiệm:
                            
                            - Cung cấp thông tin chính xác khi đăng ký tài khoản
                            - Bảo mật thông tin đăng nhập (mật khẩu, số điện thoại)
                            - Sử dụng ứng dụng đúng mục đích và tuân thủ pháp luật
                            - Không chia sẻ tài khoản với người khác
                            - Báo cáo ngay khi phát hiện lỗi hoặc sự cố bảo mật
                            
                            Bạn chịu trách nhiệm hoàn toàn về mọi hoạt động diễn ra dưới tài khoản của bạn.
                        """.trimIndent()
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))
                    
                    // Mục 4
                    TermsItem(
                        number = 4,
                        title = "Tính chính xác của thông tin",
                        content = """
                            Thông tin tra cứu được lấy từ nguồn chính thức của Cục Cảnh sát giao thông.
                            
                            Chúng tôi nỗ lực đảm bảo:
                            - Kết nối ổn định với nguồn dữ liệu chính thức
                            - Cập nhật thông tin kịp thời
                            - Hiển thị đầy đủ và chính xác kết quả tra cứu
                            
                            Tuy nhiên:
                            - Có thể có độ trễ trong việc cập nhật dữ liệu
                            - Thông tin chỉ mang tính chất tham khảo
                            - Bạn nên xác minh lại tại đơn vị CSGT nếu có nghi ngờ
                        """.trimIndent()
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))
                    
                    // Mục 5
                    TermsItem(
                        number = 5,
                        title = "Quyền sở hữu trí tuệ",
                        content = """
                            Tất cả nội dung trong ứng dụng (logo, giao diện, mã nguồn) thuộc quyền sở hữu của nhà phát triển.
                            
                            Bạn KHÔNG được:
                            - Sao chép, chỉnh sửa hoặc phân phối ứng dụng
                            - Sử dụng nội dung ứng dụng cho mục đích thương mại
                            - Đảo ngược kỹ thuật hoặc tách rời các thành phần của ứng dụng
                            
                            Bạn được phép:
                            - Sử dụng ứng dụng cho mục đích cá nhân
                            - Chia sẻ liên kết tải ứng dụng
                        """.trimIndent()
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))
                    
                    // Mục 6
                    TermsItem(
                        number = 6,
                        title = "Miễn trừ trách nhiệm",
                        content = """
                            Chúng tôi không chịu trách nhiệm cho:
                            
                            - Các thiệt hại phát sinh từ việc sử dụng hoặc không thể sử dụng ứng dụng
                            - Sự gián đoạn dịch vụ do lỗi kỹ thuật hoặc bảo trì
                            - Thông tin không chính xác do lỗi từ nguồn dữ liệu chính thức
                            - Hành vi sử dụng sai mục đích của người dùng
                            
                            Ứng dụng được cung cấp "nguyên trạng" và chúng tôi không đảm bảo:
                            - Ứng dụng hoạt động không lỗi
                            - Dịch vụ luôn sẵn sàng 24/7
                            - Thông tin luôn được cập nhật ngay lập tức
                        """.trimIndent()
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))
                    
                    // Mục 7
                    TermsItem(
                        number = 7,
                        title = "Chấm dứt sử dụng",
                        content = """
                            Chúng tôi có quyền chấm dứt hoặc tạm ngừng quyền sử dụng ứng dụng của bạn nếu:
                            
                            - Bạn vi phạm các điều khoản sử dụng
                            - Bạn sử dụng ứng dụng cho mục đích bất hợp pháp
                            - Phát hiện hành vi gian lận hoặc lừa đảo
                            - Yêu cầu của cơ quan nhà nước có thẩm quyền
                            
                            Bạn có quyền:
                            - Ngừng sử dụng ứng dụng bất cứ lúc nào
                            - Xóa tài khoản và dữ liệu liên quan
                            - Gỡ bỏ ứng dụng khỏi thiết bị
                        """.trimIndent()
                    )
                    
                    Spacer(Modifier.height(16.dp))
                    Divider()
                    Spacer(Modifier.height(16.dp))
                    
                    // Mục 8
                    TermsItem(
                        number = 8,
                        title = "Thay đổi điều khoản",
                        content = """
                            Chúng tôi có quyền cập nhật, thay đổi hoặc bổ sung các điều khoản sử dụng này bất cứ lúc nào.
                            
                            Khi có thay đổi:
                            - Thông báo sẽ được hiển thị trong ứng dụng
                            - Phiên bản mới sẽ được công bố với ngày hiệu lực
                            - Bạn nên xem lại các điều khoản định kỳ
                            
                            Việc bạn tiếp tục sử dụng ứng dụng sau khi có thay đổi được xem như bạn đã đồng ý với các điều khoản mới.
                            
                            Nếu có thắc mắc, vui lòng liên hệ: tracuuphatnguoi@gmail.com
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
fun TermsItem(
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

