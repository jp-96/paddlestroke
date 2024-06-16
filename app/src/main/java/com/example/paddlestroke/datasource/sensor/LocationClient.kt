package com.example.paddlestroke.datasource.sensor

import com.example.paddlestroke.data.DataRecord
import kotlinx.coroutines.flow.Flow

interface LocationClient {
    fun getLocationFlow(intervalMillis: Long): Flow<DataRecord>
    class LocationException(message: String) : Exception()
}
