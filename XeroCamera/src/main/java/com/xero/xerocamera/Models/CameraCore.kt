package com.xero.xerocamera.Models

import android.content.Context
import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView

data class CameraCore(
  var cameraPreview: PreviewView? = null,
  var camera: Camera? = null,
  var imageCapture: ImageCapture? = null
)