package com.example.sketchnote.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.sketchnote.utils.AlarmScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import com.example.sketchnote.data.repository.NoteRepository
import kotlinx.coroutines.*

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: NoteRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        CoroutineScope(Dispatchers.IO).launch {
            repository.getAllNotes().collect { notes ->
                notes.filter { it.reminderTime > System.currentTimeMillis() }
                    .forEach { note ->
                        AlarmScheduler.schedule(context, note.id, note.title,
                            note.content, note.reminderTime)
                    }
                cancel() // chỉ collect 1 lần
            }
        }
    }
}