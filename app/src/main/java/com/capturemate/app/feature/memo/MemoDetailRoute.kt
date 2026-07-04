package com.capturemate.app.feature.memo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capturemate.app.data.local.entity.StudyItemEntity
import com.capturemate.app.domain.repository.CaptureRepository

private val reviewDayOptions = listOf(3, 7, 14, 30)

@Composable
fun MemoDetailRoute(
    memoId: String,
    repository: CaptureRepository,
    viewModel: MemoViewModel = viewModel(factory = MemoViewModel.Factory(repository)),
) {
    val state by viewModel.detailState.collectAsState()

    LaunchedEffect(memoId) {
        viewModel.loadMemoDetail(memoId)
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
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(text = memo.title, style = MaterialTheme.typography.headlineSmall)

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
