package com.starapp.navigation.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.view.PreviewView
import com.google.android.material.slider.Slider
import com.starapp.navigation.R
import com.starapp.navigation.camera.CameraManager
import com.starapp.navigation.file.FileManager
import com.starapp.navigation.image.ImageSelectionManager
import com.starapp.navigation.intent.IntentManager
import com.starapp.navigation.location.LocationHandler
import com.starapp.navigation.location.SensorHandler
import com.starapp.navigation.main.MainManager
import com.starapp.navigation.navigation.NavigationManager
import com.starapp.navigation.permission.PermissionManager
import com.starapp.navigation.ui.manager.UIManager
import java.io.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
class MainActivity : AppCompatActivity() {

    // UI components
    private lateinit var previewView: PreviewView
    private lateinit var exposureSlider: Slider
    private lateinit var exposureTimeLabel: TextView
    private lateinit var astrometryTimeSlider: Slider
    private lateinit var astrometryTimeLabel: TextView
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var captureButton: Button
    private lateinit var chooseButton: Button

    // Camera-related
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null
    private var exposureTimeNs: Long = 1_000_000_000L

    // Astrometry-related
    private var astrometryTimeSeconds: Int = 100

    // Location and sensor data
    private var currentLocation: String = "unknown"
    private var currentAngles: String = "unknown"


    // Managers
    private lateinit var locationHandler: LocationHandler
    private lateinit var sensorHandler: SensorHandler
    private lateinit var permissionManager: PermissionManager
    private lateinit var imageSelectionManager: ImageSelectionManager
    private lateinit var intentManager: IntentManager
    private lateinit var uiManager: UIManager
    private lateinit var mainManager: MainManager


    // Debounce for exposure slider
    private val exposureHandler = Handler(Looper.getMainLooper())
    private var exposureRunnable: Runnable? = null

    // Constants
    private val PICK_IMAGE_REQUEST_CODE = 42
    private val astroPath by lazy { filesDir.absolutePath + "/astro" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        initializeUIComponents()

        // Initialize location and sensor handlers
        initializeLocationAndSensors()

        // Initialize managers
        initializeManagers()

        // Initialize directories and extract assets
        initializeFileSystem()

        // Clean up old .corr files when MainActivity is created (asynchronously)
        FileManager.cleanupCorrFilesAsync(astroPath)

        // Set up exposure slider
        setupExposureSlider()

        // Request permissions and start camera if granted
        checkAndRequestPermissions()

        // Set up buttons
        setupButtons()
    }

    private fun initializeUIComponents() {
        previewView = findViewById(R.id.previewView)
        exposureSlider = findViewById(R.id.exposureSlider)
        exposureTimeLabel = findViewById(R.id.exposureTimeLabel)
        astrometryTimeSlider = findViewById(R.id.astrometryTimeSlider)
        astrometryTimeLabel = findViewById(R.id.astrometryTimeLabel)
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)
        captureButton = findViewById(R.id.captureButton)
        chooseButton = findViewById(R.id.chooseButton)

        // Initialize exposureTimeNs from slider's initial value and update label
        exposureTimeNs = exposureSlider.value.toLong()
        updateExposureTimeLabel()

        // Initialize astrometryTimeSeconds from slider's initial value
        astrometryTimeSeconds = astrometryTimeSlider.value.toInt()
        astrometryTimeLabel.text = "Astrometry time: ${astrometryTimeSeconds}s"

