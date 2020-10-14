package org.isaac.geotrails.database


import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import org.isaac.geotrails.entity.Point
import org.isaac.geotrails.entity.Track
import org.isaac.geotrails.entity.dao.PointDao
import org.isaac.geotrails.entity.dao.TrackDao

/**
 * The Room database
 */
@Database(entities = [Track::class, Point::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun trackDao(): TrackDao
    abstract fun pointDao(): PointDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java, "database.db"
            ).build()
    }
}