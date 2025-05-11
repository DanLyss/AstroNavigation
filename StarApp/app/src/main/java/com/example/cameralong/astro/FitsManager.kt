package com.example.cameralong.astro

import android.graphics.Bitmap
import android.graphics.Color
import java.io.File
import java.io.FileOutputStream
import nom.tam.fits.Fits
import nom.tam.fits.Header
import nom.tam.fits.ImageData
import nom.tam.fits.ImageHDU
import nom.tam.fits.BinaryTableHDU
import nom.tam.util.BufferedDataOutputStream

/**
 * Manager class for FITS file operations
 */
class FitsManager {
    companion object {
        private const val TAG = "FitsManager"

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
    }
}