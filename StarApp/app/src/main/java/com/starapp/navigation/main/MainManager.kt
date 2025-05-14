package com.starapp.navigation.main

import android.os.Handler
import android.os.Looper
import android.widget.TextView


/**
 * Manager class for main screen
 * @param statusText   the TextView to update
 * @param getLocation  lambda that returns the current location string
 */
class MainManager(
    private val statusText: TextView,
    private val getLocation: () -> String
) {
    private val handler = Handler(Looper.getMainLooper())

    // self-rescheduling every 100ms
    private val tick = object : Runnable {
        override fun run() {
            val loc = getLocation()
            statusText.text = if (loc == "unknown") {
                "✨ Searching for location… Wait or proceed without it"
            } else {
                "📍 Location found: $loc"
            }
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