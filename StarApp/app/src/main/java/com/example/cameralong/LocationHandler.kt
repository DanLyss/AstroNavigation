package com.example.cameralong

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.*

class LocationHandler(private val context: Context) {
    private var fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private var callback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    fun startListening(onLocationChanged: (Location) -> Unit) {
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMinUpdateDistanceMeters(0f)
            .setWaitForAccurateLocation(true)
            .setMaxUpdateAgeMillis(30_000)
            .build()

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val accuracy = location.accuracy
                val age = System.currentTimeMillis() - location.time

                Log.d("LocationHandler", "📍 Fused location: ${location.latitude}, ${location.longitude}, acc=$accuracy, age=${age}ms")

                if (accuracy <= 50f && age <= 30_000) {
                    Toast.makeText(context, "✅ GPS fix: ${location.latitude}, ${location.longitude}", Toast.LENGTH_SHORT).show()
                    onLocationChanged(location)
                } else {
                    Log.d("LocationHandler", "❌ Ignored fused location: too inaccurate or old")
                }
            }
        }

        fusedClient.requestLocationUpdates(request, callback!!, Looper.getMainLooper())
    }

    fun stopListening() {
        callback?.let { fusedClient.removeLocationUpdates(it) }
    }
}
