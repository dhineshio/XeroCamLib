package com.xero.xerocamera.Models

import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView

data class CameraCore(
  var cameraPreview: PreviewView? = null,
  var camera: Camera? = null,
  var imageCapture: ImageCapture? = null,
  var isScanner : Boolean? = false
)