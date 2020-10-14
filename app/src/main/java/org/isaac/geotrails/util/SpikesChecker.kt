package org.isaac.geotrails.util

import timber.log.Timber
import kotlin.math.abs

internal class SpikesChecker(// The maximum vertical acceleration allowed
    private val maxAcceleration: Float,
    private var stabilizationTime: Int = 4 // Stabilization window, in seconds. It must be > 0
) {
    private var goodTime = NOT_AVAILABLE.toLong() // The time of the last good value
    private var prevAltitude = NOT_AVAILABLE.toDouble() // the previous data loaded
    private var prevTime = NOT_AVAILABLE.toLong()
    private var prevVerticalSpeed = NOT_AVAILABLE.toFloat()
    private var newAltitude = NOT_AVAILABLE.toDouble() // the new (current) data loaded
    private var newTime = NOT_AVAILABLE.toLong()
    private var newVerticalSpeed = NOT_AVAILABLE.toFloat()
    private var timeInterval = NOT_AVAILABLE.toLong() // Interval between fixes (in seconds)
    private var verticalAcceleration = 0f
    fun load(Time: Long, Altitude: Double) {
        if (Time > newTime) {
            prevTime = newTime
            newTime = Time
            prevAltitude = newAltitude
            prevVerticalSpeed = newVerticalSpeed
        }
        timeInterval =
            if (prevTime != NOT_AVAILABLE.toLong()) (newTime - prevTime) / 1000 else NOT_AVAILABLE.toLong()
        newAltitude = Altitude
        if (timeInterval > 0 && prevAltitude != NOT_AVAILABLE.toDouble()) {
            newVerticalSpeed = (newAltitude - prevAltitude).toFloat() / timeInterval
            if (prevVerticalSpeed != NOT_AVAILABLE.toFloat()) {
                verticalAcceleration =
                    if (timeInterval > 1000) NOT_AVAILABLE.toFloat() // Prevent Vertical Acceleration value from exploding
                    else 2 * (-prevVerticalSpeed * timeInterval + (newAltitude - prevAltitude).toFloat()) / (timeInterval * timeInterval)
            }
        }
        if (abs(verticalAcceleration) >= maxAcceleration) goodTime = newTime

        Timber.d("Vertical Acceleration = $verticalAcceleration")
        Timber.d("Validation window = ${(newTime - goodTime) / 1000}")
    }

    val isValid: Boolean
        get() = (newTime - goodTime) / 1000 >= stabilizationTime

    companion object {
        private const val NOT_AVAILABLE = -100000
    }
}
