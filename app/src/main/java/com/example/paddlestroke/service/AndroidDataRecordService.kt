package com.example.paddlestroke.service

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.paddlestroke.MainActivity
import com.example.paddlestroke.data.SessionDataRecorder
import com.example.paddlestroke.repository.AndroidDataRepository.Companion.getInstance
import com.example.paddlestroke.repository.DataRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AndroidDataRecordService : DataRecordService() {

    private val sessionDataRecorder = SessionDataRecorder()
    private lateinit var dataRepository: DataRepository
    private var serviceJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        dataRepository = getInstance()
    }

    override fun onDestroy() {

        super.onDestroy()
    }

    override fun onStartDataRecordService() {
        if (serviceJob?.isActive == true) return
        val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        serviceJob = dataRepository.getDataRecordFlow()
            .onEach { dataRecord ->
                sessionDataRecorder.log(dataRecord)
                dataRecordFlow.emit(dataRecord)
            }
            .launchIn(serviceScope)
        dataRepository.start(this)
    }

    override fun onStopDataRecordService() {
        dataRepository.stop()

        runBlocking {
            serviceJob?.cancelAndJoin()
        }
        serviceJob = null
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
