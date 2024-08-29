package com.xero.xerocamera.CameraModule

sealed class CaptureMode {
  data object Image : CaptureMode()
  data object Video : CaptureMode()
  data object Both : CaptureMode()
}