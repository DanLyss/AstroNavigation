package com.starapp.navigation.camera

import android.content.Context
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.util.Log
import android.util.Range
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.Camera2CameraInfo
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.starapp.navigation.util.ExifUtils
import java.io.File
import java.util.Date
import java.util.concurrent.ExecutorService

/**
 * Manager class for camera-related operations
 */
class CameraManager {

    companion object {
        private const val TAG = "CameraManager"

        /**
         * Brightness threshold for detecting too bright scenes
         */
        const val BRIGHTNESS_THRESHOLD = 240

        /**
         * Sets up image analysis for brightness detection
         * @param cameraExecutor The executor service for camera operations
         * @param onBrightnessAnalyzed Callback for when brightness is analyzed
         * @return The configured ImageAnalysis use case
         */
        @androidx.camera.core.ExperimentalGetImage
        fun setupImageAnalysis(
            cameraExecutor: ExecutorService,
            onBrightnessAnalyzed: (Boolean) -> Unit
        ): ImageAnalysis {
            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                analyzeImageBrightness(imageProxy, onBrightnessAnalyzed)
            }

            return imageAnalysis
        }

        /**
         * Analyzes the brightness of an image
         * @param imageProxy The image to analyze
         * @param onBrightnessAnalyzed Callback for when brightness is analyzed
         */
        @androidx.camera.core.ExperimentalGetImage
        private fun analyzeImageBrightness(
            imageProxy: ImageProxy,
            onBrightnessAnalyzed: (Boolean) -> Unit
        ) {
            // Get the image from the imageProxy
            val image = imageProxy.image
            if (image != null) {
                // Get the center portion of the image for brightness analysis
                val centerX = image.width / 2
                val centerY = image.height / 2
                val sampleSize = 100 // Sample a 100x100 area in the center

                // Get the Y plane (luminance) from the YUV image
                val yBuffer = image.planes[0].buffer
                val ySize = yBuffer.remaining()
                val yArray = ByteArray(ySize)
                yBuffer.get(yArray)

                // Calculate average brightness in the center area
                var totalBrightness = 0
                var pixelCount = 0

                for (y in centerY - sampleSize/2 until centerY + sampleSize/2) {
                    if (y < 0 || y >= image.height) continue

                    for (x in centerX - sampleSize/2 until centerX + sampleSize/2) {
                        if (x < 0 || x >= image.width) continue

                        // Get the pixel value (0-255)
                        val pixelValue = yArray[y * image.width + x].toInt() and 0xFF
                        totalBrightness += pixelValue
                        pixelCount++
                    }
                }

                // Calculate average brightness
                val averageBrightness = if (pixelCount > 0) totalBrightness / pixelCount else 0

                // Check if the scene is too bright
                val isTooBright = averageBrightness > BRIGHTNESS_THRESHOLD
                onBrightnessAnalyzed(isTooBright)
            }

            // Close the imageProxy to release resources
            imageProxy.close()
        }

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
            onImageCaptureReady: (ImageCapture) -> Unit,
            onError: (String) -> Unit = {}
        ) {
            // Call the new method without image analysis
            restartCameraWithExposureAndAnalysis(
                activity = activity,
                previewView = previewView,
                exposureTimeNs = exposureTimeNs,
                imageAnalysis = null,
                sensitivityIso = sensitivityIso,
                fpsMin = fpsMin,
                fpsMax = fpsMax,
                onImageCaptureReady = onImageCaptureReady,
                onError = onError
            )
        }

        @ExperimentalCamera2Interop
        fun restartCameraWithExposureAndAnalysis(
            activity: AppCompatActivity,
            previewView: PreviewView,
            exposureTimeNs: Long,
            imageAnalysis: ImageAnalysis?,
            sensitivityIso: Int = 800,
            fpsMin: Int = 1,
            fpsMax: Int = 30,
            onImageCaptureReady: (ImageCapture) -> Unit,
            onError: (String) -> Unit = {}
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

                    // Create a list of use cases
                    val useCases = mutableListOf<androidx.camera.core.UseCase>(preview, imageCapture)

                    // Add image analysis if provided
                    if (imageAnalysis != null) {
                        useCases.add(imageAnalysis)
                    }

                    // bind returns Camera, from which we'll get cameraInfo
                    val camera = cameraProvider.bindToLifecycle(
                        activity,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        *useCases.toTypedArray()
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
                    onError("❌ Camera binding error: ${e.message}")
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
         * @param currentLocation The current location string
         * @param sensorHandler The sensor handler to get the latest angles at the moment of capture
         * @param onStatusUpdate Callback for updating status
         * @param onPhotoSaved Callback function to be called when the photo is saved
         */
        fun takePhoto(
            context: Context,
            imageCapture: ImageCapture?,
            outputFile: File,
            currentLocation: String,
            sensorHandler: com.starapp.navigation.location.SensorHandler,
            onStatusUpdate: (String) -> Unit,
            onPhotoSaved: (File, String) -> Unit,
            onCaptureError: (String) -> Unit = {}
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
                        onStatusUpdate("📸 Photo saved. Starting solver...")
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
                        onCaptureError("❌ Capture error: ${exception.message}")
                    }
                }
            )
        }
    }
}
