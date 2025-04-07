package com.example.cameralong

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.slider.Slider
import androidx.camera.camera2.interop.Camera2Interop
import android.hardware.camera2.CaptureRequest
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var exposureSlider: Slider
    private lateinit var cameraExecutor: ExecutorService
    private var imageCapture: ImageCapture? = null

    private lateinit var locationHandler: LocationHandler
    private lateinit var sensorHandler: SensorHandler
    private var currentLocation: String = "unknown"
    private var currentAngles: String = "unknown"
    private var exposureTimeNs: Long = 1_000_000_000L // default 1s

    private val CAMERA_PERMISSION_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        exposureSlider = findViewById(R.id.exposureSlider)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // 📍 Init location
        locationHandler = LocationHandler(this)
        locationHandler.startListening { location ->
            currentLocation = "${location.latitude}, ${location.longitude}"
            Toast.makeText(this, "📍 Location updated: $currentLocation", Toast.LENGTH_SHORT).show()
        }

        // 🎯 Init angles
        sensorHandler = SensorHandler(this)
        sensorHandler.startListening()
        sensorHandler.setOnChangeListener { angles ->
            currentAngles = angles
        }

        // 🎚 Слайдер выдержки
        exposureSlider.addOnChangeListener { _, value, _ ->
            exposureTimeNs = value.toLong()
            restartCameraWithExposure()
        }

        // 🔐 Permissions
        val neededPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val permissionsToRequest = neededPermissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), CAMERA_PERMISSION_CODE)
        } else {
            restartCameraWithExposure()
        }

        // 📸 Capture
        findViewById<Button>(R.id.captureButton).setOnClickListener {
            if (currentLocation == "unknown") {
                Toast.makeText(this, "⚠️ No GPS fix, using fallback location", Toast.LENGTH_SHORT).show()
            }
            currentAngles = sensorHandler.getLatestAngles()
            takePhoto()
        }
    }

    private fun restartCameraWithExposure() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val builder = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)

            // 🧠 Устанавливаем выдержку вручную через Interop
            Camera2Interop.Extender(builder)
                .setCaptureRequestOption(
                    CaptureRequest.SENSOR_EXPOSURE_TIME,
                    exposureTimeNs
                )

            imageCapture = builder.build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch (e: Exception) {
                Toast.makeText(this, "❌ Failed to bind camera", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            externalMediaDirs.first(),
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Toast.makeText(applicationContext, "✅ Photo saved: ${photoFile.name}", Toast.LENGTH_SHORT).show()
                    TelegramSender.sendPhoto(photoFile, currentLocation, currentAngles)
                }

                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(applicationContext, "❌ Capture failed: ${exc.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            restartCameraWithExposure()
        } else {
            Toast.makeText(this, "❌ All permissions are required", Toast.LENGTH_LONG).show()
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
