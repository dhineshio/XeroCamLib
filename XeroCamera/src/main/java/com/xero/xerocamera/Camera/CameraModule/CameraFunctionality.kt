package com.xero.xerocamera.Camera.CameraModule

import android.content.Context
import android.widget.SeekBar
import androidx.annotation.FloatRange
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.xero.xerocamera.XeroCamera

sealed class FlashMode {
  data object FlashOn : FlashMode()
  data object FlashOff : FlashMode()
  data object FlashAuto : FlashMode()
  data object TorchMode : FlashMode()
}

sealed class LensFacing {
  data object FrontFacing : LensFacing()
  data object BackFacing : LensFacing()
}

interface CameraFunctionality {
  fun startCamera()
  fun switchLensFacing(lensFacing: Int)
  fun takePhoto(
	onSuccess: ((imagePath: String) -> Unit)? = null,
	onFailure: ((exception: Exception) -> Unit)? = null,
	useCache: Boolean? = false,
	directoryName: String? = "Demo",
	fileName: String? = "img",
	subDirectoryName: String? = "Photo",
  )
  fun startVideo(
	onStart: (() -> Unit?)? = null,
	onSuccess: ((videoPath: String) -> Unit?)? = null,
	onError: (() -> Unit?)? = null,
	directoryName: String? = "Demo",
	fileName: String? = "vid",
	subDirectoryName: String? = "Video",
  )
  fun stopVideo()
  fun pauseVideo()
  fun resumeVideo()
  fun photoCapture(
	onSuccess: ((imagePath: String) -> Unit)? = null,
	onFailure: ((exception: Exception) -> Unit)? = null,
	directoryName: String? = "Demo",
	fileName: String? = "img",
	subDirectoryName: String? ="photo"
  )

  fun enableScanner(isScanner: Boolean)
  fun resetScanning()
  fun setFlashMode(flashMode: FlashMode)
  fun setZoomRatio(@FloatRange(from = 0.0, to = 4.0) zoomRatio: Float)
  fun setSeekBarZoom(seekBar: SeekBar, onStart: (() -> Unit)? = null, onStop: (() -> Unit)? = null)

  interface CompileTimeFunctionality {
	fun setContext(context: Context): XeroCamera.Builder
	fun setCameraPreview(cameraPreview: PreviewView): XeroCamera.Builder
	fun setLifecycleOwner(owner: LifecycleOwner): XeroCamera.Builder
	fun enableScanner(isScanner: Boolean): XeroCamera.Builder
	fun setLensFacing(lensFacing: Int): XeroCamera.Builder
  }
}