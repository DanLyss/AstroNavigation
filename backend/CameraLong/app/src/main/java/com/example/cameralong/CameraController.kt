package com.example.cameralong

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.camera.camera2.interop.Camera2Interop
import android.hardware.camera2.CaptureRequest
import java.io.File

class CameraController(
    private val context: Context,
    private val previewView: PreviewView,
    private val outputDirectory: File,
    private val onPhotoCaptured: (File) -> Unit // 👈 нужен для MainActivity
) {
    private lateinit var imageCapture: ImageCapture

    fun startCameraWithManualControls(exposureTimeNs: Long) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

            val builder = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)

            // Настраиваем ручное управление
            val ext = Camera2Interop.Extender(builder)
            ext.setCaptureRequestOption(
                CaptureRequest.SENSOR_EXPOSURE_TIME,
                exposureTimeNs
            )
            ext.setCaptureRequestOption(
                CaptureRequest.SENSOR_SENSITIVITY,
                800 // ISO
            )

            imageCapture = builder.build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    context as androidx.lifecycle.LifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
                takePhoto()
            } catch (e: Exception) {
                Log.e("CameraController", "Failed to bind camera use cases", e)
            }

        }, ContextCompat.getMainExecutor(context))
    }

    fun takePhoto() {
        val photoFile = File(
            outputDirectory,
            "photo_${System.currentTimeMillis()}.jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraController", "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    onPhotoCaptured(photoFile) // 📸 возвращаем файл наружу
                }
            }
        )
    }
}
