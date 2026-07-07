package com.capturemate.app.feature.restaurant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capturemate.app.domain.repository.CaptureRepository

@Composable
fun RestaurantGroupDetailRoute(
    groupId: String,
    repository: CaptureRepository,
    onRestaurantClick: (String) -> Unit,
    viewModel: RestaurantViewModel = viewModel(factory = RestaurantViewModel.Factory(repository)),
) {
    val state by viewModel.groupState.collectAsState()

    LaunchedEffect(groupId) {
        viewModel.loadGroup(groupId)
    }

    Scaffold { innerPadding ->
        val group = state.group
        when {
            state.isLoading -> {
                Text(
                    text = "불러오는 중...",
                    modifier = Modifier.padding(innerPadding).padding(20.dp),
                )
            }

            group == null -> {
                Text(
                    text = "그룹을 찾을 수 없습니다.",
                    modifier = Modifier.padding(innerPadding).padding(20.dp),
                )
            }

            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentPadding = PaddingValues(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(text = group.group.title, style = MaterialTheme.typography.headlineSmall)
                            Text(text = "${group.restaurants.size}곳", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    items(group.restaurants, key = { it.id }) { restaurant ->
                        RestaurantListCard(
                            restaurant = restaurant,
                            onClick = { onRestaurantClick(restaurant.memoId) },
                        )
                    }
                }
            }
        }
    }
}