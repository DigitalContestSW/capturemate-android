package com.capturemate.app.core.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.capturemate.app.CaptureMateApplication
import com.capturemate.app.MainActivity
import com.capturemate.app.R
import com.capturemate.app.domain.repository.CaptureRepository
import com.capturemate.app.feature.common.categoryLabel
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val memoId = inputData.getString(KEY_MEMO_ID) ?: return Result.failure()
        val headline = inputData.getString(KEY_TITLE) ?: return Result.failure()

        NotificationScheduler.ensureChannel(applicationContext)

        val hasPermission = ActivityCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val repository = (applicationContext as CaptureMateApplication).appContainer.captureRepository
            val notification = buildReminderNotification(repository, memoId, headline)
            NotificationManagerCompat.from(applicationContext).notify(id.hashCode(), notification)
        }

        return Result.success()
    }

    private suspend fun buildReminderNotification(
        repository: CaptureRepository,
        memoId: String,
        headline: String,
    ): android.app.Notification {
        val memo = repository.observeMemoById(memoId).first()
        val capture = memo?.captureId?.let { repository.observeCaptureById(it).first() }
        val studyItem = repository.observeStudyItem(memoId).first()
        val lifeInfoItem = repository.observeLifeInfoItem(memoId).first()
        val scheduleItem = repository.observeScheduleItem(memoId).first()

        val screenshotUris = when {
            scheduleItem != null && scheduleItem.screenshotUris.isNotEmpty() -> scheduleItem.screenshotUris
            studyItem != null && studyItem.screenshotUris.isNotEmpty() -> studyItem.screenshotUris
            lifeInfoItem != null && lifeInfoItem.screenshotUris.isNotEmpty() -> lifeInfoItem.screenshotUris
            else -> emptyList()
        }
        val thumbnailUri = screenshotUris.firstOrNull() ?: capture?.localImageUri
        val screenshotCount = if (screenshotUris.isNotEmpty()) screenshotUris.size else if (capture != null) 1 else 0
        val thumbnailBitmap = thumbnailUri?.let { decodeThumbnail(it) }

        val contentIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_MEMO_ID, memoId)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            memoId.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val remoteViews = RemoteViews(applicationContext.packageName, R.layout.notification_reminder).apply {
            setTextViewText(R.id.notif_headline, headline)
            setTextViewText(R.id.notif_category, categoryLabel(memo?.category.orEmpty()))
            setTextViewText(R.id.notif_title, memo?.title.orEmpty())
            setTextViewText(R.id.notif_summary, memo?.summary.orEmpty())
            setTextViewText(R.id.notif_meta, memo?.createdAt?.let { formatSavedAt(it) }.orEmpty())

            if (thumbnailBitmap != null) {
                setImageViewBitmap(R.id.notif_thumbnail, thumbnailBitmap)
            }
            if (screenshotCount > 1) {
                setViewVisibility(R.id.notif_screenshot_count, View.VISIBLE)
                setTextViewText(R.id.notif_screenshot_count, "${screenshotCount}장")
            } else {
                setViewVisibility(R.id.notif_screenshot_count, View.GONE)
            }
            setOnClickPendingIntent(R.id.notif_open_button, pendingIntent)
        }

        return NotificationCompat.Builder(applicationContext, NotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(headline)
            .setContentText(memo?.title.orEmpty())
            .setSubText("리마인드")
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomBigContentView(remoteViews)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private suspend fun decodeThumbnail(uriString: String): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            applicationContext.contentResolver.openInputStream(Uri.parse(uriString))?.use { input ->
                BitmapFactory.decodeStream(input)
            }
        }.getOrNull()
    }

    private fun formatSavedAt(createdAt: Long): String {
        val formatter = SimpleDateFormat("M월 d일 저장", Locale.KOREA)
        return formatter.format(java.util.Date(createdAt))
    }

    companion object {
        const val KEY_MEMO_ID = "memoId"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
    }
}
