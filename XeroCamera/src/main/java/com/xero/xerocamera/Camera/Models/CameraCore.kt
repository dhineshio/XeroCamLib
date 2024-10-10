package com.xero.xerocamera.Camera.Models

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import com.xero.xerocamera.Camera.CameraModule.LensFacing
import com.xero.xerocamera.Scanner.ScannerModule.ScannerOverlay
import com.xero.xerocamera.Utility.Utility

data class CameraCore(
  var cameraPreview: PreviewView? = null,
  var lensFacing: Int = CameraSelector.LENS_FACING_BACK,
  var isScanner: Boolean? = false,
  var isLiveScan : Boolean? = false,
  var scannerOverlay: ScannerOverlay? = null,
)

enum class FileNameType{
  TIME_STAMP,
  COUNT,
  BOTH
}