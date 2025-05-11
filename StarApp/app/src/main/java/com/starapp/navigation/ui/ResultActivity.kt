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

    private var currentLocation: String = "unknown"
    private var imagePath: String? = null

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
        val overlay = findViewById<StarOverlayView>(R.id.starOverlay)
        val continueButton = findViewById<Button>(R.id.continueButton)

        // Extract parameters from intent
        val params = intentManager.extractResultActivityParams(intent)
        imagePath = params.first
        currentLocation = params.second

        if (imagePath != null) {
            // Load and show the image
            val bitmap = resultManager.loadImage(imagePath!!)
            val points = resultManager.extractStarCoordinates(imagePath!!)

            // Configure image view and overlay
            uiManager.configureImageViewAndOverlay(
                imageView = imageView,
                overlay = overlay,
                bitmap = bitmap,
                starPoints = points
            )
        }

        // Configure continue button based on .corr file existence
        if (imagePath != null) {
            val hasCorrFile = resultManager.hasCorrFile(imagePath!!)

            uiManager.configureContinueButton(
                continueButton = continueButton,
                hasCorrFile = hasCorrFile
            ) {
                val corrPath = resultManager.getCorrFilePath(imagePath!!)
                if (corrPath != null) {
                    navigationManager.navigateToStarsActivity(
                        activity = this,
                        imagePath = imagePath!!,
                        corrPath = corrPath,
                        currentLocation = currentLocation
                    )
                }
            }
        } else {
            // Disable continue button if imagePath is null
            continueButton.isEnabled = false
            continueButton.text = "Cannot Continue - No Image"
        }
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

