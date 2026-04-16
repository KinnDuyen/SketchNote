package com.example.sketchnote.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.sketchnote.MainActivity
import com.example.sketchnote.R

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "sketchnote_reminder_channel"
        const val CHANNEL_NAME = "SketchNote Nhắc nhở"
        const val NOTIFICATION_ID = 1000
    }

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getIntExtra("NOTE_ID", -1)
        val title = intent.getStringExtra("TITLE") ?: "Nhắc nhở"
        val content = intent.getStringExtra("CONTENT") ?: "Bạn có ghi chú cần xem!"

        createNotificationChannel(context)
        showNotification(context, noteId, title, content)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kênh thông báo nhắc nhở ghi chú"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 200, 500)
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(context: Context, noteId: Int, title: String, content: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("NOTE_ID", noteId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        } else {
            android.app.PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = android.app.PendingIntent.getActivity(
            context, noteId, intent, pendingIntentFlags
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_delete,  // Dùng icon có sẵn của Android
                "Đã xong",
                getDonePendingIntent(context, noteId)
            )
            .build()

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.notify(noteId + NOTIFICATION_ID, notification)
    }

    private fun getDonePendingIntent(context: Context, noteId: Int): android.app.PendingIntent {
        val intent = Intent(context, DismissNotificationReceiver::class.java).apply {
            putExtra("NOTE_ID", noteId)
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        } else {
            android.app.PendingIntent.FLAG_UPDATE_CURRENT
        }

        return android.app.PendingIntent.getBroadcast(context, noteId, intent, flags)
    }
}

class DismissNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getIntExtra("NOTE_ID", -1)
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancel(noteId + AlarmReceiver.NOTIFICATION_ID)
    }
}