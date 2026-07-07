package com.capturemate.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capturemate.app.feature.home.HomeRoute
import com.capturemate.app.feature.home.HomeViewModel
import com.capturemate.app.feature.home.HomeViewModelFactory
import com.capturemate.app.feature.home.LoginScreen
import com.capturemate.app.feature.home.SettingsRoute
import com.capturemate.app.feature.memo.MemoDetailRoute
import com.capturemate.app.feature.memo.MemoListRoute
import com.capturemate.app.feature.restaurant.RestaurantDetailRoute
import com.capturemate.app.feature.restaurant.RestaurantGroupDetailRoute
import com.capturemate.app.feature.restaurant.RestaurantMapRoute
import com.capturemate.app.ui.theme.CaptureMateTheme

private enum class Tab { Home, MemoList, Restaurant, Settings }

class MainActivity : FragmentActivity() {

    private var pendingMemoIdFromNotification by mutableStateOf<String?>(null)

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* Permission result is only needed before scheduling future notifications. */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        pendingMemoIdFromNotification = intent.getStringExtra(EXTRA_MEMO_ID)

        val appContainer = (application as CaptureMateApplication).appContainer
        val repository = appContainer.captureRepository
        val authRepository = appContainer.authRepository

        setContent {
            CaptureMateTheme {
                val context = LocalContext.current
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModelFactory(authRepository),
                )
                val homeUiState by homeViewModel.uiState.collectAsState()
                val session = homeUiState.session
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
                    !homeUiState.isSessionLoaded -> {
                        Box(modifier = Modifier.fillMaxWidth())
                    }

                    session == null -> {
                        LoginScreen(
                            isLoading = homeUiState.isLoading,
                            errorMessage = homeUiState.errorMessage,
                            versionName = BuildConfig.VERSION_NAME,
                            onGoogleClick = { homeViewModel.signIn(context) },
                        )
                    }

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
                                        Text(text = "홈")
                                    }
                                    TextButton(onClick = { selectedTab = Tab.MemoList }) {
                                        Text(text = "메모")
                                    }
                                    TextButton(onClick = { selectedTab = Tab.Restaurant }) {
                                        Text(text = "맛집")
                                    }
                                    TextButton(onClick = { selectedTab = Tab.Settings }) {
                                        Text(text = "설정")
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
                                    Tab.Settings -> SettingsRoute(
                                        session = session,
                                        onSignOut = { homeViewModel.signOut() },
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
