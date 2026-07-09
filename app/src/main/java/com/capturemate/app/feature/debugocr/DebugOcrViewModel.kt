package com.capturemate.app.feature.debugocr

import android.Manifest
import android.app.Application
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.capturemate.app.CaptureMateApplication
import com.capturemate.app.data.local.entity.CaptureEntity
import com.capturemate.app.data.local.entity.MemoEntity
import com.capturemate.app.data.remote.dto.AnalyzeBatchResponse
import com.capturemate.app.data.remote.dto.AnalyzeCaptureResponse
import com.capturemate.app.domain.model.MemoStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okio.BufferedSink
import okio.source
import java.util.UUID
import kotlin.system.measureTimeMillis

private const val LATEST_SCREENSHOT_LIMIT = 20
private const val DEFAULT_LOCALE = "ko-KR"
private val BackendResponseJson = Json { ignoreUnknownKeys = true }

data class AnalyzeUploadResult(
    val clientCaptureId: String,
    val localImageUri: String,
    val durationMillis: Long,
    val uploadedImageCount: Int,
    val groupCount: Int,
    val isUseful: Boolean?,
    val serverMemoId: String?,
    val title: String,
    val summary: String,
    val category: String,
    val recommendedAction: String?,
    val reminderAt: Long?,
    val rawBackendResponse: String,
)

data class DebugOcrUiState(
    val hasImagePermission: Boolean = false,
    val isBusy: Boolean = false,
    val isAutoDetecting: Boolean = false,
    val latestScreenshots: List<ScreenshotImage> = emptyList(),
    val selectedScreenshot: ScreenshotImage? = null,
    val lastUploadResult: AnalyzeUploadResult? = null,
    val processedCount: Int = 0,
    val skippedDuplicateCount: Int = 0,
    val statusMessage: String = "백엔드 OCR 업로드 테스트 준비 중",
    val errorMessage: String? = null,
)

private data class PendingScreenshotUpload(
    val screenshot: ScreenshotImage,
    val clientCaptureId: String,
    val capturedAt: Long,
)

