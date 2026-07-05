package com.capturemate.app.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.capturemate.app.domain.model.Memo
import com.capturemate.app.domain.model.MemoCategory
import com.capturemate.app.feature.memo.MemoPatch
import com.capturemate.app.feature.memo.MemoViewModel

private val Ink = Color(0xFF111111)
private val PageBg = Color(0xFFF7F7F7)
private val Line = Color(0xFFE5E7EB)
private val Muted = Color(0xFF6B7280)

private enum class MemoCategoryFilter(val label: String) {
    All("전체"),
    Study("학습"),
    Work("업무"),
    Schedule("일정"),
    Place("맛집"),
    Purchase("구매"),
    Development("개발"),
    Life("생활"),
}

private enum class MemoSortKey(val label: String) {
    Latest("최신순"),
    Oldest("오래된순"),
    DDay("마감임박순"),
}

private enum class MemoStatusFilter(val label: String) {
    All("전체"),
    Remind("리마인드 있음"),
    DDay("D-day 있음"),
}

private object Routes {
    const val Home = "home"
    const val MemoBox = "memobox"
    const val Search = "search"
    const val Settings = "settings"
    const val Detail = "detail"
    const val Reminders = "reminders"
}

@Composable
fun CaptureMateApp(
    onSignOut: () -> Unit,
    viewModel: MemoViewModel = viewModel(),
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    var selectedMemoId by remember { mutableStateOf<String?>(null) }
    val showBottomBar = currentRoute in listOf(Routes.Home, Routes.MemoBox, Routes.Settings)

    Scaffold(
        containerColor = PageBg,
        bottomBar = {
            if (showBottomBar) {
                BottomNavigationBar(
                    navController = navController,
                    currentDestination = backStackEntry?.destination,
                )
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Home,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.Home) {
                val memos by viewModel.unconfirmedMemos.collectAsState()
                val confirmed by viewModel.confirmedMemos.collectAsState()
                HomeScreen(
                    memos = memos,
                    confirmedCount = confirmed.size,
                    urgentMemos = confirmed.filter { (it.dDay ?: Int.MAX_VALUE) <= 7 },
                    onSave = viewModel::saveMemo,
                    onDismiss = viewModel::dismissMemo,
                    onSnooze = viewModel::snoozeMemo,
                    onGoMemoBox = { navController.navigate(Routes.MemoBox) },
                    onGroupSimilar = viewModel::groupMemos,
                    onDetail = {
                        selectedMemoId = it
                        navController.navigate(Routes.Detail)
                    },
                )
            }
            composable(Routes.MemoBox) {
                val memos by viewModel.confirmedMemos.collectAsState()
                MemoBoxScreen(
                    memos = memos,
                    onSearch = { navController.navigate(Routes.Search) },
                    onDetail = {
                        selectedMemoId = it
                        navController.navigate(Routes.Detail)
                    },
                )
            }
            composable(Routes.Search) {
                val confirmed by viewModel.confirmedMemos.collectAsState()
                val unconfirmed by viewModel.unconfirmedMemos.collectAsState()
                SearchScreen(
                    memos = confirmed + unconfirmed,
                    onBack = { navController.popBackStack() },
                    onDetail = {
                        selectedMemoId = it
                        navController.navigate(Routes.Detail)
                    },
                )
            }
            composable(Routes.Settings) {
                SettingsScreen(
                    onSignOut = onSignOut,
                    onReminders = { navController.navigate(Routes.Reminders) },
                )
            }
            composable(Routes.Detail) {
                val confirmed by viewModel.confirmedMemos.collectAsState()
                val unconfirmed by viewModel.unconfirmedMemos.collectAsState()
                val memo = (confirmed + unconfirmed).find { it.id == selectedMemoId }
                if (memo == null) {
                    EmptyHome()
                } else {
                    DetailScreen(
                        memo = memo,
                        onBack = { navController.popBackStack() },
                        onUpdate = viewModel::updateMemo,
                        onDelete = {
                            viewModel.deleteMemo(it)
                            navController.popBackStack()
                        },
                    )
                }
            }
            composable(Routes.Reminders) {
                val confirmed by viewModel.confirmedMemos.collectAsState()
                RemindersScreen(
                    memos = confirmed,
                    onBack = { navController.popBackStack() },
                    onDetail = {
                        selectedMemoId = it
                        navController.navigate(Routes.Detail)
                    },
                )
            }
        }
    }
}

