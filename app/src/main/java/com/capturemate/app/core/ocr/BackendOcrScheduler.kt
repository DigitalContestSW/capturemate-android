package com.capturemate.app.core.ocr

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object BackendOcrScheduler {
    private const val TAG = "BackendOcr"
    private const val PERIODIC_WORK_NAME = "backend_ocr_periodic_sync"
    private const val STARTUP_WORK_NAME = "backend_ocr_startup_sync"

    fun schedule(context: Context) {
        Log.i(TAG, "Scheduling backend OCR worker: startup once + periodic every 15 minutes")

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<BackendOcrSyncWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES,
        )
            .setInitialDelay(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .setInputData(workDataOf(BackendOcrSyncWorker.KEY_LIMIT to BackendOcrSyncWorker.DEFAULT_LIMIT))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        val startupRequest = OneTimeWorkRequestBuilder<BackendOcrSyncWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(BackendOcrSyncWorker.KEY_LIMIT to BackendOcrSyncWorker.DEFAULT_LIMIT))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest,
        )
        workManager.enqueueUniqueWork(
            STARTUP_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            startupRequest,
        )
    }
}
