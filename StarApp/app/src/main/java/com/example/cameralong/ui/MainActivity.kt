package com.example.cameralong.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.view.PreviewView
import com.google.android.material.slider.Slider
import com.example.cameralong.R
import com.example.cameralong.astro.AstrometryManager
import com.example.cameralong.camera.CameraManager
import com.example.cameralong.file.FileManager
import com.example.cameralong.image.ImageSelectionManager
import com.example.cameralong.intent.IntentManager
import com.example.cameralong.location.LocationHandler
import com.example.cameralong.location.SensorHandler
import com.example.cameralong.navigation.NavigationManager
import com.example.cameralong.permission.PermissionManager
import com.example.cameralong.ui.manager.UIManager
import java.io.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
class MainActivity : AppCompatActivity() {

    // UI components
    private lateinit var previewView: PreviewView
    private lateinit var exposureSlider: Slider
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

    // Constants
    private val PICK_IMAGE_REQUEST_CODE = 42
    private val astroPath by lazy { filesDir.absolutePath + "/astro" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize UI components
        initializeUIComponents()

        // Initialize managers
        initializeManagers()

        // Initialize directories and extract assets
        initializeFileSystem()

        // Clean up old .corr files when MainActivity is created
        FileManager.cleanupCorrFiles(astroPath)

        // Initialize location and sensor handlers
        initializeLocationAndSensors()

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
        astrometryTimeSlider = findViewById(R.id.astrometryTimeSlider)
        astrometryTimeLabel = findViewById(R.id.astrometryTimeLabel)
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)
        captureButton = findViewById(R.id.captureButton)
        chooseButton = findViewById(R.id.chooseButton)

        // Initialize astrometryTimeSeconds from slider's initial value
        astrometryTimeSeconds = astrometryTimeSlider.value.toInt()
        astrometryTimeLabel.text = "Astrometry time: ${astrometryTimeSeconds}s"

        cameraExecutor = Executors.newSingleThreadExecutor()
        progressBar.max = 6
        progressBar.progress = 0
    }

    private fun initializeManagers() {
        permissionManager = PermissionManager(this)
        imageSelectionManager = ImageSelectionManager(this)
        intentManager = IntentManager()
        uiManager = UIManager(this)
    }

    private fun initializeFileSystem() {
        FileManager.createDirectories(astroPath)
        FileManager.extractZipAssets(this, astroPath)
        FileManager.generateAstrometryConfig(astroPath)
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
        exposureSlider.addOnChangeListener { _, value, _ ->
            exposureTimeNs = value.toLong()
            restartCameraWithExposure()
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
        // Clean up old .corr files before taking a new photo
        FileManager.cleanupCorrFiles(astroPath)

        val photoFile = File(astroPath, "input.jpg")
        CameraManager.takePhoto(
            context = this,
            imageCapture = imageCapture,
            outputFile = photoFile,
            statusText = statusText,
            currentLocation = currentLocation,
            currentAngles = currentAngles
        ) { file ->
            // Navigate to CropActivity instead of running solver directly
            NavigationManager().navigateToCropActivity(
                activity = this,
                imagePath = file.absolutePath,
                currentLocation = currentLocation,
                currentAngles = currentAngles
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            // Clean up old .corr files before processing a selected image
            FileManager.cleanupCorrFiles(astroPath)

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
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
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
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
