package kotlintranslation

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import kotlintranslation.LattLongCalc.norm
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class AstroNavigationTests {

    @TestFactory
    fun testLatitudeAndAzimuth(): Collection<DynamicTest> {
        return TestCases.testCases.mapIndexed { idx, case ->
            DynamicTest.dynamicTest("Latitude/Azimuth Test Case #${idx + 1}") {
                val stars = case.pixelCoords.zip(case.raDec).map { (pix, radec) ->
                    Star(pix.first, pix.second, Math.toRadians(radec.first), Math.toRadians(radec.second))
                }

                val cluster = StarCluster(
                    stars = stars,
                    positionalAngle = Math.toRadians(case.positionalAngleDeg),
                    rotationAngle = Math.toRadians(case.rotationAngleDeg),
                    timeGMT = case.datetime
                )


                val errorLatitude = Math.abs(case.latitude - Math.toDegrees(cluster.phi))
                println("Latitude error: $errorLatitude deg")
                assertTrue(errorLatitude < 2.0, "Latitude error too big")

                val errorAzimuth = Math.abs(
                    norm(case.azAltDeg[0].first, 0.0, 360.0) - norm(Math.toDegrees(cluster.stars[0].Az!!), 0.0, 360.0)
                )
                println("Azimuth error: $errorAzimuth deg")
                assertTrue(errorAzimuth < 2.0, "Azimuth error too big")
            }
        }
    }

    @TestFactory
    fun testLongitude(): Collection<DynamicTest> {
        return TestCases.testCases.mapIndexed { idx, case ->
            DynamicTest.dynamicTest("Longitude Test Case #${idx + 1}") {
                val stars = case.pixelCoords.zip(case.raDec).map { (pix, radec) ->
                    Star(pix.first, pix.second, Math.toRadians(radec.first), Math.toRadians(radec.second))
                }

                val cluster = StarCluster(
                    stars = stars,
                    positionalAngle = Math.toRadians(case.positionalAngleDeg),
                    rotationAngle = Math.toRadians(case.rotationAngleDeg),
                    timeGMT = case.datetime
                )

                val trueLongitude = case.longitude
                val predictedLongitude = Math.toDegrees(cluster.longitude)
                val errorLongitude = Math.abs(trueLongitude - predictedLongitude)
                println("Longitude error: $errorLongitude deg")
                assertTrue(errorLongitude < 2.0, "Longitude error too big")
            }
        }
    }
}
