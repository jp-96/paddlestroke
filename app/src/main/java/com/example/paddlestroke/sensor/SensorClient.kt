package com.example.paddlestroke.sensor

import android.hardware.SensorEvent
import kotlinx.coroutines.flow.Flow

interface SensorClient {
    fun getSensorUpdates(samplingPeriodUs: Int): Flow<SensorEvent>
    class SensorException(message: String): Exception()
}
