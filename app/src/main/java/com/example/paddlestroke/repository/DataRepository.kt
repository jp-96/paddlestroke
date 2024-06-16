package com.example.paddlestroke.repository

import com.example.paddlestroke.data.DataRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

interface DataRepository {
    fun isBluetoothEnabled(): Boolean
    fun start(scope: CoroutineScope): Unit
    fun getDataRecordFlow(): Flow<DataRecord>

}
