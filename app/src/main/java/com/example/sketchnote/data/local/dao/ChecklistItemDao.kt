package com.example.sketchnote.data.local.dao

import androidx.room.*
import com.example.sketchnote.data.local.entity.ChecklistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChecklistItemDao {

    // Sửa `orderIndex` thành `order` cho khớp với entity
    @Query("SELECT * FROM checklist_items WHERE noteId = :noteId ORDER BY `order` ASC")
    fun getItemsByNote(noteId: Int): Flow<List<ChecklistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ChecklistItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ChecklistItemEntity>)

    @Update
    suspend fun updateItem(item: ChecklistItemEntity)

    @Query("DELETE FROM checklist_items WHERE noteId = :noteId")
    suspend fun deleteItemsByNote(noteId: Int)

    @Query("DELETE FROM checklist_items WHERE id = :id")
    suspend fun deleteItem(id: Int)
}