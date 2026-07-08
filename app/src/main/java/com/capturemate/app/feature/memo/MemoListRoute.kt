package com.capturemate.app.feature.memo

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capturemate.app.data.local.entity.MemoEntity
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
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private enum class SortOption(val label: String) {
    Latest("최신순"),
    Oldest("오래된순"),
    Deadline("마감임박순"),
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
    var showSearch by remember { mutableStateOf(false) }

    val filtered = remember(state.memos, state.itemInfo, activeCategory, sort) {
        val byCategory = if (activeCategory == CATEGORY_ALL) {
            state.memos
        } else {
            state.memos.filter { it.category == activeCategory }
        }
        when (sort) {
            SortOption.Latest -> byCategory.sortedByDescending { it.createdAt }
            SortOption.Oldest -> byCategory.sortedBy { it.createdAt }
            SortOption.Deadline -> byCategory.sortedBy {
                state.itemInfo[it.id]?.deadlineAt ?: Long.MAX_VALUE
            }
        }
    }
    val weekAgoMillis = remember { System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000 }
    val weekCount = remember(state.memos) { state.memos.count { it.createdAt > weekAgoMillis } }

    Scaffold(containerColor = CaptureBackground) { innerPadding ->
        if (showSearch) {
            MemoSearchScreen(
                memos = state.memos,
                itemInfo = state.itemInfo,
                onBack = { showSearch = false },
                onMemoClick = onMemoClick,
                modifier = Modifier.padding(innerPadding),
            )
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            MemoBoxHeader(
                totalCount = state.memos.size,
                weekCount = weekCount,
                onSearchClick = { showSearch = true },
            )
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
                            MemoGridCard(
                                memo = memo,
                                info = state.itemInfo[memo.id],
                                onClick = { onMemoClick(memo.id) },
                            )
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
                            MemoListRow(
                                memo = memo,
                                info = state.itemInfo[memo.id],
                                onClick = { onMemoClick(memo.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoBoxHeader(totalCount: Int, weekCount: Int, onSearchClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CaptureSurface)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "메모함", color = CaptureInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Surface(
                onClick = onSearchClick,
                shape = RoundedCornerShape(12.dp),
                color = CaptureMuted,
            ) {
                Text(
                    text = "🔍 검색",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    color = CaptureMutedForeground,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

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
            onClick = {
                val next = when (sort) {
                    SortOption.Latest -> SortOption.Oldest
                    SortOption.Oldest -> SortOption.Deadline
                    SortOption.Deadline -> SortOption.Latest
                }
                onSortChange(next)
            },
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
private fun MemoListRow(memo: MemoEntity, info: MemoListItemInfo?, onClick: () -> Unit) {
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
            ThumbnailBox(
                category = memo.category,
                thumbnailUri = info?.thumbnailUri,
                modifier = Modifier.size(52.dp),
                shape = RoundedCornerShape(12.dp),
                glyphFontSize = 20.sp,
            )
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryPill(category = memo.category)
                    info?.deadlineAt?.let { deadlineAt ->
                        DDayBadge(deadlineAt = deadlineAt, modifier = Modifier.padding(start = 6.dp))
                    }
                }
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
                    text = formatMemoDate(memo.createdAt) + screenshotCountSuffix(info),
                    color = CaptureMutedForeground,
                    fontSize = 12.sp,
                )
            }
            Text(text = "›", color = CaptureBorder, fontSize = 22.sp)
        }
    }
}

@Composable
private fun MemoGridCard(memo: MemoEntity, info: MemoListItemInfo?, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = CaptureSurface,
        border = BorderStroke(1.dp, CaptureBorder),
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)) {
                ThumbnailBox(
                    category = memo.category,
                    thumbnailUri = info?.thumbnailUri,
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(0.dp),
                    glyphFontSize = 32.sp,
                )
                info?.deadlineAt?.let { deadlineAt ->
                    DDayBadge(
                        deadlineAt = deadlineAt,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp),
                    )
                }
                if ((info?.screenshotCount ?: 0) > 1) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp),
                        color = CaptureInk.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(6.dp),
                    ) {
                        Text(
                            text = "${info?.screenshotCount}장",
                            color = CaptureSurface,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
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
private fun ThumbnailBox(
    category: String,
    thumbnailUri: String?,
    modifier: Modifier,
    shape: RoundedCornerShape,
    glyphFontSize: TextUnit,
) {
    val bitmap = rememberLocalBitmap(thumbnailUri)
    Box(
        modifier = modifier
            .background(CaptureMuted, shape)
            .clip(shape),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(text = categoryGlyph(category), fontSize = glyphFontSize)
        }
    }
}

private fun screenshotCountSuffix(info: MemoListItemInfo?): String =
    if ((info?.screenshotCount ?: 0) > 1) " · ${info?.screenshotCount}장" else ""

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

@Composable
private fun DDayBadge(deadlineAt: Long, modifier: Modifier = Modifier) {
    val dDay = remember(deadlineAt) { calculateDDay(deadlineAt) }
    val (bg, fg) = when {
        dDay <= 3 -> CaptureDestructive to CaptureSurface
        dDay <= 7 -> CaptureDestructive.copy(alpha = 0.7f) to CaptureSurface
        else -> CaptureMuted to CaptureMutedForeground
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(999.dp),
        color = bg,
    ) {
        Text(
            text = if (dDay >= 0) "D-$dDay" else "마감",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

private fun calculateDDay(deadlineAt: Long): Long {
    val now = ZonedDateTime.now(ZoneId.systemDefault()).toLocalDate()
    val deadline = Instant.ofEpochMilli(deadlineAt).atZone(ZoneId.systemDefault()).toLocalDate()
    return ChronoUnit.DAYS.between(now, deadline)
}

private fun formatMemoDate(createdAt: Long): String {
    val formatter = DateTimeFormatter.ofPattern("M월 d일")
    return Instant.ofEpochMilli(createdAt).atZone(ZoneId.systemDefault()).format(formatter)
}

@Composable
private fun MemoSearchScreen(
    memos: List<MemoEntity>,
    itemInfo: Map<String, MemoListItemInfo>,
    onBack: () -> Unit,
    onMemoClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val results = remember(query, memos) {
        if (query.isBlank()) {
            emptyList()
        } else {
            memos.filter { memo ->
                memo.title.contains(query, ignoreCase = true) ||
                    memo.summary.contains(query, ignoreCase = true) ||
                    categoryLabel(memo.category).contains(query, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CaptureSurface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(onClick = onBack, color = CaptureSurface) {
                Text(text = "‹", color = CaptureInk, fontSize = 26.sp, modifier = Modifier.padding(8.dp))
            }
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = CaptureMuted,
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(color = CaptureInk, fontSize = 14.sp),
                    cursorBrush = SolidColor(CaptureInk),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    decorationBox = { innerTextField ->
                        if (query.isEmpty()) {
                            Text(text = "제목, 요약, 카테고리 검색", color = CaptureMutedForeground, fontSize = 14.sp)
                        }
                        innerTextField()
                    },
                )
            }
        }

        when {
            query.isBlank() -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(text = "검색어를 입력해보세요", color = CaptureMutedForeground, fontSize = 13.sp)
                }
            }

            results.isEmpty() -> {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = "\"$query\"에 대한 결과가 없어요",
                        color = CaptureInk,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().background(CaptureBackground),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(items = results, key = { it.id }) { memo ->
                        MemoListRow(
                            memo = memo,
                            info = itemInfo[memo.id],
                            onClick = { onMemoClick(memo.id) },
                        )
                    }
                }
            }
        }
    }
}
