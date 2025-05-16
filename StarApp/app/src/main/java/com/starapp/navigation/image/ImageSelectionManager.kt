package com.starapp.navigation.image

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.starapp.navigation.navigation.NavigationManager
import com.starapp.navigation.util.ExifUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Date

/**
 * Manager class for handling image selection and processing
 */
class ImageSelectionManager(private val context: Context) {

    /**
     * Processes an image selected from the gallery
     * @param uri URI of the selected image
     * @param destFile Destination file to save the image
     * @param currentLocation Current location string (used as fallback if no EXIF location)
     * @param onStatusUpdate Callback for updating status text
     * @param onSuccess Callback for successful processing
     * @param onError Callback for error handling
     */
    fun processSelectedImage(
        uri: Uri,
        destFile: File,
        currentLocation: String,
        onStatusUpdate: (String) -> Unit,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            copyImageToFile(uri, destFile)
            val angles = extractAnglesFromExif(destFile)

            // Extract location from the photo's EXIF data
            val exif = ExifInterface(destFile.absolutePath)
            val latLon = FloatArray(2)
            val hasLocation = exif.getLatLong(latLon)
            val locationString = if (hasLocation) {
                "${latLon[0]}, ${latLon[1]}"
            } else {
                ""  // Empty string if no location in EXIF
            }

            Log.d("ImageSelectionManager", "Extracted location from gallery photo: ${if (hasLocation) locationString else "No location found"}")

            onStatusUpdate("📂 Photo loaded, navigating to crop screen...")

            // Navigate to CropActivity instead of running solver directly
            NavigationManager().navigateToCropActivity(
                activity = context as android.app.Activity,
                imagePath = destFile.absolutePath,
                currentLocation = locationString,  // Use extracted location from EXIF
                currentAngles = angles
            )

            onSuccess(angles)
        } catch (e: Exception) {
            onError("❌ File loading error: ${e.message}")
        }
    }

    /**
     * Copies an image from a URI to a file
     * @param uri URI of the source image
     * @param destFile Destination file
     * @throws IOException if copying fails
     */
    private fun copyImageToFile(uri: Uri, destFile: File) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: throw IOException("Failed to open input stream for URI: $uri")
    }

    /**
     * Extracts orientation angles from EXIF data
     * @param imageFile Image file to extract EXIF data from
     * @return String containing orientation angles or "unknown" if not available
     */
    private fun extractAnglesFromExif(imageFile: File): String {
        val exif = ExifInterface(imageFile.absolutePath)
        val userComment = exif.getAttribute(ExifInterface.TAG_USER_COMMENT)

        return if (userComment != null && userComment.contains("yaw") && userComment.contains("pitch")) {
            userComment
        } else {
            "unknown"
        }
    }

    /**
     * Shows a night sky image from assets and saves it to the gallery
     * @param currentLocation Current location string
     * @param onStatusUpdate Callback for updating status
     * @param onComplete Callback for when the operation is complete
     */
    fun showNightSkyImage(
        onStatusUpdate: (String) -> Unit,
        onComplete: (Boolean) -> Unit
    ) {
        // Show status message
        onStatusUpdate("Saving night sky image to gallery...")

        // Use a background thread for file operations
        Thread {
            try {
                // Create a temporary file to save the image with EXIF data
                val tempFile = File(context.cacheDir, "temp_night_sky.jpg")

                // Copy the image from assets to the temporary file
                val inputStream = context.assets.open("photo_example.jpg")
                val outputStream = FileOutputStream(tempFile)

                val buffer = ByteArray(1024)
                var length: Int
                while (inputStream.read(buffer).also { length = it } > 0) {
                    outputStream.write(buffer, 0, length)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()

                // Extract EXIF data from the photo in assets
                val assetInputStream = context.assets.open("photo_example.jpg")
                val tempExifFile = File(context.cacheDir, "temp_exif.jpg")
                val tempExifOutputStream = FileOutputStream(tempExifFile)

                val exifBuffer = ByteArray(1024)
                var exifLength: Int
                while (assetInputStream.read(exifBuffer).also { exifLength = it } > 0) {
                    tempExifOutputStream.write(exifBuffer, 0, exifLength)
                }

                tempExifOutputStream.flush()
                tempExifOutputStream.close()
                assetInputStream.close()

                // Extract angles from the photo in assets
                val exif = ExifInterface(tempExifFile.absolutePath)
                val (yaw, pitch, roll) = ExifUtils.extractOrientationAngles(exif)
                val extractedAngles = "Yaw: $yaw, Pitch: $pitch, Roll: $roll"

                Log.d("ImageSelectionManager", "Extracted angles from asset photo: $extractedAngles")

                // Extract location from the photo in assets
                val latLon = FloatArray(2)
                val hasLocation = exif.getLatLong(latLon)
                val locationString = if (hasLocation) {
                    "${latLon[0]}, ${latLon[1]}"
                } else {
                    ""
                }

                Log.d("ImageSelectionManager", "Extracted location from asset photo: ${if (hasLocation) locationString else "No location found"}")

                // Extract date from EXIF data
                val isoDateTime = try {
                    ExifUtils.extractDateTime(exif)
                } catch (e: Exception) {
                    Log.e("ImageSelectionManager", "Error extracting date from EXIF: ${e.message}", e)
                    onStatusUpdate("Error: Cannot process photo without valid datetime")
                    onComplete(false)
                    return@Thread
                }

                // Parse ISO date string to Date object
                val dateTime = try {
                    java.time.OffsetDateTime.parse(isoDateTime)
                        .toInstant()
                        .let { Date.from(it) }
                } catch (e: Exception) {
                    Log.e("ImageSelectionManager", "Error parsing ISO date: ${e.message}", e)
                    onStatusUpdate("Error: Invalid date format in photo")
                    onComplete(false)
                    return@Thread
                }

                // Add EXIF data to the temporary file
                ExifUtils.saveExifData(
                    tempFile.absolutePath,
                    extractedAngles,
                    locationString,
                    dateTime
                )

                // Clean up temporary EXIF file
                tempExifFile.delete()

                // Create content values with metadata for the image
                val contentValues = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "night_sky_image.jpg")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.TITLE, "Night Sky Image")
                    put(MediaStore.Images.Media.DESCRIPTION, "Downloaded from StarApp")
                }

                // Get the content resolver
                val contentResolver = context.contentResolver

                // Insert the image into the MediaStore
                val imageUri = contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )

                // If the URI is valid, save the image with EXIF data to it
                if (imageUri != null) {
                    contentResolver.openOutputStream(imageUri)?.use { galleryOutputStream ->
                        // Copy the temporary file with EXIF data to the gallery
                        val fileInputStream = FileInputStream(tempFile)
                        val buffer = ByteArray(1024)
                        var length: Int
                        while (fileInputStream.read(buffer).also { length = it } > 0) {
                            galleryOutputStream.write(buffer, 0, length)
                        }
                        galleryOutputStream.flush()
                        fileInputStream.close()
                    }

                    // Delete the temporary file
                    tempFile.delete()

                    // Update status when done
                    onStatusUpdate("Night sky image saved to gallery")
                    onComplete(true)
                } else {
                    throw IOException("Failed to create new MediaStore record")
                }
            } catch (e: Exception) {
                // Handle errors
                Log.e("ImageSelectionManager", "Error saving image to gallery: ${e.message}")
                onStatusUpdate("Gallery save error: ${e.message}")
                onComplete(false)
            }
        }.start()
    }
}
