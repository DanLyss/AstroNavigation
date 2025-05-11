package com.example.cameralong.util

import androidx.exifinterface.media.ExifInterface
import android.util.Log
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Utility class for EXIF data operations
 */
object ExifUtils {
    private const val TAG = "ExifUtils"

    /**
     * Extracts orientation angles (yaw, pitch, roll) from EXIF data
     * @param exif The ExifInterface object
     * @return Triple of yaw, pitch, and roll in degrees
     */
    fun extractOrientationAngles(exif: ExifInterface): Triple<Double, Double, Double> {
        val userComment = exif.getAttribute(ExifInterface.TAG_USER_COMMENT)

        if (userComment == null) {
            Log.w(TAG, "No EXIF UserComment found, using default values")
            return Triple(0.0, 0.0, 0.0)
        }

        val regex = Regex("""[Yy]aw[:=]\s*([-\d.]+)\s*,\s*[Pp]itch[:=]\s*([-\d.]+)\s*,\s*[Rr]oll[:=]\s*([-\d.]+)""")
        val matchResult = regex.find(userComment)

        if (matchResult == null) {
            Log.w(TAG, "Cannot parse yaw/pitch/roll from '$userComment', using default values")
            return Triple(0.0, 0.0, 0.0)
        }

        val (yawDeg, pitchDeg, rollDeg) = matchResult.destructured

        return Triple(
            yawDeg.toDouble(),
            pitchDeg.toDouble(),
            rollDeg.toDouble()
        )
    }

    /**
     * Extracts date and time from EXIF data and converts to ISO format
     * @param exif The ExifInterface object
     * @return Date and time in ISO format
     */
    fun extractDateTime(exif: ExifInterface): String {
        val dtString = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME)

        if (dtString == null) {
            Log.w(TAG, "No EXIF date/time found, using current time")
            // Use current time as fallback
            return java.time.OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        }

        try {
            // TAG_OFFSET_TIME_ORIGINAL and TAG_OFFSET_TIME are not available in ExifInterface for SDK 28
            // Use system default timezone instead
            val ldt = LocalDateTime.parse(dtString, DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss"))
            val odt = ldt.atZone(ZoneId.systemDefault()).toOffsetDateTime()
            return odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        } catch (e: Exception) {
            Log.w(TAG, "Error parsing EXIF date/time: ${e.message}, using current time")
            return java.time.OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        }
    }

    /**
     * Gets the dimensions of an image file
     * @param imageFile The image file
     * @return Pair of width and height
     */
    fun getImageDimensions(imageFile: File): Pair<Int, Int> {
        val options = android.graphics.BitmapFactory.Options().apply { 
            inJustDecodeBounds = true 
        }
        android.graphics.BitmapFactory.decodeFile(imageFile.absolutePath, options)
        return Pair(options.outWidth, options.outHeight)
    }

    /**
     * Saves EXIF data to an image file
     * @param filePath The path to the image file
     * @param angles The orientation angles string
     * @param location The location string
     */
    fun saveExifData(filePath: String, angles: String, location: String) {
        try {
            val exif = ExifInterface(filePath)

            // Save orientation angles to EXIF data
            if (angles != "unknown") {
                exif.setAttribute(ExifInterface.TAG_USER_COMMENT, angles)
                Log.i(TAG, "Saved angles to EXIF: $angles")
            }

            // Save location data to EXIF
            if (location != "unknown") {
                try {
                    // Parse latitude and longitude from the location string (format: "latitude, longitude")
                    val parts = location.split(",")
                    if (parts.size == 2) {
                        val latitude = parts[0].trim().toDouble()
                        val longitude = parts[1].trim().toDouble()

                        // Set latitude and longitude in EXIF
                        exif.setLatLong(latitude, longitude)
                        Log.i(TAG, "Saved location to EXIF: $latitude, $longitude")
                    } else {
                        Log.e(TAG, "Invalid location format: $location")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse location: ${e.message}")
                }
            }

            // Save all EXIF changes
            exif.saveAttributes()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save EXIF data: ${e.message}")
            throw e
        }
    }
}