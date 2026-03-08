package com.example.yolodetector.detection

import com.example.yolodetector.camera.FramePreprocessor
import com.example.yolodetector.util.Constants

class YoloPostProcessor(
    private val labels: List<String>,
    private val confidenceThreshold: Float = Constants.CONFIDENCE_THRESHOLD,
    private val iouThreshold: Float = Constants.NMS_IOU_THRESHOLD,
    private val maxDetections: Int = Constants.MAX_DETECTIONS
) {

    fun decode(
        output: Array<Array<FloatArray>>,
        preprocessResult: FramePreprocessor.PreprocessResult
    ): List<DetectionResult> {
        val channels = output[0]
        val numPredictions = channels[0].size
        val rawDetections = mutableListOf<DetectionResult>()

        for (i in 0 until numPredictions) {
            val cx = channels[0][i]
            val cy = channels[1][i]
            val w = channels[2][i]
            val h = channels[3][i]

            var bestClass = -1
            var bestScore = 0f

            for (c in 4 until channels.size) {
                val score = channels[c][i]
                if (score > bestScore) {
                    bestScore = score
                    bestClass = c - 4
                }
            }

            if (bestClass == -1 || bestScore < confidenceThreshold) continue

            val x1 = cx - w / 2f
            val y1 = cy - h / 2f
            val x2 = cx + w / 2f
            val y2 = cy + h / 2f

            val mapped = mapToSource(x1, y1, x2, y2, preprocessResult)
            if (mapped.right <= mapped.left || mapped.bottom <= mapped.top) continue

            rawDetections += DetectionResult(
                classId = bestClass,
                className = labels.getOrElse(bestClass) { "class_$bestClass" },
                confidence = bestScore,
                left = mapped.left,
                top = mapped.top,
                right = mapped.right,
                bottom = mapped.bottom
            )
        }

        return NmsUtils.nms(rawDetections, iouThreshold, maxDetections)
    }

    private data class MappedBox(val left: Float, val top: Float, val right: Float, val bottom: Float)

    private fun mapToSource(
        x1: Float,
        y1: Float,
        x2: Float,
        y2: Float,
        preprocessResult: FramePreprocessor.PreprocessResult
    ): MappedBox {
        val left = ((x1 - preprocessResult.padX) / preprocessResult.scale).coerceIn(0f, preprocessResult.srcWidth.toFloat())
        val top = ((y1 - preprocessResult.padY) / preprocessResult.scale).coerceIn(0f, preprocessResult.srcHeight.toFloat())
        val right = ((x2 - preprocessResult.padX) / preprocessResult.scale).coerceIn(0f, preprocessResult.srcWidth.toFloat())
        val bottom = ((y2 - preprocessResult.padY) / preprocessResult.scale).coerceIn(0f, preprocessResult.srcHeight.toFloat())

        return MappedBox(left, top, right, bottom)
    }
}
