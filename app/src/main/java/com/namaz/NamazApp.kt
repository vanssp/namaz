package com.namaz

import android.app.Application
import com.namaz.data.CalendarRepository
import com.namaz.data.LocationRepository
import com.namaz.data.PreferencesRepository
import com.namaz.reminder.NotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class NamazApp : Application() {
    lateinit var preferences: PreferencesRepository
        private set
    lateinit var location: LocationRepository
        private set
    lateinit var calendars: CalendarRepository
        private set

    override fun onCreate() {
        super.onCreate()
        preferences = PreferencesRepository(this)
        location = LocationRepository(this)
        calendars = CalendarRepository(this)
        NotificationChannels.create(this)
    }

    companion object {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
