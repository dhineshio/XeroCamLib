package com.m7corp.xerocamera

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.media.MediaActionSound
import android.os.Environment
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.m7corp.xerocamera.CameraFunctionality.CameraMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class XeroCamera private constructor(
  context: Context?,
  activity: Activity?,
  owner: LifecycleOwner?,
  mediaActionSound: MediaActionSound,
) {
  class Builder {
    private var context: Context? = null
    private var activity: Activity? = null
    private var owner: LifecycleOwner? = null
    private var cameraPreview: PreviewView? = null

    // SOUND
    private lateinit var mediaActionSound: MediaActionSound

    // FOCUS
    private var customFocusDrawable: Int? = R.drawable.focus_square
    private var focusSquare: View? = null

    // BASE CAMERA
    private var camera: Camera? = null
    private lateinit var imageCapture: ImageCapture
    private lateinit var videoCapture: VideoCapture<Recorder>
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraSelector: CameraSelector
    private lateinit var preview: Preview
    private var cameraMode: CameraMode = CameraMode.Photo

    fun setContext(context: Context) = apply { this.context = context }
    fun setLifecycleOwner(owner: LifecycleOwner) = apply { this.owner = owner }
    fun setCameraPreview(cameraPreview: PreviewView) = apply { this.cameraPreview = cameraPreview }
    fun setCustomFocusDrawable(customFocusDrawable: Int?) =
      apply { this.customFocusDrawable = customFocusDrawable }
    fun setMode(cameraMode: CameraMode) = apply { this.cameraMode = cameraMode }

    @SuppressLint("RestrictedApi")
    private fun startCamera() {
      val cameraProviderFuture = ProcessCameraProvider.getInstance(context!!)
      cameraProviderFuture.addListener({
        cameraProvider = cameraProviderFuture.get()
        cameraSelector =
          CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
        preview = Preview.Builder().build()
        imageCapture = ImageCapture.Builder().setFlashMode(ImageCapture.FLASH_MODE_ON)
          .setFlashType(ImageCapture.FLASH_TYPE_USE_TORCH_AS_FLASH).build()
        videoCapture = VideoCapture.withOutput(
          Recorder.Builder().setQualitySelector(
            QualitySelector.from(
              Quality.UHD,
              FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
            )
          ).setTargetVideoEncodingBitRate(5000000).build()
        )
        try {
          when (cameraMode) {
            is CameraMode.Photo -> {
              cameraProvider.unbindAll()
              camera = cameraProvider.bindToLifecycle(owner!!, cameraSelector, preview, imageCapture)
            }
            is CameraMode.Video -> {
              cameraProvider.unbindAll()
              camera = cameraProvider.bindToLifecycle(owner!!, cameraSelector, preview, videoCapture)
            }
            is CameraMode.PhotoVideo -> {
              cameraProvider.unbindAll()
              camera = cameraProvider.bindToLifecycle(owner!!, cameraSelector, preview, imageCapture, videoCapture)
            }
          }
          preview.setSurfaceProvider(cameraPreview?.surfaceProvider)
          setupTouchFocus()
        } catch (e: Exception) {
          Log.e("Xero Builder", "Use Case binding failed $e")
        }
      }, ContextCompat.getMainExecutor(context!!))
    }

//    fun switchMode(cameraMode: CameraMode){
//      when(cameraMode){
//        is CameraMode.Photo -> this.cameraMode = CameraMode.Photo
//        is CameraMode.Video -> this.cameraMode = CameraMode.Video
//        CameraMode.PhotoVideo -> this.cameraMode = CameraMode.PhotoVideo
//      }
//    }

    // TAKE PHOTO
    fun takePhoto(
      captureButton: View, folderName: String? = "XeroCamera",
      onImageSavedCallback: (() -> Unit?)? = null,
    ) {
      captureButton.setOnClickListener {
        captureButton.isClickable = false
        val rootDirectory = ContextCompat.getExternalFilesDirs(context!!, null).firstOrNull()?.let {
          File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            folderName!!
          )
        }?.apply {
          if (!exists()) mkdirs()
        }
        val outputDirectory = File(rootDirectory, "Photo").apply {
          if (!exists()) mkdirs()
        }
        val formattedTime =
          LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val imageFile = File(outputDirectory, "img_$formattedTime.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()
        imageCapture.takePicture(
          outputOptions,
          ContextCompat.getMainExecutor(context!!),
          object : ImageCapture.OnImageSavedCallback {
            override fun onCaptureStarted() {
              super.onCaptureStarted()
              owner!!.lifecycleScope.launch {
                withContext(Dispatchers.Main) {
                  captureSound().apply {
                    applyBlinkEffect()
                  }
                  captureButton.isClickable = true
                }
              }
            }

            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
              val imagePath = outputFileResults.savedUri?.path ?: "Path not available"
              Log.e("ImageCapture", "Image Capture has Done $imagePath")
              if (onImageSavedCallback != null) {
                onImageSavedCallback()
              }
            }

            override fun onError(exception: ImageCaptureException) {
              Log.e("ImageCapture", "Image Capture Failed $exception")
            }
          }
        )
      }
    }


    // UTILITIES
    private fun captureSound() {
      mediaActionSound.play(MediaActionSound.SHUTTER_CLICK)
    }

    private fun focusSound() {
      mediaActionSound.play(MediaActionSound.FOCUS_COMPLETE)
    }

    private fun applyBlinkEffect() {
      val overlayView = View(cameraPreview!!.context).apply {
        setBackgroundColor(android.graphics.Color.BLACK)
        alpha = 0f
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT
        )
      }
      (cameraPreview as ViewGroup).addView(overlayView)
      overlayView.animate().alpha(1f).setDuration(100).withEndAction {
        overlayView.animate().alpha(0f).setDuration(100).withEndAction {
          cameraPreview!!.removeView(overlayView)
        }.start()
      }.start()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupTouchFocus() {
      cameraPreview?.setOnTouchListener { _, event ->
        when (event.action) {
          MotionEvent.ACTION_DOWN -> {
            val factory = SurfaceOrientedMeteringPointFactory(
              cameraPreview!!.width.toFloat(),
              cameraPreview!!.height.toFloat()
            )
            val point = factory.createPoint(event.x, event.y)
            val action = FocusMeteringAction.Builder(point)
              .setAutoCancelDuration(3, TimeUnit.SECONDS)
              .build()
            camera?.cameraControl?.startFocusAndMetering(action)
            drawFocusSquare(event.x, event.y)
            focusSound()
            true
          }

          else -> false
        }
      }
    }

    private fun drawFocusSquare(
      x: Float,
      y: Float,
    ) {
      focusSquare?.let { cameraPreview!!.removeView(it) }
      val squareSize = 150
      val square = View(cameraPreview!!.context).apply {
        setBackgroundResource(customFocusDrawable!!)
        layoutParams = FrameLayout.LayoutParams(squareSize, squareSize).apply {
          leftMargin = (x - squareSize / 2).toInt()
          topMargin = (y - squareSize / 2).toInt()
        }
      }
      cameraPreview!!.addView(square)
      focusSquare = square

      square.alpha = 1f
      square.animate()
        .setDuration(1500)
        .alpha(.4f)
        .withEndAction { cameraPreview!!.removeView(square) }
        .start()
    }

    fun start(): XeroCamera {
      requireNotNull(context) {
        "Context must be set"
      }
      requireNotNull(owner) {
        "LifeCycle owner must be set"
      }
      requireNotNull(cameraPreview) {
        "Camera Preview must be set"
      }
      mediaActionSound = MediaActionSound()
      startCamera()
      return XeroCamera(context, activity, owner, mediaActionSound)
    }
  }

  companion object {
    fun builder() = Builder()
  }
}

