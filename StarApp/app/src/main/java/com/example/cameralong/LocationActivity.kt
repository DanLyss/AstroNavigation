package com.example.cameralong

import android.location.Location
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import kotlin.math.roundToInt

class LocationActivity : AppCompatActivity() {

    private lateinit var locationText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)
        locationText = findViewById(R.id.locationText)

        val predPhiRad  = intent.getDoubleExtra("pred_phi",  0.0)
        val predLongRad = intent.getDoubleExtra("pred_long", 0.0)
        val imagePath   = intent.getStringExtra("imagePath")
            ?: throw IllegalStateException("No imagePath in Intent")

        val predLatDeg = Math.toDegrees(predPhiRad)
        val predLonDeg = Math.toDegrees(predLongRad)

        val exif   = ExifInterface(imagePath)
        val latLon = FloatArray(2)
        val hasGps = exif.getLatLong(latLon)

        if (hasGps) {
            val trueLatDeg = latLon[0].toDouble()
            val trueLonDeg = latLon[1].toDouble()

            val results = FloatArray(1)
            Location.distanceBetween(
                trueLatDeg, trueLonDeg,
                predLatDeg,  predLonDeg,
                results
            )
            val errorKm = (results[0] / 1000.0).roundToInt()

            locationText.text = """
                📡 Photo EXIF GPS:
                Latitude:  ${"%.4f".format(trueLatDeg)}°
                Longitude: ${"%.4f".format(trueLonDeg)}°

                📐 Predicted (φ, λ):
                Latitude:  ${"%.4f".format(predLatDeg)}°
                Longitude: ${"%.4f".format(predLonDeg)}°

                📏 Error ≈ $errorKm km
            """.trimIndent()
        } else {
            locationText.text = """
                ⚠️ No GPS tags found in image EXIF.
                
                📐 Predicted (φ, λ):
                Latitude:  ${"%.4f".format(predLatDeg)}°
                Longitude: ${"%.4f".format(predLonDeg)}°
            """.trimIndent()
        }
    }
}
