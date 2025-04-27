//import java.io.File
//import java.util.Random
//import nom.tam.fits.Fits
//import nom.tam.util.BufferedFile
//package kotlintranslation
//import nom.tam.fits.BinaryTableHDU
//
//
//object AstrometryProcessor {
//    fun processAstrometryImage(
//        imagePath: String,
//        astrometryCommand: String = "/usr/bin/solve-field",
//        outputFile: String = "cluster_output.txt",
//        arcsecPerPix: Double = 220.0,
//        delta: Double = 10.0,
//        posAngleDeg: Double = 10.0,
//        rotAngleDeg: Double = 90.0,
//        matchWeightThreshold: Double = 0.995,
//        maxStarCount: Int = 10,
//        runAstrometry: Boolean = true,
//        seed: Int = 0
//    ): StarCluster? {
//
//        fun runSolveField() {
//            val command = listOf(
//                astrometryCommand,
//                "--scale-low", (arcsecPerPix - delta).toString(),
//                "--scale-high", (arcsecPerPix + delta).toString(),
//                "--scale-units", "arcsecperpix",
//                "--overwrite",
//                imagePath
//            )
//            println("Running astrometry command:\n${command.joinToString(" ")}")
//            ProcessBuilder(command).inheritIO().start().waitFor()
//        }
//
//
//        fun analyzeCorr(corrPath: String): List<Map<String, Double>> {
//            val file = File(corrPath)
//            if (!file.exists()) {
//                println("Error: .corr file not found!")
//                return emptyList()
//            }
//
//            val results = mutableListOf<Map<String, Double>>()
//
//            BufferedFile(corrPath, "r").use { bf ->
//                val fits = Fits(bf)
//                val hdu = fits.read()[1] as BinaryTableHDU  // No type argument
//
//                val table = hdu.kernel as Array<Array<Any>>
//                val fieldIndices = listOf("field_x", "field_y", "field_ra", "field_dec", "match_weight")
//
//                // Get column names from the BinaryTableHDU
//                val columnNames = (0 until hdu.nCols).map { hdu.getColumnName(it) }
//                val columnMap = columnNames.withIndex().associate { it.value to it.index }
//
//                for (row in table) {
//                    val star = mutableMapOf<String, Double>()
//                    for (name in fieldIndices) {
//                        val idx = columnMap[name] ?: continue
//                        star[name] = (row[idx] as Number).toDouble()
//                    }
//                    results.add(star)
//                }
//            }
//
//            return results
//        }
//
//        fun extractClusterFromOutput(amOutput: List<Map<String, Double>>): StarCluster {
//            val filteredStars = amOutput.filter { it["match_weight"] ?: 0.0 > matchWeightThreshold }
//            if (filteredStars.size < maxStarCount) throw Exception("Not enough high-confidence stars found")
//
//            val random = Random(seed.toLong())
//            val selected = filteredStars.shuffled(random).take(maxStarCount)
//            val stars = selected.map {
//                Star(
//                    it["field_x"] ?: 0.0,
//                    -(it["field_y"] ?: 0.0), // Flip Y
//                    Math.toRadians(it["field_ra"] ?: 0.0),
//                    Math.toRadians(it["field_dec"] ?: 0.0)
//                )
//            }
//
//            return StarCluster(stars, Math.toRadians(posAngleDeg), Math.toRadians(rotAngleDeg))
//        }
//
//        if (runAstrometry) {
//            runSolveField()
//            Thread.sleep(2000)
//        }
//
//        val corrPath = imagePath.replace(".jpg", ".corr")
//        val amOutput = analyzeCorr(corrPath)
//        if (amOutput.isEmpty()) {
//            println("No star data found, exiting.")
//            return null
//        }
//
//        val cluster = extractClusterFromOutput(amOutput)
//        File(outputFile).writeText(cluster.stars.joinToString("\n") { it.toString() })
//        println("Cluster written to: $outputFile")
//        return cluster
//    }
//}
