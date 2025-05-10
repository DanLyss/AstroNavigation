package com.example.cameralong

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.core.view.GestureDetectorCompat
import java.io.File

@OptIn(ExperimentalCamera2Interop::class)
class ResultActivity : AppCompatActivity() {
    private lateinit var gestureDetector: GestureDetectorCompat
    private var currentLocation: String = "unknown"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        // Initialize gesture detector for swipe navigation
        gestureDetector = GestureDetectorCompat(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
                // Check if it's a left-to-right swipe (to go back)
                if (e1 != null && e2 != null && e2.x - e1.x > 100 && Math.abs(velocityX) > 100) {
                    // Just finish this activity to return to the previous one (MainActivity)
                    finish()
                    return true
                }
                return false
            }
        })

        // Get views from layout
        val imageView = findViewById<ImageView>(R.id.resultImageView)
        val overlay = findViewById<StarOverlayView>(R.id.starOverlay)
        val continueButton = findViewById<Button>(R.id.continueButton)

        // Retrieve image path and location from intent
        val imagePath = intent.getStringExtra("imagePath")
        currentLocation = intent.getStringExtra("currentLocation") ?: "unknown"

        if (imagePath != null) {
            val file = File(imagePath)

            if (file.exists()) {
                // Load and show the image
                val bitmap = BitmapFactory.decodeFile(imagePath)
                imageView.setImageBitmap(bitmap)

                // Set image dimensions on the overlay view
                overlay.imageWidth = bitmap.width
                overlay.imageHeight = bitmap.height

                // Check if corresponding .corr file exists
                val corrPath = File(imagePath).nameWithoutExtension + ".corr"
                val corrFile = File(filesDir, "astro/output/$corrPath")
                if (corrFile.exists()) {
                    try {
                        // Extract star coordinates using FitsUtils
                        val points = FitsUtils.extractStarCoordinates(corrFile)

                        // Pass coordinates to overlay view
                        overlay.starPoints = points
                        overlay.invalidate()

                    } catch (e: Exception) {
                        Toast.makeText(this, "Error reading .corr file: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, ".corr file not found: ${corrFile.absolutePath}", Toast.LENGTH_SHORT).show()
                }

            } else {
                imageView.setImageResource(android.R.drawable.ic_delete)
                Toast.makeText(this, "Image file not found", Toast.LENGTH_SHORT).show()
            }
        }

        // Check if .corr file exists and set button state accordingly
        val imgPath = intent.getStringExtra("imagePath")
        if (imgPath != null) {
            val corrPath = File(imgPath).nameWithoutExtension + ".corr"
            val corrFile = File(filesDir, "astro/output/$corrPath")

            if (corrFile.exists()) {
                // Enable continue button only if .corr file exists
                continueButton.isEnabled = true
                continueButton.text = "Continue to Star Analysis"

                // On click: go to the next screen
                continueButton.setOnClickListener {
                    val fullCorr = corrFile.absolutePath
                    Intent(this, StarsActivity::class.java).also { i ->
                        i.putExtra("imagePath", imgPath)
                        i.putExtra("corrPath", fullCorr)
                        i.putExtra("currentLocation", currentLocation)
                        startActivity(i)
                    }
                    finish()
                }
            } else {
                // Disable continue button if .corr file doesn't exist
                continueButton.isEnabled = false
                continueButton.text = "Cannot Continue - No Star Data"
                continueButton.setOnClickListener {
                    Toast.makeText(this, "Cannot continue: No star data available for this image", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            // Disable continue button if imagePath is null
            continueButton.isEnabled = false
            continueButton.text = "Cannot Continue - No Image"
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Pass touch events to the gesture detector
        return if (gestureDetector.onTouchEvent(event)) {
            true
        } else {
            super.onTouchEvent(event)
        }
    }
}
