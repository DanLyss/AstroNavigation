package com.example.cameralong.ui

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.cameralong.R
import com.example.cameralong.intent.IntentManager
import com.example.cameralong.location.LocationManager

class LocationActivity : AppCompatActivity() {

    private lateinit var locationText: TextView

    // Managers
    private lateinit var locationManager: LocationManager
    private lateinit var intentManager: IntentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        // Initialize views
        locationText = findViewById(R.id.locationText)

        // Initialize managers
        locationManager = LocationManager()
        intentManager = IntentManager()

        // Extract parameters from intent
        val params = intentManager.extractLocationActivityParams(intent)
        val predPhiRad = params.first
        val predLongRad = params.second
        val imagePath = params.third

        // Extract GPS coordinates from image EXIF data
        val actualCoordinates = locationManager.extractGpsCoordinates(imagePath)

        // Generate and display location text
        locationText.text = locationManager.generateLocationText(
            predictedLat = predPhiRad,
            predictedLon = predLongRad,
            actualCoordinates = actualCoordinates
        )
    }
}
