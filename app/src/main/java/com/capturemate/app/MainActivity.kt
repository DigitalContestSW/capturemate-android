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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capturemate.app.feature.home.HomeRoute
import com.capturemate.app.feature.home.HomeViewModel
import com.capturemate.app.feature.home.HomeViewModelFactory
import com.capturemate.app.feature.home.LoginScreen
import com.capturemate.app.feature.home.OnboardingRoute
import com.capturemate.app.feature.home.SettingsRoute
import com.capturemate.app.feature.memo.MemoDetailRoute
import com.capturemate.app.feature.memo.MemoListRoute
import com.capturemate.app.feature.memo.RemindersRoute
import com.capturemate.app.feature.restaurant.RestaurantDetailRoute
import com.capturemate.app.feature.restaurant.RestaurantGroupDetailRoute
import com.capturemate.app.feature.restaurant.RestaurantMapRoute
import com.capturemate.app.ui.theme.CaptureBorder
import com.capturemate.app.ui.theme.CaptureInk
import com.capturemate.app.ui.theme.CaptureMateTheme
import com.capturemate.app.ui.theme.CaptureMutedForeground
import com.capturemate.app.ui.theme.CaptureSurface

private enum class Tab { Home, MemoList, Settings }

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
                var showRestaurantMap by remember { mutableStateOf(false) }
                var showReminders by remember { mutableStateOf(false) }

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

                    !homeUiState.onboardingCompleted -> {
                        OnboardingRoute(onDone = { homeViewModel.completeOnboarding() })
                    }

                    memoId != null -> {
                        BackHandler { selectedMemoId = null }
                        MemoDetailRoute(
                            memoId = memoId,
                            repository = repository,
                            onBack = { selectedMemoId = null },
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

                    showRestaurantMap -> {
                        BackHandler { showRestaurantMap = false }
                        RestaurantMapRoute(
                            repository = repository,
                            onRestaurantClick = { selectedRestaurantMemoId = it },
                            onGroupClick = { selectedRestaurantGroupId = it },
                        )
                    }

                    showReminders -> {
                        BackHandler { showReminders = false }
                        RemindersRoute(
                            repository = repository,
                            onBack = { showReminders = false },
                            onMemoClick = {
                                showReminders = false
                                selectedMemoId = it
                            },
                        )
                    }

                    else -> {
                        var selectedTab by remember { mutableStateOf(Tab.Home) }

                        Scaffold(
                            bottomBar = {
                                Column(modifier = Modifier.background(CaptureSurface)) {
                                    HorizontalDivider(color = CaptureBorder)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .navigationBarsPadding()
                                            .padding(vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                    ) {
                                        BottomNavItem(
                                            icon = Icons.Filled.Home,
                                            label = "홈",
                                            selected = selectedTab == Tab.Home,
                                            onClick = { selectedTab = Tab.Home },
                                            modifier = Modifier.weight(1f),
                                        )
                                        BottomNavItem(
                                            icon = Icons.AutoMirrored.Filled.MenuBook,
                                            label = "메모함",
                                            selected = selectedTab == Tab.MemoList,
                                            onClick = { selectedTab = Tab.MemoList },
                                            modifier = Modifier.weight(1f),
                                        )
                                        BottomNavItem(
                                            icon = Icons.Filled.Settings,
                                            label = "설정",
                                            selected = selectedTab == Tab.Settings,
                                            onClick = { selectedTab = Tab.Settings },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            },
                        ) { innerPadding ->
                            Box(modifier = Modifier.padding(innerPadding)) {
                                when (selectedTab) {
                                    Tab.Home -> HomeRoute(
                                        repository = repository,
                                        onMemoClick = { selectedMemoId = it },
                                    )
                                    Tab.MemoList -> MemoListRoute(
                                        repository = repository,
                                        onMemoClick = { selectedMemoId = it },
                                        onOpenRestaurantMap = { showRestaurantMap = true },
                                    )
                                    Tab.Settings -> SettingsRoute(
                                        session = session,
                                        onSignOut = { homeViewModel.signOut() },
                                        onReminders = { showReminders = true },
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

@Composable
private fun BottomNavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = if (selected) CaptureInk else CaptureMutedForeground
    Surface(
        onClick = onClick,
        modifier = modifier,
        color = Color.Transparent,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(imageVector = icon, contentDescription = label, modifier = Modifier.size(22.dp), tint = color)
            Text(
                text = label,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
