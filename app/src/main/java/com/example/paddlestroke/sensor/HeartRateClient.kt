package com.example.paddlestroke.sensor

import com.example.paddlestroke.ble.HeartRateMeasurement
import kotlinx.coroutines.flow.Flow

interface HeartRateClient {
    fun getHeartRateFlow(intervalMillis: Long): Flow<HeartRateMeasurement>
    class HeartRateException(message: String) : Exception()
}