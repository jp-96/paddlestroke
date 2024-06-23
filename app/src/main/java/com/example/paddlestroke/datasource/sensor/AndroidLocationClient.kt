package com.example.paddlestroke.datasource.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import com.example.paddlestroke.data.DataRecord
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class AndroidLocationClient(
    private val context: Context,
    private val providerClient: FusedLocationProviderClient
) : LocationClient {

    companion object {
        fun createDataRecord(location: Location): DataRecord {
            // data: DoubleArray 0-lat, 1-long, 2-alt, 3-speed, 4-bearing, 5-accuracy
            //val timestamp = elapsedRealtimeNanos()
            return DataRecord(
                DataRecord.Type.LOCATION,
                location.time /*timestamp*/,
                doubleArrayOf(
                    // arrayOf("lat", "long", "alt", "speed", "bearing", "accuracy")
                    location.latitude,
                    location.longitude,
                    location.altitude,
                    location.speed.toDouble(),
                    location.bearing.toDouble(),
                    location.accuracy.toDouble(),
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    override fun getLocationFlow(intervalMillis: Long) = callbackFlow<DataRecord> {
        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.lastOrNull()?.let { location ->
                    trySend(createDataRecord(location))
                }
            }
        }

        // LocationRequest.Builder
        // https://developers.google.com/android/reference/com/google/android/gms/location/LocationRequest.Builder
        val locationRequest = LocationRequest.Builder(intervalMillis)
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setWaitForAccurateLocation(true)
            .build()
        providerClient.requestLocationUpdates(
            locationRequest,
            callback,
            Looper.getMainLooper()
        ).addOnFailureListener { e ->
            close(e)
        }

        awaitClose {
            providerClient.removeLocationUpdates(callback)
        }
    }

}
