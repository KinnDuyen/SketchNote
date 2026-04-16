package com.example.sketchnote.ui.editor

import android.Manifest
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.sketchnote.R
import com.example.sketchnote.data.local.entity.NoteEntity
import com.example.sketchnote.utils.AutoTagUtils
import com.example.sketchnote.utils.ExportUtils
import com.example.sketchnote.utils.MathCalculator
import com.example.sketchnote.utils.OcrUtils
import com.example.sketchnote.utils.SpeechHelper
import java.io.File

// ── Màu helper ───────────────────────────────────────────────────────────────
fun tagToBoxColor(tag: String): Color = when (tag) {
    "RED"    -> Color(0xFFFCEAEA)
    "ORANGE" -> Color(0xFFFFF0E0)
    "PURPLE" -> Color(0xFFEEECFF)
    "GREEN"  -> Color(0xFFE8FAEC)
    "BLUE"   -> Color(0xFFE4F4FB)
    else     -> Color.White
}

fun tagToPillColor(tag: String): Color = when (tag) {
    "RED"    -> Color(0xFFDC9B9B)
    "ORANGE" -> Color(0xFFFF7444)
    "PURPLE" -> Color(0xFFB7BDF7)
    "GREEN"  -> Color(0xFFDAF9DE)
    "BLUE"   -> Color(0xFFCFECF3)
    else     -> Color.White
}

fun tagToBorderColor(tag: String): Color = when (tag) {
    "RED"    -> Color(0xFFB06060)
    "ORANGE" -> Color(0xFFCC4420)
    "PURPLE" -> Color(0xFF7A80D0)
    "GREEN"  -> Color(0xFF7AC880)
    "BLUE"   -> Color(0xFF7ABCCC)
    else     -> Color(0xFF999999)
}

@Composable
fun SketchNoteLogo(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.CenterStart) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(
                    color = Color(0xFFFCC701), fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp, fontStyle = FontStyle.Italic
                )) { append("Sketch") }
                withStyle(SpanStyle(
                    color = Color(0xFFFCC701), fontWeight = FontWeight.ExtraBold,
                    fontSize = 24.sp, fontStyle = FontStyle.Normal
                )) { append("Note,") }
            }
        )
    }
}

