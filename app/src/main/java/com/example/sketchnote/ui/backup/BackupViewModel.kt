package com.example.sketchnote.ui.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sketchnote.data.repository.NoteRepository
import com.example.sketchnote.utils.BackupUtils
import com.example.sketchnote.utils.ExportUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: NoteRepository
) : ViewModel() {

    // Xuất backup JSON
    fun exportBackup(context: Context) {
        viewModelScope.launch {
            val notes = repository.getAllNotes().first()
            val allItems = notes.flatMap { note ->
                repository.getChecklistItems(note.id).first()
            }
            BackupUtils.exportBackup(context, notes, allItems)
        }
    }

    // Nhập backup từ file JSON
    fun importBackup(context: Context, uri: Uri, onDone: () -> Unit) {
        viewModelScope.launch {
            val (notes, items) = BackupUtils.parseBackup(context, uri) ?: return@launch

            // Lưu từng note và map lại noteId cho checklist
            notes.forEach { note ->
                val newId = repository.insertNote(note).toInt()
                // Lấy items thuộc note cũ (theo thứ tự trong file)
                val noteItems = items.filter { it.noteId == note.id }
                    .mapIndexed { i, item -> item.copy(noteId = newId, id = 0, order = i) }
                if (noteItems.isNotEmpty()) {
                    repository.insertItems(noteItems)
                }
            }
            onDone()
        }
    }
}