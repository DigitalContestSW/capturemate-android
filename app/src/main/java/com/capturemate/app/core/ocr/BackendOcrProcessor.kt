package com.capturemate.app.core.ocr

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import com.capturemate.app.data.local.entity.CaptureEntity
import com.capturemate.app.data.local.entity.MemoEntity
import com.capturemate.app.data.remote.CaptureMateApi
import com.capturemate.app.data.remote.dto.AnalyzeBatchResponse
import com.capturemate.app.domain.model.MemoStatus
import com.capturemate.app.domain.model.normalizeCaptureCategory
import com.capturemate.app.domain.repository.CaptureRepository
import com.capturemate.app.feature.debugocr.ScreenshotImage
import com.capturemate.app.feature.debugocr.ScreenshotMediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okio.BufferedSink
import okio.source
import retrofit2.Response
import java.util.UUID
import kotlin.system.measureTimeMillis

private const val DEFAULT_LOCALE = "ko-KR"
private const val TAG = "BackendOcr"
private const val PROCESSED_PREFS = "backend_ocr_processed"
private const val PROCESSED_URI_SET = "processed_uri_set"
private val BackendResponseJson = Json { ignoreUnknownKeys = true }

data class BackendOcrProcessResult(
    val uploadedImageCount: Int,
    val skippedImageCount: Int,
    val groupCount: Int,
    val durationMillis: Long,
    val rawBackendResponse: String?,
)

