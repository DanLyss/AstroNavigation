package kotlintranslation

import org.apache.commons.math3.fitting.leastsquares.EvaluationRmsChecker
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer
import org.apache.commons.math3.fitting.leastsquares.MultivariateJacobianFunction
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import org.apache.commons.math3.linear.ArrayRealVector
import java.time.ZonedDateTime
import java.time.Duration
import java.time.temporal.ChronoUnit
import kotlin.math.*


object LattLongCalc {

    private const val iters = 10
    private const val AZ_SCALE = 1.0 / (2 * Math.PI)


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

    fun norm(x: Double, lower: Double, upper: Double): Double {
        var result = x
        while (result < lower) result += upper - lower
        while (result > upper) result -= upper - lower
        return result
    }



    private fun solveLatitudeAndAzimuth(
        stars: List<Star>, hemisphere: String="North"
    ): kotlin.Pair<Double,Double> {
        var bestErr = Double.POSITIVE_INFINITY
        var bestAz0 = 0.0
        var bestPhi = 0.0

        for (i in 1..iters) {
            for (j in 1..iters) {
                val rawAz0  = 2*PI*i/iters
                val phiGuess= PI*j/iters
                val sGuess  = rawAz0 * AZ_SCALE

                val model = MultivariateJacobianFunction { point ->
                    val s      = point.getEntry(0)
                    val phiArg = point.getEntry(1)
                    val az0    = s / AZ_SCALE

                    val resid  = DoubleArray(stars.size)
                    val jac    = Array(stars.size) { DoubleArray(2) }

                    for (k in stars.indices) {
                        val star  = stars[k]
                        val realAz= star.Az!! + az0
                        resid[k] = equation(star, realAz, phiArg)

                        val (A,B,_) = eqSetup(star, realAz)
                        val da     = -2*cos(star.Alt!!).pow(2) * cos(realAz)*sin(realAz)
                        val db     = -2*cos(star.Alt!!)*sin(star.dec)*sin(realAz)
                        val dR_daz0= phiArg*phiArg*da + phiArg*db
                        val dR_dphi= 2*phiArg*A + B

                        jac[k][0] = dR_daz0 * (1.0 / AZ_SCALE)
                        jac[k][1] = dR_dphi
                    }

                    org.apache.commons.math3.util.Pair(
                        ArrayRealVector(resid),
                        Array2DRowRealMatrix(jac)
                    )
                }
                val checker = EvaluationRmsChecker(1e-9)
                val problem = LeastSquaresBuilder()
                    .start(doubleArrayOf(sGuess, phiGuess))
                    .model(model)
                    .target(DoubleArray(stars.size){0.0})
                    .maxEvaluations(500).maxIterations(500)
                    .checker(checker)
                    .build()

                try {
                    val opt   = LevenbergMarquardtOptimizer().optimize(problem)
                    val sol   = opt.point.toArray()
                    val sSol  = sol[0]
                    val phiArg= sol[1]
                    if (phiArg !in -1.0..1.0) continue

                    var phi = acos(phiArg)
                    if (phi > PI/2) phi -= PI
                    if ((phiArg<0 && hemisphere=="North")
                        ||(phiArg>0 && hemisphere=="South")) continue

                    val errs = opt.residuals.toArray().map{ abs(it) }
                    val maxE = errs.maxOrNull() ?: continue
                    val az0SOLUTION = norm(sSol / AZ_SCALE, 0.0, 2*PI)

                    if (maxE < bestErr) {
                        bestErr = maxE
                        bestAz0 = az0SOLUTION
                        bestPhi = phi
                    }
                } catch (_: Exception) { }
            }
        }


        return bestPhi to bestAz0
    }


    fun meanLatitude(cluster: StarCluster, hemisphere: String = "North"): kotlin.Pair<Double,Double> {
        val cnt = (cluster.stars.size * 0.75).toInt()
        var ans = mutableListOf<Pair<Double, Double>>()
        var otherStars = cluster.stars.toMutableList()
        val anchor = cluster.stars[0]
        for (star in cluster.stars) {
            if (abs(star.xCoord!!) < 1e-6 && abs(star.yCoord!!) < 1e-6){
                val anchor = star
                otherStars.remove(star)
                break
            }
        }
        for (k in 1..50){
            val currentStars = listOf<Star>(anchor)+otherStars.shuffled().take(cnt)
            ans.add(solveLatitudeAndAzimuth(currentStars, hemisphere))
        }
        return ans.map{it.first}.average() to ans.map{it.second}.average()
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

    private fun dayOfYearFraction(dt: ZonedDateTime): Double {
        val start = dt.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS)
        val secs  = Duration.between(start, dt).seconds + dt.nano / 1e9
        return secs / 86400.0
    }
    private fun trueLocalTime(
        star: Star,
        days: Double,
        year: Int,
        phi: Double
    ): Double? {
        val az = star.Az!! % (2 * Math.PI)
        val cosH = (sin(star.Alt!!) - sin(phi) * sin(star.dec)) /
                (cos(phi) * cos(star.dec))
        if (abs(cosH) > 1) return null

        var t = acos(cosH)
        if (az > Math.PI) t = 2 * Math.PI - t


        val raSun = sunRA(days.toInt(), year)
        val localTime = (12 + (t + star.RA - raSun) / (2 * Math.PI) * 24) % 24

        return localTime - timeEq(days, year)
    }

    fun meanLongitude(cluster: StarCluster, dt: ZonedDateTime): Double {
        val phi  = cluster.phi
        val days = dayOfYearFraction(dt)
        val year = dt.year
        val hour = toHours(dt)

        val longs = cluster.stars.mapNotNull { star ->
            trueLocalTime(star, days, year, phi)?.let { lt ->
                Math.toRadians(norm((lt - hour) * 15, -180.0, 180.0))
            }
        }
        return longs.average()
    }
}