package com.xero.xerocamera.Camera.CameraModule

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

class VideoCapture(
	private val context: Context,
	private val videoCapture: VideoCapture<Recorder>
) {
	private var recording: Recording? = null
	private var isPaused = false

	//timer set variable
	private lateinit var timerView : TextView
	private var captureTime = 0L
	private var isRunning = false
	var handler: Handler = Handler()

	fun startVideo(
		directoryName: String,
		fileName: String,
		subDirectoryName: String,
		timerView : TextView? = null,
		onStart: (() -> Unit?)? = null,
		onSuccess: ((videoPath: String) -> Unit?)? = null,
		onError: (() -> Unit?)? = null,
	) {
		if (timerView != null) {
			this.timerView = timerView
		}
		if (recording != null) {
			Toast.makeText(context, "Recording already in progress", Toast.LENGTH_SHORT).show()
			return
		}
		val rootDirectory = ContextCompat.getExternalFilesDirs(context, null).firstOrNull()?.let {
			File(
				Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
				directoryName
			)
		}?.apply {
			if (!exists()) mkdirs()
		}

		var outputDirectory = rootDirectory

		if (subDirectoryName.isNotEmpty()) {
			val subDirParts = subDirectoryName.split("/")
			outputDirectory = subDirParts.fold(rootDirectory) { parentDir, dirName ->
				File(parentDir, dirName).apply {
					if (!exists()) mkdir()
				}
			}
		}

		val formattedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHss"))
		val videoFile = File(outputDirectory, "${fileName}_${formattedTime}.mp4")
		val outputOptions = FileOutputOptions.Builder(videoFile).build()

		try {
			recording = videoCapture.output.prepareRecording(context, outputOptions).apply {
				if (ActivityCompat.checkSelfPermission(
						context,
						Manifest.permission.RECORD_AUDIO
					) == PackageManager.PERMISSION_GRANTED
				) {
					withAudioEnabled()
				} else {
					Toast.makeText(context, "Please Grant Permission Record Audio", Toast.LENGTH_SHORT).show()
				}
			}.start(ContextCompat.getMainExecutor(context)) { recordEvent ->
				when (recordEvent) {
					is VideoRecordEvent.Start -> {
						startTimer()
						onStart?.invoke()
					}

					is VideoRecordEvent.Finalize -> {
						val videoPath = recordEvent.outputResults.outputUri.path ?: "Path not available"
						onSuccess?.invoke(videoPath)
						if (recordEvent.hasError()) {
							Log.e("VideoCapture", "error : ${recordEvent.cause}")
							recording?.close()
							recording = null
							onError?.invoke()
						}
					}
				}
			}
		} catch (e: Exception) {
			Log.e("VideoCapture", "Exception while starting recording", e)
		}
	}

	fun pauseVideo() {
		if (recording != null && !isPaused) {
			pauseTimer()
			recording?.pause()
			isPaused = true
		}
	}

	fun resumeVideo() {
		if (recording != null && isPaused) {
			resumeTimer()
			recording?.resume()
			isPaused = false
		}
	}

	fun stopVideo() {
		if (recording != null) {
			stopTimer()
			recording?.stop()
			recording = null
		} else {
			Toast.makeText(context, "Make Sure Video has Started", Toast.LENGTH_SHORT).show()
		}
	}

	fun capturePhoto(takePhoto: () -> Unit) {
		if (recording != null) {
			takePhoto.invoke()
		} else {
			Toast.makeText(context, "Make Sure Video Has Started...", Toast.LENGTH_SHORT).show()
		}
	}

	private val updateCaptureTime: Runnable = object : Runnable {
		override fun run() {
			if (isRunning) {
				val minutes = TimeUnit.MILLISECONDS.toMinutes(captureTime)
				val seconds = TimeUnit.MILLISECONDS.toSeconds(captureTime) % 60
				timerView.text = String.format("%02d:%02d", minutes, seconds)
				captureTime += 100 // Increment by 100ms
				handler.postDelayed(this, 100)
			}
		}
	}

	private fun startTimer() {
		if (!isRunning) {
			isRunning = true
			handler.post(updateCaptureTime)
		}
	}

	private fun pauseTimer() {
		isRunning = false
	}

	private fun resumeTimer() {
			if (!isRunning) {
				isRunning = true
				handler.post(updateCaptureTime)
			}
	}

	private fun stopTimer() {
		isRunning = false
		captureTime = 0L
		timerView.text = "00:00"
	}

}