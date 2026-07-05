package com.capturemate.app.feature.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capturemate.app.domain.repository.AuthRepository
import com.capturemate.app.feature.main.CaptureMateApp
import kotlinx.coroutines.delay

private val Ink = Color(0xFF111111)
private val Brand = Color(0xFF111111)
private val BorderGray = Color(0xFFE5E7EB)
private val TextGray = Color(0xFF6B7280)
private val MutedGray = Color(0xFF9CA3AF)

private data class OnboardingSlide(
    val emoji: String,
    val title: String,
    val desc: String,
)

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
    OnboardingSlide(
        emoji = "📷",
        title = "갤러리 접근 권한이\n필요해요",
        desc = "스크린샷을 불러오려면\n갤러리 접근 허용이 필요해요.",
    ),
)

@Composable
fun HomeRoute(
    authRepository: AuthRepository,
    viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(authRepository),
    ),
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val session = uiState.session
    var showInitialLoading by rememberSaveable { mutableStateOf(false) }

    Scaffold(containerColor = Color.White) { innerPadding ->
        when {
            session == null -> {
                LoginScreen(
                    loading = uiState.isLoading,
                    errorMessage = uiState.errorMessage,
                    onGoogleLogin = { viewModel.signIn(context) },
                    modifier = Modifier.padding(innerPadding),
                )
            }

            !uiState.onboardingCompleted -> {
                if (showInitialLoading) {
                    LoadingScreen(
                        onDone = {
                            viewModel.completeOnboarding()
                            showInitialLoading = false
                        },
                        modifier = Modifier.padding(innerPadding),
                    )
                } else {
                    OnboardingScreen(
                        onDone = { showInitialLoading = true },
                        modifier = Modifier.padding(innerPadding),
                    )
                }
            }

            else -> {
                CaptureMateApp(
                    onSignOut = viewModel::signOut,
                )
            }
        }
    }
}

