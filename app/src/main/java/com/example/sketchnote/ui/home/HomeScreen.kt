package com.example.sketchnote.ui.home

import android.Manifest
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sketchnote.data.local.entity.NoteEntity
import com.example.sketchnote.util.VoiceState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState

// --- Các hàm hỗ trợ màu sắc giữ nguyên ---
fun colorTagToColor(tag: String): Color = when (tag) {
    "RED"    -> Color(0xFFFFCDD2)
    "ORANGE" -> Color(0xFFFFE0B2)
    "YELLOW" -> Color(0xFFFFF9C4)
    "GREEN"  -> Color(0xFFC8E6C9)
    "BLUE"   -> Color(0xFFBBDEFB)
    "PURPLE" -> Color(0xFFE1BEE7)
    else     -> Color(0xFFF5F5F5)
}

fun colorTagToDark(tag: String): Color = when (tag) {
    "RED"    -> Color(0xFFE57373)
    "ORANGE" -> Color(0xFFFFB74D)
    "YELLOW" -> Color(0xFFFFF176)
    "GREEN"  -> Color(0xFF81C784)
    "BLUE"   -> Color(0xFF64B5F6)
    "PURPLE" -> Color(0xFFBA68C8)
    else     -> Color(0xFFBDBDBD)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    onNoteClick: (Int) -> Unit,
    onCreateNote: () -> Unit,
    onTrashClick: () -> Unit,
    onBackupClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val colorFilter by viewModel.colorFilter.collectAsStateWithLifecycle()
    val voiceState by viewModel.voiceState.collectAsStateWithLifecycle()

    // Permission mic
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    // Init voice manager
    LaunchedEffect(Unit) { viewModel.initVoice(context) }

    // Animation icon mic khi đang nghe
    val micScale by rememberInfiniteTransition(label = "mic").animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "micScale"
    )

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateNote,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tạo ghi chú")
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("SketchNote", fontWeight = FontWeight.Bold, fontSize = 22.sp) },
                actions = {
                    IconButton(onClick = onBackupClick) {
                        Icon(Icons.Default.Backup, contentDescription = "Sao lưu")
                    }
                    IconButton(onClick = onTrashClick) {
                        Icon(Icons.Default.Delete, contentDescription = "Thùng rác")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Thanh tìm kiếm + nút mic
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Tìm kiếm ghi chú...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp)
                )

                Spacer(Modifier.width(8.dp))

                // Nút mic
                IconButton(
                    onClick = {
                        if (voiceState == VoiceState.LISTENING) {
                            viewModel.stopVoiceSearch()
                        } else {
                            if (micPermission.status.isGranted) {
                                viewModel.startVoiceSearch()
                            } else {
                                micPermission.launchPermissionRequest()
                            }
                        }
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (voiceState == VoiceState.LISTENING)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        )
                ) {
                    Icon(
                        imageVector = if (voiceState == VoiceState.LISTENING)
                            Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Tìm kiếm giọng nói",
                        modifier = if (voiceState == VoiceState.LISTENING)
                            Modifier.scale(micScale) else Modifier,
                        tint = if (voiceState == VoiceState.LISTENING)
                            Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Chip lọc màu
            ColorFilterRow(selected = colorFilter, onSelect = viewModel::onColorFilterChange)

            // Hiển thị trạng thái đang nghe
            if (voiceState == VoiceState.LISTENING) {
                Text(
                    "Đang nghe...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Danh sách ghi chú
            if (notes.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "Chưa có ghi chú nào.\nNhấn + để tạo mới!",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalItemSpacing = 10.dp
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { onNoteClick(note.id) },
                            onDelete = { viewModel.moveToTrash(note.id) },
                            onTogglePin = { viewModel.togglePin(note) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit
) {
    val bgColor = colorTagToColor(note.colorTag)
    val borderColor = colorTagToDark(note.colorTag)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.5.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (note.title.isNotBlank()) {
                    Text(
                        note.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                IconButton(onClick = onTogglePin, modifier = Modifier.size(24.dp)) {
                    Icon(
                        imageVector = if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                        contentDescription = if (note.isPinned) "Bỏ ghim" else "Ghim",
                        modifier = Modifier.size(18.dp),
                        tint = if (note.isPinned) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (note.title.isNotBlank()) Spacer(Modifier.height(4.dp))

            if (note.content.isNotBlank()) {
                Text(
                    note.content,
                    fontSize = 13.sp,
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (note.type == "CHECKLIST") "☑ Checklist" else "📝 Text",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Xóa",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun ColorFilterRow(selected: ColorFilter, onSelect: (ColorFilter) -> Unit) {
    val colorOptions = listOf(
        ColorFilter.ALL    to Color(0xFFBDBDBD),
        ColorFilter.RED    to Color(0xFFEF9A9A),
        ColorFilter.ORANGE to Color(0xFFFFCC80),
        ColorFilter.YELLOW to Color(0xFFFFF176),
        ColorFilter.GREEN  to Color(0xFFA5D6A7),
        ColorFilter.BLUE   to Color(0xFF90CAF9),
        ColorFilter.PURPLE to Color(0xFFCE93D8)
    )

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        items(colorOptions.size) { i ->
            val (filter, color) = colorOptions[i]
            val isSelected = selected == filter

            val borderColor = when (filter) {
                ColorFilter.ALL    -> Color(0xFF757575)
                ColorFilter.RED    -> Color(0xFFE53935)
                ColorFilter.ORANGE -> Color(0xFFFB8C00)
                ColorFilter.YELLOW -> Color(0xFFFDD835)
                ColorFilter.GREEN  -> Color(0xFF43A047)
                ColorFilter.BLUE   -> Color(0xFF1E88E5)
                ColorFilter.PURPLE -> Color(0xFF8E24AA)
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .then(
                        if (isSelected) Modifier.border(2.dp, borderColor, CircleShape)
                        else Modifier
                    )
                    .padding(4.dp)
                    .clip(CircleShape)
                    .background(color)
                    .clickable { onSelect(filter) },
                contentAlignment = Alignment.Center
            ) {
                if (isSelected && filter == ColorFilter.ALL) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}