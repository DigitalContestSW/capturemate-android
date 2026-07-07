package com.capturemate.app.feature.memo

import android.app.Activity
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capturemate.app.data.local.entity.LifeInfoItemEntity
import com.capturemate.app.data.local.entity.ScheduleItemEntity
import com.capturemate.app.data.local.entity.StudyItemEntity
import com.capturemate.app.domain.repository.CaptureRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val reviewDayOptions = listOf(3, 7, 14, 30)

@Composable
fun MemoDetailRoute(
    memoId: String,
    repository: CaptureRepository,
    viewModel: MemoViewModel = viewModel(factory = MemoViewModel.Factory(repository)),
) {
    val state by viewModel.detailState.collectAsState()
    val context = LocalContext.current
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

    Scaffold { innerPadding ->
        val memo = state.memo
        if (memo == null) {
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(20.dp)) {
                Text(text = "불러오는 중...", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(text = memo.title, style = MaterialTheme.typography.headlineSmall)

                state.scheduleItem?.let { scheduleItem ->
                    ScreenshotStrip(screenshotUris = scheduleItem.screenshotUris)
                }

                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(text = "카테고리", style = MaterialTheme.typography.labelMedium)
                        Text(text = memo.category, style = MaterialTheme.typography.bodyLarge)
                        Text(text = "요약", style = MaterialTheme.typography.labelMedium)
                        Text(text = memo.summary, style = MaterialTheme.typography.bodyLarge)
                        memo.recommendedAction?.let { action ->
                            Text(text = "추천 액션", style = MaterialTheme.typography.labelMedium)
                            Text(text = action, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }

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
    }
}

@Composable
private fun StudySection(
    studyItem: StudyItemEntity,
    onSelectReviewDays: (Int) -> Unit,
) {
    Card {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(text = "핵심 정리", style = MaterialTheme.typography.titleMedium)
            studyItem.keyPoints.forEachIndexed { index, point ->
                Text(text = "${index + 1}. $point", style = MaterialTheme.typography.bodyLarge)
            }

            Text(text = "복습 리마인드", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                reviewDayOptions.forEach { days ->
                    if (days == studyItem.selectedReviewDays) {
                        Button(onClick = { onSelectReviewDays(days) }) {
                            Text(text = "${days}일 후")
                        }
                    } else {
                        OutlinedButton(onClick = { onSelectReviewDays(days) }) {
                            Text(text = "${days}일 후")
                        }
                    }
                }
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
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "핵심 정보", style = MaterialTheme.typography.titleMedium)

                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "혜택", style = MaterialTheme.typography.labelMedium)
                        Text(text = lifeInfoItem.benefit, style = MaterialTheme.typography.bodyLarge)
                    }
                }

                Text(text = "대상", style = MaterialTheme.typography.labelMedium)
                Text(text = lifeInfoItem.target, style = MaterialTheme.typography.bodyLarge)

                Text(text = "신청", style = MaterialTheme.typography.labelMedium)
                Text(text = lifeInfoItem.applicationMethod, style = MaterialTheme.typography.bodyLarge)

                Text(text = "마감", style = MaterialTheme.typography.labelMedium)
                Text(
                    text = formatDeadline(lifeInfoItem.deadline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )

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
        }

        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                var pickerVisible by remember { mutableStateOf(lifeInfoItem.customReminderAt != null) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = "리마인드 알림", style = MaterialTheme.typography.titleMedium)
                    Row {
                        Text(text = "날짜 선택")
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
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = lifeInfoItem.customReminderAt,
                    )
                    DatePicker(state = datePickerState)

                    LaunchedEffect(datePickerState.selectedDateMillis) {
                        datePickerState.selectedDateMillis?.let(onSetCustomReminderDate)
                    }
                }
            }
        }
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
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = "감지된 일정", style = MaterialTheme.typography.titleMedium)
                Text(text = scheduleItem.eventTitle, style = MaterialTheme.typography.bodyLarge)

                scheduleItem.deadlineAt?.let { deadlineAt ->
                    Text(
                        text = "마감 ${formatScheduleDeadline(deadlineAt)}",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                scheduleItem.eventDateText?.let { eventDateText ->
                    Text(text = eventDateText, style = MaterialTheme.typography.bodyLarge)
                }

                scheduleItem.location?.let { location ->
                    Text(text = location, style = MaterialTheme.typography.bodyLarge)
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
                    Text(text = message, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                var pickerVisible by remember { mutableStateOf(scheduleItem.customReminderAt != null) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(text = "리마인드 알림", style = MaterialTheme.typography.titleMedium)
                    Row {
                        Text(text = "날짜 선택")
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
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = scheduleItem.customReminderAt,
                    )
                    DatePicker(state = datePickerState)

                    LaunchedEffect(datePickerState.selectedDateMillis) {
                        datePickerState.selectedDateMillis?.let(onSetCustomReminderDate)
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenshotStrip(screenshotUris: List<String>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(2) { index ->
            val uri = screenshotUris.getOrNull(index)
            ScreenshotPreview(
                uri = uri,
                label = "${index + 1}/2",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ScreenshotPreview(
    uri: String?,
    label: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var image by remember(uri) { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

    LaunchedEffect(uri) {
        image = uri?.let { value ->
            withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(Uri.parse(value))?.use { input ->
                        BitmapFactory.decodeStream(input)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }

    Box(
        modifier = modifier
            .aspectRatio(0.72f)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        val bitmap = image
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "원본 스크린샷 $label",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

private fun formatScheduleDeadline(epochMillis: Long): String {
    val formatter = SimpleDateFormat("yyyy년 M월 d일 HH:mm", Locale.KOREA)
    return formatter.format(Date(epochMillis))
}
