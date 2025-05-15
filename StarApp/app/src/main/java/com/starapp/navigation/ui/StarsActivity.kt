package com.starapp.navigation.ui

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.Slider
import com.starapp.navigation.R
import com.starapp.navigation.intent.IntentManager
import com.starapp.navigation.navigation.NavigationManager
import com.starapp.navigation.star.StarProcessingManager
import com.starapp.navigation.star.StarProcessingResult
import com.starapp.navigation.ui.manager.UIManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class StarsActivity : AppCompatActivity() {

    private lateinit var thresholdSlider: Slider
    private lateinit var thresholdLabel: TextView
    private lateinit var textView: TextView
    private lateinit var nextButton: Button
    private lateinit var progressBar: android.widget.ProgressBar

    private lateinit var imagePath: String
    private lateinit var corrPath: String
    private var currentLocation: String = "unknown"

    private var matchWeightThreshold = 0.995
    private var processingResult: StarProcessingResult.Success? = null

    // Thread management
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())

    // Managers
    private lateinit var starProcessingManager: StarProcessingManager
    private lateinit var navigationManager: NavigationManager
    private lateinit var intentManager: IntentManager
    private lateinit var uiManager: UIManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stars)

        // Initialize managers
        starProcessingManager = StarProcessingManager(this)
        navigationManager = NavigationManager()
        intentManager = IntentManager()
        uiManager = UIManager(this)

        // Initialize views
        textView = findViewById(R.id.starsText)
        nextButton = findViewById(R.id.nextButton)
        thresholdSlider = findViewById(R.id.thresholdSlider)
        thresholdLabel = findViewById(R.id.thresholdLabel)
        progressBar = findViewById(R.id.starsProgressBar)

        // Initially hide the progress bar
        progressBar.visibility = android.view.View.GONE

        // Extract parameters from intent
        val params = intentManager.extractStarsActivityParams(intent)
        imagePath = params.first
        corrPath = params.second
        currentLocation = params.third

        // Configure threshold slider with debouncing (500ms)
        uiManager.configureThresholdSlider(
            slider = thresholdSlider,
            label = thresholdLabel,
            initialValue = matchWeightThreshold
        ) { newValue ->
            matchWeightThreshold = newValue
            processStars()
        }

        // Initial processing
        processStars()

        // Set up next button
        nextButton.setOnClickListener {
            processingResult?.let { result ->
                navigationManager.navigateToLocationActivity(
                    activity = this,
                    result = result,
                    imagePath = imagePath,
                    currentLocation = currentLocation
                )
            }
        }
    }

    private fun processStars() {
        // Show loading indicator and toast message
        progressBar.visibility = android.view.View.VISIBLE
        // Force layout update to ensure progress bar is shown immediately
        progressBar.requestLayout()
        progressBar.parent?.requestLayout()
        android.widget.Toast.makeText(
            this,
            "Recalculating star clusters with threshold: " + String.format("%.3f", matchWeightThreshold),
            android.widget.Toast.LENGTH_SHORT
        ).show()

        // Process stars in background thread
        executor.execute {
            try {
                // Small delay to ensure UI updates before processing starts
                Thread.sleep(100)

                // Process stars using the manager (in background)
                val result = starProcessingManager.processStars(imagePath, corrPath, matchWeightThreshold)

                // Update UI on main thread
                handler.post {
                    // Update UI based on result
                    processingResult = uiManager.updateUIWithStarProcessingResult(
                        result = result,
                        textView = textView,
                        nextButton = nextButton,
                        displayTextGenerator = starProcessingManager::generateDisplayText
                    )

                    // Show a success message
                    if (result is StarProcessingResult.Success) {
                        android.widget.Toast.makeText(
                            this@StarsActivity,
                            "Found ${result.cluster.stars.size} stars with threshold: " + String.format("%.3f", matchWeightThreshold),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }

                    // Hide progress bar
                    progressBar.visibility = android.view.View.GONE
                }
            } catch (e: Exception) {
                // Handle exceptions on main thread
                handler.post {
                    android.widget.Toast.makeText(
                        this@StarsActivity,
                        "Error processing stars: ${e.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()

                    // Hide progress bar
                    progressBar.visibility = android.view.View.GONE
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        // Shutdown executor to prevent memory leaks
        executor.shutdown()
        try {
            // Wait a moment for tasks to complete
            if (!executor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
        }
    }
}
