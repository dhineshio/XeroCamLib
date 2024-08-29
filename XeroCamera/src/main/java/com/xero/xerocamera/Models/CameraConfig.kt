package com.xero.xerocamera.Models

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.video.Quality
import com.xero.xerocamera.CameraModule.CaptureMode

data class CameraConfig(
  var captureMode: CaptureMode = CaptureMode.Image,
  val lensFacing : Int = CameraSelector.LENS_FACING_BACK,
  val flashMode : Int = ImageCapture.FLASH_MODE_ON,
  val photoQuality: Int = 100,
  val zoomRatio: Float = 0.0f
)
