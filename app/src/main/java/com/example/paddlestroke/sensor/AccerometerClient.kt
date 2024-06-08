package com.example.paddlestroke.sensor

import android.content.Context
import android.hardware.Sensor

class AccerometerClient(
    context: Context
): AndroidSensorClient(
    context = context,
    sensorType = Sensor.TYPE_ACCELEROMETER
)