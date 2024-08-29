package com.xero.xerocamera.Models

import androidx.camera.core.Camera
import androidx.camera.core.ImageCapture
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.constraintlayout.widget.ConstraintLayout
import com.xero.xerocamera.ScannerModule.ScannerOverlay

data class CameraCore(
  var cameraPreview: PreviewView? = null,
  var scannerOverlay: ScannerOverlay? = null,
  var camera: Camera? = null,
  var imageCapture: ImageCapture? = null,
  var videoCapture: VideoCapture<Recorder>? = null,
  var isScanner : Boolean? = false
)