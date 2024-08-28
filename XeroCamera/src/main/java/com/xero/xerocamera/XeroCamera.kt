package com.xero.xerocamera

import android.content.Context
import android.util.Log
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.camera.view.PreviewView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.xero.xerocamera.CameraModule.CameraInitializer
import com.xero.xerocamera.CameraModule.FocusManager
import com.xero.xerocamera.Models.CameraConfig
import com.xero.xerocamera.Models.CameraCore
import com.xero.xerocamera.CameraModule.PhotoCapture
import com.xero.xerocamera.ScannerModule.ScannerOverlay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class XeroCamera private constructor(
  private var context: Context,
  private var owner: LifecycleOwner,
  private var utility: Utility,
  private var cameraConfig: CameraConfig,
  private var cameraCore: CameraCore,
) : DefaultLifecycleObserver {
  private lateinit var cameraInitializer: CameraInitializer
  private lateinit var photoCapture: PhotoCapture
  private lateinit var focusManager: FocusManager
  private var scannerOverlay : ScannerOverlay? = null

  fun startCamera() {
    cameraInitializer = CameraInitializer(context, owner, cameraCore, cameraConfig)
    photoCapture = PhotoCapture(context, cameraCore) { utility.captureSound() }
    focusManager = FocusManager(cameraCore) { utility.focusSound() }
    cameraInitializer.initializeCamera()
  }

  fun switchLensFacing(lensFacing: Int) {
    updateConfig { it.copy(lensFacing = lensFacing) }
    startCamera()
  }

  fun takePhoto(captureButton: View) {
    photoCapture.takePhoto(captureButton)
  }

  fun setZoomRatio(@FloatRange zoomRatio: Float) {
    updateConfig { it.copy(zoomRatio = zoomRatio) }
  }

  fun enableScanner(isScanner: Boolean){
    updateCore { it.copy(isScanner = isScanner) }
    owner.lifecycleScope.launch {
      withContext(Dispatchers.Main){
        delay(200L)
        if (isScanner && scannerOverlay == null) {
          scannerOverlay = ScannerOverlay(context).apply {
            layoutParams = FrameLayout.LayoutParams(
              FrameLayout.LayoutParams.MATCH_PARENT,
              FrameLayout.LayoutParams.MATCH_PARENT
            )
          }
          (cameraCore.cameraPreview!!.parent as? ViewGroup)?.addView(scannerOverlay)
        } else if (!isScanner) {
          delay(200L)
          (cameraCore.cameraPreview!!.parent as? ViewGroup)?.removeView(scannerOverlay)
          scannerOverlay = null
        }
      }
    }
  }

  private fun updateConfig(update: (CameraConfig) -> CameraConfig) {
    cameraConfig = update(cameraConfig)
  }

  private fun updateCore(update : (CameraCore) -> CameraCore){
    cameraCore = update(cameraCore)
    startCamera()
  }

  class Builder {
    private var context : Context? = null
    private var owner: LifecycleOwner? = null
    private var cameraConfig: CameraConfig = CameraConfig()
    private var cameraCore: CameraCore = CameraCore()
    private var utility: Utility = Utility(cameraCore)

    fun setContext(context: Context) =
      apply { this.context = context }

    fun setCameraPreview(cameraPreview: PreviewView) =
      apply { cameraCore.cameraPreview = cameraPreview }

    fun setLifecycleOwner(owner: LifecycleOwner) =
      apply { this.owner = owner }

    fun setLensFacing(lensFacing: Int) =
      apply { cameraConfig = cameraConfig.copy(lensFacing = lensFacing) }

    fun setFlashMode(flashMode: Int) =
      apply { cameraConfig = cameraConfig.copy(flashMode = flashMode) }

    fun setPhotoQuality(@IntRange(from = 1, to = 100) photoQuality: Int) =
      apply { cameraConfig = cameraConfig.copy(photoQuality = photoQuality) }

    fun setZoomRatio(@FloatRange(from = 0.0, to = 4.0) zoomRatio: Float) =
      apply { cameraConfig = cameraConfig.copy(zoomRatio = zoomRatio) }

    fun isScanner(isScanner: Boolean)= apply { cameraCore.isScanner = isScanner }

    // START AN APP
    fun build(): XeroCamera {
      requireNotNull(context) { "Context must be set" }
      requireNotNull(owner) { "LifeCycle owner must be set" }
      requireNotNull(cameraCore.cameraPreview) { "Camera Preview must be set" }
      return XeroCamera(context!! , owner!!, utility, cameraConfig, cameraCore)
    }
  }

  companion object {
    fun builder() = Builder()
  }

  init {
    owner.lifecycle.addObserver(this)
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
      utility.cleanup()
    }
  }

  override fun onDestroy(owner: LifecycleOwner) {
    super.onDestroy(owner)
    Log.e("XeroCamera", "OnDestroy")
    owner.lifecycle.removeObserver(this)
  }
}


//    private val orientationEventListener by lazy {
//      object : OrientationEventListener(context) {
//        override fun onOrientationChanged(orientation: Int) {
//          // Update camera rotation
//        }
//      }
//    }

