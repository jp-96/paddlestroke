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
import kotlinx.coroutines.flow.MutableSharedFlow
import timber.log.Timber

abstract class DataRecordService : LifecycleService() {
    enum class ServiceState {
        NONE,
        ACTIVE,
        IN_SESSION,
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP_IF_NOT_IN_SESSION = "ACTION_STOP_IF_NOT_IN_SESSION"
        const val ACTION_START_SESSION = "ACTION_START_SESSION"
        const val ACTION_STOP_SESSION = "ACTION_STOP_SESSION"

        const val NOTIFICATION_ID = 1
        const val NOTIFICATION_CHANNEL_ID = "in_session_channel"
        const val NOTIFICATION_CHANNEL_NAME = "in session"

        val isInSession = MutableLiveData<Boolean>()
        val dataRecordFlow = MutableSharedFlow<DataRecord>()

    }

    protected var currentState: ServiceState = ServiceState.NONE

    override fun onCreate() {
        super.onCreate()
        Timber.d("onCreate()")
        initDataRecordService()
    }

    override fun onDestroy() {
        super.onDestroy()
        Timber.d("onDestroy()")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            Timber.d("onStartCommand $it")
            when (it.action) {
                ACTION_START -> {
                    when (currentState) {
                        ServiceState.NONE -> startDataRecordService()
                        else -> Unit
                    }
                }

                ACTION_STOP_IF_NOT_IN_SESSION -> {
                    when (currentState) {
                        ServiceState.ACTIVE -> stopDataRecordService()
                        else -> Unit
                    }
                }

                ACTION_START_SESSION -> {
                    when (currentState) {
                        ServiceState.ACTIVE -> startSession()
                        else -> Unit
                    }
                }

                ACTION_STOP_SESSION -> {
                    when (currentState) {
                        ServiceState.IN_SESSION -> stopSession()
                        else -> Unit
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun initDataRecordService() {
        isInSession.postValue(false)
        currentState = ServiceState.NONE

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    protected abstract fun onStartDataRecordService()

    private fun startDataRecordService() {
        // callback
        onStartDataRecordService()

        // state
        currentState = ServiceState.ACTIVE
    }

    protected abstract fun onStopDataRecordService()

    private fun stopDataRecordService() {
        // callback
        onStopDataRecordService()

        //stopSelf()

        // state
        currentState = ServiceState.NONE
    }

    protected abstract fun onStartSession(notification: NotificationCompat.Builder)

    @SuppressLint("ObsoleteSdkInt")
    private fun startSession() {

        val notification = NotificationCompat.Builder(
            this,
            NOTIFICATION_CHANNEL_ID
        )
            .setContentTitle("In Session ...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)

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
        onStartSession(notification)

        startForeground(NOTIFICATION_ID, notification.build())

        // state
        currentState = ServiceState.IN_SESSION
        isInSession.postValue(true)
    }

    protected abstract fun onStopSession()

    private fun stopSession() {

        // callback
        onStopSession()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
        stopForeground(STOP_FOREGROUND_REMOVE)

        // state
        currentState = ServiceState.ACTIVE
        isInSession.postValue(false)
    }

    class DummyDataRecordService : DataRecordService() {
        override fun onStartDataRecordService() {
            Timber.d("onStartDataRecordService()")
        }

        override fun onStopDataRecordService() {
            Timber.d("onStopDataRecordService()")
        }

        override fun onStartSession(notification: NotificationCompat.Builder) {
            Timber.d("onStartSession()")

            val mainActivityPendingIntent = PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            notification
                .setContentIntent(mainActivityPendingIntent)
                .setOngoing(true)

        }

        override fun onStopSession() {
            Timber.d("onStopSession()")
        }

    }

}