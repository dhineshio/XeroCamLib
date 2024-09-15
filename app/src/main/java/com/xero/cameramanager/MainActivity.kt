package com.xero.cameramanager

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import com.permissionx.guolindev.PermissionX
import com.xero.xerocamera.Camera.CameraModule.FlashMode
import com.xero.xerocamera.XeroCamera
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
	private lateinit var cameraPreview: PreviewView
	private lateinit var switchMode: Button
	private lateinit var frontCamera: Button
	private lateinit var captureButton: Button
	private lateinit var scanOn: Button
	private lateinit var scanOff: Button
	private lateinit var flashOn: Button
	private lateinit var flashOff: Button
	private lateinit var torch: Button
	private lateinit var zoom: SeekBar
	private lateinit var timer: TextView

	private val requiredPermissions = mutableListOf(
		Manifest.permission.CAMERA,
		Manifest.permission.RECORD_AUDIO
	)
	private var res: String? = null

	//timer set variable
	private var captureTime = 0L
	private var isRunning = false
	var handler: Handler = Handler()

	@SuppressLint("MissingInflatedId")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		enableEdgeToEdge()
		setContentView(R.layout.activity_main)

		if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
			requiredPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
			requiredPermissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			requiredPermissions.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
		}

		cameraPreview = findViewById(R.id.cameraPreview)
		switchMode = findViewById(R.id.switchMode)
		captureButton = findViewById(R.id.capture)
		scanOn = findViewById(R.id.enableScanner)
		frontCamera = findViewById(R.id.frontCamera)
		scanOff = findViewById(R.id.disableScanner)
		flashOn = findViewById(R.id.flashOn)
		flashOff = findViewById(R.id.flashOff)
		torch = findViewById(R.id.torch)
		zoom = findViewById(R.id.seekBar2)
		timer = findViewById(R.id.timer)

		val xeroCamera = XeroCamera.builder().apply {
			setContext(this@MainActivity)
			setLifecycleOwner(this@MainActivity)
			setActivity(this@MainActivity)
			setCameraPreview(cameraPreview)
		}.build()

		PermissionX.init(this)
			.permissions(requiredPermissions)
			.onExplainRequestReason { scope, deniedList ->
				scope.showRequestReasonDialog(
					deniedList,
					"This app requires the following permissions",
					"OK",
				)
			}
			.onForwardToSettings { scope, deniedList ->
				scope.showForwardToSettingsDialog(
					deniedList, "This app requires the following " +
							"permissions ! You have denied them. Please grant the permissions.", "Go to Settings"
				)
			}
			.request { allGranted, _, _ ->
				if (allGranted) {
					xeroCamera.startCamera()
				}
			}

		xeroCamera.scannerBarcode.observe(this) { barcode ->
			res = barcode
		}

//		captureButton.setOnClickListener {
//			xeroCamera.takePhoto(onSuccess = {
//				Log.e("MainActivity", "Success $it")
//			}, onFailure = {
//				Log.e("MainActivity", "failed $it")
//			}, false, "Thaagam Education", "photo",
//				"Dhinesh"
//			)
//		}

		captureButton.setOnClickListener {
			xeroCamera.startVideo(onStart = {
				startTimer()
				Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
			}, onSuccess = {
				Toast.makeText(this, "Recording success $it", Toast.LENGTH_SHORT).show()
			}, onError = {
				Toast.makeText(this, "Failed", Toast.LENGTH_SHORT).show()
			})
		}

		switchMode.setOnClickListener {
			stopTimer()
			xeroCamera.stopVideo()
		}

		scanOn.setOnClickListener {
			pauseTimer()
			xeroCamera.pauseVideo()
		}
		scanOff.setOnClickListener {
			resumeTimer()
			xeroCamera.resumeVideo()
		}
		frontCamera.setOnClickListener {
			xeroCamera.photoCapture(onSuccess = {
				Toast.makeText(this, "Capture Success : $it", Toast.LENGTH_SHORT).show()
			}, onFailure = null, true)
		}
		flashOff.setOnClickListener {
			xeroCamera.setFlashMode(FlashMode.FlashOff)
		}
		torch.setOnClickListener {
			xeroCamera.setFlashMode(FlashMode.TorchMode)
		}

		xeroCamera.setSeekBarZoom(seekBar = zoom)
		
//		frontCamera.setOnClickListener {
//			xeroCamera.switchLensFacing(CameraSelector.LENS_FACING_BACK)
//		}
	}

	private val updateCaptureTime: Runnable = object : Runnable {
		override fun run() {
			if (isRunning) {
				val minutes = TimeUnit.MILLISECONDS.toMinutes(captureTime)
				val seconds = TimeUnit.MILLISECONDS.toSeconds(captureTime) % 60
				timer.text = String.format("%02d:%02d", minutes, seconds)
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
		timer.text = "00:00"
	}
}