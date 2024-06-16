package com.example.paddlestroke.datasource.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.example.paddlestroke.data.DataRecord
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class AndroidSensorClient(
    private val context: Context, private val sensorType: Int
) : SensorClient {

    companion object {
        fun createDataRecord(event: SensorEvent): DataRecord {
            // data: FloatArray 0-x, 1-y, 2-z
            return DataRecord.create(DataRecord.Type.ACCEL, event.timestamp, event.values)
        }
    }

    override fun getSensorEventFlow(samplingPeriodUs: Int) = callbackFlow {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == sensorType) {
                    trySend(createDataRecord(event))
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
}
