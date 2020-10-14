package org.isaac.geotrails.arch.viewmodel

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import org.isaac.geotrails.di.qualifier.DefaultDispatcher
import org.isaac.geotrails.entity.Point
import org.isaac.geotrails.entity.Track
import org.isaac.geotrails.entity.repository.TrackRepository
import org.isaac.geotrails.service.GpsService
import org.isaac.geotrails.service.GpsService.LocalBinder
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


// for mutable list

class MapViewModel @ViewModelInject constructor(
    @ApplicationContext
    private val context: Context,
    private val trackRepository: TrackRepository,
    @DefaultDispatcher
    private val defaultDispatcher: CoroutineDispatcher
) : ViewModel(), ServiceConnection {

    private var gpsService: GpsService? = null

    private val _emptyPointList: LiveData<List<Point>> =
        liveData { emptyList<List<Point>>() }
//    private val _pointList: MutableLiveData<List<Point>> = MutableLiveData()

    val pointList: LiveData<List<Point>> = GpsService.TRACK.map { it as List<Point> }

    val recordingState: LiveData<GpsService.State> = GpsService.CURRENT_STATE

    init {
        initGpsService()
    }

    private var initGpsServiceJob: Deferred<Boolean>? = null
    private val initGpsServiceSemaphore: Semaphore = Semaphore(1, 1)

    private fun initGpsService() {
        if (checkPermissions(REQUIRED_PERMISSIONS) && initGpsServiceJob == null) {
            initGpsServiceJob = viewModelScope.async(defaultDispatcher) {
                val serviceIntent = Intent(context, GpsService::class.java)
                context.startService(serviceIntent)
                context.bindService(serviceIntent, this@MapViewModel, 0)
                initGpsServiceSemaphore.acquire()
                gpsService != null
            }
        }
    }

    private suspend fun onStartButtonClickAux(gpsService: GpsService) {
        when (recordingState.value) {
            GpsService.State.STANDBY, GpsService.State.STOPPED -> {
                val track = Track(
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                )
                val trackId = trackRepository.insertTrack(track)

                gpsService.startRecording(trackId)
            }
            GpsService.State.RECORDING -> {
                gpsService.pauseRecording()
            }
            GpsService.State.PAUSED -> {
                gpsService.continueRecording()
            }
            null -> TODO()
        }
    }

    fun onStartButtonClick() {
        viewModelScope.launch(defaultDispatcher) {
            if (checkPermissions(REQUIRED_PERMISSIONS)) {
                while (initGpsServiceJob == null || !initGpsServiceJob!!.await()) {
                    initGpsServiceJob = null
                    initGpsService()
                }
                onStartButtonClickAux(gpsService!!)
            }
        }
    }

    fun onStopButtonClick() {
        gpsService?.stopRecording()
    }

    override fun onCleared() {
        context.unbindService(this)
        super.onCleared()
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        gpsService = (service as LocalBinder).serviceInstance
        initGpsServiceSemaphore.release()
//        EventBus.getDefault().register(this)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        gpsService = null
//        EventBus.getDefault().unregister(this)
    }

    /**
     * Heavy operation that cannot be done in the Main Thread
     */
//    fun launchDataLoad(trackId: Long) {
//        viewModelScope.launch {
//            getPointList(trackId)
//             // TODO: Modify UI
//        }
//    }

//    private suspend fun getPointList(trackId: Long) = withContext(defaultDispatcher) {
//        pointList.value = pointsRepository.loadByTrackId(trackId)
//    }

    private fun checkPermissions(permissions: Array<String>): Boolean {
        // Check if each permission is not granted
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this.context, permission) ==
                PackageManager.PERMISSION_DENIED
            ) {
                return false
            }
        }
        return true
    }

    companion object {
        private const val REQUEST_CODE_PLAY_SERVICES_RESOLUTION = 1

        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
    }
}