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
                text = "OCR Debug",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Screenshots 폴더의 최신 이미지를 가져와 ML Kit OCR 결과와 실행 시간을 확인합니다.",
                style = MaterialTheme.typography.bodyMedium,
            )

            StatusSection(state = state)
            AssetSampleSection(
                state = state,
                onRefreshSamples = viewModel::refreshSampleList,
                onRunSamples = viewModel::runAllSampleOcr,
                onMaskSamples = viewModel::runAllSampleMasking,
                onClearSamples = viewModel::clearSampleResults,
                onSelectResult = viewModel::selectSampleResult,
            )

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
                    Text("최신 스크린샷 가져오기")
                }
                OutlinedButton(
                    onClick = viewModel::toggleAutoDetection,
                    enabled = state.hasImagePermission && !state.isBusy,
                ) {
                    Text(if (state.isAutoDetecting) "자동 감지 중지" else "자동 감지 시작")
                }
                Button(
                    onClick = viewModel::runOcrOnSelected,
                    enabled = state.hasImagePermission && state.selectedScreenshot != null && !state.isBusy,
                ) {
                    Text("OCR 실행")
                }
                Button(
                    onClick = viewModel::runMaskOnCurrentOcr,
                    enabled = state.ocrText.isNotBlank() && !state.isBusy,
                ) {
                    Text("마스킹 실행")
                }
                Button(
                    onClick = viewModel::runAnalyzeOnCurrentMasked,
                    enabled = state.maskedText.isNotBlank() && !state.isBusy,
                ) {
                    Text("분석(LLM) 실행")
                }
                Button(
                    onClick = viewModel::loadLatestScreenshotAndRunOcr,
                    enabled = state.hasImagePermission && !state.isBusy,
                ) {
                    Text("최신 스크린샷 OCR 실행")
                }
            }

            ScreenshotSection(screenshot = state.selectedScreenshot)
            OcrResultSection(state = state)
            MaskResultSection(state = state)
            AnalysisResultSection(state = state)
        }
    }
}