// ── VisualTransformation highlight ───────────────────────────────────────────
class HighlightVisualTransformation(
    private val highlights: List<HighlightSpan>
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val builder = AnnotatedString.Builder(text)
        highlights.forEach { h ->
            val start = h.start.coerceIn(0, text.length)
            val end = h.end.coerceIn(start, text.length)
            if (start < end) {
                builder.addStyle(
                    SpanStyle(background = h.color, fontWeight = FontWeight.SemiBold),
                    start, end
                )
            }
        }
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    noteId: Int,
    onBack: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    LaunchedEffect(noteId) { viewModel.loadNote(noteId) }

    val context       = LocalContext.current
    val title         by viewModel.title.collectAsStateWithLifecycle()
    val colorTag      by viewModel.colorTag.collectAsStateWithLifecycle()
    val imagePaths    by viewModel.imagePaths.collectAsStateWithLifecycle()
    val reminderTime  by viewModel.reminderTime.collectAsStateWithLifecycle()
    val type          by viewModel.type.collectAsStateWithLifecycle()
    val contentBlocks by viewModel.contentBlocks.collectAsStateWithLifecycle()

    var showHighlightPicker     by remember { mutableStateOf(false) }
    var currentHighlightBlockId by remember { mutableStateOf<String?>(null) }
    var currentSelectionRange   by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var showSketchDialog        by remember { mutableStateOf(false) }
    var editingSketchBlockId    by remember { mutableStateOf<String?>(null) }
    var activeTool              by remember { mutableStateOf<String?>(null) }
    var cameraUri               by remember { mutableStateOf<Uri?>(null) }
    var showExportMenu          by remember { mutableStateOf(false) }
    val speechHelper            = remember { SpeechHelper(context) }
    var isListening             by remember { mutableStateOf(false) }

    var suggestedTag by remember { mutableStateOf<String?>(null) }
    val allText = contentBlocks.filterIsInstance<ContentBlock.TextBlock>()
        .joinToString(" ") { it.text }
    LaunchedEffect(title, allText) {
        kotlinx.coroutines.delay(800)
        val tag = AutoTagUtils.suggestTag(title, allText)
        if (tag != null && tag != colorTag) suggestedTag = tag
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) cameraUri?.let { viewModel.addImagePath(it.toString()) }
        activeTool = null
    }

    val cameraPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                    ?: context.filesDir
                dir.mkdirs()
                val file = File(dir, "photo_${System.currentTimeMillis()}.jpg")
                val uri = FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file
                )
                cameraUri = uri
                cameraLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Không thể mở camera", Toast.LENGTH_SHORT).show()
                activeTool = null
            }
        } else {
            Toast.makeText(context, "Cần quyền camera", Toast.LENGTH_SHORT).show()
            activeTool = null
        }
    }

    val micPermLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isListening = true; activeTool = "mic"
            speechHelper.startListening(
                onResult = { text ->
                    val id = contentBlocks.filterIsInstance<ContentBlock.TextBlock>()
                        .lastOrNull()?.id
                    if (id != null) {
                        val cur = (contentBlocks.find { it.id == id }
                                as? ContentBlock.TextBlock)?.text ?: ""
                        viewModel.updateTextBlock(id, "$cur $text")
                    }
                    isListening = false; activeTool = null
                },
                onError = { isListening = false; activeTool = null }
            )
        }
    }

    val ocrLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            OcrUtils.recognizeText(context = context, imageUri = it,
                onSuccess = { text ->
                    val id = contentBlocks.filterIsInstance<ContentBlock.TextBlock>()
                        .lastOrNull()?.id
                    if (id != null) {
                        val cur = (contentBlocks.find { it.id == id }
                                as? ContentBlock.TextBlock)?.text ?: ""
                        viewModel.updateTextBlock(id, "$cur\n$text")
                    }
                },
                onError = { msg -> Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
            )
        }
        activeTool = null
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris -> uris.forEach { viewModel.addImagePath(it.toString()) }; activeTool = null }

    Scaffold(containerColor = Color(0xFFFBFCF7)) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Top bar ───────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 18.dp).padding(top = 10.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SketchNoteLogo(modifier = Modifier.height(46.dp))
                IconButton(
                    onClick = { viewModel.shareNote(context) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(painter = painterResource(R.drawable.share), contentDescription = "Chia sẻ",
                        tint = Color.Unspecified, modifier = Modifier.size(24.dp))
                }
            }

            // ── Back + Action bar ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Mũi tên tách riêng — không có box
                IconButton(
                    onClick = { viewModel.saveNote(context, onBack) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(painter = painterResource(R.drawable.arrow), contentDescription = "Quay lại",
                        tint = Color.Unspecified, modifier = Modifier.size(22.dp))
                }

                Surface(
                    modifier = Modifier.weight(1f)
                        .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp)),
                    color = Color.White, shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ActionToolButton(R.drawable.mic, "Ghi âm", activeTool == "mic") {
                            if (isListening) {
                                speechHelper.stopListening(); isListening = false; activeTool = null
                            } else { activeTool = "mic"; micPermLauncher.launch(Manifest.permission.RECORD_AUDIO) }
                        }
                        ActionToolButton(R.drawable.pen, "Vẽ", activeTool == "sketch") {
                            activeTool = null; editingSketchBlockId = null; showSketchDialog = true
                        }
                        Box(contentAlignment = Alignment.Center) {
                            ActionToolButton(R.drawable.send, "Xuất",
                                activeTool == "export" || showExportMenu) {
                                activeTool = "export"; showExportMenu = true
                            }
                            DropdownMenu(expanded = showExportMenu,
                                onDismissRequest = { showExportMenu = false; activeTool = null }) {
                                DropdownMenuItem(
                                    text = { Text("Xuất ảnh JPG") },
                                    leadingIcon = { Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showExportMenu = false; activeTool = null
                                        viewModel.saveNote(context) {
                                            val note = NoteEntity(
                                                id = noteId.coerceAtLeast(0), title = title,
                                                content = contentBlocks.filterIsInstance<ContentBlock.TextBlock>()
                                                    .joinToString("\n") { it.text },
                                                type = type, colorTag = colorTag,
                                                imagePaths = imagePaths.joinToString("||"),
                                                reminderTime = reminderTime
                                            )
                                            ExportUtils.exportAsImage(context, note)
                                                ?.let { ExportUtils.shareFile(context, it, "image/jpeg") }
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Xuất PDF") },
                                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        showExportMenu = false; activeTool = null
                                        viewModel.saveNote(context) {
                                            val note = NoteEntity(
                                                id = noteId.coerceAtLeast(0), title = title,
                                                content = contentBlocks.filterIsInstance<ContentBlock.TextBlock>()
                                                    .joinToString("\n") { it.text },
                                                type = type, colorTag = colorTag,
                                                imagePaths = imagePaths.joinToString("||"),
                                                reminderTime = reminderTime
                                            )
                                            ExportUtils.exportAsPdf(context, note)
                                                ?.let { ExportUtils.shareFile(context, it, "application/pdf") }
                                        }
                                    }
                                )
                            }
                        }
                        ActionToolButton(R.drawable.scan, "Quét chữ", activeTool == "ocr") {
                            activeTool = "ocr"; ocrLauncher.launch("image/*")
                        }
                        ActionToolButton(R.drawable.cam, "Chụp ảnh", activeTool == "cam") {
                            activeTool = "cam"; cameraPermLauncher.launch(Manifest.permission.CAMERA)
                        }
                        ActionToolButton(R.drawable.image, "Thêm ảnh", activeTool == "gallery") {
                            activeTool = "gallery"; galleryLauncher.launch("image/*")
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                item { ColorTagPicker(selected = colorTag, onSelect = viewModel::onColorTagChange) }
                item {
                    ReminderSection(
                        reminderTime = reminderTime,
                        onSetReminder = { viewModel.onReminderChange(it) },
                        onClearReminder = { viewModel.onReminderChange(0L) }
                    )
                }

                if (suggestedTag != null && suggestedTag != colorTag) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("Gợi ý nhãn: $suggestedTag", fontSize = 13.sp,
                                    modifier = Modifier.weight(1f),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer)
                                TextButton(onClick = {
                                    viewModel.onColorTagChange(suggestedTag!!); suggestedTag = null
                                }) { Text("Áp dụng") }
                                TextButton(onClick = { suggestedTag = null }) { Text("Bỏ qua") }
                            }
                        }
                    }
                }

                item {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }

                item {
                    TextField(
                        value = title, onValueChange = viewModel::onTitleChange,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        placeholder = {
                            Text("Tiêu đề", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                        },
                        textStyle = LocalTextStyle.current.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                        colors = TextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent
                        )
                    )
                }

                if (imagePaths.isNotEmpty()) {
                    item {
                        Text("Ảnh đính kèm", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, top = 4.dp, bottom = 2.dp))
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp)) {
                            itemsIndexed(imagePaths) { index, path ->
                                Box(modifier = Modifier.size(100.dp)) {
                                    AsyncImage(
                                        model = if (path.startsWith("content://")) Uri.parse(path)
                                        else File(path),
                                        contentDescription = null, contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                                    )
                                    IconButton(
                                        onClick = { viewModel.removeImagePath(index) },
                                        modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = Color.White,
                                            modifier = Modifier.size(12.dp))
                                    }
                                }
                            }
                        }
                    }
                    item {
                        HorizontalDivider(modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }

                item {
                    val boxColor = tagToBoxColor(colorTag)
                    Surface(
                        modifier = Modifier.fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp)),
                        color = boxColor, shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(modifier = Modifier.padding(4.dp)) {
                            contentBlocks.forEach { block ->
                                when (block) {
                                    is ContentBlock.TextBlock -> {
                                        TextBlockItem(
                                            block = block,
                                            onTextChange = { newText ->
                                                val result = MathCalculator.detectAndCalculate(newText)
                                                val finalText = if (result != null) {
                                                    "${newText.trimEnd().trimEnd('=')}= $result"
                                                } else newText
                                                viewModel.updateTextBlock(block.id, finalText)
                                            },
                                            onDelete = { viewModel.removeBlock(block.id) },
                                            showDelete = contentBlocks.size > 1,
                                            highlights = viewModel.getHighlightsForBlock(block.id),
                                            onShowHighlightPicker = { blockId, start, end ->
                                                currentHighlightBlockId = blockId
                                                currentSelectionRange = start to end
                                                showHighlightPicker = true
                                            }
                                        )
                                    }
                                    is ContentBlock.SketchBlock -> {
                                        // YC2 FIX: SketchBlockItem giờ hiển thị ảnh thật
                                        SketchBlockItem(
                                            block = block,
                                            onEdit = {
                                                editingSketchBlockId = block.id
                                                showSketchDialog = true
                                            },
                                            onDelete = { viewModel.removeBlock(block.id) }
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.addTextBlockAfter() },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Thêm văn bản", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { editingSketchBlockId = null; showSketchDialog = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Thêm bản vẽ", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    // ── Highlight Picker Dialog ───────────────────────────────────────────────
    if (showHighlightPicker) {
        AlertDialog(
            onDismissRequest = { showHighlightPicker = false },
            title = { Text("Chọn màu highlight") },
            text = {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf(
                        Color(0xFFFFFF00), Color(0xFFA5D6FF), Color(0xFFCE93D8),
                        Color(0xFFFFAB91), Color(0xFFA5D6A5)
                    ).forEach { color ->
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable {
                                    currentHighlightBlockId?.let { blockId ->
                                        currentSelectionRange?.let { (start, end) ->
                                            if (start < end)
                                                viewModel.addHighlight(blockId, start, end, color)
                                        }
                                    }
                                    showHighlightPicker = false
                                    currentHighlightBlockId = null
                                    currentSelectionRange = null
                                    Toast.makeText(context, "Đã thêm highlight", Toast.LENGTH_SHORT).show()
                                }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHighlightPicker = false }) { Text("Hủy") }
            }
        )
    }

    // ── Sketch Dialog — mở SketchScreen dạng dialog toàn màn hình ────────────
    if (showSketchDialog) {
        Dialog(
            onDismissRequest = { showSketchDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFBFCF7))) {
                SketchScreen(
                    onBack = { showSketchDialog = false },
                    onSave = { imagePath ->
                        if (imagePath.isNotBlank()) {
                            if (editingSketchBlockId != null)
                                viewModel.updateSketchBlockImage(editingSketchBlockId!!, imagePath)
                            else
                                viewModel.addSketchBlockFromImage(imagePath)
                        }
                        showSketchDialog = false
                    }
                )
            }
        }
    }
}

// ── ActionToolButton — vòng tròn vàng khi active ─────────────────────────────
@Composable
private fun ActionToolButton(
    drawableRes: Int,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .then(if (isActive)
                    Modifier.background(Color(0xFFFCC701).copy(alpha = 0.15f), CircleShape)
                else Modifier)
                .clip(CircleShape)
                .clickable(onClick = onClick)
        ) {
            Icon(painter = painterResource(drawableRes), contentDescription = label,
                tint = Color.Unspecified, modifier = Modifier.size(22.dp))
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(5.dp).clip(CircleShape)
                .background(if (isActive) Color(0xFFFCC701) else Color.Transparent)
        )
    }
}

