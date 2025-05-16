package com.starapp.navigation.ui.manager

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.starapp.navigation.star.StarProcessingResult
import com.starapp.navigation.ui.StarOverlayView
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
     * @param onValueChanged Callback to be executed when the slider value changes and stops for 500ms
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

        // For debouncing slider changes
        var debounceJob: java.util.Timer? = null
        val debounceDelay = 500L // 500ms delay

        // Set up slider listener
        slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                val newValue = value.toDouble()
                updateThresholdLabel(label, newValue)

                // Cancel previous timer if it exists
                debounceJob?.cancel()

                // Create new timer for debouncing
                debounceJob = java.util.Timer().apply {
                    schedule(object : java.util.TimerTask() {
                        override fun run() {
                            // Execute on UI thread since this might be called from a background thread
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                onValueChanged(newValue)
                            }
                        }
                    }, debounceDelay)
                }
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

    /**
     * Initialize UI components
     * @param components Map of UI component IDs to their corresponding views
     * @param onShowNightSkyButtonClick Callback for when the show night sky button is clicked
     * @return Map of initialized UI components
     */
    fun initializeUIComponents(
        components: Map<String, View>,
        onShowNightSkyButtonClick: () -> Unit
    ): Map<String, View> {
        // Extract UI components from the map
        val tooBrightOverlay = components["tooBrightOverlay"] as FrameLayout
        val showNightSkyButton = components["showNightSkyButton"] as Button
        val exposureSlider = components["exposureSlider"] as Slider
        val exposureTimeLabel = components["exposureTimeLabel"] as TextView
        val astrometryTimeSlider = components["astrometryTimeSlider"] as Slider
        val astrometryTimeLabel = components["astrometryTimeLabel"] as TextView

        // Set up the show night sky button click listener
        showNightSkyButton.setOnClickListener {
            // Hide the button after it's pressed
            showNightSkyButton.visibility = View.GONE
            onShowNightSkyButtonClick()
        }

        // Initialize astrometryTimeSeconds from slider's initial value
        val astrometryTimeSeconds = astrometryTimeSlider.value.toInt()
        astrometryTimeLabel.text = "Astrometry time: ${astrometryTimeSeconds}s"

        return components
    }

    /**
     * Update exposure time label
     * @param exposureTimeLabel The label to update
     * @param exposureTimeNs The exposure time in nanoseconds
     */
    fun updateExposureTimeLabel(exposureTimeLabel: TextView, exposureTimeNs: Long) {
        // Convert nanoseconds to milliseconds for display
        val exposureTimeMs = exposureTimeNs / 1_000_000
        exposureTimeLabel.text = "Exposure time: $exposureTimeMs ms"
    }

    /**
     * Set up exposure slider
     * @param exposureSlider The slider to set up
     * @param exposureTimeLabel The label to update
     * @param initialExposureTimeNs The initial exposure time in nanoseconds
     * @param onExposureChanged Callback for when the exposure time changes
     * @return Handler for exposure slider debounce
     */
    fun setupExposureSlider(
        exposureSlider: Slider,
        exposureTimeLabel: TextView,
        initialExposureTimeNs: Long,
        onExposureChanged: (Long) -> Unit
    ): Handler {
        // Initialize with initial value
        updateExposureTimeLabel(exposureTimeLabel, initialExposureTimeNs)

        // Create handler for debouncing
        val exposureHandler = Handler(Looper.getMainLooper())
        var exposureRunnable: Runnable? = null

        exposureSlider.addOnChangeListener { _, value, fromUser ->
            val exposureTimeNs = value.toLong()
            updateExposureTimeLabel(exposureTimeLabel, exposureTimeNs)

            // Only debounce if the change is from user interaction
            if (fromUser) {
                // Remove any pending runnables
                exposureRunnable?.let { exposureHandler.removeCallbacks(it) }

                // Create a new runnable for camera restart
                exposureRunnable = Runnable {
                    onExposureChanged(exposureTimeNs)
                }.also {
                    // Schedule the runnable after a delay (300ms)
                    exposureHandler.postDelayed(it, 300)
                }
            } else {
                // If programmatic change, restart immediately
                onExposureChanged(exposureTimeNs)
            }
        }

        return exposureHandler
    }

    /**
     * Set up astrometry time slider
     * @param astrometryTimeSlider The slider to set up
     * @param astrometryTimeLabel The label to update
     * @param onAstrometryTimeChanged Callback for when the astrometry time changes
     */
    fun setupAstrometryTimeSlider(
        astrometryTimeSlider: Slider,
        astrometryTimeLabel: TextView,
        onAstrometryTimeChanged: (Int) -> Unit
    ) {
        astrometryTimeSlider.addOnChangeListener { _, value, _ ->
            val astrometryTimeSeconds = value.toInt()
            astrometryTimeLabel.text = "Astrometry time: ${astrometryTimeSeconds}s"
            onAstrometryTimeChanged(astrometryTimeSeconds)
        }
    }

    /**
     * Set up buttons
     * @param captureButton The capture button
     * @param chooseButton The choose button
     * @param onCaptureClick Callback for when the capture button is clicked
     * @param onChooseClick Callback for when the choose button is clicked
     */
    fun setupButtons(
        captureButton: Button,
        chooseButton: Button,
        onCaptureClick: () -> Unit,
        onChooseClick: () -> Unit
    ) {
        captureButton.setOnClickListener { onCaptureClick() }
        chooseButton.setOnClickListener { onChooseClick() }
    }

    /**
     * Handle too bright overlay visibility
     * @param tooBrightOverlay The overlay to show/hide
     * @param isTooBright Whether the scene is too bright
     */
    fun handleTooBrightOverlay(tooBrightOverlay: FrameLayout, isTooBright: Boolean) {
        if (isTooBright) {
            // Scene is too bright, show the overlay
            if (tooBrightOverlay.visibility != View.VISIBLE) {
                ensureTooBrightOverlayContent(tooBrightOverlay)
                tooBrightOverlay.visibility = View.VISIBLE
            }
        } else {
            // Scene is not too bright, hide the overlay
            if (tooBrightOverlay.visibility != View.GONE) {
                tooBrightOverlay.visibility = View.GONE
            }
        }
    }

    /**
     * Ensures that the too bright overlay has the original content (text and button)
     * @param tooBrightOverlay The overlay to check
     */
    private fun ensureTooBrightOverlayContent(tooBrightOverlay: FrameLayout) {
        // Only recreate the content if the overlay is empty
        if (tooBrightOverlay.childCount == 0) {
            // The overlay is defined in the layout XML, so we don't need to recreate it
            // This method is just a safeguard in case the views were removed
        }
    }
}
