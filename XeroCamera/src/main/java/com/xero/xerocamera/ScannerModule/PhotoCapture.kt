package com.xero.xerocamera.ScannerModule

import android.content.Context
import android.os.Environment
import android.util.Log
import android.view.View
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import com.xero.xerocamera.Utility
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PhotoCapture (
  private val context: Context,
  private val imageCapture: ImageCapture,
  private val utility: Utility
){
  fun takePhoto(
    captureButton: View,
  ) {
    captureButton.setOnClickListener {
      val rootDirectory = ContextCompat.getExternalFilesDirs(context, null).firstOrNull()?.let {
        File(
          Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
          "Demo"
        )
      }?.apply {
        if (!exists()) mkdirs()
      }
      val outputDirectory = File(rootDirectory, "Photo").apply {
        if (!exists()) mkdirs()
      }
      val formattedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
      val imageFile = File(outputDirectory, "img_$formattedTime.jpg")
      val outputOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()
      imageCapture.takePicture(
        outputOptions, ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
          override fun onCaptureStarted() {
            super.onCaptureStarted()
            utility.captureSound()
          }

          override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
            val imagePath = outputFileResults.savedUri?.path ?: "Path not Available"
            Log.e("ImageCapture", "Image Capture has Done $imagePath")
          }

          override fun onError(exception: ImageCaptureException) {
            Log.e("ImageCapture", "Image Capture Failed $exception")
          }
        }
      )
    }
  }
}