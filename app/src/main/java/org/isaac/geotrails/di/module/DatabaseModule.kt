package org.isaac.geotrails.di.module

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import org.isaac.geotrails.database.AppDatabase
import org.isaac.geotrails.entity.dao.PointDao
import org.isaac.geotrails.entity.dao.TrackDao
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext appContext: Context): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            "database.db"
        ).build()
    }

    @Provides
    fun trackDao(database: AppDatabase): TrackDao = database.trackDao()

    @Provides
    fun pointDao(database: AppDatabase): PointDao = database.pointDao()

//    @Provides
//    fun trackRepository(trackDao: TrackDao) = TrackRepository(trackDao)
//
//    @Provides
//    fun pointRepository(pointDao: PointDao) = PointsRepository(pointDao)
}