class DebugOcrViewModel(
    application: Application,
) : AndroidViewModel(application) {
    var uiState = androidx.compose.runtime.mutableStateOf(DebugOcrUiState())
        private set

    private val appContext = application.applicationContext
    private val appContainer = (application as CaptureMateApplication).appContainer
    private val screenshotMediaStore = ScreenshotMediaStore(appContext)
    private val processedUris = mutableSetOf<String>()
    private val json = Json { explicitNulls = false }
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

    fun loadLatestScreenshot() {
        if (!ensurePermission()) return

        viewModelScope.launch {
            updateState { it.copy(isBusy = true, errorMessage = null, statusMessage = "최신 스크린샷 목록 조회 중") }
            runCatching {
                withContext(Dispatchers.IO) {
                    screenshotMediaStore.findLatestScreenshots(LATEST_SCREENSHOT_LIMIT)
                }
            }.onSuccess { screenshots ->
                updateState {
                    if (screenshots.isEmpty()) {
                        it.copy(
                            isBusy = false,
                            latestScreenshots = emptyList(),
                            selectedScreenshot = null,
                            statusMessage = "Screenshots 폴더에서 이미지를 찾지 못함",
                            errorMessage = null,
                        )
                    } else {
                        it.copy(
                            isBusy = false,
                            latestScreenshots = screenshots,
                            selectedScreenshot = screenshots.first(),
                            statusMessage = "최신 스크린샷 ${screenshots.size}개 조회됨",
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

    fun selectScreenshot(uri: String) {
        val screenshot = uiState.value.latestScreenshots.firstOrNull { it.uri.toString() == uri } ?: return
        updateState {
            it.copy(
                selectedScreenshot = screenshot,
                statusMessage = "스크린샷 선택됨",
                errorMessage = null,
            )
        }
    }

    fun uploadSelectedScreenshot() {
        val screenshot = uiState.value.selectedScreenshot
        if (screenshot == null) {
            updateState {
                it.copy(errorMessage = "먼저 최신 스크린샷을 가져오세요.")
            }
            return
        }

        viewModelScope.launch {
            processScreenshot(screenshot = screenshot, force = true)
        }
    }

    fun uploadLatestScreenshots() {
        if (!ensurePermission()) return

        viewModelScope.launch {
            val currentScreenshots = uiState.value.latestScreenshots
            val screenshots = if (currentScreenshots.isNotEmpty()) {
                currentScreenshots
            } else {
                updateState { it.copy(isBusy = true, errorMessage = null, statusMessage = "최신 스크린샷 조회 중") }
                runCatching {
                    withContext(Dispatchers.IO) {
                        screenshotMediaStore.findLatestScreenshots(LATEST_SCREENSHOT_LIMIT)
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
            }

            if (screenshots.isEmpty()) {
                updateState {
                    it.copy(
                        isBusy = false,
                        latestScreenshots = emptyList(),
                        selectedScreenshot = null,
                        statusMessage = "Screenshots 폴더에서 이미지를 찾지 못함",
                    )
                }
                return@launch
            }

            updateState {
                it.copy(
                    latestScreenshots = screenshots,
                    selectedScreenshot = screenshots.first(),
                )
            }
            processScreenshots(screenshots = screenshots, force = true)
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
                val screenshots = withContext(Dispatchers.IO) {
                    screenshotMediaStore.findLatestScreenshots(LATEST_SCREENSHOT_LIMIT)
                }
                val latestScreenshot = screenshots.firstOrNull()
                val uri = latestScreenshot?.uri?.toString()
                if (latestScreenshot != null && uri != lastObservedUri) {
                    lastObservedUri = uri
                    updateState {
                        it.copy(
                            latestScreenshots = screenshots,
                            selectedScreenshot = latestScreenshot,
                            statusMessage = "새 스크린샷 감지됨",
                            errorMessage = null,
                        )
                    }
                    processScreenshot(screenshot = latestScreenshot, force = false)
                }
            }
        }
        updateState {
            it.copy(
                isAutoDetecting = true,
                statusMessage = "자동 업로드 감지 시작됨",
                errorMessage = null,
            )
        }
        loadLatestScreenshot()
    }

    private fun stopAutoDetection() {
        observer?.let(screenshotMediaStore::unregisterObserver)
        observer = null
        updateState {
            it.copy(
                isAutoDetecting = false,
                statusMessage = "자동 업로드 감지 중지됨",
                errorMessage = null,
            )
        }
    }

    private suspend fun processScreenshot(
        screenshot: ScreenshotImage,
        force: Boolean,
    ) {
        processScreenshots(screenshots = listOf(screenshot), force = force)
    }

    private suspend fun processScreenshots(
        screenshots: List<ScreenshotImage>,
        force: Boolean,
    ) {
        if (!ensurePermission()) return

        val uploadScreenshots = if (force) {
            screenshots
        } else {
            screenshots.filterNot { processedUris.contains(it.uri.toString()) }
        }
        val skippedCount = screenshots.size - uploadScreenshots.size
        if (uploadScreenshots.isEmpty()) {
            updateState {
                it.copy(
                    skippedDuplicateCount = it.skippedDuplicateCount + skippedCount,
                    statusMessage = "이미 처리한 스크린샷 건너뜀",
                    errorMessage = null,
                )
            }
            return
        }

        updateState {
            it.copy(
                isBusy = true,
                selectedScreenshot = uploadScreenshots.first(),
                skippedDuplicateCount = it.skippedDuplicateCount + skippedCount,
                statusMessage = "백엔드 OCR ${uploadScreenshots.size}개 업로드 중",
                errorMessage = null,
            )
        }

        val createdAt = System.currentTimeMillis()
        val pendingUploads = uploadScreenshots.map { screenshot ->
            PendingScreenshotUpload(
                screenshot = screenshot,
                clientCaptureId = UUID.randomUUID().toString(),
                capturedAt = screenshot.bestTimestampMillis(),
            )
        }
        var response: BackendAnalyzePayload? = null
        val result = runCatching {
            val metadata = pendingUploads.map { upload ->
                AnalyzeImageMetadata(
                    clientId = upload.clientCaptureId,
                    capturedAt = upload.capturedAt,
                )
            }
            val durationMillis = measureTimeMillis {
                response = withContext(Dispatchers.IO) {
                    appContainer.captureMateApi.analyzeCapture(
                        images = pendingUploads.map { createImagePart(it.screenshot.uri) },
                        locale = DEFAULT_LOCALE.toPlainTextRequestBody(),
                        metadata = json.encodeToString(metadata).toPlainTextRequestBody(),
                    ).toBackendAnalyzePayload()
                }
            }
            response.toUploadResult(
                uploads = pendingUploads,
                durationMillis = durationMillis,
            )
        }

        result.onSuccess { uploadResult ->
            processedUris += pendingUploads.map { it.screenshot.uri.toString() }
            val payload = response ?: error("백엔드 응답이 비어 있습니다.")
            withContext(Dispatchers.IO) {
                saveBatchResult(
                    uploads = pendingUploads,
                    response = payload.parsed,
                    createdAt = createdAt,
                )
            }

            updateState {
                it.copy(
                    isBusy = false,
                    lastUploadResult = uploadResult,
                    processedCount = it.processedCount + pendingUploads.size,
                    statusMessage = "백엔드 분석 완료: ${uploadResult.title}",
                    errorMessage = null,
                )
            }
        }.onFailure { throwable ->
            updateState {
                it.copy(
                    isBusy = false,
                    statusMessage = "백엔드 분석 실패",
                    errorMessage = throwable.message ?: throwable::class.java.simpleName,
                )
            }
        }
    }

    private suspend fun saveBatchResult(
        uploads: List<PendingScreenshotUpload>,
        response: AnalyzeBatchResponse,
        createdAt: Long,
    ) {
        val uploadsByClientId = uploads.associateBy { it.clientCaptureId }
        val usefulGroups = response.groups.filter { it.analysis.isUseful }
        usefulGroups.forEach { group ->
            group.memberClientIds.forEach { clientId ->
                val upload = uploadsByClientId[clientId] ?: return@forEach
                appContainer.captureRepository.upsertCapture(
                    CaptureEntity(
                        id = upload.clientCaptureId,
                        localImageUri = upload.screenshot.uri.toString(),
                        rawTextLocalOnly = "",
                        maskedText = "",
                        category = group.analysis.category,
                        capturedAt = upload.capturedAt,
                        createdAt = createdAt,
                    ),
                )
            }
            appContainer.captureRepository.upsertMemo(
                MemoEntity(
                    id = group.analysis.serverMemoId ?: UUID.randomUUID().toString(),
                    captureId = group.memberClientIds.firstOrNull(),
                    serverMemoId = group.analysis.serverMemoId,
                    title = group.analysis.title,
                    summary = group.analysis.summary,
                    category = group.analysis.category,
                    recommendedAction = group.analysis.recommendedAction,
                    reminderAt = group.analysis.reminderAt,
                    status = MemoStatus.Pending.name,
                    createdAt = createdAt,
                    updatedAt = createdAt,
                ),
            )
        }
    }

    private fun createImagePart(uri: Uri): MultipartBody.Part {
        val contentResolver = appContext.contentResolver
        val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
        return MultipartBody.Part.createFormData(
            name = "images",
            filename = "capture.jpg",
            body = ContentUriRequestBody(contentResolver, uri, mimeType),
        )
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

    override fun onCleared() {
        observer?.let(screenshotMediaStore::unregisterObserver)
        observer = null
        super.onCleared()
    }
}

private class ContentUriRequestBody(
    private val contentResolver: ContentResolver,
    private val uri: Uri,
    private val mimeType: String,
) : RequestBody() {
    override fun contentType() = mimeType.toMediaType()

    override fun contentLength(): Long {
        return contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            descriptor.length
        } ?: -1L
    }

    override fun writeTo(sink: BufferedSink) {
        val inputStream = contentResolver.openInputStream(uri)
            ?: error("이미지 스트림을 열 수 없습니다.")
        inputStream.use { input ->
            sink.writeAll(input.source())
        }
    }
}

private fun String.toPlainTextRequestBody(): RequestBody {
    return toRequestBody("text/plain".toMediaType())
}

private fun retrofit2.Response<ResponseBody>.toBackendAnalyzePayload(): BackendAnalyzePayload {
    val raw = if (isSuccessful) {
        body()?.string().orEmpty()
    } else {
        errorBody()?.string().orEmpty()
    }

    if (!isSuccessful) {
        error("백엔드 HTTP ${code()}: ${raw.ifBlank { message() }}")
    }
    if (raw.isBlank()) {
        error("백엔드 응답 body가 비어 있습니다.")
    }

    return BackendAnalyzePayload(
        rawJson = raw,
        parsed = BackendResponseJson.decodeFromString(raw),
    )
}

@Serializable
private data class AnalyzeImageMetadata(
    val clientId: String,
    val capturedAt: Long,
)

private data class BackendAnalyzePayload(
    val rawJson: String,
    val parsed: AnalyzeBatchResponse,
)

private fun ScreenshotImage.bestTimestampMillis(): Long {
    return dateTakenMillis
        ?: dateAddedMillis
        ?: dateModifiedMillis
        ?: System.currentTimeMillis()
}

private fun BackendAnalyzePayload?.toUploadResult(
    uploads: List<PendingScreenshotUpload>,
    durationMillis: Long,
): AnalyzeUploadResult {
    val payload = this ?: error("백엔드 응답이 비어 있습니다.")
    val response = payload.parsed
    val clientIds = uploads.map { it.clientCaptureId }.toSet()
    val group = response.groups.firstOrNull { group ->
        group.memberClientIds.any { it in clientIds }
    }
        ?: response.groups.firstOrNull()
    val analysis = group?.analysis ?: AnalyzeCaptureResponse(
        title = "저장 제외",
        summary = "백엔드가 분석 가능한 OCR 텍스트를 찾지 못했거나 유용하지 않다고 판단했습니다.",
        category = "unknown",
        isUseful = false,
    )
    val isBatch = uploads.size > 1
    val usefulGroupCount = response.groups.count { it.analysis.isUseful }
    return AnalyzeUploadResult(
        clientCaptureId = if (isBatch) "${uploads.size}개 이미지" else uploads.firstOrNull()?.clientCaptureId.orEmpty(),
        localImageUri = if (isBatch) {
            uploads.joinToString { it.screenshot.displayName.ifBlank { it.screenshot.uri.lastPathSegment.orEmpty() } }
        } else {
            uploads.firstOrNull()?.screenshot?.uri?.toString().orEmpty()
        },
        durationMillis = durationMillis,
        uploadedImageCount = uploads.size,
        groupCount = response.groups.size,
        isUseful = if (isBatch) usefulGroupCount > 0 else analysis.isUseful,
        serverMemoId = if (isBatch) null else analysis.serverMemoId,
        title = if (isBatch) {
            "배치 분석 완료: ${response.groups.size}개 그룹"
        } else {
            analysis.title
        },
        summary = if (isBatch) {
            response.groups.joinToString(separator = "\n") { group ->
                "- ${group.analysis.title}: ${group.analysis.summary}"
            }.ifBlank { "백엔드가 분석 가능한 그룹을 반환하지 않았습니다." }
        } else {
            analysis.summary
        },
        category = if (isBatch) "batch" else analysis.category,
        recommendedAction = if (isBatch) null else analysis.recommendedAction,
        reminderAt = if (isBatch) null else analysis.reminderAt,
        rawBackendResponse = payload.rawJson,
    )
}

fun requiredImagePermission(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}