class BackendOcrProcessor(
    private val context: Context,
    private val captureMateApi: CaptureMateApi,
    private val captureRepository: CaptureRepository,
) {
    private val appContext = context.applicationContext
    private val screenshotMediaStore = ScreenshotMediaStore(appContext)
    private val json = Json { explicitNulls = false }
    private val processedPreferences = appContext.getSharedPreferences(PROCESSED_PREFS, Context.MODE_PRIVATE)

    suspend fun processLatestScreenshots(
        limit: Int,
        force: Boolean = false,
    ): BackendOcrProcessResult {
        Log.i(TAG, "Finding latest screenshots: limit=$limit, force=$force")
        val screenshots = withContext(Dispatchers.IO) {
            screenshotMediaStore.findLatestScreenshots(limit)
        }
        Log.i(
            TAG,
            "Found screenshots: count=${screenshots.size}, " +
                "items=${screenshots.joinToString(limit = 5) { it.displayName.ifBlank { it.uri.lastPathSegment.orEmpty() } }}",
        )
        return processScreenshots(screenshots = screenshots, force = force)
    }

    suspend fun processScreenshots(
        screenshots: List<ScreenshotImage>,
        force: Boolean = false,
    ): BackendOcrProcessResult {
        if (screenshots.isEmpty()) {
            Log.i(TAG, "No screenshots found. Nothing to upload.")
            return BackendOcrProcessResult(
                uploadedImageCount = 0,
                skippedImageCount = 0,
                groupCount = 0,
                durationMillis = 0,
                rawBackendResponse = null,
            )
        }

        val processedUris = processedUris()
        val uploads = screenshots
            .filter { force || it.uri.toString() !in processedUris }
            .map { screenshot ->
                PendingScreenshotUpload(
                    screenshot = screenshot,
                    clientCaptureId = UUID.randomUUID().toString(),
                    capturedAt = screenshot.bestTimestampMillis(),
                )
            }
        val skippedCount = screenshots.size - uploads.size
        Log.i(
            TAG,
            "Prepared screenshots: total=${screenshots.size}, upload=${uploads.size}, skipped=$skippedCount",
        )
        if (uploads.isEmpty()) {
            Log.i(TAG, "All screenshots were already processed. Nothing to upload.")
            return BackendOcrProcessResult(
                uploadedImageCount = 0,
                skippedImageCount = skippedCount,
                groupCount = 0,
                durationMillis = 0,
                rawBackendResponse = null,
            )
        }

        var payload: BackendAnalyzePayload? = null
        val durationMillis = measureTimeMillis {
            payload = withContext(Dispatchers.IO) {
                Log.i(TAG, "Uploading screenshots to backend: count=${uploads.size}")
                val metadata = uploads.map { upload ->
                    AnalyzeImageMetadata(
                        clientId = upload.clientCaptureId,
                        capturedAt = upload.capturedAt,
                    )
                }
                captureMateApi.analyzeCapture(
                    images = uploads.map { createImagePart(it.screenshot.uri) },
                    locale = DEFAULT_LOCALE.toPlainTextRequestBody(),
                    metadata = json.encodeToString(metadata).toPlainTextRequestBody(),
                ).toBackendAnalyzePayload()
            }
        }

        val response = payload ?: error("백엔드 응답이 비어 있습니다.")
        Log.i(
            TAG,
            "Backend response parsed: groups=${response.parsed.groups.size}, durationMs=$durationMillis",
        )
        withContext(Dispatchers.IO) {
            saveBatchResult(
                uploads = uploads,
                response = response.parsed,
                createdAt = System.currentTimeMillis(),
            )
            markProcessed(uploads.map { it.screenshot.uri.toString() })
        }
        Log.i(
            TAG,
            "Saved OCR result and marked processed: uploaded=${uploads.size}, skipped=$skippedCount",
        )

        return BackendOcrProcessResult(
            uploadedImageCount = uploads.size,
            skippedImageCount = skippedCount,
            groupCount = response.parsed.groups.size,
            durationMillis = durationMillis,
            rawBackendResponse = response.rawJson,
        )
    }

    private fun createImagePart(uri: Uri): MultipartBody.Part {
        val contentResolver = appContext.contentResolver
        val mimeType = contentResolver.getType(uri) ?: "image/jpeg"
        return MultipartBody.Part.createFormData(
            name = "images",
            filename = uri.lastPathSegment ?: "capture.jpg",
            body = ContentUriRequestBody(contentResolver, uri, mimeType),
        )
    }

    private suspend fun saveBatchResult(
        uploads: List<PendingScreenshotUpload>,
        response: AnalyzeBatchResponse,
        createdAt: Long,
    ) {
        val uploadsByClientId = uploads.associateBy { it.clientCaptureId }
        response.groups
            .filter { it.analysis.isUseful }
            .forEach { group ->
                val category = normalizeCaptureCategory(group.analysis.category)
                val memoId = group.analysis.serverMemoId ?: UUID.randomUUID().toString()
                group.memberClientIds.forEach { clientId ->
                    val upload = uploadsByClientId[clientId] ?: return@forEach
                    captureRepository.upsertCapture(
                        CaptureEntity(
                            id = upload.clientCaptureId,
                            localImageUri = upload.screenshot.uri.toString(),
                            rawTextLocalOnly = "",
                            maskedText = "",
                            category = category,
                            capturedAt = upload.capturedAt,
                            createdAt = createdAt,
                        ),
                    )
                }
                val memo = MemoEntity(
                    id = memoId,
                    captureId = group.memberClientIds.firstOrNull(),
                    serverMemoId = group.analysis.serverMemoId,
                    title = group.analysis.title,
                    summary = group.analysis.summary,
                    category = category,
                    recommendedAction = group.analysis.recommendedAction,
                    reminderAt = group.analysis.reminderAt,
                    status = MemoStatus.Pending.name,
                    createdAt = createdAt,
                    updatedAt = createdAt,
                )
                captureRepository.upsertMemo(memo)
                captureRepository.upsertMemoDetails(
                    memo = memo,
                    analysis = group.analysis,
                    screenshotUris = group.memberClientIds.mapNotNull { clientId ->
                        uploadsByClientId[clientId]?.screenshot?.uri?.toString()
                    },
                    createdAt = createdAt,
                )
            }
    }

    fun isProcessed(uri: String): Boolean {
        return uri in processedUris()
    }

    fun markProcessed(uris: List<String>) {
        val updated = processedUris() + uris
        processedPreferences.edit()
            .putStringSet(PROCESSED_URI_SET, updated)
            .apply()
    }

    private fun processedUris(): Set<String> {
        return processedPreferences.getStringSet(PROCESSED_URI_SET, emptySet()).orEmpty()
    }
}

private data class PendingScreenshotUpload(
    val screenshot: ScreenshotImage,
    val clientCaptureId: String,
    val capturedAt: Long,
)

@Serializable
private data class AnalyzeImageMetadata(
    val clientId: String,
    val capturedAt: Long,
)

private data class BackendAnalyzePayload(
    val rawJson: String,
    val parsed: AnalyzeBatchResponse,
)

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

private fun Response<ResponseBody>.toBackendAnalyzePayload(): BackendAnalyzePayload {
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

private fun ScreenshotImage.bestTimestampMillis(): Long {
    return dateTakenMillis
        ?: dateAddedMillis
        ?: dateModifiedMillis
        ?: System.currentTimeMillis()
}
