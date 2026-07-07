package com.capturemate.app.feature.restaurant

import android.os.Build
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capturemate.app.BuildConfig
import com.capturemate.app.data.local.entity.RestaurantGroupEntity
import com.capturemate.app.data.local.entity.RestaurantMemoEntity
import com.capturemate.app.domain.repository.CaptureRepository
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelTextBuilder

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
            } else if (BuildConfig.KAKAO_NATIVE_APP_KEY.isBlank()) {
                Text(text = "KAKAO_NATIVE_APP_KEY가 설정되면 네이티브 카카오맵에 저장된 맛집 핀이 표시됩니다.")
            } else if (!isKakaoNativeMapSupported()) {
                Text(text = "현재 x86 에뮬레이터에서는 카카오 네이티브 지도 SDK가 지원되지 않습니다. arm64 실제 기기 또는 arm64 에뮬레이터에서 확인하세요.")
            } else {
                KakaoNativeMapView(
                    restaurants = restaurants,
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
private fun KakaoNativeMapView(
    restaurants: List<RestaurantMemoEntity>,
    modifier: Modifier = Modifier,
) {
    val places = restaurants.mapNotNull { restaurant ->
        val latitude = restaurant.latitude ?: return@mapNotNull null
        val longitude = restaurant.longitude ?: return@mapNotNull null
        RestaurantMapPlace(
            name = restaurant.name,
            position = LatLng.from(latitude, longitude),
        )
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            MapView(context).apply {
                start(
                    object : MapLifeCycleCallback() {
                        override fun onMapDestroy() = Unit

                        override fun onMapError(error: Exception) {
                            error.printStackTrace()
                        }
                    },
                    object : KakaoMapReadyCallback() {
                        override fun onMapReady(kakaoMap: KakaoMap) {
                            kakaoMap.moveCamera(
                                CameraUpdateFactory.newCenterPosition(places.first().position),
                            )
                            val layer = kakaoMap.labelManager?.layer
                            places.forEach { place ->
                                layer?.addLabel(
                                    LabelOptions.from(place.position)
                                        .setTexts(LabelTextBuilder().setTexts(place.name)),
                                )
                            }
                        }

                        override fun getPosition(): LatLng = places.first().position

                        override fun getZoomLevel(): Int = 15
                    },
                )
            }
        }
    )
}

private data class RestaurantMapPlace(
    val name: String,
    val position: LatLng,
)

private fun isKakaoNativeMapSupported(): Boolean =
    Build.SUPPORTED_ABIS.firstOrNull() == "arm64-v8a" ||
        Build.SUPPORTED_ABIS.firstOrNull() == "armeabi-v7a"

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
