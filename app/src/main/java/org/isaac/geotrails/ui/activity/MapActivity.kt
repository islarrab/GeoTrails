package org.isaac.geotrails.ui.activity

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.MAP_TYPE_HYBRID
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.android.synthetic.main.activity_map.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.isaac.geotrails.GeoTrailsApplication
import org.isaac.geotrails.R
import org.isaac.geotrails.arch.viewmodel.MapViewModel
import org.isaac.geotrails.arch.viewmodel.TrackListViewModel
import org.isaac.geotrails.entity.Track
import org.isaac.geotrails.eventbus.FirstFixEvent
import org.isaac.geotrails.eventbus.LocationEvent
import org.isaac.geotrails.service.GpsService
import org.isaac.geotrails.ui.adapter.TrackAdapter
import timber.log.Timber

@AndroidEntryPoint
class MapActivity : AppCompatActivity(), OnMapReadyCallback, TrackAdapter.Callbacks {

    companion object {
        private const val REQUEST_CODE_PLAY_SERVICES_RESOLUTION = 1

        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
    }

    // extension to retrieve application object
    val Activity.app: GeoTrailsApplication
        get() = application as GeoTrailsApplication

    private val trackListViewModel: TrackListViewModel by viewModels()
    private val mapViewModel: MapViewModel by viewModels()

    private lateinit var mapView: GoogleMap
    private lateinit var drawer: DrawerLayout
    private lateinit var drawerToggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.v("[#${this.hashCode()}] --- onCreate()")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        drawer = activity_main_drawer_layout
        drawerToggle = ActionBarDrawerToggle(
            this,
            drawer,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer.addDrawerListener(drawerToggle)
        drawerToggle.syncState()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeButtonEnabled(true)

//        activity_main_nav_view.setNavigationItemSelectedListener(this)

        // check necessary permissions
        if (!checkPermissions(REQUIRED_PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, 0)
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.activity_main_content) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // setup track list navigation drawer
        val trackList: MutableList<Track> = mutableListOf()
        val trackAdapter = TrackAdapter(trackList, this)

        val trackListRecycler = activity_main_nav_view.getHeaderView(0) as RecyclerView
        trackListRecycler.apply {
            adapter = trackAdapter
            layoutManager = LinearLayoutManager(this@MapActivity)
        }

        trackListViewModel.trackList.observe(this, {
            trackAdapter.dataSet = it
        })

        mapViewModel.pointList.observe(this) { pointList ->
            if (this::mapView.isInitialized) {
                mapView.clear()

                val color = resources.getColor(
                    R.color.recordButton,
                    resources.newTheme()
                )

                if (pointList.isNotEmpty()) {
                    val startPoint = pointList[0]
                    mapView.addMarker(
                        MarkerOptions().position(
                            LatLng(startPoint.latitude, startPoint.longitude)
                        )
                    )
                    mapView.addPolyline(
                        PolylineOptions()
                            .addAll(pointList.map { point ->
                                LatLng(
                                    point.latitude,
                                    point.longitude
                                )
                            })
                            .color(color)
                    )

                    val lastPoint =
                        LatLng(pointList.last().latitude, pointList.last().longitude)

                    mapView.addCircle(
                        CircleOptions()
                            .center(lastPoint)
                            .clickable(false)
                            .fillColor(color)
                            .strokeWidth(0.0f)
                            .visible(true)
                            .radius(2.0)
                    )

                    if (mapViewModel.recordingState.value == GpsService.State.STANDBY) {
                        mapView.addMarker(MarkerOptions().position(lastPoint))
                    }

                    mapView.animateCamera(CameraUpdateFactory.newLatLngZoom(lastPoint, 15f))
                }
            }
        }

        mapViewModel.recordingState.observe(this) { state ->
            when (state) {
                GpsService.State.STANDBY -> {
                    activity_main_start_button.setIconResource(R.drawable.ic_fiber_manual_record_white_18dp)
                    activity_main_stop_button.visibility = View.GONE
                }
                GpsService.State.RECORDING -> {
                    activity_main_start_button.setIconResource(R.drawable.ic_pause_white_18dp)
                    activity_main_stop_button.visibility = View.VISIBLE
                }
                GpsService.State.PAUSED -> {
                    activity_main_start_button.setIconResource(R.drawable.ic_play_arrow_white_18dp)
                    activity_main_stop_button.visibility = View.VISIBLE
                }
                GpsService.State.STOPPED -> {
                    activity_main_start_button.setIconResource(R.drawable.ic_fiber_manual_record_white_18dp)
                    activity_main_stop_button.visibility = View.GONE
                }
                null -> {
                    TODO("Display an error")
                }
            }
        }

        activity_main_start_button.setOnClickListener {
            mapViewModel.onStartButtonClick()
        }

        activity_main_stop_button.setOnClickListener {
            mapViewModel.onStopButtonClick()
        }
    }

    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    override fun onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Checks if all permissions given are granted
     * @param permissions Array of values from {@link Manifest.permission}
     * @return true if all permissions are granted, false otherwise
     */
    private fun checkPermissions(permissions: Array<String>): Boolean {
        // Check if each permission is not granted
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) ==
                PackageManager.PERMISSION_DENIED
            ) {
                return false
            }
        }
        return true
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mapView = googleMap
        mapView.mapType = MAP_TYPE_HYBRID

