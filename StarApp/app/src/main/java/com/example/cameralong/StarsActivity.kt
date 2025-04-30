package com.example.cameralong

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity

class StarsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stars)

        val textView = findViewById<TextView>(R.id.starsText)

        val stars = listOf(
            "⭐ Star 1: Alt = 45°, Az = α + 15°",
            "⭐ Star 2: Alt = 60°, Az = α − 10°",
            "⭐ Star 3: Alt = 22°, Az = α + 5°",
            "⭐ Star 4: Alt = 30°, Az = α − 22°",
            "⭐ Star 5: Alt = 55°, Az = α + 33°"
        )

        textView.text = stars.joinToString("\n\n")
        val nextButton = findViewById<Button>(R.id.nextButton)
        nextButton.setOnClickListener {
            val intent = Intent(this, LocationActivity::class.java)
            startActivity(intent)
        }
    }
}
