package com.m7corp.xerocamera.CameraFunctionality

sealed class CameraMode {
  data object Photo : CameraMode()
  data object Video : CameraMode()
  data object PhotoVideo : CameraMode()
}