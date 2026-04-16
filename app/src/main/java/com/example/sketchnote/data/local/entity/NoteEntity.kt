package com.example.sketchnote.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String = "",
    val content: String = "",
    val type: String = "TEXT",
    val colorTag: String = "NONE",
    val imagePaths: String = "",
    val sketchPath: String = "",
    val sketchData: String? = null,
    val reminderTime: Long = 0L,
    val isPinned: Boolean = false,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)