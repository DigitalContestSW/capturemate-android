package com.capturemate.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.capturemate.app.feature.memo.MemoDetailRoute
import com.capturemate.app.feature.memo.MemoListRoute
import com.capturemate.app.ui.theme.CaptureMateTheme

class MainActivity : ComponentActivity() {

    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* 사용자가 허용하든 거부하든, 이후 알림 예약은 그대로 시도되고 발송 시점에 권한을 다시 확인함 */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        val repository = (application as CaptureMateApplication).appContainer.captureRepository

        setContent {
            CaptureMateTheme {
                var selectedMemoId by remember { mutableStateOf<String?>(null) }
                val memoId = selectedMemoId

                if (memoId == null) {
                    MemoListRoute(
                        repository = repository,
                        onMemoClick = { selectedMemoId = it },
                    )
                } else {
                    BackHandler { selectedMemoId = null }
                    MemoDetailRoute(
                        memoId = memoId,
                        repository = repository,
                    )
                }
            }
        }
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
}
