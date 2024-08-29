package com.xero.xerocamera.Camera.Models

import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import com.xero.xerocamera.Camera.CameraModule.LensFacing
import com.xero.xerocamera.Scanner.ScannerModule.ScannerOverlay

data class CameraCore(
  var cameraPreview: PreviewView? = null,
  var camera: Camera? = null,
  var imageCapture: ImageCapture? = null,
  var videoCapture: VideoCapture<Recorder>? = null,
  var cameraSelector: CameraSelector? = null,
  var isScanner : Boolean? = false,
  var scannerOverlay: ScannerOverlay? = null,
  val lensFacing : Int = CameraSelector.LENS_FACING_BACK,
  val lensFacing1 : LensFacing = LensFacing.BackFacing,
)