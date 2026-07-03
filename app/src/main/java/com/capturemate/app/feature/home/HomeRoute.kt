package com.capturemate.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HomeRoute() {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "캡처메이트",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "스크린샷에서 텍스트를 추출하고, 개인정보를 마스킹한 뒤 다음 행동을 제안합니다.",
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
