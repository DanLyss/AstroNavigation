package com.example.cameralong

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import com.google.android.material.slider.Slider
import java.io.File
import kotlintranslation.AstrometryReader
import kotlintranslation.StarCluster
import kotlintranslation.Star

class StarsActivity : AppCompatActivity() {

    private lateinit var thresholdSlider: Slider
    private lateinit var thresholdLabel: TextView
    private lateinit var textView: TextView
    private lateinit var nextButton: Button

    private lateinit var imagePath: String
    private lateinit var corrPath: String
    private lateinit var imageFile: File
    private lateinit var corrFile: File
    private var width = 0
    private var height = 0
    private lateinit var exif: ExifInterface
    private var yawDeg = 0.0
    private var pitchDeg = 0.0
    private var rollDeg = 0.0
    private lateinit var isoTime: String
    private var currentLocation: String = "unknown"

    private var matchWeightThreshold = 0.995
    private lateinit var cluster: StarCluster

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stars)

        // Initialize views
        textView = findViewById(R.id.starsText)
        nextButton = findViewById(R.id.nextButton)
        thresholdSlider = findViewById(R.id.thresholdSlider)
        thresholdLabel = findViewById(R.id.thresholdLabel)

        // Get paths and location from intent
        imagePath = intent.getStringExtra("imagePath")
            ?: throw IllegalStateException("No imagePath in Intent")
        corrPath = intent.getStringExtra("corrPath")
            ?: throw IllegalStateException("No corrPath in Intent")
        currentLocation = intent.getStringExtra("currentLocation") ?: "unknown"

        // Validate files
        imageFile = File(imagePath)
        require(imageFile.exists()) { "Image not found at $imagePath" }
        corrFile = File(corrPath)
        require(corrFile.exists()) { "Corr file not found at $corrPath" }

        // Get image dimensions
        val dimensions = ExifUtils.getImageDimensions(imageFile)
        width = dimensions.first
        height = dimensions.second

        // Extract EXIF data
        exif = ExifInterface(imagePath)
        val angles = ExifUtils.extractOrientationAngles(exif)
        yawDeg = angles.first
        pitchDeg = angles.second
        rollDeg = angles.third
        isoTime = ExifUtils.extractDateTime(exif)

        // Set up the threshold slider
        thresholdSlider.valueTo = 1.0f  // Set maximum value to 1.0 to prevent crashes
        thresholdSlider.value = matchWeightThreshold.toFloat()
        updateThresholdLabel()

        // Set up slider listener
        thresholdSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                matchWeightThreshold = value.toDouble()
                updateThresholdLabel()
                processStars()
                updateDisplay()
            }
        }

        // Initial processing
        processStars()
        updateDisplay()

        // Set up next button
        nextButton.setOnClickListener {
            Intent(this, LocationActivity::class.java).also { intent ->
                intent.putExtra("pred_phi", cluster.phi)
                intent.putExtra("pred_long", cluster.longitude)
                intent.putExtra("imagePath", imagePath)
                intent.putExtra("currentLocation", currentLocation)
                startActivity(intent)
            }
        }
    }

    private fun updateThresholdLabel() {
        thresholdLabel.text = "Star Match Weight Threshold: %.3f".format(matchWeightThreshold)
    }

    private fun processStars() {
        // Ensure threshold is not greater than 1.0 to prevent crashes
        val safeThreshold = minOf(matchWeightThreshold, 1.0)
        if (safeThreshold != matchWeightThreshold) {
            matchWeightThreshold = safeThreshold
            thresholdSlider.value = safeThreshold.toFloat()
            updateThresholdLabel()
        }

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
                Log.w("StarsActivity", "No stars found with threshold $safeThreshold")
                textView.text = "No stars found with current threshold. Try lowering the threshold value."
                nextButton.isEnabled = false
                return
            }

            // Create star cluster
            cluster = StarCluster(
                stars = stars,
                positionalAngle = Math.toRadians(pitchDeg),
                rotationAngle = Math.toRadians(rollDeg),
                timeGMT = isoTime
            )

            // Enable next button if we have stars
            nextButton.isEnabled = true
        } catch (e: Exception) {
            Log.e("StarsActivity", "Error processing stars: ${e.message}", e)
            textView.text = "Error processing stars: ${e.message}\nTry lowering the threshold value."
            nextButton.isEnabled = false
        }
    }

    private fun updateDisplay() {
        textView.text = buildString {
            appendLine("X size: size: $width")
            appendLine("Y size: size: $height")
            appendLine("Computed time: $isoTime")
            appendLine("Yaw:   $yawDeg°")
            appendLine("Pitch: $pitchDeg°")
            appendLine("Roll:  $rollDeg°")
            appendLine()
            appendLine("Found ${cluster.stars.size} stars")
            appendLine("X size: %.2f° per 100 pixels".format(Math.toDegrees(cluster.angularXSize)))
            appendLine("Y size: %.2f° per 100 pixels".format(Math.toDegrees(cluster.angularYSize)))
            appendLine("Cluster Az₀: %.2f°".format(Math.toDegrees(cluster.AzStar0)))
            appendLine()
            cluster.stars.forEachIndexed { i, star ->
                appendLine(
                    "⭐ Star ${i + 1}: Alt = %.1f°, Az = %.1f°"
                        .format(Math.toDegrees(star.Alt!!), Math.toDegrees(star.Az!!))
                )
            }
        }
    }
}
