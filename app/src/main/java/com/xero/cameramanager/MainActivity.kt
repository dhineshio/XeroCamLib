package com.xero.cameramanager

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import com.permissionx.guolindev.PermissionX
import com.xero.xerocamera.Camera.CameraModule.FlashMode
import com.xero.xerocamera.XeroCamera

class MainActivity : AppCompatActivity() {
  private lateinit var cameraPreview: PreviewView
  private lateinit var switchMode: Button
  private lateinit var frontCamera: Button
  private lateinit var captureButton: Button
  private lateinit var scanOn: Button
  private lateinit var scanOff: Button
  private lateinit var flashOn : Button
  private lateinit var flashOff : Button
  private lateinit var torch : Button
  private lateinit var zoom : SeekBar

  private val requiredPermissions = mutableListOf(
	Manifest.permission.CAMERA,
  )
  private var res : String ?= null

  @SuppressLint("MissingInflatedId")
  override fun onCreate(savedInstanceState: Bundle?) {
	super.onCreate(savedInstanceState)
	enableEdgeToEdge()
	setContentView(R.layout.activity_main)
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


	val xeroCamera = XeroCamera.builder().apply {
	  setContext(this@MainActivity)
	  setLifecycleOwner(this@MainActivity)
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

	captureButton.setOnClickListener {
		xeroCamera.takePhoto()
	}

	scanOn.setOnClickListener {
	  xeroCamera.enableScanner(true)
	}

	scanOff.setOnClickListener {
	  xeroCamera.resetScanning()
	}
	flashOn.setOnClickListener {
	  xeroCamera.setFlashMode(FlashMode.FlashOn)
	}
	flashOff.setOnClickListener {
	  xeroCamera.setFlashMode(FlashMode.FlashOff)
	}
	torch.setOnClickListener {
	  xeroCamera.setFlashMode(FlashMode.TorchMode)
	}

	xeroCamera.setSeekBarZoom(seekBar = zoom)

	switchMode.setOnClickListener {
	  xeroCamera.switchLensFacing(CameraSelector.LENS_FACING_FRONT)
	}
	frontCamera.setOnClickListener {
	  xeroCamera.switchLensFacing(CameraSelector.LENS_FACING_BACK)
	}
  }
}