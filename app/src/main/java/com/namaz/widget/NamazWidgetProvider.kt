package com.namaz.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.RemoteViews
import com.namaz.MainActivity
import com.namaz.NamazApp
import com.namaz.R
import com.namaz.data.UserSettings
import com.namaz.domain.PrayerMoment
import com.namaz.domain.focusPrayer
import com.namaz.domain.formatClock
import com.namaz.reminder.PrayerSchedule
import kotlinx.coroutines.launch
import java.time.ZonedDateTime

class NamazWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pending = goAsync()
        NamazApp.scope.launch {
            try {
                PrayerSchedule.refresh(context)
            } catch (error: Throwable) {
                Log.e("Namaz", "Не удалось обновить виджет", error)
            } finally {
                pending.finish()
            }
        }
    }
}

object WidgetUpdater {
    fun render(
        context: Context,
        settings: UserSettings,
        moments: List<PrayerMoment>,
        now: ZonedDateTime,
    ) {
        val manager = AppWidgetManager.getInstance(context)
        val ids = manager.getAppWidgetIds(ComponentName(context, NamazWidgetProvider::class.java))
        if (ids.isEmpty()) return
        val views = RemoteViews(context.packageName, R.layout.widget_namaz)
        val focus = focusPrayer(moments, now)
        val time = focus?.time
        if (focus == null || time == null || settings.latitude == null) {
            views.setTextViewText(R.id.widget_label, "Намаз")
            views.setTextViewText(R.id.widget_name, "—")
            views.setTextViewText(R.id.widget_time, "Откройте приложение")
        } else {
            val label = when {
                time.toLocalDate().isAfter(now.toLocalDate()) -> "Завтра"
                !time.isAfter(now) -> "Сейчас"
                else -> "Следующий намаз"
            }
            views.setTextViewText(R.id.widget_label, label)
            views.setTextViewText(R.id.widget_name, focus.prayer.title)
            views.setTextViewText(R.id.widget_time, formatClock(time))
        }
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        views.setOnClickPendingIntent(R.id.widget_root, open)
        manager.updateAppWidget(ids, views)
    }
}
