package com.capturemate.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.capturemate.app.ui.theme.CaptureMateTheme

private enum class Tab { Home, MemoList }

class MainActivity : ComponentActivity() {

    private var pendingMemoIdFromNotification by mutableStateOf<String?>(null)

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* 사용자가 허용하든 거부하든, 이후 알림 예약은 그대로 시도되고 발송 시점에 권한을 다시 확인함 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()
        pendingMemoIdFromNotification = intent.getStringExtra(EXTRA_MEMO_ID)

        val repository = (application as CaptureMateApplication).appContainer.captureRepository

        setContent {
            CaptureMateTheme {
                var selectedMemoId by remember { mutableStateOf(pendingMemoIdFromNotification) }

                LaunchedEffect(pendingMemoIdFromNotification) {
                    pendingMemoIdFromNotification?.let { selectedMemoId = it }
                }

                val memoId = selectedMemoId

                if (memoId != null) {
                    BackHandler { selectedMemoId = null }
                    MemoDetailRoute(
                        memoId = memoId,
                        repository = repository,
                    )
                } else {
                    var selectedTab by remember { mutableStateOf(Tab.Home) }

                    Scaffold(
                        bottomBar = {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                            ) {
                                TextButton(onClick = { selectedTab = Tab.Home }) {
                                    Text(text = if (selectedTab == Tab.Home) "● 홈" else "홈")
                                }
                                TextButton(onClick = { selectedTab = Tab.MemoList }) {
                                    Text(text = if (selectedTab == Tab.MemoList) "● 메모함" else "메모함")
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
