package com.example.sketchnote.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        ForeignKey(
            entity = NoteEntity::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["noteId"])] // Tối ưu hóa hiệu suất khi tìm kiếm items theo noteId
)
data class ChecklistItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val noteId: Int,
    val text: String = "",
    val isChecked: Boolean = false,
    val order: Int = 0
)