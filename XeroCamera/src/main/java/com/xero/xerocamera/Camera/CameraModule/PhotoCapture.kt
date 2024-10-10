package com.xero.xerocamera.Camera.CameraModule

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import com.xero.xerocamera.Camera.Models.FileNameType
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PhotoCapture(
  private val context: Context,
  private val imageCapture: ImageCapture,
  private val captureSound: () -> Unit,
) {
  fun takePhoto(
    onSuccess: ((imagePath: String) -> Unit)?,
    onFailure: ((exception: Exception) -> Unit)?,
    enableSound: Boolean,
    useCache: Boolean,
    directoryName: String,
    fileName: String,
    fileNameType: FileNameType,
    subDirectoryName: String
  ) {
    val rootDirectory = if (useCache) {
      context.cacheDir
    } else {
      ContextCompat.getExternalFilesDirs(context, null).firstOrNull()?.let {
        File(
          Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
          directoryName
        )
      }?.apply {
        if (!exists()) mkdirs()
      }
    } ?: context.cacheDir // Fallback to cache dir if external storage is not available

    var outputDirectory = rootDirectory
    if (subDirectoryName.isNotEmpty()) {
      val subDirParts = subDirectoryName.split("/")
      outputDirectory = subDirParts.fold(rootDirectory) { parentDir, dirName ->
        File(parentDir, dirName).apply {
          if (!exists()) mkdir()
        }
      }
    }
    val formattedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd_HHmmss"))
    val imageFileCount = File(outputDirectory.toString()).listFiles { file ->
      file.isFile && file.extension.lowercase() in arrayOf(
        "jpg",
        "jpeg",
        "png"
      )
    }
    val totalImageCount = (imageFileCount?.size?.plus(1)) ?: 1
    val fileName =  when(fileNameType){
      FileNameType.TIME_STAMP -> "${fileName}_$formattedTime.jpg"
      FileNameType.COUNT -> {
        "${fileName}_$totalImageCount.jpg"
      }
      FileNameType.BOTH -> "${fileName}_${totalImageCount}_$formattedTime.jpg"
    }
    val imageFile = File(outputDirectory, fileName)
    val outputOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()
    if (enableSound) {
      captureSound.invoke()
    }
    imageCapture.takePicture(
      outputOptions, ContextCompat.getMainExecutor(context),
      object : ImageCapture.OnImageSavedCallback {
        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
          val imagePath = outputFileResults.savedUri?.path ?: "Path not Available"
          onSuccess?.invoke(imagePath)
          Log.e("ImageCapture", "Image Capture has Done $imagePath")
        }

        override fun onError(exception: ImageCaptureException) {
          onFailure?.invoke(exception)
          Log.e("ImageCapture", "Image Capture Failed $exception")
        }
      }
    )
  }
}