// ── ColorTagPicker ────────────────────────────────────────────────────────────
@Composable
fun ColorTagPicker(selected: String, onSelect: (String) -> Unit) {
    val tags = listOf("NONE", "RED", "ORANGE", "PURPLE", "GREEN", "BLUE")
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        itemsIndexed(tags) { _, tag ->
            val sel = selected == tag
            Box(
                modifier = Modifier
                    .height(26.dp).width(58.dp)
                    .shadow(elevation = if (sel) 0.dp else 3.dp, shape = RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(tagToPillColor(tag))
                    .border(
                        width = if (sel) 2.dp else 0.dp,
                        color = if (sel) tagToBorderColor(tag) else Color.Transparent,
                        shape = RoundedCornerShape(20.dp)
                    )
                    .clickable { onSelect(tag) }
            )
        }
    }
}

// ── ReminderSection ───────────────────────────────────────────────────────────
@Composable
fun ReminderSection(
    reminderTime: Long,
    onSetReminder: (Long) -> Unit,
    onClearReminder: () -> Unit
) {
    val context = LocalContext.current
    val hasReminder = reminderTime > System.currentTimeMillis()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            modifier = Modifier.size(48.dp)
                .shadow(elevation = 3.dp, shape = RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp), color = Color.White
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(painter = painterResource(R.drawable.ring), contentDescription = null,
                    tint = Color.Unspecified, modifier = Modifier.size(24.dp))
            }
        }
        Surface(
            modifier = Modifier.height(48.dp)
                .shadow(elevation = 3.dp, shape = RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp), color = Color.White
        ) {
            if (hasReminder) {
                Row(modifier = Modifier.padding(horizontal = 14.dp).fillMaxHeight(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        java.text.SimpleDateFormat("dd/MM HH:mm", java.util.Locale.getDefault())
                            .format(java.util.Date(reminderTime)),
                        fontSize = 13.sp, color = Color(0xFFE8B800),
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = onClearReminder, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                    }
                }
            } else {
                TextButton(
                    onClick = {
                        val cal = java.util.Calendar.getInstance()
                        android.app.DatePickerDialog(context, { _, y, m, d ->
                            android.app.TimePickerDialog(context, { _, h, min ->
                                cal.set(y, m, d, h, min, 0); onSetReminder(cal.timeInMillis)
                            }, cal.get(java.util.Calendar.HOUR_OF_DAY),
                                cal.get(java.util.Calendar.MINUTE), true).show()
                        }, cal.get(java.util.Calendar.YEAR),
                            cal.get(java.util.Calendar.MONTH),
                            cal.get(java.util.Calendar.DAY_OF_MONTH)).show()
                    },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Add, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                        Text("Nhắc nhở", fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF333333))
                    }
                }
            }
        }
    }
}

