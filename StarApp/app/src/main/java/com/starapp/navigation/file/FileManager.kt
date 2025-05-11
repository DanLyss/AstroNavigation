package com.starapp.navigation.file

import android.content.Context
import android.os.Environment
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * Manager class for file-related operations
 */
class FileManager {
    companion object {
        private const val TAG = "FileManager"

        /**
         * Cleans up old .corr files in the output directory
         * @param astroPath The path to the astrometry files
         */
        fun cleanupCorrFiles(astroPath: String) {
            val outputDir = File("$astroPath/output")
            if (outputDir.exists() && outputDir.isDirectory) {
                outputDir.listFiles()?.filter { it.name.endsWith(".corr") }?.forEach { it.delete() }
            }
        }

        /**
         * Saves a file to the Downloads directory
         * @param context The application context
         * @param sourceFile The source file to save
         */
        fun saveFileToDownloads(context: Context, sourceFile: File) {
            try {
                val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsFolder.exists()) {
                    downloadsFolder.mkdirs()
                }
                val destFile = File(downloadsFolder, sourceFile.name)
                sourceFile.copyTo(destFile, overwrite = true)

                Toast.makeText(context, "✅ File saved: ${destFile.absolutePath}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "❌ Save error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

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
         * Creates necessary directories for the application
         * @param astroPath The base path for astrometry files
         */
        fun createDirectories(astroPath: String) {
            File("$astroPath/output").mkdirs()
            File("$astroPath/tmp").mkdirs()
        }
    }
}
