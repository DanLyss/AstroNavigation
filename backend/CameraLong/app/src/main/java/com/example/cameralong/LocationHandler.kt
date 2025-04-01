package com.example.cameralong

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.*

class LocationHandler(context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun getLocation(callback: (Location?) -> Unit) {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                callback(location)
            }
            .addOnFailureListener {
                callback(null)
            }
    }
}
