package org.isaac.geotrails.entity

import android.location.Location
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import org.isaac.geotrails.GeoTrailsApplication
import org.isaac.geotrails.entity.dao.TrackDao
import org.isaac.geotrails.util.EGM96
import org.isaac.geotrails.util.SpikesChecker
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Entity(tableName = TrackDao.TABLE_NAME)
data class Track(
    @ColumnInfo(name = "name")
    var name: String = ""
) {
    @Ignore
    private val TRACK_TYPE_STEADY = 0
    @Ignore
    private val TRACK_TYPE_WALK = 1
    @Ignore
    private val TRACK_TYPE_MOUNTAIN = 2
    @Ignore
    private val TRACK_TYPE_RUN = 3
    @Ignore
    private val TRACK_TYPE_BICYCLE = 4
    @Ignore
    private val TRACK_TYPE_CAR = 5
    @Ignore
    private val TRACK_TYPE_FLIGHT = 6
    @Ignore
    private val TRACK_TYPE_ND = NOT_AVAILABLE

    @Ignore
    private var Start_EGMAltitudeCorrection = NOT_AVAILABLE.toDouble()
    @Ignore
    private var End_EGMAltitudeCorrection = NOT_AVAILABLE.toDouble()

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Long = 0L

    @ColumnInfo(name = "start_latitude")
    var start_Latitude = NOT_AVAILABLE
        .toDouble()

    @ColumnInfo(name = "start_longitude")
    var start_Longitude = NOT_AVAILABLE
        .toDouble()

    @ColumnInfo(name = "start_altitude")
    var start_Altitude = NOT_AVAILABLE
        .toDouble()

    @ColumnInfo(name = "start_accuracy")
    var start_Accuracy = STANDARD_ACCURACY

    @ColumnInfo(name = "start_speed")
    var start_Speed = NOT_AVAILABLE
        .toFloat()

    @ColumnInfo(name = "start_time")
    var start_Time = NOT_AVAILABLE
        .toLong()

    @ColumnInfo(name = "last_fix_time")
    var lastFix_Time = NOT_AVAILABLE
        .toLong()

    @ColumnInfo(name = "end_latitude")
    var end_Latitude = NOT_AVAILABLE
        .toDouble()

    @ColumnInfo(name = "end_longitude")
    var end_Longitude = NOT_AVAILABLE
        .toDouble()

    @ColumnInfo(name = "end_altitude")
    var end_Altitude = NOT_AVAILABLE
        .toDouble()

    @ColumnInfo(name = "end_accuracy")
    var end_Accuracy = STANDARD_ACCURACY

    @ColumnInfo(name = "end_speed")
    var end_Speed = NOT_AVAILABLE
        .toFloat()

    @ColumnInfo(name = "end_time")
    var end_Time = NOT_AVAILABLE
        .toLong()

    @ColumnInfo(name = "last_step_distance_latitude")
    var lastStepDistance_Latitude = NOT_AVAILABLE
        .toDouble()

    @ColumnInfo(name = "last_step_distance_longitude")
    var lastStepDistance_Longitude = NOT_AVAILABLE
        .toDouble()

    @ColumnInfo(name = "last_step_distance_accuracy")
    var lastStepDistance_Accuracy = STANDARD_ACCURACY

    @ColumnInfo(name = "last_step_altitude_altitude")
    var lastStepAltitude_Altitude = NOT_AVAILABLE
        .toDouble()

    @ColumnInfo(name = "last_step_altitude_accuracy")
    var lastStepAltitude_Accuracy = STANDARD_ACCURACY

    @ColumnInfo(name = "min_latitude")
    var min_Latitude = NOT_AVAILABLE
        .toDouble()

    @ColumnInfo(name = "min_longitude")
    var min_Longitude = NOT_AVAILABLE
        .toDouble()

    @ColumnInfo(name = "max_latitude")
    var max_Latitude = NOT_AVAILABLE
        .toDouble()

    @ColumnInfo(name = "max_longitude")
    var max_Longitude = NOT_AVAILABLE
        .toDouble()

    @ColumnInfo(name = "duration")
    var duration = NOT_AVAILABLE
        .toLong()

    @ColumnInfo(name = "duration_moving")
    var duration_Moving = NOT_AVAILABLE
        .toLong()

    @ColumnInfo(name = "distance")
    var distance = NOT_AVAILABLE
        .toFloat()

    @ColumnInfo(name = "distanceInProgress")
    var distanceInProgress = NOT_AVAILABLE
        .toFloat()

    @ColumnInfo(name = "distanceLastAltitude")
    var distanceLastAltitude = NOT_AVAILABLE
        .toLong()

    @ColumnInfo(name = "altitude_up")
    var altitude_Up = NOT_AVAILABLE
        .toDouble()

    @ColumnInfo(name = "altitude_down")
    var altitude_Down = NOT_AVAILABLE
        .toDouble()

    @ColumnInfo(name = "altitude_in_progress")
    var altitude_InProgress = NOT_AVAILABLE
        .toDouble()

    @ColumnInfo(name = "speed_max")
    var speedMax = NOT_AVAILABLE
        .toFloat()

    @ColumnInfo(name = "speed_average")
    var speedAverage = NOT_AVAILABLE
        .toFloat()

    @ColumnInfo(name = "speed_average_moving")
    var speedAverageMoving = NOT_AVAILABLE
        .toFloat()

    @ColumnInfo(name = "number_of_locations")
    var numberOfLocations: Long = 0

    @ColumnInfo(name = "number_of_placemarks")
    var numberOfPlacemarks: Long = 0

    /**
     * true = Map extents valid, OK generation of Thumb
     * false = Do not generate thumb (track crosses antimeridian)
     */
    @ColumnInfo(name = "valid_map")
    var validMap = true

    @ColumnInfo(name = "type")
    var type = TRACK_TYPE_ND

    // True if the card view is selected
    @Ignore
    var isSelected = false

    // The altitude validator (the anti-spikes filter):
    // - Max Acceleration = 12 m/s^2
    // - Stabilization time = 4 s
    @Ignore
    private val AltitudeFilter: SpikesChecker = SpikesChecker(12f, 4)

    fun add(location: Point) {
        if (numberOfLocations == 0L) {
            // Init "Start" variables
            start_Latitude = location.latitude
            start_Longitude = location.longitude
            start_Altitude = location.altitude
            Start_EGMAltitudeCorrection = location.altitudeEgm96Correction
            start_Speed = location.speed
            start_Accuracy =
                if (location.accuracy != NOT_AVAILABLE.toFloat()) location.accuracy
                else STANDARD_ACCURACY
            start_Time = location.timestamp
            lastStepDistance_Latitude = start_Latitude
            lastStepDistance_Longitude = start_Longitude
            lastStepDistance_Accuracy = start_Accuracy
            max_Latitude = start_Latitude
            max_Longitude = start_Longitude
            min_Latitude = start_Latitude
            min_Longitude = start_Longitude
            if (name == "") {
                val df2 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                name = df2.format(start_Time)
            }
            lastFix_Time = start_Time
            end_Time = start_Time
            duration_Moving = 0
            duration = 0
            distance = 0f
        }
        lastFix_Time = end_Time
        end_Latitude = location.latitude
        end_Longitude = location.longitude
        end_Altitude = location.altitude

        End_EGMAltitudeCorrection = location.altitudeEgm96Correction
        end_Speed = location.speed
        end_Accuracy =
            if (location.accuracy != NOT_AVAILABLE.toFloat()) location.accuracy
            else STANDARD_ACCURACY

        end_Time = location.timestamp

        if (End_EGMAltitudeCorrection == NOT_AVAILABLE.toDouble()) getEnd_EGMAltitudeCorrection()
        if (Start_EGMAltitudeCorrection == NOT_AVAILABLE.toDouble()) getStart_EGMAltitudeCorrection()

        // Load the new value into the anti-spikes filter
        if (end_Altitude != NOT_AVAILABLE.toDouble()) AltitudeFilter.load(
            end_Time,
            end_Altitude
        )

        // Coords for thumb and stats
        if (validMap) {
            if (end_Latitude > max_Latitude) max_Latitude = end_Latitude
            if (end_Longitude > max_Longitude) max_Longitude = end_Longitude
            if (end_Latitude < min_Latitude) min_Latitude = end_Latitude
            if (end_Longitude < min_Longitude) min_Longitude = end_Longitude
            if (abs(lastStepDistance_Longitude - end_Longitude) > 90) validMap = false
            //  YOU PASS FROM -180 TO +180, OR REVERSE. iN THE PACIFIC OCEAN.
            //  in that case the app doesn't generate the thumb map.
        }

        // time calculation
        duration = end_Time - start_Time
        if (end_Speed >= MOVEMENT_SPEED_THRESHOLD) duration_Moving += end_Time - lastFix_Time

        // --------------------------- Spaces (Distances) increment if distance > sum of accuracies

        // -- Temp locations for "DistanceTo"
        val lastStepDistanceLoc = Location("TEMP")
        lastStepDistanceLoc.latitude = lastStepDistance_Latitude
        lastStepDistanceLoc.longitude = lastStepDistance_Longitude
        val endLoc = Location("TEMP")
        endLoc.latitude = end_Latitude
        endLoc.longitude = end_Longitude
        // -----------------------------------
        distanceInProgress = lastStepDistanceLoc.distanceTo(endLoc)
        val deltaDistancePlusAccuracy = distanceInProgress + end_Accuracy
        if (deltaDistancePlusAccuracy < distanceInProgress + end_Accuracy) {
            lastStepDistance_Accuracy = deltaDistancePlusAccuracy
            //Log.w("myApp", "[#] Track.java - LastStepDistance_Accuracy updated to " + LastStepDistance_Accuracy );
        }
        if (distanceInProgress > end_Accuracy + lastStepDistance_Accuracy) {
            distance += distanceInProgress
            if (distanceLastAltitude != NOT_AVAILABLE.toLong()) distanceLastAltitude += distanceInProgress.toLong()
            distanceInProgress = 0f
            lastStepDistance_Latitude = end_Latitude
            lastStepDistance_Longitude = end_Longitude
            lastStepDistance_Accuracy = end_Accuracy
        }

        // Found a first fix with altitude!!
        if (end_Altitude != NOT_AVAILABLE.toDouble() && distanceLastAltitude == NOT_AVAILABLE.toLong()) {
            distanceLastAltitude = 0
            altitude_Up = 0.0
            altitude_Down = 0.0
            if (start_Altitude == NOT_AVAILABLE.toDouble()) start_Altitude =
                end_Altitude
            lastStepAltitude_Altitude = end_Altitude
            lastStepAltitude_Accuracy = end_Accuracy
        }
        if (lastStepAltitude_Altitude != NOT_AVAILABLE.toDouble() && end_Altitude != NOT_AVAILABLE.toDouble()) {
            altitude_InProgress = end_Altitude - lastStepAltitude_Altitude
            // Improve last step accuracy in case of new data elements:
            val deltaAltitudePlusAccuracy = abs(altitude_InProgress)
                .toFloat() + end_Accuracy
            if (deltaAltitudePlusAccuracy <= lastStepAltitude_Accuracy) {
                lastStepAltitude_Accuracy = deltaAltitudePlusAccuracy
                distanceLastAltitude = 0
                //Log.w("myApp", "[#] Track.java - LastStepAltitude_Accuracy updated to " + LastStepAltitude_Accuracy );
            }
            // Evaluate the altitude step convalidation:
            if (abs(altitude_InProgress) > MIN_ALTITUDE_STEP && AltitudeFilter.isValid
                && abs(altitude_InProgress)
                    .toFloat() > SECURITY_COEFF * (lastStepAltitude_Accuracy + end_Accuracy)
            ) {
                // Altitude step:
                // increment distance only if the inclination is relevant (assume deltah=20m in max 5000m)
                if (distanceLastAltitude < 5000) {
                    val hypotenuse =
                        Math.sqrt((distanceLastAltitude * distanceLastAltitude).toDouble() + altitude_InProgress * altitude_InProgress)
                            .toFloat()
                    distance = distance + hypotenuse - distanceLastAltitude
                    //Log.w("myApp", "[#] Track.java - Distance += " + (hypotenuse - DistanceLastAltitude));
                }
                //Reset variables
                lastStepAltitude_Altitude = end_Altitude
                lastStepAltitude_Accuracy = end_Accuracy
                distanceLastAltitude = 0
                if (altitude_InProgress > 0) altitude_Up += altitude_InProgress // Increment the correct value of Altitude UP/DOWN
                else altitude_Down -= altitude_InProgress
                altitude_InProgress = 0.0
            }
        }

        // --------------------------------------------------------------------------------- Speeds
        if (end_Speed != NOT_AVAILABLE.toFloat() && end_Speed > speedMax) speedMax =
            end_Speed
        if (duration > 0) speedAverage =
            (distance + distanceInProgress) / (duration.toFloat() / 1000f)
        if (duration_Moving > 0) speedAverageMoving =
            (distance + distanceInProgress) / (duration_Moving.toFloat() / 1000f)
        numberOfLocations++
    }

    @Deprecated(message = "delete when possible")
    fun FromDB(
        id: Long,
        Name: String,
        From: String?,
        To: String?,
        Start_Latitude: Double,
        Start_Longitude: Double,
        Start_Altitude: Double,
        Start_Accuracy: Float,
        Start_Speed: Float,
        Start_Time: Long,
        LastFix_Time: Long,
        End_Latitude: Double,
        End_Longitude: Double,
        End_Altitude: Double,
        End_Accuracy: Float,
        End_Speed: Float,
        End_Time: Long,
        LastStepDistance_Latitude: Double,
        LastStepDistance_Longitude: Double,
        LastStepDistance_Accuracy: Float,
        LastStepAltitude_Altitude: Double,
        LastStepAltitude_Accuracy: Float,
        Min_Latitude: Double,
        Min_Longitude: Double,
        Max_Latitude: Double,
        Max_Longitude: Double,
        Duration: Long,
        Duration_Moving: Long,
        Distance: Float,
        DistanceInProgress: Float,
        DistanceLastAltitude: Long,
        Altitude_Up: Double,
        Altitude_Down: Double,
        Altitude_InProgress: Double,
        SpeedMax: Float,
        SpeedAverage: Float,
        SpeedAverageMoving: Float,
        NumberOfLocations: Long,
        NumberOfPlacemarks: Long,
        ValidMap: Boolean,
        Type: Int
    ) {
        this.id = id
        name = Name
        start_Latitude = Start_Latitude
        start_Longitude = Start_Longitude
        start_Altitude = Start_Altitude
        start_Accuracy = Start_Accuracy
        start_Speed = Start_Speed
        start_Time = Start_Time
        lastFix_Time = LastFix_Time
        end_Latitude = End_Latitude
        end_Longitude = End_Longitude
        end_Altitude = End_Altitude
        end_Accuracy = End_Accuracy
        end_Speed = End_Speed
        end_Time = End_Time
        lastStepDistance_Latitude = LastStepDistance_Latitude
        lastStepDistance_Longitude = LastStepDistance_Longitude
        lastStepDistance_Accuracy = LastStepDistance_Accuracy
        lastStepAltitude_Altitude = LastStepAltitude_Altitude
        lastStepAltitude_Accuracy = LastStepAltitude_Accuracy
        min_Latitude = Min_Latitude
        min_Longitude = Min_Longitude
        max_Latitude = Max_Latitude
        max_Longitude = Max_Longitude
        duration = Duration
        duration_Moving = Duration_Moving
        distance = Distance
        distanceInProgress = DistanceInProgress
        distanceLastAltitude = DistanceLastAltitude
        altitude_Up = Altitude_Up
        altitude_Down = Altitude_Down
        altitude_InProgress = Altitude_InProgress
        speedMax = SpeedMax
        speedAverage = SpeedAverage
        speedAverageMoving = SpeedAverageMoving
        numberOfLocations = NumberOfLocations
        numberOfPlacemarks = NumberOfPlacemarks
        validMap = ValidMap
        type = Type

        if (EGM96.isEgmGridLoaded) {
            if (Start_Latitude != NOT_AVAILABLE.toDouble()) Start_EGMAltitudeCorrection =
                EGM96.getEGMCorrection(Start_Latitude, Start_Longitude)
            if (End_Latitude != NOT_AVAILABLE.toDouble()) End_EGMAltitudeCorrection =
                EGM96.getEGMCorrection(End_Latitude, End_Longitude)
        }
    }

    fun getStart_EGMAltitudeCorrection(): Double {
        if (Start_EGMAltitudeCorrection == NOT_AVAILABLE.toDouble()) {
            if (EGM96.isEgmGridLoaded) {
                if (start_Latitude != NOT_AVAILABLE.toDouble()) Start_EGMAltitudeCorrection =
                    EGM96.getEGMCorrection(
                        start_Latitude,
                        start_Longitude
                    )
            }
        }
        return Start_EGMAltitudeCorrection
    }

    fun getEnd_EGMAltitudeCorrection(): Double {
        if (End_EGMAltitudeCorrection == NOT_AVAILABLE.toDouble()) {
            if (EGM96.isEgmGridLoaded) {
                if (end_Latitude != NOT_AVAILABLE.toDouble()) End_EGMAltitudeCorrection =
                    EGM96.getEGMCorrection(
                        end_Latitude,
                        end_Longitude
                    )
            }
        }
        return End_EGMAltitudeCorrection
    }

    // --------------------------------------------------------------------------------------------
    val isValidAltitude: Boolean
        get() = AltitudeFilter.isValid

    fun addPlacemark(location: Point): Long {
        numberOfPlacemarks++
        if (name == "") {
            val df2 = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            name = df2.format(location.timestamp)
        }
        return numberOfPlacemarks
    }

    val estimatedDistance: Float
        get() {
            return when (numberOfLocations) {
                0L -> NOT_AVAILABLE.toFloat()
                1L -> 0f
                else -> distance + distanceInProgress
            }
        }

    fun getEstimatedAltitudeUp(EGMCorrection: Boolean): Double {
        // Retrieve EGM Corrections if available
        if (Start_EGMAltitudeCorrection == NOT_AVAILABLE.toDouble() ||
            End_EGMAltitudeCorrection == NOT_AVAILABLE.toDouble()
        ) {
            if (EGM96.isEgmGridLoaded) {
                if (start_Latitude != NOT_AVAILABLE.toDouble()) {
                    Start_EGMAltitudeCorrection = EGM96.getEGMCorrection(
                        start_Latitude,
                        start_Longitude
                    )
                }
                if (end_Latitude != NOT_AVAILABLE.toDouble()) {
                    End_EGMAltitudeCorrection = EGM96.getEGMCorrection(
                        end_Latitude,
                        end_Longitude
                    )
                }
            }
        }

        var egmcorr = 0.0
        if (EGMCorrection && Start_EGMAltitudeCorrection != NOT_AVAILABLE.toDouble() && End_EGMAltitudeCorrection != NOT_AVAILABLE.toDouble()) {
            egmcorr = Start_EGMAltitudeCorrection - End_EGMAltitudeCorrection
        }

        var dResultUp =
            if (altitude_InProgress > 0) altitude_Up + altitude_InProgress else altitude_Up
        dResultUp -= if (egmcorr < 0) egmcorr else 0.0

        var dResultDown =
            if (altitude_InProgress < 0) altitude_Down - altitude_InProgress else altitude_Down
        dResultDown -= if (egmcorr > 0) egmcorr else 0.0

        if (dResultUp < 0) {
            dResultDown -= dResultUp
            dResultUp = 0.0
        }

        if (dResultDown < 0) {
            dResultUp -= dResultDown
            //dResultDown = 0;
        }
        return dResultUp
    }

    fun getEstimatedAltitudeDown(EGMCorrection: Boolean): Double {
        // Retrieve EGM Corrections if available
        if (Start_EGMAltitudeCorrection == NOT_AVAILABLE.toDouble() || End_EGMAltitudeCorrection == NOT_AVAILABLE.toDouble()) {
            if (EGM96.isEgmGridLoaded) {
                if (start_Latitude != NOT_AVAILABLE.toDouble()) Start_EGMAltitudeCorrection =
                    EGM96.getEGMCorrection(
                        start_Latitude,
                        start_Longitude
                    )
                if (end_Latitude != NOT_AVAILABLE.toDouble()) End_EGMAltitudeCorrection =
                    EGM96.getEGMCorrection(
                        end_Latitude,
                        end_Longitude
                    )
            }
        }
        var egmcorr = 0.0
        if (EGMCorrection && Start_EGMAltitudeCorrection != NOT_AVAILABLE.toDouble() && End_EGMAltitudeCorrection != NOT_AVAILABLE.toDouble()) {
            egmcorr = Start_EGMAltitudeCorrection - End_EGMAltitudeCorrection
        }

        var dResultUp =
            if (altitude_InProgress > 0) altitude_Up + altitude_InProgress else altitude_Up
        dResultUp -= if (egmcorr < 0) egmcorr else 0.0

        var dResultDown =
            if (altitude_InProgress < 0) altitude_Down - altitude_InProgress else altitude_Down

        dResultDown -= if (egmcorr > 0) egmcorr else 0.0

        if (dResultUp < 0) {
            dResultDown -= dResultUp
            dResultUp = 0.0
        }
        if (dResultDown < 0) {
            //dresultUp -= dresultDown;
            dResultDown = 0.0
        }
        return dResultDown
    }

    fun getEstimatedAltitudeGap(EGMCorrection: Boolean): Double {
        return getEstimatedAltitudeUp(EGMCorrection) - getEstimatedAltitudeDown(EGMCorrection)
    }

    val bearing: Float
        get() {
            if (end_Latitude != NOT_AVAILABLE.toDouble() &&
                (start_Latitude != end_Latitude || start_Longitude != end_Longitude) &&
                distance != 0f
            ) {
                val endLoc = Location("TEMP")
                endLoc.latitude = end_Latitude
                endLoc.longitude = end_Longitude
                val startLoc = Location("TEMP")
                startLoc.latitude = start_Latitude
                startLoc.longitude = start_Longitude
                var BTo = startLoc.bearingTo(endLoc)
                if (BTo < 0) BTo += 360f
                return BTo
            } else {
                return NOT_AVAILABLE.toFloat()
            }
        }

    // Returns the time, based on preferences (Total or Moving)
    val prefTime: Long
        get() {
//            val gpsApplication: GeoTrailsApplication = GeoTrailsApplication.instance!!
//            val pTime: Int = gpsApplication.prefShowTrackStatsType
            val pTime = 1
            return when (pTime) {
                0 -> duration
                1 -> duration_Moving
                else -> duration
            }
        }

    // Returns the average speed, based on preferences (Total or Moving)
    val prefSpeedAverage: Float
        get() {
            if (numberOfLocations == 0L) return NOT_AVAILABLE.toFloat()
//            val gpsApplication: GeoTrailsApplication = GeoTrailsApplication.instance!!
//            val pTime: Int = gpsApplication.prefShowTrackStatsType
            val pTime = 1
            return when (pTime) {
                0 -> speedAverage
                1 -> speedAverageMoving
                else -> speedAverage
            }
        }/*
            if (SpeedAverageMoving > 20.0f / 3.6f) return TRACK_TYPE_CAR;
            if (SpeedAverageMoving > 12.0f / 3.6) return TRACK_TYPE_BICYCLE;
            else if (SpeedAverageMoving > 8.0f / 3.6f) return TRACK_TYPE_RUN;
            else {
                if ((Altitude_Up != NOT_AVAILABLE) && (Altitude_Down != NOT_AVAILABLE))
                    if ((Altitude_Down + Altitude_Up > (0.1f * Distance)) && (Distance > 500.0f))
                        return TRACK_TYPE_MOUNTAIN;
                else return TRACK_TYPE_WALK;
            }*/

    //if (Type != TRACK_TYPE_ND) return Type;
    val trackType: Int
        get() {

            //if (Type != TRACK_TYPE_ND) return Type;
            if (distance == NOT_AVAILABLE.toFloat() || speedMax == NOT_AVAILABLE.toFloat()) {
                return if (numberOfPlacemarks == 0L) TRACK_TYPE_ND else TRACK_TYPE_STEADY
            }
            if (distance < 15.0f || speedMax == 0.0f || speedAverageMoving == NOT_AVAILABLE.toFloat()) return TRACK_TYPE_STEADY
            if (speedMax < 7.0f / 3.6f) {
                if (altitude_Up != NOT_AVAILABLE.toDouble() && altitude_Down != NOT_AVAILABLE.toDouble()) return if (altitude_Down + altitude_Up > 0.1f * distance && distance > 500.0f) TRACK_TYPE_MOUNTAIN else TRACK_TYPE_WALK
            }
            if (speedMax < 15.0f / 3.6f) {
                if (speedAverageMoving > 8.0f / 3.6f) return TRACK_TYPE_RUN else {
                    if (altitude_Up != NOT_AVAILABLE.toDouble() && altitude_Down != NOT_AVAILABLE.toDouble()) return if (altitude_Down + altitude_Up > 0.1f * distance && distance > 500.0f) TRACK_TYPE_MOUNTAIN else TRACK_TYPE_WALK
                }
            }
            if (speedMax < 50.0f / 3.6f) {
                if ((speedAverageMoving + speedMax) / 2 > 35.0f / 3.6f) return TRACK_TYPE_CAR
                if ((speedAverageMoving + speedMax) / 2 > 20.0f / 3.6) return TRACK_TYPE_BICYCLE else if ((speedAverageMoving + speedMax) / 2 > 12.0f / 3.6f) return TRACK_TYPE_RUN else {
                    if (altitude_Up != NOT_AVAILABLE.toDouble() && altitude_Down != NOT_AVAILABLE.toDouble()) return if (altitude_Down + altitude_Up > 0.1f * distance && distance > 500.0f) TRACK_TYPE_MOUNTAIN else TRACK_TYPE_WALK
                }
                /*
           if (SpeedAverageMoving > 20.0f / 3.6f) return TRACK_TYPE_CAR;
           if (SpeedAverageMoving > 12.0f / 3.6) return TRACK_TYPE_BICYCLE;
           else if (SpeedAverageMoving > 8.0f / 3.6f) return TRACK_TYPE_RUN;
           else {
               if ((Altitude_Up != NOT_AVAILABLE) && (Altitude_Down != NOT_AVAILABLE))
                   if ((Altitude_Down + Altitude_Up > (0.1f * Distance)) && (Distance > 500.0f))
                       return TRACK_TYPE_MOUNTAIN;
               else return TRACK_TYPE_WALK;
           }*/
            }
            if (altitude_Up != NOT_AVAILABLE.toDouble() && altitude_Down != NOT_AVAILABLE.toDouble()) if (altitude_Down + altitude_Up > 5000.0 && speedMax > 300.0f / 3.6f) return TRACK_TYPE_FLIGHT
            return TRACK_TYPE_CAR
        }

    companion object {
        // Constants
        private const val NOT_AVAILABLE = -100000
        private const val MIN_ALTITUDE_STEP = 8.0
        private const val MOVEMENT_SPEED_THRESHOLD =
            0.5f // The minimum speed (in m/s) to consider the user in movement
        private const val STANDARD_ACCURACY = 10.0f
        private const val SECURITY_COEFF = 1.7f
    }
}
