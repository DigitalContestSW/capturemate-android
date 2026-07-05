package com.capturemate.app.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    const val CHANNEL_ID = "capturemate_reminders"

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "캡처메이트 리마인드",
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    fun scheduleReminder(
        context: Context,
        workName: String,
        memoId: String,
        title: String,
        body: String,
        triggerAtMillis: Long,
    ) {
        val delayMillis = (triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(0)

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    ReminderWorker.KEY_MEMO_ID to memoId,
                    ReminderWorker.KEY_TITLE to title,
                    ReminderWorker.KEY_BODY to body,
                ),
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(workName, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelReminder(context: Context, workName: String) {
        WorkManager.getInstance(context).cancelUniqueWork(workName)
    }
}
