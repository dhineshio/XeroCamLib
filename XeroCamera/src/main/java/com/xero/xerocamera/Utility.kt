package com.xero.xerocamera

import android.media.MediaActionSound
import android.view.View
import android.view.ViewGroup
import androidx.camera.view.PreviewView

class Utility(private val cameraPreview: PreviewView) {
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
    val overlayView = View(cameraPreview.context).apply {
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
        cameraPreview.removeView(overlayView)
      }.start()
    }.start()
  }
}