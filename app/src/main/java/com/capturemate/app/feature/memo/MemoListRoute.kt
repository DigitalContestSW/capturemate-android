package com.capturemate.app.feature.memo

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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

@Composable
fun MemoListRoute(
    repository: CaptureRepository,
    onMemoClick: (String) -> Unit,
    viewModel: MemoViewModel = viewModel(factory = MemoViewModel.Factory(repository)),
) {
    val state by viewModel.listState.collectAsState()

    Scaffold { innerPadding ->
        when {
            state.isLoading -> {
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(20.dp)) {
                    Text(text = "불러오는 중...", style = MaterialTheme.typography.bodyLarge)
                }
            }

            state.memos.isEmpty() -> {
                Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(20.dp)) {
                    Text(text = "아직 저장된 메모가 없습니다.", style = MaterialTheme.typography.bodyLarge)
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items = state.memos, key = { it.id }) { memo ->
                        MemoListItem(memo = memo, onClick = { onMemoClick(memo.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoListItem(memo: MemoEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = memo.category, style = MaterialTheme.typography.labelMedium)
            Text(text = memo.title, style = MaterialTheme.typography.titleMedium)
            Text(text = memo.summary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
