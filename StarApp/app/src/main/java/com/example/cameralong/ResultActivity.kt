package com.example.cameralong

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import nom.tam.fits.Fits
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

                // Check if corresponding .corr file exists
                val corrFile = File(imagePath.replace(".jpg", ".corr"))
                if (corrFile.exists()) {
                    try {
                        val fits = Fits(corrFile)
                        val hdus = fits.read()
                        val tableHDU = hdus[1] as nom.tam.fits.BinaryTableHDU

                        val fieldX = tableHDU.getColumn("field_x") as DoubleArray
                        val fieldY = tableHDU.getColumn("field_y") as DoubleArray


                        // Convert to float pairs for drawing
                        val points = fieldX.zip(fieldY).map { (x, y) ->
                            x.toFloat() to y.toFloat()
                        }

                        // Pass coordinates to overlay view
                        overlay.starPoints = points
                        overlay.invalidate()

                    } catch (e: Exception) {
                        Toast.makeText(this, "Error reading .corr file: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this, ".corr file not found", Toast.LENGTH_SHORT).show()
                }

            } else {
                imageView.setImageResource(android.R.drawable.ic_delete)
                Toast.makeText(this, "Image file not found", Toast.LENGTH_SHORT).show()
            }
        }

        // On click: go to the next screen
        continueButton.setOnClickListener {
            val intent = Intent(this, StarsActivity::class.java)
            startActivity(intent)
        }
    }
}
