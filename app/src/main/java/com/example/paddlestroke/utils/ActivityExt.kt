package com.example.paddlestroke.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat

fun Activity.hasPermission(permission: String): Boolean {
    return ActivityCompat.checkSelfPermission(
        this, permission
    ) == PackageManager.PERMISSION_GRANTED
}

fun Activity.hasLocationPermission(): Boolean {
    return hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
            && hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
}
