package com.example.paddlestroke

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var sensorManager: SensorManager
    private var accelSensor: Sensor? = null
    private lateinit var mySensorEventListener: MySensorEventListener

    private lateinit var textViewX: TextView
    private lateinit var textViewY: TextView
    private lateinit var textViewZ: TextView

    private val UPDATE_INTERVAL: Int = 1000
    private var lastUpdate: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textViewX = findViewById<View>(R.id.textViewX) as TextView
        textViewY = findViewById<View>(R.id.textViewY) as TextView
        textViewZ = findViewById<View>(R.id.textViewZ) as TextView

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelSensor == null) {
            Toast.makeText(this, "Missing TYPE_ACCELEROMETER", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (accelSensor != null) {
            mySensorEventListener = MySensorEventListener()
            sensorManager.registerListener(
                mySensorEventListener,
                accelSensor, SensorManager.SENSOR_DELAY_UI
            )
        }
        lastUpdate = System.currentTimeMillis()
    }

    override fun onPause() {
        sensorManager.unregisterListener(mySensorEventListener)
        super.onPause()
    }

    internal inner class MySensorEventListener : SensorEventListener {
        override fun onSensorChanged(sensorEvent: SensorEvent) {
            if (sensorEvent.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val actualTime = System.currentTimeMillis()
                if (actualTime - lastUpdate > UPDATE_INTERVAL) {
                    lastUpdate = actualTime

                    val x = sensorEvent.values[0]
                    val y = sensorEvent.values[1]
                    val z = sensorEvent.values[2]

                    textViewX.text = x.toString()
                    textViewY.text = y.toString()
                    textViewZ.text = z.toString()
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }
}

