package com.starapp.navigation.ui

import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import com.starapp.navigation.R
import com.starapp.navigation.gesture.GestureManager
import com.starapp.navigation.intent.IntentManager
import com.starapp.navigation.navigation.NavigationManager
import com.starapp.navigation.result.ResultManager
import com.starapp.navigation.ui.manager.UIManager

@OptIn(ExperimentalCamera2Interop::class)
class ResultActivity : AppCompatActivity() {
    private lateinit var gestureManager: GestureManager
    private lateinit var resultManager: ResultManager
    private lateinit var navigationManager: NavigationManager
    private lateinit var intentManager: IntentManager
    private lateinit var uiManager: UIManager

    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var overlay: StarOverlayView

    private var currentLocation: String = "unknown"
    private var imagePath: String? = null
    private var starsVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        // Initialize managers
        resultManager = ResultManager(this)
        navigationManager = NavigationManager()
        intentManager = IntentManager()
        uiManager = UIManager(this)

        // Initialize gesture manager for swipe navigation
        gestureManager = GestureManager(this)
        gestureManager.initializeGestureDetector {
            navigationManager.navigateBack(this)
        }

        // Get views from layout
        val imageView = findViewById<ImageView>(R.id.resultImageView)
        overlay = findViewById<StarOverlayView>(R.id.starOverlay)
        val continueButton = findViewById<Button>(R.id.continueButton)
        progressBar = findViewById(R.id.resultProgressBar)

        // Extract parameters from intent
        val params = intentManager.extractResultActivityParams(intent)
        imagePath = params.first
        currentLocation = params.second

        if (imagePath != null) {
            // Load and show the image
            val bitmap = resultManager.loadImage(imagePath!!)

            // Only extract star coordinates if the image was loaded successfully
            val points = if (bitmap != null) {
                resultManager.extractStarCoordinates(imagePath!!)
            } else {
                emptyList()
            }

            // Configure image view and overlay
            uiManager.configureImageViewAndOverlay(
                imageView = imageView,
                overlay = overlay,
                bitmap = bitmap,
                starPoints = points
            )

            // Initially hide the star overlay
            overlay.visibility = android.view.View.INVISIBLE
        }

        // Configure continue button based on .corr file existence
        if (imagePath != null) {
            val hasCorrFile = resultManager.hasCorrFile(imagePath!!)

            uiManager.configureContinueButton(
                continueButton = continueButton,
                hasCorrFile = hasCorrFile
            ) {
                // Show progress bar when navigating to next activity
                progressBar.visibility = android.view.View.VISIBLE

                val corrPath = resultManager.getCorrFilePath(imagePath!!)
                if (corrPath != null) {
                    navigationManager.navigateToStarsActivity(
                        activity = this,
                        imagePath = imagePath!!,
                        corrPath = corrPath,
                        currentLocation = currentLocation
                    )
                } else {
                    // Hide progress bar if navigation fails
                    progressBar.visibility = android.view.View.GONE
                }
            }
        } else {
            // Disable continue button if imagePath is null
            continueButton.isEnabled = false
            continueButton.text = "Cannot Continue - No Image"
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Handle tap events to toggle star overlay visibility
        if (event.action == MotionEvent.ACTION_UP) {
            // Toggle visibility
            starsVisible = !starsVisible
            overlay.visibility = if (starsVisible) android.view.View.VISIBLE else android.view.View.INVISIBLE
            return true
        }

        // Pass touch events to the gesture manager
        return if (gestureManager.processTouchEvent(event)) {
            true
        } else {
            super.onTouchEvent(event)
        }
    }
}
