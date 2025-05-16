package com.starapp.navigation.result

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.starapp.navigation.astro.FitsManager
import java.io.File

/**
 * Manager class for handling result-related operations
 */
class ResultManager(private val context: Context) {

    /**
     * Loads an image from the given path
     * @param imagePath Path to the image file
     * @param onError Callback for error handling
     * @return Bitmap if successful, null otherwise
     */
    fun loadImage(imagePath: String, onError: (String) -> Unit = {}): Bitmap? {
        val file = File(imagePath)
        return if (file.exists()) {
            BitmapFactory.decodeFile(imagePath)
        } else {
            onError("Image file not found")
            null
        }
    }

    /**
     * Extracts star coordinates from a .corr file
     * @param imagePath Path to the image file
     * @param onError Callback for error handling
     * @return List of star points (x, y coordinates) if successful, empty list otherwise
     */
    fun extractStarCoordinates(imagePath: String, onError: (String) -> Unit = {}): List<Pair<Float, Float>> {
        // First try with the image name
        val corrPath = File(imagePath).nameWithoutExtension + ".corr"
        val corrFile = File(context.filesDir, "astro/output/$corrPath")

        // If not found, try with "input.corr" which is what the solver generates
        val inputCorrFile = File(context.filesDir, "astro/output/input.corr")

        return when {
            corrFile.exists() -> {
                try {
                    FitsManager.extractStarCoordinates(corrFile)
                } catch (e: Exception) {
                    onError("Error reading .corr file: ${e.message}")
                    emptyList()
                }
            }
            inputCorrFile.exists() -> {
                try {
                    FitsManager.extractStarCoordinates(inputCorrFile)
                } catch (e: Exception) {
                    onError("Error reading input.corr file: ${e.message}")
                    emptyList()
                }
            }
            else -> {
                onError(".corr files not found")
                emptyList()
            }
        }
    }

    /**
     * Checks if a .corr file exists for the given image
     * @param imagePath Path to the image file
     * @return true if .corr file exists, false otherwise
     */
    fun hasCorrFile(imagePath: String): Boolean {
        // First try with the image name
        val corrPath = File(imagePath).nameWithoutExtension + ".corr"
        val corrFile = File(context.filesDir, "astro/output/$corrPath")

        // If not found, try with "input.corr" which is what the solver generates
        val inputCorrFile = File(context.filesDir, "astro/output/input.corr")

        return corrFile.exists() || inputCorrFile.exists()
    }

    /**
     * Gets the full path to the .corr file for the given image
     * @param imagePath Path to the image file
     * @return Full path to the .corr file if it exists, null otherwise
     */
    fun getCorrFilePath(imagePath: String): String? {
        // First try with the image name
        val corrPath = File(imagePath).nameWithoutExtension + ".corr"
        val corrFile = File(context.filesDir, "astro/output/$corrPath")

        // If not found, try with "input.corr" which is what the solver generates
        val inputCorrFile = File(context.filesDir, "astro/output/input.corr")

        return when {
            corrFile.exists() -> corrFile.absolutePath
            inputCorrFile.exists() -> inputCorrFile.absolutePath
            else -> null
        }
    }
}
