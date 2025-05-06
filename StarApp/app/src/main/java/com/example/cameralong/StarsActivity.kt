package com.example.cameralong

import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlintranslation.CorrClusterReader
import kotlintranslation.StarCluster

class StarsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stars)

        val textView   = findViewById<TextView>(R.id.starsText)
        val nextButton = findViewById<Button>(R.id.nextButton)

        val imagePath = intent.getStringExtra("imagePath")
            ?: throw IllegalStateException("No imagePath in Intent")
        val corrPath  = intent.getStringExtra("corrPath")
            ?: throw IllegalStateException("No corrPath in Intent")

        val imageFile = File(imagePath)
        require(imageFile.exists()) { "Image not found at $imagePath" }
        val corrFile  = File(corrPath)
        require(corrFile.exists())  { "Corr file not found at $corrPath" }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(imagePath, bounds)
        val width  = bounds.outWidth
        val height = bounds.outHeight

        val exif = ExifInterface(imagePath)
        val userComment = exif.getAttribute(ExifInterface.TAG_USER_COMMENT)
            ?: throw IllegalStateException("No EXIF UserComment")
        val regex = Regex("""[Yy]aw[:=]\s*([-\d.]+)\s*,\s*[Pp]itch[:=]\s*([-\d.]+)\s*,\s*[Rr]oll[:=]\s*([-\d.]+)""")
        val (yawDeg, pitchDeg, rollDeg) = regex.find(userComment)?.destructured
            ?: throw IllegalStateException("Cannot parse yaw/pitch/roll from '$userComment'")

        val dtString = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
            ?: throw IllegalStateException("No EXIF date/time")

        val offsetString = exif.getAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL)
            ?: exif.getAttribute(ExifInterface.TAG_OFFSET_TIME)

        val ldt = LocalDateTime.parse(dtString, DateTimeFormatter.ofPattern("yyyy:MM:dd HH:mm:ss"))
        val odt = if (!offsetString.isNullOrBlank()) {
            OffsetDateTime.of(ldt, ZoneOffset.of(offsetString))
        } else {
            ldt.atZone(ZoneId.systemDefault()).toOffsetDateTime()
        }
        val isoTime = odt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        val stars = CorrClusterReader.fromCorrFile(
            corrPath             = corrFile.absolutePath,
            matchWeightThreshold = 0.995,
            sizex                = width,
            sizey                = height
        )

        val cluster = StarCluster(
            stars           = stars,
            positionalAngle = Math.toRadians(pitchDeg.toDouble()),
            rotationAngle   = Math.toRadians(rollDeg.toDouble()),
            timeGMT         = isoTime
        )

        textView.text = buildString {
            appendLine("X size: size: $width")
            appendLine("Y size: size: $height")
            appendLine("Computed time: $isoTime")
            appendLine("Yaw:   $yawDeg°")
            appendLine("Pitch: $pitchDeg°")
            appendLine("Roll:  $rollDeg°")
            appendLine()
            appendLine("Found ${cluster.stars.size} stars")
            appendLine("X size: %.2f° per 100 pixels".format(Math.toDegrees(cluster.angularXSize)))
            appendLine("Y size: %.2f° per 100 pixels".format(Math.toDegrees(cluster.angularYSize)))
            appendLine("Cluster Az₀: %.2f°".format(Math.toDegrees(cluster.AzStar0)))
            appendLine()
            cluster.stars.forEachIndexed { i, star ->
                appendLine(
                    "⭐ Star ${i + 1}: Alt = %.1f°, Az = %.1f°"
                        .format(Math.toDegrees(star.Alt!!), Math.toDegrees(star.Az!!))
                )
            }
        }

        nextButton.setOnClickListener {
            Intent(this, LocationActivity::class.java).also { intent ->
                intent.putExtra("pred_phi",  cluster.phi)
                intent.putExtra("pred_long", cluster.longitude)
                intent.putExtra("imagePath", imagePath)
                startActivity(intent)
            }
        }
    }
}