//        val pointDao = AppDatabase.getInstance(this).pointDao()
//        val pointListObserver = pointDao.getByTrackId(1)
//
//        val disposable = pointListObserver
//            .observeOn(AndroidSchedulers.mainThread())
//            .subscribe(
//                { pointList ->
//                    mMap.clear()
//
//                    if (pointList.isNotEmpty()) {
//                        val startPoint = pointList[0]
//                        mMap.addMarker(
//                            MarkerOptions().position(
//                                LatLng(startPoint.latitude, startPoint.longitude)
//                            )
//                        )
//                        mMap.addPolyline(PolylineOptions().addAll(pointList.map {
//                            LatLng(it.latitude, it.longitude)
//                        }))
//
//                        val lastPoint =
//                            LatLng(pointList.last().latitude, pointList.last().longitude)
//                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastPoint, 15f))
//                    }
//                },
//                { t ->
//                    Toast.makeText(
//                        this,
//                        "Error while observing the location list: ${t.message}",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                })
    }

    //        val latLng = locationEvent.latLng
//        if (prevLatLng == null) {
//            mMap.addMarker(MarkerOptions().position(latLng).title("Start"))
//        } else {
//            mMap.addPolyline(
//                PolylineOptions()
//                    .add(prevLatLng, latLng)
//                    .color(getColor(R.color.colorPrimary))
//            )
//        }
//        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
    override fun onTrackClick(track: Track, isSelected: Boolean) {
        trackListViewModel.onTrackClick(track, isSelected)
    }

    @Subscribe(sticky = true)
    fun onFirstFixEvent(firstFixEvent: FirstFixEvent) {
        mapView.moveCamera(CameraUpdateFactory.newLatLng(firstFixEvent.location))
    }

    private var prevLatLng: LatLng? = null


    @Subscribe()
    fun onLocationEvent(locationEvent: LocationEvent) {
//        if (this::mMap.isInitialized) {
//            mMap.addCircle(
//                CircleOptions()
//                    .center(locationEvent.latLng)
//                    .clickable(false)
//                    .fillColor(resources.getColor(R.color.colorPrimary, resources.newTheme()))
//                    .visible(true)
//                    .radius(1.0)
//            )
    }


//        prevLatLng = latLng

    private fun checkPlayServices(): Boolean {
        val apiAvailability = GoogleApiAvailability.getInstance()
        val resultCode = apiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(
                    this,
                    resultCode,
                    REQUEST_CODE_PLAY_SERVICES_RESOLUTION
                )
            } else {
                finish()
            }
            return false
        }
        return true
    }
}