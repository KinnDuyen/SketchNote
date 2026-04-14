package com.example.sketchnote.utils

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

object OcrUtils {

    fun recognizeText(
        context: Context,
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val image = InputImage.fromFilePath(context, imageUri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            recognizer.process(image)
                .addOnSuccessListener { result ->
                    val text = result.text
                    if (text.isBlank()) onError("Không tìm thấy chữ trong ảnh")
                    else onSuccess(text)
                }
                .addOnFailureListener { e ->
                    onError("Lỗi OCR: ${e.message}")
                }
        } catch (e: Exception) {
            onError("Lỗi: ${e.message}")
        }
    }
}