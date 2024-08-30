package com.xero.xerocamera.Camera.CameraModule

import android.content.Context
import android.view.View
import androidx.annotation.FloatRange
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.xero.xerocamera.Scanner.ScannerModule.ScannerViewState
import com.xero.xerocamera.XeroCamera

sealed class FlashMode {
  data object FlashOn : FlashMode()
  data object FlashOff : FlashMode()
  data object FlashAuto : FlashMode()
  data object TorchMode : FlashMode()
}

sealed class LensFacing{
  data object FrontFacing : LensFacing()
  data object BackFacing : LensFacing()
}

interface CameraFunctionality {
  fun startCamera()
  fun switchLensFacing(lensFacing: Int)
  fun takePhoto(
	onSuccess: (imagePath: String) -> Unit,
	onFailure: (exception: Exception) -> Unit
  )
  fun setFlashMode(flashMode : FlashMode)
  fun setZoomRatio(@FloatRange(from = 0.0 , to = 4.0 ) zoomRatio : Float )
  fun enableScanner(isScanner : Boolean)

  interface CompileTimeFunctionality{
	fun setContext(context : Context) : XeroCamera.Builder
	fun setCameraPreview(cameraPreview: PreviewView) : XeroCamera.Builder
	fun setLifecycleOwner(owner : LifecycleOwner) : XeroCamera.Builder
	fun enableScanner(isScanner: Boolean) : XeroCamera.Builder
  }
}