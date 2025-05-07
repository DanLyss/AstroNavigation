package com.example.cameralong

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.slider.Slider
import java.io.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@androidx.camera.camera2.interop.ExperimentalCamera2Interop
class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var exposureSlider: Slider
    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    private lateinit var locationHandler: LocationHandler
    private lateinit var sensorHandler: SensorHandler
    private var currentLocation: String = "unknown"
    private var currentAngles: String = "unknown"
    private var exposureTimeNs: Long = 1_000_000_000L

    private val PICK_IMAGE_REQUEST_CODE = 42
    private val CAMERA_PERMISSION_CODE = 100
    private val astroPath by lazy { filesDir.absolutePath + "/astro" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        exposureSlider = findViewById(R.id.exposureSlider)
        statusText = findViewById(R.id.statusText)
        progressBar = findViewById(R.id.progressBar)
        cameraExecutor = Executors.newSingleThreadExecutor()
        progressBar.max = 6
        progressBar.progress = 0

        File("$astroPath/output").mkdirs()
        File("$astroPath/tmp").mkdirs()

        extractZipAssets()
        generateAstrometryConfig()

        locationHandler = LocationHandler(this)
        locationHandler.startListening { location ->
            currentLocation = "${location.latitude}, ${location.longitude}"
        }

        sensorHandler = SensorHandler(this)
        sensorHandler.startListening()
        sensorHandler.setOnChangeListener { angles -> currentAngles = angles }

        exposureSlider.addOnChangeListener { _, value, _ ->
            exposureTimeNs = value.toLong()
            restartCameraWithExposure()
        }

        val neededPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        val permissionsToRequest = neededPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), CAMERA_PERMISSION_CODE)
        } else {
            restartCameraWithExposure()
        }

        findViewById<Button>(R.id.captureButton).setOnClickListener {
            currentAngles = sensorHandler.getLatestAngles()
            takePhoto()
        }

        findViewById<Button>(R.id.chooseButton).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE)
        }
    }

    private fun generateAstrometryConfig() {
        AstrometryUtils.generateAstrometryConfig(astroPath)
    }

    private fun extractZipAssets() {
        AstrometryUtils.extractZipAssets(this, astroPath)
    }


    private fun takePhoto() {
        val photoFile = File(astroPath, "input.jpg")
        CameraUtils.takePhoto(
            context = this,
            imageCapture = imageCapture,
            outputFile = photoFile,
            statusText = statusText,
            currentLocation = currentLocation
        ) { file ->
            AstrometryUtils.runSolver(
                context = this,
                imageFile = file,
                astroPath = astroPath,
                statusText = statusText,
                currentLocation = currentLocation,
                currentAngles = currentAngles
            )
        }
    }







    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val uri: Uri = data.data ?: return
            val destFile = File(astroPath, "input.jpg")
            try {
                contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val exif = androidx.exifinterface.media.ExifInterface(destFile.absolutePath)
                val userComment = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT)

                if (userComment != null && userComment.contains("yaw") && userComment.contains("pitch")) {
                    currentAngles = userComment
                   }
                else{
                    currentAngles = "unknown"
                }

                statusText.text = "📂 Photo loaded, starting solver..."
                AstrometryUtils.runSolver(
                    context = this,
                    imageFile = destFile,
                    astroPath = astroPath,
                    statusText = statusText,
                    currentLocation = currentLocation,
                    currentAngles = currentAngles
                )
            } catch (e: Exception) {
                Toast.makeText(this, "❌ File loading error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            restartCameraWithExposure()
        } else {
            Toast.makeText(this, "❌ All permissions are required", Toast.LENGTH_LONG).show()
        }
    }

    @androidx.camera.camera2.interop.ExperimentalCamera2Interop
    private fun restartCameraWithExposure() {
        CameraUtils.restartCameraWithExposure(
            activity = this,
            previewView = previewView,
            exposureTimeNs = exposureTimeNs
        ) { capture ->
            imageCapture = capture
        }
    }
    // This method has been moved to FileUtils class


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
