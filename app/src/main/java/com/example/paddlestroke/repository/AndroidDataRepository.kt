package com.example.paddlestroke.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.example.paddlestroke.data.DataRecord
import com.example.paddlestroke.datasource.ble.AndroidBluetoothProvider
import com.example.paddlestroke.datasource.sensor.AndroidLocationClient
import com.example.paddlestroke.datasource.sensor.AndroidSensorClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber

class AndroidDataRepository(private val context: Context) :
    DataRepository {

    private val dataRecordFlow = MutableSharedFlow<DataRecord>()

    private val accelerometerClient = AndroidSensorClient(context, Sensor.TYPE_LINEAR_ACCELERATION)

    private val locationClient = AndroidLocationClient(context)

    private var bleProvider = AndroidBluetoothProvider(context)

    private var accelerometerJob: Job? = null
    private var locationJob: Job? = null
    private var heartRateJob: Job? = null

    override fun startIn(scope: CoroutineScope) {

        // Accelerometer
        if (accelerometerJob?.isActive != true) {
            accelerometerJob = accelerometerClient.getSensorEventFlow(SensorManager.SENSOR_DELAY_UI)
                .catch { e -> e.printStackTrace() }
                .onEach { dataRecord ->
                    dataRecordFlow.emit(dataRecord)
                }
                .onCompletion { e -> Timber.d("Accelerometer Done: $e") }
                .launchIn(scope)
        }

        // Location
        if (locationJob?.isActive != true) {
            locationJob = locationClient.getLocationFlow(1000L)
                .catch { e -> e.printStackTrace() }
                .onEach { dataRecord ->
                    dataRecordFlow.emit(dataRecord)
                }
                .onCompletion { e -> Timber.d("Location Done: $e") }
                .launchIn(scope)
        }

        // BLE: HeartRate
        if (heartRateJob?.isActive != true) {
            if (bleProvider.isEnabled) {
                bleProvider.startIn(scope)
                heartRateJob = bleProvider.getHeartRateChannel().receiveAsFlow()
                    .catch { e -> e.printStackTrace() }
                    .onEach { dataRecord ->
                        dataRecordFlow.emit(dataRecord)
                    }
                    .onCompletion { e -> Timber.d("HearRate Done: $e") }
                    .launchIn(scope)
            }
        }
    }

    override fun getDataRecordFlow(): Flow<DataRecord> {
        return dataRecordFlow
    }

}
