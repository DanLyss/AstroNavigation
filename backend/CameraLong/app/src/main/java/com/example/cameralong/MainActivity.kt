package com.example.cameralong

import android.Manifest
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import com.google.android.material.slider.Slider
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var captureButton: Button
    private lateinit var exposureSlider: Slider

    private lateinit var locationHandler: LocationHandler
    private lateinit var sensorHandler: SensorHandler

    private lateinit var outputDirectory: File
    private var currentLocation: Location? = null

    private lateinit var cameraController: CameraController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.previewView)
        captureButton = findViewById(R.id.captureButton)
        exposureSlider = findViewById(R.id.exposureSlider)

        // Запрашиваем разрешения
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.INTERNET
            ),
            0
        )

        locationHandler = LocationHandler(this)
        sensorHandler = SensorHandler(this)
        outputDirectory = getOutputDirectory()

        sensorHandler.register()
        locationHandler.getLocation {
            currentLocation = it
        }

        cameraController = CameraController(
            context = this,
            previewView = previewView,
            outputDirectory = outputDirectory,
            onPhotoCaptured = { photoFile ->
                val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

                val metaFile = File(outputDirectory, "meta_${System.currentTimeMillis()}.json")
                metaFile.writeText(
                    """
                    {
                      "timestamp": "$time",
                      "location": {
                        "latitude": ${currentLocation?.latitude},
                        "longitude": ${currentLocation?.longitude}
                      },
                      "angles": {
                        "azimuth": ${sensorHandler.azimuth},
                        "pitch": ${sensorHandler.pitch},
                        "roll": ${sensorHandler.roll}
                      }
                    }
                    """.trimIndent()
                )

                TelegramSender.sendFiles(photoFile, metaFile)
                Toast.makeText(this, "Фотка и мета отправлены!", Toast.LENGTH_SHORT).show()
            }
        )

        captureButton.setOnClickListener {
            val exposureNs = exposureSlider.value.toLong()
            cameraController.startCameraWithManualControls(exposureNs)
        }
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, "CameraLong").apply { mkdirs() }
        }
        return mediaDir ?: filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        sensorHandler.unregister()
    }
}
