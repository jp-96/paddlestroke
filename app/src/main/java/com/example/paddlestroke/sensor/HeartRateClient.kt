package com.example.paddlestroke.sensor

import com.welie.blessedexample.HeartRateMeasurement
import kotlinx.coroutines.flow.Flow

interface HeartRateClient {
    fun getHeartRateFlow(intervalMillis: Long): Flow<HeartRateMeasurement>
    class HeartRateException(message: String) : Exception()
}