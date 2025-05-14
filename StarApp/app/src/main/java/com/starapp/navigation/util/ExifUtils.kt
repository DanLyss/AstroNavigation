package com.starapp.navigation.util

import androidx.exifinterface.media.ExifInterface
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
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


    fun saveExifData(
        filePath: String,
        angles: String,
        locationString: String,
        date: Date
    ) {
        val exif = ExifInterface(filePath)

        if (angles != "unknown") {
            exif.setAttribute(ExifInterface.TAG_USER_COMMENT, angles)
            Log.i(TAG, "Saved angles to EXIF: $angles")
        }

        Log.i(TAG, locationString)
        val parts = locationString.split("\\s*,\\s*".toRegex())
        if (parts.size == 2) {
            try {
                val lat = parts[0].toDouble()
                val lon = parts[1].toDouble()

                exif.setLatLong(lat, lon)


                val dateFmt = SimpleDateFormat("yyyy:MM:dd", Locale.US)
                exif.setAttribute(
                    ExifInterface.TAG_GPS_DATESTAMP,
                    dateFmt.format(date)
                )

                val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)
                exif.setAttribute(
                    ExifInterface.TAG_GPS_TIMESTAMP,
                    timeFmt.format(date)
                )

                Log.i(TAG, "Saved manual GPS to EXIF: $lat, $lon")
            } catch (e: NumberFormatException) {
                Log.e(TAG, "Invalid number in locationString=‘$locationString’", e)
            }
        } else {
            Log.e(TAG, "Invalid location format (expected ‘lat, lon’): $locationString")
        }

        exif.saveAttributes()


    }
}
