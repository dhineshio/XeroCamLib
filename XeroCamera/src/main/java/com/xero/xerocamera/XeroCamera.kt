package com.xero.xerocamera

import android.content.Context
import android.util.Log
import android.view.View
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import androidx.camera.core.ImageCapture
import androidx.camera.view.PreviewView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.xero.xerocamera.CameraModule.CameraInitializer
import com.xero.xerocamera.CameraModule.FocusManager
import com.xero.xerocamera.Models.CameraConfig
import com.xero.xerocamera.ScannerModule.PhotoCapture

class XeroCamera private constructor(
    private var context: Context,
    private var owner: LifecycleOwner,
    private var cameraPreview: PreviewView,
    private var cameraConfig: CameraConfig,
) : DefaultLifecycleObserver {
    private lateinit var cameraInitializer: CameraInitializer
    private lateinit var photoCapture: PhotoCapture
    private lateinit var utility: Utility
    private lateinit var focusManager: FocusManager

    private var imageCapture = ImageCapture.Builder().setFlashMode(cameraConfig.flashMode)
        .setJpegQuality(cameraConfig.photoQuality)
        .build()

    fun startCamera() {
        cameraInitializer =
            CameraInitializer(context, owner, cameraPreview, cameraConfig, imageCapture)
        utility = Utility(cameraPreview)
        photoCapture = PhotoCapture(context, imageCapture) { utility.captureSound() }
        focusManager = FocusManager(cameraInitializer, cameraPreview) { utility.focusSound() }
        cameraInitializer.initializeCamera()
        focusManager.setupTouchFocus()
    }


    fun switchLensFacing(lensFacing: Int) {
        updateConfig { it.copy(lensFacing = lensFacing) }
        startCamera()
    }

    fun takePhoto(captureButton: View) {
        photoCapture.takePhoto(captureButton)
    }

    fun setZoomRatio(@FloatRange zoomRatio : Float){
        updateConfig { it.copy(zoomRatio = zoomRatio) }
    }

    private fun updateConfig(update: (CameraConfig) -> CameraConfig) {
        cameraConfig = update(cameraConfig)
    }

    class Builder {
        private var context: Context? = null
        private var owner: LifecycleOwner? = null
        private var cameraPreview: PreviewView? = null
        private var cameraConfig: CameraConfig = CameraConfig()

        fun setContext(context: Context) =
            apply { this.context = context }

        fun setLifecycleOwner(owner: LifecycleOwner) =
            apply { this.owner = owner }

        fun setCameraPreview(cameraPreview: PreviewView) =
            apply { this.cameraPreview = cameraPreview }

        fun setLensFacing(lensFacing: Int) =
            apply { cameraConfig = cameraConfig.copy(lensFacing = lensFacing) }

        fun setFlashMode(flashMode: Int) =
            apply { cameraConfig = cameraConfig.copy(flashMode = flashMode) }

        fun setPhotoQuality(@IntRange(from = 1, to = 100) photoQuality: Int) =
            apply { cameraConfig = cameraConfig.copy(photoQuality = photoQuality) }

        fun setZoomRatio(@FloatRange(from = 0.0, to = 4.0 ) zoomRatio: Float) =
            apply { cameraConfig = cameraConfig.copy(zoomRatio = zoomRatio) }


        // START AN APP
        fun build(): XeroCamera {
            requireNotNull(context) { "Context must be set" }
            requireNotNull(owner) { "LifeCycle owner must be set" }
            requireNotNull(cameraPreview) { "Camera Preview must be set" }
            return XeroCamera(context!!, owner!!, cameraPreview!!, cameraConfig)
        }
    }



    companion object {
        fun builder() = Builder()
    }

    init {
        owner.lifecycle.addObserver(this)
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        Log.e("XeroCamera", "OnResume")
        if (::cameraInitializer.isInitialized) {
            startCamera()
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        Log.e("XeroCamera", "OnPause")
        if (::cameraInitializer.isInitialized) {
            cameraInitializer.shutdownCamera()
            utility.cleanup()
        }
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        Log.e("XeroCamera", "OnDestroy")
        owner.lifecycle.removeObserver(this)
    }
}


//    private val orientationEventListener by lazy {
//      object : OrientationEventListener(context) {
//        override fun onOrientationChanged(orientation: Int) {
//          // Update camera rotation
//        }
//      }
//    }

