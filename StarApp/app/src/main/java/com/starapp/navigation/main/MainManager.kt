package com.starapp.navigation.main

import android.os.Handler
import android.os.Looper


/**
 * Manager class for main screen
 * @param getLocation lambda that returns the current location string
 * @param onStatusUpdate callback for updating status text
 */
class MainManager(
    private val getLocation: () -> String,
    private val onStatusUpdate: (String) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())

    // self-rescheduling every 100ms
    private val tick = object : Runnable {
        override fun run() {
            val loc = getLocation()
            val statusText = if (loc == "unknown") {
                "✨ Searching for location… Wait or proceed without it"
            } else {
                "📍 Location found: $loc"
            }
            onStatusUpdate(statusText)
            handler.postDelayed(this, 100L)
        }
    }

    /** Start the 10 Hz updates. */
    fun start() {
        handler.post(tick)
    }

    /** Stop updating. */
    fun stop() {
        handler.removeCallbacks(tick)
    }
}
