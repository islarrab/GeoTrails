package org.isaac.geotrails.di.qualifier

import javax.inject.Qualifier

@Qualifier
annotation class MainDispatcher {}

@Qualifier
annotation class DefaultDispatcher {}

@Qualifier
annotation class IODispatcher {}
