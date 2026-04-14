package com.example.sketchnote.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sketchnote.R

// Bảng màu chuẩn theo thiết kế của cưng
private val YellowHighlight = Color(0xFFF9DC3B)
private val YellowText = Color(0xFFFFDF2D)
private val BlackBtn = Color(0xFF000000)
private val GrayDescription = Color(0xFF676666)
private val LightGrayTagline = Color(0xFF959595)

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val contentAlpha = remember { Animatable(0f) }

    // Hiệu ứng fade-in khi vào màn hình
    LaunchedEffect(Unit) {
        contentAlpha.animateTo(1f, animationSpec = tween(800))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .alpha(contentAlpha.value)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 29.dp)
        ) {
            // ── TOP: Tiêu đề và 3 Trái tim bay so le đè lên nhau ──
            Spacer(modifier = Modifier.height(74.dp))

            Box(modifier = Modifier.fillMaxWidth().height(120.dp)) {
                Column {
                    Text(
                        text = "SketchNote,",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = YellowText,
                        softWrap = false // Ép chữ không xuống hàng
                    )
                    Text(
                        text = "Xin chào bạn",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = YellowText,
                        softWrap = false
                    )
                }

                // Trái tim 1: To nhất, nằm cao nhất bên phải
                Image(
                    painter = painterResource(id = R.drawable.heartsplash),
                    contentDescription = null,
                    modifier = Modifier
                        .size(70.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (10).dp, y = (-10).dp)
                )

                // Trái tim 2: Nhỏ, đè nhẹ lên tim 1 và nằm thấp hơn
                Image(
                    painter = painterResource(id = R.drawable.heartsplash),
                    contentDescription = null,
                    modifier = Modifier
                        .size(45.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-40).dp, y = (40).dp)
                )

                // Trái tim 3: Vừa, nằm sát về phía chữ hơn
                Image(
                    painter = painterResource(id = R.drawable.heartsplash),
                    contentDescription = null,
                    modifier = Modifier
                        .size(55.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = (-80).dp, y = (0).dp)
                )
            }

            // ── MIDDLE: Ảnh minh họa trung tâm (Ảnh to rõ) ──
            Spacer(modifier = Modifier.height(10.dp))
            Image(
                painter = painterResource(id = R.drawable.splash),
                contentDescription = "Main Illustration",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                contentScale = ContentScale.Fit
            )

            // ── BOTTOM: Cụm thông tin App (Đã đẩy xít lên trên) ──
            Spacer(modifier = Modifier.weight(1f))

            Column(
                modifier = Modifier.offset(y = (-25).dp) // Đẩy toàn bộ cụm này lên cao hơn 25dp
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(9.dp))
                        .background(YellowHighlight)
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "SketchNote",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        softWrap = false
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Nơi lưu giữ\nmọi ghi chú của bạn",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Medium,
                    color = GrayDescription,
                    lineHeight = 38.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Viết nhanh, tìm dễ, không bao giờ quên mọi suy nghĩ đều xứng đáng được ghi lại.",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal,
                    color = LightGrayTagline,
                    lineHeight = 26.sp
                )
            }

            // Khoảng trống lớn ở đáy để tạo sự thông thoáng với nút bấm
            Spacer(modifier = Modifier.height(140.dp))
        }

        // ── NÚT BẤM GET STARTED (Căn giữa đáy màn hình) ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(15.dp))
                    .background(BlackBtn)
                    .clickable {
                        // Nhấn vào đây mới thực hiện chuyển màn hình tiếp theo
                        onFinished()
                    }
                    .padding(horizontal = 60.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Get started",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}