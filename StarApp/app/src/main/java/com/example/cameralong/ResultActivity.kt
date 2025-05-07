package com.example.cameralong

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File

class ResultActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        // Get views from layout
        val imageView = findViewById<ImageView>(R.id.resultImageView)
        val overlay = findViewById<StarOverlayView>(R.id.starOverlay)
        val continueButton = findViewById<Button>(R.id.continueButton)

        // Retrieve image path from intent
        val imagePath = intent.getStringExtra("imagePath")

        if (imagePath != null) {
            val file = File(imagePath)

            if (file.exists()) {
                // Load and show the image
                val bitmap = BitmapFactory.decodeFile(imagePath)
                imageView.setImageBitmap(bitmap)

                // Set image dimensions on the overlay view
                overlay.imageWidth = bitmap.width
                overlay.imageHeight = bitmap.height

                // Check if corresponding .corr file exists
                val corrPath = File(imagePath).nameWithoutExtension + ".corr"
                val corrFile = File(filesDir, "astro/output/$corrPath")
                if (corrFile.exists()) {
                    try {
                        // Extract star coordinates using FitsUtils
                        val points = FitsUtils.extractStarCoordinates(corrFile)

                        // Pass coordinates to overlay view
                        overlay.starPoints = points
                        overlay.invalidate()

                    } catch (e: Exception) {
                        Toast.makeText(this, "Error reading .corr file: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, ".corr file not found: ${corrFile.absolutePath}", Toast.LENGTH_SHORT).show()
                }

            } else {
                imageView.setImageResource(android.R.drawable.ic_delete)
                Toast.makeText(this, "Image file not found", Toast.LENGTH_SHORT).show()
            }
        }

        // On click: go to the next screen
        continueButton.setOnClickListener {
                 val imgPath  = intent.getStringExtra("imagePath")
                     ?: throw IllegalStateException("No imagePath")
                 val corrPath = File(imgPath).nameWithoutExtension + ".corr"
                 val fullCorr = File(filesDir, "astro/output/$corrPath").absolutePath

                 Intent(this, StarsActivity::class.java).also { i ->
                         i.putExtra("imagePath", imgPath)
                         i.putExtra("corrPath", fullCorr)
                         startActivity(i)
                     }
                 finish()
             }
    }
}
