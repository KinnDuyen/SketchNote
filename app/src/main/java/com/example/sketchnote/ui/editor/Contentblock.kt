package com.example.sketchnote.ui.editor

import java.util.UUID

sealed class ContentBlock {
    abstract val id: String

    data class TextBlock(
        override val id: String = UUID.randomUUID().toString(),
        val text: String = "",
        val highlights: List<HighlightSpan> = emptyList()
    ) : ContentBlock() {
        fun copy(
            text: String = this.text,
            highlights: List<HighlightSpan> = this.highlights
        ): TextBlock = TextBlock(id = this.id, text = text, highlights = highlights)
    }

    data class SketchBlock(
        override val id: String = UUID.randomUUID().toString(),
        val imagePath: String = "",
        val heightDp: Int = 200
    ) : ContentBlock() {
        fun copy(
            imagePath: String = this.imagePath,
            heightDp: Int = this.heightDp
        ): SketchBlock = SketchBlock(id = this.id, imagePath = imagePath, heightDp = heightDp)
    }
}

enum class WrapMode { INLINE, FLOAT_LEFT, FLOAT_RIGHT }

data class PathPoint(val x: Float, val y: Float, val type: PointType)
enum class PointType { MOVE, LINE }

// KHÔNG KHAI BÁO BrushType Ở ĐÂY NỮA
// BrushType đã được khai báo trong SketchView.kt

data class SerializableAction(
    val points: List<PathPoint>,
    val color: Int,
    val strokeWidth: Float,
    val brushType: BrushType,  // BrushType lấy từ SketchView.kt
    val alpha: Int = 255
)