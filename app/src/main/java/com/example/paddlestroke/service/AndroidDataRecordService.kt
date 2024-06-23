package com.example.paddlestroke.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import com.example.paddlestroke.MainActivity
import com.example.paddlestroke.R
import com.example.paddlestroke.repository.AndroidDataRepository
import com.example.paddlestroke.repository.DataRepository
import com.example.paddlestroke.service.DataRecordService.Companion.ACTION_START
import com.example.paddlestroke.service.DataRecordService.Companion.ACTION_STOP_IF_NOT_IN_SESSION
import com.example.paddlestroke.service.DataRecordService.Companion.NOTIFICATION_CHANNEL_ID
import com.example.paddlestroke.service.DataRecordService.Companion.NOTIFICATION_CHANNEL_NAME
import com.example.paddlestroke.service.DataRecordService.Companion.NOTIFICATION_ID
import com.example.paddlestroke.service.DataRecordService.Companion.dataRecordFlow
import com.example.paddlestroke.service.DataRecordService.Companion.isInSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

class AndroidDataRecordService : LifecycleService() {

//    companion object {
//        const val ACTION_START = "ACTION_START"
//        const val ACTION_STOP = "ACTION_STOP"
//
//        const val NOTIFICATION_ID = 1
//        const val NOTIFICATION_CHANNEL_ID = "running_channel"
//        const val NOTIFICATION_CHANNEL_NAME = "running"
//
//        val isRunning = MutableLiveData<Boolean>()
//        val dataRecordFlow = MutableSharedFlow<DataRecord>()
//
//    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    lateinit var dataRepository: DataRepository

    @SuppressLint("ObsoleteSdkInt")
    private fun startRunningService() {
        if (isInSession.value == true) return

        val mainActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Logging ...")
            .setContentText("Time: 00:00:00, Dist: 0M")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentIntent(mainActivityPendingIntent)
            .setOngoing(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                    as NotificationManager
            notificationManager.createNotificationChannel(
                NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    NOTIFICATION_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }

        startForeground(NOTIFICATION_ID, notification.build())

        dataRepository.getDataRecordFlow()
            .onEach { dataRecord ->
                dataRecordFlow.emit(dataRecord)
            }
            .launchIn(serviceScope)

        dataRepository.start()
    }

    private fun stopRunningService() {
        // stopForeground(true)
        stopForeground(STOP_FOREGROUND_DETACH)
        stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        Timber.i("onCreate")

        isInSession.postValue(false)
        dataRepository = AndroidDataRepository(this, serviceScope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            Timber.i("onStartCommand $it")
            when (it.action) {
                ACTION_START -> startRunningService()
                ACTION_STOP_IF_NOT_IN_SESSION -> stopRunningService()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

}
