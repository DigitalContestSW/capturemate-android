package com.capturemate.app.feature.memo

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capturemate.app.data.local.entity.LifeInfoItemEntity
import com.capturemate.app.data.local.entity.ScheduleItemEntity
import com.capturemate.app.data.local.entity.StudyItemEntity
import com.capturemate.app.domain.repository.CaptureRepository
import com.capturemate.app.feature.common.categoryGlyph
import com.capturemate.app.feature.common.categoryLabel
import com.capturemate.app.feature.common.rememberLocalBitmap
import com.capturemate.app.ui.theme.CaptureBackground
import com.capturemate.app.ui.theme.CaptureBorder
import com.capturemate.app.ui.theme.CaptureDestructive
import com.capturemate.app.ui.theme.CaptureInk
import com.capturemate.app.ui.theme.CaptureMuted
import com.capturemate.app.ui.theme.CaptureMutedForeground
import com.capturemate.app.ui.theme.CaptureSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val reviewDayOptions = listOf(3, 7, 14, 30)

@Composable
fun MemoDetailRoute(
    memoId: String,
    repository: CaptureRepository,
    onBack: () -> Unit,
    viewModel: MemoViewModel = viewModel(factory = MemoViewModel.Factory(repository)),
) {
    val state by viewModel.detailState.collectAsState()
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var pendingGoogleCalendarMemoId by remember { mutableStateOf<String?>(null) }
    val googleCalendarAuthorizationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val memoId = pendingGoogleCalendarMemoId
        pendingGoogleCalendarMemoId = null
        if (memoId != null) {
            viewModel.finishAddScheduleToGoogleCalendar(
                context = context,
                memoId = memoId,
                data = if (result.resultCode == Activity.RESULT_OK) result.data else null,
            )
        }
    }

    LaunchedEffect(memoId) {
        viewModel.loadMemoDetail(memoId)
    }

    LaunchedEffect(viewModel) {
        viewModel.calendarEvents.collect { event ->
            when (event) {
                is GoogleCalendarUiEvent.RequestConsent -> {
                    pendingGoogleCalendarMemoId = memoId
                    googleCalendarAuthorizationLauncher.launch(
                        IntentSenderRequest.Builder(event.pendingIntent).build(),
                    )
                }
            }
        }
    }

    Scaffold(containerColor = CaptureBackground) { innerPadding ->
        val memo = state.memo
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (memo == null) {
                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                    Text(text = "불러오는 중...", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    DetailHeader(
                        title = memo.title,
                        onBack = onBack,
                        onMenuClick = { showMenu = true },
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        val scheduleItem = state.scheduleItem
                        val studyItem = state.studyItem
                        val lifeInfoItem = state.lifeInfoItem
                        val capture = state.capture
                        when {
                            scheduleItem != null && scheduleItem.screenshotUris.isNotEmpty() ->
                                ScreenshotStrip(screenshotUris = scheduleItem.screenshotUris)
                            studyItem != null && studyItem.screenshotUris.isNotEmpty() ->
                                ScreenshotStrip(screenshotUris = studyItem.screenshotUris)
                            lifeInfoItem != null && lifeInfoItem.screenshotUris.isNotEmpty() ->
                                ScreenshotStrip(screenshotUris = lifeInfoItem.screenshotUris)
                            capture != null -> ScreenshotStrip(screenshotUris = listOf(capture.localImageUri))
                        }

                        AiSummaryCard(
                            category = memo.category,
                            summary = memo.summary,
                            recommendedAction = memo.recommendedAction,
                        )

                        state.studyItem?.let { studyItem ->
                            StudySection(
                                studyItem = studyItem,
                                onSelectReviewDays = { days -> viewModel.selectReviewDays(memo.id, days) },
                            )
                        }

                        state.lifeInfoItem?.let { lifeInfoItem ->
                            LifeInfoSection(
                                lifeInfoItem = lifeInfoItem,
                                onToggleDeadlineReminder = { enabled ->
                                    viewModel.toggleDeadlineReminder(memo.id, enabled)
                                },
                                onSetCustomReminderDate = { at ->
                                    viewModel.setCustomReminderDate(memo.id, at)
                                },
                            )
                        }

                        state.scheduleItem?.let { scheduleItem ->
                            ScheduleSection(
                                scheduleItem = scheduleItem,
                                isAddingToGoogleCalendar = state.isAddingToGoogleCalendar,
                                googleCalendarMessage = state.googleCalendarMessage,
                                onAddToGoogleCalendar = {
                                    viewModel.addScheduleToGoogleCalendar(context, memo.id)
                                },
                                onSetCustomReminderDate = { at ->
                                    viewModel.setScheduleCustomReminderDate(memo.id, at)
                                },
                            )
                        }
                    }
                }

                if (showMenu) {
                    MemoMenuSheet(
                        onDismiss = { showMenu = false },
                        onDelete = {
                            showMenu = false
                            viewModel.deleteMemo(memo.id)
                            onBack()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailHeader(title: String, onBack: () -> Unit, onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CaptureSurface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(onClick = onBack, color = CaptureSurface) {
            Text(text = "‹", color = CaptureInk, fontSize = 26.sp, modifier = Modifier.padding(8.dp))
        }
        Text(
            text = title,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            color = CaptureInk,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Surface(onClick = onMenuClick, color = CaptureSurface) {
            Text(text = "⋯", color = CaptureMutedForeground, fontSize = 20.sp, modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
private fun MemoMenuSheet(onDismiss: () -> Unit, onDelete: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CaptureInk.copy(alpha = 0.3f))
                .clickable(onClick = onDismiss),
        )
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = CaptureSurface,
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    text = "메모 삭제",
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDelete)
                        .padding(vertical = 14.dp),
                    color = CaptureDestructive,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun AiSummaryCard(category: String, summary: String, recommendedAction: String?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CaptureSurface,
        border = BorderStroke(1.dp, CaptureBorder),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "⚡", fontSize = 13.sp)
                Text(
                    text = "AI 분석 결과",
                    modifier = Modifier.padding(start = 6.dp),
                    color = CaptureInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            SectionLabel("카테고리")
            Row(
                modifier = Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = RoundedCornerShape(999.dp), color = CaptureMuted) {
                    Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)) {
                        Text(text = categoryGlyph(category), fontSize = 10.sp)
                        Text(
                            text = categoryLabel(category),
                            modifier = Modifier.padding(start = 4.dp),
                            color = CaptureMutedForeground,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            SectionLabel("요약")
            Text(
                text = summary,
                modifier = Modifier.padding(top = 2.dp),
                color = CaptureInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )

            recommendedAction?.let { action ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = CaptureMuted,
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text(text = "추천 액션", color = CaptureMutedForeground, fontSize = 11.sp)
                        Text(
                            text = action,
                            modifier = Modifier.padding(top = 2.dp),
                            color = CaptureInk,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CaptureSurface,
        border = BorderStroke(1.dp, CaptureBorder),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, color = CaptureMutedForeground, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun StudySection(
    studyItem: StudyItemEntity,
    onSelectReviewDays: (Int) -> Unit,
) {
    var selectedDays by remember(studyItem.id) { mutableStateOf(studyItem.selectedReviewDays) }

    SectionCard {
        Text(text = "핵심 정리", color = CaptureInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        studyItem.keyPoints.forEachIndexed { index, point ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .background(CaptureMuted, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "${index + 1}", color = CaptureMutedForeground, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = point,
                    modifier = Modifier.padding(start = 10.dp),
                    color = CaptureInk,
                    fontSize = 14.sp,
                )
            }
        }

        Text(
            text = "복습 리마인드",
            modifier = Modifier.padding(top = 8.dp),
            color = CaptureInk,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            reviewDayOptions.forEach { days ->
                val selected = days == selectedDays
                Surface(
                    onClick = { selectedDays = days },
                    shape = RoundedCornerShape(999.dp),
                    color = if (selected) CaptureInk else CaptureMuted,
                ) {
                    Text(
                        text = "${days}일 후",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        color = if (selected) CaptureSurface else CaptureMutedForeground,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        val isConfirmed = selectedDays == studyItem.selectedReviewDays
        Surface(
            onClick = { onSelectReviewDays(selectedDays) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            shape = RoundedCornerShape(12.dp),
            color = CaptureMuted,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = if (isConfirmed) "✓" else "🔔", fontSize = 12.sp)
                Text(
                    text = if (isConfirmed) "알림 설정 완료" else "${selectedDays}일 후 복습 알림 설정",
                    modifier = Modifier.padding(start = 6.dp),
                    color = CaptureInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LifeInfoSection(
    lifeInfoItem: LifeInfoItemEntity,
    onToggleDeadlineReminder: (Boolean) -> Unit,
    onSetCustomReminderDate: (Long?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCard {
            Text(text = "핵심 정보", color = CaptureInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)

            Surface(shape = RoundedCornerShape(12.dp), color = CaptureMuted) {
                Column(modifier = Modifier.padding(12.dp)) {
                    SectionLabel("혜택")
                    Text(
                        text = lifeInfoItem.benefit,
                        modifier = Modifier.padding(top = 2.dp),
                        color = CaptureInk,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            SectionLabel("대상")
            Text(text = lifeInfoItem.target, color = CaptureInk, fontSize = 14.sp)

            SectionLabel("신청")
            Text(text = lifeInfoItem.applicationMethod, color = CaptureInk, fontSize = 14.sp)

            SectionLabel("마감")
            Text(text = formatDeadline(lifeInfoItem.deadline), color = CaptureDestructive, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

            Button(
                onClick = { onToggleDeadlineReminder(!lifeInfoItem.deadlineReminderEnabled) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = if (lifeInfoItem.deadlineReminderEnabled) {
                        "마감 3일 전 알림 설정됨"
                    } else {
                        "마감 3일 전 알림 설정"
                    },
                )
            }
        }

        ReminderPickerCard(
            initialDate = lifeInfoItem.customReminderAt,
            onSetCustomReminderDate = onSetCustomReminderDate,
        )
    }
}

private fun formatDeadline(epochMillis: Long): String {
    val formatter = SimpleDateFormat("yyyy년 M월 d일", Locale.KOREA)
    return formatter.format(Date(epochMillis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleSection(
    scheduleItem: ScheduleItemEntity,
    isAddingToGoogleCalendar: Boolean,
    googleCalendarMessage: String?,
    onAddToGoogleCalendar: () -> Unit,
    onSetCustomReminderDate: (Long?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "📅", fontSize = 12.sp)
                Text(
                    text = "감지된 일정",
                    modifier = Modifier.padding(start = 6.dp),
                    color = CaptureInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(text = scheduleItem.eventTitle, color = CaptureInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)

            scheduleItem.deadlineAt?.let { deadlineAt ->
                Text(
                    text = "마감 ${formatScheduleDeadline(deadlineAt)}",
                    color = CaptureDestructive,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            scheduleItem.eventDateText?.let { eventDateText ->
                Text(text = eventDateText, color = CaptureMutedForeground, fontSize = 13.sp)
            }

            scheduleItem.location?.let { location ->
                Text(text = location, color = CaptureMutedForeground, fontSize = 13.sp)
            }

            Button(
                onClick = onAddToGoogleCalendar,
                enabled = !isAddingToGoogleCalendar && scheduleItem.googleCalendarEventId == null,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = when {
                        scheduleItem.googleCalendarEventId != null -> "이미 추가됨"
                        isAddingToGoogleCalendar -> "추가 중..."
                        else -> "구글 캘린더에 추가"
                    },
                )
            }

            googleCalendarMessage?.let { message ->
                Text(text = message, color = CaptureMutedForeground, fontSize = 12.sp)
            }
        }

        ReminderPickerCard(
            initialDate = scheduleItem.customReminderAt,
            onSetCustomReminderDate = onSetCustomReminderDate,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderPickerCard(initialDate: Long?, onSetCustomReminderDate: (Long?) -> Unit) {
    SectionCard {
        var pickerVisible by remember { mutableStateOf(initialDate != null) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "리마인드 알림", color = CaptureInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "날짜 선택", color = CaptureMutedForeground, fontSize = 12.sp)
                Checkbox(
                    checked = pickerVisible,
                    onCheckedChange = { checked ->
                        pickerVisible = checked
                        if (!checked) onSetCustomReminderDate(null)
                    },
                )
            }
        }

        if (pickerVisible) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)
            DatePicker(state = datePickerState)

            LaunchedEffect(datePickerState.selectedDateMillis) {
                datePickerState.selectedDateMillis?.let(onSetCustomReminderDate)
            }
        }
    }
}

@Composable
private fun ScreenshotStrip(screenshotUris: List<String>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CaptureSurface,
        border = BorderStroke(1.dp, CaptureBorder),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "원본 스크린샷 ${screenshotUris.size}장",
                color = CaptureMutedForeground,
                fontSize = 12.sp,
            )
            Row(
                modifier = Modifier.padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                screenshotUris.forEachIndexed { index, uri ->
                    ScreenshotPreview(
                        uri = uri,
                        label = "${index + 1}/${screenshotUris.size}",
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenshotPreview(uri: String?, label: String) {
    val bitmap = rememberLocalBitmap(uri)

    Box(
        modifier = Modifier
            .size(100.dp)
            .background(CaptureMuted, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "원본 스크린샷 $label",
                modifier = Modifier
                    .fillMaxSize()
                    .background(CaptureMuted, RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop,
            )
        }
        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp),
            color = CaptureInk.copy(alpha = 0.5f),
            shape = RoundedCornerShape(6.dp),
        ) {
            Text(
                text = label,
                color = CaptureSurface,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

private fun formatScheduleDeadline(epochMillis: Long): String {
    val formatter = SimpleDateFormat("yyyy년 M월 d일 HH:mm", Locale.KOREA)
    return formatter.format(Date(epochMillis))
}
