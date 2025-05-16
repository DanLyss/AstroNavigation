package com.starapp.navigation.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Looper
import android.util.Log
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task

class LocationHandler(private val context: Context) {
    private var fusedClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private var callback: LocationCallback? = null
    private var startTime = 0L  // Track when we started listening

    @SuppressLint("MissingPermission")
    fun startListening(onLocationChanged: (Location) -> Unit) {
        // Record start time for fallback mechanism
        startTime = System.currentTimeMillis()

        // First try to get the last known location immediately
        getLastLocation(onLocationChanged)

        // Then set up continuous location updates
        setupLocationUpdates(onLocationChanged)
    }

    @SuppressLint("MissingPermission")
    private fun getLastLocation(onLocationChanged: (Location) -> Unit) {
        try {
            // Get last location from fused provider
            val lastLocationTask: Task<Location> = fusedClient.lastLocation

            lastLocationTask.addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val accuracy = location.accuracy
                    val age = System.currentTimeMillis() - location.time

                    Log.d("LocationHandler", "📍 Last known location: ${location.latitude}, ${location.longitude}, acc=$accuracy, age=${age}ms")

                    // Accept any location that's reasonably recent (within 24 hours)
                    // This is very lenient but ensures we get at least some location data
                    if (age <= 86_400_000) { // 24 hours
                        onLocationChanged(location)
                        Log.d("LocationHandler", "✅ Accepted last known location: age=${age}ms")
                    } else {
                        Log.d("LocationHandler", "❌ Last known location too old: age=${age}ms")
                    }
                } else {
                    Log.d("LocationHandler", "❌ No last known location available")
                }
            }
        } catch (e: Exception) {
            Log.e("LocationHandler", "Error getting last location: ${e.message}")
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupLocationUpdates(onLocationChanged: (Location) -> Unit) {
        // Use high accuracy for fastest acquisition
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 1000L
            fastestInterval = 100L  // Faster updates
            smallestDisplacement = 0f
            maxWaitTime = 5_000  // Shorter wait time
        }

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val location = result.lastLocation ?: return
                val accuracy = location.accuracy
                val age = System.currentTimeMillis() - location.time
                val timeSinceStart = System.currentTimeMillis() - startTime

                Log.d("LocationHandler", "📍 Fused location: ${location.latitude}, ${location.longitude}, acc=$accuracy, age=${age}ms, timeSinceStart=${timeSinceStart}ms")

                // Accept location in any of these cases:
                // 1. Location is reasonably recent (within 5 minutes)
                // 2. We've been waiting for more than 10 seconds for any location
                if (age <= 300_000 || timeSinceStart > 10_000) { // 5 minutes or 10 seconds since start
                    onLocationChanged(location)
                    Log.d("LocationHandler", "✅ Accepted location: age=${age}ms, timeSinceStart=${timeSinceStart}ms")
                } else {
                    Log.d("LocationHandler", "❌ Ignored fused location: too old and still waiting for better")
                }
            }
        }

        fusedClient.requestLocationUpdates(locationRequest, callback!!, Looper.getMainLooper())
    }

    fun stopListening() {
        callback?.let { fusedClient.removeLocationUpdates(it) }
    }
}
