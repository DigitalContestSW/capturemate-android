package com.capturemate.app.feature.restaurant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capturemate.app.domain.model.RestaurantMemo
import com.capturemate.app.domain.repository.CaptureRepository
import java.text.NumberFormat
import java.util.Locale

@Composable
fun RestaurantDetailRoute(
    memoId: String,
    repository: CaptureRepository,
    viewModel: RestaurantViewModel = viewModel(factory = RestaurantViewModel.Factory(repository)),
) {
    val state by viewModel.detailState.collectAsState()

    LaunchedEffect(memoId) {
        viewModel.loadDetail(memoId)
    }

    Scaffold { innerPadding ->
        when {
            state.isLoading -> {
                Text(
                    text = "불러오는 중...",
                    modifier = Modifier.padding(innerPadding).padding(20.dp),
                )
            }

            state.restaurantMemo == null -> {
                Text(
                    text = "맛집 상세 정보가 없습니다.",
                    modifier = Modifier.padding(innerPadding).padding(20.dp),
                )
            }

            else -> {
                RestaurantDetailContent(
                    restaurantMemo = state.restaurantMemo,
                    modifier = Modifier.padding(innerPadding),
                )
            }
        }
    }
}

@Composable
fun RestaurantDetailContent(
    restaurantMemo: RestaurantMemo?,
    modifier: Modifier = Modifier,
) {
    val data = restaurantMemo ?: return
    val restaurant = data.restaurant

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = restaurant.name, style = MaterialTheme.typography.headlineSmall)
                Text(text = restaurant.summary, style = MaterialTheme.typography.bodyLarge)
                if (restaurant.needsUserReview) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            text = "장소 정보 확인 필요",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = "장소 정보", style = MaterialTheme.typography.titleMedium)
                    Text(text = restaurant.address ?: restaurant.roadAddress ?: "주소 정보 없음")
                    if (restaurant.latitude != null && restaurant.longitude != null) {
                        Text(
                            text = "좌표 ${restaurant.latitude}, ${restaurant.longitude}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    val priceRange = formatPriceRange(
                        restaurant.estimatedPricePerPersonMin,
                        restaurant.estimatedPricePerPersonMax,
                    )
                    if (priceRange != null) {
                        Text(text = "1인 예상 금액 $priceRange")
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = "메뉴", style = MaterialTheme.typography.titleMedium)
                    if (data.menus.isEmpty()) {
                        Text(text = "추출된 메뉴가 없습니다.")
                    } else {
                        data.menus.forEach { menu ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(text = menu.name)
                                Text(text = menu.price?.let(::formatWon) ?: "-")
                            }
                        }
                    }
                }
            }
        }

        if (data.tags.isNotEmpty()) {
            item {
                Text(
                    text = data.tags.joinToString("  ") { "#${it.name}" },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (data.features.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(text = "특징", style = MaterialTheme.typography.titleMedium)
                        data.features.forEach { feature ->
                            Text(text = "• ${feature.text}")
                        }
                    }
                }
            }
        }

        if (data.recommendedActions.isNotEmpty()) {
            items(data.recommendedActions, key = { it.id }) { action ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(text = action.title, style = MaterialTheme.typography.titleMedium)
                        action.description?.let { Text(text = it) }
                    }
                }
            }
        }
    }
}

@Composable
fun RestaurantInlineSection(
    restaurantMemo: RestaurantMemo,
    modifier: Modifier = Modifier,
) {
    val restaurant = restaurantMemo.restaurant
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "맛집 상세", style = MaterialTheme.typography.titleMedium)
            Text(text = restaurant.name, style = MaterialTheme.typography.titleMedium)
            Text(text = restaurant.address ?: restaurant.roadAddress ?: "주소 정보 없음")
            formatPriceRange(
                restaurant.estimatedPricePerPersonMin,
                restaurant.estimatedPricePerPersonMax,
            )?.let { Text(text = "1인 예상 금액 $it") }
            if (restaurantMemo.menus.isNotEmpty()) {
                Text(text = restaurantMemo.menus.joinToString { it.name })
            }
            if (restaurant.needsUserReview) {
                Text(
                    text = "장소 정보 확인 필요",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

private fun formatPriceRange(min: Int?, max: Int?): String? =
    when {
        min != null && max != null -> "${formatWon(min)} ~ ${formatWon(max)}"
        min != null -> "${formatWon(min)} 이상"
        max != null -> "${formatWon(max)} 이하"
        else -> null
    }

private fun formatWon(value: Int): String =
    NumberFormat.getNumberInstance(Locale.KOREA).format(value) + "원"
