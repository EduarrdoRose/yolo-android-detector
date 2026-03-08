package com.example.yolodetector

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.yolodetector.camera.CameraManager
import com.example.yolodetector.databinding.ActivityMainBinding
import com.example.yolodetector.detection.YoloDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraManager: CameraManager
    private var detector: YoloDetector? = null
    private val inferenceMutex = Mutex()
    private var detectionJob: Job? = null

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            startPipeline()
        } else {
            Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraManager = CameraManager(this)
        detector = YoloDetector(this)

        ensureCameraPermission()
    }

    override fun onDestroy() {
        super.onDestroy()
        detectionJob?.cancel()
        cameraManager.stopCamera()
        detector?.close()
    }

    private fun ensureCameraPermission() {
        val granted = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

        if (granted) {
            startPipeline()
        } else {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startPipeline() {
        cameraManager.startCamera(this, binding.previewView) { bitmap ->
            runDetection(bitmap)
        }
    }

    private fun runDetection(bitmap: Bitmap) {
        if (detectionJob?.isActive == true) {
            return
        }

        detectionJob = lifecycleScope.launch(Dispatchers.Default) {
            inferenceMutex.withLock {
                val yolo = detector ?: return@withLock
                val results = yolo.detect(bitmap)
                launch(Dispatchers.Main) {
                    binding.overlayView.updateDetections(results, bitmap.width, bitmap.height)
                }
            }
        }
    }
}
