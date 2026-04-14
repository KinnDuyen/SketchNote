package com.example.sketchnote.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.sketchnote.data.local.entity.NoteEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object ExportUtils {

    // ── Export ra ảnh JPG ─────────────────────────────────────────────
    fun exportAsImage(context: Context, note: NoteEntity): Uri? {
        val bitmap = createNoteBitmap(note)
        val fileName = "SmartNote_${System.currentTimeMillis()}.jpg"

        return try {
            val outputStream: OutputStream
            val uri: Uri

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ dùng MediaStore
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/SmartNote")
                }
                uri = context.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
                ) ?: return null
                outputStream = context.contentResolver.openOutputStream(uri) ?: return null
            } else {
                // Android 9 trở xuống
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES), "SmartNote"
                )
                dir.mkdirs()
                val file = File(dir, fileName)
                uri = FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file
                )
                outputStream = FileOutputStream(file)
            }

            bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
            outputStream.close()
            Toast.makeText(context, "Đã lưu ảnh vào Thư viện!", Toast.LENGTH_SHORT).show()
            uri
        } catch (e: Exception) {
            Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    // ── Export ra PDF ─────────────────────────────────────────────────
    fun exportAsPdf(context: Context, note: NoteEntity): Uri? {
        val fileName = "SmartNote_${System.currentTimeMillis()}.pdf"

        return try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            drawNoteOnCanvas(canvas, note)
            pdfDocument.finishPage(page)

            val outputStream: OutputStream
            val uri: Uri

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH,
                        Environment.DIRECTORY_DOWNLOADS + "/SmartNote")
                }
                uri = context.contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                ) ?: return null
                outputStream = context.contentResolver.openOutputStream(uri) ?: return null
            } else {
                val dir = File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS), "SmartNote"
                )
                dir.mkdirs()
                val file = File(dir, fileName)
                uri = FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file
                )
                outputStream = FileOutputStream(file)
            }

            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()
            Toast.makeText(context, "Đã lưu PDF vào Downloads!", Toast.LENGTH_SHORT).show()
            uri
        } catch (e: Exception) {
            Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    // ── Chia sẻ file qua Intent ───────────────────────────────────────
    fun shareFile(context: Context, uri: Uri, mimeType: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(
            Intent.createChooser(shareIntent, "Chia sẻ ghi chú qua...")
        )
    }

    // ── Tạo Bitmap từ nội dung ghi chú ───────────────────────────────
    private fun createNoteBitmap(note: NoteEntity): Bitmap {
        val width = 800
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var yPos = 60f

        // Tính chiều cao cần thiết
        val titleHeight = if (note.title.isNotBlank()) 80f else 0f
        val contentLines = note.content.split("\n").size
        val contentHeight = contentLines * 35f + 40f
        val totalHeight = (yPos + titleHeight + contentHeight + 60f).toInt()
            .coerceAtLeast(400)

        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Nền màu theo colorTag
        val bgColor = noteColorToArgb(note.colorTag)
        canvas.drawColor(bgColor)

        drawNoteOnCanvas(canvas, note, width.toFloat())
        return bitmap
    }

    private fun drawNoteOnCanvas(
        canvas: Canvas,
        note: NoteEntity,
        width: Float = 595f
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var yPos = 60f
        val padding = 40f

        // Tiêu đề
        if (note.title.isNotBlank()) {
            paint.apply {
                color = Color.BLACK
                textSize = 28f
                isFakeBoldText = true
            }
            canvas.drawText(note.title, padding, yPos, paint)
            yPos += 50f

            // Đường kẻ ngang dưới tiêu đề
            paint.apply {
                strokeWidth = 1f
                color = Color.GRAY
                style = Paint.Style.STROKE
            }
            canvas.drawLine(padding, yPos, width - padding, yPos, paint)
            yPos += 20f
        }

        // Nội dung
        paint.apply {
            color = Color.DKGRAY
            textSize = 18f
            isFakeBoldText = false
            style = Paint.Style.FILL
        }

        if (note.type == "CHECKLIST") {
            // Render checklist dạng text
            val items = note.content.split("\n").filter { it.isNotBlank() }
            items.forEach { item ->
                canvas.drawText("☐ $item", padding, yPos, paint)
                yPos += 35f
            }
        } else {
            note.content.split("\n").forEach { line ->
                canvas.drawText(line.ifBlank { " " }, padding, yPos, paint)
                yPos += 35f
            }
        }

        // Footer: tên app + ngày xuất
        paint.apply {
            color = Color.GRAY
            textSize = 14f
        }
        val dateStr = java.text.SimpleDateFormat(
            "dd/MM/yyyy HH:mm", java.util.Locale.getDefault()
        ).format(java.util.Date())
        canvas.drawText(
            "Smart Note • $dateStr",
            padding,
            canvas.height.toFloat() - 20f,
            paint
        )
    }

    private fun noteColorToArgb(colorTag: String): Int = when (colorTag) {
        "RED"    -> Color.parseColor("#FFCDD2")
        "ORANGE" -> Color.parseColor("#FFE0B2")
        "YELLOW" -> Color.parseColor("#FFF9C4")
        "GREEN"  -> Color.parseColor("#C8E6C9")
        "BLUE"   -> Color.parseColor("#BBDEFB")
        "PURPLE" -> Color.parseColor("#E1BEE7")
        else     -> Color.WHITE
    }
}