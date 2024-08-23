package com.xero.xerocamera

import android.content.Context
import android.view.View
import androidx.annotation.IntRange
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.xero.xerocamera.CameraModule.CameraInitializer
import com.xero.xerocamera.Models.CameraConfig
import com.xero.xerocamera.ScannerModule.PhotoCapture

class XeroCamera private constructor(
  private var context: Context,
  private var owner: LifecycleOwner,
  private var cameraPreview: PreviewView,
  private var cameraConfig: CameraConfig,
) {
  private lateinit var cameraInitializer: CameraInitializer
  private lateinit var photoCapture: PhotoCapture
  private lateinit var utility: Utility

  private var imageCapture = ImageCapture.Builder().setFlashMode(cameraConfig.flashMode)
    .setJpegQuality(cameraConfig.photoQuality)
    .build()

  fun startCamera() {
    cameraInitializer = CameraInitializer(context, owner, cameraPreview, cameraConfig, imageCapture)
    utility = Utility(cameraPreview)
    photoCapture = PhotoCapture(context, imageCapture, utility)
    cameraInitializer.initializeCamera()
  }

  fun switchLensFacing(lensFacing: Int) {
    updateConfig { it.copy(lensFacing = lensFacing) }
  }

  fun takePhoto(captureButton: View) {
    photoCapture.takePhoto(captureButton)
  }

  private fun updateConfig(update: (CameraConfig) -> CameraConfig) {
    cameraConfig = update(cameraConfig)
    startCamera()
  }

  class Builder {
    private var context: Context? = null
    private var owner: LifecycleOwner? = null
    private var cameraPreview: PreviewView? = null
    private var cameraConfig: CameraConfig = CameraConfig()

    fun setContext(context: Context) =
      apply { this.context = context }

    fun setLifecycleOwner(owner: LifecycleOwner) =
      apply { this.owner = owner }

    fun setCameraPreview(cameraPreview: PreviewView) =
      apply { this.cameraPreview = cameraPreview }

    fun setLensFacing(lensFacing: Int) =
      apply { cameraConfig = cameraConfig.copy(lensFacing = lensFacing) }

    fun setFlashMode(flashMode: Int) =
      apply { cameraConfig = cameraConfig.copy(flashMode = flashMode) }

    fun setPhotoQuality(@IntRange(from = 1, to = 100) photoQuality: Int) =
      apply { cameraConfig = cameraConfig.copy(photoQuality = photoQuality) }









    // START AN APP
    fun build(): XeroCamera {
      requireNotNull(context) { "Context must be set" }
      requireNotNull(owner) { "LifeCycle owner must be set" }
      requireNotNull(cameraPreview) { "Camera Preview must be set" }
      return XeroCamera(context!!, owner!!, cameraPreview!!, cameraConfig)
    }
  }

  companion object {
    fun builder() = Builder()
  }
}

//     FOCUS
//    private var customFocusDrawable: Int? = R.drawable.focus_square
//    private var focusSquare: View? = null
//    fun setCustomFocusDrawable(customFocusDrawable: Int?) = apply { this.customFocusDrawable = customFocusDrawable }

//    // FOCUS
//    @SuppressLint("ClickableViewAccessibility")
//    private fun setupTouchFocus() {
//      cameraPreview?.setOnTouchListener { _, event ->
//        when (event.action) {
//          MotionEvent.ACTION_DOWN -> {
//            val factory = SurfaceOrientedMeteringPointFactory(
//              cameraPreview!!.width.toFloat(),
//              cameraPreview!!.height.toFloat()
//            )
//            val point = factory.createPoint(event.x, event.y)
//            val action = FocusMeteringAction.Builder(point)
//              .setAutoCancelDuration(3, TimeUnit.SECONDS)
//              .build()
//            camera?.cameraControl?.startFocusAndMetering(action)
//            drawFocusSquare(event.x, event.y)
//            focusSound()
//            true
//          }
//          else -> false
//        }
//      }
//    }
//
//    private fun drawFocusSquare(
//      x: Float,
//      y: Float,
//    ) {
//      focusSquare?.let { cameraPreview!!.removeView(it) }
//      val squareSize = 150
//      val square = View(cameraPreview!!.context).apply {
//        setBackgroundResource(customFocusDrawable!!)
//        layoutParams = FrameLayout.LayoutParams(squareSize, squareSize).apply {
//          leftMargin = (x - squareSize / 2).toInt()
//          topMargin = (y - squareSize / 2).toInt()
//        }
//      }
//      cameraPreview!!.addView(square)
//      focusSquare = square
//      square.alpha = 1f
//      square.animate()
//        .setDuration(1500)
//        .alpha(.4f)
//        .withEndAction { cameraPreview!!.removeView(square) }
//        .start()
//    }
//
//    fun setZoom(zoomRatio: Float) {
//      camera?.cameraControl?.setZoomRatio(zoomRatio)
//    }
//    fun setExposureCompensation(value: Int) {
//      camera?.cameraControl?.setExposureCompensationIndex(value)
//    }
//    fun cleanup() {
//      mediaActionSound.release()
//      // Release other resources
//    }
//    private val orientationEventListener by lazy {
//      object : OrientationEventListener(context) {
//        override fun onOrientationChanged(orientation: Int) {
//          // Update camera rotation
//        }
//      }
//    }

