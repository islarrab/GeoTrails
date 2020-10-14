package org.isaac.geotrails.entity.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import org.isaac.geotrails.entity.Track

/**
 * Data Access Object for the users table.
 */
@Dao
interface TrackDao : BaseDao<Track> {

    companion object {
        const val TABLE_NAME = "Tracks"
    }

    @Query("SELECT * FROM $TABLE_NAME WHERE id = :id")
    fun getById(id: Long): LiveData<Track>

    /**
     * Retrieve all saved tracks, ordered by start time descending.
     */
    @Query("SELECT * FROM $TABLE_NAME ORDER BY start_time DESC")
    fun getAllDescendingAsLiveData(): LiveData<List<Track>>

    /**
     * Retrieve all saved tracks, ordered by start time descending.
     */
    @Query("SELECT * FROM $TABLE_NAME ORDER BY start_time DESC")
    fun getAllAsFlow(): Flow<List<Track>>

    /**
     * Retrieve all saved tracks, ordered by start time descending.
     */
    @Query("SELECT * FROM $TABLE_NAME ORDER BY start_time DESC")
    suspend fun getAllDescending(): List<Track>

}
