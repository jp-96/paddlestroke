package com.example.paddlestroke.datasource.ble

import android.content.Context
import com.example.paddlestroke.data.DataRecord
import com.example.paddlestroke.datasource.ble.BluetoothHandler.Companion.closeInstance
import com.example.paddlestroke.datasource.ble.BluetoothHandler.Companion.getInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking

class AndroidBluetoothProvider() {

    private var bluetoothHandler: BluetoothHandler? = null

    fun startIn(context: Context, scope: CoroutineScope): Boolean {
        stop()
        bluetoothHandler = getInstance(context, scope)
        return bluetoothHandler!!.central.isBluetoothEnabled
    }

    fun stop() {
        closeInstance()
        bluetoothHandler = null
    }

    fun getHeartRateChannel(): Channel<DataRecord> {
        return bluetoothHandler!!.heartRateChannel
    }

}
