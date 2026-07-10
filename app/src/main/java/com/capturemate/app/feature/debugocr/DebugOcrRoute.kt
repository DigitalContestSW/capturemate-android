package com.capturemate.app.feature.debugocr

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DebugOcrRoute(
    viewModel: DebugOcrViewModel = viewModel(),
) {
    val state by viewModel.uiState
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        viewModel.onPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        viewModel.refreshPermissionState()
    }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Backend OCR Debug",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "스크린샷 이미지를 백엔드로 업로드하고 서버 OCR, 마스킹, 분석 응답을 확인합니다.",
                style = MaterialTheme.typography.bodyMedium,
            )

            StatusSection(state = state)

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { permissionLauncher.launch(requiredImagePermission()) },
                    enabled = !state.isBusy,
                ) {
                    Text("권한 요청")
                }
                Button(
                    onClick = viewModel::loadLatestScreenshot,
                    enabled = state.hasImagePermission && !state.isBusy,
                ) {
                    Text("최신 목록")
                }
                Button(
                    onClick = viewModel::uploadSelectedScreenshot,
                    enabled = state.hasImagePermission && state.selectedScreenshot != null && !state.isBusy,
                ) {
                    Text("선택 업로드")
                }
                Button(
                    onClick = viewModel::uploadLatestScreenshots,
                    enabled = state.hasImagePermission && !state.isBusy,
                ) {
                    Text("목록 전체 업로드")
                }
                OutlinedButton(
                    onClick = viewModel::toggleAutoDetection,
                    enabled = state.hasImagePermission && !state.isBusy,
                ) {
                    Text(if (state.isAutoDetecting) "자동 업로드 중지" else "자동 업로드 시작")
                }
            }

            ScreenshotSection(
                screenshots = state.latestScreenshots,
                selectedScreenshot = state.selectedScreenshot,
                onSelectScreenshot = viewModel::selectScreenshot,
            )
            UploadResultSection(result = state.lastUploadResult)
        }
    }
}

@Composable
private fun StatusSection(state: DebugOcrUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = state.statusMessage,
                style = MaterialTheme.typography.titleSmall,
            )
            DebugValue(label = "권한", value = if (state.hasImagePermission) "허용" else "필요")
            DebugValue(label = "자동 업로드", value = if (state.isAutoDetecting) "실행 중" else "중지")
            DebugValue(label = "처리 완료", value = state.processedCount.toString())
            DebugValue(label = "중복 건너뜀", value = state.skippedDuplicateCount.toString())
            if (state.errorMessage != null) {
                Text(
                    text = state.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun ScreenshotSection(
    screenshots: List<ScreenshotImage>,
    selectedScreenshot: ScreenshotImage?,
    onSelectScreenshot: (String) -> Unit,
) {
    SectionTitle("최신 스크린샷")
    DebugValue(label = "조회 개수", value = screenshots.size.toString())

    if (screenshots.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            screenshots.forEach { screenshot ->
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onSelectScreenshot(screenshot.uri.toString()) },
                ) {
                    Text(
                        text = buildString {
                            if (screenshot.uri == selectedScreenshot?.uri) {
                                append("선택됨 | ")
                            }
                            append(screenshot.displayName.ifBlank { screenshot.uri.lastPathSegment.orEmpty() })
                            append(" | ")
                            append(screenshot.dateAddedMillis.formatMillis())
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }

    SectionTitle("선택된 스크린샷")
    if (selectedScreenshot == null) {
        Text(
            text = "아직 선택된 스크린샷이 없습니다.",
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DebugValue(label = "파일", value = selectedScreenshot.displayName)
        DebugValue(label = "경로", value = selectedScreenshot.relativePath)
        DebugValue(label = "URI", value = selectedScreenshot.uri.toString())
        DebugValue(label = "추가 시간", value = selectedScreenshot.dateAddedMillis.formatMillis())
        DebugValue(label = "수정 시간", value = selectedScreenshot.dateModifiedMillis.formatMillis())
        DebugValue(label = "촬영 시간", value = selectedScreenshot.dateTakenMillis.formatMillis())
    }
}

@Composable
private fun UploadResultSection(result: AnalyzeUploadResult?) {
    SectionTitle("백엔드 분석 결과")
    if (result == null) {
        Text(
            text = "아직 업로드 결과가 없습니다.",
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DebugValue(label = "클라이언트 캡처 ID", value = result.clientCaptureId)
        DebugValue(label = "서버 메모 ID", value = result.serverMemoId.orEmpty())
        DebugValue(label = "유용성", value = result.isUseful?.toString() ?: "-")
        DebugValue(label = "업로드/분석 시간", value = "${result.durationMillis}ms")
        DebugValue(label = "제목", value = result.title)
        DebugValue(label = "카테고리", value = result.category)
        DebugValue(label = "추천 액션", value = result.recommendedAction.orEmpty())
        DebugValue(label = "리마인드", value = result.reminderAt.formatMillis())
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        Text(
            text = "요약",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = result.summary.ifBlank { "-" },
            style = MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = "백엔드 원본 응답",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = result.rawBackendResponse.ifBlank { "-" },
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(modifier = Modifier.height(4.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
    )
}

@Composable
private fun DebugValue(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value.ifBlank { "-" },
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun Long?.formatMillis(): String {
    if (this == null || this <= 0L) return "-"
    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return formatter.format(Date(this))
}
