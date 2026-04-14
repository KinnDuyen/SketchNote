package com.example.sketchnote.data.local.dao

import androidx.room.*
import com.example.sketchnote.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE isDeleted = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getAllNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun getDeletedNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY isPinned DESC, updatedAt DESC")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND colorTag = :colorTag ORDER BY isPinned DESC, updatedAt DESC")
    fun getNotesByColor(colorTag: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("UPDATE notes SET isPinned = :pinned WHERE id = :id")
    suspend fun togglePin(id: Int, pinned: Boolean)

    @Query("UPDATE notes SET isDeleted = 1, updatedAt = :time WHERE id = :id")
    suspend fun moveToTrash(id: Int, time: Long = System.currentTimeMillis())

    @Query("UPDATE notes SET isDeleted = 0, updatedAt = :time WHERE id = :id")
    suspend fun restoreFromTrash(id: Int, time: Long = System.currentTimeMillis())

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNotePermanently(id: Int)

    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun emptyTrash()
}