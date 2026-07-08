package com.capturemate.app.feature.memo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capturemate.app.domain.repository.CaptureRepository
import com.capturemate.app.feature.common.categoryGlyph
import com.capturemate.app.feature.common.categoryLabel
import com.capturemate.app.ui.theme.CaptureBackground
import com.capturemate.app.ui.theme.CaptureBorder
import com.capturemate.app.ui.theme.CaptureInk
import com.capturemate.app.ui.theme.CaptureMuted
import com.capturemate.app.ui.theme.CaptureMutedForeground
import com.capturemate.app.ui.theme.CaptureSurface
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun RemindersRoute(
    repository: CaptureRepository,
    onBack: () -> Unit,
    onMemoClick: (String) -> Unit,
    viewModel: MemoViewModel = viewModel(factory = MemoViewModel.Factory(repository)),
) {
    val reminders by viewModel.remindersState.collectAsState()

    Scaffold(containerColor = CaptureBackground) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
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
                    text = "리마인드 예정",
                    modifier = Modifier.padding(start = 8.dp),
                    color = CaptureInk,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (reminders.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(text = "예정된 리마인드가 없어요", color = CaptureMutedForeground, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(items = reminders, key = { it.memoId + it.source }) { entry ->
                        ReminderRow(entry = entry, onClick = { onMemoClick(entry.memoId) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderRow(entry: ReminderEntry, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = CaptureSurface,
        border = BorderStroke(1.dp, CaptureBorder),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(CaptureMuted, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = categoryGlyph(entry.category), fontSize = 16.sp)
            }
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
            ) {
                Text(
                    text = "${categoryLabel(entry.category)} · ${entry.source}",
                    color = CaptureMutedForeground,
                    fontSize = 11.sp,
                )
                Text(
                    text = entry.title,
                    modifier = Modifier.padding(top = 2.dp),
                    color = CaptureInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Text(
                text = formatReminderDate(entry.remindAt),
                color = CaptureInk,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private val reminderDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("M월 d일 HH:mm")

private fun formatReminderDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(reminderDateFormatter)
