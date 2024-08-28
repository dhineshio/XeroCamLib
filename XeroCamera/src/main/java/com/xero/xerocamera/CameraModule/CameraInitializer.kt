package com.xero.xerocamera.CameraModule

import android.content.Context
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.xero.xerocamera.Models.CameraConfig
import com.xero.xerocamera.Models.CameraCore
import com.xero.xerocamera.ScannerModule.ScannerAnalyzer
import com.xero.xerocamera.ScannerModule.ScannerOverlay
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraInitializer(
  private val context: Context,
  private val owner: LifecycleOwner,
  private val cameraCore: CameraCore,
  private val cameraConfig: CameraConfig,
) {
  private lateinit var cameraProvider: ProcessCameraProvider
  private val cameraExecutor: ExecutorService by lazy {
    Executors.newSingleThreadExecutor()
  }

  fun initializeCamera() {
    ProcessCameraProvider.getInstance(context).also { it ->
      it.addListener({
        cameraProvider = it.get()
        try {
          shutdownCamera()
          cameraCore.imageCapture = bindImageCapture()
          bindCamera(
            cameraCore.imageCapture!!,
            if (cameraCore.isScanner!!)  {
              val scannerAnalyzer = ScannerAnalyzer(cameraCore.scannerOverlay!!) { state, barcode ->
                Log.e("Scanner", "$state $barcode")
              }
              getImageAnalysis(cameraExecutor, scannerAnalyzer)
            } else {
              null
            }
          )
        } catch (e: Exception) {
          Log.e("Xero Builder", "Use Case binding failed $e")
        }
      }, ContextCompat.getMainExecutor(context))
    }
  }

  private fun bindCamera(vararg useCase: UseCase?) {
    val nonNullUseCases = useCase.filterNotNull()
    cameraCore.camera = cameraProvider.bindToLifecycle(
      owner,
      bindCameraSelector(),
      bindPreview(),
      *nonNullUseCases.toTypedArray()
    )
    setZoom(cameraConfig.zoomRatio)
  }
  private fun bindImageCapture(): ImageCapture {
    return ImageCapture.Builder().setFlashMode(cameraConfig.flashMode)
      .setJpegQuality(cameraConfig.photoQuality)
      .build()
  }

  private fun bindCameraSelector(): CameraSelector {
    return CameraSelector.Builder().also {
      it.requireLensFacing(cameraConfig.lensFacing)
    }.build()
  }

  private fun bindPreview(): Preview {
    return Preview.Builder().build().also {
      it.setSurfaceProvider(cameraCore.cameraPreview!!.surfaceProvider)
    }
  }

  private fun setZoom(zoomRatio: Float) {
    cameraCore.camera!!.cameraControl.setZoomRatio(zoomRatio)
  }

  private fun bindVideoCapture(): VideoCapture<Recorder> {
    return VideoCapture.withOutput(
      Recorder.Builder().also {
        it.setQualitySelector(
          QualitySelector.from(
            Quality.UHD,
            FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
          )
        )
        it.setAspectRatio(AspectRatio.RATIO_16_9)
        it.setTargetVideoEncodingBitRate(5000000)
      }.build()
    )
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
        it.setAnalyzer(cameraExecutor!!,scannerAnalyzer)
      }
  }

  fun shutdownCamera() {
    cameraProvider.unbindAll()
  }

}