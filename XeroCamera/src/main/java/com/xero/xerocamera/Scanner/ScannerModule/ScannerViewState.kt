package com.xero.xerocamera.Scanner.ScannerModule

sealed class ScannerViewState {
  data object Success : ScannerViewState()
  data object Failure : ScannerViewState()
}