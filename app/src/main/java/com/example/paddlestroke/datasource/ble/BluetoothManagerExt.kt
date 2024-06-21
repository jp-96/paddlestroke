package com.example.paddlestroke.datasource.ble

import android.bluetooth.BluetoothManager

val BluetoothManager.hasDevice: Boolean
    get() {
        return this.adapter != null
    }

val BluetoothManager.isEnabled: Boolean
    get() {
        this.adapter?.let {
            return it.isEnabled
        }
        return false
    }
