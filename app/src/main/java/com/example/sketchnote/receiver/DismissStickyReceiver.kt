package com.example.sketchnote.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DismissStickyReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getIntExtra("noteId", -1)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        // Hủy sticky notification khi nhấn "Đã xong"
        manager.cancel(noteId + 10000)
    }
}