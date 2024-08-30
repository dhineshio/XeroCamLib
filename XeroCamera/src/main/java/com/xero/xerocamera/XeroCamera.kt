package com.xero.xerocamera

import android.content.Context
import android.util.Log
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.FloatRange
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.xero.xerocamera.Camera.CameraModule.CameraFunctionality
import com.xero.xerocamera.Camera.CameraModule.CameraInitializer
import com.xero.xerocamera.Camera.CameraModule.FlashMode
import com.xero.xerocamera.Camera.CameraModule.FocusManager
import com.xero.xerocamera.Camera.CameraModule.PhotoCapture
import com.xero.xerocamera.Camera.Models.CameraCore
import com.xero.xerocamera.Scanner.ScannerModule.ScannerOverlay
import com.xero.xerocamera.Scanner.ScannerModule.ScannerViewState
import com.xero.xerocamera.Utility.Utility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class XeroCamera private constructor(
  private var context: Context,
  private var owner: LifecycleOwner,
  private var cameraCore: CameraCore,
) : CameraFunctionality, DefaultLifecycleObserver, CameraInitializer.ScannerCallback {
  private lateinit var utility: Utility
  private lateinit var cameraInitializer: CameraInitializer
  private lateinit var photoCapture: PhotoCapture
  private val imageCapture = ImageCapture.Builder().build()

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
	cameraInitializer = CameraInitializer(context, owner, cameraCore, imageCapture, utility)
	cameraInitializer.setScannerCallback(this)
	photoCapture = PhotoCapture(context, imageCapture) { utility.captureSound() }
	cameraInitializer.initializeCamera()
  }

  override fun switchLensFacing(lensFacing: Int) {
	if(lensFacing == CameraSelector.LENS_FACING_FRONT){
	  enableScanner(false)
	}
	updateCore { it.copy(lensFacing = lensFacing) }
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

  fun resetScanning(){
	_scannerBarcode.postValue(null)
	cameraInitializer.resetScanning()
  }

  override fun setFlashMode(flashMode: FlashMode) {
	cameraInitializer.setFlashMode(flashMode)
  }

  override fun setZoomRatio(@FloatRange(from = 0.0, to = 4.0) zoomRatio: Float) {
	cameraInitializer.setZoom(zoomRatio)
  }







  override fun takePhoto(
	onSuccess: (imagePath: String) -> Unit,
	onFailure: (exception: Exception) -> Unit
  ) {
	photoCapture.takePhoto(onSuccess, onFailure)
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
	private var cameraCore: CameraCore = CameraCore()

	override fun setContext(context: Context) =
	  apply { this.context = context }

	override fun setCameraPreview(cameraPreview: PreviewView) =
	  apply { cameraCore.cameraPreview = cameraPreview }

	override fun setLifecycleOwner(owner: LifecycleOwner) =
	  apply { this.owner = owner }

	override fun enableScanner(isScanner: Boolean) = apply {
	  cameraCore.isScanner = isScanner
	}

	fun build(): XeroCamera {
	  requireNotNull(context) { "Context must be set" }
	  requireNotNull(owner) { "LifeCycle owner must be set" }
	  requireNotNull(cameraCore.cameraPreview) { "Camera Preview must be set" }
	  return XeroCamera(context!!, owner!!, cameraCore)
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

