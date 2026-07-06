package com.capturemate.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.capturemate.app.domain.model.AuthSession

@Composable
fun SettingsRoute(
    session: AuthSession?,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "설정",
            style = MaterialTheme.typography.headlineSmall,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = if (session == null) "로그아웃됨" else "로그인됨",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "앱이 현재 어떤 계정으로 동작 중인지 확인하는 화면입니다.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        if (session == null) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = "세션이 없습니다.", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "로그인 후 계정 정보가 이곳에 표시됩니다.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
            InfoCard(label = "이름", value = session.user.name ?: "-")
            InfoCard(label = "이메일", value = session.user.email)
            InfoCard(label = "사용자 ID", value = session.user.id)
            InfoCard(label = "Provider", value = session.provider)
            InfoCard(
                label = "ID Token",
                value = if (session.providerIdToken.isNullOrBlank()) "없음" else "있음",
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onSignOut,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "로그아웃")
            }
        }
    }
}

@Composable
private fun InfoCard(
    label: String,
    value: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = label, style = MaterialTheme.typography.labelMedium)
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
