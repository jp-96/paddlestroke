package com.example.paddlestroke.datasource.sensor

import com.example.paddlestroke.data.DataRecord
import kotlinx.coroutines.flow.Flow

interface SensorClient {
    fun getSensorEventFlow(samplingPeriodUs: Int): Flow<DataRecord>
    class SensorException(message: String) : Exception()
}
