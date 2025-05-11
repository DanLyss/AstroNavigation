package com.starapp.navigation.image

import android.content.Context
import android.net.Uri
import android.widget.TextView
import android.widget.Toast
import androidx.exifinterface.media.ExifInterface
import com.starapp.navigation.navigation.NavigationManager
import java.io.File
import java.io.IOException

/**
 * Manager class for handling image selection and processing
 */
class ImageSelectionManager(private val context: Context) {

    /**
     * Processes an image selected from the gallery
     * @param uri URI of the selected image
     * @param destFile Destination file to save the image
     * @param statusText TextView to update with status messages
     * @param currentLocation Current location string
     * @param onSuccess Callback for successful processing
     */
    fun processSelectedImage(
        uri: Uri,
        destFile: File,
        statusText: TextView,
        currentLocation: String,
        onSuccess: (String) -> Unit
    ) {
        try {
            copyImageToFile(uri, destFile)
            val angles = extractAnglesFromExif(destFile)

            statusText.text = "📂 Photo loaded, navigating to crop screen..."

            // Navigate to CropActivity instead of running solver directly
            NavigationManager().navigateToCropActivity(
                activity = context as android.app.Activity,
                imagePath = destFile.absolutePath,
                currentLocation = currentLocation,
                currentAngles = angles
            )

            onSuccess(angles)
        } catch (e: Exception) {
            Toast.makeText(context, "❌ File loading error: ${e.message}", Toast.LENGTH_LONG).show()
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
}

