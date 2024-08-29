package com.xero.xerocamera.Camera.CameraModule

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.annotation.FloatRange
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
import com.xero.xerocamera.Camera.Models.CameraCore
import com.xero.xerocamera.Scanner.ScannerModule.ScannerAnalyzer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraInitializer(
  private val context: Context,
  private val owner: LifecycleOwner,
  private val cameraCore: CameraCore,
) {
  private lateinit var cameraProvider: ProcessCameraProvider
  private val cameraExecutor: ExecutorService by lazy {
	Executors.newSingleThreadExecutor()
  }

  @SuppressLint("RestrictedApi")
  fun initializeCamera() {
	ProcessCameraProvider.getInstance(context).also { it ->
	  it.addListener({
		cameraProvider = it.get()
		try {
		  shutdownCamera()
		  cameraCore.imageCapture = ImageCapture.Builder().build()
		  cameraCore.videoCapture = VideoCapture.withOutput(
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
		  cameraCore.cameraSelector = CameraSelector.Builder().also {
			it.requireLensFacing(
			  when (cameraCore.lensFacing1) {
				is LensFacing.BackFacing -> CameraSelector.LENS_FACING_BACK
				is LensFacing.FrontFacing -> CameraSelector.LENS_FACING_FRONT
			  }
			)
		  }.build()
		  bindCamera(
			cameraCore.imageCapture,
			cameraCore.videoCapture,
			if (cameraCore.isScanner!!) {
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

  fun setFlashMode(flashMode: FlashMode) {
	when (flashMode) {
	  is FlashMode.FlashOn -> {
		cameraCore.camera!!.cameraControl.enableTorch(false)
		cameraCore.imageCapture?.flashMode = ImageCapture.FLASH_MODE_ON
	  }

	  is FlashMode.FlashOff -> {
		cameraCore.camera!!.cameraControl.enableTorch(false)
		cameraCore.imageCapture?.flashMode = ImageCapture.FLASH_MODE_OFF
	  }

	  is FlashMode.FlashAuto -> {
		cameraCore.camera!!.cameraControl.enableTorch(false)
		cameraCore.imageCapture?.flashMode = ImageCapture.FLASH_MODE_OFF
	  }

	  is FlashMode.TorchMode -> {
		cameraCore.camera!!.cameraControl.enableTorch(true)
		cameraCore.imageCapture?.flashMode = ImageCapture.FLASH_MODE_OFF
	  }
	}
  }

  fun setZoom(@FloatRange(from = 0.0, to = 4.0) zoomRatio: Float) {
	cameraCore.camera!!.cameraControl.setZoomRatio(zoomRatio)
  }

  private fun bindCamera(vararg useCase: UseCase?) {
	val nonNullUseCases = useCase.filterNotNull()
	cameraCore.camera = cameraProvider.bindToLifecycle(
	  owner,
	  cameraCore.cameraSelector!!,
	  bindPreview(),
	  *nonNullUseCases.toTypedArray()
	)
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
	cameraCore.imageCapture = null
	cameraCore.cameraSelector = null
	cameraCore.videoCapture = null
  }
}