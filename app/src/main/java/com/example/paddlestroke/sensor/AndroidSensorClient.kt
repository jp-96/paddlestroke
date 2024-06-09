package com.example.paddlestroke.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

open class AndroidSensorClient(
    private val context: Context,
    private val sensorType: Int
) : SensorClient {

    lateinit var sensorUpdate: Flow<SensorEvent>

    override fun getSensorUpdates(samplingPeriodUs: Int): Flow<SensorEvent> {

        sensorUpdate = callbackFlow {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent?) {
                    if (event?.sensor?.type == sensorType) {
                        launch { send(event) }
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }

            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val sensor = sensorManager.getDefaultSensor(sensorType)
                ?: throw SensorClient.SensorException("Missing sensor $sensorType")

            sensorManager.registerListener(listener, sensor, samplingPeriodUs)

            awaitClose {
                sensorManager.unregisterListener(listener, sensor)
            }
        }

        return sensorUpdate
    }
}