@Composable
private fun AnalysisResultSection(state: DebugOcrUiState) {
    SectionTitle("LLM 분석 결과")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DebugValue(label = "실행 시간", value = state.analyzeDurationMillis?.let { "${it}ms" } ?: "-")
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        val analysis = state.analysis
        if (analysis == null) {
            Text(
                text = if (state.isAnalyzing) "분석 중..." else "분석 결과가 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            DebugValue(label = "카테고리", value = analysis.category)
            DebugValue(label = "제목", value = analysis.title)
            DebugValue(label = "추천 액션", value = analysis.recommendedAction ?: "-")
            DebugValue(label = "리마인더", value = analysis.reminderAt.formatMillis())
            SectionTitle("요약")
            Text(
                text = analysis.summary.ifBlank { "-" },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun AssetSampleSection(
    state: DebugOcrUiState,
    onRefreshSamples: () -> Unit,
    onRunSamples: () -> Unit,
    onMaskSamples: () -> Unit,
    onClearSamples: () -> Unit,
    onSelectResult: (String) -> Unit,
) {
    SectionTitle("샘플 이미지 일괄 OCR")
    Text(
        text = "app/src/main/assets/ocr_samples 에 넣은 테스트 이미지를 이름순으로 OCR합니다.",
        style = MaterialTheme.typography.bodyMedium,
    )
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Button(
            onClick = onRefreshSamples,
            enabled = !state.isBusy,
        ) {
            Text("샘플 목록 새로고침")
        }
        Button(
            onClick = onRunSamples,
            enabled = !state.isBusy,
        ) {
            Text("샘플 전체 OCR 실행")
        }
        Button(
            onClick = onMaskSamples,
            enabled = !state.isBusy && state.sampleResults.any { it.isSuccess && it.text.isNotBlank() },
        ) {
            Text("샘플 전체 마스킹 실행")
        }
        OutlinedButton(
            onClick = onClearSamples,
            enabled = !state.isBusy && state.sampleResults.isNotEmpty(),
        ) {
            Text("결과 지우기")
        }
    }

    DebugValue(label = "샘플 개수", value = state.sampleFiles.size.toString())
    SampleSummary(state = state)

    if (state.sampleResults.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.sampleResults.forEach { result ->
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onSelectResult(result.sample.assetPath) },
                ) {
                    Text(
                        text = buildString {
                            append(result.sample.displayName)
                            append(" | ")
                            if (result.isSuccess) {
                                append("${result.durationMillis}ms")
                                append(" | ${result.charCount}자")
                                if (result.hasMaskResult) {
                                    append(" | 민감 ${result.detectedSensitiveTypes.size}")
                                }
                            } else {
                                append("실패")
                            }
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        SampleResultDetail(result = state.selectedSampleResult)
    }

    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
}

@Composable
private fun SampleSummary(state: DebugOcrUiState) {
    if (state.sampleResults.isEmpty()) {
        DebugValue(label = "일괄 결과", value = "-")
        return
    }

    val successCount = state.sampleResults.count { it.isSuccess }
    val average = state.averageSampleDurationMillis?.let { "${it}ms" } ?: "-"
    DebugValue(
        label = "일괄 결과",
        value = "성공 $successCount/${state.sampleResults.size}, 평균 $average",
    )
}

@Composable
private fun SampleResultDetail(result: OcrSampleResult?) {
    SectionTitle("선택한 샘플 OCR 원문")
    if (result == null) {
        Text(
            text = "선택된 결과가 없습니다.",
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DebugValue(label = "파일", value = result.sample.displayName)
        DebugValue(label = "OCR 실행 시간", value = result.durationMillis?.let { "${it}ms" } ?: "-")
        DebugValue(label = "글자 수", value = result.charCount.toString())
        DebugValue(label = "마스킹 실행 시간", value = result.maskDurationMillis?.let { "${it}ms" } ?: "-")
        DebugValue(label = "감지 타입", value = result.detectedSensitiveTypes.joinToString().ifBlank { "-" })
        if (result.errorMessage != null) {
            Text(
                text = result.errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else if (result.text.isBlank()) {
            Text(
                text = "OCR 결과가 비어 있습니다.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Text(
                text = result.text,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            )
        }
        if (result.hasMaskResult) {
            SectionTitle("선택한 샘플 마스킹 결과")
            if (result.maskedText.isBlank()) {
                Text(
                    text = "마스킹 결과가 비어 있습니다.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Text(
                    text = result.maskedText,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                )
            }
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
            Text(
                text = "권한: ${if (state.hasImagePermission) "허용" else "필요"}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "자동 감지: ${if (state.isAutoDetecting) "실행 중" else "중지"}",
                style = MaterialTheme.typography.bodyMedium,
            )
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
private fun ScreenshotSection(screenshot: ScreenshotImage?) {
    SectionTitle("선택된 스크린샷")
    if (screenshot == null) {
        Text(
            text = "아직 선택된 스크린샷이 없습니다.",
            style = MaterialTheme.typography.bodyMedium,
        )
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DebugValue(label = "파일", value = screenshot.displayName)
        DebugValue(label = "경로", value = screenshot.relativePath)
        DebugValue(label = "URI", value = screenshot.uri.toString())
        DebugValue(label = "추가 시간", value = screenshot.dateAddedMillis.formatMillis())
        DebugValue(label = "수정 시간", value = screenshot.dateModifiedMillis.formatMillis())
        DebugValue(label = "촬영 시간", value = screenshot.dateTakenMillis.formatMillis())
    }
}

@Composable
private fun OcrResultSection(state: DebugOcrUiState) {
    SectionTitle("OCR 결과")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DebugValue(label = "실행 시간", value = state.ocrDurationMillis?.let { "${it}ms" } ?: "-")
        DebugValue(label = "글자 수", value = state.ocrCharCount.toString())
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        if (state.ocrText.isBlank()) {
            Text(
                text = "OCR 결과가 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Text(
                text = state.ocrText,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            )
        }
    }
}

@Composable
private fun MaskResultSection(state: DebugOcrUiState) {
    SectionTitle("마스킹 결과")
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        DebugValue(label = "실행 시간", value = state.maskDurationMillis?.let { "${it}ms" } ?: "-")
        DebugValue(label = "감지 타입", value = state.detectedSensitiveTypes.joinToString().ifBlank { "-" })
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        if (!state.hasSingleMaskResult) {
            Text(
                text = "마스킹 결과가 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else if (state.maskedText.isBlank()) {
            Text(
                text = "마스킹 결과가 비어 있습니다.",
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Text(
                text = state.maskedText,
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            )
        }
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
