package com.example.paddlestroke.datasource.ble

import android.bluetooth.BluetoothManager
import android.content.Context
import com.example.paddlestroke.data.DataRecord
import com.example.paddlestroke.datasource.ble.BluetoothHandler.Companion.getInstance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel

class BluetoothProvider(private val context: Context, private val scope: CoroutineScope) {
    private val bluetoothManager: BluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothHandler by lazy {
        getInstance(context, scope)
    }

    val hasDevice: Boolean
        get() {
            return bluetoothManager.adapter != null
        }

    val isEnabled: Boolean
        get() {
            val bluetoothAdapter = bluetoothManager.adapter ?: return false
            return bluetoothAdapter.isEnabled
        }

    fun getHeartRateChannel(): Channel<DataRecord> {
        return bluetoothHandler.heartRateChannel
    }

}