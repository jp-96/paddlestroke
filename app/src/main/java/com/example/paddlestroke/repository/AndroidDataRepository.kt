package com.example.paddlestroke.repository

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import com.example.paddlestroke.data.DataRecord
import com.example.paddlestroke.datasource.ble.AndroidBluetoothProvider
import com.example.paddlestroke.datasource.sensor.AndroidLocationClient
import com.example.paddlestroke.datasource.sensor.AndroidSensorClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import timber.log.Timber

class AndroidDataRepository :
    DataRepository {

    companion object {
        private var instance: AndroidDataRepository? = null
        fun getInstance(): AndroidDataRepository {
            if (instance == null) {
                instance = AndroidDataRepository()
            }
            return requireNotNull(instance)
        }
    }

    private val dataRecordFlow = MutableSharedFlow<DataRecord>()

    private var accelerometerJob: Job? = null
    private var locationJob: Job? = null
    private var heartRateJob: Job? = null

    override fun start(context: Context) {

        // Accelerometer
        if (accelerometerJob?.isActive != true) {
            val scopeSvIO = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val accelerometerClient = AndroidSensorClient(context, Sensor.TYPE_LINEAR_ACCELERATION)
            accelerometerJob = accelerometerClient.getSensorEventFlow(SensorManager.SENSOR_DELAY_UI)
                .catch { e -> e.printStackTrace() }
                .onEach { dataRecord ->
                    dataRecordFlow.emit(dataRecord)
                }
                .onCompletion { e -> Timber.d("Accelerometer Done: $e") }
                .launchIn(scopeSvIO)
        }

        // Location
        if (locationJob?.isActive != true) {
            val scopeSvIO = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val locationClient = AndroidLocationClient(context)
            locationJob = locationClient.getLocationFlow(1000L)
                .catch { e -> e.printStackTrace() }
                .onEach { dataRecord ->
                    dataRecordFlow.emit(dataRecord)
                }
                .onCompletion { e -> Timber.d("Location Done: $e") }
                .launchIn(scopeSvIO)
        }

        // BLE: HeartRate
        if (heartRateJob?.isActive != true) {
            val bleProvider = AndroidBluetoothProvider()
            val scopeSvIO = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            if (bleProvider.startIn(context, scopeSvIO)) {
                heartRateJob = bleProvider.getHeartRateChannel().receiveAsFlow()
                    .catch { e -> e.printStackTrace() }
                    .onEach { dataRecord ->
                        dataRecordFlow.emit(dataRecord)
                    }
                    .onCompletion { e ->
                        Timber.d("HearRate Done: $e")
                        bleProvider.stop()
                    }
                    .launchIn(scopeSvIO)
            }
        }

    }

    override fun stop() {
        runBlocking {
            accelerometerJob?.cancelAndJoin()
            locationJob?.cancelAndJoin()
            heartRateJob?.cancelAndJoin()
        }
        accelerometerJob = null
        locationJob = null
        heartRateJob = null

//        bleProvider!!.stop()
//        bleProvider = null
    }

    override fun getDataRecordFlow(): Flow<DataRecord> {
        return dataRecordFlow
    }

}
