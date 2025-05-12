package com.starapp.navigation.ui

import android.os.Bundle
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

class StarsActivity : AppCompatActivity() {

    private lateinit var thresholdSlider: Slider
    private lateinit var thresholdLabel: TextView
    private lateinit var textView: TextView
    private lateinit var nextButton: Button

    private lateinit var imagePath: String
    private lateinit var corrPath: String
    private var currentLocation: String = "unknown"

    private var matchWeightThreshold = 0.995
    private var processingResult: StarProcessingResult.Success? = null

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
        // Process stars using the manager
        val result = starProcessingManager.processStars(imagePath, corrPath, matchWeightThreshold)

        // Update UI based on result
        processingResult = uiManager.updateUIWithStarProcessingResult(
            result = result,
            textView = textView,
            nextButton = nextButton,
            displayTextGenerator = starProcessingManager::generateDisplayText
        )
    }
}
