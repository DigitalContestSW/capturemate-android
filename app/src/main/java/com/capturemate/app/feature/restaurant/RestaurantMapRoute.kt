package com.capturemate.app.feature.restaurant

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.ViewCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commit
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capturemate.app.BuildConfig
import com.capturemate.app.data.local.entity.RestaurantGroupEntity
import com.capturemate.app.data.local.entity.RestaurantMemoEntity
import com.capturemate.app.domain.repository.CaptureRepository
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.overlay.Marker

@Composable
fun RestaurantMapRoute(
    repository: CaptureRepository,
    onRestaurantClick: (String) -> Unit,
    onGroupClick: (String) -> Unit,
    viewModel: RestaurantViewModel = viewModel(factory = RestaurantViewModel.Factory(repository)),
) {
    val state by viewModel.mapState.collectAsState()
    val groupedIds = state.groupedRestaurantIds
    val standaloneRestaurants = state.restaurants.filter { it.id !in groupedIds }
    val restaurantsWithCoordinates = state.restaurants.filter {
        it.latitude != null && it.longitude != null
    }
    val restaurantsWithoutCoordinates = standaloneRestaurants.filter {
        it.latitude == null || it.longitude == null
    }

    Scaffold { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "맛집", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        text = "저장된 맛집 ${state.restaurants.size}개",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "AI 서버: ${BuildConfig.CAPTUREMATE_AI_BASE_URL}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Button(
                        onClick = viewModel::analyzeDebugRestaurantText,
                        enabled = !state.isDebugAnalyzing,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = if (state.isDebugAnalyzing) "분석 중..." else "테스트 TEXT 분석")
                    }
                    state.debugErrorMessage?.let { message ->
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            item {
                MapPreviewCard(
                    restaurants = restaurantsWithCoordinates,
                    onRestaurantClick = onRestaurantClick,
                )
            }

            if (state.visibleGroups.isNotEmpty()) {
                item {
                    Text(text = "동네 그룹", style = MaterialTheme.typography.titleMedium)
                }
                items(state.visibleGroups, key = { it.id }) { group ->
                    RestaurantGroupCard(group = group, onClick = { onGroupClick(group.id) })
                }
            }

            if (standaloneRestaurants.isNotEmpty()) {
                item {
                    Text(text = "저장된 장소", style = MaterialTheme.typography.titleMedium)
                }
                items(standaloneRestaurants, key = { it.id }) { restaurant ->
                    RestaurantListCard(
                        restaurant = restaurant,
                        onClick = { onRestaurantClick(restaurant.memoId) },
                    )
                }
            }

            if (restaurantsWithoutCoordinates.isNotEmpty()) {
                item {
                    Text(
                        text = "좌표가 없는 맛집은 지도에는 표시되지 않고 목록에만 유지됩니다.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun MapPreviewCard(
    restaurants: List<RestaurantMemoEntity>,
    onRestaurantClick: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = "지도", style = MaterialTheme.typography.titleMedium)
            if (restaurants.isEmpty()) {
                Text(text = "지도에 표시할 좌표가 있는 맛집이 없습니다.")
            } else if (BuildConfig.NAVER_MAP_NCP_KEY_ID.isBlank()) {
                Text(text = "NAVER_MAP_NCP_KEY_ID 또는 NAVER_MAP_CLIENT_ID가 설정되면 네이버맵에 저장된 맛집 핀이 표시됩니다.")
            } else {
                NaverRestaurantMapView(
                    restaurants = restaurants,
                    onRestaurantClick = onRestaurantClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp),
                )
                restaurants.forEach { restaurant ->
                    OutlinedButton(
                        onClick = { onRestaurantClick(restaurant.memoId) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = "핀: ${restaurant.name}")
                    }
                }
            }
        }
    }
}

@Composable
private fun NaverRestaurantMapView(
    restaurants: List<RestaurantMemoEntity>,
    onRestaurantClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val mapContainerId = remember { ViewCompat.generateViewId() }
    val mapFragmentTag = remember { "restaurant_naver_map_$mapContainerId" }
    val naverMapState = remember { mutableStateOf<NaverMap?>(null) }
    val markers = remember { mutableStateListOf<Marker>() }
    val places = restaurants.mapNotNull { restaurant ->
        val latitude = restaurant.latitude ?: return@mapNotNull null
        val longitude = restaurant.longitude ?: return@mapNotNull null
        RestaurantMapPlace(
            memoId = restaurant.memoId,
            name = restaurant.name,
            position = LatLng(latitude, longitude),
        )
    }

    if (activity == null) {
        Text(text = "지도를 표시하려면 FragmentActivity가 필요합니다.")
        return
    }

    DisposableEffect(Unit) {
        onDispose {
            markers.forEach { it.map = null }
            markers.clear()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { androidContext ->
            androidx.fragment.app.FragmentContainerView(androidContext).apply {
                id = mapContainerId
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
                post {
                    val fragmentManager = activity.supportFragmentManager
                    val existing = fragmentManager.findFragmentByTag(mapFragmentTag) as? MapFragment
                    val mapFragment = existing ?: MapFragment.newInstance().also { fragment ->
                        fragmentManager.commit {
                            replace(mapContainerId, fragment, mapFragmentTag)
                        }
                    }
                    mapFragment.getMapAsync { naverMap ->
                        naverMapState.value = naverMap
                        naverMap.renderRestaurantMarkers(
                            places = places,
                            markers = markers,
                            onRestaurantClick = onRestaurantClick,
                        )
                    }
                }
            }
        },
        update = {
            naverMapState.value?.renderRestaurantMarkers(
                places = places,
                markers = markers,
                onRestaurantClick = onRestaurantClick,
            )
        },
    )
}

private fun NaverMap.renderRestaurantMarkers(
    places: List<RestaurantMapPlace>,
    markers: MutableList<Marker>,
    onRestaurantClick: (String) -> Unit,
) {
    markers.forEach { it.map = null }
    markers.clear()

    val first = places.firstOrNull() ?: return
    moveCamera(CameraUpdate.scrollAndZoomTo(first.position, 15.0))

    places.forEach { place ->
        Marker().apply {
            position = place.position
            captionText = place.name
            setOnClickListener {
                onRestaurantClick(place.memoId)
                true
            }
            map = this@renderRestaurantMarkers
            markers.add(this)
        }
    }
}

private data class RestaurantMapPlace(
    val memoId: String,
    val name: String,
    val position: LatLng,
)

@Composable
private fun RestaurantGroupCard(group: RestaurantGroupEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = group.title, style = MaterialTheme.typography.titleMedium)
            Text(text = group.neighborhood, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun RestaurantListCard(
    restaurant: RestaurantMemoEntity,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = restaurant.name, style = MaterialTheme.typography.titleMedium)
                if (restaurant.needsUserReview) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            text = "확인 필요",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
            Text(text = restaurant.address ?: restaurant.roadAddress ?: "주소 정보 없음")
            restaurant.neighborhood?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
