package com.capturemate.app.core.location

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.tasks.await

class RestaurantGeofenceManager(
    private val context: Context,
) {
    private val appContext = context.applicationContext
    private val geofencingClient = LocationServices.getGeofencingClient(appContext)

    suspend fun register(
        restaurantMemoId: String,
        memoId: String,
        name: String,
        latitude: Double,
        longitude: Double,
        radiusMeters: Float = DEFAULT_RADIUS_METERS,
    ): Boolean {
        if (!hasRequiredLocationPermission()) return false

        val geofence = Geofence.Builder()
            .setRequestId(restaurantMemoId)
            .setCircularRegion(latitude, longitude, radiusMeters)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_DWELL)
            .setLoiteringDelay(DWELL_DELAY_MILLIS)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val pendingIntent = geofencePendingIntent(
            restaurantMemoId = restaurantMemoId,
            memoId = memoId,
            title = "근처에 저장한 맛집이 있어요",
            body = name,
        )

        return runCatching {
            geofencingClient.addGeofences(request, pendingIntent).await()
        }.isSuccess
    }

    suspend fun unregister(restaurantMemoId: String) {
        runCatching {
            geofencingClient.removeGeofences(listOf(restaurantMemoId)).await()
        }
    }

    private fun hasRequiredLocationPermission(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        val backgroundLocationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED

        return fineLocationGranted && backgroundLocationGranted
    }

    private fun geofencePendingIntent(
        restaurantMemoId: String,
        memoId: String,
        title: String,
        body: String,
    ): PendingIntent {
        val intent = Intent(appContext, RestaurantGeofenceReceiver::class.java).apply {
            putExtra(RestaurantGeofenceReceiver.EXTRA_MEMO_ID, memoId)
            putExtra(RestaurantGeofenceReceiver.EXTRA_TITLE, title)
            putExtra(RestaurantGeofenceReceiver.EXTRA_BODY, body)
        }

        return PendingIntent.getBroadcast(
            appContext,
            restaurantMemoId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    companion object {
        const val DEFAULT_RADIUS_METERS = 200f
        private const val DWELL_DELAY_MILLIS = 5 * 60 * 1000
    }
}
