package com.starapp.navigation.navigation

import android.app.Activity
import android.content.Intent
import com.starapp.navigation.ui.CropActivity
import com.starapp.navigation.ui.LocationActivity
import com.starapp.navigation.ui.StarsActivity
import com.starapp.navigation.star.StarProcessingResult

/**
 * Manager class for handling navigation between activities
 */
class NavigationManager {

    /**
     * Navigate from ResultActivity to StarsActivity
     * @param activity The source activity
     * @param imagePath Path to the image file
     * @param corrPath Path to the .corr file
     * @param currentLocation Current location string
     */
    fun navigateToStarsActivity(
        activity: Activity,
        imagePath: String,
        corrPath: String,
        currentLocation: String
    ) {
        Intent(activity, StarsActivity::class.java).also { intent ->
            intent.putExtra("imagePath", imagePath)
            intent.putExtra("corrPath", corrPath)
            intent.putExtra("currentLocation", currentLocation)
            activity.startActivity(intent)
        }
        activity.finish()
    }

    /**
     * Navigate from StarsActivity to LocationActivity
     * @param activity The source activity
     * @param result The star processing result
     * @param imagePath Path to the image file
     * @param currentLocation Current location string
     */
    fun navigateToLocationActivity(
        activity: Activity,
        result: StarProcessingResult.Success,
        imagePath: String,
        currentLocation: String
    ) {
        Intent(activity, LocationActivity::class.java).also { intent ->
            intent.putExtra("pred_phi", result.cluster.phi)
            intent.putExtra("pred_long", result.cluster.longitude)
            intent.putExtra("imagePath", imagePath)
            intent.putExtra("currentLocation", currentLocation)
            activity.startActivity(intent)
        }
    }

    /**
     * Navigate to CropActivity
     * @param activity The source activity
     * @param imagePath Path to the image file
     * @param currentLocation Current location string
     * @param currentAngles Current angles string
     * @param astrometryTimeSeconds Maximum astrometry processing time in seconds
     */
    fun navigateToCropActivity(
        activity: Activity,
        imagePath: String,
        currentLocation: String,
        currentAngles: String,
        astrometryTimeSeconds: Int = 100
    ) {
        Intent(activity, CropActivity::class.java).also { intent ->
            intent.putExtra("imagePath", imagePath)
            intent.putExtra("currentLocation", currentLocation)
            intent.putExtra("currentAngles", currentAngles)
            intent.putExtra("astrometryTimeSeconds", astrometryTimeSeconds)
            activity.startActivity(intent)
        }
    }

    /**
     * Navigate back to previous activity
     * @param activity The current activity to finish
     */
    fun navigateBack(activity: Activity) {
        activity.finish()
    }
}
