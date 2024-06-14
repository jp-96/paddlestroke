package com.example.paddlestroke.ble

import android.content.Context
import com.welie.blessedexample.BluetoothHandler
import com.welie.blessedexample.BluetoothHandler.Companion.getInstance
import com.welie.blessedexample.HeartRateMeasurement
import kotlinx.coroutines.channels.Channel

class BluetoothProvider(private val context: Context) {
    private val bluetoothHandler: BluetoothHandler = getInstance(context)

    fun getHeartRateChannel(): Channel<HeartRateMeasurement> {
        return bluetoothHandler.heartRateChannel
    }

}