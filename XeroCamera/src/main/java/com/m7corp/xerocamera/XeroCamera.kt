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
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
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
    private var focusSquare: View? = null
    private lateinit var imageCapture: ImageCapture
    private var camera: Camera? = null

    fun setContext(context: Context) = apply { this.context = context }
    fun setActivity(activity: Activity) = apply { this.activity = activity }
    fun setLifecycleOwner(owner: LifecycleOwner) = apply { this.owner = owner }
    fun setCameraPreview(cameraPreview: PreviewView) = apply { this.cameraPreview = cameraPreview }

    // SOUND
    private lateinit var mediaActionSound: MediaActionSound

    private fun startCamera() {
      val cameraProviderFuture = ProcessCameraProvider.getInstance(context!!)
      cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val cameraSelector = CameraSelector.Builder().requireLensFacing(
          CameraSelector
            .LENS_FACING_BACK
        ).build()
        val preview = Preview.Builder().build()
        imageCapture = ImageCapture.Builder().build()
        try {
          cameraProvider.unbindAll()
          camera = cameraProvider.bindToLifecycle(
            owner!!, cameraSelector, preview, imageCapture
          )
          preview.setSurfaceProvider(cameraPreview?.surfaceProvider)
          setupTouchFocus()
        } catch (e: Exception) {
          Log.e("Xero Builder", "Use Case binding failed $e")
        }
      }, ContextCompat.getMainExecutor(context!!))
    }

    fun takePhoto(
      captureButton: View, folderName: String? = "XeroCam",
      onImageSavedCallback: (() -> Unit?)? = null,
    ) {
      captureButton.setOnClickListener {
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
      customFocusDrawable: Int? = R.drawable.focus_square,
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

    fun build(): XeroCamera {
      requireNotNull(context) {
        "Context must be set"
      }
      requireNotNull(owner) {
        "LifeCycle owner must be set"
      }
      requireNotNull(cameraPreview) {
        "Camera Preview must be set"
      }
      startCamera()
      mediaActionSound = MediaActionSound()
      return XeroCamera(context, activity, owner, mediaActionSound)
    }
  }

  companion object {
    fun builder() = Builder()
  }
}

