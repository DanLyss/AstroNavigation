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
         * @param currentLocation The current location string
         * @param currentAngles The current angles string
         */
        fun runSolver(
            context: Context,
            imageFile: File,
            astroPath: String,
            statusText: TextView,
            currentLocation: String,
            currentAngles: String,
            cpuTimeLimit: Int = 100
        ) {
            try {
                val bitmap = android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, Uri.fromFile(imageFile))
                val fitsFile = File(astroPath, "input.fits")
                FitsManager.convertBitmapToFits(bitmap, fitsFile)

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

                // Wait for 30 seconds max to ensure responsive UI and prevent excessive battery drain
                val finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS)
                if (!finished) {
                    process.destroy()
                    statusText.text = "⚠️ Solver interrupted due to timeout"
                    navigateToResultActivity(context, imageFile, currentLocation)
                    return
                }

                val exitCode = process.exitValue()
                if (exitCode != 0) {
                    statusText.text = "⚠️ Solver could not solve this image"
                    navigateToResultActivity(context, imageFile, currentLocation)
                    return
                }

                val outputDir = File("$astroPath/output")

                if (outputDir.exists() && outputDir.isDirectory) {
                    val filesToSend = outputDir.listFiles()?.filter { it.isFile } ?: emptyList()
                    val corrFile = filesToSend.find { it.name.contains("corr") }
                    if (corrFile != null) {
                        navigateToResultActivity(context, imageFile, currentLocation)
                    }
                    if (filesToSend.isNotEmpty()) {
                        for (file in filesToSend) {
                            val anglesToSend = if (currentAngles == "unknown") "Photo has no EXIF" else currentAngles
                            FileManager.saveFileToDownloads(context, file)
                        }
                        statusText.text = "✅ All files from output sent"
                    } else {
                        statusText.text = "⚠️ Output folder is empty"
                        navigateToResultActivity(context, imageFile, currentLocation)
                    }
                } else {
                    statusText.text = "⚠️ Output folder not found"
                    navigateToResultActivity(context, imageFile, currentLocation)
                }

            } catch (e: Exception) {
                statusText.text = "❌ Solver error: ${e.message}"
                navigateToResultActivity(context, imageFile, currentLocation)
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
