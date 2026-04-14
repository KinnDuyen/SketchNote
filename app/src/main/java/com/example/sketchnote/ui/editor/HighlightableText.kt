package com.example.sketchnote.ui.editor

import androidx.compose.ui.graphics.Color
import java.util.UUID

data class HighlightSpan(
    val id: String = UUID.randomUUID().toString(),
    val start: Int,
    val end: Int,
    val color: Color
)

// Helper: serialize highlight list thành string để lưu DB
fun List<HighlightSpan>.toJson(): String =
    joinToString(";;;") { h ->
        "${h.start},${h.end},${h.color.value}"
    }

// Helper: parse từ string ra list
fun parseHighlights(raw: String): List<HighlightSpan> {
    if (raw.isBlank()) return emptyList()
    return raw.split(";;;").mapNotNull { part ->
        val parts = part.split(",")
        if (parts.size < 3) return@mapNotNull null
        try {
            HighlightSpan(
                start = parts[0].toInt(),
                end = parts[1].toInt(),
                color = Color(parts[2].toULong())
            )
        } catch (_: Exception) { null }
    }
}