package org.isaac.geotrails.util

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Preferences @Inject constructor() {

    @Inject
    @ApplicationContext
    lateinit var appContext: Context

    private val _preferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(appContext)

    var distanceMetric: DistanceMetric
        get() = DistanceMetric.valueOf(
            _preferences.getString(DISTANCE_METRIC, null)
                ?: DistanceMetric.KM.name
        )
        set(value) = _preferences.edit().putString(DISTANCE_METRIC, value.name).apply()

    var velocityMetric: VelocityMetric
        get() = VelocityMetric.valueOf(
            _preferences.getString(VELOCITY_METRIC, null)
                ?: VelocityMetric.KM_PER_HOUR.name
        )
        set(value) = _preferences.edit().putString(VELOCITY_METRIC, value.name).apply()

    enum class DistanceMetric(name: String) {
        KM("km"),
        METERS("m"),
        MILES("mi"),
        FEET("ft")
    }

    enum class VelocityMetric(name: String) {
        KM_PER_HOUR("km/h"),
        METERS_PER_SECOND("km/h"),
        MILES_PER_HOUR("km/h"),
        FEET_PER_SECOND("km/h")
    }

    companion object {
        const val DISTANCE_METRIC = "GeoTrails.DISTANCE_METRIC"
        const val VELOCITY_METRIC = "GeoTrails.VELOCITY_METRIC"
    }
}