package com.kurupdevs.mynotes.ocr

import android.graphics.BitmapFactory
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import java.io.File

/** On-device OCR for image notes — free, no cloud. */
object OcrProcessor {
    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun extractText(image: File): String = runCatching {
        val bmp = BitmapFactory.decodeFile(image.absolutePath) ?: return@runCatching ""
        val input = InputImage.fromBitmap(bmp, 0)
        val text = recognizer.process(input).await().text.orEmpty()
        if (!bmp.isRecycled) bmp.recycle()
        text
    }.getOrDefault("")
}
