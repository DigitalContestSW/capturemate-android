package com.capturemate.app.core.ai

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import kotlinx.coroutines.tasks.await

class MlKitOcrTextExtractor(
    private val context: Context,
) : OcrTextExtractor {
    private val recognizer = TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())

    override suspend fun extractText(imageUri: Uri): String {
        val image = InputImage.fromFilePath(context, imageUri)
        return recognizer.process(image).await().text
    }
}
