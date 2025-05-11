package com.example.cameralong.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.cameralong.R
import com.example.cameralong.gesture.GestureManager
import com.example.cameralong.image.ImageCropManager
import com.example.cameralong.intent.IntentManager
import com.example.cameralong.navigation.NavigationManager
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

    private var imagePath: String? = null
    private var currentLocation: String = "unknown"
    private var currentAngles: String = "unknown"
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
            navigationManager.navigateBack(this)
        }

        // Get views from layout
        imageView = findViewById(R.id.cropImageView)
        cropOverlay = findViewById(R.id.cropOverlay)
        cancelButton = findViewById(R.id.cancelCropButton)
        confirmButton = findViewById(R.id.confirmCropButton)
        statusText = findViewById(R.id.cropStatusText)

        // Extract parameters from intent
        val params = intentManager.extractCropActivityParams(intent)
        imagePath = params.first
        currentLocation = params.second
        currentAngles = params.third

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
        // Load the bitmap from file using the manager
        originalBitmap = imageCropManager.loadImageFromPath(path)
        originalBitmap?.let { bitmap ->
            // Display the bitmap in the ImageView
            imageView.setImageBitmap(bitmap)

            // Set the image dimensions in the overlay view
            cropOverlay.imageWidth = bitmap.width
            cropOverlay.imageHeight = bitmap.height
        }
    }

    private fun setupButtons() {
        // Cancel button - skip cropping and proceed with original image
        cancelButton.setOnClickListener {
            proceedWithImage(imagePath!!)
        }

        // Confirm button - crop the image and proceed
        confirmButton.setOnClickListener {
            val croppedImagePath = cropImage()
            if (croppedImagePath != null) {
                proceedWithImage(croppedImagePath)
            } else {
                // If cropping fails, proceed with original image
                proceedWithImage(imagePath!!)
            }
        }
    }

    private fun cropImage(): String? {
        val bitmap = originalBitmap ?: return null
        val originalImagePath = imagePath ?: return null

        // Get the crop rectangle in image coordinates (0-1 range)
        val cropRect = cropOverlay.getCropRectInImageCoordinates()

        // Use the manager to crop the image
        return imageCropManager.cropImage(
            imagePath = originalImagePath,
            bitmap = bitmap,
            cropRect = cropRect,
            currentLocation = currentLocation,
            currentAngles = currentAngles
        )
    }

    private fun proceedWithImage(path: String) {
        // Use the manager to process the image with astrometry
        imageCropManager.processImageWithAstrometry(
            imagePath = path,
            statusText = statusText,
            currentLocation = currentLocation,
            currentAngles = currentAngles
        )

        // Finish this activity
        finish()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Pass touch events to the gesture manager
        return if (gestureManager.processTouchEvent(event)) {
            true
        } else {
            super.onTouchEvent(event)
        }
    }
}
