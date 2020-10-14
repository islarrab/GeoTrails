package org.isaac.geotrails.ui.adapter

import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import org.isaac.geotrails.R
import org.isaac.geotrails.entity.PhysicalData
import org.isaac.geotrails.entity.PhysicalDataFormatter
import org.isaac.geotrails.entity.Track
import org.isaac.geotrails.ui.adapter.TrackAdapter.TrackHolder

internal class TrackAdapter(
    private val _dataSet: MutableList<Track>,
    private val _callbacks: Callbacks
) : RecyclerView.Adapter<TrackHolder>() {

    interface Callbacks {
        fun onTrackClick(track: Track, isSelected: Boolean)
    }

    var dataSet: List<Track>
        get() {
            return _dataSet
        }
        set(value) {
            _dataSet.clear()
            _dataSet.addAll(value)
            notifyDataSetChanged()
        }

    private val colorMatrixColorFilter = ColorMatrixColorFilter(NEGATIVE)

    private var isLightTheme = false
    private var StartAnimationTime: Long = 0
    private var PointsCount: Long = 0L

    internal inner class TrackHolder(itemView: View) : RecyclerView.ViewHolder(itemView),
        View.OnClickListener {

        private val phdformatter: PhysicalDataFormatter = PhysicalDataFormatter()
        private val card: CardView
        private val textViewTrackName: TextView

        //        private val textViewTrackDescription: TextView
        private val textViewTrackLength: TextView
        private val textViewTrackDuration: TextView
        private val textViewTrackAltitudeGap: TextView
        private val textViewTrackMaxSpeed: TextView
        private val textViewTrackAverageSpeed: TextView
        private val textViewTrackGeopoints: TextView
        private val textViewTrackPlacemarks: TextView
        private val imageViewThumbnail: ImageView
        private val imageViewPulse: ImageView
        private val imageViewIcon: ImageView

        private var phd: PhysicalData? = null
        private var track: Track? = null
        private var trackType = 0
        private var isSelected = false

        override fun onClick(v: View) {
            val track = _dataSet[adapterPosition]
            isSelected = !isSelected
            _callbacks.onTrackClick(track, isSelected)
            notifyItemChanged(adapterPosition)
        }

        fun UpdateTrackStats(trk: Track) {
            //textViewTrackName.setText(trk.getName());
            if (trk.numberOfLocations >= 1) {
                phd = phdformatter.format(
                    trk.estimatedDistance,
                    PhysicalDataFormatter.FORMAT_DISTANCE
                )
                textViewTrackLength.setText(phd?.value + " " + phd?.um)
                phd = phdformatter.format(trk.prefTime, PhysicalDataFormatter.FORMAT_DURATION)
                textViewTrackDuration.setText(phd?.value)
                phd = phdformatter.format(
                    trk.getEstimatedAltitudeGap(false), PhysicalDataFormatter.FORMAT_ALTITUDE
                )
                textViewTrackAltitudeGap.setText(phd?.value + " " + phd?.um)
                phd = phdformatter.format(trk.speedMax, PhysicalDataFormatter.FORMAT_SPEED)
                textViewTrackMaxSpeed.setText(phd?.value + " " + phd?.um)
                phd = phdformatter.format(
                    trk.prefSpeedAverage,
                    PhysicalDataFormatter.FORMAT_SPEED_AVG
                )
                textViewTrackAverageSpeed.setText(phd?.value.toString() + " " + phd?.um)
            } else {
                textViewTrackLength.text = ""
                textViewTrackDuration.text = ""
                textViewTrackAltitudeGap.text = ""
                textViewTrackMaxSpeed.text = ""
                textViewTrackAverageSpeed.text = ""
            }
            textViewTrackGeopoints.text = trk.numberOfLocations.toString()
            textViewTrackPlacemarks.text = trk.numberOfPlacemarks.toString()
            trackType = trk.trackType
            if (trackType != NOT_AVAILABLE) imageViewIcon.setImageResource(trackTypeResId[trackType]) else imageViewIcon.setImageBitmap(
                null
            )

//            if (GeoTrailsApplication.instance!!.getRecording()) {
//                imageViewThumbnail.setImageResource(bmpCurrentTrackRecording)
//                imageViewPulse.visibility = View.VISIBLE
//                if (PointsCount != trk.numberOfLocations + trk.numberOfPlacemarks && System.currentTimeMillis() - StartAnimationTime >= 700L) {
//                    PointsCount = trk.numberOfLocations + trk.numberOfPlacemarks
//                    val sunRise = AnimationUtils.loadAnimation(
//                        GeoTrailsApplication.instance!!.getApplicationContext(), R.anim.record_pulse
//                    )
//                    imageViewPulse.startAnimation(sunRise)
//                    StartAnimationTime = System.currentTimeMillis()
//                }
//            } else {
            imageViewPulse.visibility = View.INVISIBLE
            imageViewThumbnail.setImageResource(bmpCurrentTrackPaused)
//            }
        }

        fun bindTrack(trk: Track) {
            val context = card.context
            track = trk
            card.isSelected = track!!.isSelected
            imageViewPulse.visibility = View.INVISIBLE
            textViewTrackName.text = track!!.name
//            textViewTrackDescription.text = context.getString(R.string.track_id) + " " + track!!.id
            if (trk.numberOfLocations >= 1) {
                phd = phdformatter.format(
                    track!!.estimatedDistance,
                    PhysicalDataFormatter.FORMAT_DISTANCE
                )
                textViewTrackLength.text = phd?.value + " " + phd?.um
                phd = phdformatter.format(track!!.prefTime, PhysicalDataFormatter.FORMAT_DURATION)
                textViewTrackDuration.text = phd?.value
                phd = phdformatter.format(
                    track!!.getEstimatedAltitudeGap(false), PhysicalDataFormatter.FORMAT_ALTITUDE
                )
                textViewTrackAltitudeGap.text = phd?.value + " " + phd?.um
                phd = phdformatter.format(track!!.speedMax, PhysicalDataFormatter.FORMAT_SPEED)
                textViewTrackMaxSpeed.text = phd?.value.toString() + " " + phd?.um
                phd = phdformatter.format(
                    track!!.prefSpeedAverage,
                    PhysicalDataFormatter.FORMAT_SPEED_AVG
                )
                textViewTrackAverageSpeed.text = phd?.value + " " + phd?.um
            } else {
                textViewTrackLength.text = ""
                textViewTrackDuration.text = ""
                textViewTrackAltitudeGap.text = ""
                textViewTrackMaxSpeed.text = ""
                textViewTrackAverageSpeed.text = ""
            }
            textViewTrackGeopoints.text = track!!.numberOfLocations.toString()
            textViewTrackPlacemarks.text = track!!.numberOfPlacemarks.toString()
            trackType = trk.trackType

            if (trackType != NOT_AVAILABLE) {
                imageViewIcon.setImageResource(trackTypeResId[trackType])
            } else {
                imageViewIcon.setImageResource(0)
            }

//            if (GeoTrailsApplication.instance!!.currentTrack?.id == track!!.id) {
//                imageViewThumbnail.setImageResource(
//                    if (isRecording) bmpCurrentTrackRecording
//                    else bmpCurrentTrackPaused
//                )
//            } else {
//                Glide.with(GeoTrailsApplication.instance!!.applicationContext)
//                    .load(
//                        GeoTrailsApplication.instance!!.applicationContext.filesDir.toString() +
//                                "/Thumbnails/" + track!!.id + ".png"
//                    )
//                    .diskCacheStrategy(DiskCacheStrategy.NONE) //.skipMemoryCache(true)
//                    .error(android.R.drawable.stat_notify_error) // TODO: change id
//                    .dontAnimate()
//                    .into(imageViewThumbnail)
//            }
        }

        init {
            itemView.setOnClickListener(this)

            // CardView
            card = itemView.findViewById(R.id.card_view)

            // TextViews
            textViewTrackName = itemView.findViewById(R.id.id_textView_card_TrackName)
//            textViewTrackDescription = itemView.findViewById(R.id.id_textView_card_TrackDesc)
            textViewTrackLength = itemView.findViewById(R.id.id_textView_card_length)
            textViewTrackDuration = itemView.findViewById(R.id.id_textView_card_duration)
            textViewTrackAltitudeGap = itemView.findViewById(R.id.id_textView_card_altitudegap)
            textViewTrackMaxSpeed = itemView.findViewById(R.id.id_textView_card_maxspeed)
            textViewTrackAverageSpeed = itemView.findViewById(R.id.id_textView_card_averagespeed)
            textViewTrackGeopoints = itemView.findViewById(R.id.id_textView_card_geopoints)
            textViewTrackPlacemarks = itemView.findViewById(R.id.id_textView_card_placemarks)

            // ImageViews
            imageViewThumbnail = itemView.findViewById(R.id.id_imageView_card_minimap)
            imageViewPulse = itemView.findViewById(R.id.id_imageView_card_pulse)
            imageViewIcon = itemView.findViewById(R.id.id_imageView_card_tracktype)
            if (isLightTheme) {
                imageViewThumbnail.colorFilter = colorMatrixColorFilter
                imageViewPulse.colorFilter = colorMatrixColorFilter
                imageViewIcon.colorFilter = colorMatrixColorFilter
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackHolder {
        return TrackHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.card_trackinfo, parent, false)
        )
    }

    override fun onBindViewHolder(holder: TrackHolder, position: Int) {
        holder.bindTrack(_dataSet[position])
    }

    override fun getItemCount(): Int {
        return _dataSet.size
    }

    companion object {
        private val NEGATIVE = floatArrayOf(
            -1.0f,
            0f,
            0f,
            0f,
            248f,
            0f,
            -1.0f,
            0f,
            0f,
            248f,
            0f,
            0f,
            -1.0f,
            0f,
            248f,
            0f,
            0f,
            0f,
            1.00f,
            0f
        )
        private const val NOT_AVAILABLE = -100000


        private val trackTypeResId = arrayOf(
            R.mipmap.ic_place_white_24dp,
            R.mipmap.ic_directions_walk_white_24dp,
            R.mipmap.ic_terrain_white_24dp,
            R.mipmap.ic_directions_run_white_24dp,
            R.mipmap.ic_directions_bike_white_24dp,
            R.mipmap.ic_directions_car_white_24dp,
            R.mipmap.ic_flight_white_24dp
        )

        private val bmpCurrentTrackRecording = R.mipmap.ic_recording_48dp
        private val bmpCurrentTrackPaused = R.mipmap.ic_paused_white_48dp
    }
}