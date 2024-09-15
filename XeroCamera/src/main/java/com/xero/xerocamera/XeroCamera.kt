package com.xero.xerocamera

import android.app.Activity
import android.content.Context
import android.util.Log
import android.util.Size
import android.view.HapticFeedbackConstants
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.FloatRange
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.view.PreviewView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.xero.xerocamera.Camera.CameraModule.CameraFunctionality
import com.xero.xerocamera.Camera.CameraModule.CameraInitializer
import com.xero.xerocamera.Camera.CameraModule.FlashMode
import com.xero.xerocamera.Camera.CameraModule.PhotoCapture
import com.xero.xerocamera.Camera.CameraModule.VideoCapture
import com.xero.xerocamera.Camera.Models.CameraCore
import com.xero.xerocamera.Scanner.ScannerModule.ScannerOverlay
import com.xero.xerocamera.Utility.Utility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class XeroCamera private constructor(
  private var context: Context,
  private var owner: LifecycleOwner,
  private var activity: Activity,
  private var cameraCore: CameraCore,
) : CameraFunctionality, DefaultLifecycleObserver, CameraInitializer.ScannerCallback {
  private lateinit var utility: Utility
  private lateinit var cameraInitializer: CameraInitializer
  private lateinit var photoCapture: PhotoCapture
  private lateinit var videoCapture: VideoCapture
  private val imageCapture = ImageCapture.Builder().setTargetResolution(getDesiredResolution()).build()
  private val recordVideo = androidx.camera.video.VideoCapture.withOutput(Recorder.Builder().also {
	it.setQualitySelector(
	  QualitySelector.from(
		Quality.UHD,
		FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
	  )
	)
	it.setAspectRatio(AspectRatio.RATIO_16_9)
	it.setTargetVideoEncodingBitRate(5000000)
  }.build())

  private val _scannerBarcode = MutableLiveData<String>()
  val scannerBarcode: LiveData<String> get() = _scannerBarcode

  init {
	owner.lifecycle.addObserver(this)
	if (cameraCore.isScanner!!) {
	  createScannerOverlay()
	} else {
	  endScannerOverlay()
	}
  }

  override fun startCamera() {
	utility = Utility(cameraCore)
	cameraInitializer =
	  CameraInitializer(context, owner, cameraCore, imageCapture, recordVideo, utility)
	cameraInitializer.setScannerCallback(this)
	photoCapture = PhotoCapture(context, imageCapture) { utility.captureSound() }
	videoCapture = VideoCapture(context, recordVideo)
	cameraInitializer.initializeCamera()
  }

  override fun switchLensFacing(lensFacing: Int) {
	if (lensFacing == CameraSelector.LENS_FACING_FRONT) {
	  enableScanner(false)
	}
	updateCore { it.copy(lensFacing = lensFacing) }
  }

  override fun startVideo(
	onStart: (() -> Unit?)?,
	onSuccess: ((videoPath: String) -> Unit?)?,
	onError: (() -> Unit?)?,
	timerView: TextView,
	directoryName: String?,
	fileName: String?,
	subDirectoryName: String?
  ) {
	videoCapture.startVideo(
	  directoryName!!,
	  fileName!!,
	  subDirectoryName!!,
		timerView,
	  onStart,
	  onSuccess,
	  onError
	)
  }

  override fun stopVideo() {
	videoCapture.stopVideo()
  }

  override fun pauseVideo() {
	videoCapture.pauseVideo()
  }

  override fun resumeVideo() {
	videoCapture.resumeVideo()
  }

  override fun photoCapture(
	onSuccess: ((imagePath: String) -> Unit)?,
	onFailure: ((exception: Exception) -> Unit)?,
	enableSound: Boolean?,
	directoryName: String?,
	fileName: String?,
	subDirectoryName: String?
  ) {
		videoCapture.capturePhoto {
			photoCapture.takePhoto(onSuccess, onFailure, enableSound!!,false, directoryName!!, fileName!!, subDirectoryName!!)
		}
  }

  override fun takePhoto(
	onSuccess: ((imagePath: String) -> Unit)?,
	onFailure: ((exception: Exception) -> Unit)?,
	enableSound : Boolean?,
	useCache: Boolean?,
	directoryName: String?,
	fileName: String?,
	subDirectoryName: String?
  ) {
	photoCapture.takePhoto(
	  onSuccess,
	  onFailure,
		enableSound!!,
	  useCache!!,
	  directoryName!!,
	  fileName!!,
	  subDirectoryName!!
	)
  }

  override fun enableScanner(isScanner: Boolean) {
	if (isScanner != cameraCore.isScanner) {
	  switchLensFacing(CameraSelector.LENS_FACING_BACK)
	  updateCore { it.copy(isScanner = isScanner) }
	}
	owner.lifecycleScope.launch {
	  withContext(Dispatchers.Main) {
		if (isScanner && cameraCore.scannerOverlay == null) {
		  createScannerOverlay()
		  cameraCore.scannerOverlay!!.alpha = 0f
		  cameraCore.scannerOverlay!!.animate()
			.setDuration(500)
			.alpha(1f)
			.start()
		} else if (!isScanner && cameraCore.scannerOverlay != null) {
		  cameraCore.scannerOverlay!!.alpha = 1f
		  cameraCore.scannerOverlay!!.animate()
			.setDuration(500)
			.alpha(0f)
			.start()
		  endScannerOverlay()
		}
	  }
	}
  }

  override fun resetScanning() {
	_scannerBarcode.postValue(null)
	cameraInitializer.resetScanning()
  }

  override fun setFlashMode(flashMode: FlashMode) {
	cameraInitializer.setFlashMode(flashMode)
  }

  override fun setZoomRatio(@FloatRange(from = 0.0, to = 4.0) zoomRatio: Float) {
	cameraInitializer.setZoom(zoomRatio)
  }

  override fun setSeekBarZoom(
	seekBar: SeekBar,
	onStart: (() -> Unit)?,
	onStop: (() -> Unit)?
  ) {
	seekBar.max = 100
	seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
	  override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
		val minValue = 0.0f
		val maxValue = 4.0f
		val currentValue = minValue + (progress / 100.0f) * (maxValue - minValue)
		setZoomRatio(currentValue)
	  }

	  override fun onStartTrackingTouch(seekBar: SeekBar?) {
		onStart?.invoke()
	  }

	  override fun onStopTrackingTouch(seekBar: SeekBar?) {
		onStop?.invoke()
	  }
	})
  }

  private fun getDesiredResolution(): Size {
	val ratio = 1.1f
	val displayMetric = activity.resources.displayMetrics
	val screenWidthDp = displayMetric.widthPixels / displayMetric.density
	val screenHeightDp = displayMetric.heightPixels / displayMetric.density
	val desireWidthDp = screenWidthDp * ratio
	val desiredHeightDp = screenHeightDp * ratio
	val desiredWidthPx = (desireWidthDp * displayMetric.density).toInt()
	val desiredHeightPx = (desiredHeightDp * displayMetric.density).toInt()
	return Size(desiredWidthPx, desiredHeightPx)
  }

  private fun createScannerOverlay() {
	cameraCore.scannerOverlay = ScannerOverlay(
	  cameraCore.cameraPreview,
	  HapticFeedbackConstants.KEYBOARD_TAP,
	  context
	).apply {
	  layoutParams = FrameLayout.LayoutParams(
		FrameLayout.LayoutParams.MATCH_PARENT,
		FrameLayout.LayoutParams.MATCH_PARENT
	  )
	}
	(cameraCore.cameraPreview!!.parent as? ViewGroup)?.addView(cameraCore.scannerOverlay)
  }

  private fun endScannerOverlay() {
	(cameraCore.cameraPreview!!.parent as? ViewGroup)?.removeView(cameraCore.scannerOverlay)
	cameraCore.scannerOverlay = null
  }

  private fun updateCore(update: (CameraCore) -> CameraCore) {
	cameraCore = update(cameraCore)
	startCamera()
  }

  class Builder : CameraFunctionality.CompileTimeFunctionality {
	private var context: Context? = null
	private var owner: LifecycleOwner? = null
	private var activity: Activity? = null
	private var cameraCore: CameraCore = CameraCore()

	override fun setContext(context: Context) =
	  apply { this.context = context }

	fun setActivity(activity: Activity) = apply {
	  this.activity = activity
	}

	override fun setCameraPreview(cameraPreview: PreviewView) =
	  apply { cameraCore.cameraPreview = cameraPreview }

	override fun setLifecycleOwner(owner: LifecycleOwner) =
	  apply { this.owner = owner }

	override fun enableScanner(isScanner: Boolean) = apply {
	  cameraCore.isScanner = isScanner
	}

	override fun setLensFacing(lensFacing: Int) = apply {
	  cameraCore.lensFacing = lensFacing
	}

	fun build(): XeroCamera {
	  requireNotNull(context) { "Context must be set" }
	  requireNotNull(owner) { "LifeCycle owner must be set" }
	  requireNotNull(activity) { "Activity must be set" }
	  requireNotNull(cameraCore.cameraPreview) { "Camera Preview must be set" }
	  return XeroCamera(context!!, owner!!, activity!!, cameraCore)
	}
  }

  override fun onScannerStateChanged(barcode: String) {
	_scannerBarcode.postValue(barcode)
  }

  override fun onResume(owner: LifecycleOwner) {
	super.onResume(owner)
	Log.e("XeroCamera", "OnResume")
	if (::cameraInitializer.isInitialized) {
	  startCamera()
	}
  }

  override fun onPause(owner: LifecycleOwner) {
	super.onPause(owner)
	Log.e("XeroCamera", "OnPause")
	if (::cameraInitializer.isInitialized) {
	  cameraInitializer.shutdownCamera()
	}
  }

  override fun onDestroy(owner: LifecycleOwner) {
	super.onDestroy(owner)
	Log.e("XeroCamera", "OnDestroy")
	owner.lifecycle.removeObserver(this)
  }

  companion object {
	fun builder() = Builder()
  }
}

