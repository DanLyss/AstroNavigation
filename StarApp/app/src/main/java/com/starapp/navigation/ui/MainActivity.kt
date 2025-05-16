package com.starapp.navigation.ui

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Log
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.core.ExperimentalGetImage
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
import com.starapp.navigation.util.ExifUtils
import java.io.*
import java.util.Date
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
@ExperimentalGetImage
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

    // Too bright overlay components
    private lateinit var tooBrightOverlay: FrameLayout
    private lateinit var showNightSkyButton: Button

    // Image analysis for brightness detection
    private var imageAnalysis: ImageAnalysis? = null
    private val brightnessThreshold = CameraManager.BRIGHTNESS_THRESHOLD // Threshold for detecting too bright scenes

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

        // Initialize managers first to ensure they're available for UI components
        initializeManagers()

        // Initialize UI components
        initializeUIComponents()

        // Initialize location and sensor handlers
        initializeLocationAndSensors()

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

        // Initialize too bright overlay components
        tooBrightOverlay = findViewById(R.id.tooBrightOverlay)
        showNightSkyButton = findViewById(R.id.showNightSkyButton)

        // Create a map of UI components for the UIManager
        val components = mapOf(
            "previewView" to previewView,
            "exposureSlider" to exposureSlider,
            "exposureTimeLabel" to exposureTimeLabel,
            "astrometryTimeSlider" to astrometryTimeSlider,
            "astrometryTimeLabel" to astrometryTimeLabel,
            "statusText" to statusText,
            "progressBar" to progressBar,
            "captureButton" to captureButton,
            "chooseButton" to chooseButton,
            "tooBrightOverlay" to tooBrightOverlay,
            "showNightSkyButton" to showNightSkyButton
        )

        // Initialize UI components using UIManager
        uiManager.initializeUIComponents(components) {
            showNightSkyImage()
        }

        // Initialize exposureTimeNs from slider's initial value
        exposureTimeNs = exposureSlider.value.toLong()

        // Initialize astrometryTimeSeconds from slider's initial value
        astrometryTimeSeconds = astrometryTimeSlider.value.toInt()

        cameraExecutor = Executors.newSingleThreadExecutor()
        progressBar.max = 100
        progressBar.progress = 0
    }


    private fun showNightSkyImage() {
        // Use ImageSelectionManager to handle night sky image
        imageSelectionManager.showNightSkyImage(
            onStatusUpdate = { message -> 
                statusText.text = message 
            },
            onComplete = { success ->
                // Hide the overlay
                tooBrightOverlay.visibility = View.GONE

                // Show success or error message
                if (success) {
                    uiManager.showToast("Image saved to gallery successfully")
                } else {
                    uiManager.showToast("Error saving image to gallery")
                }

                // Restart camera
                restartCameraWithExposure()
            }
        )
    }

    private fun updateExposureTimeLabel() {
        uiManager.updateExposureTimeLabel(exposureTimeLabel, exposureTimeNs)
    }

    private fun initializeManagers() {
        permissionManager = PermissionManager(this)
        imageSelectionManager = ImageSelectionManager(this)
        intentManager = IntentManager()
        uiManager = UIManager(this)
        mainManager = MainManager(
            getLocation = { currentLocation },
            onStatusUpdate = { text -> statusText.text = text }
        )
    }

    private fun initializeFileSystem() {
        // Use FileManager to initialize the file system
        FileManager.initializeFileSystem(
            context = this,
            astroPath = astroPath,
            onStatusUpdate = { message -> statusText.text = message },
            onComplete = { success ->
                if (success) {
                    // Start camera preview after file system is initialized
                    if (permissionManager.areAllPermissionsGranted()) {
                        restartCameraWithExposure()
                    }
                }
            }
        )
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
        // Set up buttons using UIManager
        uiManager.setupButtons(
            captureButton = captureButton,
            chooseButton = chooseButton,
            onCaptureClick = {
                currentAngles = sensorHandler.getLatestAngles()
                takePhoto()
            },
            onChooseClick = {
                val intent = intentManager.createGallerySelectionIntent()
                startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE)
            }
        )
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
            currentLocation = currentLocation,
            sensorHandler = sensorHandler,
            onStatusUpdate = { message -> statusText.text = message },
            onPhotoSaved = { file, capturedAngles ->
                // Navigate to CropActivity instead of running solver directly
                // Use the angles captured at the moment the photo was taken
                NavigationManager().navigateToCropActivity(
                    activity = this,
                    imagePath = file.absolutePath,
                    currentLocation = currentLocation,
                    currentAngles = capturedAngles,
                    astrometryTimeSeconds = astrometryTimeSeconds
                )
            },
            onCaptureError = { errorMessage ->
                uiManager.showToast(errorMessage)
            }
        )
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
            currentLocation = currentLocation,
            onStatusUpdate = { message -> 
                statusText.text = message 
            },
            onSuccess = { angles: String ->
                currentAngles = angles
            },
            onError = { errorMessage ->
                uiManager.showToast(errorMessage)
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
    @ExperimentalGetImage
    private fun restartCameraWithExposure() {
        // Hide the too bright overlay when restarting camera
        tooBrightOverlay.visibility = View.GONE

        // Set up image analysis for brightness detection
        setupImageAnalysis()

        CameraManager.restartCameraWithExposureAndAnalysis(
            activity = this,
            previewView = previewView,
            exposureTimeNs = exposureTimeNs,
            imageAnalysis = imageAnalysis,
            onImageCaptureReady = { capture ->
                imageCapture = capture
            },
            onError = { errorMessage ->
                uiManager.showToast(errorMessage)
            }
        )
    }

    @ExperimentalGetImage
    private fun setupImageAnalysis() {
        // Create image analysis use case if it doesn't exist
        if (imageAnalysis == null) {
            // Use CameraManager to set up image analysis
            imageAnalysis = CameraManager.setupImageAnalysis(
                cameraExecutor = cameraExecutor,
                onBrightnessAnalyzed = { isTooBright ->
                    runOnUiThread {
                        // Use UIManager to handle too bright overlay
                        uiManager.handleTooBrightOverlay(tooBrightOverlay, isTooBright)
                    }
                }
            )
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
