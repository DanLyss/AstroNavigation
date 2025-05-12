package com.starapp.navigation.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.view.View
import android.widget.TextView
import androidx.exifinterface.media.ExifInterface
import com.starapp.navigation.astro.AstrometryManager
import com.starapp.navigation.util.ExifUtils
import java.io.File
import java.io.FileOutputStream

/**
 * Manager class for handling image cropping and processing operations
 */
class ImageCropManager(private val context: Context) {

    /**
     * Loads a bitmap from the specified file path
     * @param path Path to the image file
     * @return The loaded bitmap or null if loading fails
     */
    fun loadImageFromPath(path: String): Bitmap? {
        return try {
            BitmapFactory.decodeFile(path)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Crops an image based on the provided crop rectangle
     * @param imagePath Path to the original image
     * @param bitmap Original bitmap to crop
     * @param cropRect Crop rectangle in image coordinates (0-1 range)
     * @param currentLocation Current location string for EXIF data
     * @param currentAngles Current angles string for EXIF data
     * @return Path to the cropped image or null if cropping fails
     */
    fun cropImage(
        imagePath: String,
        bitmap: Bitmap,
        cropRect: RectF,
        currentLocation: String,
        currentAngles: String
    ): String? {
        try {
            // Convert to pixel coordinates
            val left = (cropRect.left * bitmap.width).toInt().coerceIn(0, bitmap.width)
            val top = (cropRect.top * bitmap.height).toInt().coerceIn(0, bitmap.height)
            val right = (cropRect.right * bitmap.width).toInt().coerceIn(0, bitmap.width)
            val bottom = (cropRect.bottom * bitmap.height).toInt().coerceIn(0, bitmap.height)

            // Calculate width and height
            val width = right - left
            val height = bottom - top

            // Ensure valid dimensions
            if (width <= 0 || height <= 0) return null

            // Create a new bitmap with the cropped region
            val croppedBitmap = Bitmap.createBitmap(bitmap, left, top, width, height)

            // Save the cropped bitmap to a temporary file
            val croppedFile = File(imagePath.replace(".jpg", "_cropped.jpg"))
            FileOutputStream(croppedFile).use { out ->
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            }

            // Copy EXIF data from original image to cropped image
            copyExifData(imagePath, croppedFile.absolutePath, currentAngles, currentLocation)

            return croppedFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Copies EXIF data from the original image to the cropped image
     * @param originalImagePath Path to the original image
     * @param croppedImagePath Path to the cropped image
     * @param currentAngles Current angles string for EXIF data
     * @param currentLocation Current location string for EXIF data
     */
    private fun copyExifData(
        originalImagePath: String,
        croppedImagePath: String,
        currentAngles: String,
        currentLocation: String
    ) {
        try {
            // Read EXIF from original image
            val originalExif = ExifInterface(originalImagePath)

            // Write EXIF to cropped image
            val croppedExif = ExifInterface(croppedImagePath)

            // Copy all important EXIF tags
            copyExifTag(originalExif, croppedExif, ExifInterface.TAG_USER_COMMENT)
            copyExifTag(originalExif, croppedExif, ExifInterface.TAG_DATETIME)
            copyExifTag(originalExif, croppedExif, ExifInterface.TAG_DATETIME_ORIGINAL)
            copyExifTag(originalExif, croppedExif, ExifInterface.TAG_GPS_LATITUDE)
            copyExifTag(originalExif, croppedExif, ExifInterface.TAG_GPS_LATITUDE_REF)
            copyExifTag(originalExif, croppedExif, ExifInterface.TAG_GPS_LONGITUDE)
            copyExifTag(originalExif, croppedExif, ExifInterface.TAG_GPS_LONGITUDE_REF)

            // Save the EXIF changes
            croppedExif.saveAttributes()

            // Alternatively, we can use ExifUtils to save the data directly
            if (currentAngles != "unknown") {
                ExifUtils.saveExifData(croppedImagePath, currentAngles, currentLocation)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // Continue even if EXIF copying fails
        }
    }

    /**
     * Helper method to copy an EXIF tag from one ExifInterface to another
     */
    private fun copyExifTag(source: ExifInterface, destination: ExifInterface, tag: String) {
        val value = source.getAttribute(tag)
        if (value != null) {
            destination.setAttribute(tag, value)
        }
    }

    /**
     * Processes the image with the astrometry solver
     * @param imagePath Path to the image to process
     * @param statusText TextView to update with status messages
     * @param progressBar ProgressBar to show solver progress
     * @param currentLocation Current location string
     * @param currentAngles Current angles string
     * @param astrometryTimeSeconds Maximum astrometry processing time in seconds
     */
    fun processImageWithAstrometry(
        imagePath: String,
        statusText: TextView,
        progressBar: android.widget.ProgressBar,
        currentLocation: String,
        currentAngles: String,
        astrometryTimeSeconds: Int = 100
    ) {
        // Make status text and progress bar visible
        statusText.visibility = View.VISIBLE
        progressBar.visibility = View.VISIBLE
        statusText.text = "Starting astrometry solver..."

        // Run the astrometry solver on the image
        AstrometryManager.runSolver(
            context = context,
            imageFile = File(imagePath),
            astroPath = File(imagePath).parent ?: "",
            statusText = statusText,
            progressBar = progressBar,
            currentLocation = currentLocation,
            currentAngles = currentAngles,
            cpuTimeLimit = astrometryTimeSeconds
        )
    }
}
