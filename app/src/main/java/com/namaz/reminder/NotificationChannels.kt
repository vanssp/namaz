package com.namaz.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import com.namaz.R

object NotificationChannels {
    const val SOUND = "namaz_sound"
    const val SILENT = "namaz_silent"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val sound = Uri.parse("android.resource://${context.packageName}/${R.raw.adhan_reminder}")
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val audible = NotificationChannel(SOUND, "Намаз", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Напоминание ко времени намаза"
            setSound(sound, attributes)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 180, 120, 180)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        val quiet = NotificationChannel(SILENT, "Намаз без звука", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Тихое напоминание"
            setSound(null, null)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 120)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(audible)
        manager.createNotificationChannel(quiet)
    }
}
