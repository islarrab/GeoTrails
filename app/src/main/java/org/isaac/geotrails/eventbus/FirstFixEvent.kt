package org.isaac.geotrails.eventbus

import com.google.android.gms.maps.model.LatLng

data class FirstFixEvent(var location: LatLng)