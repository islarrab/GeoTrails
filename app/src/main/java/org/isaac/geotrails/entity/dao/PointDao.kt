package org.isaac.geotrails.entity.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import org.isaac.geotrails.entity.Point

/**
 * Data Access Object for the locations table.
 */
@Dao
interface PointDao : BaseDao<Point> {

    companion object {
        const val TABLE_NAME = "Points"
    }

    /**
     * Get a list of points by their track id, ordered by the timestamp, ascending
     * @return the location from the table with a specific id.
     */
    @Query("SELECT * FROM $TABLE_NAME WHERE track_id = :trackId ORDER BY timestamp ASC")
    fun getByTrackId(trackId: Long): LiveData<List<Point>>

    /**
     * Get a point by id.
     * @return the location from the table with a specific id.
     */
    @Query("SELECT * FROM $TABLE_NAME WHERE track_id = :trackId AND id >= :startId AND id <= :endId")
    fun getListBetweenIds(
        trackId: Long,
        startId: Long,
        endId: Long
    ): List<Point>

    @Query("SELECT * FROM $TABLE_NAME ORDER BY rowId DESC LIMIT 1")
    fun getLatestPoint(): Point

}