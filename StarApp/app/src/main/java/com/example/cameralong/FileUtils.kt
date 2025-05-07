package com.example.cameralong

import android.content.Context
import android.os.Environment
import android.widget.Toast
import java.io.File

/**
 * Utility class for file-related operations
 */
object FileUtils {

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
}
