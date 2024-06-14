package com.example.paddlestroke.sensor

import android.content.Context
import com.example.paddlestroke.ble.BluetoothProvider
import com.example.paddlestroke.ble.HeartRateMeasurement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow

class AndroidHeartRateClient(
    private val context: Context,
    private val provider: BluetoothProvider
): HeartRateClient {
    override fun getHeartRateFlow(intervalMillis: Long): Flow<HeartRateMeasurement> {
        return provider.getHeartRateChannel().consumeAsFlow()
    }
}