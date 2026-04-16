package com.example.sketchnote.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.sketchnote.data.local.SketchNoteDatabase
import com.example.sketchnote.utils.AlarmScheduler
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Khôi phục tất cả alarm còn hiệu lực
            runBlocking {
                val database = SketchNoteDatabase.getDatabase(context)
                val noteDao = database.noteDao()
                val notes = noteDao.getAllNotesNonFlow() // Cần thêm function này

                val now = System.currentTimeMillis()
                for (note in notes) {
                    if (note.reminderTime > now) {
                        AlarmScheduler.schedule(
                            context = context,
                            noteId = note.id,
                            title = note.title.ifBlank { "Nhắc nhở" },
                            content = note.content.ifBlank { "Bạn có ghi chú cần xem!" },
                            timeMillis = note.reminderTime
                        )
                    }
                }
            }
        }
    }
}