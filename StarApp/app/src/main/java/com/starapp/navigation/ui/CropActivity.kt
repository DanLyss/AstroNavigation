package com.starapp.navigation.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.starapp.navigation.R
import com.starapp.navigation.gesture.GestureManager
import com.starapp.navigation.image.ImageCropManager
import com.starapp.navigation.intent.IntentManager
import com.starapp.navigation.navigation.NavigationManager
import com.starapp.navigation.util.ExifUtils
import androidx.exifinterface.media.ExifInterface
import java.io.File

class CropActivity : AppCompatActivity() {
    private lateinit var gestureManager: GestureManager
    private lateinit var navigationManager: NavigationManager
    private lateinit var intentManager: IntentManager
    private lateinit var imageCropManager: ImageCropManager

    private lateinit var imageView: ImageView
    private lateinit var cropOverlay: CropOverlayView
    private lateinit var cancelButton: Button
    private lateinit var confirmButton: Button
    private lateinit var statusText: TextView
    private lateinit var anglesText: TextView
    private lateinit var progressBar: android.widget.ProgressBar

    private var imagePath: String? = null
    private var currentLocation: String = "unknown"
    private var currentAngles: String = "unknown"
    private var astrometryTimeSeconds: Int = 100
    private var originalBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop)

        // Initialize managers
        navigationManager = NavigationManager()
        intentManager = IntentManager()
        imageCropManager = ImageCropManager(this)

        // Initialize gesture manager for swipe navigation
        gestureManager = GestureManager(this)
        gestureManager.initializeGestureDetector {
            // Cancel any running solver process before navigating back
            com.starapp.navigation.astro.AstrometryManager.cancelSolver(progressBar, statusText)
            navigationManager.navigateBack(this)
        }

        // Get views from layout
        imageView = findViewById(R.id.cropImageView)
        cropOverlay = findViewById(R.id.cropOverlay)
        cancelButton = findViewById(R.id.cancelCropButton)
        confirmButton = findViewById(R.id.confirmCropButton)
        statusText = findViewById(R.id.cropStatusText)
        anglesText = findViewById(R.id.exposureTimeText)
        progressBar = findViewById(R.id.cropProgressBar)

        // Make progress bar and status text visible immediately
        progressBar.visibility = android.view.View.VISIBLE
        progressBar.progress = 0
        statusText.visibility = android.view.View.VISIBLE
        statusText.text = "Ready to process image. Crop or continue."

        // Extract parameters from intent
        val params = intentManager.extractCropActivityParams(intent)
        imagePath = params.first
        currentLocation = params.second
        currentAngles = params.third
        astrometryTimeSeconds = params.fourth

        // Load and display the image
        if (imagePath != null) {
            loadAndDisplayImage(imagePath!!)
        } else {
            // If no image path, finish the activity
            finish()
            return
        }

        // Set up button click listeners
        setupButtons()
    }

    private fun loadAndDisplayImage(path: String) {
        // Show loading state
        statusText.text = "Loading image..."

        // Load the bitmap from file using the manager asynchronously
        imageCropManager.loadImageFromPath(path) { bitmap ->
            originalBitmap = bitmap
            bitmap?.let {
                // Display the bitmap in the ImageView
                imageView.setImageBitmap(it)

                // Set the image dimensions in the overlay view
                cropOverlay.imageWidth = it.width
                cropOverlay.imageHeight = it.height

                // Force redraw of the overlay view
                cropOverlay.invalidate()

                // Extract angles from EXIF data if available
                try {
                    val exif = androidx.exifinterface.media.ExifInterface(path)
                    val angles = com.starapp.navigation.util.ExifUtils.extractOrientationAngles(exif)
                    val anglesStr = "Yaw=${angles.first}, Pitch=${angles.second}, Roll=${angles.third}"

                    // Update currentAngles if we got valid data from EXIF
                    if (angles != Triple(0.0, 0.0, 0.0)) {
                        currentAngles = anglesStr
                    }

                    // Display angles from EXIF data or from intent
                    anglesText.text = "Angles at capture: $currentAngles"
                } catch (e: Exception) {
                    // If there's an error reading EXIF, use the angles from intent
                    anglesText.text = "Angles at capture: $currentAngles"
                }

                // Reset status text
                statusText.text = "Ready to process image. Crop or continue."
            } ?: run {
                // Handle error case
                statusText.text = "Error loading image"
            }
        }
    }

    private fun setupButtons() {
        // Cancel button - skip cropping and proceed with original image
        cancelButton.setOnClickListener {
            proceedWithImage(imagePath!!)
        }

        // Confirm button - crop the image and proceed
        confirmButton.setOnClickListener {
            // Show cropping state
            statusText.text = "Cropping image..."

            // Use the asynchronous version with callback
            cropImage { croppedImagePath ->
                if (croppedImagePath != null) {
                    proceedWithImage(croppedImagePath)
                } else {
                    // If cropping fails, proceed with original image
                    proceedWithImage(imagePath!!)
                }
            }
        }
    }

    private fun cropImage(callback: (String?) -> Unit) {
        val bitmap = originalBitmap
        val originalImagePath = imagePath

        if (bitmap == null || originalImagePath == null) {
            callback(null)
            return
        }

        // Show cropping state
        statusText.text = "Cropping image..."

        // Get the crop rectangle in image coordinates (0-1 range)
        val cropRect = cropOverlay.getCropRectInImageCoordinates()

        // Use the manager to crop the image asynchronously
        imageCropManager.cropImage(
            imagePath = originalImagePath,
            bitmap = bitmap,
            cropRect = cropRect,
            currentLocation = currentLocation,
            currentAngles = currentAngles
        ) { croppedImagePath ->
            // Reset status text
            statusText.text = if (croppedImagePath != null) {
                "Image cropped successfully"
            } else {
                "Error cropping image"
            }

            // Pass the result to the callback
            callback(croppedImagePath)
        }
    }

    private fun proceedWithImage(path: String) {
        // Show loading indicator and update status text
        progressBar.visibility = android.view.View.VISIBLE
        statusText.visibility = android.view.View.VISIBLE
        statusText.text = "Starting star analysis... Please wait"

        // Show a toast message to inform the user to wait
        android.widget.Toast.makeText(
            this,
            "Starting star analysis. Please wait while computations are performed.",
            android.widget.Toast.LENGTH_LONG
        ).show()

        // Use the manager to process the image with astrometry
        imageCropManager.processImageWithAstrometry(
            imagePath = path,
            statusText = statusText,
            progressBar = progressBar,
            currentLocation = currentLocation,
            currentAngles = currentAngles,
            astrometryTimeSeconds = astrometryTimeSeconds
        )

        // Don't finish the activity - let the solver run and show progress
        // The AstrometryManager will navigate to the next activity when done
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Pass touch events to the gesture manager
        return if (gestureManager.processTouchEvent(event)) {
            true
        } else {
            super.onTouchEvent(event)
        }
    }

    override fun onPause() {
        super.onPause()
        // Cancel any running solver process when the activity is paused
        com.starapp.navigation.astro.AstrometryManager.cancelSolver(progressBar, statusText)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel any running solver process when the activity is destroyed
        com.starapp.navigation.astro.AstrometryManager.cancelSolver(progressBar, statusText)
    }

    override fun onResume() {
        super.onResume()
        // Restore progress bar visibility when returning from stars activity
        if (progressBar.progress > 0) {
            progressBar.progress = 0
            progressBar.visibility = android.view.View.VISIBLE
            statusText.visibility = android.view.View.VISIBLE
        }
    }
}
