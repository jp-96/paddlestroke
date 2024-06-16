package com.example.paddlestroke

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.paddlestroke.data.DataRecord
import com.example.paddlestroke.datasource.ble.BluetoothProvider
import com.example.paddlestroke.datasource.sensor.AndroidLocationClient
import com.example.paddlestroke.datasource.sensor.AndroidSensorClient
import com.example.paddlestroke.datasource.sensor.LocationClient
import com.example.paddlestroke.datasource.sensor.SensorClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
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

    private lateinit var bleProvider: BluetoothProvider
    private var heartRateJob: Job? = null

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
            heartRateJob = bleProvider.getHeartRateChannel().receiveAsFlow()
                .catch { e -> e.printStackTrace() }
                .onEach { dataRecord ->
                    setHeartRateDataRecord(dataRecord)
                }.launchIn(lifecycleScope)
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

        // Ble: HeartRate
//        bleProvider = BluetoothProvider(this, CoroutineScope(SupervisorJob() + Dispatchers.IO))
        bleProvider = BluetoothProvider(this, lifecycleScope)
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
            .onEach { dataRecord ->
                setDataRecordText(dataRecord)
            }.launchIn(lifecycleScope)

        // Job: Sensor
        accelerometerJob = accelerometerClient.getSensorEventFlow(SensorManager.SENSOR_DELAY_UI)
            .catch { e -> e.printStackTrace() }
            .onEach { dataRecord ->
                setDataRecordText(dataRecord)
            }.launchIn(lifecycleScope)

        // ble: HeartRate
        textViewBle.text = "---"
        if (bleProvider.isEnabled) {
            //checkPermissions()
            startHeartRateJob()
        } else {
            askToEnableBluetooth()
        }

    }

    override fun onPause() {

        runBlocking {
            accelerometerJob?.cancelAndJoin()
            locationJob?.cancelAndJoin()
            heartRateJob?.cancelAndJoin()
        }
        accelerometerJob = null
        locationJob = null
        heartRateJob = null

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

    private fun setDataRecordText(dataRecord: DataRecord) {
        when (dataRecord.type) {
            DataRecord.Type.ACCEL -> setAccelerometerText(dataRecord)
            DataRecord.Type.LOCATION -> setLocationText(dataRecord)
            DataRecord.Type.HEART_BPM -> setHeartRateDataRecord(dataRecord)
            else -> Unit
        }
    }

    private fun setAccelerometerText(dataRecord: DataRecord) {
        // ファイル出力
        val timestamp = dataRecord.timestamp
        addRecord(timestamp, "acce", 3, dataRecord.data as FloatArray)

        val actualTime = System.currentTimeMillis()
        if (actualTime - lastUpdate > UPDATE_INTERVAL_MS) {
            lastUpdate = actualTime
            val arr = dataRecord.data as FloatArray
            val x = arr[0]
            val y = arr[1]
            val z = arr[2]

            textViewX.text = x.toString()
            textViewY.text = y.toString()
            textViewZ.text = z.toString()
        }
    }

    private fun setLocationText(dataRecord: DataRecord) {
        val time: String = SimpleDateFormat(
            "yyyy/MM/dd HH:mm:ss", Locale.getDefault()
        ).format(Date(dataRecord.timestamp))
        // data: DoubleArray 0- "lat" 1- "long" 2- "alt" 3- "speed" 4- "bearing" 5- "accuracy"
        val arr = dataRecord.data as DoubleArray
        val lat = arr[0]
        val lon = arr[1]
        val alt = arr[2]
        val spd = arr[3]
        val bea = arr[4]
        val acc = arr[5]
        textViewTime.text = "測定時刻：$time"
        textViewLon.text = "経度：$lon"
        textViewLat.text = "緯度：$lat"
        textViewAcc.text = "精度：$acc"
        textViewAlt.text = "速度:$spd"
    }

    private fun setHeartRateDataRecord(dataRecord: DataRecord) {
        Timber.i("setHeartRateMeasurement: %s", dataRecord)
        textViewBle.text = String.format("%d bpm", dataRecord.data)
    }

}
