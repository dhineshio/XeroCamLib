package com.xero.xerocamera.ScannerModule

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScannerAnalyzer(
  private val onResult: (state: ScannerViewState, barcode: String) -> Unit,
) : ImageAnalysis.Analyzer {
//  private var isScanning: Boolean = false

  @SuppressLint("UnsafeOptInUsageError")
  override fun analyze(imageProxy: ImageProxy) {
//    if (isScanning) {
//      imageProxy.close()
//      return
//    }
    val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
    val scanner = BarcodeScanning.getClient(options)
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
      InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        .let { image ->
          scanner.process(image)
            .addOnSuccessListener { barcodes ->
              for (barcode in barcodes) {
                onResult(ScannerViewState.Success, barcode.rawValue ?: "")
//                isScanning = true
              }
            }
            .addOnFailureListener {
              onResult(ScannerViewState.Failure, it.message ?: "")
            }
            .addOnCompleteListener {
              CoroutineScope(Dispatchers.IO).launch {
                imageProxy.close()
              }
            }
        }
    }
//    else {
//      Log.d("ScannerAnalyzer", "analyze: $isScanning")
//    }
  }

//  fun startScanning(isScanned: Boolean) {
//    isScanning = isScanned
//    Log.d("ScannerAnalyzer", "setTest: $isScanning")
//  }
}