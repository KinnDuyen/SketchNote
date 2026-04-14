package com.example.sketchnote.ui.trash

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sketchnote.data.local.entity.NoteEntity
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val notes by viewModel.deletedNotes.collectAsStateWithLifecycle()
    var showConfirmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thùng rác", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    if (notes.isNotEmpty()) {
                        TextButton(onClick = { showConfirmDialog = true }) {
                            Text("Dọn sạch", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (notes.isEmpty()) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Thùng rác trống",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header thông báo tự xóa sau 30 ngày
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "⚠️ Ghi chú trong thùng rác sẽ tự động xóa sau 30 ngày",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                items(notes, key = { it.id }) { note ->
                    TrashNoteCard(
                        note = note,
                        onRestore = { viewModel.restore(note.id) },
                        onDelete = { viewModel.deletePermanently(note.id) }
                    )
                }
            }
        }
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("Dọn sạch thùng rác?") },
            text = { Text("Tất cả ghi chú sẽ bị xóa vĩnh viễn, không thể khôi phục.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.emptyTrash()
                    showConfirmDialog = false
                }) { Text("Xóa hết", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) { Text("Hủy") }
            }
        )
    }
}

@Composable
fun TrashNoteCard(note: NoteEntity, onRestore: () -> Unit, onDelete: () -> Unit) {
    // Tính ngày tự hủy = updatedAt + 30 ngày
    val deleteAt = note.updatedAt + 30L * 24 * 60 * 60 * 1000
    val daysLeft = ((deleteAt - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
    val deleteDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(deleteAt))

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (note.title.isNotBlank()) {
                Text(
                    note.title, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(4.dp))
            }
            if (note.content.isNotBlank()) {
                Text(
                    note.content, fontSize = 13.sp, maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(6.dp))

            // Hiển thị ngày tự hủy
            Text(
                text = if (daysLeft > 0) "🗑 Tự xóa sau $daysLeft ngày ($deleteDate)"
                else "🗑 Sẽ bị xóa hôm nay!",
                fontSize = 11.sp,
                color = if (daysLeft <= 3) MaterialTheme.colorScheme.error
                else Color(0xFF888888)
            )

            Spacer(Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth()
            ) {
                TextButton(onClick = onRestore) { Text("Khôi phục") }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onDelete) {
                    Text("Xóa vĩnh viễn", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}