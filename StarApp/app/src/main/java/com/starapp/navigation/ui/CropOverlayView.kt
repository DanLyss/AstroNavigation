package com.starapp.navigation.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.min

/**
 * Custom view for displaying a crop rectangle overlay on an image
 * The crop is always symmetric around the center of the image
 */
class CropOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paint for drawing the crop rectangle
    private val paint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f // Increased stroke width for better visibility
    }

    // Paint for drawing the semi-transparent overlay outside the crop area
    private val overlayPaint = Paint().apply {
        color = Color.parseColor("#80000000") // Semi-transparent black
        style = Paint.Style.FILL
    }

    // Image dimensions (needed for scaling)
    var imageWidth: Int = 0
    var imageHeight: Int = 0

    // Crop rectangle size as a percentage of the smaller dimension (width or height)
    // Default to 80% of the smaller dimension
    private var cropSizePercent: Float = 0.8f

    // Rectangle for the crop area
    private val cropRect = RectF()

    // Scaling and positioning variables
    private var scaledImageWidth: Float = 0f
    private var scaledImageHeight: Float = 0f
    private var offsetX: Float = 0f
    private var offsetY: Float = 0f

    // Last touch position for handling resize gestures
    private var lastTouchX: Float = 0f
    private var lastTouchY: Float = 0f
    private var isResizing: Boolean = false

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Skip drawing if no image dimensions
        if (imageWidth <= 0 || imageHeight <= 0) {
            return
        }

        // Calculate the scaling and positioning for fitCenter behavior
        calculateScalingAndPositioning()

        // Calculate the crop rectangle
        updateCropRect()

        // Draw the semi-transparent overlay outside the crop area
        // Top
        canvas.drawRect(offsetX, offsetY, offsetX + scaledImageWidth, cropRect.top, overlayPaint)
        // Bottom
        canvas.drawRect(offsetX, cropRect.bottom, offsetX + scaledImageWidth, offsetY + scaledImageHeight, overlayPaint)
        // Left
        canvas.drawRect(offsetX, cropRect.top, cropRect.left, cropRect.bottom, overlayPaint)
        // Right
        canvas.drawRect(cropRect.right, cropRect.top, offsetX + scaledImageWidth, cropRect.bottom, overlayPaint)

        // Draw the crop rectangle
        canvas.drawRect(cropRect, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                isResizing = isNearCropRectEdge(event.x, event.y)
                return isResizing
            }
            MotionEvent.ACTION_MOVE -> {
                if (isResizing) {
                    // Calculate the distance moved
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY

                    // Determine the direction of the resize
                    // For symmetric cropping, we'll adjust the cropSizePercent based on the movement
                    val distance = (dx + dy) / 2 // Average of horizontal and vertical movement

                    // Adjust crop size percentage based on movement
                    // Moving outward (positive distance) increases the crop size
                    // Moving inward (negative distance) decreases the crop size
                    cropSizePercent = (cropSizePercent + distance / min(scaledImageWidth, scaledImageHeight) * 0.5f)
                        .coerceIn(0.1f, 0.95f) // Limit to reasonable values

                    lastTouchX = event.x
                    lastTouchY = event.y
                    invalidate()
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isResizing = false
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * Check if the touch point is near the edge of the crop rectangle
     */
    private fun isNearCropRectEdge(x: Float, y: Float): Boolean {
        val edgeThreshold = 50f // Pixels
        return (x >= cropRect.left - edgeThreshold && x <= cropRect.left + edgeThreshold ||
                x >= cropRect.right - edgeThreshold && x <= cropRect.right + edgeThreshold ||
                y >= cropRect.top - edgeThreshold && y <= cropRect.top + edgeThreshold ||
                y >= cropRect.bottom - edgeThreshold && y <= cropRect.bottom + edgeThreshold)
    }

    /**
     * Calculate the scaling and positioning for fitCenter behavior
     */
    private fun calculateScalingAndPositioning() {
        val imageAspectRatio = imageWidth.toFloat() / imageHeight
        val viewAspectRatio = width.toFloat() / height

        if (imageAspectRatio > viewAspectRatio) {
            // Image is wider than view (relative to their heights)
            scaledImageWidth = width.toFloat()
            scaledImageHeight = scaledImageWidth / imageAspectRatio
            offsetX = 0f
            offsetY = (height - scaledImageHeight) / 2
        } else {
            // Image is taller than view (relative to their widths)
            scaledImageHeight = height.toFloat()
            scaledImageWidth = scaledImageHeight * imageAspectRatio
            offsetX = (width - scaledImageWidth) / 2
            offsetY = 0f
        }
    }

    /**
     * Update the crop rectangle based on current settings
     */
    private fun updateCropRect() {
        // Calculate the crop size based on the smaller dimension
        val smallerDimension = min(scaledImageWidth, scaledImageHeight)
        val cropSize = smallerDimension * cropSizePercent

        // Calculate the center of the displayed image
        val centerX = offsetX + scaledImageWidth / 2
        val centerY = offsetY + scaledImageHeight / 2

        // Set the crop rectangle centered on the image
        cropRect.left = centerX - cropSize / 2
        cropRect.top = centerY - cropSize / 2
        cropRect.right = centerX + cropSize / 2
        cropRect.bottom = centerY + cropSize / 2
    }

    /**
     * Get the crop rectangle in image coordinates (0-1 range)
     */
    fun getCropRectInImageCoordinates(): RectF {
        val imageRect = RectF()

        // Convert from view coordinates to image coordinates (0-1 range)
        imageRect.left = (cropRect.left - offsetX) / scaledImageWidth
        imageRect.top = (cropRect.top - offsetY) / scaledImageHeight
        imageRect.right = (cropRect.right - offsetX) / scaledImageWidth
        imageRect.bottom = (cropRect.bottom - offsetY) / scaledImageHeight

        return imageRect
    }
}
