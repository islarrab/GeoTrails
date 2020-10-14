package org.isaac.geotrails.service

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
import android.location.*
import android.os.*
import android.os.PowerManager.WakeLock
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.isaac.geotrails.R
import org.isaac.geotrails.ui.activity.MapActivity
import org.isaac.geotrails.entity.Point
import org.isaac.geotrails.entity.repository.TrackRepository
import org.isaac.geotrails.eventbus.FirstFixEvent
import org.isaac.geotrails.eventbus.LocationEvent
import org.isaac.geotrails.util.add
import org.isaac.geotrails.util.clear
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class GpsService : Service(), LocationListener {

    @Inject
    lateinit var trackRepository: TrackRepository

    companion object {
        const val ARG_TRACK_ID: String = "ARG_TRACK_ID"
        const val ACTION_START = "ACTION_START"
        const val ACTION_RECORD_START = "ACTION_RECORD_START"
        const val ACTION_RECORD_PAUSE = "ACTION_RECORD_PAUSE"
        const val ACTION_RECORD_PLAY = "ACTION_RECORD_PLAY"
        const val ACTION_RECORD_STOP = "ACTION_RECORD_STOP"
        const val ACTION_STOP = "ACTION_STOP"

        private const val CHANNEL_ID = "GeoTrails.GpsService"

        val CURRENT_STATE: MutableLiveData<State> = MutableLiveData(State.STANDBY)
        val TRACK: MutableLiveData<MutableList<Point>> = MutableLiveData(mutableListOf())
    }

    inner class LocalBinder : Binder() {
        // returns the instance of the service
        val serviceInstance: GpsService
            get() = this@GpsService
    }

    inner class GnssStatusCallbacks : GnssStatus.Callback() {
        override fun onStarted() {
//            Timber.v("[#${this.hashCode()}] --- onStarted()")
            super.onStarted()
        }

        override fun onStopped() {
//            Timber.v("[#${this.hashCode()}] --- onStopped()")
            super.onStopped()
        }

        override fun onFirstFix(ttffMillis: Int) {
//            Timber.v("#${this.hashCode()}] --- onFirstFix()")
            super.onFirstFix(ttffMillis)
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            if (ActivityCompat.checkSelfPermission(
                    this@GpsService,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (location != null) {
                    EventBus.getDefault()
                        .postSticky(FirstFixEvent(LatLng(location.latitude, location.longitude)))
                }
            }
        }

        override fun onSatelliteStatusChanged(status: GnssStatus?) {
//            Timber.v("[#${this.hashCode()}] --- onSatelliteStatusChanged()")
            super.onSatelliteStatusChanged(status)
        }
    }

    inner class GnssMeasurementsEventCallbacks : GnssMeasurementsEvent.Callback() {
        override fun onGnssMeasurementsReceived(eventArgs: GnssMeasurementsEvent?) {
//            Timber.v("[#${this.hashCode()}] --- onGnssMeasurementsReceived()")
            super.onGnssMeasurementsReceived(eventArgs)
        }

        override fun onStatusChanged(status: Int) {
//            Timber.v("[#${this.hashCode()}] --- onStatusChanged()")
            super.onStatusChanged(status)
        }
    }

    private val _binder: IBinder = LocalBinder()
    private val _gnssStatusCallbacks: GnssStatusCallbacks = GnssStatusCallbacks()
    private val _gnssMeasurementCallbacks: GnssMeasurementsEventCallbacks =
        GnssMeasurementsEventCallbacks()

    private lateinit var _locationManager: LocationManager
    private lateinit var _wakeLock: WakeLock

    private val notification: Notification
        get() {
            val builder = NotificationCompat.Builder(this, CHANNEL_ID)

            val startIntent = Intent(applicationContext, MapActivity::class.java)
            startIntent.action = Intent.ACTION_MAIN
            startIntent.addCategory(Intent.CATEGORY_LAUNCHER)
            startIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            val contentIntent = PendingIntent.getActivity(applicationContext, 1, startIntent, 0)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create the NotificationChannel
                val name = getString(R.string.gps_service_channel_name)
                val descriptionText = getString(R.string.gps_service_channel_description)
                val importance = NotificationManager.IMPORTANCE_LOW
                val mChannel = NotificationChannel(CHANNEL_ID, name, importance)
                mChannel.description = descriptionText
                // Register the channel with the system; you can't change the importance
                // or other notification behaviors after this
                val notificationManager =
                    getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(mChannel)
            }


            builder.setSmallIcon(R.mipmap.ic_notify_24dp)
                .setColor(resources.getColor(R.color.colorPrimary, resources.newTheme()))
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.notification_contenttext))
                .setTicker(getString(R.string.app_name))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setShowWhen(false)
                .setOngoing(true)
                .setContentIntent(contentIntent)

            return builder.build()
        }

    private var _trackId: Long = 0L

    private val checkPermission: Boolean
        get() = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    override fun onCreate() {
        Timber.v("[#${this.hashCode()}] --- onCreate()")
        super.onCreate()

        _locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        try {
            _locationManager.registerGnssStatusCallback(_gnssStatusCallbacks)
            _locationManager.registerGnssMeasurementsCallback(_gnssMeasurementCallbacks)
            _locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 100L, 1.0f, this)
        } catch (e: SecurityException) {
            //ignore
        }

        // Initialize a new partial wake lock
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        _wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "GeoTrails:wakelock")
    }

    private var isRecording: Boolean = false

    fun startRecording(trackId: Long) {
        CURRENT_STATE.postValue(State.RECORDING)
        _trackId = trackId
        if (_trackId != 0L) {
            isRecording = true
        }
        TRACK.clear()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notification, FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notification)
        }
    }

    fun pauseRecording() {
        CURRENT_STATE.postValue(State.PAUSED)
        isRecording = false
    }

    fun continueRecording() {
        CURRENT_STATE.postValue(State.RECORDING)
        isRecording = true
    }

    fun stopRecording() {
        CURRENT_STATE.postValue(State.STANDBY)
        isRecording = false

        try {
            _locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } catch (e:SecurityException) {
            //ignore
        }

        stopForeground(true)
    }

    /**
     * Convenience method to acquire wake lock
     */
    private fun acquireWakeLock() {
        if (!_wakeLock.isHeld) {
            _wakeLock.acquire(10 * 60 * 1000L /*10 minutes*/)
            Timber.v("[#${this.hashCode()}] --- WakeLock acquired, timeout for 10 minutes")
        }
    }

    /**
     * Convenience method to release wake lock
     */
    private fun releaseWakeLock() {
        if (_wakeLock.isHeld) {
            _wakeLock.release()
            Timber.v("[#${this.hashCode()}] --- WakeLock released")
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        Timber.v("[#${this.hashCode()}] --- onBind()")
        acquireWakeLock()
        return _binder
    }

    override fun onRebind(intent: Intent?) {
        acquireWakeLock()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        releaseWakeLock()
        return true
    }

    override fun onDestroy() {
        Timber.v("[#${this.hashCode()}] --- onDestroy()")
        releaseWakeLock()
        _locationManager.unregisterGnssStatusCallback(_gnssStatusCallbacks)
        _locationManager.unregisterGnssMeasurementsCallback(_gnssMeasurementCallbacks)
        super.onDestroy()
    }

    override fun onLocationChanged(location: Location?) {
        Timber.v("[#${this.hashCode()}] --- onLocationChanged()")
        Timber.d("location = (${location?.latitude}, ${location?.longitude})")
        if (location != null) {
            if (isRecording) {
                val point = Point(location)

                TRACK.add(point)

                CoroutineScope(IO).launch {
                    trackRepository.insertPoint(_trackId, point)
                }
            }
            EventBus.getDefault().post(LocationEvent(LatLng(location.latitude, location.longitude)))
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        Timber.v("[#${this.hashCode()}] --- onStatusChanged()")
    }

    override fun onProviderEnabled(provider: String?) {
        Timber.v("[#${this.hashCode()}] --- onProviderEnabled()")
    }

    override fun onProviderDisabled(provider: String?) {
        Timber.v("[#${this.hashCode()}] --- onProviderDisabled()")
    }

    enum class State {
        STANDBY, RECORDING, PAUSED, STOPPED
    }
}
