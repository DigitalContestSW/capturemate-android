package com.capturemate.app.core.calendar

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GoogleCalendarClient(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    suspend fun requestAccessToken(context: Context): CalendarAuthorizationResult {
        val authorizationRequest = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(CALENDAR_SCOPE)))
            .build()

        val result = Identity.getAuthorizationClient(context)
            .authorize(authorizationRequest)
            .await()

        if (result.hasResolution()) {
            return CalendarAuthorizationResult.NeedsUserConsent(
                result.pendingIntent ?: error("Google Calendar authorization resolution was empty."),
            )
        }

        return CalendarAuthorizationResult.Authorized(
            accessToken = result.accessToken ?: error("Google Calendar access token was empty."),
        )
    }

    fun readAccessTokenFromConsentResult(context: Context, data: Intent?): String {
        val result = Identity.getAuthorizationClient(context)
            .getAuthorizationResultFromIntent(data ?: error("Google Calendar authorization was canceled."))

        return result.accessToken ?: error("Google Calendar access token was empty.")
    }

    suspend fun insertEvent(
        accessToken: String,
        event: GoogleCalendarEvent,
    ): GoogleCalendarInsertResult = withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            put("summary", event.title)
            event.location?.let { put("location", it) }
            event.description?.let { put("description", it) }
            put(
                "start",
                buildJsonObject {
                    put("dateTime", event.startDateTime)
                    put("timeZone", event.timeZone)
                },
            )
            put(
                "end",
                buildJsonObject {
                    put("dateTime", event.endDateTime)
                    put("timeZone", event.timeZone)
                },
            )
        }

        val request = Request.Builder()
            .url("https://www.googleapis.com/calendar/v3/calendars/primary/events")
            .addHeader("Authorization", "Bearer $accessToken")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            val responseBody = response.body.string()
            if (!response.isSuccessful) {
                throw IOException("Google Calendar event insert failed: ${response.code} $responseBody")
            }

            val responseJson = json.parseToJsonElement(responseBody).jsonObject
            GoogleCalendarInsertResult(
                eventId = responseJson["id"]?.jsonPrimitive?.content,
                htmlLink = responseJson["htmlLink"]?.jsonPrimitive?.content,
            )
        }
    }

    private companion object {
        const val CALENDAR_SCOPE = "https://www.googleapis.com/auth/calendar"
    }
}

sealed interface CalendarAuthorizationResult {
    data class Authorized(val accessToken: String) : CalendarAuthorizationResult
    data class NeedsUserConsent(val pendingIntent: PendingIntent) : CalendarAuthorizationResult
}

data class GoogleCalendarEvent(
    val title: String,
    val description: String?,
    val location: String?,
    val startDateTime: String,
    val endDateTime: String,
    val timeZone: String,
)

data class GoogleCalendarInsertResult(
    val eventId: String?,
    val htmlLink: String?,
)
