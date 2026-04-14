package com.example.sketchnote.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sketchnote.data.local.entity.NoteEntity
import com.example.sketchnote.data.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    private val repository: NoteRepository
) : ViewModel() {

    val deletedNotes: StateFlow<List<NoteEntity>> = repository.getDeletedNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restore(id: Int) = viewModelScope.launch { repository.restoreFromTrash(id) }
    fun deletePermanently(id: Int) = viewModelScope.launch { repository.deleteNotePermanently(id) }
    fun emptyTrash() = viewModelScope.launch { repository.emptyTrash() }
}