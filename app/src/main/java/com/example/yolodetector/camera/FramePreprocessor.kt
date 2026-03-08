package com.example.yolodetector.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.example.yolodetector.util.Constants

class FramePreprocessor(private val inputSize: Int = Constants.INPUT_SIZE) {

    data class PreprocessResult(
        val inputTensor: FloatArray,
        val scale: Float,
        val padX: Float,
        val padY: Float,
        val srcWidth: Int,
        val srcHeight: Int
    )

    fun preprocess(bitmap: Bitmap): PreprocessResult {
        val srcWidth = bitmap.width
        val srcHeight = bitmap.height

        val scale = minOf(inputSize.toFloat() / srcWidth, inputSize.toFloat() / srcHeight)
        val resizedW = (srcWidth * scale).toInt()
        val resizedH = (srcHeight * scale).toInt()

        val padX = (inputSize - resizedW) / 2f
        val padY = (inputSize - resizedH) / 2f

        val letterboxed = Bitmap.createBitmap(inputSize, inputSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(letterboxed)
        canvas.drawColor(Color.BLACK)

        val dstLeft = padX
        val dstTop = padY
        val dstRight = padX + resizedW
        val dstBottom = padY + resizedH
        canvas.drawBitmap(bitmap, null, android.graphics.RectF(dstLeft, dstTop, dstRight, dstBottom), Paint())

        val pixels = IntArray(inputSize * inputSize)
        letterboxed.getPixels(pixels, 0, inputSize, 0, 0, inputSize, inputSize)

        val tensor = FloatArray(1 * 3 * inputSize * inputSize)
        var index = 0
        val channelSize = inputSize * inputSize

        for (y in 0 until inputSize) {
            for (x in 0 until inputSize) {
                val pixel = pixels[y * inputSize + x]
                val r = ((pixel shr 16) and 0xFF) / 255f
                val g = ((pixel shr 8) and 0xFF) / 255f
                val b = (pixel and 0xFF) / 255f

                tensor[index] = r
                tensor[index + channelSize] = g
                tensor[index + channelSize * 2] = b
                index++
            }
        }

        letterboxed.recycle()

        return PreprocessResult(
            inputTensor = tensor,
            scale = scale,
            padX = padX,
            padY = padY,
            srcWidth = srcWidth,
            srcHeight = srcHeight
        )
    }
}
