package org.isaac.geotrails.di.module

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.isaac.geotrails.di.qualifier.DefaultDispatcher
import org.isaac.geotrails.di.qualifier.IODispatcher
import org.isaac.geotrails.di.qualifier.MainDispatcher

@Module
@InstallIn(ApplicationComponent::class)
object DispatcherModule {

    @Provides
    @MainDispatcher
    fun mainDispatcher(): CoroutineDispatcher {
        return Dispatchers.Main
    }

    @Provides
    @DefaultDispatcher
    fun defaultDispatcher(): CoroutineDispatcher {
        return Dispatchers.Default
    }

    @Provides
    @IODispatcher
    fun ioDispatcher(): CoroutineDispatcher {
        return Dispatchers.IO
    }
}