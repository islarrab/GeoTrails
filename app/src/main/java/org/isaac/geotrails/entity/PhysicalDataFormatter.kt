package org.isaac.geotrails.entity

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.isaac.geotrails.util.Preferences
import javax.inject.Inject

internal class PhysicalDataFormatter {

    //private PhysicalData _PhysicalData = new PhysicalData();
    @Inject
    @ApplicationContext
    lateinit var appContext: Context

    @Inject
    lateinit var preferences: Preferences

    fun format(n: Int, format: Byte): PhysicalData {
        val data = PhysicalData(n.toString(), "")
        return data
    }

    fun format(n: Long, format: Byte): PhysicalData {
        val data = PhysicalData(n.toString(), "")
        return data
    }

    fun format(n: Float, format: Byte): PhysicalData {
        val data = PhysicalData(n.toString(), "")
        return data
    }

    fun format(n: Double, format: Byte): PhysicalData {
        val data = PhysicalData(n.toString(), "")
        return data
    }

    companion object {
        const val FORMAT_LATITUDE: Byte = 1
        const val FORMAT_LONGITUDE: Byte = 2
        const val FORMAT_ALTITUDE: Byte = 3
        const val FORMAT_SPEED: Byte = 4
        const val FORMAT_ACCURACY: Byte = 5
        const val FORMAT_BEARING: Byte = 6
        const val FORMAT_DURATION: Byte = 7
        const val FORMAT_SPEED_AVG: Byte = 8
        const val FORMAT_DISTANCE: Byte = 9
        const val FORMAT_TIME: Byte = 10

        private const val NOT_AVAILABLE = -100000
        private const val UM_METRIC_MS = 0
        private const val UM_METRIC_KMH = 1
        private const val UM_IMPERIAL_FPS = 8
        private const val UM_IMPERIAL_MPH = 9
        private const val UM_NAUTICAL_KN = 16
        private const val UM_NAUTICAL_MPH = 17
        private const val M_TO_FT = 3.280839895f
        private const val M_TO_NM = 0.000539957f
        private const val MS_TO_MPH = 2.2369363f
        private const val MS_TO_KMH = 3.6f
        private const val MS_TO_KN = 1.943844491f
        private const val KM_TO_MI = 0.621371192237f
    }
}