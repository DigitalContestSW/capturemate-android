package com.capturemate.app.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xFF111111)
private val SubtleText = Color(0xFF6B7280)
private val FineText = Color(0xFF9CA3AF)
private val BorderGray = Color(0xFFE5E7EB)

@Composable
fun LoginScreen(
    isLoading: Boolean,
    errorMessage: String?,
    versionName: String,
    onGoogleClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(Ink),
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            LogoMark(modifier = Modifier.size(64.dp))

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "CaptureMate",
                color = Ink,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            )
            Text(
                text = "갤러리 속 스크린샷을\nAI가 메모로 정리해드려요",
                color = SubtleText,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 40.dp),
            )

            Surface(
                onClick = onGoogleClick,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 1.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(2.dp, BorderGray, RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Ink,
                        )
                    } else {
                        GoogleLogo(modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.size(12.dp))

                    Text(
                        text = if (isLoading) "로그인 중..." else "구글로 계속하기",
                        color = if (isLoading) Ink.copy(alpha = 0.5f) else Ink,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            TermsText(
                modifier = Modifier.padding(top = 24.dp),
            )
        }

        Text(
            text = "v$versionName",
            color = Color(0xFFD1D5DB),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(bottom = 40.dp),
        )
    }
}

@Composable
private fun LogoMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Ink),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(30.dp)) {
            val stroke = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(size.width * 0.1f, size.height * 0.28f),
                size = Size(size.width * 0.8f, size.height * 0.55f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),
                style = stroke,
            )
            drawCircle(
                color = Color.White,
                radius = size.width * 0.16f,
                center = Offset(size.width * 0.5f, size.height * 0.56f),
                style = stroke,
            )
            drawLine(
                color = Color.White,
                start = Offset(size.width * 0.28f, size.height * 0.28f),
                end = Offset(size.width * 0.36f, size.height * 0.15f),
                strokeWidth = 2.6.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color.White,
                start = Offset(size.width * 0.36f, size.height * 0.15f),
                end = Offset(size.width * 0.55f, size.height * 0.15f),
                strokeWidth = 2.6.dp.toPx(),
                cap = StrokeCap.Round,
            )
            drawLine(
                color = Color.White,
                start = Offset(size.width * 0.55f, size.height * 0.15f),
                end = Offset(size.width * 0.63f, size.height * 0.28f),
                strokeWidth = 2.6.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun GoogleLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.22f
        val rect = Rect(
            offset = Offset(strokeWidth / 2f, strokeWidth / 2f),
            size = Size(size.width - strokeWidth, size.height - strokeWidth),
        )
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -38f,
            sweepAngle = 86f,
            useCenter = false,
            topLeft = rect.topLeft,
            size = rect.size,
            style = Stroke(strokeWidth, cap = StrokeCap.Square),
        )
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 48f,
            sweepAngle = 104f,
            useCenter = false,
            topLeft = rect.topLeft,
            size = rect.size,
            style = Stroke(strokeWidth, cap = StrokeCap.Square),
        )
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 152f,
            sweepAngle = 84f,
            useCenter = false,
            topLeft = rect.topLeft,
            size = rect.size,
            style = Stroke(strokeWidth, cap = StrokeCap.Square),
        )
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 236f,
            sweepAngle = 86f,
            useCenter = false,
            topLeft = rect.topLeft,
            size = rect.size,
            style = Stroke(strokeWidth, cap = StrokeCap.Square),
        )
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(size.width * 0.52f, size.height * 0.5f),
            end = Offset(size.width * 0.95f, size.height * 0.5f),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Square,
        )
    }
}

@Composable
private fun TermsText(modifier: Modifier = Modifier) {
    Text(
        text = buildAnnotatedString {
            append("로그인하면 ")
            withStyle(SpanStyle(color = Ink, fontWeight = FontWeight.Medium)) {
                append("이용약관")
            }
            append("과 ")
            withStyle(SpanStyle(color = Ink, fontWeight = FontWeight.Medium)) {
                append("개인정보처리방침")
            }
            append("에 동의하게 됩니다.\n스크린샷은 기기 내에서만 처리되며 외부 서버로 전송되지 않아요.")
        },
        color = FineText,
        fontSize = 11.sp,
        lineHeight = 17.sp,
        textAlign = TextAlign.Center,
        modifier = modifier.padding(horizontal = 4.dp),
    )
}
