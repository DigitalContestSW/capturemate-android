package com.capturemate.app.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capturemate.app.data.local.entity.MemoEntity
import com.capturemate.app.domain.repository.CaptureRepository
import com.capturemate.app.feature.common.categoryIcon
import com.capturemate.app.feature.common.categoryLabel
import com.capturemate.app.feature.memo.MemoViewModel
import com.capturemate.app.feature.memo.UrgentDeadlineEntry
import com.capturemate.app.ui.theme.CaptureBackground
import com.capturemate.app.ui.theme.CaptureBorder
import com.capturemate.app.ui.theme.CaptureDestructive
import com.capturemate.app.ui.theme.CaptureInk
import com.capturemate.app.ui.theme.CaptureMuted
import com.capturemate.app.ui.theme.CaptureMutedForeground
import com.capturemate.app.ui.theme.CaptureSurface
import com.capturemate.app.ui.theme.CaptureUrgentOrange
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val HeaderGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF2A2A2A), Color(0xFF111111), Color(0xFF000000)),
)

@Composable
fun HomeRoute(
    repository: CaptureRepository,
    onMemoClick: (String) -> Unit,
    viewModel: MemoViewModel = viewModel(factory = MemoViewModel.Factory(repository)),
) {
    val state by viewModel.pendingListState.collectAsState()
    val urgentDeadline by viewModel.urgentDeadlineState.collectAsState()

    Scaffold(containerColor = CaptureBackground) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            val todayCount = remember(state.memos) {
                state.memos.count { formatGroupDate(it.createdAt) == "오늘" }
            }
            HomeHeader(totalCount = state.memos.size, todayCount = todayCount)

            urgentDeadline?.let { entry ->
                UrgentDeadlineCard(entry = entry, onClick = { onMemoClick(entry.memoId) })
            }

            when {
                state.isLoading -> {
                    Text(
                        text = "불러오는 중...",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(20.dp),
                    )
                }

                state.memos.isEmpty() -> {
                    EmptyHome()
                }

                else -> {
                    val grouped = remember(state.memos) {
                        state.memos.groupBy { formatGroupDate(it.createdAt) }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                    ) {
                        grouped.forEach { (date, memos) ->
                            item(key = "date-$date") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = date,
                                        color = CaptureInk,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        text = "${memos.size}개",
                                        color = CaptureMutedForeground,
                                        fontSize = 12.sp,
                                    )
                                }
                            }
                            items(items = memos, key = { it.id }) { memo ->
                                PendingMemoCard(
                                    memo = memo,
                                    onClick = { onMemoClick(memo.id) },
                                    onSave = { viewModel.confirmMemo(memo.id) },
                                    onDiscard = { viewModel.deleteMemo(memo.id) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UrgentDeadlineCard(entry: UrgentDeadlineEntry, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = CaptureDestructive,
            )
            Text(
                text = "긴급 마감",
                modifier = Modifier.padding(start = 4.dp),
                color = CaptureDestructive,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Surface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            shape = RoundedCornerShape(14.dp),
            color = CaptureSurface,
            border = BorderStroke(1.dp, CaptureDestructive.copy(alpha = 0.2f)),
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = RoundedCornerShape(999.dp), color = CaptureUrgentOrange) {
                    Text(
                        text = "D-${entry.dDay}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Text(
                        text = entry.title,
                        color = CaptureInk,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = formatUrgentDeadlineDate(entry.deadlineAt),
                        modifier = Modifier.padding(top = 2.dp),
                        color = CaptureMutedForeground,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

private val urgentDeadlineDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", Locale.KOREAN)

private fun formatUrgentDeadlineDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(urgentDeadlineDateFormatter)

@Composable
private fun HomeHeader(totalCount: Int, todayCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(HeaderGradient)
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp),
                    tint = Color.White,
                )
            }
            Text(
                text = "CAPTUREMATE",
                modifier = Modifier.padding(start = 8.dp),
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
            )
        }
        Text(
            text = "확인하지 않은 메모",
            modifier = Modifier.padding(top = 6.dp),
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = if (totalCount > 0) "오늘 ${todayCount}개 · 총 ${totalCount}개" else "모두 확인했어요",
            modifier = Modifier.padding(top = 2.dp),
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun EmptyHome() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(CaptureMuted),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                modifier = Modifier.size(26.dp),
                tint = CaptureMutedForeground,
            )
        }
        Text(
            text = "오늘은 모두 확인했어요",
            modifier = Modifier.padding(top = 14.dp),
            color = CaptureInk,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun PendingMemoCard(
    memo: MemoEntity,
    onClick: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CaptureSurface,
        border = BorderStroke(1.dp, CaptureBorder),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            CategoryBadge(category = memo.category)
            Text(
                text = memo.title,
                color = CaptureInk,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = memo.summary,
                color = CaptureMutedForeground,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    onClick = onSave,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    color = CaptureInk,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "메모 저장",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                }
                Surface(
                    onClick = onDiscard,
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = CaptureMuted,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "메모 버리기",
                            modifier = Modifier.size(18.dp),
                            tint = CaptureMutedForeground,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryBadge(category: String) {
    val icon = categoryIcon(category)
    val label = categoryLabel(category)
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = CaptureMuted,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(10.dp),
                tint = CaptureMutedForeground,
            )
            Text(
                text = label,
                modifier = Modifier.padding(start = 4.dp),
                color = CaptureMutedForeground,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun formatGroupDate(createdAt: Long): String {
    val zone = ZoneId.systemDefault()
    val date = Instant.ofEpochMilli(createdAt).atZone(zone).toLocalDate()
    val today = LocalDate.now(zone)
    return when (date) {
        today -> "오늘"
        today.minusDays(1) -> "어제"
        else -> "${date.monthValue}월 ${date.dayOfMonth}일"
    }
}
