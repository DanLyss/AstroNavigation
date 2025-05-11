package com.starapp.navigation.location

import android.location.Location
import androidx.exifinterface.media.ExifInterface
import kotlin.math.roundToInt

/**
 * Manager class for handling location-related operations
 */
class LocationManager {

    /**
     * Extracts GPS coordinates from image EXIF data
     * @param imagePath Path to the image file
     * @return Pair of latitude and longitude if GPS data exists, null otherwise
     */
    fun extractGpsCoordinates(imagePath: String): Pair<Double, Double>? {
        val exif = ExifInterface(imagePath)
        val latLon = FloatArray(2)
        val hasGps = exif.getLatLong(latLon)
        
        return if (hasGps) {
            Pair(latLon[0].toDouble(), latLon[1].toDouble())
        } else {
            null
        }
    }

    /**
     * Calculates the distance between two coordinates in kilometers
     * @param lat1 Latitude of first coordinate
     * @param lon1 Longitude of first coordinate
     * @param lat2 Latitude of second coordinate
     * @param lon2 Longitude of second coordinate
     * @return Distance in kilometers
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return (results[0] / 1000.0).roundToInt()
    }

    /**
     * Generates display text for location information
     * @param predictedLat Predicted latitude in radians
     * @param predictedLon Predicted longitude in radians
     * @param actualCoordinates Actual GPS coordinates (latitude, longitude) if available
     * @return Formatted text for display
     */
    fun generateLocationText(
        predictedLat: Double, 
        predictedLon: Double, 
        actualCoordinates: Pair<Double, Double>?
    ): String {
        val predLatDeg = Math.toDegrees(predictedLat)
        val predLonDeg = Math.toDegrees(predictedLon)

        return if (actualCoordinates != null) {
            val trueLatDeg = actualCoordinates.first
            val trueLonDeg = actualCoordinates.second
            val errorKm = calculateDistance(trueLatDeg, trueLonDeg, predLatDeg, predLonDeg)

            """
                📡 Photo EXIF GPS:
                Latitude:  ${"%.4f".format(trueLatDeg)}°
                Longitude: ${"%.4f".format(trueLonDeg)}°

                📐 Predicted (φ, λ):
                Latitude:  ${"%.4f".format(predLatDeg)}°
                Longitude: ${"%.4f".format(predLonDeg)}°

                📏 Error ≈ $errorKm km
            """.trimIndent()
        } else {
            """
                ⚠️ No GPS tags found in image EXIF.

                📐 Predicted (φ, λ):
                Latitude:  ${"%.4f".format(predLatDeg)}°
                Longitude: ${"%.4f".format(predLonDeg)}°
            """.trimIndent()
        }
    }
}