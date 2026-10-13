package com.namaz.reminder

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.namaz.MainActivity
import com.namaz.R
import com.namaz.domain.Prayer

object Notifier {
    private const val NOTIFICATION_ID = 1001

    fun show(context: Context, prayer: Prayer, clock: String, leadMinutes: Int, sound: Boolean) {
        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            if (granted != PackageManager.PERMISSION_GRANTED) return
        }
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = if (leadMinutes > 0) "${prayer.title} через $leadMinutes мин" else prayer.title
        val text = if (leadMinutes > 0) "Намаз в $clock" else "Время намаза · $clock"
        val notification = NotificationCompat.Builder(
            context,
            if (sound) NotificationChannels.SOUND else NotificationChannels.SILENT,
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .build()
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        manager.notify(NOTIFICATION_ID, notification)
    }
}
