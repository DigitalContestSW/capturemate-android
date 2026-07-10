package com.capturemate.app.feature.memo

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
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
import com.capturemate.app.domain.model.RestaurantMemo
import com.capturemate.app.domain.repository.CaptureRepository
import com.capturemate.app.feature.common.categoryIcon
import com.capturemate.app.feature.common.categoryLabel
import com.capturemate.app.feature.common.rememberLocalBitmap
import com.capturemate.app.ui.theme.CaptureBackground
import com.capturemate.app.ui.theme.CaptureBorder
import com.capturemate.app.ui.theme.CaptureDestructive
import com.capturemate.app.ui.theme.CaptureInk
import com.capturemate.app.ui.theme.CaptureMuted
import com.capturemate.app.ui.theme.CaptureMutedForeground
import com.capturemate.app.ui.theme.CaptureSurface
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val reviewDayOptions = listOf(3, 7, 14, 30)
private val NaverMapButtonBackground = Color(0xFFE8F8EE)
private val NaverMapButtonContent = Color(0xFF03C75A)

@Composable
fun MemoDetailRoute(
    memoId: String,
    repository: CaptureRepository,
    onBack: () -> Unit,
    onRequestFineLocationPermission: () -> Boolean = { true },
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

                        state.restaurantMemo?.let { restaurantMemo ->
                            RestaurantSection(
                                restaurantMemo = restaurantMemo,
                                onLocationReminderChange = { restaurantMemoId, enabled, radiusMeters ->
                                    if (enabled && !onRequestFineLocationPermission()) return@RestaurantSection
                                    viewModel.setRestaurantLocationReminderEnabled(
                                        restaurantMemoId = restaurantMemoId,
                                        enabled = enabled,
                                        radiusMeters = radiusMeters,
                                    )
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
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "뒤로",
                modifier = Modifier.padding(8.dp).size(22.dp),
                tint = CaptureInk,
            )
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
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = "더보기",
                modifier = Modifier.padding(8.dp).size(20.dp),
                tint = CaptureMutedForeground,
            )
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
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = CaptureInk,
                )
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
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = categoryIcon(category),
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = CaptureMutedForeground,
                        )
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
private fun LabeledInfoRow(
    label: String,
    value: String,
    valueColor: Color = CaptureInk,
    valueFontWeight: FontWeight = FontWeight.Normal,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.width(44.dp)) {
            SectionLabel(label)
        }
        Text(
            text = value,
            modifier = Modifier.weight(1f).padding(start = 8.dp),
            color = valueColor,
            fontSize = 14.sp,
            fontWeight = valueFontWeight,
        )
    }
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

        val isConfirmed = studyItem.reminderConfirmed && selectedDays == studyItem.selectedReviewDays
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
                Icon(
                    imageVector = if (isConfirmed) Icons.Filled.Check else Icons.Filled.Notifications,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = CaptureInk,
                )
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

            LabeledInfoRow(label = "대상", value = lifeInfoItem.target)
            LabeledInfoRow(label = "신청", value = lifeInfoItem.applicationMethod)
            LabeledInfoRow(
                label = "마감",
                value = formatDeadline(lifeInfoItem.deadline),
                valueColor = CaptureDestructive,
                valueFontWeight = FontWeight.SemiBold,
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
                Icon(
                    imageVector = Icons.Filled.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = CaptureInk,
                )
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

@Composable
private fun RestaurantSection(
    restaurantMemo: RestaurantMemo,
    onLocationReminderChange: (String, Boolean, Float) -> Unit,
) {
    val restaurant = restaurantMemo.restaurant
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(text = "맛집 정보", color = CaptureInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Surface(
                    onClick = {
                        openNaverMapSearch(
                            context = context,
                            name = restaurant.name,
                            area = restaurant.neighborhood,
                        )
                    },
                    shape = RoundedCornerShape(999.dp),
                    color = NaverMapButtonBackground,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = NaverMapButtonContent,
                        )
                        Text(
                            text = "네이버맵에서 보기",
                            modifier = Modifier.padding(start = 4.dp),
                            color = NaverMapButtonContent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Text(text = restaurant.name, color = CaptureInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)

            val chips = buildList {
                restaurant.neighborhood?.let { add(it) }
                addAll(restaurantMemo.tags.map { "#${it.name}" })
            }
            if (chips.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(top = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    chips.forEach { chip ->
                        Surface(shape = RoundedCornerShape(999.dp), color = CaptureMuted) {
                            Text(
                                text = chip,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                color = CaptureMutedForeground,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            LabeledInfoRow(
                label = "주소",
                value = restaurant.address ?: restaurant.roadAddress ?: "주소 정보 없음",
            )
            formatRestaurantPriceRange(
                restaurant.estimatedPricePerPersonMin,
                restaurant.estimatedPricePerPersonMax,
            )?.let { priceRange ->
                LabeledInfoRow(label = "예산", value = priceRange)
            }
            if (restaurant.latitude == null || restaurant.longitude == null) {
                Text(text = "좌표가 있는 맛집만 위치 알림을 설정할 수 있어요.", color = CaptureMutedForeground, fontSize = 12.sp)
            }
        }

        if (restaurantMemo.menus.isNotEmpty()) {
            SectionCard {
                Text(text = "🍽 메뉴", color = CaptureInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                restaurantMemo.menus.forEach { menu ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(text = menu.name, color = CaptureInk, fontSize = 14.sp)
                        Text(text = menu.price?.let(::formatRestaurantWon) ?: "-", color = CaptureMutedForeground, fontSize = 13.sp)
                    }
                }
            }
        }

        if (restaurantMemo.features.isNotEmpty()) {
            SectionCard {
                Text(text = "특징", color = CaptureInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                restaurantMemo.features.forEach { feature ->
                    Text(text = feature.text, color = CaptureInk, fontSize = 14.sp)
                }
            }
        }

        SectionCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = CaptureInk,
                        )
                        Text(
                            text = "근처 알림",
                            modifier = Modifier.padding(start = 6.dp),
                            color = CaptureInk,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        text = "${restaurant.locationReminderRadiusMeters.toInt()}m 안에 들어오면 알려줘요",
                        modifier = Modifier.padding(top = 2.dp),
                        color = CaptureMutedForeground,
                        fontSize = 12.sp,
                    )
                }
                Switch(
                    checked = restaurant.locationReminderEnabled,
                    enabled = restaurant.latitude != null && restaurant.longitude != null,
                    onCheckedChange = { enabled ->
                        onLocationReminderChange(
                            restaurant.id,
                            enabled,
                            restaurant.locationReminderRadiusMeters,
                        )
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderPickerCard(initialDate: Long?, onSetCustomReminderDate: (Long?) -> Unit) {
    SectionCard {
        var selectedDate by remember(initialDate) { mutableStateOf(initialDate) }
        var showDialog by remember { mutableStateOf(false) }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(text = "리마인드 알림", color = CaptureInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                selectedDate?.let {
                    Text(
                        text = formatDeadline(it),
                        modifier = Modifier.padding(top = 2.dp),
                        color = CaptureMutedForeground,
                        fontSize = 12.sp,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "날짜 선택", color = CaptureMutedForeground, fontSize = 12.sp)
                Checkbox(
                    checked = selectedDate != null,
                    onCheckedChange = { checked ->
                        if (checked) {
                            showDialog = true
                        } else {
                            selectedDate = null
                            onSetCustomReminderDate(null)
                        }
                    },
                )
            }
        }

        if (showDialog) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDate)
            DatePickerDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                selectedDate = it
                                onSetCustomReminderDate(it)
                            }
                            showDialog = false
                        },
                    ) {
                        Text("확인")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("취소")
                    }
                },
            ) {
                DatePicker(state = datePickerState)
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

private fun formatRestaurantPriceRange(min: Int?, max: Int?): String? =
    when {
        min != null && max != null -> "${formatRestaurantWon(min)} ~ ${formatRestaurantWon(max)}"
        min != null -> "${formatRestaurantWon(min)} 이상"
        max != null -> "${formatRestaurantWon(max)} 이하"
        else -> null
    }

private fun formatRestaurantWon(value: Int): String =
    NumberFormat.getNumberInstance(Locale.KOREA).format(value) + "원"

private fun openNaverMapSearch(context: android.content.Context, name: String, area: String?) {
    val query = listOfNotNull(name, area?.takeIf { it.isNotBlank() })
        .joinToString(" ")
    val encodedQuery = Uri.encode(query)
    val appIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("nmap://search?query=$encodedQuery"),
    )
    val webIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://map.naver.com/p/search/$encodedQuery"),
    )
    runCatching {
        context.startActivity(appIntent)
    }.onFailure { throwable ->
        if (throwable is ActivityNotFoundException) {
            context.startActivity(webIntent)
        } else {
            throw throwable
        }
    }
}

private fun formatScheduleDeadline(epochMillis: Long): String {
    val formatter = SimpleDateFormat("yyyy년 M월 d일 HH:mm", Locale.KOREA)
    return formatter.format(Date(epochMillis))
}
