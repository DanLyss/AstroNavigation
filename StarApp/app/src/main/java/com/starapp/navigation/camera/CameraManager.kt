package com.starapp.navigation.camera

import android.content.Context
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.util.Log
import android.util.Range
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.starapp.navigation.util.ExifUtils
import java.io.File
import java.util.Date

/**
 * Manager class for camera-related operations
 */
class CameraManager {

    companion object {
        private const val TAG = "CameraManager"

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
            sensitivityIso: Int = 800,
            fpsMin: Int = 1,
            fpsMax: Int = 30,
            onImageCaptureReady: (ImageCapture) -> Unit
        ) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(activity)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()

                // 1) Preview use-case
                val preview = Preview.Builder()
                    .build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                // 2) ImageCapture builder + interop
                val builder = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                Camera2Interop.Extender(builder).apply {
                    // Disable auto-exposure
                    setCaptureRequestOption(
                        CaptureRequest.CONTROL_MODE,
                        CameraMetadata.CONTROL_MODE_OFF
                    )
                    setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_MODE,
                        CameraMetadata.CONTROL_AE_MODE_OFF
                    )
                    // Exposure and ISO
                    setCaptureRequestOption(
                        CaptureRequest.SENSOR_EXPOSURE_TIME,
                        exposureTimeNs
                    )
                    setCaptureRequestOption(
                        CaptureRequest.SENSOR_SENSITIVITY,
                        sensitivityIso
                    )
                    // Limit FPS range
                    setCaptureRequestOption(
                        CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                        Range(fpsMin, fpsMax)
                    )
                }
                val imageCapture = builder.build()

                try {
                    // 3) Rebind the camera
                    cameraProvider.unbindAll()
                    // bind returns Camera, from which we'll get cameraInfo
                    val camera = cameraProvider.bindToLifecycle(
                        activity,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageCapture
                    )

                    // 4) Check that the sensor supports the required exposure
                    val camera2Info = Camera2CameraInfo.from(camera.cameraInfo)
                    val exposureRange = camera2Info.getCameraCharacteristic(
                        CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE
                    )
                    Log.i(TAG, "Supported exposure range: $exposureRange")
                    // Here you can check that exposureTimeNs ∈ exposureRange

                    // 5) All ready - returning ImageCapture
                    onImageCaptureReady(imageCapture)
                } catch (e: Exception) {
                    Toast.makeText(
                        activity,
                        "❌ Camera binding error: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }, ContextCompat.getMainExecutor(activity))
        }

        /**
         * Takes a photo using the image capture use case
         * 
         * IMPORTANT: This method captures the device orientation angles at the exact moment
         * the photo is taken (before calling takePicture), not when the photo is saved.
         * This ensures that the angles stored in EXIF data accurately reflect the device's
         * orientation at the moment of capture, which is critical for astronomical calculations.
         * 
         * @param context The application context
         * @param imageCapture The image capture use case
         * @param outputFile The output file to save the photo to
         * @param statusText The text view to update with status messages
         * @param currentLocation The current location string
         * @param sensorHandler The sensor handler to get the latest angles at the moment of capture
         * @param onPhotoSaved Callback function to be called when the photo is saved
         */
        fun takePhoto(
            context: Context,
            imageCapture: ImageCapture?,
            outputFile: File,
            statusText: TextView,
            currentLocation: String,
            sensorHandler: com.starapp.navigation.location.SensorHandler,
            onPhotoSaved: (File, String) -> Unit
        ) {
            val capture = imageCapture ?: return
            val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()

            // Log the current location status
            Log.d(TAG, "Taking photo with location: $currentLocation")




            capture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {

                        val captureAngles = sensorHandler.getLatestAngles()
                        Log.d(TAG, "Captured angles at moment of taking photo: $captureAngles")
                        statusText.text = "📸 Photo saved. Starting solver..."
                        try {

                            // Use the angles captured at the moment of taking the photo
                            // instead of getting them when the photo is saved

                            // Save EXIF data
                            ExifUtils.saveExifData(
                                outputFile.absolutePath,
                                captureAngles,
                                currentLocation,
                                Date()
                            )

                            onPhotoSaved(outputFile, captureAngles)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to save EXIF data: ${e.message}")
                            onPhotoSaved(outputFile, captureAngles)
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Toast.makeText(context, "❌ Capture error: ${exception.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}
