package com.example.cameralong

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class StarOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var starPoints: List<Pair<Float, Float>> = emptyList()
    var imageWidth: Int = 0
    var imageHeight: Int = 0
    var imageMatrix: Matrix? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val paint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        if (starPoints.isEmpty() || imageWidth <= 0 || imageHeight <= 0) {
            return
        }

        // Calculate scaling factors
        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val scaleX = viewWidth / imageWidth
        val scaleY = viewHeight / imageHeight
        val scale = minOf(scaleX, scaleY)

        // Calculate offset to center the image
        val offsetX = (viewWidth - imageWidth * scale) / 2
        val offsetY = (viewHeight - imageHeight * scale) / 2

        for ((x, y) in starPoints) {
            // Scale and position the star point to match the image
            val scaledX = x * scale + offsetX
            val scaledY = y * scale + offsetY
            canvas.drawCircle(scaledX, scaledY, 8f, paint)
        }
    }
}
