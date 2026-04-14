package com.example.sketchnote.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sketchnote.data.local.entity.NoteEntity
import com.example.sketchnote.data.repository.NoteRepository
import com.example.sketchnote.util.VoiceSearchManager
import com.example.sketchnote.util.VoiceState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ColorFilter {
    ALL, RED, ORANGE, YELLOW, GREEN, BLUE, PURPLE
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: NoteRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _colorFilter = MutableStateFlow(ColorFilter.ALL)
    val colorFilter: StateFlow<ColorFilter> = _colorFilter.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val notes: StateFlow<List<NoteEntity>> = combine(
        _searchQuery, _colorFilter
    ) { query, color -> Pair(query, color) }
        .flatMapLatest { (query, color) ->
            when {
                query.isNotBlank() -> repository.searchNotes(query)
                color != ColorFilter.ALL -> repository.getNotesByColor(color.name)
                else -> repository.getAllNotes()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- TÌNH NĂNG GIỌNG NÓI ---
    private var voiceManager: VoiceSearchManager? = null

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    fun initVoice(context: Context) {
        if (voiceManager == null) {
            voiceManager = VoiceSearchManager(context)
        }
    }

    fun startVoiceSearch() {
        voiceManager?.startListening { text ->
            _searchQuery.value = text
        }
        _voiceState.value = VoiceState.LISTENING
    }

    fun stopVoiceSearch() {
        voiceManager?.stopListening()
        _voiceState.value = VoiceState.IDLE
    }

    // --- CÁC HÀM XỬ LÝ DỮ LIỆU ---
    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onColorFilterChange(color: ColorFilter) {
        _colorFilter.value = color
    }

    fun moveToTrash(noteId: Int) {
        viewModelScope.launch { repository.moveToTrash(noteId) }
    }

    fun togglePin(note: NoteEntity) {
        viewModelScope.launch {
            // Đảo ngược trạng thái isPinned hiện tại của note
            repository.togglePin(note.id, !note.isPinned)
        }
    }

    override fun onCleared() {
        super.onCleared()
        voiceManager?.stopListening()
    }
}