package org.isaac.geotrails.entity

import android.location.Location
import androidx.room.*
import org.isaac.geotrails.entity.dao.PointDao
import org.isaac.geotrails.util.EGM96

@Entity(
    tableName = PointDao.TABLE_NAME,
    foreignKeys = [
        ForeignKey(entity = Track::class, parentColumns = ["id"], childColumns = ["track_id"])
    ],
    indices = [Index("track_id")]
)
data class Point(
    @ColumnInfo(name = "latitude")
    var latitude: Double,
    @ColumnInfo(name = "longitude")
    var longitude: Double,
    @ColumnInfo(name = "timestamp")
    var timestamp: Long
) {

    companion object {
        const val NOT_AVAILABLE = -100000
    }

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0L

    @ColumnInfo(name = "track_id")
    var trackId: Long = 0L

    @ColumnInfo(name = "has_altitude")
    var hasAltitude: Boolean = false

    @ColumnInfo(name = "altitude")
    var altitude: Double = NOT_AVAILABLE.toDouble()

    @ColumnInfo(name = "has_speed")
    var hasSpeed: Boolean = false

    @ColumnInfo(name = "speed")
    var speed: Float = NOT_AVAILABLE.toFloat()

    @ColumnInfo(name = "has_accuracy")
    var hasAccuracy: Boolean = false

    @ColumnInfo(name = "accuracy")
    var accuracy: Float = NOT_AVAILABLE.toFloat()

    @ColumnInfo(name = "has_bearing")
    var hasBearing: Boolean = false

    @ColumnInfo(name = "bearing")
    var bearing: Float = NOT_AVAILABLE.toFloat()

    @ColumnInfo(name = "description")
    var description: String = ""

    @ColumnInfo(name = "number_of_satellites")
    var numberOfSatellites = NOT_AVAILABLE

    @ColumnInfo(name = "number_of_satellites_used_in_fix")
    var numberOfSatellitesUsedInFix = NOT_AVAILABLE

    @Ignore
    private var _altitudeEgm96Correction = NOT_AVAILABLE.toDouble()

    val altitudeEgm96Correction: Double
        get() {
            if (_altitudeEgm96Correction == NOT_AVAILABLE.toDouble() && EGM96.isEgmGridLoaded) {
                _altitudeEgm96Correction = EGM96.getEGMCorrection(latitude, longitude)
            }
            return _altitudeEgm96Correction
        }

    constructor(location: Location) : this(
        latitude = location.latitude,
        longitude = location.longitude,
        timestamp = location.time,
    ) {
        hasAltitude = location.hasAltitude()
        altitude = location.altitude
        hasSpeed = location.hasSpeed()
        speed = location.speed
        hasAccuracy = location.hasAccuracy()
        accuracy = location.accuracy
        hasBearing = location.hasBearing()
        bearing = location.bearing
    }

    // Constructor
    init {
        if (EGM96.isEgmGridLoaded) {
            _altitudeEgm96Correction = EGM96.getEGMCorrection(latitude, longitude)
        }
    }

    fun getAltitudeCorrected(manualCorrection: Double, useEgmCorrection: Boolean): Double {
        return if (!hasAltitude) {
            NOT_AVAILABLE.toDouble()
        } else if (useEgmCorrection && altitudeEgm96Correction != NOT_AVAILABLE.toDouble()) {
            altitude - altitudeEgm96Correction + manualCorrection
        } else {
            altitude + manualCorrection
        }
    }

}

