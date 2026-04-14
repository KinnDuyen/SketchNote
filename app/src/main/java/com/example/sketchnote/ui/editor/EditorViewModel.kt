package com.example.sketchnote.ui.editor

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sketchnote.data.local.entity.ChecklistItemEntity
import com.example.sketchnote.data.local.entity.NoteEntity
import com.example.sketchnote.data.repository.NoteRepository
import com.example.sketchnote.utils.AlarmScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val repository: NoteRepository
) : ViewModel() {

    private var currentNoteId: Int = -1
    private var isLoaded = false

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _type = MutableStateFlow("TEXT")
    val type: StateFlow<String> = _type.asStateFlow()

    private val _colorTag = MutableStateFlow("NONE")
    val colorTag: StateFlow<String> = _colorTag.asStateFlow()

    private val _imagePaths = MutableStateFlow<List<String>>(emptyList())
    val imagePaths: StateFlow<List<String>> = _imagePaths.asStateFlow()

    private val _reminderTime = MutableStateFlow(0L)
    val reminderTime: StateFlow<Long> = _reminderTime.asStateFlow()

    private val _checklistItems = MutableStateFlow<List<ChecklistItemEntity>>(emptyList())
    val checklistItems: StateFlow<List<ChecklistItemEntity>> = _checklistItems.asStateFlow()

    private val _content = MutableStateFlow("")
    val content: StateFlow<String> = _content.asStateFlow()

    private val _contentBlocks = MutableStateFlow<List<ContentBlock>>(
        listOf(ContentBlock.TextBlock())
    )
    val contentBlocks: StateFlow<List<ContentBlock>> = _contentBlocks.asStateFlow()

    fun loadNote(noteId: Int) {
        if (isLoaded && currentNoteId == noteId) return
        isLoaded = true
        currentNoteId = noteId

        _title.value = ""
        _content.value = ""
        _type.value = "TEXT"
        _colorTag.value = "NONE"
        _reminderTime.value = 0L
        _imagePaths.value = emptyList()
        _checklistItems.value = emptyList()
        _contentBlocks.value = listOf(ContentBlock.TextBlock())

        if (noteId == -1) return

        viewModelScope.launch {
            repository.getNoteById(noteId)?.let { note ->
                _title.value = note.title
                _content.value = note.content
                _type.value = note.type
                _colorTag.value = note.colorTag
                _reminderTime.value = note.reminderTime
                _imagePaths.value = if (note.imagePaths.isBlank()) emptyList()
                else note.imagePaths.split("||")

                _contentBlocks.value = if (!note.sketchData.isNullOrBlank()) {
                    parseSketchDataToBlocks(note.sketchData)
                } else {
                    listOf(ContentBlock.TextBlock(text = note.content))
                }
            }

            repository.getChecklistItems(noteId)
                .collect { _checklistItems.value = it }
        }
    }

    private fun parseSketchDataToBlocks(sketchData: String): List<ContentBlock> {
        return listOf(ContentBlock.TextBlock(text = _content.value))
    }

    fun updateTextBlock(blockId: String, newText: String) {
        _contentBlocks.value = _contentBlocks.value.map { block ->
            if (block.id == blockId && block is ContentBlock.TextBlock)
                block.copy(text = newText)
            else block
        }
        autoSave()
    }

    fun addTextBlockAfter(afterId: String? = null) {
        val newBlock = ContentBlock.TextBlock()
        _contentBlocks.value = if (afterId == null) {
            _contentBlocks.value + newBlock
        } else {
            val list = _contentBlocks.value.toMutableList()
            val idx = list.indexOfFirst { it.id == afterId }
            if (idx >= 0) list.add(idx + 1, newBlock)
            list
        }
        autoSave()
    }

    fun addSketchBlockAfter(afterId: String? = null) {
        val newBlock = ContentBlock.SketchBlock()
        _contentBlocks.value = if (afterId == null) {
            _contentBlocks.value + newBlock
        } else {
            val list = _contentBlocks.value.toMutableList()
            val idx = list.indexOfFirst { it.id == afterId }
            if (idx >= 0) list.add(idx + 1, newBlock)
            list
        }
        autoSave()
    }

    fun removeBlock(blockId: String) {
        if (_contentBlocks.value.size <= 1) return
        _contentBlocks.value = _contentBlocks.value.filter { it.id != blockId }
        autoSave()
    }

    // ===== HIGHLIGHT FUNCTIONS =====

    fun addHighlight(blockId: String, start: Int, end: Int, color: androidx.compose.ui.graphics.Color) {
        _contentBlocks.value = _contentBlocks.value.map { block ->
            if (block.id == blockId && block is ContentBlock.TextBlock) {
                val newHighlight = HighlightSpan(
                    start = start,
                    end = end,
                    color = color
                )
                // Gộp highlight mới với highlight cũ
                block.copy(highlights = block.highlights + newHighlight)
            } else block
        }
        autoSave()
    }

    fun removeHighlight(blockId: String, highlightId: String) {
        _contentBlocks.value = _contentBlocks.value.map { block ->
            if (block.id == blockId && block is ContentBlock.TextBlock) {
                block.copy(highlights = block.highlights.filter { it.id != highlightId })
            } else block
        }
        autoSave()
    }

    fun clearAllHighlights(blockId: String) {
        _contentBlocks.value = _contentBlocks.value.map { block ->
            if (block.id == blockId && block is ContentBlock.TextBlock) {
                block.copy(highlights = emptyList())
            } else block
        }
        autoSave()
    }

    fun getHighlightsForBlock(blockId: String): List<HighlightSpan> {
        val block = _contentBlocks.value.find { it.id == blockId }
        return (block as? ContentBlock.TextBlock)?.highlights ?: emptyList()
    }

    private fun autoSave() {
        viewModelScope.launch {
            val plainText = _contentBlocks.value
                .filterIsInstance<ContentBlock.TextBlock>()
                .joinToString("\n") { it.text }

            val sketchJson = blocksToJson(_contentBlocks.value)

            val now = System.currentTimeMillis()

            if (currentNoteId == -1) {
                val newId = repository.insertNote(
                    NoteEntity(
                        title = _title.value,
                        content = plainText,
                        type = _type.value,
                        colorTag = _colorTag.value,
                        imagePaths = _imagePaths.value.joinToString("||"),
                        reminderTime = _reminderTime.value,
                        sketchData = sketchJson,
                        createdAt = now,
                        updatedAt = now
                    )
                ).toInt()
                currentNoteId = newId
                isLoaded = false
            } else {
                repository.getNoteById(currentNoteId)?.let {
                    repository.updateNote(
                        it.copy(
                            title = _title.value,
                            content = plainText,
                            type = _type.value,
                            colorTag = _colorTag.value,
                            imagePaths = _imagePaths.value.joinToString("||"),
                            reminderTime = _reminderTime.value,
                            sketchData = sketchJson,
                            updatedAt = now
                        )
                    )
                }
            }
        }
    }

    private fun blocksToJson(blocks: List<ContentBlock>): String {
        return blocks.joinToString("|||") { block ->
            when (block) {
                is ContentBlock.TextBlock -> {
                    val highlightsJson = block.highlights.joinToString(";;;") { h ->
                        "${h.start},${h.end},${h.color.hashCode()}"
                    }
                    "TEXT:${block.text.replace("|||", " ")}:$highlightsJson"
                }
                is ContentBlock.SketchBlock -> "SKETCH:${block.imagePath}:${block.heightDp}"
            }
        }
    }

    fun saveNote(context: Context, onDone: () -> Unit) {
        viewModelScope.launch {
            val plainText = _contentBlocks.value
                .filterIsInstance<ContentBlock.TextBlock>()
                .joinToString("\n") { it.text }
            val now = System.currentTimeMillis()

            val hasText = _title.value.isNotBlank() || plainText.isNotBlank()
            val hasImage = _imagePaths.value.isNotEmpty()
            val hasSketch = _contentBlocks.value
                .filterIsInstance<ContentBlock.SketchBlock>()
                .any { it.imagePath.isNotBlank() }

            if (currentNoteId == -1 && !hasText && !hasImage && !hasSketch) {
                onDone()
                return@launch
            }

            val sketchJson = blocksToJson(_contentBlocks.value)

            val id = if (currentNoteId == -1) {
                val newId = repository.insertNote(
                    NoteEntity(
                        title = _title.value,
                        content = plainText,
                        type = _type.value,
                        colorTag = _colorTag.value,
                        imagePaths = _imagePaths.value.joinToString("||"),
                        reminderTime = _reminderTime.value,
                        sketchData = sketchJson,
                        createdAt = now,
                        updatedAt = now
                    )
                ).toInt()
                currentNoteId = newId
                isLoaded = false
                newId
            } else {
                repository.getNoteById(currentNoteId)?.let {
                    repository.updateNote(
                        it.copy(
                            title = _title.value,
                            content = plainText,
                            type = _type.value,
                            colorTag = _colorTag.value,
                            imagePaths = _imagePaths.value.joinToString("||"),
                            reminderTime = _reminderTime.value,
                            sketchData = sketchJson,
                            updatedAt = now
                        )
                    )
                }
                isLoaded = false
                currentNoteId
            }

            if (_type.value == "CHECKLIST") {
                repository.deleteItemsByNote(id)
                repository.insertItems(
                    _checklistItems.value.mapIndexed { i, item ->
                        item.copy(noteId = id, order = i)
                    }
                )
            }

            if (_reminderTime.value > now) {
                AlarmScheduler.schedule(
                    context = context,
                    noteId = id,
                    title = _title.value.ifBlank { "Nhắc nhở" },
                    content = plainText.ifBlank { "Bạn có ghi chú cần xem!" },
                    timeMillis = _reminderTime.value
                )
            } else {
                AlarmScheduler.cancel(context, id)
            }

            onDone()
        }
    }

    fun onTitleChange(v: String) { _title.value = v }
    fun onContentChange(v: String) { _content.value = v }
    fun onColorTagChange(v: String) { _colorTag.value = v }
    fun onReminderChange(v: Long) { _reminderTime.value = v }

    fun addImagePath(path: String) {
        _imagePaths.value = _imagePaths.value + path
        autoSave()
    }

    fun removeImagePath(index: Int) {
        _imagePaths.value = _imagePaths.value.toMutableList().also { it.removeAt(index) }
        autoSave()
    }

    fun toggleType() {
        _type.value = if (_type.value == "TEXT") "CHECKLIST" else "TEXT"
    }

    fun addChecklistItem() {
        val newItem = ChecklistItemEntity(
            noteId = currentNoteId.coerceAtLeast(0),
            text = "",
            isChecked = false,
            order = _checklistItems.value.size
        )
        _checklistItems.value = _checklistItems.value + newItem
    }

    fun updateChecklistItem(index: Int, text: String) {
        _checklistItems.value = _checklistItems.value.toMutableList().also {
            it[index] = it[index].copy(text = text)
        }
    }

    fun toggleChecklistItem(index: Int) {
        _checklistItems.value = _checklistItems.value.toMutableList().also {
            it[index] = it[index].copy(isChecked = !it[index].isChecked)
        }
    }

    fun removeChecklistItem(index: Int) {
        _checklistItems.value = _checklistItems.value.toMutableList().also { it.removeAt(index) }
    }

    fun shareNote(context: Context) {
        viewModelScope.launch {
            val plainText = _contentBlocks.value
                .filterIsInstance<ContentBlock.TextBlock>()
                .joinToString("\n") { it.text }

            val shareText = buildString {
                if (_title.value.isNotBlank()) {
                    append(_title.value)
                    append("\n\n")
                }
                if (_type.value == "CHECKLIST") {
                    _checklistItems.value.forEach { item ->
                        append(if (item.isChecked) "☑ " else "☐ ")
                        append(item.text)
                        append("\n")
                    }
                } else {
                    append(plainText)
                }
            }

            val imageUris = mutableListOf<android.net.Uri>()

            _imagePaths.value.forEach { path ->
                try {
                    val uri = if (path.startsWith("content://")) {
                        android.net.Uri.parse(path)
                    } else {
                        androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            java.io.File(path)
                        )
                    }
                    imageUris.add(uri)
                } catch (e: Exception) { }
            }

            _contentBlocks.value
                .filterIsInstance<ContentBlock.SketchBlock>()
                .forEach { block ->
                    if (block.imagePath.isNotBlank()) {
                        try {
                            val file = File(block.imagePath)
                            if (file.exists()) {
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                imageUris.add(uri)
                            }
                        } catch (e: Exception) { }
                    }
                }

            val intent = if (imageUris.isEmpty()) {
                android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, _title.value)
                }
            } else if (imageUris.size == 1) {
                android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, _title.value)
                    putExtra(android.content.Intent.EXTRA_STREAM, imageUris[0])
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } else {
                android.content.Intent(android.content.Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "image/*"
                    putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                    putExtra(android.content.Intent.EXTRA_SUBJECT, _title.value)
                    putParcelableArrayListExtra(
                        android.content.Intent.EXTRA_STREAM,
                        ArrayList(imageUris)
                    )
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

            context.startActivity(
                android.content.Intent.createChooser(intent, "Chia sẻ ghi chú")
            )
        }
    }

    fun addSketchBlockFromImage(imagePath: String, afterId: String? = null) {
        val newBlock = ContentBlock.SketchBlock(imagePath = imagePath)
        _contentBlocks.value = if (afterId == null) {
            _contentBlocks.value + newBlock
        } else {
            val list = _contentBlocks.value.toMutableList()
            val idx = list.indexOfFirst { it.id == afterId }
            if (idx >= 0) list.add(idx + 1, newBlock)
            list
        }
        autoSave()
    }

    fun updateSketchBlockImage(blockId: String, imagePath: String) {
        _contentBlocks.value = _contentBlocks.value.map { block ->
            if (block.id == blockId && block is ContentBlock.SketchBlock)
                block.copy(imagePath = imagePath)
            else block
        }
        autoSave()
    }

    fun getSketchImagePath(blockId: String): String {
        val block = _contentBlocks.value.find { it.id == blockId }
        return (block as? ContentBlock.SketchBlock)?.imagePath ?: ""
    }

    fun updateSketchHeight(blockId: String, newHeightDp: Int) {
        _contentBlocks.value = _contentBlocks.value.map { block ->
            if (block.id == blockId && block is ContentBlock.SketchBlock)
                block.copy(heightDp = newHeightDp)
            else block
        }
        autoSave()
    }
}