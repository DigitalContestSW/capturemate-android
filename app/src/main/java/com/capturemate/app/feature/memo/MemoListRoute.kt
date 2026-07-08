package com.capturemate.app.feature.memo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capturemate.app.data.local.entity.MemoEntity
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

private enum class SortOption(val label: String) {
    Latest("최신순"),
    Oldest("오래된순"),
}

private const val CATEGORY_ALL = "전체"
private val CATEGORY_ORDER = listOf("Schedule", "Study", "LifeInfo", "Restaurant")

@Composable
fun MemoListRoute(
    repository: CaptureRepository,
    onMemoClick: (String) -> Unit,
    onOpenRestaurantMap: () -> Unit,
    viewModel: MemoViewModel = viewModel(factory = MemoViewModel.Factory(repository)),
) {
    val state by viewModel.listState.collectAsState()
    var activeCategory by remember { mutableStateOf(CATEGORY_ALL) }
    var sort by remember { mutableStateOf(SortOption.Latest) }
    var isGrid by remember { mutableStateOf(false) }

    val filtered = remember(state.memos, activeCategory, sort) {
        val byCategory = if (activeCategory == CATEGORY_ALL) {
            state.memos
        } else {
            state.memos.filter { it.category == activeCategory }
        }
        when (sort) {
            SortOption.Latest -> byCategory.sortedByDescending { it.createdAt }
            SortOption.Oldest -> byCategory.sortedBy { it.createdAt }
        }
    }
    val weekAgoMillis = remember { System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000 }
    val weekCount = remember(state.memos) { state.memos.count { it.createdAt > weekAgoMillis } }

    Scaffold(containerColor = CaptureBackground) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            MemoBoxHeader(totalCount = state.memos.size, weekCount = weekCount)
            CategoryTabs(
                active = activeCategory,
                onSelect = { category ->
                    if (category == "Restaurant") {
                        onOpenRestaurantMap()
                    } else {
                        activeCategory = category
                    }
                },
            )
            FilterBar(
                sort = sort,
                onSortChange = { sort = it },
                count = filtered.size,
                isGrid = isGrid,
                onToggleGrid = { isGrid = it },
            )

            when {
                state.isLoading -> {
                    Text(
                        text = "불러오는 중...",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(20.dp),
                    )
                }

                filtered.isEmpty() -> {
                    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                        Text(
                            text = "아직 저장된 메모가 없습니다.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = CaptureMutedForeground,
                        )
                    }
                }

                isGrid -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(items = filtered, key = { it.id }) { memo ->
                            MemoGridCard(memo = memo, onClick = { onMemoClick(memo.id) })
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(items = filtered, key = { it.id }) { memo ->
                            MemoListRow(memo = memo, onClick = { onMemoClick(memo.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoBoxHeader(totalCount: Int, weekCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CaptureSurface)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(text = "메모함", color = CaptureInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            shape = RoundedCornerShape(16.dp),
            color = CaptureMuted,
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                StatCell(label = "저장된 메모", value = totalCount, modifier = Modifier.weight(1f))
                StatCell(label = "이번 주 추가", value = weekCount, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: Int, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = value.toString(), color = CaptureInk, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = CaptureMutedForeground, fontSize = 10.sp)
    }
}

@Composable
private fun CategoryTabs(active: String, onSelect: (String) -> Unit) {
    val categories = remember { listOf(CATEGORY_ALL) + CATEGORY_ORDER }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CaptureSurface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        categories.forEach { category ->
            val selected = category == active
            Surface(
                onClick = { onSelect(category) },
                shape = RoundedCornerShape(999.dp),
                color = if (selected) CaptureInk else CaptureMuted,
            ) {
                Text(
                    text = if (category == CATEGORY_ALL) CATEGORY_ALL else categoryLabel(category),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    maxLines = 1,
                    color = if (selected) CaptureSurface else CaptureMutedForeground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun FilterBar(
    sort: SortOption,
    onSortChange: (SortOption) -> Unit,
    count: Int,
    isGrid: Boolean,
    onToggleGrid: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CaptureSurface)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = { onSortChange(if (sort == SortOption.Latest) SortOption.Oldest else SortOption.Latest) },
            shape = RoundedCornerShape(999.dp),
            color = CaptureMuted,
        ) {
            Text(
                text = sort.label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = CaptureMutedForeground,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Text(
            text = "${count}개",
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1f),
            color = CaptureMutedForeground,
            fontSize = 12.sp,
        )

        Row(
            modifier = Modifier
                .background(CaptureMuted, RoundedCornerShape(10.dp))
                .padding(2.dp),
        ) {
            ViewToggleButton(label = "☰", selected = !isGrid, onClick = { onToggleGrid(false) })
            ViewToggleButton(label = "⊞", selected = isGrid, onClick = { onToggleGrid(true) })
        }
    }
}

@Composable
private fun ViewToggleButton(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(28.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) CaptureSurface else CaptureMuted,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = if (selected) CaptureInk else CaptureMutedForeground,
                fontSize = 13.sp,
            )
        }
    }
}

@Composable
private fun MemoListRow(memo: MemoEntity, onClick: () -> Unit) {
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
                    .size(52.dp)
                    .background(CaptureMuted, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = categoryGlyph(memo.category), fontSize = 20.sp)
            }
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
            ) {
                CategoryPill(category = memo.category)
                Text(
                    text = memo.title,
                    modifier = Modifier.padding(top = 4.dp),
                    color = CaptureInk,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatMemoDate(memo.createdAt),
                    color = CaptureMutedForeground,
                    fontSize = 12.sp,
                )
            }
            Text(text = "›", color = CaptureBorder, fontSize = 22.sp)
        }
    }
}

@Composable
private fun MemoGridCard(memo: MemoEntity, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = CaptureSurface,
        border = BorderStroke(1.dp, CaptureBorder),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .background(CaptureMuted),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = categoryGlyph(memo.category), fontSize = 32.sp)
            }
            Column(modifier = Modifier.padding(10.dp)) {
                CategoryPill(category = memo.category)
                Text(
                    text = memo.title,
                    modifier = Modifier.padding(top = 6.dp),
                    color = CaptureInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatMemoDate(memo.createdAt),
                    modifier = Modifier.padding(top = 4.dp),
                    color = CaptureMutedForeground,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun CategoryPill(category: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = CaptureMuted,
    ) {
        Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)) {
            Text(text = categoryGlyph(category), fontSize = 9.sp)
            Text(
                text = categoryLabel(category),
                modifier = Modifier.padding(start = 3.dp),
                color = CaptureMutedForeground,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

private fun formatMemoDate(createdAt: Long): String {
    val formatter = DateTimeFormatter.ofPattern("M월 d일")
    return Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).format(formatter)
}
