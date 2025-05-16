package com.starapp.navigation.file

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

/**
 * Manager class for file-related operations
 */
class FileManager {
    companion object {
        private const val TAG = "FileManager"

        // Shared thread pool for file operations
        private val fileExecutor = java.util.concurrent.Executors.newSingleThreadExecutor()

        /**
         * Initializes the file system for the application
         * @param context The application context
         * @param astroPath The path to the astrometry files
         * @param onStatusUpdate Callback for updating status
         * @param onComplete Callback for when initialization is complete
         */
        fun initializeFileSystem(
            context: Context,
            astroPath: String,
            onStatusUpdate: (String) -> Unit,
            onComplete: (Boolean) -> Unit
        ) {
            // Show a loading indicator or message
            onStatusUpdate("Initializing...")

            // Use a background thread for file operations
            fileExecutor.execute {
                try {
                    createDirectories(astroPath)
                    extractZipAssets(context, astroPath)
                    generateAstrometryConfig(astroPath)

                    // Update UI on main thread when done
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onStatusUpdate("")
                        onComplete(true)
                    }
                } catch (e: Exception) {
                    // Handle errors on main thread
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onStatusUpdate("Initialization error: ${e.message}")
                        onComplete(false)
                    }
                }
            }
        }

        /**
         * Cleans up old .corr files in the output directory synchronously
         * @param astroPath The path to the astrometry files
         */
        fun cleanupCorrFiles(astroPath: String) {
            val outputDir = File("$astroPath/output")
            if (outputDir.exists() && outputDir.isDirectory) {
                outputDir.listFiles()?.filter { it.name.endsWith(".corr") }?.forEach { it.delete() }
            }
        }

        /**
         * Cleans up old .corr files in the output directory asynchronously
         * @param astroPath The path to the astrometry files
         * @param callback Optional callback to be executed when cleanup is complete
         */
        fun cleanupCorrFilesAsync(astroPath: String, callback: (() -> Unit)? = null) {
            fileExecutor.execute {
                cleanupCorrFiles(astroPath)
                callback?.invoke()
            }
        }

        /**
         * Saves a file to the Downloads directory synchronously
         * @param context The application context
         * @param sourceFile The source file to save
         * @param onSuccess Callback for successful save
         * @param onError Callback for error handling
         */
        fun saveFileToDownloads(
            context: Context, 
            sourceFile: File,
            onSuccess: (String) -> Unit = {},
            onError: (String) -> Unit = {}
        ) {
            try {
                val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsFolder.exists()) {
                    downloadsFolder.mkdirs()
                }
                val destFile = File(downloadsFolder, sourceFile.name)
                sourceFile.copyTo(destFile, overwrite = true)

                onSuccess("✅ File saved: ${destFile.absolutePath}")
            } catch (e: Exception) {
                onError("❌ Save error: ${e.message}")
            }
        }

        /**
         * Saves a file to the Downloads directory asynchronously
         * @param context The application context
         * @param sourceFile The source file to save
         * @param onSuccess Callback for successful save
         * @param onError Callback for error handling
         * @param onComplete Optional callback to be executed when save is complete
         */
        fun saveFileToDownloadsAsync(
            context: Context, 
            sourceFile: File, 
            onSuccess: (String) -> Unit = {},
            onError: (String) -> Unit = {},
            onComplete: ((success: Boolean) -> Unit)? = null
        ) {
            fileExecutor.execute {
                try {
                    val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsFolder.exists()) {
                        downloadsFolder.mkdirs()
                    }
                    val destFile = File(downloadsFolder, sourceFile.name)
                    sourceFile.copyTo(destFile, overwrite = true)

                    // Call success callback on main thread
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onSuccess("✅ File saved: ${destFile.absolutePath}")
                    }

                    onComplete?.invoke(true)
                } catch (e: Exception) {
                    // Call error callback on main thread
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        onError("❌ Save error: ${e.message}")
                    }

                    onComplete?.invoke(false)
                }
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
                "index $indexDir/index-4114.fits",
                "index $indexDir/index-4115.fits",
                "index $indexDir/index-4116.fits",
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
