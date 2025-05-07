package com.example.cameralong

import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Utility class for EXIF data extraction
 */
object ExifUtils {

    /**
     * Extracts orientation angles (yaw, pitch, roll) from EXIF data
     * @param exif The ExifInterface object
     * @return Triple of yaw, pitch, and roll in degrees
     */
    fun extractOrientationAngles(exif: ExifInterface): Triple<Double, Double, Double> {
        val userComment = exif.getAttribute(ExifInterface.TAG_USER_COMMENT)

        if (userComment == null) {
            android.util.Log.w("ExifUtils", "No EXIF UserComment found, using default values")
            return Triple(0.0, 0.0, 0.0)
        }

        val regex = Regex("""[Yy]aw[:=]\s*([-\d.]+)\s*,\s*[Pp]itch[:=]\s*([-\d.]+)\s*,\s*[Rr]oll[:=]\s*([-\d.]+)""")
        val matchResult = regex.find(userComment)

        if (matchResult == null) {
            android.util.Log.w("ExifUtils", "Cannot parse yaw/pitch/roll from '$userComment', using default values")
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
            android.util.Log.w("ExifUtils", "No EXIF date/time found, using current time")
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
            android.util.Log.w("ExifUtils", "Error parsing EXIF date/time: ${e.message}, using current time")
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
}
