package com.capturemate.app.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capturemate.app.domain.model.AuthSession
import com.capturemate.app.ui.theme.CaptureBackground
import com.capturemate.app.ui.theme.CaptureBorder
import com.capturemate.app.ui.theme.CaptureDestructive
import com.capturemate.app.ui.theme.CaptureInk
import com.capturemate.app.ui.theme.CaptureMuted
import com.capturemate.app.ui.theme.CaptureMutedForeground
import com.capturemate.app.ui.theme.CaptureSurface

@Composable
fun SettingsRoute(
    session: AuthSession?,
    onSignOut: () -> Unit,
    onReminders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier, containerColor = CaptureBackground) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CaptureSurface)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Text(text = "설정", color = CaptureInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "앱 동작 및 개인정보 설정",
                    modifier = Modifier.padding(top = 2.dp),
                    color = CaptureMutedForeground,
                    fontSize = 13.sp,
                )
            }

            SettingsSection(title = "알림") {
                SettingsActionRow(label = "리마인드 예정 목록", destructive = false, onClick = onReminders)
            }

            SettingsSection(title = "계정") {
                if (session == null) {
                    SettingsRow(label = "로그인 정보 없음", sub = "로그인 후 계정 정보가 표시돼요")
                } else {
                    SettingsRow(
                        glyph = (session.user.name?.firstOrNull() ?: session.user.email.first()).uppercase(),
                        label = session.user.name ?: session.user.email,
                        sub = session.user.email,
                    )
                    SettingsActionRow(label = "로그아웃", destructive = true, onClick = onSignOut)
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            color = CaptureMutedForeground,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            color = CaptureSurface,
            border = BorderStroke(1.dp, CaptureBorder),
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingsRow(label: String, sub: String? = null, glyph: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(CaptureMuted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (glyph != null) {
                Text(text = glyph, color = CaptureMutedForeground, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            } else {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = CaptureMutedForeground,
                )
            }
        }
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(text = label, color = CaptureInk, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            if (sub != null) {
                Text(
                    text = sub,
                    modifier = Modifier.padding(top = 2.dp),
                    color = CaptureMutedForeground,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun SettingsActionRow(label: String, destructive: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = if (destructive) CaptureDestructive else CaptureInk,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
