package com.xero.xerocamera.Camera.CameraModule

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.SoundEffectConstants
import android.view.View
import android.widget.FrameLayout
import androidx.camera.core.Camera
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import com.xero.xerocamera.Camera.Models.CameraCore
import com.xero.xerocamera.R
import com.xero.xerocamera.Utility.Utility
import java.util.concurrent.TimeUnit

class FocusManager(
  private val cameraCore: CameraCore,
  private val camera: Camera,
  private val utility: Utility
) {
  private var focusSquare: View? = null

  init {
    setupTouchFocus()
  }

  @SuppressLint("ClickableViewAccessibility")
  fun setupTouchFocus() {
    cameraCore.cameraPreview!!.setOnTouchListener { _, event ->
      when (event.action) {
        MotionEvent.ACTION_DOWN -> {
          val factory = SurfaceOrientedMeteringPointFactory(
            cameraCore.cameraPreview!!.width.toFloat(),
            cameraCore.cameraPreview!!.height.toFloat()
          )
          val point = factory.createPoint(event.x, event.y)
          val action = FocusMeteringAction.Builder(point)
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()
          camera.cameraControl.startFocusAndMetering(action)
          drawFocusSquare(event.x, event.y)
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
    focusSquare?.let { cameraCore.cameraPreview!!.removeView(it) }
    val squareSize = 150
    val square = View(cameraCore.cameraPreview!!.context).apply {
      setBackgroundResource(R.drawable.focus_square)
      layoutParams = FrameLayout.LayoutParams(squareSize, squareSize).apply {
        leftMargin = (x - squareSize / 2).toInt()
        topMargin = (y - squareSize / 2).toInt()
      }
    }
    cameraCore.cameraPreview!!.addView(square)
    utility.focusSound()
    focusSquare = square
    square.alpha = 1f
    square.animate()
      .setDuration(1500)
      .alpha(.4f)
      .withEndAction { cameraCore.cameraPreview!!.removeView(square) }
      .start()
  }
}