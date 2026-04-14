package com.example.sketchnote.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.sketchnote.MainActivity
import com.example.sketchnote.R

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val noteId = intent.getIntExtra("noteId", -1)
        val title = intent.getStringExtra("title") ?: "Nhắc nhở"
        val content = intent.getStringExtra("content") ?: "Bạn có ghi chú cần xem!"

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        // ── Channel 1: Thông báo nhắc nhở thường (tự đóng) ──────────────
        val reminderChannelId = "sketchnote_reminder"
        manager.createNotificationChannel(
            NotificationChannel(
                reminderChannelId,
                "Nhắc nhở ghi chú",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Thông báo nhắc nhở từ Smart Note" }
        )

        // ── Channel 2: Sticky Notification (ghim, không tự đóng) ─────────
        val stickyChannelId = "sketchnote_sticky"
        manager.createNotificationChannel(
            NotificationChannel(
                stickyChannelId,
                "Ghi chú đang ghim",
                NotificationManager.IMPORTANCE_LOW  // LOW để không kêu, chỉ hiện
            ).apply {
                description = "Ghi chú được ghim trên thanh thông báo"
                setShowBadge(false)
            }
        )

        // Intent mở app khi nhấn notification
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("noteId", noteId)
        }
        val openPending = PendingIntent.getActivity(
            context, noteId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent bấm "Đã xong" để dismiss sticky
        val dismissIntent = Intent(context, DismissStickyReceiver::class.java).apply {
            putExtra("noteId", noteId)
        }
        val dismissPending = PendingIntent.getBroadcast(
            context, noteId + 10000, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ── Thông báo nhắc nhở chính (pop-up, tự đóng khi nhấn) ──────────
        val reminderNotification = NotificationCompat.Builder(context, reminderChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .build()

        // ── Sticky Notification (ghim, không tự đóng) ────────────────────
        val stickyNotification = NotificationCompat.Builder(context, stickyChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)          // ← Quan trọng: không cho vuốt bỏ
            .setAutoCancel(false)
            .setContentIntent(openPending)
            .addAction(                // Nút "Đã xong" để tự tắt sticky
                R.drawable.ic_launcher_foreground,
                "Đã xong",
                dismissPending
            )
            .build()

        // Hiện cả 2 notification
        manager.notify(noteId, reminderNotification)
        manager.notify(noteId + 10000, stickyNotification)  // ID khác để không đè nhau
    }
}