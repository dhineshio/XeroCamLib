package com.m7corp.cameramanager

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Camera
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import com.m7corp.xerocamera.CameraFunctionality.CameraMode
import com.m7corp.xerocamera.XeroCamera
import com.permissionx.guolindev.PermissionX

class MainActivity : AppCompatActivity() {
  private lateinit var cameraPreview: PreviewView
  private lateinit var switchMode: Button
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
          XeroCamera.builder().apply {
            setContext(this@MainActivity)
            setLifecycleOwner(this@MainActivity)
            setCameraPreview(cameraPreview)
            takePhoto(switchMode)
            setMode(CameraMode.PhotoVideo)
            start()
          }
        }
      }
  }
}