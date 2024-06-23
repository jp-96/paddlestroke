package com.example.paddlestroke.datasource.ble

import android.bluetooth.BluetoothManager
import android.content.Context
import com.example.paddlestroke.data.DataRecord
import com.example.paddlestroke.datasource.ble.BluetoothHandler.Companion.getInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel

class AndroidBluetoothProvider(private val context: Context) {

    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private lateinit var bluetoothHandler: BluetoothHandler

    val hasDevice: Boolean
        get() {
            return bluetoothManager.hasDevice
        }

    val isEnabled: Boolean
        get() {
            return bluetoothManager.isEnabled
        }

    fun startIn(scope: CoroutineScope) {
        bluetoothHandler = getInstance(context, scope)
    }

    fun stop() {
        bluetoothHandler.central.close()
    }

    fun getHeartRateChannel(): Channel<DataRecord> {
        return bluetoothHandler.heartRateChannel
    }

}
