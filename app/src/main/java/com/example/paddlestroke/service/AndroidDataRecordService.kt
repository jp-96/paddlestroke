package com.example.paddlestroke.service

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.paddlestroke.MainActivity
import com.example.paddlestroke.data.SessionDataRecorder
import com.example.paddlestroke.repository.AndroidDataRepository
import com.example.paddlestroke.repository.DataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AndroidDataRecordService : DataRecordService() {

    private val sessionDataRecorder = SessionDataRecorder()
    private lateinit var dataRepository: DataRepository
    private var serviceScope: CoroutineScope? = null

    override fun onCreate() {
        super.onCreate()
        dataRepository = AndroidDataRepository(this)
    }

    override fun onDestroy() {

        super.onDestroy()
    }

    override fun onStartDataRecordService() {
        if (serviceScope != null) return
        serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        dataRepository.getDataRecordFlow()
            .onEach { dataRecord ->
                sessionDataRecorder.log(dataRecord)
                dataRecordFlow.emit(dataRecord)
            }
            .launchIn(serviceScope!!)
        dataRepository.startIn(serviceScope!!)
    }

    override fun onStopDataRecordService() {
        if (serviceScope == null) return
        serviceScope?.cancel()
        serviceScope = null
    }

    override fun onStartSession(notification: NotificationCompat.Builder) {
        val mainActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        notification
            .setContentIntent(mainActivityPendingIntent)
            .setOngoing(true)

        // start session
        val currentTime: Calendar = Calendar.getInstance()
        val formatter = SimpleDateFormat("yyyyMMddhhmmss", Locale.getDefault())
        val filename: String = "session_" + formatter.format(currentTime.getTime()) + ".txt"
        val file = File(getExternalFilesDir("logs"), filename)
        sessionDataRecorder.open(file)
    }

    override fun onStopSession() {
        // stop session
        sessionDataRecorder.close()
    }
}
