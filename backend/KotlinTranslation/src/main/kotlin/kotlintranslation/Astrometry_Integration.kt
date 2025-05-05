package kotlintranslation

import java.io.File
import nom.tam.fits.Fits
import nom.tam.fits.BinaryTableHDU
import nom.tam.util.ColumnTable
import kotlin.math.pow
import kotlintranslation.Star

object CorrClusterReader {
    private fun analyzeCorr(corrPath: String): List<Map<String, Double>> {
        val file = File(corrPath)
        require(file.exists()) { "Error: .corr file not found at $corrPath" }

        val fits = Fits(corrPath)
        val hdus = fits.read()
        val hdu = hdus[1] as BinaryTableHDU
        val ct = hdu.kernel as ColumnTable<*>
        val nRows = ct.nRows
        val nCols = ct.nCols

        val fieldNames = listOf("field_x", "field_y", "field_ra", "field_dec", "match_weight")
        val colNames = (0 until nCols).map { hdu.getColumnName(it) }
        val colIndex = colNames.withIndex().associate { it.value to it.index }

        val results = mutableListOf<Map<String, Double>>()
        for (r in 0 until nRows) {
            val rowMap = mutableMapOf<String, Double>()
            for (field in fieldNames) {
                val idx = colIndex[field] ?: continue
                val raw = ct.getElement(r, idx)
                val value = when (raw) {
                    is Number -> raw.toDouble()
                    is DoubleArray -> raw.first()
                    is Array<*> -> (raw[0] as Number).toDouble()
                    else -> throw IllegalArgumentException("Unexpected cell type: ${'$'}{raw.javaClass}")
                }
                rowMap[field] = value
            }
            results += rowMap
        }
        return results
    }

    fun fromCorrFile(corrPath: String, matchWeightThreshold: Double = 0.995): List<Star> {
        val data = analyzeCorr(corrPath)
        val filtered = data.filter { it["match_weight"] ?: 0.0 > matchWeightThreshold }
        require(filtered.isNotEmpty()) { "No high-confidence star data in $corrPath" }

        val xList = filtered.map { it["field_x"] ?: 0.0 }
        val yList = filtered.map { -(it["field_y"] ?: 0.0) }
        val cx = xList.average()
        val cy = yList.average()

        val offset = filtered
            .mapIndexed { i, props -> xList[i] to yList[i] }
            .minByOrNull { (x, y) -> (x - cx).pow(2) + (y - cy).pow(2) }
            ?: (0.0 to 0.0)

        return filtered.map { props ->
            val rawX = props["field_x"] ?: 0.0
            val rawY = -(props["field_y"] ?: 0.0)
            val x = rawX - offset.first
            val y = rawY - offset.second
            val ra = Math.toRadians(props["field_ra"] ?: 0.0)
            val dec = Math.toRadians(props["field_dec"] ?: 0.0)
            Star(x, y, ra, dec)
        }
    }
}
