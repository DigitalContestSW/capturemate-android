package com.capturemate.app.core.ai

import android.net.Uri

interface OcrTextExtractor {
    suspend fun extractText(imageUri: Uri): String
}