@Composable
private fun LoadingScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val totalFiles = 23
    val steps = listOf(
        "스크린샷 불러오는 중",
        "OCR 텍스트 추출 중",
        "AI 유용성 판단 중",
        "카테고리 분류 중",
    )
    var progress by rememberSaveable { mutableIntStateOf(0) }
    var currentStep by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (progress < totalFiles) {
            delay(200)
            progress += 1
        }
        delay(500)
        onDone()
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            currentStep = (currentStep + 1) % steps.size
        }
    }
    val progressPercent = progress / totalFiles.toFloat()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Ink),
            contentAlignment = Alignment.Center,
        ) {
            Text("📷", fontSize = 34.sp)
        }
        Text(
            text = "스크린샷 분석 중",
            modifier = Modifier.padding(top = 22.dp),
            color = Ink,
            fontSize = 24.sp,
            lineHeight = 30.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "갤러리에서 유용한 정보를 찾고 있어요",
            modifier = Modifier.padding(top = 8.dp),
            color = TextGray,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 34.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(steps[currentStep], color = TextGray, fontSize = 13.sp)
                Text("$progress/$totalFiles", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 9.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color(0xFFE5E7EB)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressPercent.coerceIn(0f, 1f))
                        .fillMaxHeight()
                        .background(Ink),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFF3F4F6))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("지금까지 발견한 유용한 메모", color = TextGray, fontSize = 13.sp)
            Text(
                text = "${progress / 3}개",
                modifier = Modifier.padding(top = 6.dp),
                color = Ink,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            repeat(minOf(3, progress)) { index ->
                val fileNumber = progress - index
                val status = if (index == 0 && progress < totalFiles) "진행 중" else "완료"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF9FAFB))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Screenshot_$fileNumber.png", color = TextGray, fontSize = 12.sp)
                    Text(
                        status,
                        color = if (status == "진행 중") Ink else Color(0xFF16A34A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(
    loading: Boolean,
    errorMessage: String?,
    onGoogleLogin: () -> Unit,
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
            LogoBlock()

            GoogleSignInButton(
                loading = loading,
                onClick = onGoogleLogin,
                modifier = Modifier.fillMaxWidth(),
            )

            errorMessage?.let {
                Text(
                    text = it,
                    modifier = Modifier.padding(top = 14.dp),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    textAlign = TextAlign.Center,
                )
            }

            TermsText(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .padding(horizontal = 4.dp),
            )
        }

        Text(
            text = "v1.0.0",
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .padding(bottom = 40.dp),
            color = Color(0xFFD1D5DB),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LogoBlock() {
    Column(
        modifier = Modifier.padding(bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Ink),
            contentAlignment = Alignment.Center,
        ) {
            CameraGlyph(
                modifier = Modifier.size(28.dp),
                color = Color.White,
            )
        }

        Text(
            text = "CaptureMate",
            modifier = Modifier.padding(top = 16.dp),
            color = Ink,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Text(
            text = "갤러리 속 스크린샷을\nAI가 메모로 정리해드려요",
            modifier = Modifier.padding(top = 6.dp),
            color = TextGray,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun GoogleSignInButton(
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.material3.Surface(
        onClick = onClick,
        enabled = !loading,
        modifier = modifier
            .height(56.dp)
            .scale(if (loading) 0.995f else 1f)
            .border(
                width = 2.dp,
                color = BorderGray,
                shape = RoundedCornerShape(16.dp),
            ),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Ink,
                )
            } else {
                GoogleLogo(modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.size(12.dp))

            Text(
                text = if (loading) "로그인 중..." else "구글로 계속하기",
                color = Ink.copy(alpha = if (loading) 0.5f else 1f),
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
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
        modifier = modifier,
        color = MutedGray,
        fontSize = 11.sp,
        lineHeight = 17.sp,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun OnboardingScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    val slide = OnboardingSlides[step]

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OnboardingSlides.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .width(if (index == step) 24.dp else 8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (index == step) Brand else BorderGray),
                    )
                }
            }

            androidx.compose.material3.Surface(
                onClick = onDone,
                color = Color.Transparent,
            ) {
                Text(
                    text = "건너뛰기",
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                    color = MutedGray,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            EmojiTile(
                emoji = slide.emoji,
                step = step,
                modifier = Modifier.padding(bottom = 32.dp),
            )

            Text(
                text = slide.title,
                color = Ink,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = slide.desc,
                modifier = Modifier.padding(top = 12.dp),
                color = TextGray,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
            )

            if (step == 3) {
                PrivacyDetails(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                )
            }
        }

        androidx.compose.material3.Surface(
            onClick = {
                if (step == OnboardingSlides.lastIndex) {
                    onDone()
                } else {
                    step += 1
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            color = Brand,
            shadowElevation = 8.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = if (step == OnboardingSlides.lastIndex) "시작하기" else "다음",
                    color = Color.White,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun EmojiTile(
    emoji: String,
    step: Int,
    modifier: Modifier = Modifier,
) {
    val background = when (step) {
        0 -> Color(0xFF374151)
        1 -> Color(0xFF4B5563)
        2 -> Color(0xFF1F2937)
        3 -> Color(0xFF6B7280)
        else -> Ink
    }

    Box(
        modifier = modifier
            .size(112.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(background),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = emoji,
            fontSize = 56.sp,
            lineHeight = 64.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PrivacyDetails(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF9FAFB))
            .border(1.dp, Color(0xFFF3F4F6), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PrivacyDetailRow(
            icon = "📵",
            label = "서버 미전송",
            desc = "스크린샷·텍스트는 기기 밖으로 나가지 않아요",
        )
        PrivacyDetailRow(
            icon = "🔐",
            label = "온디바이스 AI",
            desc = "텍스트 분석은 기기 내 AI로 처리돼요",
        )
        PrivacyDetailRow(
            icon = "🗑️",
            label = "언제든 삭제",
            desc = "설정에서 모든 데이터를 즉시 삭제할 수 있어요",
        )
        PrivacyDetailRow(
            icon = "📋",
            label = "이용약관",
            desc = "로그인 시 이용약관 및 개인정보처리방침에 동의돼요",
        )
    }
}

@Composable
private fun PrivacyDetailRow(
    icon: String,
    label: String,
    desc: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(text = icon, fontSize = 16.sp, lineHeight = 20.sp)
        Column {
            Text(
                text = label,
                color = Ink,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = desc,
                modifier = Modifier.padding(top = 2.dp),
                color = TextGray,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun LoggedInScreen(
    name: String,
    onSignOut: () -> Unit,
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
                .fillMaxHeight()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            LogoBlock()

            Text(
                text = "${name}님, 환영합니다.",
                color = Ink,
                fontSize = 18.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "이 기기에 로그인 상태가 저장되었습니다.",
                modifier = Modifier.padding(top = 8.dp),
                color = TextGray,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
            )
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Text("로그아웃")
            }
        }
    }
}

@Composable
private fun CameraGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = Stroke(
            width = size.minDimension * 0.08f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.12f, size.height * 0.28f),
            size = Size(size.width * 0.76f, size.height * 0.56f),
            cornerRadius = CornerRadius(size.width * 0.12f),
            style = stroke,
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(size.width * 0.30f, size.height * 0.18f),
            size = Size(size.width * 0.28f, size.height * 0.16f),
            cornerRadius = CornerRadius(size.width * 0.04f),
            style = stroke,
        )
        drawCircle(
            color = color,
            radius = size.minDimension * 0.16f,
            center = Offset(size.width * 0.50f, size.height * 0.56f),
            style = stroke,
        )
    }
}

@Composable
private fun GoogleLogo(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.20f
        val bounds = Rect(
            left = strokeWidth / 2,
            top = strokeWidth / 2,
            right = size.width - strokeWidth / 2,
            bottom = size.height - strokeWidth / 2,
        )

        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -35f,
            sweepAngle = 95f,
            useCenter = false,
            topLeft = bounds.topLeft,
            size = bounds.size,
            style = Stroke(strokeWidth, cap = StrokeCap.Butt),
        )
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 60f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = bounds.topLeft,
            size = bounds.size,
            style = Stroke(strokeWidth, cap = StrokeCap.Butt),
        )
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 170f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = bounds.topLeft,
            size = bounds.size,
            style = Stroke(strokeWidth, cap = StrokeCap.Butt),
        )
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 250f,
            sweepAngle = 75f,
            useCenter = false,
            topLeft = bounds.topLeft,
            size = bounds.size,
            style = Stroke(strokeWidth, cap = StrokeCap.Butt),
        )

        val path = Path().apply {
            moveTo(size.width * 0.52f, size.height * 0.50f)
            lineTo(size.width * 0.90f, size.height * 0.50f)
            lineTo(size.width * 0.90f, size.height * 0.42f)
        }
        drawPath(
            path = path,
            color = Color(0xFF4285F4),
            style = Stroke(strokeWidth, cap = StrokeCap.Butt, join = StrokeJoin.Miter),
        )
    }
}
