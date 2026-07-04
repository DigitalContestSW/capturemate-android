package com.capturemate.app.feature.debugocr

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.capturemate.app.CaptureMateApplication
import com.capturemate.app.data.remote.dto.AnalyzeCaptureRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.measureTimeMillis

private const val OCR_SAMPLE_ASSET_DIR = "ocr_samples"

data class OcrSampleFile(
    val assetPath: String,
    val displayName: String,
)

data class OcrSampleResult(
    val sample: OcrSampleFile,
    val durationMillis: Long?,
    val text: String = "",
    val maskDurationMillis: Long? = null,
    val maskedText: String = "",
    val detectedSensitiveTypes: List<String> = emptyList(),
    val errorMessage: String? = null,
) {
    val isSuccess: Boolean = errorMessage == null
    val charCount: Int = text.length
    val hasMaskResult: Boolean = maskDurationMillis != null
}

data class AnalysisUi(
    val title: String,
    val summary: String,
    val category: String,
    val recommendedAction: String?,
    val reminderAt: Long?,
)

data class DebugOcrUiState(
    val hasImagePermission: Boolean = false,
    val isBusy: Boolean = false,
    val isAutoDetecting: Boolean = false,
    val selectedScreenshot: ScreenshotImage? = null,
    val ocrText: String = "",
    val ocrDurationMillis: Long? = null,
    val maskedText: String = "",
    val detectedSensitiveTypes: List<String> = emptyList(),
    val maskDurationMillis: Long? = null,
    val isAnalyzing: Boolean = false,
    val analyzeDurationMillis: Long? = null,
    val analysis: AnalysisUi? = null,
    val sampleFiles: List<OcrSampleFile> = emptyList(),
    val sampleResults: List<OcrSampleResult> = emptyList(),
    val selectedSampleAssetPath: String? = null,
    val statusMessage: String = "OCR 테스트 준비 중",
    val errorMessage: String? = null,
) {
    val ocrCharCount: Int = ocrText.length
    val hasSingleMaskResult: Boolean = maskDurationMillis != null
    val selectedSampleResult: OcrSampleResult?
        get() = sampleResults.firstOrNull { it.sample.assetPath == selectedSampleAssetPath }

    val successfulSampleResults: List<OcrSampleResult>
        get() = sampleResults.filter { it.isSuccess && it.durationMillis != null }

    val averageSampleDurationMillis: Long?
        get() {
            val durations = successfulSampleResults.mapNotNull { it.durationMillis }
            return if (durations.isEmpty()) null else durations.average().toLong()
        }
}

