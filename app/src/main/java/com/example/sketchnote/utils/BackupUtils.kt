package com.example.sketchnote.utils

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.sketchnote.data.local.entity.ChecklistItemEntity
import com.example.sketchnote.data.local.entity.NoteEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object BackupUtils {

    // ── Xuất toàn bộ ghi chú ra file JSON ────────────────────────────
    fun exportBackup(
        context: Context,
        notes: List<NoteEntity>,
        checklistItems: List<ChecklistItemEntity>
    ): Uri? {
        return try {
            val json = JSONObject().apply {
                put("version", 1)
                put("exportedAt", System.currentTimeMillis())

                // Danh sách ghi chú
                put("notes", JSONArray().apply {
                    notes.forEach { note ->
                        put(JSONObject().apply {
                            put("id", note.id)
                            put("title", note.title)
                            put("content", note.content)
                            put("type", note.type)
                            put("colorTag", note.colorTag)
                            put("imagePaths", note.imagePaths)
                            put("sketchPath", note.sketchPath)
                            put("reminderTime", note.reminderTime)
                            put("isDeleted", note.isDeleted)
                            put("isPinned", note.isPinned)
                            put("createdAt", note.createdAt)
                            put("updatedAt", note.updatedAt)
                        })
                    }
                })

                // Danh sách checklist items
                put("checklistItems", JSONArray().apply {
                    checklistItems.forEach { item ->
                        put(JSONObject().apply {
                            put("id", item.id)
                            put("noteId", item.noteId)
                            put("text", item.text)
                            put("isChecked", item.isChecked)
                            put("order", item.order)
                        })
                    }
                })
            }

            val jsonString = json.toString(2) // indent 2 cho dễ đọc
            val fileName = "SmartNote_Backup_${System.currentTimeMillis()}.json"
            val outputStream: OutputStream
            val uri: Uri

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/json")
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

            outputStream.write(jsonString.toByteArray())
            outputStream.close()
            Toast.makeText(context, "Backup thành công!", Toast.LENGTH_SHORT).show()
            uri
        } catch (e: Exception) {
            Toast.makeText(context, "Lỗi backup: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    // ── Đọc file JSON backup và trả về dữ liệu ───────────────────────
    fun parseBackup(
        context: Context,
        uri: Uri
    ): Pair<List<NoteEntity>, List<ChecklistItemEntity>>? {
        return try {
            val jsonString = context.contentResolver
                .openInputStream(uri)
                ?.bufferedReader()
                ?.readText() ?: return null

            val json = JSONObject(jsonString)
            val notesArray = json.getJSONArray("notes")
            val itemsArray = json.getJSONArray("checklistItems")

            val notes = (0 until notesArray.length()).map { i ->
                val obj = notesArray.getJSONObject(i)
                NoteEntity(
                    id = 0, // Reset ID để Room tự tạo mới
                    title = obj.getString("title"),
                    content = obj.getString("content"),
                    type = obj.getString("type"),
                    colorTag = obj.getString("colorTag"),
                    imagePaths = obj.optString("imagePaths", ""),
                    sketchPath = obj.optString("sketchPath", ""),
                    reminderTime = obj.optLong("reminderTime", -1L),
                    isDeleted = obj.optBoolean("isDeleted", false),
                    isPinned = obj.optBoolean("isPinned", false),
                    createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                )
            }

            val items = (0 until itemsArray.length()).map { i ->
                val obj = itemsArray.getJSONObject(i)
                ChecklistItemEntity(
                    id = 0, // Reset ID
                    noteId = obj.getInt("noteId"),
                    text = obj.getString("text"),
                    isChecked = obj.getBoolean("isChecked"),
                    order = obj.getInt("order")
                )
            }

            Pair(notes, items)
        } catch (e: Exception) {
            Toast.makeText(context, "Lỗi đọc backup: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }
}