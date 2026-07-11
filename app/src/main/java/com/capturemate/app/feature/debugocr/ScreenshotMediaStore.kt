package com.capturemate.app.feature.debugocr

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore

data class ScreenshotImage(
    val uri: Uri,
    val displayName: String,
    val relativePath: String,
    val dateAddedMillis: Long?,
    val dateModifiedMillis: Long?,
    val dateTakenMillis: Long?,
)

class ScreenshotMediaStore(
    private val context: Context,
) {
    private val contentResolver: ContentResolver = context.contentResolver

    fun findLatestScreenshot(): ScreenshotImage? {
        return findLatestScreenshots(limit = 1).firstOrNull()
    }

    fun findLatestScreenshots(limit: Int): List<ScreenshotImage> {
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.DATE_TAKEN,
        )
        val selection = buildString {
            append("${MediaStore.Images.Media.RELATIVE_PATH} LIKE ?")
            append(" OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?")
            append(" OR ${MediaStore.Images.Media.DISPLAY_NAME} LIKE ?")
        }
        val selectionArgs = arrayOf("%Screenshots%", "%Screenshot%", "%스크린샷%")
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC, ${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        return contentResolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val screenshots = mutableListOf<ScreenshotImage>()
            while (cursor.moveToNext() && screenshots.size < limit) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                screenshots += ScreenshotImage(
                    uri = ContentUris.withAppendedId(collection, id),
                    displayName = cursor.getStringOrEmpty(MediaStore.Images.Media.DISPLAY_NAME),
                    relativePath = cursor.getStringOrEmpty(MediaStore.Images.Media.RELATIVE_PATH),
                    dateAddedMillis = cursor.getSecondsAsMillis(MediaStore.Images.Media.DATE_ADDED),
                    dateModifiedMillis = cursor.getSecondsAsMillis(MediaStore.Images.Media.DATE_MODIFIED),
                    dateTakenMillis = cursor.getLongOrNull(MediaStore.Images.Media.DATE_TAKEN),
                )
            }
            screenshots
        }.orEmpty()
    }

    fun registerObserver(onChanged: () -> Unit): ContentObserver {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                onChanged()
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                onChanged()
            }
        }
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer,
        )
        return observer
    }

    fun unregisterObserver(observer: ContentObserver) {
        contentResolver.unregisterContentObserver(observer)
    }
}

private fun android.database.Cursor.getStringOrEmpty(columnName: String): String {
    val index = getColumnIndex(columnName)
    return if (index >= 0 && !isNull(index)) getString(index) else ""
}

private fun android.database.Cursor.getLongOrNull(columnName: String): Long? {
    val index = getColumnIndex(columnName)
    return if (index >= 0 && !isNull(index)) getLong(index) else null
}

private fun android.database.Cursor.getSecondsAsMillis(columnName: String): Long? {
    return getLongOrNull(columnName)?.times(1000L)
}
