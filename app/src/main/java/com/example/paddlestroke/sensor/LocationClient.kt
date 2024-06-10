package com.example.paddlestroke.sensor

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface LocationClient {
    fun getLocationFlow(intervalMillis: Long): Flow<Location>
    class LocationException(message: String) : Exception()
}
