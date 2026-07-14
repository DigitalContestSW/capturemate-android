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
    const val PERIODIC_WORK_NAME = "backend_ocr_periodic_sync"
    const val STARTUP_WORK_NAME = "backend_ocr_startup_sync"

    fun schedule(context: Context) {
        Log.i(TAG, "Scheduling backend OCR worker: startup once + periodic every 1 hour")

        val periodicRequest = PeriodicWorkRequestBuilder<BackendOcrSyncWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
        )
            .setInitialDelay(1, TimeUnit.HOURS)
            .setConstraints(networkConstraints())
            .setInputData(workDataOf(BackendOcrSyncWorker.KEY_LIMIT to BackendOcrSyncWorker.DEFAULT_LIMIT))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest,
        )
        enqueueImmediateSync(context, ExistingWorkPolicy.REPLACE)
    }

    fun requestImmediateSync(context: Context) {
        Log.i(TAG, "Requesting immediate backend OCR sync")
        enqueueImmediateSync(context, ExistingWorkPolicy.KEEP)
    }

    private fun enqueueImmediateSync(context: Context, policy: ExistingWorkPolicy) {
        val startupRequest = OneTimeWorkRequestBuilder<BackendOcrSyncWorker>()
            .setConstraints(networkConstraints())
            .setInputData(workDataOf(BackendOcrSyncWorker.KEY_LIMIT to BackendOcrSyncWorker.DEFAULT_LIMIT))
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()

        val workManager = WorkManager.getInstance(context)
        workManager.enqueueUniqueWork(
            STARTUP_WORK_NAME,
            policy,
            startupRequest,
        )
    }

    private fun networkConstraints(): Constraints =
        Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
}
