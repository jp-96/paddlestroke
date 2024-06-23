package com.example.paddlestroke

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.paddlestroke.data.DataRecord
import com.example.paddlestroke.datasource.ble.hasDevice
import com.example.paddlestroke.datasource.ble.isEnabled
import com.example.paddlestroke.service.RunningService
import com.example.paddlestroke.service.RunningService.Companion.ACTION_START
import com.example.paddlestroke.service.RunningService.Companion.ACTION_STOP_IF_NOT_RECORDING
import com.example.paddlestroke.service.RunningService.Companion.ACTION_START_RECORDING
import com.example.paddlestroke.service.RunningService.Companion.ACTION_STOP_RECORDING
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import pub.devrel.easypermissions.AppSettingsDialog
import pub.devrel.easypermissions.EasyPermissions
import timber.log.Timber
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.StringJoiner

class MainActivity : AppCompatActivity(), EasyPermissions.PermissionCallbacks {

    companion object {
        const val REQUEST_CODE_LOCATION_PERMISSION = 0
    }

    private lateinit var textViewX: TextView
    private lateinit var textViewY: TextView
    private lateinit var textViewZ: TextView

    private lateinit var textViewTime: TextView
    private lateinit var textViewLon: TextView
    private lateinit var textViewLat: TextView
    private lateinit var textViewAlt: TextView
    private lateinit var textViewAcc: TextView

    private lateinit var textViewBle: TextView

    //    private lateinit var dataRepository: DataRepository
    private var repositoryJob: Job? = null

    private var bufferedWriter: BufferedWriter? = null

    private val UPDATE_INTERVAL_MS: Long = 1000L
    private var lastUpdate: Long = 0L

    private var isFirstAskBLE = true

    private val enableBluetoothRequest =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == RESULT_OK) {
                // Bluetooth has been enabled
                startDataRepository()
            } else {
                // Bluetooth has not been enabled, try again
                askToEnableBluetooth()
            }
        }

    private fun askToEnableBluetooth() {
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        enableBluetoothRequest.launch(enableBtIntent)
    }

    private fun sendCommandToService(action: String) =
        Intent(applicationContext, RunningService.DummyRunningService::class.java).also {
            it.action = action
            applicationContext.startService(it)
        }

    private fun startDataRepository() {
        sendCommandToService(ACTION_START)
    }

    private fun stopDataRepository() {
        sendCommandToService(ACTION_STOP_IF_NOT_RECORDING)
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

        val buttonStart = findViewById<Button>(R.id.buttonStart)
        buttonStart.setOnClickListener{
            sendCommandToService(ACTION_START_RECORDING)
        }
        val buttonStop = findViewById<Button>(R.id.buttonStop)
        buttonStop.setOnClickListener{
            sendCommandToService(ACTION_STOP_RECORDING)
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

        requestPermissions()
    }

    override fun onResume() {
        super.onResume()

        lastUpdate = System.currentTimeMillis()

        val currentTime: Calendar = Calendar.getInstance()
        val formatter = SimpleDateFormat("yyyyMMddhhmmss", Locale.getDefault())
        val filename: String = "datalog_" + formatter.format(currentTime.getTime()) + ".csv"
        val file = File(getExternalFilesDir("logs"), filename)
        bufferedWriter = BufferedWriter(FileWriter(file))

        val bluetoothManager: BluetoothManager =
            getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        if (!bluetoothManager.hasDevice || bluetoothManager.isEnabled || !isFirstAskBLE) {
            startDataRepository()
        } else {
            isFirstAskBLE = false
            askToEnableBluetooth()
        }

        repositoryJob = RunningService.dataRecordFlow
            .onEach { dataRecord ->
                setDataRecordText(dataRecord)
            }.launchIn(lifecycleScope)

    }

    override fun onPause() {

        runBlocking {
            repositoryJob?.cancelAndJoin()
        }
        repositoryJob = null

        synchronized(this) {
            bufferedWriter?.write("onPause()")
            bufferedWriter?.newLine()
            bufferedWriter?.flush()
            bufferedWriter?.close()
        }

        stopDataRepository()

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
        textViewAlt.text = "速度：$spd"
    }

    private fun setHeartRateDataRecord(dataRecord: DataRecord) {
        Timber.i("setHeartRateMeasurement: %s", dataRecord)
        textViewBle.text = String.format("%d bpm", dataRecord.data)
    }


    fun hasLocationPermissions(context: Context) =
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.hasPermissions(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        } else {
            EasyPermissions.hasPermissions(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }

    private fun requestPermissions() {
        //if(hasLocationPermissions(requireContext())) {
        if(hasLocationPermissions(this)) {
            return
        }
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            EasyPermissions.requestPermissions(
                this,
                "You need to accept location permissions to use this app.",
                REQUEST_CODE_LOCATION_PERMISSION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            EasyPermissions.requestPermissions(
                this,
                "You need to accept location permissions to use this app.",
                REQUEST_CODE_LOCATION_PERMISSION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }
    }

    // EasyPermissions.PermissionCallbacks
    override fun onPermissionsGranted(requestCode: Int, perms: MutableList<String>) {
        // nop
    }

    // EasyPermissions.PermissionCallbacks
    override fun onPermissionsDenied(requestCode: Int, perms: MutableList<String>) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            AppSettingsDialog.Builder(this).build().show()
        } else {
            requestPermissions()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

}
