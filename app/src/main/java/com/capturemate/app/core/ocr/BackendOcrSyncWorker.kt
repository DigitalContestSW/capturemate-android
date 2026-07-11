package com.capturemate.app.core.ocr

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.capturemate.app.CaptureMateApplication
import com.capturemate.app.feature.debugocr.requiredImagePermission
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

class BackendOcrSyncWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val limit = inputData.getInt(KEY_LIMIT, DEFAULT_LIMIT)
        Log.i(TAG, "Worker started: id=$id, runAttemptCount=$runAttemptCount, limit=$limit")

        if (!hasImagePermission()) {
            Log.w(TAG, "Worker skipped: image permission is not granted")
            return Result.success()
        }

        return runCatching {
            val appContainer = (applicationContext as CaptureMateApplication).appContainer
            appContainer.backendOcrProcessor.processLatestScreenshots(
                limit = limit,
                force = false,
            )
        }.fold(
            onSuccess = { result ->
                Log.i(
                    TAG,
                    "Worker completed: uploaded=${result.uploadedImageCount}, " +
                        "skipped=${result.skippedImageCount}, groups=${result.groupCount}, " +
                        "durationMs=${result.durationMillis}",
                )
                Result.success()
            },
            onFailure = { throwable ->
                when (throwable) {
                    is ConnectException,
                    is SocketTimeoutException,
                    is UnknownHostException,
                    -> {
                        Log.w(TAG, "Worker retry: ${throwable.javaClass.simpleName}: ${throwable.message}")
                        Result.retry()
                    }
                    else -> {
                        Log.e(TAG, "Worker failed: ${throwable.javaClass.simpleName}: ${throwable.message}", throwable)
                        Result.failure()
                    }
                }
            },
        )
    }

    private fun hasImagePermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true
        return ContextCompat.checkSelfPermission(
            applicationContext,
            requiredImagePermission(),
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "BackendOcr"
        const val KEY_LIMIT = "limit"
        const val DEFAULT_LIMIT = 100
    }
}
