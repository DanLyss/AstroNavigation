package com.starapp.navigation.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

/**
 * Custom view for overlaying star points on an image
 */
class StarOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paint for drawing star points
    private val paint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    // List of star points (x, y coordinates)
    var starPoints: List<Pair<Float, Float>> = emptyList()

    // Image dimensions (needed for scaling)
    var imageWidth: Int = 0
    var imageHeight: Int = 0

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Skip drawing if no image dimensions or star points
        if (imageWidth <= 0 || imageHeight <= 0 || starPoints.isEmpty()) {
            return
        }

        // Calculate the scaling and positioning for fitCenter behavior
        val imageAspectRatio = imageWidth.toFloat() / imageHeight
        val viewAspectRatio = width.toFloat() / height

        var scaledImageWidth: Float
        var scaledImageHeight: Float
        var offsetX = 0f
        var offsetY = 0f

        if (imageAspectRatio > viewAspectRatio) {
            // Image is wider than view (relative to their heights)
            // Width will be constrained to view width
            scaledImageWidth = width.toFloat()
            scaledImageHeight = scaledImageWidth / imageAspectRatio
            // Center vertically
            offsetY = (height - scaledImageHeight) / 2
        } else {
            // Image is taller than view (relative to their widths)
            // Height will be constrained to view height
            scaledImageHeight = height.toFloat()
            scaledImageWidth = scaledImageHeight * imageAspectRatio
            // Center horizontally
            offsetX = (width - scaledImageWidth) / 2
        }

        // Calculate scaling factors based on the actual displayed image size
        val scaleX = scaledImageWidth / imageWidth
        val scaleY = scaledImageHeight / imageHeight

        // Draw each star point as a circle
        for ((x, y) in starPoints) {
            // Scale coordinates to match the actual displayed image size and position
            val scaledX = x * scaleX + offsetX
            val scaledY = y * scaleY + offsetY

            // Draw a circle at the star position
            canvas.drawCircle(scaledX, scaledY, 10f, paint)

            // Draw crosshairs
            canvas.drawLine(scaledX - 15, scaledY, scaledX + 15, scaledY, paint)
            canvas.drawLine(scaledX, scaledY - 15, scaledX, scaledY + 15, paint)
        }
    }
}
