package com.tuhoc.phatnguoi.ui.faq

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.size
import com.tuhoc.phatnguoi.Divider
import com.tuhoc.phatnguoi.ui.theme.RedPrimary
import com.tuhoc.phatnguoi.ui.theme.TextPrimary
import com.tuhoc.phatnguoi.ui.theme.TextSub

data class FAQItem(
    val question: String,
    val answer: String
)

@Composable
fun FAQScreen() {
    val faqItems = remember {
        listOf(
            FAQItem(
                question = "Tra cứu phạt nguội tại đây có chính xác không?",
                answer = """
                    Cục Cảnh sát giao thông (CSGT) đã triển khai website https://csgt.vn để xử lý thống nhất phạt nguội thông qua hình ảnh, video từ camera giám sát.
                    
                    Sau khi xác minh, CSGT sẽ cập nhật thông tin vi phạm lên website, cho phép người dân tra cứu và thanh toán phạt trước khi đi đăng kiểm xe.
                    
                    CSGT cũng sẽ gửi thông báo vi phạm đến địa chỉ đăng ký xe theo thông tin trong hệ thống.
                    
                    Khi đến các đơn vị CSGT tại địa phương, người dân sẽ được cung cấp đầy đủ hình ảnh, video và thông tin chi tiết về vi phạm và mức phạt.
                    
                    Lưu ý: Mọi hành vi gọi điện thông báo phạt nguội đều là lừa đảo, cần xác minh kỹ trước khi chuyển tiền hay đóng phạt cho bất cứ cá nhân nào khác.
                """.trimIndent()
            ),
            FAQItem(
                question = "Tôi cần tra như thế nào?",
                answer = """
                    Bạn có thể tra cứu phạt nguội theo các bước sau:
                    
                    1. Nhập biển số xe (không có ký tự đặc biệt, viết liền)
                    2. Chọn loại phương tiện (Ô tô, Xe máy, hoặc Xe đạp điện)
                    3. Nhấn nút "Tra ngay"
                    4. Chờ kết quả tra cứu từ hệ thống
                    
                    Nếu có vi phạm, bạn sẽ thấy thông tin chi tiết về: thời gian, địa điểm, hành vi vi phạm, trạng thái xử phạt và nơi giải quyết.
                """.trimIndent()
            ),
            FAQItem(
                question = "Tôi nhận được thông báo phạt nguội về nhà, nhưng tra cứu không có lỗi vi phạm?",
                answer = """
                    Trong trường hợp này, bạn có thể:
                    
                    1. Kiểm tra lại thông tin biển số xe và loại phương tiện đã nhập chính xác chưa
                    2. Đợi vài ngày rồi tra cứu lại, vì có thể dữ liệu chưa được cập nhật kịp thời
                    3. Liên hệ trực tiếp với Cục Cảnh sát giao thông để xác minh
                    4. Đến đơn vị CSGT tại địa phương để kiểm tra trực tiếp
                    
                    Nếu thông báo được gửi qua điện thoại mà không có giấy tờ chính thức, hãy cảnh giác với lừa đảo.
                """.trimIndent()
            ),
            FAQItem(
                question = "Tôi có thể xử lý vi phạm tại địa phương không?",
                answer = """
                    Có, bạn có thể xử lý vi phạm tại địa phương. Thông tin về nơi giải quyết vụ việc sẽ được hiển thị trong kết quả tra cứu.
                    
                    Khi đến đơn vị CSGT, bạn sẽ được:
                    - Xem hình ảnh, video vi phạm
                    - Nhận thông tin chi tiết về vi phạm và mức phạt
                    - Thanh toán phạt theo quy định
                    - Nhận giấy tờ chứng nhận đã xử phạt
                    
                    Vui lòng mang theo: CMND/CCCD, Giấy đăng ký xe, Giấy phép lái xe khi đến xử lý.
                """.trimIndent()
            ),
            FAQItem(
                question = "Nộp phạt trực tuyến trên cổng dịch vụ công đối với vi phạm giao thông đường bộ như thế nào?",
                answer = """
                    Bạn có thể nộp phạt trực tuyến qua cổng dịch vụ công quốc gia tại:
                    https://dichvucong.gov.vn/p/home/dvc-thanh-toan-vi-pham-giao-thong.html
                    
                    Các bước thực hiện:
                    1. Truy cập cổng dịch vụ công
                    2. Đăng nhập/Đăng ký tài khoản
                    3. Tra cứu vi phạm theo mã quyết định hoặc biên bản vi phạm
                    4. Xác nhận thông tin vi phạm
                    5. Chọn phương thức thanh toán và hoàn tất
                    
                    Lưu ý: Chỉ thanh toán được các vi phạm thuộc thẩm quyền của Cục Cảnh sát giao thông và Thanh tra giao thông đường bộ.
                """.trimIndent()
            ),
            FAQItem(
                question = "Tôi nhận được cuộc gọi thông báo lỗi vi phạm",
                answer = """
                    CẢNH BÁO: Đây có thể là hành vi lừa đảo!
                    
                    Cục Cảnh sát giao thông KHÔNG gọi điện thông báo phạt nguội qua điện thoại. Thông báo chính thức sẽ được gửi qua đường bưu điện đến địa chỉ đăng ký xe.
                    
                    Nếu nhận được cuộc gọi yêu cầu:
                    - Chuyển tiền ngay lập tức
                    - Cung cấp thông tin thẻ ATM/ngân hàng
                    - Thanh toán qua số điện thoại lạ
                    
                    → Hãy từ chối và báo cáo ngay cho cơ quan chức năng.
                    
                    Để tra cứu chính xác, hãy sử dụng ứng dụng này hoặc truy cập website chính thức của CSGT.
                """.trimIndent()
            ),
            FAQItem(
                question = "Bị dính phạt nguội, có thể đăng kiểm xe không?",
                answer = """
                    Không, bạn KHÔNG THỂ đăng kiểm xe nếu còn vi phạm chưa được xử lý.
                    
                    Theo quy định:
                    - Tất cả các vi phạm giao thông phải được xử lý trước khi đăng kiểm
                    - Hệ thống đăng kiểm sẽ kiểm tra và từ chối nếu phát hiện vi phạm chưa xử lý
                    
                    Vì vậy, bạn cần:
                    1. Tra cứu và kiểm tra tất cả vi phạm
                    2. Thanh toán phạt và xử lý các vi phạm còn tồn đọng
                    3. Chờ hệ thống cập nhật trạng thái đã xử phạt
                    4. Sau đó mới đến đăng kiểm xe
                    
                    Việc xử phạt kịp thời sẽ giúp bạn tránh bị phạt chậm nộp và đảm bảo đăng kiểm đúng hạn.
                """.trimIndent()
            ),
            FAQItem(
                question = "Tôi cho thuê xe, nhưng sau đó xe bị dính lỗi phạt nguội thì giải quyết như thế nào?",
                answer = """
                    Khi cho thuê xe mà bị phạt nguội, bạn có các lựa chọn:
                    
                    Nếu có hợp đồng cho thuê xe rõ ràng:
                    1. Người thuê xe phải chịu trách nhiệm về vi phạm trong thời gian thuê
                    2. Bạn có thể yêu cầu người thuê thanh toán phạt
                    3. Lưu giữ hợp đồng và biên lai thanh toán để làm bằng chứng
                    
                    Nếu không có hợp đồng hoặc người thuê không chịu trách nhiệm:
                    1. Chủ xe (bạn) phải thanh toán phạt trước
                    2. Sau đó có thể yêu cầu người thuê bồi thường theo quy định dân sự
                    3. Lưu ý: Cơ quan CSGT chỉ xử phạt chủ xe đăng ký trong hệ thống
                    
                    Khuyến nghị: Luôn có hợp đồng cho thuê xe rõ ràng, quy định trách nhiệm về vi phạm giao thông để tránh tranh chấp.
                """.trimIndent()
            )
        )
    }
    
    var expandedItems by remember { mutableStateOf<Set<Int>>(emptySet()) }
    
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
            
            faqItems.forEachIndexed { index, faq ->
                val isExpanded = expandedItems.contains(index)
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column {
                        // Câu hỏi - có thể click để mở/đóng
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    expandedItems = if (isExpanded) {
                                        expandedItems - index
                                    } else {
                                        expandedItems + index
                                    }
                                }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = faq.question,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = if (isExpanded) "Thu gọn" else "Mở rộng",
                                tint = RedPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        
                        // Câu trả lời - chỉ hiển thị khi expanded
                        if (isExpanded) {
                            Divider(
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Text(
                                text = faq.answer,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                lineHeight = 22.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(16.dp))
        }
    }
}

