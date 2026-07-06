package com.capturemate.app.feature.restaurant

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
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
            } else if (BuildConfig.KAKAO_JAVASCRIPT_KEY.isBlank()) {
                Text(text = "KAKAO_JAVASCRIPT_KEY가 설정되면 카카오맵에 저장된 맛집 핀이 표시됩니다.")
            } else {
                KakaoMapWebView(
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
private fun KakaoMapWebView(
    restaurants: List<RestaurantMemoEntity>,
    modifier: Modifier = Modifier,
) {
    val html = buildKakaoMapHtml(
        javascriptKey = BuildConfig.KAKAO_JAVASCRIPT_KEY,
        restaurants = restaurants,
    )

    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                webChromeClient = WebChromeClient()
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                loadDataWithBaseURL(KAKAO_MAP_BASE_URL, html, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(KAKAO_MAP_BASE_URL, html, "text/html", "UTF-8", null)
        },
    )
}

private fun buildKakaoMapHtml(
    javascriptKey: String,
    restaurants: List<RestaurantMemoEntity>,
): String {
    val markers = restaurants.mapNotNull { restaurant ->
        val latitude = restaurant.latitude ?: return@mapNotNull null
        val longitude = restaurant.longitude ?: return@mapNotNull null
        """
            {
                name: ${restaurant.name.toJsString()},
                address: ${(restaurant.address ?: restaurant.roadAddress ?: "").toJsString()},
                lat: $latitude,
                lng: $longitude
            }
        """.trimIndent()
    }.joinToString(",")

    return """
        <!doctype html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0">
            <style>
                html, body, #map {
                    width: 100%;
                    height: 100%;
                    margin: 0;
                    padding: 0;
                    overflow: hidden;
                    font-family: sans-serif;
                    background: #f3f4f6;
                }
                .info {
                    min-width: 132px;
                    padding: 8px 10px;
                    color: #111827;
                    font-size: 12px;
                    line-height: 1.35;
                }
                .name {
                    font-weight: 700;
                    font-size: 13px;
                    margin-bottom: 3px;
                }
                .address {
                    color: #4b5563;
                }
            </style>
            <script src="https://dapi.kakao.com/v2/maps/sdk.js?appkey=$javascriptKey&autoload=false"></script>
        </head>
        <body>
            <div id="map"></div>
            <script>
                const places = [$markers];
                kakao.maps.load(function() {
                    const first = places[0];
                    const map = new kakao.maps.Map(document.getElementById('map'), {
                        center: new kakao.maps.LatLng(first.lat, first.lng),
                        level: places.length > 1 ? 5 : 3
                    });
                    const bounds = new kakao.maps.LatLngBounds();

                    places.forEach(function(place) {
                        const position = new kakao.maps.LatLng(place.lat, place.lng);
                        bounds.extend(position);

                        const marker = new kakao.maps.Marker({
                            map: map,
                            position: position,
                            title: place.name
                        });

                        const info = new kakao.maps.InfoWindow({
                            content:
                                '<div class="info">' +
                                '<div class="name">' + escapeHtml(place.name) + '</div>' +
                                '<div class="address">' + escapeHtml(place.address) + '</div>' +
                                '</div>'
                        });

                        kakao.maps.event.addListener(marker, 'click', function() {
                            info.open(map, marker);
                        });

                        if (places.length === 1) {
                            info.open(map, marker);
                        }
                    });

                    if (places.length > 1) {
                        map.setBounds(bounds);
                    }
                });

                function escapeHtml(value) {
                    return String(value || '')
                        .replace(/&/g, '&amp;')
                        .replace(/</g, '&lt;')
                        .replace(/>/g, '&gt;')
                        .replace(/"/g, '&quot;')
                        .replace(/'/g, '&#039;');
                }
            </script>
        </body>
        </html>
    """.trimIndent()
}

private fun String.toJsString(): String =
    buildString {
        append("'")
        this@toJsString.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '\'' -> append("\\'")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                else -> append(char)
            }
        }
        append("'")
    }

private const val KAKAO_MAP_BASE_URL = "http://localhost/"

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
