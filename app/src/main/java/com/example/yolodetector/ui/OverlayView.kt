package com.example.yolodetector.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.example.yolodetector.detection.DetectionResult
import java.util.concurrent.CopyOnWriteArrayList

class OverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val detections = CopyOnWriteArrayList<DetectionResult>()
    private var sourceWidth: Int = 1
    private var sourceHeight: Int = 1

    private val boxPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 36f
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val textBgPaint = Paint().apply {
        color = Color.argb(180, 0, 0, 0)
        style = Paint.Style.FILL
    }

    fun updateDetections(results: List<DetectionResult>, srcWidth: Int, srcHeight: Int) {
        sourceWidth = srcWidth
        sourceHeight = srcHeight
        detections.clear()
        detections.addAll(results)
        postInvalidateOnAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (width == 0 || height == 0 || sourceWidth == 0 || sourceHeight == 0) return

        val scaleX = width.toFloat() / sourceWidth
        val scaleY = height.toFloat() / sourceHeight

        for (detection in detections) {
            val left = detection.left * scaleX
            val top = detection.top * scaleY
            val right = detection.right * scaleX
            val bottom = detection.bottom * scaleY

            val rect = RectF(left, top, right, bottom)
            canvas.drawRect(rect, boxPaint)

            val label = "${detection.className} ${(detection.confidence * 100).toInt()}%"
            val textWidth = textPaint.measureText(label)
            val textHeight = textPaint.textSize + 8f
            val textLeft = left.coerceAtLeast(0f)
            val textTop = (top - textHeight).coerceAtLeast(0f)

            canvas.drawRect(textLeft, textTop, textLeft + textWidth + 16f, textTop + textHeight, textBgPaint)
            canvas.drawText(label, textLeft + 8f, textTop + textPaint.textSize, textPaint)
        }
    }
}
