package com.example.paddlestroke.datasource.sensor

import android.hardware.SensorEvent
import kotlinx.coroutines.flow.Flow

interface SensorClient {
    fun getSensorEventFlow(samplingPeriodUs: Int): Flow<SensorEvent>
    class SensorException(message: String) : Exception()
}
