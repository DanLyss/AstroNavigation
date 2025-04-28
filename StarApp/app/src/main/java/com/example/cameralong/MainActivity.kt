package com.example.cameralong


import android.os.Environment
import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
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
import android.os.Build
import androidx.annotation.RequiresApi
import java.io.*
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.zip.ZipInputStream
import nom.tam.fits.Fits
import nom.tam.fits.Header
import nom.tam.fits.ImageData
import nom.tam.fits.ImageHDU
import nom.tam.util.BufferedDataOutputStream
import kotlin.concurrent.thread

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
        TelegramSender.init(applicationContext)
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
            if (currentLocation == "unknown") {
                Toast.makeText(this, "⏳ Ожидание локации...", Toast.LENGTH_SHORT).show()
                TelegramSender.sendText("⏳ Локация ещё не получена, подожди пару секунд и попробуй снова")
                return@setOnClickListener
            }
            takePhoto()
        }

        findViewById<Button>(R.id.chooseButton).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST_CODE)
        }
    }

    private fun generateAstrometryConfig() {
        try {
            val cfgFile = File("$astroPath/bin/my.cfg")
            val indexDir = "$astroPath"
            cfgFile.parentFile?.mkdirs()
            //можно потом еще докинуть если нужно будет
            val lines = listOf(
                "index $indexDir/index-4117.fits",
                "index $indexDir/index-4118.fits",
                "index $indexDir/index-4119.fits"
            )
            cfgFile.writeText(lines.joinToString("\n"))
            TelegramSender.sendText("✅ Конфиг my.cfg создан:\n${lines.joinToString("\n")}")
        } catch (e: Exception) {
            TelegramSender.sendText("❌ Ошибка создания my.cfg: ${e.message}")
        }
    }

    private fun extractZipAssets() {
        val zipFiles = listOf("astrometry.zip", "index_files.zip")
        for (zipName in zipFiles) {
            try {
                assets.open(zipName).use { zipStream ->
                    ZipInputStream(zipStream).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            val cleanName = entry.name.removePrefix("astro/")
                            val outFile = File(astroPath, cleanName)

                            if (entry.isDirectory) {
                                outFile.mkdirs()
                            } else {
                                outFile.parentFile?.mkdirs()
                                FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
                                outFile.setExecutable(true)
                            }
                            entry = zis.nextEntry
                        }
                    }
                }
            } catch (e: Exception) {
                TelegramSender.sendText("❌ Ошибка распаковки $zipName: ${e.message}")
            }
        }
    }


    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(astroPath, "input.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        TelegramSender.sendText("📤 Запуск захвата фото и solver...")
        if (currentLocation == "unknown") {
            Toast.makeText(this, "⏳ Ожидание локации...", Toast.LENGTH_SHORT).show()
            TelegramSender.sendText("⏳ Локация ещё не получена, подожди пару секунд и попробуй снова")
            return
        }
        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                statusText.text = "📸 Фото сохранено. Запуск solver..."
                runSolver(photoFile)
            }

            override fun onError(exc: ImageCaptureException) {
                Toast.makeText(applicationContext, "❌ Ошибка съёмки: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }


private fun runSolver(imageFile: File) {
    try {
        val bitmap = android.provider.MediaStore.Images.Media.getBitmap(contentResolver, Uri.fromFile(imageFile))
        val fitsFile = File(astroPath, "input.fits")
        convertBitmapToFits(bitmap, fitsFile)

        val cmd = listOf(
            "sh", "-c",
            "LD_LIBRARY_PATH=$astroPath/lib " +
                    "$astroPath/bin/solve-field " +
                    "--fits-image ${fitsFile.absolutePath} " +
                    "--dir $astroPath/output " +
                    "--temp-dir $astroPath/tmp " +
                    "-O -L 0.1 -H 180.0 -u dw -z 2 -p -y -9 " +
                    "--uniformize 0 --cpulimit 300 " +
                    "--config $astroPath/bin/my.cfg -v"
        )

        val pb = ProcessBuilder(*cmd.toTypedArray())
        pb.directory(File("$astroPath/output"))
        pb.environment().putAll(mapOf("LD_LIBRARY_PATH" to "$astroPath/lib"))
        val process = pb.start()

        val stdout = File(astroPath, "solve-out.log")
        val stderr = File(astroPath, "solve-err.log")

        thread {
            stdout.printWriter().use { out ->
                process.inputStream.bufferedReader().forEachLine { out.println(it) }
            }
        }
        thread {
            stderr.printWriter().use { err ->
                process.errorStream.bufferedReader().forEachLine { err.println(it) }
            }
        }

        val finished = process.waitFor(2, java.util.concurrent.TimeUnit.MINUTES)
        if (!finished) {
            process.destroy()
            TelegramSender.sendText("⏱ solve-field превысил лимит в 2 мин, процесс убит.")
            statusText.text = "⚠️ Solver прерван по таймауту"
            return
        }

        val exitCode = process.exitValue()
        if (exitCode != 0) {
            TelegramSender.sendPhoto(stderr, "STDERR log", "❗️ solve-field завершился с ошибкой ($exitCode)")
            return
        }


        val corrFile = File("$astroPath/output/input.corr")

        if (corrFile.exists()) {
                val anglesToSend = if (currentAngles == "unknown") "Photo has no EXIF" else currentAngles
                saveFileToDownloads(corrFile)
                TelegramSender.sendPhoto(corrFile, currentLocation, anglesToSend)
                statusText.text = "✅ Solver завершён"
        } else {
            statusText.text = "⚠️ .corr не найден"
        }

        TelegramSender.sendPhoto(stdout, "STDOUT log", "📄 solve-field stdout")
        TelegramSender.sendPhoto(stderr, "STDERR log", "📄 solve-field stderr")

    } catch (e: Exception) {
        statusText.text = "❌ Solver error: ${e.message}"
        TelegramSender.sendText("❌ Исключение при solver: ${e.message}")
    }
}



    private fun convertBitmapToFits(bitmap: Bitmap, output: File) {
        val width = bitmap.width
        val height = bitmap.height

        val pixels2D: Array<ByteArray> = Array(height) { y ->
            ByteArray(width) { x ->
                val pixel = bitmap.getPixel(x, y)
                val gray = (Color.red(pixel) * 0.3 + Color.green(pixel) * 0.59 + Color.blue(pixel) * 0.11).toInt()
                gray.toByte()
            }
        }

        val header = Header()
        header.setSimple(true)
        header.setBitpix(8) // 8-bit unsigned
        header.setNaxes(2)
        header.setNaxis(1, width)
        header.setNaxis(2, height)
        header.addValue("BSCALE", 1.0, null)
        header.addValue("BZERO", 0.0, null)
        header.addValue("DATAMAX", 255.0, null)
        header.addValue("DATAMIN", 0.0, null)

        val data = ImageData(pixels2D as Any)
        val hdu = ImageHDU(header, data)
        val fits = Fits()
        fits.addHDU(hdu)
        BufferedDataOutputStream(FileOutputStream(output)).use {
            fits.write(it)
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
                if (currentLocation == "unknown") {
                    Toast.makeText(this, "⏳ Ожидание локации...", Toast.LENGTH_SHORT).show()
                    TelegramSender.sendText("⏳ Локация ещё не получена, подожди пару секунд и попробуй снова")
                    return
                }
                val exif = androidx.exifinterface.media.ExifInterface(destFile.absolutePath)
                val userComment = exif.getAttribute(androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT)

                if (userComment != null && userComment.contains("yaw") && userComment.contains("pitch")) {
                    currentAngles = userComment
                    TelegramSender.sendText("📌 Углы загружены из EXIF: $currentAngles")
                }
                else{
                    currentAngles = "unknown"
                }

                statusText.text = "📂 Фото загружено, запускаем solver..."
                runSolver(destFile)
            } catch (e: Exception) {
                Toast.makeText(this, "❌ Ошибка загрузки файла: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            restartCameraWithExposure()
        } else {
            Toast.makeText(this, "❌ Требуются все разрешения", Toast.LENGTH_LONG).show()
        }
    }

    private fun restartCameraWithExposure() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val builder = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            Camera2Interop.Extender(builder).setCaptureRequestOption(
                CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTimeNs
            )
            imageCapture = builder.build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch (e: Exception) {
                Toast.makeText(this, "❌ Ошибка привязки камеры", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }
    private fun saveFileToDownloads(sourceFile: File) {
        try {
            val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsFolder.exists()) {
                downloadsFolder.mkdirs()
            }
            val destFile = File(downloadsFolder, sourceFile.name)
            sourceFile.copyTo(destFile, overwrite = true)

            Toast.makeText(this, "✅ Файл сохранен: ${destFile.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Ошибка сохранения: ${e.message}", Toast.LENGTH_LONG).show()
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
