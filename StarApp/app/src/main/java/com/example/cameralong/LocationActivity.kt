package com.example.cameralong

import android.location.Location
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.roundToInt

class LocationActivity : AppCompatActivity() {

    private lateinit var locationText: TextView
    private lateinit var locationHandler: LocationHandler

    // Заглушка: координаты Тель-Авива
    private val telAvivLat = 32.0853
    private val telAvivLon = 34.7818

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        locationText = findViewById(R.id.locationText)
        locationHandler = LocationHandler(this)

        locationText.text = "⏳ Waiting for GPS..."

        locationHandler.startListening { location ->
            displayLocationInfo(location)
            locationHandler.stopListening() // отключаемся после первого валидного значения
        }
    }

    private fun displayLocationInfo(location: Location) {
        val userLat = location.latitude
        val userLon = location.longitude

        // Расчёт расстояния в км
        val results = FloatArray(1)
        Location.distanceBetween(userLat, userLon, telAvivLat, telAvivLon, results)
        val distanceKm = (results[0] / 1000).roundToInt()

        // Вывод
        locationText.text = """
            🧭 Your real GPS coordinates:
            Latitude: ${"%.4f".format(userLat)}°
            Longitude: ${"%.4f".format(userLon)}°
            
            📌 Our prediction:
            Latitude: $telAvivLat°
            Longitude: $telAvivLon°
            
            📏 Error 😱:
            ≈ $distanceKm km
        """.trimIndent()
    }

    override fun onPause() {
        super.onPause()
        locationHandler.stopListening()
    }
}
