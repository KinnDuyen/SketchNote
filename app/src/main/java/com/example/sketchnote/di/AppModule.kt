package com.example.sketchnote.di

import android.content.Context
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
        return SketchNoteDatabase.getDatabase(context)
    }

    @Provides
    @Singleton
    fun provideNoteDao(db: SketchNoteDatabase): NoteDao = db.noteDao()

    @Provides
    @Singleton
    fun provideChecklistItemDao(db: SketchNoteDatabase): ChecklistItemDao = db.checklistItemDao()
}