@Composable
private fun BottomNavigationBar(
    navController: NavHostController,
    currentDestination: NavDestination?,
) {
    val items = listOf(
        BottomItem(Routes.Home, "홈", "⌂"),
        BottomItem(Routes.MemoBox, "메모함", "▣"),
        BottomItem(Routes.Settings, "설정", "⚙"),
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(Color.White)
            .border(1.dp, Color(0x0F000000))
            .padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val active = currentDestination?.route == item.route ||
                (currentDestination?.route == Routes.Search && item.route == Routes.MemoBox)

            Surface(
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                modifier = Modifier.weight(1f),
                color = Color.Transparent,
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = item.icon,
                        color = if (active) Ink else Color(0xFF9CA3AF),
                        fontSize = 21.sp,
                        lineHeight = 23.sp,
                    )
                    Text(
                        text = item.label,
                        color = if (active) Ink else Color(0xFF9CA3AF),
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    memos: List<Memo>,
    confirmedCount: Int = 0,
    urgentMemos: List<Memo>,
    onSave: (String, Int?) -> Unit,
    onDismiss: (String) -> Unit,
    onSnooze: (String) -> Unit,
    onGoMemoBox: () -> Unit = {},
    onGroupSimilar: (List<String>) -> Unit = {},
    onDetail: (String) -> Unit,
) {
    val groupedMemos = remember(memos) { memos.groupBy { formatDate(it.createdAt) } }
    val similarMemos = remember(memos) {
        memos
            .filter { memo -> memos.any { it.id != memo.id && it.category == memo.category } }
            .take(3)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg),
    ) {
        HomeHeader(memoCount = memos.size)

        if (urgentMemos.isNotEmpty()) {
            UrgentBanner(
                memos = urgentMemos,
                onDetail = onDetail,
            )
        }

        if (memos.isEmpty()) {
            EmptyHome(
                confirmedCount = confirmedCount,
                onGoMemoBox = onGoMemoBox,
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (similarMemos.size >= 2) {
                    item {
                        SimilarMemosSuggestion(
                            memos = similarMemos,
                            onGroup = { onGroupSimilar(similarMemos.map { it.id }) },
                            onSeparate = { similarMemos.forEach { onSave(it.id, null) } },
                        )
                    }
                }
                groupedMemos.forEach { (date, dateMemos) ->
                    item(key = "date-$date") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(date, color = Ink, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text("${dateMemos.size}개", color = Muted, fontSize = 12.sp)
                        }
                    }
                    items(dateMemos, key = { it.id }) { memo ->
                        MemoCard(
                            memo = memo,
                            onDetail = { onDetail(memo.id) },
                            onSave = { onSave(memo.id, null) },
                            onDismiss = { onDismiss(memo.id) },
                            onSnooze = { onSnooze(memo.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SimilarMemosSuggestion(
    memos: List<Memo>,
    onGroup: () -> Unit,
    onSeparate: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFEFF6FF))
            .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                memos.forEach { memo ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White)
                            .border(2.dp, Color.White, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(categoryIcon(memo.category), color = Muted, fontSize = 18.sp)
                    }
                }
            }
            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                Text(
                    text = "비슷한 스크린샷 ${memos.size}장이 있어요",
                    color = Color(0xFF1E3A8A),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "하나의 메모로 묶을까요?",
                    color = Color(0xFF2563EB),
                    fontSize = 12.sp,
                )
            }
        }
        Row(
            modifier = Modifier.padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                onClick = onGroup,
                modifier = Modifier.weight(1f).height(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF2563EB),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("묶어서 저장", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            Surface(
                onClick = onSeparate,
                modifier = Modifier.weight(1f).height(42.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("따로 저장", color = Color(0xFF2563EB), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun UrgentBanner(
    memos: List<Memo>,
    onDetail: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PageBg)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = "긴급 마감",
            color = Color(0xFFEF4444),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
        )
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            memos.forEach { memo ->
                Surface(
                    onClick = { onDetail(memo.id) },
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier.border(1.dp, Color(0xFFFEE2E2), RoundedCornerShape(16.dp)),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        DDayBadge(dDay = memo.dDay ?: 0)
                        Column(modifier = Modifier.padding(start = 10.dp)) {
                            Text(
                                text = memo.title,
                                color = Ink,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = memo.scheduleInfo?.deadline ?: memo.scheduleInfo?.date.orEmpty(),
                                color = Muted,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(memoCount: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Ink)
            .padding(horizontal = 20.dp, vertical = 20.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color.White.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "□", color = Color.White, fontSize = 12.sp)
                }
                Text(
                    text = "CaptureMate",
                    modifier = Modifier.padding(start = 8.dp),
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.sp,
                )
            }
            Text(
                text = "확인하지 않은 메모",
                modifier = Modifier.padding(top = 8.dp),
                color = Color.White,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = if (memoCount > 0) "총 ${memoCount}개" else "모두 확인했어요",
                color = Color.White.copy(alpha = 0.62f),
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
fun EmptyHome(
    confirmedCount: Int = 0,
    onGoMemoBox: () -> Unit = {},
) {
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
                .background(Color(0xFFEDEDED)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "✓", color = Muted, fontSize = 26.sp, fontWeight = FontWeight.Bold)
        }

        Text(
            text = "오늘은 모두 확인했어요",
            modifier = Modifier.padding(top = 16.dp),
            color = Ink,
            fontSize = 18.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "저장된 메모 ${confirmedCount}개",
            color = Muted,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
        )

        Surface(
            onClick = {},
            modifier = Modifier
                .padding(top = 22.dp)
                .height(46.dp),
            shape = RoundedCornerShape(14.dp),
            color = Ink,
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("지금 스크린샷 다시 분석하기", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Surface(
            onClick = onGoMemoBox,
            modifier = Modifier
                .padding(top = 10.dp)
                .height(46.dp),
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("메모함 바로가기", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MemoCard(
    memo: Memo,
    onDetail: () -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    onSnooze: () -> Unit,
) {
    var showDismiss by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0x0F000000), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Surface(onClick = onDetail, color = Color.Transparent) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CategoryBadge(category = memo.category)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        memo.dDay?.let { DDayBadge(dDay = it) }
                        val count = memo.screenshotCount ?: memo.screenshots.size
                        if (count > 1) {
                            Text(text = "${count}장", color = Muted, fontSize = 11.sp)
                        }
                    }
                }
                Text(
                    text = memo.title,
                    modifier = Modifier.padding(top = 10.dp),
                    color = Ink,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = memo.summary,
                    modifier = Modifier.padding(top = 4.dp),
                    color = Muted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        if (showDismiss) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    onClick = {
                        showDismiss = false
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF3F4F6),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("유용하지 않음", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Surface(
                    onClick = {
                        showDismiss = false
                        onSnooze()
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF9FAFB),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("나중에 다시", color = Ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("메모 저장")
                }
                Surface(
                    onClick = { showDismiss = true },
                    modifier = Modifier.size(44.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF3F4F6),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("×", color = Muted, fontSize = 20.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun MemoBoxScreen(
    memos: List<Memo>,
    onSearch: () -> Unit,
    onDetail: (String) -> Unit,
) {
    var activeCategory by remember { mutableStateOf(MemoCategoryFilter.All) }
    var sortKey by remember { mutableStateOf(MemoSortKey.Latest) }
    var statusFilter by remember { mutableStateOf(MemoStatusFilter.All) }
    val weekAgo = remember { System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L }
    val thisWeekCount = memos.count { it.createdAt > weekAgo }
    val uniqueCategoryCount = memos.map { it.category }.toSet().size

    val filtered = memos
        .filter { memo ->
            activeCategory == MemoCategoryFilter.All ||
                memo.category == activeCategory.toMemoCategory()
        }
        .filter { memo ->
            when (statusFilter) {
                MemoStatusFilter.All -> true
                MemoStatusFilter.Remind -> memo.remindAt != null
                MemoStatusFilter.DDay -> memo.dDay != null
            }
        }
        .sortedWith(
            when (sortKey) {
                MemoSortKey.Latest -> compareByDescending { it.createdAt }
                MemoSortKey.Oldest -> compareBy { it.createdAt }
                MemoSortKey.DDay -> compareBy { it.dDay ?: Int.MAX_VALUE }
            },
        )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "메모함",
                color = Ink,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )
            Surface(onClick = onSearch, shape = RoundedCornerShape(12.dp), color = Color(0xFFF3F4F6)) {
                Text(
                    text = "검색",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    color = Muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp)
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatCard(label = "저장된 메모", value = memos.size, modifier = Modifier.weight(1f))
            StatCard(label = "이번 주 추가", value = thisWeekCount, modifier = Modifier.weight(1f))
            StatCard(label = "카테고리 수", value = uniqueCategoryCount, modifier = Modifier.weight(1f))
        }

        CategoryTabs(
            activeCategory = activeCategory,
            onCategoryChange = { activeCategory = it },
        )

        MemoFilterBar(
            sortKey = sortKey,
            statusFilter = statusFilter,
            count = filtered.size,
            onSortChange = {
                sortKey = when (sortKey) {
                    MemoSortKey.Latest -> MemoSortKey.Oldest
                    MemoSortKey.Oldest -> MemoSortKey.DDay
                    MemoSortKey.DDay -> MemoSortKey.Latest
                }
            },
            onStatusChange = {
                statusFilter = when (statusFilter) {
                    MemoStatusFilter.All -> MemoStatusFilter.Remind
                    MemoStatusFilter.Remind -> MemoStatusFilter.DDay
                    MemoStatusFilter.DDay -> MemoStatusFilter.All
                }
            },
        )

        if (filtered.isEmpty()) {
            CategoryEmptyState(category = activeCategory)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(filtered, key = { it.id }) { memo ->
                    MemoListRow(
                        memo = memo,
                        onClick = { onDetail(memo.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryTabs(
    activeCategory: MemoCategoryFilter,
    onCategoryChange: (MemoCategoryFilter) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MemoCategoryFilter.entries.forEach { category ->
            Surface(
                onClick = { onCategoryChange(category) },
                shape = RoundedCornerShape(999.dp),
                color = if (activeCategory == category) Ink else Color(0xFFF3F4F6),
            ) {
                Text(
                    text = category.label,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    color = if (activeCategory == category) Color.White else Muted,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun MemoFilterBar(
    sortKey: MemoSortKey,
    statusFilter: MemoStatusFilter,
    count: Int,
    onSortChange: () -> Unit,
    onStatusChange: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(1.dp, Color(0x0A000000))
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onSortChange,
            shape = RoundedCornerShape(999.dp),
            color = Color(0xFFF9FAFB),
            modifier = Modifier.border(1.dp, Line, RoundedCornerShape(999.dp)),
        ) {
            Text(
                text = sortKey.label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                color = Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Surface(
            onClick = onStatusChange,
            shape = RoundedCornerShape(999.dp),
            color = if (statusFilter == MemoStatusFilter.All) Color(0xFFF9FAFB) else Ink,
            modifier = Modifier
                .padding(start = 8.dp)
                .border(
                    width = 1.dp,
                    color = if (statusFilter == MemoStatusFilter.All) Line else Ink,
                    shape = RoundedCornerShape(999.dp),
                ),
        ) {
            Text(
                text = statusFilter.label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                color = if (statusFilter == MemoStatusFilter.All) Muted else Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "${count}개",
            color = Muted,
            fontSize = 12.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: Int,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF3F3F3))
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = value.toString(), color = Ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = Muted, fontSize = 10.sp)
    }
}

@Composable
private fun CategoryEmptyState(category: MemoCategoryFilter) {
    val guide = when (category) {
        MemoCategoryFilter.All -> EmptyGuide(
            emoji = "📋",
            title = "아직 저장된 메모가 없어요",
            desc = "홈에서 AI가 분석한 메모를 저장하면\n여기에 모아볼 수 있어요.",
        )
        MemoCategoryFilter.Study -> EmptyGuide(
            emoji = "📚",
            title = "학습 메모가 없어요",
            desc = "강의 자료나 시험 준비 캡처를 저장하면\n복습하기 쉽게 정리돼요.",
        )
        MemoCategoryFilter.Work -> EmptyGuide(
            emoji = "💼",
            title = "업무 메모가 없어요",
            desc = "회의, 업무 공지, 액션 아이템 캡처를\n업무 메모로 모아볼 수 있어요.",
        )
        MemoCategoryFilter.Schedule -> EmptyGuide(
            emoji = "📅",
            title = "일정 메모가 없어요",
            desc = "공연, 공지, 마감일 스크린샷을 저장하면\n일정 메모로 모아볼 수 있어요.",
        )
        MemoCategoryFilter.Place -> EmptyGuide(
            emoji = "🍽️",
            title = "맛집 메모가 없어요",
            desc = "가고 싶은 식당 캡처를 저장하면\n맛집 메모로 모아볼 수 있어요.",
        )
        MemoCategoryFilter.Purchase -> EmptyGuide(
            emoji = "🛍️",
            title = "구매 메모가 없어요",
            desc = "사고 싶은 상품이나 세일 정보를 저장하면\n구매 메모로 정리돼요.",
        )
        MemoCategoryFilter.Development -> EmptyGuide(
            emoji = "⌘",
            title = "개발 메모가 없어요",
            desc = "코드, 문서, 에러 화면 캡처를 저장하면\n개발 메모로 모아볼 수 있어요.",
        )
        MemoCategoryFilter.Life -> EmptyGuide(
            emoji = "🌿",
            title = "생활 메모가 없어요",
            desc = "지원금, 혜택, 신청 안내 스크린샷을\n생활 메모로 모아볼 수 있어요.",
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = guide.emoji, fontSize = 44.sp)
        Text(
            text = guide.title,
            modifier = Modifier.padding(top = 12.dp),
            color = Ink,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = guide.desc,
            modifier = Modifier.padding(top = 8.dp),
            color = Muted,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            textAlign = TextAlign.Center,
        )
    }
}

private data class EmptyGuide(
    val emoji: String,
    val title: String,
    val desc: String,
)

@Composable
private fun MemoListRow(
    memo: Memo,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, Color(0x0F000000), RoundedCornerShape(16.dp))
            .background(Color.White),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF3F4F6)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = categoryIcon(memo.category), fontSize = 20.sp)
            }
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CategoryBadge(category = memo.category)
                    memo.dDay?.let { DDayBadge(dDay = it) }
                }
                Text(
                    text = memo.title,
                    modifier = Modifier.padding(top = 4.dp),
                    color = Ink,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${formatDate(memo.createdAt)} · ${memo.screenshotCount ?: memo.screenshots.size}장",
                    color = Muted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(text = "›", color = Color(0xFFD1D5DB), fontSize = 22.sp)
        }
    }
}

@Composable
fun SearchScreen(
    memos: List<Memo>,
    onBack: () -> Unit,
    onDetail: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var recentSearches by remember { mutableStateOf(listOf("정보처리기사", "카페", "영화제")) }
    val results = remember(query, memos) {
        if (query.isBlank()) {
            emptyList()
        } else {
            memos.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.summary.contains(query, ignoreCase = true) ||
                    it.category.label.contains(query, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(onClick = onBack, color = Color.Transparent) {
                Text(
                    text = "‹",
                    modifier = Modifier.padding(end = 12.dp),
                    color = Ink,
                    fontSize = 30.sp,
                    lineHeight = 30.sp,
                )
            }
            androidx.compose.material3.TextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                placeholder = { Text("제목, 요약, 카테고리 검색", color = Color(0xFF9CA3AF), fontSize = 14.sp) },
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF3F4F6)),
            )
        }

        if (query.isBlank()) {
            Column(
                modifier = Modifier.padding(20.dp),
            ) {
                Text("최근 검색", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                recentSearches.forEach { text ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            onClick = { query = text },
                            modifier = Modifier.weight(1f),
                            color = Color.Transparent,
                        ) {
                            Text(
                                text = text,
                                modifier = Modifier.padding(vertical = 7.dp),
                                color = Ink,
                                fontSize = 14.sp,
                            )
                        }
                        Surface(
                            onClick = { recentSearches = recentSearches.filterNot { it == text } },
                            color = Color.Transparent,
                        ) {
                            Text("×", modifier = Modifier.padding(8.dp), color = Muted, fontSize = 18.sp)
                        }
                    }
                }
            }
        } else if (results.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(text = "\"${query}\"에 대한 결과가 없어요", color = Ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "다른 키워드로 검색해보세요.",
                    modifier = Modifier.padding(top = 8.dp),
                    color = Muted,
                    fontSize = 13.sp,
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(results, key = { it.id }) { memo ->
                    MemoListRow(memo = memo, onClick = { onDetail(memo.id) })
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    onSignOut: () -> Unit,
    onReminders: () -> Unit,
) {
    var serverAi by remember { mutableStateOf(true) }
    var sensitiveDetection by remember { mutableStateOf(true) }
    var notificationEnabled by remember { mutableStateOf(true) }
    var sensitiveCategoryExcluded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Text(text = "설정", color = Ink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                text = "앱 동작 및 개인정보 설정",
                modifier = Modifier.padding(top = 2.dp),
                color = Muted,
                fontSize = 13.sp,
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsSection(title = "분석") {
                SettingsRow(label = "분석 대상 설정", sub = "스크린샷 폴더")
                SettingsRow(label = "AI 분석 방식", sub = "자동")
            }

            SettingsSection(title = "알림") {
                SettingsSwitchRow(
                    label = "스크린샷 분석 알림",
                    checked = notificationEnabled,
                    onCheckedChange = { notificationEnabled = it },
                )
                SettingsRow(label = "알림 시간", sub = "즉시")
                SettingsRow(label = "잠금화면 알림 표시 방식", sub = "모두 표시")
                SettingsSwitchRow(
                    label = "민감 카테고리 제외",
                    checked = sensitiveCategoryExcluded,
                    onCheckedChange = { sensitiveCategoryExcluded = it },
                )
                Surface(onClick = onReminders, color = Color.Transparent) {
                    SettingsRow(label = "리마인드 예정 목록", sub = "저장된 리마인드 확인")
                }
            }

            SettingsSection(title = "카테고리") {
                SettingsRow(label = "카테고리 관리", sub = "7개")
            }

            SettingsSection(title = "원본 보관 방식") {
                SettingsRow(label = "앱 안에 보관", sub = "선택됨")
            }

            SettingsSection(title = "개인정보") {
                SettingsSwitchRow(
                    label = "서버 AI 요약 사용",
                    checked = serverAi,
                    onCheckedChange = { serverAi = it },
                )
                SettingsSwitchRow(
                    label = "민감정보 보호·감지",
                    checked = sensitiveDetection,
                    onCheckedChange = { sensitiveDetection = it },
                )
                SettingsRow(label = "데이터 내보내기", sub = "전체 메모 백업")
                SettingsRow(label = "데이터 전체 삭제", destructive = true)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                color = Ink,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = "로그아웃", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun DetailScreen(
    memo: Memo,
    onBack: () -> Unit,
    onUpdate: (String, MemoPatch) -> Unit,
    onDelete: (String) -> Unit,
) {
    var editTitle by remember(memo.id) { mutableStateOf(memo.title) }
    var editSummary by remember(memo.id) { mutableStateOf(memo.summary) }
    var showEditor by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(onClick = onBack, color = Color.Transparent) {
                Text("‹", color = Ink, fontSize = 30.sp, lineHeight = 30.sp)
            }
            Text(
                text = memo.title,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                color = Ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Surface(onClick = { showEditor = !showEditor }, color = Color.Transparent) {
                Text("⋯", color = Muted, fontSize = 24.sp, lineHeight = 30.sp)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ScreenshotStrip(memo = memo)
            }
            item {
                AiSummaryCard(memo = memo)
            }
            if (memo.detectedEvent != null && memo.scheduleInfo == null) {
                memo.detectedEvent.let { item { InfoCard("감지된 일정", listOf(it.eventTitle, it.date, it.time, it.location, it.notes)) } }
            }
            if (memo.keyPoints.isNotEmpty() && memo.studyInfo == null) {
                item { InfoCard("핵심 정보", memo.keyPoints) }
            }
            memo.scheduleInfo?.let { item { InfoCard("감지된 일정", listOf(it.eventTitle, it.deadline, it.date, it.location, it.notes)) } }
            memo.studyInfo?.let { item { InfoCard("핵심 정리", it.keyPoints + listOfNotNull(it.reviewSchedule?.let { s -> "복습: $s" })) } }
            memo.lifeInfo?.let { item { InfoCard("핵심 정보", listOf(it.benefit, it.target, it.applyMethod, it.deadline)) } }
            memo.shopInfo?.let { item { InfoCard("상품 정보", listOf(it.productName, it.price, it.seller)) } }
            memo.placeInfo?.let { item { InfoCard("장소 정보", listOf(it.placeName, it.address, it.priceRange) + it.menu + it.tags.map { tag -> "#$tag" }) } }

            if (showEditor) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, Color(0x0F000000), RoundedCornerShape(16.dp))
                            .padding(16.dp),
                    ) {
                        Text("메모 관리", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        androidx.compose.material3.TextField(
                            value = editTitle,
                            onValueChange = { editTitle = it },
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            singleLine = true,
                            label = { Text("제목") },
                        )
                        androidx.compose.material3.TextField(
                            value = editSummary,
                            onValueChange = { editSummary = it },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            label = { Text("요약") },
                        )
                        Row(
                            modifier = Modifier.padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(
                                onClick = {
                                    onUpdate(
                                        memo.id,
                                        MemoPatch(
                                            title = editTitle.trim().ifBlank { memo.title },
                                            summary = editSummary.trim().ifBlank { memo.summary },
                                        ),
                                    )
                                    showEditor = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                            ) {
                                Text("저장")
                            }
                            Surface(
                                onClick = { onDelete(memo.id) },
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFEE2E2),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("삭제", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScreenshotStrip(memo: Memo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .padding(16.dp),
    ) {
        Text(
            text = "원본 스크린샷 ${memo.screenshots.size}장",
            color = Muted,
            fontSize = 12.sp,
        )
        Row(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val count = memo.screenshots.size.coerceAtLeast(1)
            repeat(count) { index ->
                Box(
                    modifier = Modifier
                        .size(92.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF3F4F6)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(categoryIcon(memo.category), color = Muted, fontSize = 24.sp)
                    Text(
                        text = "${index + 1}/${count}",
                        modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp),
                        color = Muted,
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun AiSummaryCard(memo: Memo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0x0F000000), RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Text("AI 분석 결과", color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        CategoryBadge(category = memo.category)
        Text(
            text = memo.summary,
            modifier = Modifier.padding(top = 12.dp),
            color = Ink,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = FontWeight.SemiBold,
        )
        memo.recommendedAction?.let {
            Text(
                text = "추천 액션: $it",
                modifier = Modifier.padding(top = 10.dp),
                color = Muted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    rows: List<String?>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, Color(0x0F000000), RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, color = Ink, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        rows.filterNot { it.isNullOrBlank() }.forEachIndexed { index, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${index + 1}", color = Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(row.orEmpty(), color = Color(0xFF374151), fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
    }
}

@Composable
private fun RemindersScreen(
    memos: List<Memo>,
    onBack: () -> Unit,
    onDetail: (String) -> Unit,
) {
    val withRemind = memos.filter { it.remindAt != null }.sortedBy { it.remindAt }
    val noRemind = memos.filter { it.remindAt == null && it.isConfirmed }.take(5)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBg),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(onClick = onBack, color = Color.Transparent) {
                Text("‹", color = Ink, fontSize = 30.sp, lineHeight = 30.sp)
            }
            Text(
                text = "리마인드 예정",
                modifier = Modifier.padding(start = 10.dp),
                color = Ink,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (withRemind.isEmpty() && noRemind.isEmpty()) {
                item {
                    CategoryEmptyState(category = MemoCategoryFilter.All)
                }
            }
            if (withRemind.isNotEmpty()) {
                item { Text("알림 예정", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                items(withRemind, key = { it.id }) { memo ->
                    MemoListRow(memo = memo, onClick = { onDetail(memo.id) })
                }
            }
            if (noRemind.isNotEmpty()) {
                item { Text("리마인드 미설정", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                items(noRemind, key = { it.id }) { memo ->
                    MemoListRow(memo = memo, onClick = { onDetail(memo.id) })
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            color = Muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(1.dp, Color(0xFFEFEFEF), RoundedCornerShape(16.dp)),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    sub: String? = null,
    destructive: Boolean = false,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(
            text = label,
            color = if (destructive) Color(0xFFEF4444) else Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        sub?.let {
            Text(
                text = it,
                modifier = Modifier.padding(top = 2.dp),
                color = Muted,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
        Surface(
            onClick = { onCheckedChange(!checked) },
            modifier = Modifier.size(width = 46.dp, height = 26.dp),
            shape = RoundedCornerShape(999.dp),
            color = if (checked) Color(0xFF2563EB) else Color(0xFFE5E7EB),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(3.dp),
                contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart,
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                )
            }
        }
    }
}

@Composable
private fun CategoryBadge(category: MemoCategory) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFFF3F4F6))
            .padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = categoryIcon(category), fontSize = 10.sp)
        Text(
            text = category.label,
            modifier = Modifier.padding(start = 4.dp),
            color = Muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

private data class BottomItem(
    val route: String,
    val label: String,
    val icon: String,
)

private fun categoryIcon(category: MemoCategory): String =
    when (category) {
        MemoCategory.Study -> "◇"
        MemoCategory.Work -> "▤"
        MemoCategory.Schedule -> "◷"
        MemoCategory.Place -> "⌖"
        MemoCategory.Purchase -> "△"
        MemoCategory.Development -> "⌘"
        MemoCategory.Life -> "□"
    }

@Composable
private fun DDayBadge(dDay: Int) {
    val bg = when {
        dDay <= 3 -> Color(0xFFEF4444)
        dDay <= 7 -> Color(0xFFFB923C)
        else -> Color(0xFFF3F4F6)
    }
    val fg = if (dDay <= 7) Color.White else Muted
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "D-$dDay",
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 12.sp,
        )
    }
}

private fun formatDate(timestamp: Long): String {
    val diff = ((System.currentTimeMillis() - timestamp) / (24L * 60L * 60L * 1000L)).toInt()
    if (diff <= 0) return "오늘"
    if (diff == 1) return "어제"
    val date = java.util.Date(timestamp)
    @Suppress("DEPRECATION")
    return "${date.month + 1}월 ${date.date}일"
}

private fun MemoCategoryFilter.toMemoCategory(): MemoCategory? =
    when (this) {
        MemoCategoryFilter.All -> null
        MemoCategoryFilter.Study -> MemoCategory.Study
        MemoCategoryFilter.Work -> MemoCategory.Work
        MemoCategoryFilter.Schedule -> MemoCategory.Schedule
        MemoCategoryFilter.Place -> MemoCategory.Place
        MemoCategoryFilter.Purchase -> MemoCategory.Purchase
        MemoCategoryFilter.Development -> MemoCategory.Development
        MemoCategoryFilter.Life -> MemoCategory.Life
    }