class DebugOcrViewModel(
    application: Application,
) : AndroidViewModel(application) {
    var uiState = androidx.compose.runtime.mutableStateOf(DebugOcrUiState())
        private set

    private val appContext = application.applicationContext
    private val appContainer = (application as CaptureMateApplication).appContainer
    private val screenshotMediaStore = ScreenshotMediaStore(appContext)
    private var observer: ContentObserver? = null
    private var lastObservedUri: String? = null

    fun refreshPermissionState() {
        val granted = hasImagePermission()
        updateState {
            it.copy(
                hasImagePermission = granted,
                statusMessage = if (granted) "이미지 권한 허용됨" else "이미지 권한 필요",
                errorMessage = null,
            )
        }
    }

    fun onPermissionResult(granted: Boolean) {
        updateState {
            it.copy(
                hasImagePermission = granted,
                statusMessage = if (granted) "이미지 권한 허용됨" else "이미지 권한이 거부됨",
                errorMessage = if (granted) null else "스크린샷 조회를 위해 이미지 읽기 권한이 필요합니다.",
            )
        }
    }

    fun refreshSampleList() {
        viewModelScope.launch {
            updateState { it.copy(isBusy = true, errorMessage = null, statusMessage = "샘플 목록 조회 중") }
            runCatching {
                withContext(Dispatchers.IO) {
                    listOcrSampleFiles()
                }
            }.onSuccess { samples ->
                updateState {
                    it.copy(
                        isBusy = false,
                        sampleFiles = samples,
                        sampleResults = emptyList(),
                        selectedSampleAssetPath = null,
                        statusMessage = "샘플 ${samples.size}개 조회됨",
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                updateState {
                    it.copy(
                        isBusy = false,
                        statusMessage = "샘플 목록 조회 실패",
                        errorMessage = throwable.message ?: throwable::class.java.simpleName,
                    )
                }
            }
        }
    }

    fun runAllSampleOcr() {
        viewModelScope.launch {
            val samples = if (uiState.value.sampleFiles.isNotEmpty()) {
                uiState.value.sampleFiles
            } else {
                withContext(Dispatchers.IO) {
                    listOcrSampleFiles()
                }
            }

            if (samples.isEmpty()) {
                updateState {
                    it.copy(
                        sampleFiles = emptyList(),
                        sampleResults = emptyList(),
                        selectedSampleAssetPath = null,
                        statusMessage = "샘플 이미지가 없습니다",
                        errorMessage = "app/src/main/assets/ocr_samples 에 png, jpg, jpeg, webp 파일을 넣으세요.",
                    )
                }
                return@launch
            }

            updateState {
                it.copy(
                    isBusy = true,
                    sampleFiles = samples,
                    sampleResults = emptyList(),
                    selectedSampleAssetPath = null,
                    statusMessage = "샘플 OCR 실행 중 0/${samples.size}",
                    errorMessage = null,
                )
            }

            val results = mutableListOf<OcrSampleResult>()
            samples.forEachIndexed { index, sample ->
                updateState {
                    it.copy(statusMessage = "샘플 OCR 실행 중 ${index + 1}/${samples.size}: ${sample.displayName}")
                }

                val result = runCatching {
                    var extractedText = ""
                    val durationMillis = measureTimeMillis {
                        val sampleUri = withContext(Dispatchers.IO) {
                            android.net.Uri.fromFile(copySampleToCache(sample))
                        }
                        extractedText = withContext(Dispatchers.IO) {
                            appContainer.ocrTextExtractor.extractText(sampleUri)
                        }
                    }
                    OcrSampleResult(
                        sample = sample,
                        durationMillis = durationMillis,
                        text = extractedText,
                    )
                }.getOrElse { throwable ->
                    OcrSampleResult(
                        sample = sample,
                        durationMillis = null,
                        errorMessage = throwable.message ?: throwable::class.java.simpleName,
                    )
                }

                results += result
                updateState {
                    it.copy(
                        sampleResults = results.toList(),
                        selectedSampleAssetPath = result.sample.assetPath,
                    )
                }
            }

            val successCount = results.count { it.isSuccess }
            updateState {
                it.copy(
                    isBusy = false,
                    sampleResults = results.toList(),
                    selectedSampleAssetPath = results.firstOrNull()?.sample?.assetPath,
                    statusMessage = "샘플 OCR 완료: 성공 $successCount/${results.size}, 평균 ${it.averageSampleDurationMillis ?: "-"}ms",
                    errorMessage = null,
                )
            }
        }
    }

    fun clearSampleResults() {
        updateState {
            it.copy(
                sampleResults = emptyList(),
                selectedSampleAssetPath = null,
                statusMessage = "샘플 결과 지움",
                errorMessage = null,
            )
        }
    }

    fun selectSampleResult(assetPath: String) {
        updateState {
            it.copy(selectedSampleAssetPath = assetPath)
        }
    }

    fun runMaskOnCurrentOcr() {
        val rawText = uiState.value.ocrText
        if (rawText.isBlank()) {
            updateState {
                it.copy(
                    statusMessage = "마스킹할 OCR 결과가 없습니다",
                    errorMessage = "먼저 OCR을 실행하세요.",
                )
            }
            return
        }

        viewModelScope.launch {
            updateState { it.copy(isBusy = true, errorMessage = null, statusMessage = "마스킹 실행 중") }
            var maskedValue = ""
            var detectedTypes = emptyList<String>()
            val durationMillis = measureTimeMillis {
                val masked = withContext(Dispatchers.Default) {
                    appContainer.sensitiveTextMasker.mask(rawText)
                }
                maskedValue = masked.value
                detectedTypes = masked.detectedTypes
            }

            updateState {
                it.copy(
                    isBusy = false,
                    maskedText = maskedValue,
                    detectedSensitiveTypes = detectedTypes,
                    maskDurationMillis = durationMillis,
                    // 새로 마스킹하면 이전 LLM 분석 결과는 무효 -> 초기화
                    analysis = null,
                    analyzeDurationMillis = null,
                    statusMessage = "마스킹 완료: ${detectedTypes.size}개 타입 감지",
                    errorMessage = null,
                )
            }
        }
    }

    /**
     * 파이프라인 마지막 단계: 마스킹된 텍스트를 백엔드 /v1/analyze 로 보내 LLM 분석을 받는다.
     * (OCR -> 마스킹 -> 여기서 서버 LLM 호출)
     */
    fun runAnalyzeOnCurrentMasked() {
        val maskedText = uiState.value.maskedText
        if (maskedText.isBlank()) {
            updateState {
                it.copy(
                    statusMessage = "분석할 마스킹 텍스트가 없습니다",
                    errorMessage = "먼저 마스킹을 실행하세요.",
                )
            }
            return
        }

        viewModelScope.launch {
            updateState {
                it.copy(
                    isBusy = true,
                    isAnalyzing = true,
                    errorMessage = null,
                    statusMessage = "LLM 분석 요청 중",
                )
            }

            var analysisUi: AnalysisUi? = null
            val result = runCatching {
                measureTimeMillis {
                    val response = withContext(Dispatchers.IO) {
                        appContainer.captureMateApi.analyzeCapture(
                            AnalyzeCaptureRequest(maskedText = maskedText),
                        )
                    }
                    analysisUi = AnalysisUi(
                        title = response.title,
                        summary = response.summary,
                        category = response.category,
                        recommendedAction = response.recommendedAction,
                        reminderAt = response.reminderAt,
                    )
                }
            }

            result.onSuccess { durationMillis ->
                updateState {
                    it.copy(
                        isBusy = false,
                        isAnalyzing = false,
                        analysis = analysisUi,
                        analyzeDurationMillis = durationMillis,
                        statusMessage = "LLM 분석 완료",
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                updateState {
                    it.copy(
                        isBusy = false,
                        isAnalyzing = false,
                        statusMessage = "LLM 분석 실패",
                        errorMessage = throwable.message ?: throwable::class.java.simpleName,
                    )
                }
            }
        }
    }

    fun runAllSampleMasking() {
        val results = uiState.value.sampleResults
        if (results.none { it.isSuccess && it.text.isNotBlank() }) {
            updateState {
                it.copy(
                    statusMessage = "마스킹할 샘플 OCR 결과가 없습니다",
                    errorMessage = "먼저 샘플 전체 OCR 실행을 완료하세요.",
                )
            }
            return
        }

        viewModelScope.launch {
            updateState { it.copy(isBusy = true, errorMessage = null, statusMessage = "샘플 마스킹 실행 중") }
            val maskedResults = results.mapIndexed { index, result ->
                if (!result.isSuccess || result.text.isBlank()) {
                    result
                } else {
                    updateState {
                        it.copy(statusMessage = "샘플 마스킹 실행 중 ${index + 1}/${results.size}: ${result.sample.displayName}")
                    }

                    var maskedValue = ""
                    var detectedTypes = emptyList<String>()
                    val durationMillis = measureTimeMillis {
                        val masked = withContext(Dispatchers.Default) {
                            appContainer.sensitiveTextMasker.mask(result.text)
                        }
                        maskedValue = masked.value
                        detectedTypes = masked.detectedTypes
                    }
                    result.copy(
                        maskDurationMillis = durationMillis,
                        maskedText = maskedValue,
                        detectedSensitiveTypes = detectedTypes,
                    )
                }
            }
            val maskedCount = maskedResults.count { it.hasMaskResult }
            updateState {
                it.copy(
                    isBusy = false,
                    sampleResults = maskedResults,
                    selectedSampleAssetPath = maskedResults.firstOrNull()?.sample?.assetPath,
                    statusMessage = "샘플 마스킹 완료: $maskedCount/${maskedResults.size}",
                    errorMessage = null,
                )
            }
        }
    }

    fun loadLatestScreenshot() {
        if (!ensurePermission()) return

        viewModelScope.launch {
            updateState { it.copy(isBusy = true, errorMessage = null, statusMessage = "최신 스크린샷 조회 중") }
            runCatching {
                withContext(Dispatchers.IO) {
                    screenshotMediaStore.findLatestScreenshot()
                }
            }.onSuccess { screenshot ->
                updateState {
                    if (screenshot == null) {
                        it.copy(
                            isBusy = false,
                            selectedScreenshot = null,
                            statusMessage = "Screenshots 폴더에서 이미지를 찾지 못함",
                            errorMessage = null,
                        )
                    } else {
                        it.copy(
                            isBusy = false,
                            selectedScreenshot = screenshot,
                            ocrText = "",
                            ocrDurationMillis = null,
                            maskedText = "",
                            detectedSensitiveTypes = emptyList(),
                            maskDurationMillis = null,
                            statusMessage = "최신 스크린샷 선택됨",
                            errorMessage = null,
                        )
                    }
                }
            }.onFailure { throwable ->
                updateState {
                    it.copy(
                        isBusy = false,
                        statusMessage = "스크린샷 조회 실패",
                        errorMessage = throwable.message ?: throwable::class.java.simpleName,
                    )
                }
            }
        }
    }

    fun runOcrOnSelected() {
        val screenshot = uiState.value.selectedScreenshot
        if (screenshot == null) {
            updateState {
                it.copy(errorMessage = "먼저 최신 스크린샷을 가져오세요.")
            }
            return
        }

        viewModelScope.launch {
            updateState { it.copy(isBusy = true, errorMessage = null, statusMessage = "OCR 실행 중") }
            var extractedText = ""
            val result = runCatching {
                measureTimeMillis {
                    extractedText = withContext(Dispatchers.IO) {
                        appContainer.ocrTextExtractor.extractText(screenshot.uri)
                    }
                }
            }

            result.onSuccess { durationMillis ->
                updateState {
                    it.copy(
                        isBusy = false,
                        ocrText = extractedText,
                        ocrDurationMillis = durationMillis,
                        maskedText = "",
                        detectedSensitiveTypes = emptyList(),
                        maskDurationMillis = null,
                        statusMessage = "OCR 완료",
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                updateState {
                    it.copy(
                        isBusy = false,
                        statusMessage = "OCR 실패",
                        errorMessage = throwable.message ?: throwable::class.java.simpleName,
                    )
                }
            }
        }
    }

    fun loadLatestScreenshotAndRunOcr() {
        if (!ensurePermission()) return

        viewModelScope.launch {
            updateState { it.copy(isBusy = true, errorMessage = null, statusMessage = "최신 스크린샷 조회 중") }
            val screenshot = runCatching {
                withContext(Dispatchers.IO) {
                    screenshotMediaStore.findLatestScreenshot()
                }
            }.getOrElse { throwable ->
                updateState {
                    it.copy(
                        isBusy = false,
                        statusMessage = "스크린샷 조회 실패",
                        errorMessage = throwable.message ?: throwable::class.java.simpleName,
                    )
                }
                return@launch
            }

            if (screenshot == null) {
                updateState {
                    it.copy(
                        isBusy = false,
                        selectedScreenshot = null,
                        statusMessage = "Screenshots 폴더에서 이미지를 찾지 못함",
                    )
                }
                return@launch
            }

            updateState {
                it.copy(
                    selectedScreenshot = screenshot,
                    ocrText = "",
                    ocrDurationMillis = null,
                    maskedText = "",
                    detectedSensitiveTypes = emptyList(),
                    maskDurationMillis = null,
                    statusMessage = "OCR 실행 중",
                )
            }

            var extractedText = ""
            runCatching {
                measureTimeMillis {
                    extractedText = withContext(Dispatchers.IO) {
                        appContainer.ocrTextExtractor.extractText(screenshot.uri)
                    }
                }
            }.onSuccess { durationMillis ->
                updateState {
                    it.copy(
                        isBusy = false,
                        ocrText = extractedText,
                        ocrDurationMillis = durationMillis,
                        maskedText = "",
                        detectedSensitiveTypes = emptyList(),
                        maskDurationMillis = null,
                        statusMessage = "최신 스크린샷 OCR 완료",
                        errorMessage = null,
                    )
                }
            }.onFailure { throwable ->
                updateState {
                    it.copy(
                        isBusy = false,
                        statusMessage = "OCR 실패",
                        errorMessage = throwable.message ?: throwable::class.java.simpleName,
                    )
                }
            }
        }
    }

    fun toggleAutoDetection() {
        if (uiState.value.isAutoDetecting) {
            stopAutoDetection()
        } else {
            startAutoDetection()
        }
    }

    private fun startAutoDetection() {
        if (!ensurePermission()) return
        if (observer != null) return

        observer = screenshotMediaStore.registerObserver {
            viewModelScope.launch {
                delay(500L)
                val screenshot = withContext(Dispatchers.IO) {
                    screenshotMediaStore.findLatestScreenshot()
                }
                val uri = screenshot?.uri?.toString()
                if (screenshot != null && uri != lastObservedUri) {
                    lastObservedUri = uri
                    updateState {
                        it.copy(
                            selectedScreenshot = screenshot,
                            ocrText = "",
                            ocrDurationMillis = null,
                            maskedText = "",
                            detectedSensitiveTypes = emptyList(),
                            maskDurationMillis = null,
                            statusMessage = "새 스크린샷 감지됨",
                            errorMessage = null,
                        )
                    }
                }
            }
        }
        updateState {
            it.copy(
                isAutoDetecting = true,
                statusMessage = "자동 감지 시작됨",
                errorMessage = null,
            )
        }
    }

    private fun stopAutoDetection() {
        observer?.let(screenshotMediaStore::unregisterObserver)
        observer = null
        updateState {
            it.copy(
                isAutoDetecting = false,
                statusMessage = "자동 감지 중지됨",
                errorMessage = null,
            )
        }
    }

    private fun ensurePermission(): Boolean {
        val granted = hasImagePermission()
        if (!granted) {
            updateState {
                it.copy(
                    hasImagePermission = false,
                    statusMessage = "이미지 권한 필요",
                    errorMessage = "권한 요청 버튼을 먼저 누르세요.",
                )
            }
        } else {
            updateState { it.copy(hasImagePermission = true) }
        }
        return granted
    }

    private fun hasImagePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            requiredImagePermission(),
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun updateState(reducer: (DebugOcrUiState) -> DebugOcrUiState) {
        uiState.value = reducer(uiState.value)
    }

    private fun listOcrSampleFiles(): List<OcrSampleFile> {
        return appContext.assets.list(OCR_SAMPLE_ASSET_DIR)
            .orEmpty()
            .filter { fileName ->
                val lowerName = fileName.lowercase()
                lowerName.endsWith(".png") ||
                    lowerName.endsWith(".jpg") ||
                    lowerName.endsWith(".jpeg") ||
                    lowerName.endsWith(".webp")
            }
            .sorted()
            .map { fileName ->
                OcrSampleFile(
                    assetPath = "$OCR_SAMPLE_ASSET_DIR/$fileName",
                    displayName = fileName,
                )
            }
    }

    private fun copySampleToCache(sample: OcrSampleFile): File {
        val cacheDirectory = File(appContext.cacheDir, OCR_SAMPLE_ASSET_DIR).apply {
            mkdirs()
        }
        val safeFileName = sample.displayName.replace(Regex("""[^A-Za-z0-9._-]"""), "_")
        val outputFile = File(cacheDirectory, safeFileName)
        appContext.assets.open(sample.assetPath).use { input ->
            outputFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return outputFile
    }

    override fun onCleared() {
        observer?.let(screenshotMediaStore::unregisterObserver)
        observer = null
        super.onCleared()
    }
}

fun requiredImagePermission(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}
