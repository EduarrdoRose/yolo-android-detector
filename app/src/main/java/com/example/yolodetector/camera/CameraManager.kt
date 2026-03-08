package com.example.yolodetector.camera

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.yolodetector.util.ImageUtils
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class CameraManager(private val context: Context) {

    private var analysisExecutor: ExecutorService? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private val isAnalyzing = AtomicBoolean(false)

    fun startCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        onFrame: (bitmap: Bitmap) -> Unit
    ) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        analysisExecutor = Executors.newSingleThreadExecutor()

        providerFuture.addListener({
            cameraProvider = providerFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            imageAnalysis.setAnalyzer(analysisExecutor!!) { imageProxy ->
                if (!isAnalyzing.compareAndSet(false, true)) {
                    imageProxy.close()
                    return@setAnalyzer
                }

                try {
                    val bitmap = ImageUtils.imageProxyToBitmap(imageProxy)
                    val rotated = ImageUtils.rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)
                    onFrame(rotated)
                } finally {
                    isAnalyzing.set(false)
                    imageProxy.close()
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider?.unbindAll()
            cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                preview,
                imageAnalysis
            )
        }, ContextCompat.getMainExecutor(context))
    }

    fun stopCamera() {
        cameraProvider?.unbindAll()
        analysisExecutor?.shutdown()
        analysisExecutor = null
    }
}
