package com.example.cameralong

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class StarOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    var starPoints: List<Pair<Float, Float>> = emptyList()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val paint = Paint().apply {
            color = Color.RED
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        for ((x, y) in starPoints) {
            canvas.drawCircle(x, y, 8f, paint)
        }
    }
}
