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
import androidx.lifecycle.MutableLiveData
import com.example.paddlestroke.MainActivity
import com.example.paddlestroke.R
import com.example.paddlestroke.data.DataRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import timber.log.Timber

abstract class RunningService : LifecycleService() {
    enum class ServiceState {
        NONE,
        ACTIVE,
        RECORDING,
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP_IF_NOT_RECORDING = "ACTION_STOP"
        const val ACTION_START_RECORDING = "ACTION_START_RECORDING"
        const val ACTION_STOP_RECORDING = "ACTION_STOP_RECORDING"

        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_CHANNEL_ID = "running_channel"
        const val NOTIFICATION_CHANNEL_NAME = "running"

        val isRunning = MutableLiveData<Boolean>()
        val dataRecordFlow = MutableSharedFlow<DataRecord>()

    }

    protected val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    protected var currentState: ServiceState = ServiceState.NONE

    override fun onCreate() {
        super.onCreate()
        Timber.i("onCreate")

        isRunning.postValue(false)
    }

    protected abstract fun onStartRunningService()

    private fun startRunningService() {
        // callback
        onStartRunningService()
        currentState = ServiceState.ACTIVE
    }

    protected abstract fun onStopRunningService()

    private fun stopRunningService() {
        // callback
        onStopRunningService()

        stopSelf()
        currentState = ServiceState.NONE
    }

    protected abstract fun onStartRecording(notification: NotificationCompat.Builder)

    @SuppressLint("ObsoleteSdkInt")
    private fun startRecording() {

        val notification = NotificationCompat.Builder(
            this,
            NOTIFICATION_CHANNEL_ID
        )
            .setContentTitle("Recording ...")
            .setSmallIcon(R.drawable.ic_launcher_background)
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

        // callback
        onStartRecording(notification)

        startForeground(NOTIFICATION_ID, notification.build())

        currentState = ServiceState.RECORDING
    }

    protected abstract fun onStopRecording()

    private fun stopRecording() {

        // callback
        onStopRecording()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        //stopForeground(true)
        stopForeground(STOP_FOREGROUND_REMOVE)
        //stopForeground(STOP_FOREGROUND_DETACH)
        currentState = ServiceState.ACTIVE

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            Timber.i("onStartCommand $it")
            when (it.action) {
                ACTION_START -> {
                    when (currentState) {
                        ServiceState.NONE -> startRunningService()
                        else -> Unit
                    }
                }

                ACTION_STOP_IF_NOT_RECORDING -> {
                    when (currentState) {
                        ServiceState.ACTIVE -> stopRunningService()
                        else -> Unit
                    }
                }

                ACTION_START_RECORDING -> {
                    when (currentState) {
                        ServiceState.ACTIVE -> startRecording()
                        else -> Unit
                    }
                }

                ACTION_STOP_RECORDING -> {
                    when (currentState) {
                        ServiceState.RECORDING -> stopRecording()
                        else -> Unit
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        Timber.d("onDestroy()")
        serviceScope.cancel()
        super.onDestroy()
    }

    class DummyRunningService : RunningService() {
        override fun onStartRunningService() {
            Timber.d("onStartRunningService()")
        }

        override fun onStopRunningService() {
            Timber.d("onStopRunningService()")
        }

        override fun onStartRecording(notification: NotificationCompat.Builder) {
            Timber.d("onStartRecording()")

            val mainActivityPendingIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            notification.setContentIntent(mainActivityPendingIntent)

        }

        override fun onStopRecording() {
            Timber.d("onStopRecording()")
        }

    }

}