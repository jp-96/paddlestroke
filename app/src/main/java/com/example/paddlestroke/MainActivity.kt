package com.example.paddlestroke

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.paddlestroke.ble.BluetoothProvider
import com.example.paddlestroke.ble.HeartRateMeasurement
import com.example.paddlestroke.sensor.AndroidHeartRateClient
import com.example.paddlestroke.sensor.AndroidLocationClient
import com.example.paddlestroke.sensor.AndroidSensorClient
import com.example.paddlestroke.sensor.LocationClient
import com.example.paddlestroke.sensor.SensorClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.StringJoiner

class MainActivity : AppCompatActivity() {

    private lateinit var textViewX: TextView
    private lateinit var textViewY: TextView
    private lateinit var textViewZ: TextView

    private lateinit var textViewTime: TextView
    private lateinit var textViewLon: TextView
    private lateinit var textViewLat: TextView
    private lateinit var textViewAlt: TextView
    private lateinit var textViewAcc: TextView

    private lateinit var textViewBle: TextView

    private var bufferedWriter: BufferedWriter? = null

    private val UPDATE_INTERVAL_MS: Long = 1000L
    private var lastUpdate: Long = 0L

    private lateinit var accelerometerClient: SensorClient
    private var accelerometerJob: Job? = null

    private lateinit var locationClient: LocationClient
    private var locationJob: Job? = null

    private var heartRateJob: Job? = null

    private val bluetoothManager by lazy {
        applicationContext
            .getSystemService(BLUETOOTH_SERVICE)
                as BluetoothManager
    }

    private val isBluetoothEnabled: Boolean
        get() {
            val bluetoothAdapter = bluetoothManager.adapter ?: return false
            return bluetoothAdapter.isEnabled
        }

    private val enableBluetoothRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                // Bluetooth has been enabled
                //checkPermissions()
                startHeartRateJob()
            } else {
                // Bluetooth has not been enabled, try again
                askToEnableBluetooth()
            }
        }

    private fun askToEnableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBluetoothRequest.launch(enableBtIntent)
    }

    private fun startHeartRateJob() {
        if (heartRateJob == null) {
            // HeartRate
            val bleProvider = BluetoothProvider(this)
            val heartRateClient = AndroidHeartRateClient(this, bleProvider)
            // Job: HeartRate
            heartRateJob = heartRateClient.getHeartRateFlow()
                .catch { e -> e.printStackTrace() }
                .onEach { heartRateMeasurement ->
                    setHeartRateMeasurement(heartRateMeasurement)
                }.launchIn(MainScope())
        }
    }

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

        textViewTime = findViewById<View>(R.id.textViewTime) as TextView
        textViewLon = findViewById<View>(R.id.textViewLng) as TextView
        textViewLat = findViewById<View>(R.id.textViewLat) as TextView
        textViewAlt = findViewById<View>(R.id.textViewAlt) as TextView
        textViewAcc = findViewById<View>(R.id.textViewAcc) as TextView

        textViewBle = findViewById<View>(R.id.textViewBle) as TextView

        // Location
        val providerClient = LocationServices.getFusedLocationProviderClient(this)
        locationClient = AndroidLocationClient(this, providerClient)

        // Accelerometer
        //accelerometerClient = AndroidSensorClient(this, Sensor.TYPE_ACCELEROMETER)
        accelerometerClient = AndroidSensorClient(this, Sensor.TYPE_LINEAR_ACCELERATION)

    }

    override fun onResume() {
        super.onResume()

        lastUpdate = System.currentTimeMillis()

        val currentTime: Calendar = Calendar.getInstance()
        val formatter = SimpleDateFormat("yyyyMMddhhmmss")
        val filename: String = "datalog_" + formatter.format(currentTime.getTime()) + ".csv"
        val file = File(getExternalFilesDir("logs"), filename)
        bufferedWriter = BufferedWriter(FileWriter(file))

        // Job: Location
        locationJob = locationClient.getLocationFlow(1000L)
            .catch { e -> e.printStackTrace() }
            .onEach { location ->
                setLocationText(location)
            }.launchIn(lifecycleScope)

        // Job: Sensor
        accelerometerJob = accelerometerClient.getSensorEventFlow(SensorManager.SENSOR_DELAY_UI)
            .catch { e -> e.printStackTrace() }
            .onEach { sensorEvent ->
                val timestamp = sensorEvent.timestamp
                addRecord(timestamp, "acce", 3, sensorEvent.values)
                setAccelerometerText(sensorEvent)
            }.launchIn(lifecycleScope)

        // ble: HeartRate
        if (bluetoothManager.adapter != null) {
            if (!isBluetoothEnabled) {
                askToEnableBluetooth()
            } else {
                //checkPermissions()
                startHeartRateJob()
            }
        } else {
            Timber.e("This device has no Bluetooth hardware")
        }

    }

    override fun onPause() {

        runBlocking {
            accelerometerJob?.cancelAndJoin()
            locationJob?.cancelAndJoin()
            //heartRateJob?.cancelAndJoin()
        }
        accelerometerJob = null
        locationJob = null
        //heartRateJob = null

        synchronized(this) {
            bufferedWriter?.write("onPause()")
            bufferedWriter?.newLine()
            bufferedWriter?.flush()
            bufferedWriter?.close()
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
            writer?.write(stringJoiner.toString())
            writer?.newLine()
            writer?.flush()
        }
    }

    private fun setAccelerometerText(sensorEvent: SensorEvent) {

        val actualTime = System.currentTimeMillis()

        if (actualTime - lastUpdate > UPDATE_INTERVAL_MS) {
            lastUpdate = actualTime

            val x = sensorEvent.values[0]
            val y = sensorEvent.values[1]
            val z = sensorEvent.values[2]

            textViewX.text = x.toString()
            textViewY.text = y.toString()
            textViewZ.text = z.toString()
        }
    }

    private fun setLocationText(location: Location?) {
        if (location != null) {
            val time: String = SimpleDateFormat(
                "yyyy/MM/dd HH:mm:ss", Locale.getDefault()
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

    private fun setHeartRateMeasurement(heartRateMeasurement: HeartRateMeasurement) {
        textViewBle.text = String.format("%d bpm", heartRateMeasurement.pulse)
    }
}
