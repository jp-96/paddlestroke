package com.example.paddlestroke.datasource.sensor

import android.location.Location
import kotlinx.coroutines.flow.Flow

interface LocationClient {
    fun getLocationFlow(intervalMillis: Long): Flow<Location>
    class LocationException(message: String) : Exception()
}
