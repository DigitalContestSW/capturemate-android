package com.capturemate.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capturemate.app.data.local.entity.MemoEntity
import com.capturemate.app.domain.repository.CaptureRepository
import com.capturemate.app.feature.memo.MemoViewModel

@Composable
fun HomeRoute(
    repository: CaptureRepository,
    viewModel: MemoViewModel = viewModel(factory = MemoViewModel.Factory(repository)),
) {
    val state by viewModel.pendingListState.collectAsState()

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
        ) {
            Text(text = "확인하지 않은 메모", style = MaterialTheme.typography.headlineSmall)

            when {
                state.isLoading -> {
                    Text(
                        text = "불러오는 중...",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }

                state.memos.isEmpty() -> {
                    Text(
                        text = "확인할 메모가 없습니다.",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(items = state.memos, key = { it.id }) { memo ->
                            PendingMemoCard(
                                memo = memo,
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

@Composable
private fun PendingMemoCard(
    memo: MemoEntity,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = memo.category, style = MaterialTheme.typography.labelMedium)
            Text(text = memo.title, style = MaterialTheme.typography.titleMedium)
            Text(text = memo.summary, style = MaterialTheme.typography.bodyMedium)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                    Text(text = "메모 저장")
                }
                OutlinedButton(onClick = onDiscard) {
                    Text(text = "X")
                }
            }
        }
    }
}
