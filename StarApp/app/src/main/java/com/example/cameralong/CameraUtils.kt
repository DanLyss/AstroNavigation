package com.example.cameralong

import android.content.Context
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
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
import android.util.Range
import android.util.Log
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
                // Отключаем авто-экспозицию
                setCaptureRequestOption(
                    CaptureRequest.CONTROL_MODE,
                    CameraMetadata.CONTROL_MODE_OFF
                )
                setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CameraMetadata.CONTROL_AE_MODE_OFF
                )
                // Выдержка и ISO
                setCaptureRequestOption(
                    CaptureRequest.SENSOR_EXPOSURE_TIME,
                    exposureTimeNs
                )
                setCaptureRequestOption(
                    CaptureRequest.SENSOR_SENSITIVITY,
                    sensitivityIso
                )
                // Ограничиваем FPS-диапазон
                setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    Range(fpsMin, fpsMax)
                )
            }
            val imageCapture = builder.build()

            try {
                // 3) Перепривязываем камеру
                cameraProvider.unbindAll()
                // bind возвращает Camera, из которого и возьмём cameraInfo
                val camera = cameraProvider.bindToLifecycle(
                    activity,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )

                // 4) Проверяем, что датчик поддерживает нужную выдержку
                val camera2Info = Camera2CameraInfo.from(camera.cameraInfo)
                val exposureRange = camera2Info.getCameraCharacteristic(
                    CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE
                )
                Log.i("CameraUtils", "Supported exposure range: $exposureRange")
                // Здесь можно проверить, что exposureTimeNs ∈ exposureRange

                // 5) Всё готово — возвращаем ImageCapture
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

        if (currentLocation == "unknown") {
            Toast.makeText(context, "⏳ Waiting for location...", Toast.LENGTH_SHORT).show()
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
