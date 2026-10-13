package com.namaz.ui.qibla

import android.app.Application
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.namaz.NamazApp
import com.namaz.domain.Qibla
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class QiblaUiState(
    val hasLocation: Boolean = false,
    val place: String = "",
    val bearing: Double? = null,
    val distanceKm: Double? = null,
    val heading: Float? = null,
    val sensorAvailable: Boolean = true,
    val lowAccuracy: Boolean = false,
)

class QiblaViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {
    private val sensorManager = application.getSystemService(SensorManager::class.java)
    private val gravity = FloatArray(3)
    private val magnetic = FloatArray(3)
    private var hasGravity = false
    private var hasMagnetic = false
    private var listening = false
    private var smoothed = 0f
    private var hasHeading = false

    private val heading = MutableStateFlow<Float?>(null)
    private val sensorAvailable = MutableStateFlow(true)
    private val lowAccuracy = MutableStateFlow(false)

    val state: StateFlow<QiblaUiState> = combine(
        (application as NamazApp).preferences.settings,
        heading,
        sensorAvailable,
        lowAccuracy,
    ) { settings, magneticHeading, available, inaccurate ->
        val latitude = settings.latitude
        val longitude = settings.longitude
        val declination = if (latitude != null && longitude != null) {
            GeomagneticField(
                latitude.toFloat(),
                longitude.toFloat(),
                0f,
                System.currentTimeMillis(),
            ).declination
        } else {
            0f
        }
        QiblaUiState(
            hasLocation = latitude != null && longitude != null,
            place = settings.locationName,
            bearing = if (latitude != null && longitude != null) Qibla.bearing(latitude, longitude) else null,
            distanceKm = if (latitude != null && longitude != null) Qibla.distanceKm(latitude, longitude) else null,
            heading = magneticHeading?.let { (it + declination + 360f) % 360f },
            sensorAvailable = available,
            lowAccuracy = inaccurate,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), QiblaUiState())

    fun start() {
        if (listening) return
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager?.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        if (sensorManager == null || accelerometer == null || magnetometer == null) {
            sensorAvailable.value = false
            return
        }
        sensorAvailable.value = true
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME)
        listening = true
    }

    fun stop() {
        if (!listening) return
        sensorManager?.unregisterListener(this)
        listening = false
    }

    override fun onCleared() {
        stop()
        super.onCleared()
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, gravity, 0, 3)
                hasGravity = true
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetic, 0, 3)
                hasMagnetic = true
            }
        }
        if (!hasGravity || !hasMagnetic || sensorManager == null) return
        val rotation = FloatArray(9)
        val inclination = FloatArray(9)
        if (!SensorManager.getRotationMatrix(rotation, inclination, gravity, magnetic)) return
        val orientation = FloatArray(3)
        SensorManager.getOrientation(rotation, orientation)
        val azimuth = (Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f
        smoothed = if (hasHeading) smooth(smoothed, azimuth) else azimuth
        hasHeading = true
        heading.value = smoothed
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        if (sensor?.type == Sensor.TYPE_MAGNETIC_FIELD) {
            lowAccuracy.value = accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE ||
                accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW
        }
    }

    private fun smooth(previous: Float, next: Float): Float {
        var delta = next - previous
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        return (previous + delta * 0.12f + 360f) % 360f
    }
}
