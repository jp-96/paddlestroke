package com.example.paddlestroke.repository

import android.content.Context
import com.example.paddlestroke.data.DataRecord
import kotlinx.coroutines.flow.Flow

interface DataRepository {
    fun start(context: Context)
    fun stop()
    fun getDataRecordFlow(): Flow<DataRecord>
}
