package com.example.cameralong.intent

import android.content.Intent
import android.net.Uri

/**
 * Manager class for handling intent parameter extraction
 */
class IntentManager {

    /**
     * Extract parameters from CropActivity intent
     * @param intent The intent to extract parameters from
     * @return Triple of imagePath, currentLocation, and currentAngles
     */
    fun extractCropActivityParams(intent: Intent): Triple<String?, String, String> {
        val imagePath = intent.getStringExtra("imagePath")
        val currentLocation = intent.getStringExtra("currentLocation") ?: "unknown"
        val currentAngles = intent.getStringExtra("currentAngles") ?: "unknown"
        return Triple(imagePath, currentLocation, currentAngles)
    }

    /**
     * Extract parameters from ResultActivity intent
     * @param intent The intent to extract parameters from
     * @return Pair of imagePath and currentLocation
     */
    fun extractResultActivityParams(intent: Intent): Pair<String?, String> {
        val imagePath = intent.getStringExtra("imagePath")
        val currentLocation = intent.getStringExtra("currentLocation") ?: "unknown"
        return Pair(imagePath, currentLocation)
    }

    /**
     * Extract parameters from StarsActivity intent
     * @param intent The intent to extract parameters from
     * @return Triple of imagePath, corrPath, and currentLocation
     * @throws IllegalStateException if required parameters are missing
     */
    fun extractStarsActivityParams(intent: Intent): Triple<String, String, String> {
        val imagePath = intent.getStringExtra("imagePath")
            ?: throw IllegalStateException("No imagePath in Intent")
        val corrPath = intent.getStringExtra("corrPath")
            ?: throw IllegalStateException("No corrPath in Intent")
        val currentLocation = intent.getStringExtra("currentLocation") ?: "unknown"

        return Triple(imagePath, corrPath, currentLocation)
    }

    /**
     * Extract parameters from LocationActivity intent
     * @param intent The intent to extract parameters from
     * @return Triple of predPhiRad, predLongRad, and imagePath
     * @throws IllegalStateException if required parameters are missing
     */
    fun extractLocationActivityParams(intent: Intent): Triple<Double, Double, String> {
        val predPhiRad = intent.getDoubleExtra("pred_phi", 0.0)
        val predLongRad = intent.getDoubleExtra("pred_long", 0.0)
        val imagePath = intent.getStringExtra("imagePath")
            ?: throw IllegalStateException("No imagePath in Intent")

        return Triple(predPhiRad, predLongRad, imagePath)
    }

    /**
     * Extract URI from gallery selection result
     * @param intent The intent from onActivityResult
     * @return The selected image URI or null if no data
     */
    fun extractGallerySelectionUri(intent: Intent?): Uri? {
        return intent?.data
    }

    /**
     * Create an intent for gallery image selection
     * @return Intent configured for gallery image selection
     */
    fun createGallerySelectionIntent(): Intent {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        return intent
    }
}
