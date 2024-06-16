package com.example.paddlestroke.repository

import com.example.paddlestroke.data.DataRecord
import kotlinx.coroutines.flow.Flow

interface DataRepository {
    fun getDataRecordFlow(): Flow<DataRecord>
}
