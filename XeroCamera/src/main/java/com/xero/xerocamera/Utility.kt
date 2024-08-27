package com.xero.xerocamera

import android.graphics.Color
import android.media.MediaActionSound
import android.view.View
import android.view.ViewGroup
import com.xero.xerocamera.Models.CameraCore

class Utility(private val cameraCore: CameraCore) {
  private val mediaActionSound = MediaActionSound()

  fun captureSound() {
    mediaActionSound.play(MediaActionSound.SHUTTER_CLICK).apply {
      applyBlinkEffect()
    }
  }

  fun focusSound() {
    mediaActionSound.play(MediaActionSound.FOCUS_COMPLETE)
  }

  private fun applyBlinkEffect() {
    val overlayView = View(cameraCore.cameraPreview!!.context).apply {
      setBackgroundColor(Color.BLACK)
      alpha = 0f
      layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
    }
    (cameraCore.cameraPreview!! as ViewGroup).addView(overlayView)
    overlayView.animate().alpha(1f).setDuration(100).withEndAction {
      overlayView.animate().alpha(0f).setDuration(100).withEndAction {
        cameraCore.cameraPreview!!.removeView(overlayView)
      }.start()
    }.start()
  }

  fun cleanup() {
      mediaActionSound.release()
  }
}