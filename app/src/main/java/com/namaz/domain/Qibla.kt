package com.namaz.domain

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Qibla {
    const val KAABA_LATITUDE = 21.4225
    const val KAABA_LONGITUDE = 39.8262

    fun bearing(latitude: Double, longitude: Double): Double {
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(KAABA_LATITUDE)
        val delta = Math.toRadians(KAABA_LONGITUDE - longitude)
        val y = sin(delta) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(delta)
        return (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
    }

    fun distanceKm(latitude: Double, longitude: Double): Double {
        val earth = 6371.0
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(KAABA_LATITUDE)
        val dLat = lat2 - lat1
        val dLng = Math.toRadians(KAABA_LONGITUDE - longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(lat1) * cos(lat2) * sin(dLng / 2) * sin(dLng / 2)
        return 2 * earth * asin(sqrt(a))
    }
}