// ── TextBlockItem ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TextBlockItem(
    block: ContentBlock.TextBlock,
    onTextChange: (String) -> Unit,
    onDelete: () -> Unit,
    showDelete: Boolean,
    highlights: List<HighlightSpan>,
    onShowHighlightPicker: (blockId: String, start: Int, end: Int) -> Unit
) {
    var textFieldValue by remember(block.id) {
        mutableStateOf(TextFieldValue(block.text))
    }
    LaunchedEffect(block.text) {
        if (textFieldValue.text != block.text) {
            textFieldValue = TextFieldValue(block.text)
        }
    }
    val context = LocalContext.current

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        BasicTextField(
            value = textFieldValue,
            onValueChange = { newValue ->
                textFieldValue = newValue
                onTextChange(newValue.text)
            },
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 80.dp)
                .padding(4.dp),
            textStyle = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, color = Color.Black),
            cursorBrush = SolidColor(Color(0xFFFCC701)),
            visualTransformation = if (highlights.isEmpty()) VisualTransformation.None
            else HighlightVisualTransformation(highlights),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp)) {
                    if (textFieldValue.text.isEmpty()) {
                        Text("Nội dung ghi chú...", fontSize = 16.sp,
                            color = Color(0xFF333333).copy(alpha = 0.4f),
                            fontWeight = FontWeight.SemiBold)
                    }
                    innerTextField()
                }
            }
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(
                onClick = {
                    val selection = textFieldValue.selection
                    val start = selection.min
                    val end = selection.max
                    if (start < end) {
                        onShowHighlightPicker(block.id, start, end)
                    } else {
                        Toast.makeText(context, "Hãy bôi đen văn bản trước", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Brush, contentDescription = "Highlight",
                    tint = Color(0xFFFCC701), modifier = Modifier.size(20.dp))
            }
            if (showDelete) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp),
                        tint = Color.Gray.copy(alpha = 0.6f))
                }
            }
        }
    }
}

// ── YC2 FIX: SketchBlockItem — hiển thị ảnh thật thay vì chỉ text ────────────
@Composable
private fun SketchBlockItem(
    block: ContentBlock.SketchBlock,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        if (block.imagePath.isNotBlank()) {
            // Có ảnh → hiển thị ảnh thật
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = File(block.imagePath),
                    contentDescription = "Bản vẽ",
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onEdit() }
                )
                // Overlay buttons góc trên phải
                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 4.dp)
                ) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, "Sửa", tint = Color.White,
                            modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, "Xóa", tint = Color(0xFFFF6B6B),
                            modifier = Modifier.size(16.dp))
                    }
                }
            }
        } else {
            // Chưa có ảnh → hiển thị placeholder có thể nhấn để vẽ
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5))
                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                    .clickable { onEdit() },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Edit, null, tint = Color(0xFFFCC701),
                        modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("Nhấn để vẽ", fontSize = 14.sp, color = Color.Gray)
                }
                // Nút xóa góc trên phải
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
                ) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(14.dp),
                        tint = Color.Gray)
                }
            }
        }
    }
}