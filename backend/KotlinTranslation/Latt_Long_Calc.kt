import kotlin.math.*
import java.time.ZonedDateTime
import java.time.temporal.ChronoField
import org.apache.commons.math3.fitting.leastsquares.*
import org.apache.commons.math3.linear.*
import org.apache.commons.math3.stat.descriptive.summary.Sum
import org.apache.commons.math3.util.Pair


object LattLongCalc {

    private const val iters = 5

    data class AstroSolution(
        val latitude: Double,
        val azShift: Double,
        val errors: DoubleArray
    )

    private fun eqSetup(star: Star, az: Double): Triple<Double, Double, Double> {
        val a = cos(star.Alt!!).pow(2) * cos(az).pow(2) + sin(star.Alt!!).pow(2)
        val b = 2 * cos(star.Alt!!) * cos(az) * sin(star.dec)
        val c = sin(star.dec).pow(2) - sin(star.Alt!!).pow(2)
        return Triple(a, b, c)
    }

    private fun equation(star: Star, az: Double, phi: Double): Double {
        val (a, b, c) = eqSetup(star, az)
        return phi * phi * a + phi * b + c
    }

    private fun norm(x: Double, lower: Double, upper: Double): Double {
        var result = x
        while (result < lower) result += upper - lower
        while (result > upper) result -= upper - lower
        return result
    }

    private fun solveLatitudeAndAzimuth(stars: List<Star>, phi_guess: Double, az_guess: Double): AstroSolution {
        require(stars.isNotEmpty()) { "Star list cannot be empty" }

        val model = MultivariateJacobianFunction { point ->
            val phi = point.getEntry(0)
            val az0 = point.getEntry(1)

            val residuals = DoubleArray(stars.size) { i ->
                val star = stars[i]
                val realAz = star.Az!! + az0
                val predictedAlt = asin(
                    sin(phi) * sin(star.dec) +
                            cos(phi) * cos(star.dec) * cos(realAz)
                )
                predictedAlt - star.Alt!!
            }

            val jacobian = Array(stars.size) { i ->
                val star = stars[i]
                val realAz = star.Az!! + az0
                val delta = star.dec

                val cosArg = sin(phi) * sin(delta) + cos(phi) * cos(delta) * cos(realAz)
                val denom = sqrt(1 - cosArg.pow(2))

                val dFdPhi = (cos(phi) * sin(delta) - sin(phi) * cos(delta) * cos(realAz)) / denom
                val dFdAz0 = (-cos(phi) * cos(delta) * sin(realAz)) / denom

                doubleArrayOf(dFdPhi, dFdAz0)
            }

            Pair(ArrayRealVector(residuals), Array2DRowRealMatrix(jacobian))
        }

        val initialGuess = doubleArrayOf(phi_guess, az_guess)

        val problem = LeastSquaresBuilder()
            .start(initialGuess)
            .model(model)
            .target(DoubleArray(stars.size) { 0.0 })
            .lazyEvaluation(false)
            .maxEvaluations(1000)
            .maxIterations(1000)
            .build()

        val optimizer = LevenbergMarquardtOptimizer()
        val optimum = optimizer.optimize(problem)

        val solution = optimum.point.toArray()
        val latitude = solution[0]
        val azShiftFirstStar = solution[1]

        // Now compute final error vector
        val errors = DoubleArray(stars.size) { i ->
            val star = stars[i]
            val realAz = star.Az!! + azShiftFirstStar
            val predictedAlt = asin(
                sin(latitude) * sin(star.dec) +
                        cos(latitude) * cos(star.dec) * cos(realAz)
            )
            predictedAlt - star.Alt!!
        }

        return AstroSolution(
            latitude = latitude,
            azShift = azShiftFirstStar,
            errors = errors
        )
    }


    fun meanLatitude(cluster: StarCluster, hemisphere: String = "North"): kotlin.Pair<Double, Double> {

        var ans = Triple(0.0, 0.0, 1e9)
        for (i in 1..iters) {
            for (j in 1..iters) {

                val result = solveLatitudeAndAzimuth(cluster.stars, Math.PI * i / iters, 2 * Math.PI * j / iters)
                val max_err = result.errors.max()

                if ((cos(result.latitude) < 0 && hemisphere == "North") || (cos(result.latitude) > 0 && hemisphere == "South")) continue
                if (max_err < ans.third) {
                    ans = Triple(norm(result.azShift, 0.0, 2 * Math.PI), result.latitude, max_err)
                }
            }
        }

        return ans.first to ans.second
    }

    private fun timeEq(days: Double, year: Int): Double {
        val d = 6.24 + 0.0172 * (365.35 * (year - 2000) + days)
        return (-7.659 * sin(d) + 9.863 * sin(2 * d + 3.5932)) / 60
    }

    private fun sunRA(dayOfYear: Int, year: Int): Double {
        fun julian_day(year: Int, dayOfYear: Int): Int {
            val a = (14 - 1) / 12
            val y = year + 4800 - a
            val m = 1 + 12 * a - 3
            return dayOfYear + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
        }

        val jd = julian_day(year, dayOfYear)
        val T = (jd - 2451545.0) / 36525
        val L0 = (280.46646 + 36000.76983 * T + 0.0003032 * T * T) % 360
        val M = (357.52911 + 35999.05029 * T - 0.0001537 * T * T) % 360
        val e = 0.016708634 - 0.000042037 * T - 0.0000001267 * T * T

        val C = (1.914602 - 0.004817 * T - 0.000014 * T * T) * sin(Math.toRadians(M)) +
                (0.019993 - 0.000101 * T) * sin(Math.toRadians(2 * M)) +
                0.000289 * sin(Math.toRadians(3 * M))

        val trueLong = L0 + C
        val epsilon = 23 + 26.0 / 60 + 21.448 / 3600 - (46.8150 * T + 0.00059 * T * T - 0.001813 * T * T * T) / 3600
        var alphaRad =
            atan2(cos(Math.toRadians(epsilon)) * sin(Math.toRadians(trueLong)), cos(Math.toRadians(trueLong)))

        if (alphaRad < 0) {
            alphaRad += 2 * Math.PI
        }

        return alphaRad
    }

    private fun toHours(dt: ZonedDateTime): Double {
        return dt.hour + dt.minute / 60.0 + dt.second / 3600.0 + dt.nano / 3_600_000_000_000.0
    }

    private fun trueLocalTime(star: Star, N: Int, year: Int, phi: Double): Double? {
        val az = star.Az!! % (2 * Math.PI)
        val cosH = (sin(star.Alt!!) - sin(phi) * sin(star.dec)) / (cos(phi) * cos(star.dec))
        if (abs(cosH) > 1) return null

        var t = acos(cosH)
        if (az > Math.PI) t = 2 * Math.PI - t
        val raSun = sunRA(N, year)
        val localTime = (12 + (t + star.RA - raSun) / (2 * Math.PI) * 24) % 24
        return localTime - timeEq(N.toDouble(), year)
    }

    fun meanLongitude(cluster: StarCluster, dt: ZonedDateTime): Double {
        val phi = meanLatitude(cluster).second
        val day = dt.get(ChronoField.DAY_OF_YEAR)
        val year = dt.year
        val hour = toHours(dt)

        val longitudes = cluster.stars.mapNotNull {
            trueLocalTime(it, day, year, phi)?.let { time ->
                Math.toRadians(norm((time - hour) * 15, -180.0, 180.0))
            }
        }

        return longitudes.average()
    }
}
