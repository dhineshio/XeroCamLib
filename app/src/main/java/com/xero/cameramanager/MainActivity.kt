package com.xero.cameramanager

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import com.permissionx.guolindev.PermissionX
import com.xero.xerocamera.ScannerModule.ScannerViewState
import com.xero.xerocamera.XeroCamera

class MainActivity : AppCompatActivity(){
  private lateinit var cameraPreview: PreviewView
  private lateinit var switchMode: Button
  private lateinit var captureButton: Button
  private val requiredPermissions = mutableListOf(
    Manifest.permission.CAMERA,
  )

  @SuppressLint("MissingInflatedId")
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContentView(R.layout.activity_main)
    cameraPreview = findViewById(R.id.cameraPreview)
    switchMode = findViewById(R.id.switchMode)
    captureButton = findViewById(R.id.capture)

    val xeroCamera = XeroCamera.builder().apply {
      setContext(this@MainActivity)
      setLifecycleOwner(this@MainActivity)
      setCameraPreview(cameraPreview)
      setLensFacing(CameraSelector.LENS_FACING_BACK)
      setFlashMode(ImageCapture.FLASH_MODE_OFF)
      setPhotoQuality(100)
      isScanner(true)
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
          xeroCamera.apply {
            startCamera()
          }
        }
      }

    switchMode.setOnClickListener {
      xeroCamera.enableScanner(true)
    }
    captureButton.setOnClickListener{
      xeroCamera.enableScanner(false)
    }
  }
}