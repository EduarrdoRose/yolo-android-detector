package com.example.yolodetector.detection

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import com.example.yolodetector.camera.FramePreprocessor
import com.example.yolodetector.util.Constants
import java.nio.FloatBuffer

class YoloDetector(private val context: Context) {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession
    private val inputName: String
    private val labels: List<String>
    private val preprocessor = FramePreprocessor(Constants.INPUT_SIZE)
    private val postProcessor: YoloPostProcessor

    init {
        val modelBytes = context.assets.open(Constants.MODEL_PATH).use { it.readBytes() }
        val options = OrtSession.SessionOptions().apply {
            setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT)
            setIntraOpNumThreads(2)
        }
        session = env.createSession(modelBytes, options)
        inputName = session.inputNames.first()

        labels = context.assets.open(Constants.LABELS_PATH).bufferedReader().useLines { lines ->
            lines.filter { it.isNotBlank() }.toList()
        }
        postProcessor = YoloPostProcessor(labels)
    }

    fun detect(bitmap: Bitmap): List<DetectionResult> {
        val prep = preprocessor.preprocess(bitmap)
        val shape = longArrayOf(1, 3, Constants.INPUT_SIZE.toLong(), Constants.INPUT_SIZE.toLong())

        val tensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(prep.inputTensor), shape)
        tensor.use { inputTensor ->
            session.run(mapOf(inputName to inputTensor)).use { results ->
                @Suppress("UNCHECKED_CAST")
                val output = results[0].value as Array<Array<FloatArray>>
                return postProcessor.decode(output, prep)
            }
        }
    }

    fun close() {
        session.close()
        env.close()
    }
}
