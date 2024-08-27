package com.xero.xerocamera.CameraModule

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.view.PreviewView
import com.xero.xerocamera.R
import java.util.concurrent.TimeUnit

class FocusManager(
  private val cameraInitializer: CameraInitializer,
  private val cameraPreview: PreviewView,
  private val focusSound : () -> Unit
) {
  private var focusSquare: View? = null

  @SuppressLint("ClickableViewAccessibility")
  fun setupTouchFocus() {
    cameraPreview.setOnTouchListener { _, event ->
      when (event.action) {
        MotionEvent.ACTION_DOWN -> {
          val factory = SurfaceOrientedMeteringPointFactory(
            cameraPreview.width.toFloat(),
            cameraPreview.height.toFloat()
          )
          val point = factory.createPoint(event.x, event.y)
          val action = FocusMeteringAction.Builder(point)
            .setAutoCancelDuration(3, TimeUnit.SECONDS)
            .build()
          cameraInitializer.getCamera().cameraControl.startFocusAndMetering(action)
          drawFocusSquare(event.x, event.y)
          focusSound.invoke()
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
    focusSquare?.let { cameraPreview.removeView(it) }
    val squareSize = 150
    val square = View(cameraPreview.context).apply {
      setBackgroundResource(R.drawable.focus_square)
      layoutParams = FrameLayout.LayoutParams(squareSize, squareSize).apply {
        leftMargin = (x - squareSize / 2).toInt()
        topMargin = (y - squareSize / 2).toInt()
      }
    }
    cameraPreview.addView(square)
    focusSquare = square
    square.alpha = 1f
    square.animate()
      .setDuration(1500)
      .alpha(.4f)
      .withEndAction { cameraPreview.removeView(square) }
      .start()
  }
}