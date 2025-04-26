import kotlin.math.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class Star(
    var xMeasuredCoord: Double,
    var yMeasuredCoord: Double,
    var RA: Double,
    var dec: Double
) {
    var xCoord: Double? = null
    var yCoord: Double? = null
    var Alt: Double? = null
    var Az: Double? = null

    fun angularDist(other: Star): Double {
        return acos(sin(dec) * sin(other.dec) + cos(dec) * cos(other.dec) * cos(RA - other.RA))
    }

    fun planarDist(other: Star): Double {
        return sqrt((xCoord!! - other.xCoord!!).pow(2) + (yCoord!! - other.yCoord!!).pow(2))
    }

    fun deltaX(other: Star): Double = abs(xCoord!! - other.xCoord!!)
    fun deltaY(other: Star): Double = abs(yCoord!! - other.yCoord!!)

    fun angularXProjDist(other: Star): Double {
        return asin(sin(angularDist(other)) * (deltaX(other) / planarDist(other)))
    }

    fun angularYProjDist(other: Star): Double {
        return asin(sin(angularDist(other)) * (deltaY(other) / planarDist(other)))
    }

    fun precalculateABCbeta(other: Star, axis: Int, pixLength: Double): Quadruple {
        val P1 = this.copy().apply {
            if (axis == 0) xCoord = abs(xCoord!!)
            if (axis == 1) yCoord = abs(yCoord!!)
        }
        val P2 = other.copy().apply {
            if (axis == 0) xCoord = abs(xCoord!!)
            if (axis == 1) yCoord = abs(yCoord!!)
        }

        return if (axis == 0) {
            if (P1.xCoord!! > P2.xCoord!!) P1 to P2 else P2 to P1
            Quadruple(P1.xCoord!!, P2.xCoord!! - P1.xCoord!!, pixLength - P2.xCoord!!, P1.angularXProjDist(P2))
        } else {
            if (P1.yCoord!! > P2.yCoord!!) P1 to P2 else P2 to P1
            Quadruple(P1.yCoord!!, P2.yCoord!! - P1.yCoord!!, pixLength - P2.yCoord!!, P1.angularYProjDist(P2))
        }
    }

    fun setAltAz(angularX: Double, angularY: Double, pixelX: Double, pixelY: Double, positionalAngle: Double) {
        val pi = positionalAngle
        val alpha = atan((xCoord!! * tan(angularX)) / pixelX)
        val beta = atan((yCoord!! * tan(angularY)) / pixelY)
        val c = Math.PI / 2 - beta
        val z = alpha

        Alt = asin(cos(c) * cos(pi) + sin(c) * sin(pi) * cos(z))
        Az = asin((sin(c) * sin(z)) / cos(Alt!!))
    }

    fun setNormalXY(rotationAngle: Double) {
        val cosRot = cos(rotationAngle)
        val sinRot = sin(rotationAngle)
        xCoord = cosRot * xMeasuredCoord + sinRot * yMeasuredCoord
        yCoord = -sinRot * xMeasuredCoord + cosRot * yMeasuredCoord
    }

    fun copy(): Star {
        val newStar = Star(xMeasuredCoord, yMeasuredCoord, RA, dec)
        newStar.xCoord = this.xCoord
        newStar.yCoord = this.yCoord
        newStar.Alt = this.Alt
        newStar.Az = this.Az
        return newStar
    }
}

class StarCluster(
    val stars: List<Star>,
    val positionalAngle: Double,
    val rotationAngle: Double,
    val timeGMT: String = "2001-09-11T22:00:00+02:00"
) {
    var angularXSize: Double = 0.0
    var angularYSize: Double = 0.0
    var pixLengthX: Double = 100.0
    var pixLengthY: Double = 100.0
    var AzStar0: Double = 0.0
    var phi: Double = 0.0
    var longitude: Double = 0.0

    init {
        setNormalXYForAll()
        solveAngularSizes()
        setAltAzForAll()
        val result = LattLongCalc.meanLatitude(this)
        AzStar0 = result.first
        phi = result.second
        makeAzAbsolute()
        longitude = LattLongCalc.meanLongitude(this, ZonedDateTime.parse(timeGMT, DateTimeFormatter.ISO_OFFSET_DATE_TIME))
    }

    private fun setNormalXYForAll() {
        stars.forEach { it.setNormalXY(rotationAngle) }
    }

    private fun solveAngularSizes() {
        val abcbetaX = mutableListOf<Quadruple>()
        val abcbetaY = mutableListOf<Quadruple>()

        for (i in stars.indices) {
            for (j in stars.indices) {
                if (i != j) {
                    val s1 = stars[i]
                    val s2 = stars[j]
                    if (s1.xCoord!! * s2.xCoord!! > 0) abcbetaX.add(s1.precalculateABCbeta(s2, 0, pixLengthX / 2))
                    if (s1.yCoord!! * s2.yCoord!! > 0) abcbetaY.add(s1.precalculateABCbeta(s2, 1, pixLengthY / 2))
                }
            }
        }

        val solver = Solver()
        angularXSize = solver.imagSolveTotal(abcbetaX)
        angularYSize = solver.imagSolveTotal(abcbetaY)
    }

    private fun setAltAzForAll() {
        for (star in stars) {
            star.setAltAz(angularXSize, angularYSize, pixLengthX / 2, pixLengthY / 2, positionalAngle)
        }
    }

    private fun makeAzAbsolute() {
        for (star in stars) {
            star.Az = (star.Az!! + AzStar0) % (2 * Math.PI)
        }
    }
}

data class Quadruple(val a: Double, val b: Double, val c: Double, val beta: Double)

class Solver {
    fun imagScaleOneInput(input: Quadruple): Pair<Double?, Double?> {
        val (a, b, c, beta) = input
        val D = b * b - 4 * (a + b) * tan(beta).pow(2) * a
        if (D < 0) return Pair(null, null)

        val x1 = (b + sqrt(D)) / (2 * (a + b) * tan(beta))
        val x2 = (b - sqrt(D)) / (2 * (a + b) * tan(beta))

        val totalSize1 = atan(((a + b + c) * x1) / a)
        val totalSize2 = atan(((a + b + c) * x2) / a)
        return Pair(totalSize1, totalSize2)
    }

    fun imagSolveTotal(inputs: List<Quadruple>): Double {
        val sizes = inputs.mapNotNull {
            val (s1, s2) = imagScaleOneInput(it)
            if (s1 != null && s2 != null) minOf(s1, s2) else null
        }.filterNotNull().toDoubleArray()

        val mean = sizes.average()
        val std = sqrt(sizes.map { (it - mean).pow(2) }.average())
        val filtered = sizes.filter { it > mean - 2 * std && it < mean + 2 * std }

        return filtered.average()
    }
}
