package com.capturemate.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.capturemate.app.core.ocr.BackendOcrScheduler
import com.capturemate.app.feature.debugocr.requiredImagePermission
import com.capturemate.app.feature.home.HomeRoute
import com.capturemate.app.feature.home.HomeViewModel
import com.capturemate.app.feature.home.HomeViewModelFactory
import com.capturemate.app.feature.home.LoginScreen
import com.capturemate.app.feature.home.OnboardingRoute
import com.capturemate.app.feature.home.SettingsRoute
import com.capturemate.app.feature.memo.MemoDetailRoute
import com.capturemate.app.feature.memo.MemoListRoute
import com.capturemate.app.feature.memo.RemindersRoute
import com.capturemate.app.feature.restaurant.RestaurantMapRoute
import com.capturemate.app.ui.theme.CaptureBorder
import com.capturemate.app.ui.theme.CaptureInk
import com.capturemate.app.ui.theme.CaptureMateTheme
import com.capturemate.app.ui.theme.CaptureMutedForeground
import com.capturemate.app.ui.theme.CaptureSurface

private enum class Tab { Home, MemoList, Settings }

class MainActivity : FragmentActivity() {

    private var pendingMemoIdFromNotification by mutableStateOf<String?>(null)
    private var showBackgroundLocationPermissionDialog by mutableStateOf(false)
    private var completeOnboardingAfterPermissionFlow: (() -> Unit)? = null
    private var pendingBackgroundLocationCompletion: (() -> Unit)? = null

    private val requestGalleryPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { requestNotificationPermissionForOnboarding() }

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { requestFineLocationPermissionForOnboarding() }

    private val requestFineLocationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && needsBackgroundLocationPermission()) {
            pendingBackgroundLocationCompletion = completeOnboardingAfterPermissionFlow
            showBackgroundLocationPermissionDialog = true
        } else if (completeOnboardingAfterPermissionFlow != null) {
            finishPostOnboardingPermissionFlow()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingMemoIdFromNotification = intent.getStringExtra(EXTRA_MEMO_ID)

        val appContainer = (application as CaptureMateApplication).appContainer
        val repository = appContainer.captureRepository
        val authRepository = appContainer.authRepository

        setContent {
            CaptureMateTheme {
                val context = LocalContext.current
                val lifecycleOwner = LocalLifecycleOwner.current
                val homeViewModel: HomeViewModel = viewModel(
                    factory = HomeViewModelFactory(authRepository),
                )
                val homeUiState by homeViewModel.uiState.collectAsState()
                val session = homeUiState.session
                var selectedMemoId by remember { mutableStateOf(pendingMemoIdFromNotification) }
                var showRestaurantMap by remember { mutableStateOf(false) }
                var showReminders by remember { mutableStateOf(false) }
                var selectedTab by remember { mutableStateOf(Tab.Home) }
                var memoListActiveCategory by remember { mutableStateOf("전체") }

                LaunchedEffect(pendingMemoIdFromNotification) {
                    pendingMemoIdFromNotification?.let { selectedMemoId = it }
                }

                DisposableEffect(session, homeUiState.onboardingCompleted, lifecycleOwner) {
                    if (session == null || !homeUiState.onboardingCompleted) {
                        onDispose {}
                    } else {
                        BackendOcrScheduler.requestImmediateSync(context)
                        val observer = LifecycleEventObserver { _, event ->
                            if (event == Lifecycle.Event.ON_RESUME) {
                                BackendOcrScheduler.requestImmediateSync(context)
                            }
                        }
                        lifecycleOwner.lifecycle.addObserver(observer)
                        onDispose {
                            lifecycleOwner.lifecycle.removeObserver(observer)
                        }
                    }
                }

                val memoId = selectedMemoId
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
                        OnboardingRoute(
                            onDone = {
                                startPostOnboardingPermissionFlow {
                                    homeViewModel.completeOnboarding()
                                }
                            },
                        )
                    }

                    memoId != null -> {
                        BackHandler { selectedMemoId = null }
                        MemoDetailRoute(
                            memoId = memoId,
                            repository = repository,
                            onBack = { selectedMemoId = null },
                            onRequestFineLocationPermission = {
                                requestFineLocationPermissionIfNeeded()
                            },
                        )
                    }

                    showRestaurantMap -> {
                        BackHandler { showRestaurantMap = false }
                        RestaurantMapRoute(
                            repository = repository,
                            onRestaurantClick = { selectedMemoId = it },
                            onGroupClick = { showRestaurantMap = false },
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
                                        activeCategory = memoListActiveCategory,
                                        onActiveCategoryChange = { memoListActiveCategory = it },
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

                if (showBackgroundLocationPermissionDialog) {
                    BackgroundLocationPermissionDialog(
                        onOpenSettings = {
                            openAppSettings()
                            finishBackgroundLocationPermissionStep()
                        },
                        onDismiss = { finishBackgroundLocationPermissionStep() },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingMemoIdFromNotification = intent.getStringExtra(EXTRA_MEMO_ID)
    }

    private fun startPostOnboardingPermissionFlow(onComplete: () -> Unit) {
        completeOnboardingAfterPermissionFlow = onComplete
        requestGalleryPermissionForOnboarding()
    }

    private fun requestGalleryPermissionForOnboarding() {
        val permission = requiredImagePermission()
        val granted = ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            requestNotificationPermissionForOnboarding()
        } else {
            requestGalleryPermission.launch(permission)
        }
    }

    private fun requestNotificationPermissionForOnboarding() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            requestFineLocationPermissionForOnboarding()
            return
        }

        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        if (granted) {
            requestFineLocationPermissionForOnboarding()
        } else {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestFineLocationPermissionForOnboarding() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            requestFineLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        if (needsBackgroundLocationPermission()) {
            pendingBackgroundLocationCompletion = completeOnboardingAfterPermissionFlow
            showBackgroundLocationPermissionDialog = true
        } else {
            finishPostOnboardingPermissionFlow()
        }
    }

    private fun finishPostOnboardingPermissionFlow() {
        completeOnboardingAfterPermissionFlow?.invoke()
        completeOnboardingAfterPermissionFlow = null
        BackendOcrScheduler.requestImmediateSync(this)
    }

    private fun finishBackgroundLocationPermissionStep() {
        showBackgroundLocationPermissionDialog = false
        pendingBackgroundLocationCompletion?.invoke()
        pendingBackgroundLocationCompletion = null
        completeOnboardingAfterPermissionFlow = null
        BackendOcrScheduler.requestImmediateSync(this)
    }

    private fun requestFineLocationPermissionIfNeeded(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocationGranted) {
            requestFineLocationPermission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return false
        }

        if (needsBackgroundLocationPermission()) {
            showBackgroundLocationPermissionDialog = true
            return false
        }

        return true
    }

    private fun needsBackgroundLocationPermission(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            ) != PackageManager.PERMISSION_GRANTED
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null),
        )
        startActivity(intent)
    }

    companion object {
        const val EXTRA_MEMO_ID = "memoId"
    }
}

@Composable
private fun BackgroundLocationPermissionDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "근처 알림을 사용하려면 위치 권한이 필요해요")
        },
        text = {
            Text(
                text = "앱이 닫혀 있어도 저장한 맛집 근처에 도착하면 알려드리기 위해 위치 권한을 항상 허용으로 변경해주세요.",
            )
        },
        confirmButton = {
            TextButton(onClick = onOpenSettings) {
                Text(text = "설정으로 이동")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "나중에")
            }
        },
    )
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
