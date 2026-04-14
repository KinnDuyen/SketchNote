package com.example.sketchnote.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.sketchnote.receiver.AlarmReceiver

object AlarmScheduler {

    fun schedule(
        context: Context,
        noteId: Int,
        title: String,
        content: String,
        timeMillis: Long
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("noteId", noteId)
            putExtra("title", title)
            putExtra("content", content)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, noteId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            // Android 12+ cần kiểm tra quyền SCHEDULE_EXACT_ALARM trước
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent
                    )
                } else {
                    // Không có quyền exact alarm — dùng inexact thay thế
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent
                    )
                }
            } else {
                // Android 11 trở xuống — dùng bình thường
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback nếu bị từ chối quyền
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, timeMillis, pendingIntent
            )
        }
    }

    fun cancel(context: Context, noteId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context, noteId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    // Kiểm tra app có quyền đặt exact alarm không
    fun canScheduleExact(context: Context): Boolean {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Android 11 trở xuống luôn được phép
        }
    }
}