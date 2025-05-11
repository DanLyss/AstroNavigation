package com.example.cameralong.ui.manager

import android.content.Context
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.cameralong.star.StarProcessingResult
import com.example.cameralong.ui.StarOverlayView
import com.google.android.material.slider.Slider

/**
 * Manager class for handling UI configuration and updates
 */
class UIManager(private val context: Context) {

    /**
     * Configure the continue button based on .corr file existence
     * @param continueButton The button to configure
     * @param hasCorrFile Whether a .corr file exists
     * @param onContinue Callback to be executed when the button is clicked and a .corr file exists
     */
    fun configureContinueButton(
        continueButton: Button,
        hasCorrFile: Boolean,
        onContinue: () -> Unit
    ) {
        if (hasCorrFile) {
            // Enable continue button only if .corr file exists
            continueButton.isEnabled = true
            continueButton.text = "Continue to Star Analysis"
            continueButton.setOnClickListener { onContinue() }
        } else {
            // Disable continue button if .corr file doesn't exist
            continueButton.isEnabled = false
            continueButton.text = "Cannot Continue - No Star Data"
            continueButton.setOnClickListener {
                Toast.makeText(context, "Cannot continue: No star data available for this image", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Configure the image view and overlay with the provided bitmap and star points
     * @param imageView The image view to configure
     * @param overlay The star overlay view to configure
     * @param bitmap The bitmap to display
     * @param starPoints The star points to display
     */
    fun configureImageViewAndOverlay(
        imageView: ImageView,
        overlay: StarOverlayView,
        bitmap: android.graphics.Bitmap?,
        starPoints: List<Pair<Float, Float>>
    ) {
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap)

            // Set image dimensions on the overlay view
            overlay.imageWidth = bitmap.width
            overlay.imageHeight = bitmap.height

            // Set star points on the overlay view
            overlay.starPoints = starPoints
            overlay.invalidate()
        } else {
            imageView.setImageResource(android.R.drawable.ic_delete)
        }
    }

    /**
     * Configure the threshold slider
     * @param slider The slider to configure
     * @param label The label to update
     * @param initialValue The initial value for the slider
     * @param onValueChanged Callback to be executed when the slider value changes
     */
    fun configureThresholdSlider(
        slider: Slider,
        label: TextView,
        initialValue: Double,
        onValueChanged: (Double) -> Unit
    ) {
        // Set up the threshold slider
        slider.valueTo = 1.0f  // Set maximum value to 1.0 to prevent crashes
        slider.value = initialValue.toFloat()
        updateThresholdLabel(label, initialValue)

        // Set up slider listener
        slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val newValue = value.toDouble()
                updateThresholdLabel(label, newValue)
                onValueChanged(newValue)
            }
        }
    }

    /**
     * Update the threshold label with the current value
     * @param label The label to update
     * @param value The current threshold value
     */
    private fun updateThresholdLabel(label: TextView, value: Double) {
        label.text = "Star Match Weight Threshold: %.3f".format(value)
    }

    /**
     * Update the UI based on star processing results
     * @param result The star processing result
     * @param textView The text view to update
     * @param nextButton The next button to enable/disable
     * @param displayTextGenerator Function to generate display text from a successful result
     */
    fun updateUIWithStarProcessingResult(
        result: StarProcessingResult,
        textView: TextView,
        nextButton: Button,
        displayTextGenerator: (StarProcessingResult.Success) -> String
    ): StarProcessingResult.Success? {
        return when (result) {
            is StarProcessingResult.Success -> {
                textView.text = displayTextGenerator(result)
                nextButton.isEnabled = true
                result
            }
            is StarProcessingResult.NoStarsFound -> {
                textView.text = result.message
                nextButton.isEnabled = false
                null
            }
            is StarProcessingResult.Error -> {
                textView.text = result.message
                nextButton.isEnabled = false
                null
            }
        }
    }

    /**
     * Show a toast message
     * @param message The message to show
     * @param duration The duration of the toast
     */
    fun showToast(message: String, duration: Int = Toast.LENGTH_LONG) {
        Toast.makeText(context, message, duration).show()
    }
}