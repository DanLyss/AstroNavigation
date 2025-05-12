package com.starapp.navigation.astro

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.TextView
import com.starapp.navigation.file.FileManager
import com.starapp.navigation.ui.ResultActivity
import java.io.File
import kotlin.concurrent.thread

/**
 * Manager class for astrometry-related operations
 */
class AstrometryManager {
    companion object {
        private const val TAG = "AstrometryManager"

        /**
         * Runs the astrometry solver on an image file
         * @param context The application context
         * @param imageFile The image file to solve
         * @param astroPath The path to the astrometry files
         * @param statusText The text view to update with status messages
         * @param progressBar The progress bar to update during solving
         * @param currentLocation The current location string
         * @param currentAngles The current angles string
         */
        fun runSolver(
            context: Context,
            imageFile: File,
            astroPath: String,
            statusText: TextView,
            progressBar: android.widget.ProgressBar,
            currentLocation: String,
            currentAngles: String,
            cpuTimeLimit: Int = 100
        ) {
            // Set up time-based progress bar
            // Add 20% safety margin to the CPU time limit
            val maxTimeSeconds = (cpuTimeLimit * 1.2).toInt()
            progressBar.max = 100
            progressBar.progress = 0

            // Start time tracking
            val startTime = System.currentTimeMillis()

            // Handler for updating progress based on elapsed time
            val handler = android.os.Handler(android.os.Looper.getMainLooper())
            val progressUpdater = object : Runnable {
                override fun run() {
                    val elapsedTimeMs = System.currentTimeMillis() - startTime
                    val elapsedTimeSeconds = elapsedTimeMs / 1000

                    // Calculate progress as a percentage of elapsed time relative to max time
                    val progress = ((elapsedTimeSeconds.toFloat() / maxTimeSeconds) * 100).toInt().coerceIn(0, 100)
                    progressBar.progress = progress

                    // Continue updating every 100ms until we reach 100% or the process completes
                    if (progress < 100) {
                        handler.postDelayed(this, 100)
                    }
                }
            }

            // Function to update status message only (progress is handled by timer)
            fun updateStage(newStage: String, statusMessage: String) {
                statusText.text = statusMessage
            }
            try {
                // Reset progress bar
                progressBar.progress = 0
                progressBar.visibility = android.view.View.VISIBLE

                // Start progress updater
                handler.post(progressUpdater)

                // Update progress (Step 1: Converting image)
                updateStage("converting", "Converting image to FITS format...")

                val bitmap = android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, Uri.fromFile(imageFile))
                val fitsFile = File(astroPath, "input.fits")
                FitsManager.convertBitmapToFits(bitmap, fitsFile)

                // Update progress (Step 2: Starting solver)
                updateStage("starting", "Starting astrometry solver...")

                val cmd = listOf(
                    "sh", "-c",
                    "LD_LIBRARY_PATH=$astroPath/lib " +
                            "$astroPath/bin/solve-field " +
                            "--fits-image ${fitsFile.absolutePath} " +
                            "--dir $astroPath/output " +
                            "--temp-dir $astroPath/tmp " +
                            "-O -L 0.1 -H 180.0 -u dw -z 2 -p -y -9 " +
                                "--uniformize 0 --cpulimit $cpuTimeLimit " +  // CPU time limit set by slider
                            "--config $astroPath/bin/my.cfg -v"
                )

                val pb = ProcessBuilder(*cmd.toTypedArray())
                pb.directory(File("$astroPath/output"))
                pb.environment().putAll(mapOf("LD_LIBRARY_PATH" to "$astroPath/lib"))
                val process = pb.start()

                val stdout = File(astroPath, "solve-out.log")
                val stderr = File(astroPath, "solve-err.log")

                // Update progress (Step 3: Processing)
                updateStage("reading", "Processing image with astrometry solver...")

                // Monitor solver progress
                thread {
                    stdout.printWriter().use { out ->
                        process.inputStream.bufferedReader().forEachLine { line ->
                            out.println(line)

                            // Update progress based on solver output
                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                if (line.contains("Reading input file")) {
                                    updateStage("reading", "Reading input file...")
                                } else if (line.contains("Extracting sources")) {
                                    updateStage("extracting", "Extracting stars from image...")
                                } else if (line.contains("Solving...")) {
                                    updateStage("solving", "Solving star pattern...")
                                } else if (line.contains("Solving field")) {
                                    updateStage("finalizing", "Finalizing solution...")
                                }
                            }
                        }
                    }
                }

                thread {
                    stderr.printWriter().use { err ->
                        process.errorStream.bufferedReader().forEachLine { err.println(it) }
                    }
                }

                // Wait for process completion in a background thread to avoid blocking UI
                thread {
                    // Wait for 30 seconds max to ensure responsive UI and prevent excessive battery drain
                    val finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)

                    // Handle process completion on the main thread
                    handler.post {
                        if (!finished) {
                            process.destroy()
                            updateStage("reading", "⚠️ Solver interrupted due to timeout")
                            // Add a small delay to ensure the UI updates before navigating
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                navigateToResultActivity(context, imageFile, currentLocation)
                            }, 1000) // 1000ms delay to allow progress bar to update
                            return@post
                        }

