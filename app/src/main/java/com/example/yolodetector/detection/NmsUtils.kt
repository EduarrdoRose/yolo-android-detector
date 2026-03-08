package com.example.yolodetector.detection

object NmsUtils {
    fun nms(detections: List<DetectionResult>, iouThreshold: Float, maxDetections: Int): List<DetectionResult> {
        if (detections.isEmpty()) return emptyList()

        val sorted = detections.sortedByDescending { it.confidence }.toMutableList()
        val selected = mutableListOf<DetectionResult>()

        while (sorted.isNotEmpty() && selected.size < maxDetections) {
            val candidate = sorted.removeAt(0)
            selected.add(candidate)

            val iterator = sorted.iterator()
            while (iterator.hasNext()) {
                val other = iterator.next()
                if (iou(candidate, other) > iouThreshold) {
                    iterator.remove()
                }
            }
        }

        return selected
    }

    private fun iou(a: DetectionResult, b: DetectionResult): Float {
        val intersectionLeft = maxOf(a.left, b.left)
        val intersectionTop = maxOf(a.top, b.top)
        val intersectionRight = minOf(a.right, b.right)
        val intersectionBottom = minOf(a.bottom, b.bottom)

        val intersectionW = maxOf(0f, intersectionRight - intersectionLeft)
        val intersectionH = maxOf(0f, intersectionBottom - intersectionTop)
        val intersectionArea = intersectionW * intersectionH

        if (intersectionArea <= 0f) return 0f

        val areaA = (a.right - a.left) * (a.bottom - a.top)
        val areaB = (b.right - b.left) * (b.bottom - b.top)
        val union = areaA + areaB - intersectionArea

        return if (union <= 0f) 0f else intersectionArea / union
    }
}
