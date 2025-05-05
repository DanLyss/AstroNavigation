package kotlintranslation

import java.io.File
import nom.tam.fits.Fits
import nom.tam.fits.BinaryTableHDU
import kotlin.math.pow


object CorrClusterReader {

    fun fromCorrFile(
        corrPath: String,
        matchWeightThreshold: Double = 0.995
    ): List<Star> {

        val file = File(corrPath)
        require(file.exists()) { "Error: .corr file not found at $corrPath" }


        val fits = Fits(corrPath)
        val hdus = fits.read()
        val hdu = hdus[1] as BinaryTableHDU
        @Suppress("UNCHECKED_CAST")
        val table = hdu.kernel as Array<Array<Any>>


        val colNames: List<String> = (0 until hdu.nCols).map { hdu.getColumnName(it) }
        val colIndex: Map<String, Int> = colNames.withIndex().associate { it.value to it.index }


        val fieldNames = listOf("field_x", "field_y", "field_ra", "field_dec", "match_weight")


        val data: List<Map<String, Double>> = table.map { row: Array<Any> ->
            val map = mutableMapOf<String, Double>()
            for (field in fieldNames) {
                val idx = colIndex[field] ?: continue
                map[field] = (row[idx] as Number).toDouble()
            }
            map
        }


        val filtered: List<Map<String, Double>> = data.filter { props: Map<String, Double> ->
            (props["match_weight"] ?: 0.0) > matchWeightThreshold
        }
        require(filtered.isNotEmpty()) { "No high-confidence star data found in $corrPath" }


        val xList: List<Double> = filtered.map { props: Map<String, Double> -> props["field_x"] ?: 0.0 }
        val yList: List<Double> = filtered.map { props: Map<String, Double> -> -(props["field_y"] ?: 0.0) }
        val cx = xList.average()
        val cy = yList.average()


        val offsetPair: Pair<Double, Double> = filtered
            .mapIndexed { idx: Int, props: Map<String, Double> -> Pair(xList[idx], yList[idx]) }
            .minByOrNull { p: Pair<Double, Double> -> (p.first - cx).pow(2) + (p.second - cy).pow(2) }
            ?: Pair(0.0, 0.0)

        val stars: List<Star> = filtered.map { props: Map<String, Double> ->
            val rawX = props["field_x"] ?: 0.0
            val rawY = -(props["field_y"] ?: 0.0)
            val x = rawX - offsetPair.first
            val y = rawY + offsetPair.second
            val ra = Math.toRadians(props["field_ra"] ?: 0.0)
            val dec = Math.toRadians(props["field_dec"] ?: 0.0)
            Star(x, y, ra, dec)
        }

        return stars
    }
}
