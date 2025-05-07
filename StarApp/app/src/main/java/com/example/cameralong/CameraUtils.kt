package com.example.cameralong

import android.content.Context
import android.hardware.camera2.CaptureRequest
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.io.File

/**
 * Utility class for camera-related operations
 */
object CameraUtils {

    /**
     * Restarts the camera with the specified exposure time
     * @param activity The activity context
     * @param previewView The preview view to display the camera feed
     * @param exposureTimeNs The exposure time in nanoseconds
     * @param onImageCaptureReady Callback function to be called when the image capture is ready
     */
    @ExperimentalCamera2Interop
    fun restartCameraWithExposure(
        activity: AppCompatActivity,
        previewView: PreviewView,
        exposureTimeNs: Long,
        onImageCaptureReady: (ImageCapture) -> Unit
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val builder = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            Camera2Interop.Extender(builder).setCaptureRequestOption(
                CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTimeNs
            )
            val imageCapture = builder.build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(activity, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
                onImageCaptureReady(imageCapture)
            } catch (e: Exception) {
                Toast.makeText(activity, "❌ Camera binding error", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(activity))
    }

    /**
     * Takes a photo using the image capture use case
     * @param context The application context
     * @param imageCapture The image capture use case
     * @param outputFile The output file to save the photo to
     * @param statusText The text view to update with status messages
     * @param currentLocation The current location string
     * @param onPhotoSaved Callback function to be called when the photo is saved
     */
    fun takePhoto(
        context: Context,
        imageCapture: ImageCapture?,
        outputFile: File,
        statusText: TextView,
        currentLocation: String,
        onPhotoSaved: (File) -> Unit
    ) {
        val capture = imageCapture ?: return
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

        TelegramSender.sendText("📤 Starting photo capture and solver...")

        if (currentLocation == "unknown") {
            Toast.makeText(context, "⏳ Waiting for location...", Toast.LENGTH_SHORT).show()
            TelegramSender.sendText("⏳ Location not yet received, wait a few seconds and try again")
            return
        }

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    statusText.text = "📸 Photo saved. Starting solver..."
                    onPhotoSaved(outputFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(context, "❌ Capture error: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}
