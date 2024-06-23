package com.example.paddlestroke.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.example.paddlestroke.data.DataRecord
import com.example.paddlestroke.datasource.ble.AndroidBluetoothProvider
import com.example.paddlestroke.datasource.sensor.AndroidLocationClient
import com.example.paddlestroke.datasource.sensor.AndroidSensorClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow

class AndroidDataRepository(private val context: Context) :
    DataRepository {

    private val dataRecordFlow = MutableSharedFlow<DataRecord>()

    private val accelerometerClient = AndroidSensorClient(context, Sensor.TYPE_LINEAR_ACCELERATION)

    private val providerClient = LocationServices.getFusedLocationProviderClient(context)
    private val locationClient = AndroidLocationClient(context, providerClient)

    private var bleProvider = AndroidBluetoothProvider(context)

    private var accelerometerJob: Job? = null
    private var locationJob: Job? = null
    private var heartRateJob: Job? = null

    override fun startIn(scope: CoroutineScope) {

        // Accelerometer
        accelerometerJob = accelerometerClient.getSensorEventFlow(SensorManager.SENSOR_DELAY_UI)
            .catch { e -> e.printStackTrace() }
            .onEach { dataRecord ->
                dataRecordFlow.emit(dataRecord)
            }.launchIn(scope)

        // Location
        locationJob = locationClient.getLocationFlow(1000L)
            .catch { e -> e.printStackTrace() }
            .onEach { dataRecord ->
                dataRecordFlow.emit(dataRecord)
            }.launchIn(scope)

        // BLE: HeartRate
        if (bleProvider.isEnabled) {
            bleProvider.startIn(scope)
            heartRateJob = bleProvider.getHeartRateChannel().receiveAsFlow()
                .catch { e -> e.printStackTrace() }
                .onEach { dataRecord ->
                    dataRecordFlow.emit(dataRecord)
                }.launchIn(scope)
        }
    }

    override fun getDataRecordFlow(): Flow<DataRecord> {
        return dataRecordFlow
    }

}
