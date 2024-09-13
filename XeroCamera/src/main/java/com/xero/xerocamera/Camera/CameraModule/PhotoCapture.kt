package com.xero.xerocamera.Camera.CameraModule

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
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
		useCache: Boolean,
		directoryName: String,
		fileName: String,
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
		}
		val outputDirectory = File(rootDirectory, subDirectoryName).apply {
			if (!exists()) mkdirs()
		}
		val formattedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
		val imageFile = File(outputDirectory, "${fileName}_$formattedTime.jpg")
		val outputOptions = ImageCapture.OutputFileOptions.Builder(imageFile).build()
		imageCapture.takePicture(
			outputOptions, ContextCompat.getMainExecutor(context),
			object : ImageCapture.OnImageSavedCallback {
				override fun onCaptureStarted() {
					super.onCaptureStarted()
					captureSound.invoke()
				}

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