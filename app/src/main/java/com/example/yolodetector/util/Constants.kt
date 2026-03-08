package com.example.yolodetector.util

object Constants {
    const val MODEL_PATH = "yolov8n.onnx"
    const val LABELS_PATH = "labels.txt"
    const val INPUT_SIZE = 640

    const val CONFIDENCE_THRESHOLD = 0.25f
    const val NMS_IOU_THRESHOLD = 0.45f
    const val MAX_DETECTIONS = 100
}
