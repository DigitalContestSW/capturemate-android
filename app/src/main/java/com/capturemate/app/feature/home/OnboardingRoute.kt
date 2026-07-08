package com.capturemate.app.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capturemate.app.ui.theme.CaptureInk
import com.capturemate.app.ui.theme.CaptureMuted
import com.capturemate.app.ui.theme.CaptureMutedForeground
import com.capturemate.app.ui.theme.CaptureSurface
import kotlinx.coroutines.launch

private data class OnboardingSlide(val emoji: String, val title: String, val desc: String)

private val OnboardingSlides = listOf(
    OnboardingSlide(
        emoji = "🖼️",
        title = "갤러리 속에 묻힌\n정보를 꺼내드려요",
        desc = "스크린샷으로 저장해 둔 정보들,\n기억은커녕 찾기도 어렵죠?",
    ),
    OnboardingSlide(
        emoji = "🤖",
        title = "AI가 읽고\n메모로 만들어요",
        desc = "OCR로 텍스트를 추출하고\nAI가 핵심 정보만 골라 정리해요.",
    ),
    OnboardingSlide(
        emoji = "🔔",
        title = "잊을 만할 때\n다시 알려드려요",
        desc = "일정·마감·복습까지\n딱 그날 알림을 보내드려요.",
    ),
    OnboardingSlide(
        emoji = "🔒",
        title = "개인정보를\n안전하게 지켜요",
        desc = "스크린샷은 기기 안에서만 처리돼요.\n외부 서버로 전송되지 않아요.",
    ),
)

@Composable
fun OnboardingRoute(onDone: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { OnboardingSlides.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == OnboardingSlides.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CaptureSurface),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
        ) { page ->
            val slide = OnboardingSlides[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = slide.emoji, fontSize = 56.sp)
                Text(
                    text = slide.title,
                    modifier = Modifier.padding(top = 20.dp),
                    color = CaptureInk,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp,
                )
                Text(
                    text = slide.desc,
                    modifier = Modifier.padding(top = 10.dp),
                    color = CaptureMutedForeground,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            OnboardingSlides.indices.forEach { index ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                        .background(
                            if (index == pagerState.currentPage) CaptureInk else CaptureMuted,
                            CircleShape,
                        ),
                )
            }
        }

        Surface(
            onClick = {
                if (isLastPage) {
                    onDone()
                } else {
                    scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 24.dp),
            shape = RoundedCornerShape(14.dp),
            color = CaptureInk,
        ) {
            Box(
                modifier = Modifier.padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (isLastPage) "시작하기" else "다음",
                    color = CaptureSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}
