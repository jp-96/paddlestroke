package com.example.paddlestroke.sensor

import android.content.Context
import android.hardware.Sensor

class AccelerometerClient(
    context: Context
) : AndroidSensorClient(
    context = context,
    sensorType = Sensor.TYPE_ACCELEROMETER
)