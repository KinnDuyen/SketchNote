package com.example.sketchnote.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.sketchnote.data.local.SketchNoteDatabase
import com.example.sketchnote.data.local.dao.ChecklistItemDao
import com.example.sketchnote.data.local.dao.NoteDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SketchNoteDatabase {
        return Room.databaseBuilder(
            context,
            SketchNoteDatabase::class.java,
            "sketchnote_db"
        )
            .addMigrations(MIGRATION_1_2) // Kích hoạt Migration từ bản 1 sang 2
            .build()
    }

    @Provides
    @Singleton
    fun provideNoteDao(db: SketchNoteDatabase): NoteDao = db.noteDao()

    @Provides
    @Singleton
    fun provideChecklistItemDao(db: SketchNoteDatabase): ChecklistItemDao =
        db.checklistItemDao()
}

/**
 * Định nghĩa Migration để thêm cột isPinned vào bảng notes
 * SQLite không có kiểu Boolean riêng, nên ta dùng INTEGER (0 là false, 1 là true)
 */
private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE notes ADD COLUMN isPinned INTEGER NOT NULL DEFAULT 0")
    }
}