        cameraExecutor = Executors.newSingleThreadExecutor()
        progressBar.max = 100
        progressBar.progress = 0
    }

    private fun updateExposureTimeLabel() {
        // Convert nanoseconds to milliseconds for display
        val exposureTimeMs = exposureTimeNs / 1_000_000
        exposureTimeLabel.text = "Exposure time: $exposureTimeMs ms"
    }

    private fun initializeManagers() {
        permissionManager = PermissionManager(this)
        imageSelectionManager = ImageSelectionManager(this)
        intentManager = IntentManager()
        uiManager = UIManager(this)
        mainManager = MainManager(statusText) { currentLocation }
    }

    private fun initializeFileSystem() {
        // Show a loading indicator or message if needed
        statusText.text = "Initializing..."

        // Use a background thread for file operations
        Thread {
            try {
                FileManager.createDirectories(astroPath)
                FileManager.extractZipAssets(this, astroPath)
                FileManager.generateAstrometryConfig(astroPath)

                // Update UI on main thread when done
                runOnUiThread {
                    statusText.text = ""
                    // Start camera preview after file system is initialized
                    if (permissionManager.areAllPermissionsGranted()) {
                        restartCameraWithExposure()
                    }
                }
            } catch (e: Exception) {
                // Handle errors on main thread
                runOnUiThread {
                    statusText.text = "Initialization error: ${e.message}"
                }
            }
        }.start()
    }

    private fun initializeLocationAndSensors() {
        locationHandler = LocationHandler(this)

        locationHandler.startListening { location ->
            currentLocation = "${location.latitude}, ${location.longitude}"
        }

        sensorHandler = SensorHandler(this)
        sensorHandler.startListening()
        sensorHandler.setOnChangeListener { angles -> currentAngles = angles }
    }

    private fun setupExposureSlider() {
        exposureSlider.addOnChangeListener { _, value, fromUser ->
            exposureTimeNs = value.toLong()
            updateExposureTimeLabel()

            // Only debounce if the change is from user interaction
            if (fromUser) {
                // Remove any pending runnables
                exposureRunnable?.let { exposureHandler.removeCallbacks(it) }

                // Create a new runnable for camera restart
                exposureRunnable = Runnable {
                    restartCameraWithExposure()
                }.also {
                    // Schedule the runnable after a delay (300ms)
                    exposureHandler.postDelayed(it, 300)
                }
            } else {
                // If programmatic change, restart immediately
                restartCameraWithExposure()
            }
        }

        // Setup astrometry time slider
        astrometryTimeSlider.addOnChangeListener { _, value, _ ->
            astrometryTimeSeconds = value.toInt()
            astrometryTimeLabel.text = "Astrometry time: ${astrometryTimeSeconds}s"
        }
    }

    private fun checkAndRequestPermissions() {
        if (permissionManager.areAllPermissionsGranted()) {
            restartCameraWithExposure()
        } else {
            permissionManager.requestPermissions(this)
        }
    }

    private fun setupButtons() {
        // Set up capture button
        captureButton.setOnClickListener {
            currentAngles = sensorHandler.getLatestAngles()
            takePhoto()
        }

        // Set up choose button
        chooseButton.setOnClickListener {
            val intent = intentManager.createGallerySelectionIntent()
            startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE)
        }
    }

    private fun takePhoto() {
        // Clean up old .corr files before taking a new photo (asynchronously)
        // Use a callback to continue with photo capture after cleanup
        FileManager.cleanupCorrFilesAsync(astroPath) {
            // This runs after cleanup is complete
            runOnUiThread {
                continueWithPhotoCapture()
            }
        }
    }

    private fun continueWithPhotoCapture() {
        val photoFile = File(astroPath, "input.jpg")
        CameraManager.takePhoto(
            context = this,
            imageCapture = imageCapture,
            outputFile = photoFile,
            statusText = statusText,
            currentLocation = currentLocation,
            sensorHandler = sensorHandler
        ) { file, capturedAngles ->
            // Navigate to CropActivity instead of running solver directly
            // Use the angles captured at the moment the photo was taken
            NavigationManager().navigateToCropActivity(
                activity = this,
                imagePath = file.absolutePath,
                currentLocation = currentLocation,
                currentAngles = capturedAngles,
                astrometryTimeSeconds = astrometryTimeSeconds
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            // Store data for use after cleanup
            val imageData = data

            // Clean up old .corr files before processing a selected image (asynchronously)
            FileManager.cleanupCorrFilesAsync(astroPath) {
                // This runs after cleanup is complete
                runOnUiThread {
                    continueWithImageProcessing(imageData)
                }
            }
        }
    }

    private fun continueWithImageProcessing(data: Intent) {
        val uri = intentManager.extractGallerySelectionUri(data) ?: return
        val destFile = File(astroPath, "input.jpg")

        imageSelectionManager.processSelectedImage(
            uri = uri,
            destFile = destFile,
            statusText = statusText,
            currentLocation = currentLocation,
            onSuccess = { angles: String ->
                currentAngles = angles
            }
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (permissionManager.handlePermissionResult(requestCode, grantResults)) {
            restartCameraWithExposure()
        } else {
            uiManager.showToast("❌ All permissions are required")
        }
    }

    @androidx.camera.camera2.interop.ExperimentalCamera2Interop
    private fun restartCameraWithExposure() {
        CameraManager.restartCameraWithExposure(
            activity = this,
            previewView = previewView,
            exposureTimeNs = exposureTimeNs
        ) { capture ->
            imageCapture = capture
        }
    }

    override fun onPause() {
        super.onPause()
        locationHandler.stopListening()
        sensorHandler.stopListening()
        mainManager.stop()
    }

    override fun onResume() {
        super.onResume()
        sensorHandler.startListening()
        mainManager.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove any pending exposure slider callbacks
        exposureRunnable?.let { exposureHandler.removeCallbacks(it) }
        cameraExecutor.shutdown()
    }
}
