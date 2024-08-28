package com.xero.xerocamera.ScannerModule

sealed class ScannerViewState {
  data object Success : ScannerViewState()
  data object Failure : ScannerViewState()
}