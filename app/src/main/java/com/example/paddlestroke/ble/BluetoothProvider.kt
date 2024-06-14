package com.example.paddlestroke.ble

import android.content.Context
import com.example.paddlestroke.ble.BluetoothHandler.Companion.getInstance
import kotlinx.coroutines.channels.Channel

class BluetoothProvider(private val context: Context) {
    private val bluetoothHandler: BluetoothHandler = getInstance(context)

    fun getHeartRateChannel(): Channel<HeartRateMeasurement> {
        return bluetoothHandler.heartRateChannel
    }

}