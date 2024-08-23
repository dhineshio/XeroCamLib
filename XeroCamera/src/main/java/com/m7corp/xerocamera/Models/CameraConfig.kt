package com.m7corp.xerocamera.Models

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.video.Quality

data class CameraConfig(
  val lensFacing : Int = CameraSelector.LENS_FACING_BACK,
  val flashMode : Int = ImageCapture.FLASH_MODE_ON,
  val photoQuality: Int = 100
)