                        val exitCode = process.exitValue()
                        if (exitCode != 0) {
                            updateStage("reading", "⚠️ Solver could not solve this image")
                            // Add a small delay to ensure the UI updates before navigating
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                navigateToResultActivity(context, imageFile, currentLocation)
                            }, 1000) // 1000ms delay to allow progress bar to update
                            return@post
                        }

                        // Process results after successful completion
                        val outputDir = File("$astroPath/output")

                        if (outputDir.exists() && outputDir.isDirectory) {
                            val filesToSend = outputDir.listFiles()?.filter { it.isFile } ?: emptyList()
                            val corrFile = filesToSend.find { it.name.contains("corr") }
                            if (corrFile != null) {
                                // Set progress to maximum before navigating
                                updateStage("finalizing", "✅ Solution found!")
                                // Add a small delay to ensure the UI updates before navigating
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    navigateToResultActivity(context, imageFile, currentLocation)
                                }, 1000) // 1000ms delay to allow progress bar to update
                            }
                            if (filesToSend.isNotEmpty()) {
                                for (file in filesToSend) {
                                    val anglesToSend = if (currentAngles == "unknown") "Photo has no EXIF" else currentAngles
                                    FileManager.saveFileToDownloads(context, file)
                                }
                                updateStage("finalizing", "✅ All files from output sent")
                            } else {
                                updateStage("reading", "⚠️ Output folder is empty")
                                // Add a small delay to ensure the UI updates before navigating
                                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                    navigateToResultActivity(context, imageFile, currentLocation)
                                }, 1000) // 1000ms delay to allow progress bar to update
                            }
                        } else {
                            updateStage("reading", "⚠️ Output folder not found")
                            // Add a small delay to ensure the UI updates before navigating
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                navigateToResultActivity(context, imageFile, currentLocation)
                            }, 1000) // 1000ms delay to allow progress bar to update
                        }
                    }
                }

            } catch (e: Exception) {
                updateStage("reading", "❌ Solver error: ${e.message}")
                // Add a small delay to ensure the UI updates before navigating
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    navigateToResultActivity(context, imageFile, currentLocation)
                }, 1000) // 1000ms delay to allow progress bar to update
            }
        }

        /**
         * Navigates to the ResultActivity
         * @param context The application context
         * @param imageFile The image file to display
         * @param currentLocation The current location string
         */
        private fun navigateToResultActivity(context: Context, imageFile: File, currentLocation: String) {
            val intent = Intent(context, ResultActivity::class.java)
            intent.putExtra("imagePath", imageFile.absolutePath)
            intent.putExtra("currentLocation", currentLocation)
            context.startActivity(intent)
        }
    }
}
