package org.isaac.geotrails.util

import android.content.Context
import android.os.Environment
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.isaac.geotrails.BuildConfig
import org.isaac.geotrails.R
import org.isaac.geotrails.entity.PhysicalData
import org.isaac.geotrails.entity.PhysicalDataFormatter
import org.isaac.geotrails.entity.Point
import org.isaac.geotrails.entity.dao.PointDao
import org.isaac.geotrails.entity.dao.TrackDao
import java.io.BufferedWriter
import java.io.File
import java.io.IOException
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import javax.inject.Inject

class Exporter(
    exportingTask: ExportingTask,
    ExportKML: Boolean,
    ExportGPX: Boolean,
    ExportTXT: Boolean,
    SaveIntoFolder: String
) : Thread() {
    private val exportingTask: ExportingTask
    private val ExportKML = true
    private val ExportGPX = true
    private val ExportTXT = true
    private val SaveIntoFolder = "/"
    private var AltitudeManualCorrection = 0.0
    private var EGMAltitudeCorrection = false
    private var getPrefKMLAltitudeMode = 0
    private var getPrefGPXVersion = 0
    private var TXTFirstTrackpointFlag = true
    private var UnableToWriteFile = false
    var GroupOfLocations // Reads and writes location grouped by this number;
            = 0
    private val arrayGeopoints: ArrayBlockingQueue<Point> =
        ArrayBlockingQueue(3500)
    private val asyncGeopointsLoader = AsyncGeopointsLoader()

    @Inject
    @ApplicationContext
    lateinit var appContext: Context

    @Inject
    lateinit var trackDao: TrackDao

    @Inject
    lateinit var pointDao: PointDao

    override fun run() {
        CoroutineScope(IO).launch {
            val track = trackDao.getById(exportingTask.id).value!!
            val GPX1_0 = 100
            val GPX1_1 = 110
            val creationTime: Date
            val elements_total: Long
            val versionName: String = BuildConfig.VERSION_NAME
            elements_total = track.numberOfLocations + track.numberOfPlacemarks
            val start_Time = System.currentTimeMillis()

            // ------------------------------------------------- Create the Directory tree if not exist
            var sd = File(Environment.getExternalStorageDirectory().toString() + "/GPSLogger")
            if (!sd.exists()) {
                sd.mkdir()
            }
            sd = File(Environment.getExternalStorageDirectory().toString() + "/GPSLogger/AppData")
            if (!sd.exists()) {
                sd.mkdir()
            }
            // ----------------------------------------------------------------------------------------

            if (track.numberOfLocations + track.numberOfPlacemarks == 0L) {
                exportingTask.status = ExportingTask.STATUS_ENDED_FAILED
                //EventBus.getDefault().post(new EventBusMSGNormal(EventBusMSG.TOAST_UNABLE_TO_WRITE_THE_FILE, track.id));
                return@launch
            }

            //EventBus.getDefault().post(new EventBusMSGLong(EventBusMSG.TRACK_SETPROGRESS, track.id, 1));
            if (EGMAltitudeCorrection && EGM96.isEGMGridLoading) {
                try {
                    Log.w("myApp", "[#] Exporter.java - Wait, EGMGrid is loading")
                    do {
                        sleep(200)
                        // Lazy polling until EGM grid finish to load
                    } while (EGM96.isEGMGridLoading)
                } catch (e: InterruptedException) {
                    Log.w("myApp", "[#] Exporter.java - Cannot wait!!")
                }
            }
            val dfdtGPX =
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'") // date and time formatter for GPX timestamp (with millis)
            dfdtGPX.timeZone = TimeZone.getTimeZone("GMT")
            val dfdtGPX_NoMillis =
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'") // date and time formatter for GPX timestamp (without millis)
            dfdtGPX_NoMillis.timeZone = TimeZone.getTimeZone("GMT")
            val dfdtTXT =
                SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss.SSS") // date and time formatter for TXT timestamp (with millis)
            dfdtTXT.timeZone = TimeZone.getTimeZone("GMT")
            val dfdtTXT_NoMillis =
                SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss") // date and time formatter for TXT timestamp (without millis)
            dfdtTXT_NoMillis.timeZone = TimeZone.getTimeZone("GMT")
            var KMLfile: File? = null
            var GPXfile: File? = null
            var TXTfile: File? = null
            //final String newLine = System.getProperty("line.separator"); //\n\r
            val newLine = "\r\n"

            // Verify if Folder exists
            sd = File(SaveIntoFolder)
            var success = true
            if (!sd.exists()) {
                success = sd.mkdir()
            }
            if (!success) {
                Log.w("myApp", "[#] Exporter.java - Unable to sd.mkdir")
                exportingTask.status = ExportingTask.STATUS_ENDED_FAILED
                //EventBus.getDefault().post(new EventBusMSGNormal(EventBusMSG.TOAST_UNABLE_TO_WRITE_THE_FILE, track.id));
                return@launch
            }

            // Create files, deleting old version if exists
            if (ExportKML) {
                KMLfile = File(sd, track.name + ".kml")
                if (KMLfile.exists()) KMLfile.delete()
            }
            if (ExportGPX) {
                GPXfile = File(sd, track.name + ".gpx")
                if (GPXfile.exists()) GPXfile.delete()
            }
            if (ExportTXT) {
                TXTfile = File(sd, track.name + ".txt")
                if (TXTfile.exists()) TXTfile.delete()
            }

            // Create buffers for Write operations
            var KMLfw: PrintWriter? = null
            var KMLbw: BufferedWriter? = null
            var GPXfw: PrintWriter? = null
            var GPXbw: BufferedWriter? = null
            var TXTfw: PrintWriter? = null
            var TXTbw: BufferedWriter? = null

            // Check if all the files are writable:
            try {
                if (ExportGPX && !GPXfile!!.createNewFile() || ExportKML && !KMLfile!!.createNewFile() || ExportTXT && !TXTfile!!.createNewFile()) {
                    UnableToWriteFile = true
                    Log.w("myApp", "[#] Exporter.java - Unable to write the file")
                }
            } catch (e: SecurityException) {
                UnableToWriteFile = true
                Log.w("myApp", "[#] Exporter.java - Unable to write the file: SecurityException")
            } catch (e: IOException) {
                UnableToWriteFile = true
                Log.w("myApp", "[#] Exporter.java - Unable to write the file: IOException")
            } finally {
                // If the file is not writable abort exportation:
                if (UnableToWriteFile) {
                    Log.w("myApp", "[#] Exporter.java - Unable to write the file!!")
                    exportingTask.status = ExportingTask.STATUS_ENDED_FAILED
                    //EventBus.getDefault().post(new EventBusMSGNormal(EventBusMSG.TOAST_UNABLE_TO_WRITE_THE_FILE, track.id));
                    return@launch
                }
            }
            asyncGeopointsLoader.start()
            try {

                if (ExportKML && KMLfile != null) {
                    KMLfw = PrintWriter(KMLfile)
                    KMLbw = BufferedWriter(KMLfw)
                }
                if (ExportGPX && GPXfile != null) {
                    GPXfw = PrintWriter(GPXfile)
                    GPXbw = BufferedWriter(GPXfw)
                }
                if (ExportTXT && TXTfile != null) {
                    TXTfw = PrintWriter(TXTfile)
                    TXTbw = BufferedWriter(TXTfw)
                }
                creationTime = Calendar.getInstance().time

                // ---------------------------------------------------------------------- Writing Heads
                Log.w("myApp", "[#] Exporter.java - Writing Heads")
                if (ExportKML) {
                    // Writing head of KML file
                    KMLbw!!.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>$newLine")
                    KMLbw.write("<!-- Created with BasicAirData GPS Logger for Android - ver. $versionName -->$newLine")
                    KMLbw.write(
                        "<!-- Track " + track.id + " = " + track.numberOfLocations +
                                " TrackPoints + " + track.numberOfPlacemarks + " Placemarks -->" + newLine
                    )
                    KMLbw.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\">$newLine")
                    KMLbw.write(" <Document>$newLine")
                    KMLbw.write(
                        "  <name>GPS Logger " + track.name + "</name>" + newLine
                    )
                    KMLbw.write(
                        "  <description><![CDATA[" + track.numberOfLocations + " Trackpoints<br>" +
                                track.numberOfPlacemarks + " Placemarks]]></description>" + newLine
                    )
                    if (track.numberOfLocations > 0) {
                        KMLbw.write("  <Style id=\"TrackStyle\">$newLine")
                        KMLbw.write("   <LineStyle>$newLine")
                        KMLbw.write("    <color>ff0000ff</color>$newLine")
                        KMLbw.write("    <width>3</width>$newLine")
                        KMLbw.write("   </LineStyle>$newLine")
                        KMLbw.write("   <PolyStyle>$newLine")
                        KMLbw.write("    <color>7f0000ff</color>$newLine")
                        KMLbw.write("   </PolyStyle>$newLine")
                        KMLbw.write("   <BalloonStyle>$newLine")
                        KMLbw.write(
                            "    <text><![CDATA[<p style=\"color:red;font-weight:bold\">$[name]</p><p style=\"font-size:11px\">$[description]</p><p style=\"font-size:7px\">" +
                                    appContext.getString(R.string.pref_track_stats)
                                        .toString() + ": " +
                                    appContext
                                        .getString(R.string.pref_track_stats_totaltime)
                                        .toString() + " | " +
                                    appContext
                                        .getString(R.string.pref_track_stats_movingtime)
                                        .toString() + "</p>]]></text>" + newLine
                        )
                        KMLbw.write("   </BalloonStyle>$newLine")
                        KMLbw.write("  </Style>$newLine")
                    }
                    if (track.numberOfPlacemarks > 0) {
                        KMLbw.write("  <Style id=\"PlacemarkStyle\">$newLine")
                        KMLbw.write("   <IconStyle>$newLine")
                        KMLbw.write("    <Icon><href>http://maps.google.com/mapfiles/kml/shapes/placemark_circle_highlight.png</href></Icon>$newLine")
                        KMLbw.write("   </IconStyle>$newLine")
                        KMLbw.write("  </Style>$newLine")
                    }
                    KMLbw.write(newLine)
                }
                if (ExportGPX) {
                    // Writing head of GPX file
                    GPXbw!!.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>$newLine")
                    GPXbw.write("<!-- Created with BasicAirData GPS Logger for Android - ver. $versionName -->$newLine")
                    GPXbw.write(
                        "<!-- Track " + java.lang.String.valueOf(track.id) + " = " + java.lang.String.valueOf(
                            track.numberOfLocations
                        )
                                + " TrackPoints + " + java.lang.String.valueOf(track.numberOfPlacemarks) + " Placemarks -->" + newLine
                    )
                    if (getPrefGPXVersion == GPX1_0) {     // GPX 1.0
                        GPXbw.write(
                            "<gpx version=\"1.0\"" + newLine
                                    + "     creator=\"BasicAirData GPS Logger " + versionName + "\"" + newLine
                                    + "     xmlns=\"http://www.topografix.com/GPX/1/0\"" + newLine
                                    + "     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + newLine
                                    + "     xsi:schemaLocation=\"http://www.topografix.com/GPX/1/0 http://www.topografix.com/GPX/1/0/gpx.xsd\">" + newLine
                        )
                        GPXbw.write(
                            "<name>GPS Logger " + track.name + "</name>" + newLine
                        )
                        GPXbw.write("<time>" + dfdtGPX_NoMillis.format(creationTime) + "</time>" + newLine + newLine)
                    }
                    if (getPrefGPXVersion == GPX1_1) {    // GPX 1.1
                        GPXbw.write(
                            "<gpx version=\"1.1\"" + newLine
                                    + "     creator=\"BasicAirData GPS Logger " + versionName + "\"" + newLine
                                    + "     xmlns=\"http://www.topografix.com/GPX/1/1\"" + newLine
                                    + "     xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"" + newLine
                                    + "     xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">" + newLine
                        )
                        //          + "     xmlns:gpxx=\"http://www.garmin.com/xmlschemas/GpxExtensions/v3\"" + newLine           // Garmin extension to include speeds
                        //          + "     xmlns:gpxtrkx=\"http://www.garmin.com/xmlschemas/TrackStatsExtension/v1\"" + newLine  //
                        //          + "     xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\">" + newLine); //
                        GPXbw.write("<metadata> $newLine") // GPX Metadata
                        GPXbw.write(
                            " <name>GPS Logger " + track.name + "</name>" + newLine
                        )
                        GPXbw.write(" <time>" + dfdtGPX_NoMillis.format(creationTime) + "</time>" + newLine)
                        GPXbw.write("</metadata>$newLine$newLine")
                    }
                }
                if (ExportTXT) {
                    // Writing head of TXT file
                    TXTbw!!.write("type,date time,latitude,longitude,accuracy(m),altitude(m),geoid_height(m),speed(m/s),bearing(deg),sat_used,sat_inview,name,desc$newLine")
                }
                var formattedLatitude = ""
                var formattedLongitude = ""
                var formattedAltitude = ""
                var formattedSpeed: String? = ""

                // ---------------------------------------------------------------- Writing Placemarks
                Log.w("myApp", "[#] Exporter.java - Writing Placemarks")
                if (track.numberOfPlacemarks > 0) {
                    var placemark_id = 1 // It is used to add a progressive "id" to Placemarks

                    // Writes track headings
                    val placemarkList: MutableList<Point> =
                        ArrayList<Point>(GroupOfLocations)
                    var i = 0L
                    while (i <= track.numberOfPlacemarks) {

                        //Log.w("myApp", "[#] Exporter.java - " + (i + GroupOfLocations));

                        placemarkList.addAll(
                            pointDao.getListBetweenIds(
                                track.id,
                                i,
                                i + GroupOfLocations - 1
                            )
                        )

                        //TODO: retrieve placemark list
//                    placemarkList.addAll(
//                        GPSApp.GPSDataBase.getPlacemarksList(
//                            track.id,
//                            i,
//                            i + GroupOfLocations - 1
//                        )
//                    )

                        if (!placemarkList.isEmpty()) {
                            for (loc in placemarkList) {
                                formattedLatitude = java.lang.String.format(
                                    Locale.US,
                                    "%.8f",
                                    loc.latitude
                                )
                                formattedLongitude = java.lang.String.format(
                                    Locale.US,
                                    "%.8f",
                                    loc.longitude
                                )
                                if (loc.hasAltitude) formattedAltitude =
                                    java.lang.String.format(
                                        Locale.US,
                                        "%.3f",
                                        loc.altitude + AltitudeManualCorrection - if (loc.altitudeEgm96Correction == NOT_AVAILABLE.toDouble() || !EGMAltitudeCorrection) 0.0 else loc.altitudeEgm96Correction
                                    )
                                if (ExportGPX || ExportTXT) {
                                    if (loc.hasSpeed) formattedSpeed =
                                        java.lang.String.format(
                                            Locale.US, "%.3f", loc.speed
                                        )
                                }

                                // KML
                                if (ExportKML) {
                                    KMLbw!!.write("  <Placemark id=\"$placemark_id\">$newLine")
                                    KMLbw.write("   <name>")
                                    KMLbw.write(
                                        loc.description
                                            .replace("<", "&lt;")
                                            .replace("&", "&amp;")
                                            .replace(">", "&gt;")
                                            .replace("\"", "&quot;")
                                            .replace("'", "&apos;")
                                    )
                                    KMLbw.write("</name>$newLine")
                                    KMLbw.write("   <styleUrl>#PlacemarkStyle</styleUrl>$newLine")
                                    KMLbw.write("   <Point>$newLine")
                                    KMLbw.write("    <altitudeMode>" + (if (getPrefKMLAltitudeMode == 1) "clampToGround" else "absolute") + "</altitudeMode>" + newLine)
                                    KMLbw.write("    <coordinates>")
                                    if (loc.hasAltitude) {
                                        KMLbw.write("$formattedLongitude,$formattedLatitude,$formattedAltitude")
                                    } else {
                                        KMLbw.write("$formattedLongitude,$formattedLatitude,0")
                                    }
                                    KMLbw.write("</coordinates>$newLine")
                                    KMLbw.write("    <extrude>1</extrude>$newLine")
                                    KMLbw.write("   </Point>$newLine")
                                    KMLbw.write("  </Placemark>$newLine$newLine")
                                }

                                // GPX
                                if (ExportGPX) {
                                    GPXbw!!.write("<wpt lat=\"$formattedLatitude\" lon=\"$formattedLongitude\">")
                                    if (loc.hasAltitude) {
                                        GPXbw.write("<ele>") // Elevation
                                        GPXbw.write(formattedAltitude)
                                        GPXbw.write("</ele>")
                                    }
                                    GPXbw.write("<time>") // Time
                                    //GPXbw.write(dfdtGPX.format(loc.time));
                                    GPXbw.write(
                                        if (loc.timestamp % 1000L === 0L
                                        ) dfdtGPX_NoMillis.format(
                                            loc.timestamp
                                        ) else dfdtGPX.format(loc.timestamp)
                                    )
                                    GPXbw.write("</time>")
                                    GPXbw.write("<name>") // Name
                                    GPXbw.write(
                                        loc.description
                                            .replace("<", "&lt;")
                                            .replace("&", "&amp;")
                                            .replace(">", "&gt;")
                                            .replace("\"", "&quot;")
                                            .replace("'", "&apos;")
                                    )
                                    GPXbw.write("</name>")
                                    if (loc.numberOfSatellitesUsedInFix > 0) {     // Satellites used in fix
                                        GPXbw.write("<sat>")
                                        GPXbw.write(java.lang.String.valueOf(loc.numberOfSatellitesUsedInFix))
                                        GPXbw.write("</sat>")
                                    }
                                    GPXbw.write("</wpt>$newLine$newLine")
                                }

                                // TXT
                                if (ExportTXT) {
                                    //type,time,latitude,longitude,altitude (m),geoid_height (m),speed (m/s),sat_used,sat_inview,name,desc
                                    //TXTbw.write("W," + dfdtTXT.format(loc.time) + "," + formattedLatitude + "," + formattedLongitude + ",");
                                    TXTbw!!.write(
                                        "W," + (if (loc.timestamp % 1000L == 0L
                                        ) dfdtTXT_NoMillis.format(
                                            loc.timestamp
                                        ) else dfdtTXT.format(loc.timestamp))
                                                + "," + formattedLatitude + "," + formattedLongitude + ","
                                    )
                                    if (loc.hasAccuracy) TXTbw.write(
                                        java.lang.String.format(
                                            Locale.US, "%.0f", loc.accuracy
                                        )
                                    )
                                    TXTbw.write(",")
                                    if (loc.hasAltitude) TXTbw.write(formattedAltitude)
                                    TXTbw.write(",")
                                    if (loc.altitudeEgm96Correction != NOT_AVAILABLE.toDouble() && EGMAltitudeCorrection) TXTbw.write(
                                        java.lang.String.format(
                                            Locale.US, "%.3f", loc.altitudeEgm96Correction
                                        )
                                    )
                                    TXTbw.write(",")
                                    if (loc.hasSpeed) TXTbw.write(formattedSpeed)
                                    TXTbw.write(",")
                                    if (loc.hasBearing) TXTbw.write(
                                        java.lang.String.format(
                                            Locale.US, "%.0f", loc.bearing
                                        )
                                    )
                                    TXTbw.write(",")
                                    if (loc.numberOfSatellitesUsedInFix > 0) TXTbw.write(
                                        java.lang.String.valueOf(
                                            loc.numberOfSatellitesUsedInFix
                                        )
                                    )
                                    TXTbw.write(",")
                                    if (loc.numberOfSatellites > 0) TXTbw.write(
                                        java.lang.String.valueOf(
                                            loc.numberOfSatellites
                                        )
                                    )
                                    TXTbw.write(",")
                                    // Name is an empty field
                                    TXTbw.write(",")
                                    TXTbw.write(loc.description.replace(",", "_"))
                                    TXTbw.write(newLine)
                                }
                                placemark_id++
                                exportingTask.numberOfPoints_Processed++
                            }
                            placemarkList.clear()
                        }
                        i += GroupOfLocations
                    }
                    exportingTask.numberOfPoints_Processed = track.numberOfPlacemarks
                }


                // ---------------------------------------------------------------- Writing Track
                // Approximation: 0.00000001 = 0° 0' 0.000036"
                // On equator 1" ~= 31 m  ->  0.000036" ~= 1.1 mm
                // We'll use 1 mm also for approx. altitudes!
                Log.w("myApp", "[#] Exporter.java - Writing Trackpoints")
                if (track.numberOfLocations > 0) {

                    // Writes track headings
                    if (ExportKML) {
                        val phdformatter = PhysicalDataFormatter()
                        val phdDuration: PhysicalData
                        val phdDurationMoving: PhysicalData
                        val phdSpeedMax: PhysicalData
                        val phdSpeedAvg: PhysicalData
                        val phdSpeedAvgMoving: PhysicalData
                        val phdDistance: PhysicalData
                        val phdAltitudeGap: PhysicalData
                        val phdOverallDirection: PhysicalData
                        phdDuration = phdformatter.format(
                            track.duration,
                            PhysicalDataFormatter.FORMAT_DURATION
                        )
                        phdDurationMoving = phdformatter.format(
                            track.duration_Moving,
                            PhysicalDataFormatter.FORMAT_DURATION
                        )
                        phdSpeedMax =
                            phdformatter.format(track.speedMax, PhysicalDataFormatter.FORMAT_SPEED)
                        phdSpeedAvg = phdformatter.format(
                            track.speedAverage,
                            PhysicalDataFormatter.FORMAT_SPEED_AVG
                        )
                        phdSpeedAvgMoving = phdformatter.format(
                            track.speedAverageMoving,
                            PhysicalDataFormatter.FORMAT_SPEED_AVG
                        )
                        phdDistance = phdformatter.format(
                            track.estimatedDistance,
                            PhysicalDataFormatter.FORMAT_DISTANCE
                        )
                        phdAltitudeGap = phdformatter.format(
//                            track.getEstimatedAltitudeGap(GPSApp.prefEGM96AltitudeCorrection),
                            track.getEstimatedAltitudeGap(false),
                            PhysicalDataFormatter.FORMAT_ALTITUDE
                        )
                        phdOverallDirection = phdformatter.format(
                            track.bearing,
                            PhysicalDataFormatter.FORMAT_BEARING
                        )
                        val TrackDesc: String =
                            appContext.getString(R.string.distance)
                                .toString() + " = " + phdDistance.value + " " + phdDistance.um +
                                    "<br>" + appContext
                                .getString(R.string.duration) + " = " + phdDuration.value + " | " + phdDurationMoving.value +
                                    "<br>" + appContext
                                .getString(R.string.altitude_gap) + " = " + phdAltitudeGap.value + " " + phdAltitudeGap.um +
                                    "<br>" + appContext
                                .getString(R.string.max_speed) + " = " + phdSpeedMax.value + " " + phdSpeedMax.um +
                                    "<br>" + appContext
                                .getString(R.string.average_speed) + " = " + phdSpeedAvg.value + " | " + phdSpeedAvgMoving.value + " " + phdSpeedAvg.um +
                                    "<br>" + appContext
                                .getString(R.string.direction) + " = " + phdOverallDirection.value + " " + phdOverallDirection.um +
                                    "<br><br><i>" + track.numberOfLocations + " " + appContext
                                .getString(R.string.trackpoints) + "</i>"
                        KMLbw!!.write(
                            "  <Placemark id=\"" + track.name + "\">" + newLine
                        )
                        KMLbw.write(
                            "   <name>" + appContext
                                .getString(R.string.tab_track)
                                .toString() + " " + track.name + "</name>" + newLine
                        )
                        KMLbw.write("   <description><![CDATA[$TrackDesc]]></description>$newLine")
                        KMLbw.write("   <styleUrl>#TrackStyle</styleUrl>$newLine")
                        KMLbw.write("   <LineString>$newLine")
                        KMLbw.write("    <extrude>0</extrude>$newLine")
                        KMLbw.write("    <tessellate>0</tessellate>$newLine")
                        KMLbw.write("    <altitudeMode>" + (if (getPrefKMLAltitudeMode == 1) "clampToGround" else "absolute") + "</altitudeMode>" + newLine)
                        KMLbw.write("    <coordinates>$newLine")
                    }
                    if (ExportGPX) {
                        GPXbw!!.write("<trk>$newLine")
                        GPXbw.write(
                            " <name>" + appContext.getString(R.string.tab_track)
                                .toString() + " " + track.name + "</name>" + newLine
                        )
                        GPXbw.write(" <trkseg>$newLine")
                    }
                    var loc: Point
                    for (i in 0 until track.numberOfLocations) {
                        loc = arrayGeopoints.take()

                        // Create formatted strings
                        formattedLatitude =
                            java.lang.String.format(Locale.US, "%.8f", loc.latitude)
                        formattedLongitude =
                            java.lang.String.format(Locale.US, "%.8f", loc.longitude)
                        if (loc.hasAltitude) formattedAltitude =
                            java.lang.String.format(
                                Locale.US,
                                "%.3f",
                                loc.altitude +
                                        AltitudeManualCorrection -
                                        if (loc.altitudeEgm96Correction == NOT_AVAILABLE.toDouble() || !EGMAltitudeCorrection) 0.0
                                        else loc.altitudeEgm96Correction
                            )
                        if (ExportGPX || ExportTXT) {
                            if (loc.hasSpeed) formattedSpeed = java.lang.String.format(
                                Locale.US, "%.3f", loc.speed
                            )
                        }

                        // KML
                        if (ExportKML) {
                            if (loc.hasAltitude) {
                                KMLbw!!.write("     $formattedLongitude,$formattedLatitude,$formattedAltitude$newLine")
                            } else {
                                KMLbw!!.write("     $formattedLongitude,$formattedLatitude,0$newLine")
                            }
                        }

                        // GPX
                        if (ExportGPX) {
                            GPXbw!!.write("  <trkpt lat=\"$formattedLatitude\" lon=\"$formattedLongitude\">")
                            if (loc.hasAltitude) {
                                GPXbw.write("<ele>") // Elevation
                                GPXbw.write(formattedAltitude)
                                GPXbw.write("</ele>")
                            }
                            GPXbw.write("<time>") // Time
                            //GPXbw.write(dfdtGPX.format(loc.time));
                            GPXbw.write(
                                if (loc.timestamp % 1000L == 0L) dfdtGPX_NoMillis.format(
                                    loc.timestamp
                                ) else dfdtGPX.format(
                                    loc.timestamp
                                )
                            )
                            GPXbw.write("</time>")
                            if (getPrefGPXVersion == GPX1_0) {
                                if (loc.hasSpeed) {
                                    GPXbw.write("<speed>") // Speed
                                    GPXbw.write(formattedSpeed)
                                    GPXbw.write("</speed>")
                                }
                            }
                            if (loc.numberOfSatellitesUsedInFix > 0) {                   // GPX standards requires sats used for FIX.
                                GPXbw.write("<sat>") // and NOT the number of satellites in view!!!
                                GPXbw.write(java.lang.String.valueOf(loc.numberOfSatellitesUsedInFix))
                                GPXbw.write("</sat>")
                            }
                            /*
                            if (getPrefGPXVersion == GPX1_1) {                                // GPX 1.1 doesn't support speed tags. Let's switch to Garmin extensions :(
                                if (loc.hasSpeed) {
                                    GPXbw.write("<extensions><gpxtpx:TrackPointExtension><gpxtpx:speed>");     // Speed (as Garmin extension)
                                    GPXbw.write(formattedSpeed);
                                    GPXbw.write("</gpxtpx:speed></gpxtpx:TrackPointExtension></extensions>");
                                }
                            } */GPXbw.write("</trkpt>$newLine")
                        }

                        // TXT
                        if (ExportTXT) {
                            //type,time,latitude,longitude,altitude (m),geoid_height (m),speed (m/s),sat_used,sat_inview,name,desc
                            //TXTbw.write("T," + dfdtTXT.format(loc.time) + "," + formattedLatitude + "," + formattedLongitude + ",");
                            TXTbw!!.write(
                                "T," + (if (loc.timestamp % 1000L == 0L
                                ) dfdtTXT_NoMillis.format(
                                    loc.timestamp
                                ) else dfdtTXT.format(loc.timestamp))
                                        + "," + formattedLatitude + "," + formattedLongitude + ","
                            )
                            if (loc.hasAccuracy) TXTbw.write(
                                java.lang.String.format(
                                    Locale.US, "%.0f", loc.accuracy
                                )
                            )
                            TXTbw.write(",")
                            if (loc.hasAltitude) TXTbw.write(formattedAltitude)
                            TXTbw.write(",")
                            if (loc.altitudeEgm96Correction != NOT_AVAILABLE.toDouble() && EGMAltitudeCorrection) TXTbw.write(
                                java.lang.String.format(
                                    Locale.US, "%.3f", loc.altitudeEgm96Correction
                                )
                            )
                            TXTbw.write(",")
                            if (loc.hasSpeed) TXTbw.write(formattedSpeed)
                            TXTbw.write(",")
                            if (loc.hasBearing) TXTbw.write(
                                java.lang.String.format(
                                    Locale.US, "%.0f", loc.bearing
                                )
                            )
                            TXTbw.write(",")
                            if (loc.numberOfSatellitesUsedInFix > 0) TXTbw.write(
                                java.lang.String.valueOf(
                                    loc.numberOfSatellitesUsedInFix
                                )
                            )
                            TXTbw.write(",")
                            if (loc.numberOfSatellites > 0) TXTbw.write(
                                java.lang.String.valueOf(
                                    loc.numberOfSatellites
                                )
                            )
                            TXTbw.write(",")
                            if (TXTFirstTrackpointFlag) {           // First trackpoint of the track: add the description
                                TXTbw.write(
                                    track.name + ",GPS Logger: " + track.name
                                )
                                TXTFirstTrackpointFlag = false
                            } else TXTbw.write(",")
                            TXTbw.write(newLine)
                        }
                        exportingTask.numberOfPoints_Processed++
                    }
                    exportingTask.numberOfPoints_Processed =
                        track.numberOfPlacemarks + track.numberOfLocations
                    arrayGeopoints.clear()
                    if (ExportKML) {
                        KMLbw!!.write("    </coordinates>$newLine")
                        KMLbw.write("   </LineString>$newLine")
                        KMLbw.write("  </Placemark>$newLine$newLine")
                    }
                    if (ExportGPX) {
                        GPXbw!!.write(" </trkseg>$newLine")
                        GPXbw.write("</trk>$newLine$newLine")
                    }
                }


                // ------------------------------------------------------------ Writing tails and close
                Log.w("myApp", "[#] Exporter.java - Writing Tails and close files")
                if (ExportKML) {
                    KMLbw!!.write(" </Document>$newLine")
                    KMLbw.write("</kml>")
                    KMLbw.close()
                    KMLfw!!.close()
                }
                if (ExportGPX) {
                    GPXbw!!.write("</gpx>")
                    GPXbw.close()
                    GPXfw!!.close()
                }
                if (ExportTXT) {
                    TXTbw!!.close()
                    TXTfw!!.close()
                }
                Log.w(
                    "myApp",
                    "[#] Exporter.java - Track " + track.id
                        .toString() + " exported in " + (System.currentTimeMillis() - start_Time).toString() + " ms (" + elements_total.toString() + " pts @ " + (1000L * elements_total / (System.currentTimeMillis() - start_Time)).toString() + " pts/s)"
                )
                //EventBus.getDefault().post(new EventBusMSGNormal(EventBusMSG.TRACK_EXPORTED, track.id));
                exportingTask.status = ExportingTask.STATUS_ENDED_SUCCESS
            } catch (e: IOException) {
                exportingTask.status = ExportingTask.STATUS_ENDED_FAILED
                //EventBus.getDefault().post(new EventBusMSGNormal(EventBusMSG.TOAST_UNABLE_TO_WRITE_THE_FILE, track.id));
                asyncGeopointsLoader.interrupt()
                Log.w("myApp", "[#] Exporter.java - Unable to write the file: $e")
            } catch (e: InterruptedException) {
                exportingTask.status = ExportingTask.STATUS_ENDED_FAILED
                asyncGeopointsLoader.interrupt()
                Log.w("myApp", "[#] Exporter.java - Interrupted: $e")
            }
        }
    }

    private inner class AsyncGeopointsLoader : Thread() {
        override fun run() {
            CoroutineScope(IO).launch {
                val track = trackDao.getById(exportingTask.id).value!!
                val lList: MutableList<Point> = ArrayList<Point>(GroupOfLocations)
                var i = 0L
                while (i <= track.numberOfLocations) {

                    //Log.w("myApp", "[#] Exporter.java - " + (i + GroupOfLocations));
                    lList.addAll(
                        pointDao.getListBetweenIds(
                            track.id,
                            i,
                            i + GroupOfLocations - 1
                        )
                    )
                    if (!lList.isEmpty()) {
                        for (loc in lList) {
                            try {
                                arrayGeopoints.put(loc)
                                //Log.w("myApp", "[#] Exporter.java - " + ArrayGeopoints.size());
                            } catch (e: InterruptedException) {
                                Log.w("myApp", "[#] Exporter.java - Interrupted: $e")
                            }
                        }
                        lList.clear()
                    }
                    i += GroupOfLocations
                }
            }
        }
    }

    companion object {
        private const val NOT_AVAILABLE = -100000
    }

    init {
        this.exportingTask = exportingTask
        this.exportingTask.numberOfPoints_Processed = 0
        this.exportingTask.status = ExportingTask.STATUS_RUNNING

//        AltitudeManualCorrection = GeoTrailsApplication.instance!!.prefAltitudeCorrection
//        EGMAltitudeCorrection = GeoTrailsApplication.instance!!.prefEGM96AltitudeCorrection
//        getPrefKMLAltitudeMode = GeoTrailsApplication.instance!!.prefKMLAltitudeMode
//        getPrefGPXVersion = GeoTrailsApplication.instance!!.prefGPXVersion
        AltitudeManualCorrection = 0.0
        EGMAltitudeCorrection = false
        getPrefKMLAltitudeMode = 0
        getPrefGPXVersion = 1

        var Formats = 0
        if (ExportKML) Formats++
        if (ExportGPX) Formats++
        if (ExportTXT) Formats++
        if (Formats == 1) GroupOfLocations = 1500 else {
            GroupOfLocations = 1900
            if (ExportKML) GroupOfLocations -= 200 // KML is a light format, less time to write file
            if (ExportTXT) GroupOfLocations -= 800 //
            if (ExportGPX) GroupOfLocations -= 600 // GPX is the heavier format, more time to write the file
        }
        //GroupOfLocations = 300;
    }
}