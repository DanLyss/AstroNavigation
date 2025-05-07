package com.example.cameralong

import nom.tam.fits.Fits
import nom.tam.fits.BinaryTableHDU
import java.io.File

/**
 * Utility class for FITS file operations
 */
object FitsUtils {

    /**
     * Extracts star coordinates from a FITS file
     * @param corrFile The FITS file containing star coordinates
     * @return A list of star coordinates as pairs of floats (x, y)
     * @throws Exception if the file cannot be read or does not contain the expected data
     */
    fun extractStarCoordinates(corrFile: File): List<Pair<Float, Float>> {
        if (!corrFile.exists()) {
            throw IllegalArgumentException("FITS file does not exist: ${corrFile.absolutePath}")
        }

        val fits = Fits(corrFile)
        val hdus = fits.read()
        val tableHDU = hdus[1] as BinaryTableHDU

        val fieldX = tableHDU.getColumn("field_x") as DoubleArray
        val fieldY = tableHDU.getColumn("field_y") as DoubleArray

        // Convert to float pairs for drawing
        return fieldX.zip(fieldY).map { (x, y) ->
            x.toFloat() to y.toFloat()
        }
    }
}
