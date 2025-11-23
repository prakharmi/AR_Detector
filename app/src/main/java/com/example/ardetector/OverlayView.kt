package com.example.ardetector

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {
    // array to hold the objects around which we have to draw
    private var boxRects: List<RectF> = emptyList()

    // Function to create the box around the object
    private val boxPaint = Paint().apply {
        color = Color.parseColor("#00FF00")
        style = Paint.Style.STROKE
        strokeWidth = 8f
        isAntiAlias = true
    }

    // Function to create the background of the tick mark
    private val circlePaint = Paint().apply {
        color = Color.parseColor("#00FF00")
        style = Paint.Style.FILL
        isAntiAlias = true
        setShadowLayer(10f, 0f, 0f, Color.BLACK)
    }

    // Function to create the white tick mark
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 50f
        style = Paint.Style.FILL
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    fun setResults(rects: List<RectF>) {
        boxRects = rects
        invalidate() // Trigger redraw
    }

    // Function to draw the boxes and ticks for every object
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // loop through all the detected boxes and draw
        for (rect in boxRects) {
            // draw the box
            canvas.drawRect(rect, boxPaint)

            //draw the tick
            val cx = rect.centerX()
            val cy = rect.centerY()
            canvas.drawCircle(cx, cy, 40f, circlePaint)
            canvas.drawText("✓", cx, cy + 15, textPaint)
        }
    }
}