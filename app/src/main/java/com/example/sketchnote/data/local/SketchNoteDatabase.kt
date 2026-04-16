package com.example.sketchnote.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.sketchnote.data.local.dao.ChecklistItemDao
import com.example.sketchnote.data.local.dao.NoteDao
import com.example.sketchnote.data.local.entity.ChecklistItemEntity
import com.example.sketchnote.data.local.entity.NoteEntity
import com.example.sketchnote.utils.Converters

@Database(
    entities = [NoteEntity::class, ChecklistItemEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SketchNoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun checklistItemDao(): ChecklistItemDao

    companion object {
        @Volatile
        private var INSTANCE: SketchNoteDatabase? = null

        fun getDatabase(context: Context): SketchNoteDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SketchNoteDatabase::class.java,
                    "sketchnote_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // Migration từ version 1 lên 2
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE notes ADD COLUMN sketchData TEXT DEFAULT ''")
                } catch (e: Exception) {
                    // Column might already exist
                }
                try {
                    database.execSQL("ALTER TABLE notes ADD COLUMN isPinned INTEGER DEFAULT 0")
                } catch (e: Exception) {
                    // Column might already exist
                }
            }
        }

        // Migration từ version 2 lên 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                try {
                    database.execSQL("ALTER TABLE notes ADD COLUMN reminderTime INTEGER DEFAULT 0")
                } catch (e: Exception) {
                    // Column might already exist
                }
                try {
                    database.execSQL("ALTER TABLE notes ADD COLUMN pinned INTEGER DEFAULT 0")
                } catch (e: Exception) {
                    // Column might already exist
                }
            }
        }
    }
}