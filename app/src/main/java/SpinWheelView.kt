package com.example.spinwheelapp


import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class SpinWheelView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val paint = Paint()
    private val numSlices = 10
    private val colors = listOf(
        Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA,
        Color.CYAN, Color.GRAY, Color.LTGRAY, Color.DKGRAY, Color.BLACK
    )

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val rectF = RectF(100f, 100f, width - 100f, width - 100f)
        val sweepAngle = 360f / numSlices

        for (i in 0 until numSlices) {
            paint.color = colors[i % colors.size]
            canvas.drawArc(rectF, i * sweepAngle, sweepAngle, true, paint)
        }
    }
}
