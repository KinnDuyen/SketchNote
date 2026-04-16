package com.example.sketchnote.ui.editor

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.sketchnote.R
import com.github.skydoves.colorpicker.compose.*
import java.io.File
import java.io.FileOutputStream

val SketchYellow = Color(0xFFFCC701)
val SketchBg = Color(0xFFFBFCF7)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SketchScreen(
    onBack: () -> Unit,
    onSave: (String) -> Unit
) {
    val context = LocalContext.current
    var sketchViewRef        by remember { mutableStateOf<SketchView?>(null) }
    var selectedColor        by remember { mutableStateOf(Color.Black) }
    var strokeWidth          by remember { mutableFloatStateOf(15f) }
    var selectedBrush        by remember { mutableStateOf(BrushType.PENCIL) }
    var isRulerMode          by remember { mutableStateOf(false) }
    var showColorPickerDialog by remember { mutableStateOf(false) }
    var canUndo              by remember { mutableStateOf(false) }
    var canRedo              by remember { mutableStateOf(false) }

    fun refreshUndoRedo() {
        canUndo = sketchViewRef?.canUndo() ?: false
        canRedo = sketchViewRef?.canRedo() ?: false
    }

    fun saveBitmapAndReturn(bitmap: Bitmap) {
        try {
            val sketchDir = File(context.filesDir, "sketches")
            if (!sketchDir.exists()) sketchDir.mkdirs()
            val file = File(sketchDir, "sketch_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            onSave(file.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            onSave("")
        }
        onBack()
    }

    val paletteColors = listOf(
        Color.White,
        Color(0xFF606060),
        Color(0xFFABE2FF),
        Color(0xFF9977CC),
        Color(0xFFEC9CCC),
        Color(0xFF97CD97),
        Color(0xFFFF5F2D),
        Color(0xFFEDB55E)
    )

    val imagePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val bitmap = if (Build.VERSION.SDK_INT >= 28) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, it))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, it)
            }
            sketchViewRef?.setBackgroundImage(bitmap)
        }
    }

    val cameraUri = remember { mutableStateOf<android.net.Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri.value?.let { uri ->
                val bitmap = if (Build.VERSION.SDK_INT >= 28) {
                    ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(context.contentResolver, uri)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                sketchViewRef?.setBackgroundImage(bitmap)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SketchBg)
    ) {

        // ── YC1: Logo dùng Text thuần, to hơn ────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // YC1: Text logo thay Image
            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(
                        color = Color(0xFFFCC701),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        fontStyle = FontStyle.Italic
                    )) { append("Sketch") }
                    withStyle(SpanStyle(
                        color = Color(0xFFFCC701),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        fontStyle = FontStyle.Normal
                    )) { append("Note,") }
                }
            )

            IconButton(
                onClick = {
                    sketchViewRef?.getBitmap()?.let { saveBitmapAndReturn(it) }
                },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.share),
                    contentDescription = "Lưu & Chia sẻ",
                    tint = Color(0xFF333333),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // ── YC2: Back tách riêng, action box chứa 6 icon còn lại ─────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Mũi tên nằm NGOÀI box
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.arrow),
                    contentDescription = "Quay lại",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Action box chỉ chứa 6 icon còn lại
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp)),
                color = Color.White,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Mic (placeholder)
                    ActionIconBtn(
                        drawableRes = R.drawable.mic,
                        desc = "Ghi âm",
                        onClick = {},
                        isSelected = false
                    )

                    // Pen / Bút
                    ActionIconBtn(
                        drawableRes = R.drawable.pen,
                        desc = "Bút",
                        onClick = {
                            selectedBrush = BrushType.PENCIL
                            isRulerMode = false
                            sketchViewRef?.isRulerMode = false
                        },
                        isSelected = selectedBrush == BrushType.PENCIL && !isRulerMode
                    )

                    // Lưu
                    ActionIconBtn(
                        drawableRes = R.drawable.send,
                        desc = "Lưu",
                        onClick = {
                            sketchViewRef?.getBitmap()?.let { saveBitmapAndReturn(it) }
                        }
                    )

                    // Scan
                    ActionIconBtn(
                        drawableRes = R.drawable.scan,
                        desc = "Quét",
                        onClick = {}
                    )

                    // Camera
                    ActionIconBtn(
                        drawableRes = R.drawable.cam,
                        desc = "Chụp ảnh",
                        onClick = {
                            val file = File(
                                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES),
                                "sketch_bg_${System.currentTimeMillis()}.jpg"
                            )
                            val uri = FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                file
                            )
                            cameraUri.value = uri
                            cameraLauncher.launch(uri)
                        }
                    )

                    // Gallery
                    ActionIconBtn(
                        drawableRes = R.drawable.image,
                        desc = "Chọn ảnh",
                        onClick = { imagePickerLauncher.launch("image/*") }
                    )
                }
            }
        }

        // ── Vùng vẽ chính ─────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
            ) {
                AndroidView(
                    factory = { ctx ->
                        SketchView(ctx).also { view ->
                            view.onDrawChanged = { refreshUndoRedo() }
                            sketchViewRef = view
                        }
                    },
                    update = { view ->
                        view.strokeColor = if (selectedBrush == BrushType.ERASER)
                            Color.White.toArgb()
                        else selectedColor.toArgb()
                        view.strokeWidth = strokeWidth
                        view.brushType = selectedBrush
                        view.isEnabled = true
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // ── YC3: FAB Palette đổi màu theo selectedColor ─────────────────
            val fabBg = if (selectedBrush == BrushType.ERASER) Color(0xFF222222)
            else selectedColor
            // Icon màu tương phản với nền FAB
            val fabIconTint = if (fabBg.luminance() > 0.5f) Color(0xFF222222) else Color.White

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(fabBg)
                    .then(
                        if (fabBg == Color.White || fabBg.luminance() > 0.85f)
                            Modifier.border(1.dp, Color(0xFFDDDDDD), CircleShape)
                        else Modifier
                    )
                    .clickable { showColorPickerDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Palette,
                    contentDescription = "Bảng màu",
                    tint = fabIconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // ── Bottom panel ───────────────────────────────────────────────────────
        Surface(color = SketchBg, tonalElevation = 0.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Size slider
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Size:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF222222),
                        modifier = Modifier.width(38.dp)
                    )
                    Slider(
                        value = strokeWidth,
                        onValueChange = { strokeWidth = it },
                        valueRange = 2f..60f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = SketchYellow,
                            activeTrackColor = SketchYellow,
                            inactiveTrackColor = Color(0xFFDDDDDD)
                        )
                    )
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size((strokeWidth / 3f).coerceIn(4f, 22f).dp)
                            .clip(CircleShape)
                            .background(
                                if (selectedBrush == BrushType.ERASER) Color.LightGray
                                else selectedColor
                            )
                            .then(
                                if (selectedColor == Color.White || selectedBrush == BrushType.ERASER)
                                    Modifier.border(1.dp, Color(0xFFDDDDDD), CircleShape)
                                else Modifier
                            )
                    )
                }

                // Brush selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BrushIconBtn(
                        drawableRes = R.drawable.pen1, desc = "Bút chì",
                        isSelected = selectedBrush == BrushType.PENCIL && !isRulerMode,
                        selectedColor = selectedColor
                    ) { selectedBrush = BrushType.PENCIL; isRulerMode = false; sketchViewRef?.isRulerMode = false }

                    BrushIconBtn(
                        drawableRes = R.drawable.pen2, desc = "Marker",
                        isSelected = selectedBrush == BrushType.MARKER && !isRulerMode,
                        selectedColor = selectedColor
                    ) { selectedBrush = BrushType.MARKER; isRulerMode = false; sketchViewRef?.isRulerMode = false }

                    BrushIconBtn(
                        drawableRes = R.drawable.pen3, desc = "Dạ quang",
                        isSelected = selectedBrush == BrushType.HIGHLIGHTER && !isRulerMode,
                        selectedColor = selectedColor
                    ) { selectedBrush = BrushType.HIGHLIGHTER; isRulerMode = false; sketchViewRef?.isRulerMode = false }

                    BrushIconBtn(
                        drawableRes = R.drawable.pen4, desc = "Bút sáp",
                        isSelected = selectedBrush == BrushType.CRAYON && !isRulerMode,
                        selectedColor = selectedColor
                    ) { selectedBrush = BrushType.CRAYON; isRulerMode = false; sketchViewRef?.isRulerMode = false }

                    BrushIconBtn(
                        drawableRes = R.drawable.eraser, desc = "Tẩy",
                        isSelected = selectedBrush == BrushType.ERASER,
                        selectedColor = Color(0xFFE53935)
                    ) { selectedBrush = BrushType.ERASER; isRulerMode = false; sketchViewRef?.isRulerMode = false }

                    BrushIconBtn(
                        drawableRes = R.drawable.ruler, desc = "Thước kẻ",
                        isSelected = isRulerMode,
                        selectedColor = selectedColor
                    ) { isRulerMode = !isRulerMode; sketchViewRef?.toggleRuler() }
                }

                // Color palette
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    paletteColors.forEach { color ->
                        val isSelected = selectedColor == color && selectedBrush != BrushType.ERASER
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 36.dp else 32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .then(
                                        if (color == Color.White)
                                            Modifier.border(1.dp, Color(0xFFDDDDDD), CircleShape)
                                        else Modifier
                                    )
                                    .then(
                                        if (isSelected)
                                            Modifier.border(2.5.dp, SketchYellow, CircleShape)
                                        else Modifier
                                    )
                                    .clickable {
                                        selectedColor = color
                                        if (selectedBrush == BrushType.ERASER) {
                                            selectedBrush = BrushType.PENCIL
                                        }
                                    }
                            )
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(SketchYellow)
                                )
                            } else {
                                Spacer(modifier = Modifier.size(5.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }

    // ── Color Picker Dialog ────────────────────────────────────────────────────
    if (showColorPickerDialog) {
        val controller = rememberColorPickerController()
        Dialog(onDismissRequest = { showColorPickerDialog = false }) {
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.padding(8.dp)) {
                Column(
                    Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        "Chọn màu tuỳ chỉnh",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    HsvColorPicker(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp),
                        controller = controller,
                        initialColor = selectedColor,
                        onColorChanged = { selectedColor = it.color }
                    )
                    BrightnessSlider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(36.dp),
                        controller = controller
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(selectedColor)
                            .then(
                                if (selectedColor == Color.White)
                                    Modifier.border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(10.dp))
                                else Modifier
                            )
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showColorPickerDialog = false },
                            modifier = Modifier.weight(1f)
                        ) { Text("Hủy") }
                        Button(
                            onClick = {
                                if (selectedBrush == BrushType.ERASER) {
                                    selectedBrush = BrushType.PENCIL
                                }
                                showColorPickerDialog = false
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = SketchYellow)
                        ) { Text("Áp dụng", color = Color.Black) }
                    }
                }
            }
        }
    }
}

// ── ActionIconBtn ─────────────────────────────────────────────────────────────
@Composable
private fun ActionIconBtn(
    drawableRes: Int,
    desc: String,
    onClick: () -> Unit,
    isSelected: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) SketchYellow.copy(alpha = 0.9f) else Color.Transparent
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = drawableRes),
                contentDescription = desc,
                tint = if (isSelected) Color.White else Color.Unspecified,
                modifier = Modifier.size(20.dp)
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(SketchYellow)
            )
        } else {
            Spacer(modifier = Modifier.size(5.dp))
        }
    }
}

// ── BrushIconBtn ──────────────────────────────────────────────────────────────
@Composable
private fun BrushIconBtn(
    drawableRes: Int,
    desc: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(if (isSelected) SketchYellow else Color.White)
                .border(
                    width = if (isSelected) 0.dp else 1.dp,
                    color = Color(0xFFE0E0E0),
                    shape = CircleShape
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = drawableRes),
                contentDescription = desc,
                tint = if (isSelected) Color.White else Color(0xFF333333),
                modifier = Modifier.size(22.dp)
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(SketchYellow)
            )
        } else {
            Spacer(modifier = Modifier.size(6.dp))
        }
    }
}