package com.example.cameralong

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import nom.tam.fits.Fits
import nom.tam.fits.Header
import nom.tam.fits.ImageData
import nom.tam.fits.ImageHDU
import nom.tam.util.BufferedDataOutputStream
import kotlin.concurrent.thread

/**
 * Utility class for astrometry-related operations
 */
object AstrometryUtils {

    /**
     * Extracts zip assets to the application's files directory
     * @param context The application context
     * @param astroPath The path to extract the assets to
     */
    fun extractZipAssets(context: Context, astroPath: String) {
        val zipFiles = listOf("astrometry.zip", "index_files.zip")
        for (zipName in zipFiles) {
            context.assets.open(zipName).use { zipStream ->
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
        }
    }

    /**
     * Generates the astrometry configuration file
     * @param astroPath The path to the astrometry files
     */
    fun generateAstrometryConfig(astroPath: String) {
        val cfgFile = File("$astroPath/bin/my.cfg")
        val indexDir = "$astroPath"
        cfgFile.parentFile?.mkdirs()
        //can add more later if needed
        val lines = listOf(
            "index $indexDir/index-4117.fits",
            "index $indexDir/index-4118.fits",
            "index $indexDir/index-4119.fits"
        )
        cfgFile.writeText(lines.joinToString("\n"))
    }

    /**
     * Converts a bitmap to FITS format
     * @param bitmap The bitmap to convert
     * @param output The output file
     */
    fun convertBitmapToFits(bitmap: Bitmap, output: File) {
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
        currentAngles: String
    ) {
        try {
            val bitmap = android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, Uri.fromFile(imageFile))
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
                statusText.text = "⚠️ Solver interrupted due to timeout"
                return
            }

            val exitCode = process.exitValue()
            if (exitCode != 0) {
                return
            }

            val outputDir = File("$astroPath/output")

            if (outputDir.exists() && outputDir.isDirectory) {
                val filesToSend = outputDir.listFiles()?.filter { it.isFile } ?: emptyList()
                val corrFile = filesToSend.find { it.name.contains("corr") }
                if (corrFile != null) {
                    val intent = Intent(context, ResultActivity::class.java)
                    intent.putExtra("imagePath", imageFile.absolutePath)
                    context.startActivity(intent)
                }
                if (filesToSend.isNotEmpty()) {
                    for (file in filesToSend) {
                        val anglesToSend = if (currentAngles == "unknown") "Photo has no EXIF" else currentAngles
                        FileUtils.saveFileToDownloads(context, file)
                    }
                    statusText.text = "✅ All files from output sent"
                } else {
                    statusText.text = "⚠️ Output folder is empty"
                }
            } else {
                statusText.text = "⚠️ Output folder not found"
            }


        } catch (e: Exception) {
            statusText.text = "❌ Solver error: ${e.message}"
        }
    }
}
