package com.capturemate.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
<<<<<<< Updated upstream
import com.capturemate.app.feature.home.HomeRoute
import com.capturemate.app.ui.theme.CaptureMateTheme

class MainActivity : ComponentActivity() {
=======
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.capturemate.app.feature.home.HomeRoute
import com.capturemate.app.feature.memo.MemoDetailRoute
import com.capturemate.app.feature.memo.MemoListRoute
import com.capturemate.app.feature.restaurant.RestaurantDetailRoute
import com.capturemate.app.feature.restaurant.RestaurantGroupDetailRoute
import com.capturemate.app.feature.restaurant.RestaurantMapRoute
import com.capturemate.app.ui.theme.CaptureMateTheme

private enum class Tab { Home, MemoList, Restaurant }

class MainActivity : ComponentActivity() {

    private var pendingMemoIdFromNotification by mutableStateOf<String?>(null)

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* 알림 권한 결과와 무관하게 이후 예약 시점에서 다시 확인한다. */ }

>>>>>>> Stashed changes
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CaptureMateTheme {
<<<<<<< Updated upstream
                HomeRoute()
            }
        }
    }
}
=======
                var selectedMemoId by remember { mutableStateOf(pendingMemoIdFromNotification) }
                var selectedRestaurantMemoId by remember { mutableStateOf<String?>(null) }
                var selectedRestaurantGroupId by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(pendingMemoIdFromNotification) {
                    pendingMemoIdFromNotification?.let { selectedMemoId = it }
                }

                val memoId = selectedMemoId
                val restaurantMemoId = selectedRestaurantMemoId
                val restaurantGroupId = selectedRestaurantGroupId

                when {
                    memoId != null -> {
                        BackHandler { selectedMemoId = null }
                        MemoDetailRoute(
                            memoId = memoId,
                            repository = repository,
                        )
                    }

                    restaurantMemoId != null -> {
                        BackHandler { selectedRestaurantMemoId = null }
                        RestaurantDetailRoute(
                            memoId = restaurantMemoId,
                            repository = repository,
                        )
                    }

                    restaurantGroupId != null -> {
                        BackHandler { selectedRestaurantGroupId = null }
                        RestaurantGroupDetailRoute(
                            groupId = restaurantGroupId,
                            repository = repository,
                            onRestaurantClick = { selectedRestaurantMemoId = it },
                        )
                    }

                    else -> {
                        var selectedTab by remember { mutableStateOf(Tab.Home) }

                        Scaffold(
                            bottomBar = {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .navigationBarsPadding()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                ) {
                                    TextButton(onClick = { selectedTab = Tab.Home }) {
                                        Text(text = if (selectedTab == Tab.Home) "홈" else "홈")
                                    }
                                    TextButton(onClick = { selectedTab = Tab.MemoList }) {
                                        Text(text = if (selectedTab == Tab.MemoList) "내 메모" else "메모")
                                    }
                                    TextButton(onClick = { selectedTab = Tab.Restaurant }) {
                                        Text(text = if (selectedTab == Tab.Restaurant) "맛집" else "맛집")
                                    }
                                }
                            },
                        ) { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding)) {
                                when (selectedTab) {
                                    Tab.Home -> HomeRoute(repository = repository)
                                    Tab.MemoList -> MemoListRoute(
                                        repository = repository,
                                        onMemoClick = { selectedMemoId = it },
                                    )
                                    Tab.Restaurant -> RestaurantMapRoute(
                                        repository = repository,
                                        onRestaurantClick = { selectedRestaurantMemoId = it },
                                        onGroupClick = { selectedRestaurantGroupId = it },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingMemoIdFromNotification = intent.getStringExtra(EXTRA_MEMO_ID)
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    companion object {
        const val EXTRA_MEMO_ID = "memoId"
    }
}
>>>>>>> Stashed changes
