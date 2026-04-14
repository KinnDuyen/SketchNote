package com.example.sketchnote.data.repository

import com.example.sketchnote.data.local.dao.ChecklistItemDao
import com.example.sketchnote.data.local.dao.NoteDao
import com.example.sketchnote.data.local.entity.ChecklistItemEntity
import com.example.sketchnote.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val dao: NoteDao,
    private val checklistDao: ChecklistItemDao
) {
    fun getAllNotes(): Flow<List<NoteEntity>> = dao.getAllNotes()
    fun getDeletedNotes(): Flow<List<NoteEntity>> = dao.getDeletedNotes()
    fun searchNotes(query: String): Flow<List<NoteEntity>> = dao.searchNotes(query)
    fun getNotesByColor(colorTag: String): Flow<List<NoteEntity>> = dao.getNotesByColor(colorTag)
    suspend fun getNoteById(id: Int): NoteEntity? = dao.getNoteById(id)
    suspend fun insertNote(note: NoteEntity): Long = dao.insertNote(note)
    suspend fun updateNote(note: NoteEntity) = dao.updateNote(note)
    suspend fun moveToTrash(id: Int) = dao.moveToTrash(id)
    suspend fun restoreFromTrash(id: Int) = dao.restoreFromTrash(id)
    suspend fun deleteNotePermanently(id: Int) = dao.deleteNotePermanently(id)
    suspend fun emptyTrash() = dao.emptyTrash()
    suspend fun togglePin(id: Int, pinned: Boolean) = dao.togglePin(id, pinned)

    // upsertNote = insert nếu id=0, update nếu đã có id
    suspend fun upsertNote(note: NoteEntity): Long {
        return if (note.id == 0) {
            dao.insertNote(note)
        } else {
            dao.updateNote(note)
            note.id.toLong()
        }
    }

    // Checklist
    fun getChecklistItems(noteId: Int): Flow<List<ChecklistItemEntity>> =
        checklistDao.getItemsByNote(noteId)
    suspend fun insertItems(items: List<ChecklistItemEntity>) =
        checklistDao.insertItems(items)
    suspend fun updateItem(item: ChecklistItemEntity) =
        checklistDao.updateItem(item)
    suspend fun deleteItem(id: Int) =
        checklistDao.deleteItem(id)
    suspend fun deleteItemsByNote(noteId: Int) =
        checklistDao.deleteItemsByNote(noteId)
}