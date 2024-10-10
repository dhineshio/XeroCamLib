package com.xero.xerocamera.Camera.CameraModule

import android.content.Context
import android.util.Log
import android.util.Size
import android.view.Surface
import android.widget.Toast
import androidx.annotation.FloatRange
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.xero.xerocamera.Camera.Models.CameraCore
import com.xero.xerocamera.Scanner.ScannerModule.ScannerAnalyzer
import com.xero.xerocamera.Scanner.ScannerModule.ScannerViewState
import com.xero.xerocamera.Utility.Utility
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraInitializer(
  private val context: Context,
  private val owner: LifecycleOwner,
  private val cameraCore: CameraCore,
  private var imageCapture: ImageCapture,
  private var videoCapture: VideoCapture<Recorder>,
  private val utility: Utility
) {
  interface ScannerCallback {
    fun onScannerStateChanged(barcode: String)
  }

  private lateinit var cameraProvider: ProcessCameraProvider
  private val cameraExecutor: ExecutorService by lazy {
    Executors.newSingleThreadExecutor()
  }
  private lateinit var camera: Camera
  private var scannerCallback: ScannerCallback? = null
  private lateinit var scannerAnalyzer: ScannerAnalyzer
  private lateinit var focusManager: FocusManager

  fun initializeCamera() {
    ProcessCameraProvider.getInstance(context).also { it ->
      it.addListener({
        cameraProvider = it.get()
        try {
          bindCamera()
        } catch (e: Exception) {
          Log.e("Xero Builder", "Use Case binding failed $e")
        }
      }, ContextCompat.getMainExecutor(context))
    }
  }

  fun resetScanning() {
    if (::scannerAnalyzer.isInitialized) {
      scannerAnalyzer.isScanned(false)
    }
  }

  fun setScannerCallback(callback: ScannerCallback) {
    this.scannerCallback = callback
  }

  fun setFlashMode(flashMode: FlashMode) {
    when (flashMode) {
      is FlashMode.FlashOn -> {
        camera.cameraControl.enableTorch(false)
        imageCapture.flashMode = ImageCapture.FLASH_MODE_ON
      }

      is FlashMode.FlashOff -> {
        camera.cameraControl.enableTorch(false)
        imageCapture.flashMode = ImageCapture.FLASH_MODE_OFF
      }

      is FlashMode.FlashAuto -> {
        camera.cameraControl.enableTorch(false)
        imageCapture.flashMode = ImageCapture.FLASH_MODE_AUTO
      }

      is FlashMode.TorchMode -> {
        camera.cameraControl.enableTorch(true)
        imageCapture.flashMode = ImageCapture.FLASH_MODE_OFF
      }
    }
  }

  fun setZoom(@FloatRange(from = 0.0, to = 4.0) zoomRatio: Float) {
    camera.cameraControl.setZoomRatio(zoomRatio)
  }

  private fun bindCamera() {
    cameraProvider.unbindAll()
    val useCases = mutableListOf<UseCase>()
    useCases.add(imageCapture)
    useCases.add(videoCapture)
    if (cameraCore.isScanner!!) {
      scannerAnalyzer = ScannerAnalyzer(cameraCore.scannerOverlay!!, cameraCore.isLiveScan!!) { state, barcode ->
        when (state) {
          is ScannerViewState.Success -> scannerCallback?.onScannerStateChanged(barcode)
          is ScannerViewState.Failure -> {
            resetScanning()
            Toast.makeText(context, "Scanning Failed", Toast.LENGTH_SHORT).show()
          }
        }
      }
      useCases.add(getImageAnalysis(cameraExecutor, scannerAnalyzer))
    }
    camera = cameraProvider.bindToLifecycle(
      owner,
      bindCameraSelector(),
      bindPreview(),
      *useCases.toTypedArray()
    )
    focusManager = FocusManager(cameraCore, camera, utility)
  }

  private fun bindCameraSelector(): CameraSelector {
    return CameraSelector.Builder().also {
      it.requireLensFacing(cameraCore.lensFacing)
    }.build()
  }

  private fun bindPreview(): Preview {
    return Preview.Builder().build().also {
      it.setSurfaceProvider(cameraCore.cameraPreview!!.surfaceProvider)
    }
  }

  private fun getImageAnalysis(
    cameraExecutor: ExecutorService?,
    scannerAnalyzer: ScannerAnalyzer
  ): ImageAnalysis {
    return ImageAnalysis.Builder()
      .setTargetResolution(Size(720, 1280))
      .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
      .setTargetRotation(Surface.ROTATION_0)
      .build().also {
        it.setAnalyzer(cameraExecutor!!, scannerAnalyzer)
      }
  }


  fun shutdownCamera() {
    cameraProvider.unbindAll()
  }
}