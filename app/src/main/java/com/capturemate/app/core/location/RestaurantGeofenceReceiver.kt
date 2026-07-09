package com.capturemate.app.core.location

import android.Manifest
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.capturemate.app.MainActivity
import com.capturemate.app.R
import com.capturemate.app.core.notification.NotificationScheduler
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class RestaurantGeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        if (
            event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER &&
            event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_DWELL
        ) {
            return
        }

        val memoId = intent.getStringExtra(EXTRA_MEMO_ID) ?: return
        val title = intent.getStringExtra(EXTRA_TITLE)
            ?: "근처에 저장한 맛집이 있어요"
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()

        NotificationScheduler.ensureChannel(context)

        val hasPermission = ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_MEMO_ID, memoId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            memoId.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, NotificationScheduler.CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context)
            .notify("restaurant_geofence_$memoId".hashCode(), notification)
    }

    companion object {
        const val EXTRA_MEMO_ID = "memoId"
        const val EXTRA_TITLE = "title"
        const val EXTRA_BODY = "body"
    }
}
