package com.starapp.navigation.astro

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.starapp.navigation.file.FileManager
import com.starapp.navigation.ui.ResultActivity
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Manager class for astrometry-related operations
 */
class AstrometryManager {
    companion object {
        private const val TAG = "AstrometryManager"

        // Track current solver process and handler for cleanup
        private var currentProcess: Process? = null
        private var currentHandler: Handler? = null
        private var currentProgressUpdater: Runnable? = null

        // Shared thread pool for all astrometry operations
        private val executor: ExecutorService = Executors.newFixedThreadPool(3)

        // Ensure executor is shutdown when app is closed
        init {
            Runtime.getRuntime().addShutdownHook(Thread {
                shutdownExecutor()
            })
        }

        private fun shutdownExecutor() {
            executor.shutdown()
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow()
                }
            } catch (e: InterruptedException) {
                executor.shutdownNow()
            }
        }

        /**
         * Runs the astrometry solver on an image file.
         * Heavy work is moved to a background thread to keep the UI responsive.
         * 
         * @param context The application context
         * @param imageFile The image file to process
         * @param astroPath The path to the astrometry files
         * @param currentLocation The current location string
         * @param currentAngles The current angles string
         * @param cpuTimeLimit The CPU time limit for the solver in seconds
         * @param onStatusUpdate Callback for updating status text
         * @param onProgressUpdate Callback for updating progress bar
         * @param onProgressVisibilityChanged Callback for changing progress bar visibility
         */
        fun runSolver(
            context: Context,
            imageFile: File,
            astroPath: String,
            currentLocation: String,
            currentAngles: String,
            cpuTimeLimit: Int = 100,
            onStatusUpdate: (String) -> Unit,
            onProgressUpdate: (Int, Int) -> Unit, // (progress, max)
            onProgressVisibilityChanged: (Boolean) -> Unit
        ) {
            // Prepare progress bar
            val maxTimeSeconds = (cpuTimeLimit * 1.2).toInt()
            onProgressUpdate(0, 100)
            onProgressVisibilityChanged(true)

            // Start timer to update progress bar by elapsed time
            val startTime = System.currentTimeMillis()
            val handler = Handler(Looper.getMainLooper())
            val progressUpdater = object : Runnable {
                override fun run() {
                    val elapsedSec = (System.currentTimeMillis() - startTime) / 1000
                    val percent = ((elapsedSec.toFloat() / maxTimeSeconds) * 100)
                        .toInt()
                        .coerceIn(0, 100)
                    onProgressUpdate(percent, 100)
                    if (percent < 100) {
                        handler.postDelayed(this, 100)
                    }
                }
            }

            // Store for cancellation
            currentHandler = handler
            currentProgressUpdater = progressUpdater

            handler.post(progressUpdater)

            // Perform all heavy operations in a background thread
            executor.execute {
                try {
                    // Step 1: convert image to FITS
                    handler.post {
                        onStatusUpdate("Converting image to FITS format...")
                    }
                    val bitmap = android.provider.MediaStore.Images.Media
                        .getBitmap(context.contentResolver, Uri.fromFile(imageFile))
                    val fitsFile = File(astroPath, "input.fits")
                    FitsManager.convertBitmapToFits(bitmap, fitsFile)

                    // Step 2: start the astrometry solver process
                    handler.post {
                        onStatusUpdate("Starting astrometry solver...")
                    }
                    val cmd = listOf(
                        "sh", "-c",
                        "LD_LIBRARY_PATH=$astroPath/lib " +
                                "$astroPath/bin/solve-field " +
                                "--fits-image ${fitsFile.absolutePath} " +
                                "--dir $astroPath/output " +
                                "--temp-dir $astroPath/tmp " +
                                "-O -L 0.1 -H 180.0 -u dw -z 2 -p -y -9 " +
                                "--uniformize 0 --cpulimit $cpuTimeLimit " +
                                "--config $astroPath/bin/my.cfg -v"
                    )
                    val pb = ProcessBuilder(*cmd.toTypedArray())
                        .directory(File("$astroPath/output"))
                        .apply { environment()["LD_LIBRARY_PATH"] = "$astroPath/lib" }
                    val process = pb.start()
                    currentProcess = process

                    // Log stdout and update status based on output
                    executor.execute {
                        val outLog = File(astroPath, "solve-out.log")
                        outLog.printWriter().use { writer ->
                            process.inputStream.bufferedReader().forEachLine { line ->
                                writer.println(line)
                                handler.post {
                                    when {
                                        line.contains("Reading input file") ->
                                            onStatusUpdate("Reading input file...")
                                        line.contains("Extracting sources") ->
                                            onStatusUpdate("Extracting stars from image...")
                                        line.contains("Solving...") ->
                                            onStatusUpdate("Solving star pattern...")
                                        line.contains("Solving field") ->
                                            onStatusUpdate("Finalizing solution...")
                                    }
                                }
                            }
                        }
                    }

                    // Log stderr
                    executor.execute {
                        val errLog = File(astroPath, "solve-err.log")
                        errLog.printWriter().use { writer ->
                            process.errorStream.bufferedReader().forEachLine { writer.println(it) }
                        }
                    }

                    // Wait for completion with timeout
                    val finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)
                    handler.post {
                        if (!finished) {
                            process.destroy()
                            onStatusUpdate("⚠️ Solver timed out")
                        } else if (process.exitValue() != 0) {
                            onStatusUpdate("⚠️ Solver failed with code ${process.exitValue()}")
                        } else {
                            onStatusUpdate("✅ Solution found!")
                        }
                    }

                    // Delay briefly to show final status
                    handler.postDelayed({
                        // Navigate to result screen
                        navigateToResultActivity(context, imageFile, currentLocation)
                    }, 1_000)

                } catch (e: Exception) {
                    handler.post {
                        onStatusUpdate("❌ Solver error: ${e.message}")
                    }
                    handler.postDelayed({
                        navigateToResultActivity(context, imageFile, currentLocation)
                    }, 1_000)
                }
            }

            // Add a timeout to prevent hanging if the executor gets stuck
            executor.execute {
                try {
                    Thread.sleep(35_000) // 35 seconds timeout (slightly longer than process timeout)
                    if (currentProcess?.isAlive == true) {
                        currentProcess?.destroy()
                        handler.post {
                            onStatusUpdate("⚠️ Processing timed out")
                        }
                        handler.postDelayed({
                            navigateToResultActivity(context, imageFile, currentLocation)
                        }, 1_000)
                    }
                } catch (e: InterruptedException) {
                    // Timeout thread was interrupted, which is fine
                }
            }
        }

        /**
         * Navigate to the ResultActivity with the solved image.
         */
        private fun navigateToResultActivity(
            context: Context,
            imageFile: File,
            currentLocation: String
        ) {
            val intent = Intent(context, ResultActivity::class.java).apply {
                putExtra("imagePath", imageFile.absolutePath)
                putExtra("currentLocation", currentLocation)
            }
            context.startActivity(intent)
        }

        /**
         * Cancel any running solver process and cleanup.
         * 
         * @param onProgressReset Callback for resetting progress bar
         * @param onProgressVisibilityChanged Callback for changing progress bar visibility
         * @param onStatusReset Callback for resetting status text
         * @param onStatusVisibilityChanged Callback for changing status text visibility
         */
        fun cancelSolver(
            onProgressReset: (() -> Unit)? = null,
            onProgressVisibilityChanged: ((Boolean) -> Unit)? = null,
            onStatusReset: (() -> Unit)? = null,
            onStatusVisibilityChanged: ((Boolean) -> Unit)? = null
        ) {
            // Remove pending UI updates
            currentProgressUpdater?.let { currentHandler?.removeCallbacks(it) }

            // Destroy running process
            currentProcess?.takeIf { it.isAlive }?.destroy()

            // Reset UI
            onProgressReset?.invoke()
            onProgressVisibilityChanged?.invoke(false)
            onStatusReset?.invoke()
            onStatusVisibilityChanged?.invoke(false)

            // Clear references
            currentProcess = null
            currentHandler = null
            currentProgressUpdater = null

            // Note: We don't shut down the executor here as it's shared across multiple operations
            // The executor will be properly shut down when the app is closed via the shutdown hook
        }
    }
}
