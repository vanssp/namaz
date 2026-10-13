package com.namaz.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.namaz.NamazApp
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        NamazApp.scope.launch {
            try {
                PrayerSchedule.refresh(context)
            } catch (error: Throwable) {
                Log.e("Namaz", "Не удалось восстановить расписание", error)
            } finally {
                pending.finish()
            }
        }
    }
}
