package com.namaz.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.namaz.NamazApp
import kotlinx.coroutines.launch

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        NamazApp.scope.launch {
            try {
                if (intent.action == PrayerSchedule.ACTION_PRAYER) {
                    PrayerSchedule.onPrayer(
                        context = context,
                        prayerName = intent.getStringExtra(PrayerSchedule.EXTRA_PRAYER).orEmpty(),
                        clock = intent.getStringExtra(PrayerSchedule.EXTRA_CLOCK).orEmpty(),
                        lead = intent.getIntExtra(PrayerSchedule.EXTRA_LEAD, 0),
                    )
                } else {
                    PrayerSchedule.refresh(context)
                }
            } catch (error: Throwable) {
                Log.e("Namaz", "Не удалось обработать напоминание", error)
            } finally {
                pending.finish()
            }
        }
    }
}
