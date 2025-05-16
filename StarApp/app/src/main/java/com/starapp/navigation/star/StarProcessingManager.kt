package com.starapp.navigation.star

import android.content.Context
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.starapp.navigation.util.ExifUtils
import kotlintranslation.AstrometryReader
import kotlintranslation.StarCluster
import java.io.File

/**
 * Manager class for handling star processing operations
 */
class StarProcessingManager(private val context: Context) {

    /**
     * Processes stars from a .corr file
     * @param imagePath Path to the image file
     * @param corrPath Path to the .corr file
     * @param matchWeightThreshold Threshold for star matching weight
     * @return StarProcessingResult containing the processed data
     */
    fun processStars(
        imagePath: String,
        corrPath: String,
        matchWeightThreshold: Double
    ): StarProcessingResult {
        // Validate files
        val imageFile = File(imagePath)
        if (!imageFile.exists()) {
            return StarProcessingResult.Error("Image not found at $imagePath")
        }
        
        val corrFile = File(corrPath)
        if (!corrFile.exists()) {
            return StarProcessingResult.Error("Corr file not found at $corrPath")
        }

        // Get image dimensions
        val dimensions = ExifUtils.getImageDimensions(imageFile)
        val width = dimensions.first
        val height = dimensions.second

        // Extract EXIF data
        val exif = ExifInterface(imagePath)
        val angles = ExifUtils.extractOrientationAngles(exif)
        val yawDeg = angles.first
        val pitchDeg = angles.second
        val rollDeg = angles.third
        val isoTime = ExifUtils.extractDateTime(exif)

        // Ensure threshold is not greater than 1.0 to prevent crashes
        val safeThreshold = minOf(matchWeightThreshold, 0.999)

        try {
            // Process stars with safe threshold
            val stars = AstrometryReader.fromCorrFile(
                corrPath = corrFile.absolutePath,
                matchWeightThreshold = safeThreshold,
                sizex = width,
                sizey = height
            )

            // Check if we have enough stars to proceed
            if (stars.isEmpty()) {
                Log.w("StarProcessingManager", "No stars found with threshold $safeThreshold")
                return StarProcessingResult.NoStarsFound("No stars found with current threshold. Try lowering the threshold value.")
            }

            // Create star cluster
            val cluster = StarCluster(
                stars = stars,
                positionalAngle = Math.toRadians(pitchDeg),
                rotationAngle = Math.toRadians(rollDeg),
                timeGMT = isoTime
            )

            return StarProcessingResult.Success(
                cluster = cluster,
                width = width,
                height = height,
                yawDeg = yawDeg,
                pitchDeg = pitchDeg,
                rollDeg = rollDeg,
                isoTime = isoTime
            )
        } catch (e: Exception) {
            Log.e("StarProcessingManager", "Error processing stars: ${e.message}", e)
            return StarProcessingResult.Error("Error processing stars: ${e.message}")
        }
    }

    /**
     * Generates display text for the star processing result
     * @param result The star processing result
     * @return Formatted text for display
     */
    fun generateDisplayText(result: StarProcessingResult.Success): String {
        return buildString {
            appendLine("X size: size: ${result.width}")
            appendLine("Y size: size: ${result.height}")
            appendLine("Computed time: ${result.isoTime}")
            appendLine("Yaw:   ${result.yawDeg}°")
            appendLine("Pitch: ${result.pitchDeg}°")
            appendLine("Roll:  ${result.rollDeg}°")
            appendLine()
            appendLine("Found ${result.cluster.stars.size} stars")
            appendLine("X size: %.2f° per 100 pixels".format(Math.toDegrees(result.cluster.angularXSize)))
            appendLine("Y size: %.2f° per 100 pixels".format(Math.toDegrees(result.cluster.angularYSize)))
            appendLine("Cluster Az₀: %.2f°".format(Math.toDegrees(result.cluster.AzStar0)))
            result.cluster.stars.forEachIndexed { i, star ->
                appendLine(
                    "⭐ Star ${i + 1}: Alt = %.1f°, Az = %.1f°, xCoord = %.1f, yCoord = %.1f"
                        .format(Math.toDegrees(star.Alt!!), Math.toDegrees(star.Az!!),
                            star.xCoord, star.yCoord)
                )
            }
        }
    }
}

/**
 * Sealed class representing the result of star processing
 */
sealed class StarProcessingResult {
    /**
     * Successful star processing
     */
    data class Success(
        val cluster: StarCluster,
        val width: Int,
        val height: Int,
        val yawDeg: Double,
        val pitchDeg: Double,
        val rollDeg: Double,
        val isoTime: String
    ) : StarProcessingResult()

    /**
     * No stars found with the current threshold
     */
    data class NoStarsFound(val message: String) : StarProcessingResult()

    /**
     * Error during star processing
     */
    data class Error(val message: String) : StarProcessingResult()
}
