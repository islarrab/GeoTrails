package org.isaac.geotrails.entity.repository

import androidx.lifecycle.LiveData
import kotlinx.coroutines.flow.Flow
import org.isaac.geotrails.entity.Point
import org.isaac.geotrails.entity.Track
import org.isaac.geotrails.entity.dao.PointDao
import org.isaac.geotrails.entity.dao.TrackDao
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackRepository @Inject constructor(
    private val trackDao: TrackDao,
    private val pointDao: PointDao
) {

    fun insertTrack(track: Track): Long {
        return trackDao.insert(track)
    }

    fun insertPoint(trackId: Long, point: Point): Long {
        val track = trackDao.getById(trackId).value
        track?.let {
            it.numberOfLocations++
            trackDao.insert(it)
        }
        point.trackId = trackId
        return pointDao.insert(point)
    }

    fun loadPointList(trackId: Long): LiveData<List<Point>> {
        return pointDao.getByTrackId(trackId)
    }

    fun loadLatestPoint(): Point {
        return pointDao.getLatestPoint()
    }

    fun loadAllTracks() : Flow<List<Track>> {
        return trackDao.getAllAsFlow()
    }
}