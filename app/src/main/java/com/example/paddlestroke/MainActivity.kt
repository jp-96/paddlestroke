package com.example.paddlestroke

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.StringJoiner

class MainActivity : AppCompatActivity() {

    private lateinit var sensorManager: SensorManager
    private var accelSensor: Sensor? = null
    private lateinit var mySensorEventListener: MySensorEventListener

    private lateinit var textViewX: TextView
    private lateinit var textViewY: TextView
    private lateinit var textViewZ: TextView

    private val UPDATE_INTERVAL: Int = 1000
    private var lastUpdate: Long = 0

    private val MIN_TIME: Long = 1000 * 10
    private val MIN_DISTANCE: Float = 10.0f

    private lateinit var textViewTime: TextView
    private lateinit var textViewLon: TextView
    private lateinit var textViewLat: TextView
    private lateinit var textViewAlt: TextView
    private lateinit var textViewAcc: TextView

    private lateinit var locationManager: LocationManager
    private lateinit var myLocationListener: MyLocationListener

    private lateinit var bufferedWriter: BufferedWriter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        textViewX = findViewById<View>(R.id.textViewX) as TextView
        textViewY = findViewById<View>(R.id.textViewY) as TextView
        textViewZ = findViewById<View>(R.id.textViewZ) as TextView


        val currentTime: Calendar = Calendar.getInstance()
        val formatter = SimpleDateFormat("yyyyMMddhhmmss")
        val filename: String = "datalog_" + formatter.format(currentTime.getTime()) + ".csv"
        val file = File(getExternalFilesDir("logs"), filename)
        bufferedWriter = BufferedWriter(FileWriter(file))

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelSensor == null) {
            Toast.makeText(this, "Missing TYPE_ACCELEROMETER", Toast.LENGTH_SHORT).show()
        }

        textViewTime = findViewById<View>(R.id.textViewTime) as TextView
        textViewLon = findViewById<View>(R.id.textViewLng) as TextView
        textViewLat = findViewById<View>(R.id.textViewLat) as TextView
        textViewAlt = findViewById<View>(R.id.textViewAlt) as TextView
        textViewAcc = findViewById<View>(R.id.textViewAcc) as TextView

        myLocationListener = MyLocationListener()

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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "PERMISSION ERROR: ACCESS_FINE_LOCATION", Toast.LENGTH_SHORT).show()
        } else {
            locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            if (null != locationManager.getProvider(LocationManager.GPS_PROVIDER)) {
                val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                setLocationText(location)
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME, MIN_DISTANCE, myLocationListener
                )
            }
        }
    }

    override fun onPause() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "PERMISSION ERROR: ACCESS_FINE_LOCATION", Toast.LENGTH_SHORT).show()
        } else {
            locationManager.removeUpdates(myLocationListener)
        }
        sensorManager.unregisterListener(mySensorEventListener)

        synchronized(this) {
            val writer = bufferedWriter
            writer.flush()
        }
        super.onPause()
    }

    fun addRecord(timestamp: Long, tag: String, numValues: Int, values: FloatArray) {
        // record timestamp, and values in text file
        val stringJoiner = StringJoiner(",") //StringBuilder()
        stringJoiner.add("$timestamp")
        stringJoiner.add(tag)
        for (i in 0 until numValues) {
            stringJoiner.add(String.format(Locale.US, "%.6f", values[i]))
        }
        synchronized(this) {
            val writer = bufferedWriter
            writer.write(stringJoiner.toString())
            writer.newLine()
        }
    }

    internal inner class MySensorEventListener : SensorEventListener {
        override fun onSensorChanged(sensorEvent: SensorEvent) {
            if (sensorEvent.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                val timestamp = sensorEvent.timestamp
                addRecord(timestamp, "acce",3, sensorEvent.values)

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

    private fun setLocationText(location: Location?) {
        if (location != null) {
            val time: String = SimpleDateFormat(
                "yyyy/MM/dd HH:mm:ss",
                Locale.getDefault()
            ).format(Date(location.time))
            val lon = location.longitude
            val lat = location.latitude
            textViewTime.text = "測定時刻：$time"
            textViewLon.text = "経度：$lon"
            textViewLat.text = "緯度：$lat"
            if (location.hasAccuracy()) {
                val acc = location.accuracy.toDouble()
                textViewAcc.text = "精度：$acc"
            }
            if (location.hasAltitude()) {
                val alt = location.altitude
                textViewAlt.text = "高度：$alt"
            }
        }
    }
    internal inner class MyLocationListener : LocationListener {
        override fun onLocationChanged(location: Location) {
            setLocationText(location)
        }
    }

}

