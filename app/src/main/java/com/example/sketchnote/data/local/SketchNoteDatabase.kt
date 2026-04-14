package com.example.sketchnote.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.sketchnote.data.local.dao.ChecklistItemDao
import com.example.sketchnote.data.local.dao.NoteDao
import com.example.sketchnote.data.local.entity.ChecklistItemEntity
import com.example.sketchnote.data.local.entity.NoteEntity

@Database(
    entities = [NoteEntity::class, ChecklistItemEntity::class],
    version = 2,
    exportSchema = false
)
abstract class SketchNoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun checklistItemDao(): ChecklistItemDao
}