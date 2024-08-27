package com.xero.xerocamera.CameraModule

import android.content.Context
import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.xero.xerocamera.Models.CameraConfig

class CameraInitializer(
    private val context: Context,
    private val owner: LifecycleOwner,
    private val cameraPreview: PreviewView,
    private val cameraConfig: CameraConfig,
    private val imageCapture: ImageCapture,
) {
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera

    fun initializeCamera() {
        ProcessCameraProvider.getInstance(context).also { it ->
            it.addListener({
                cameraProvider = it.get()
                try {
                    shutdownCamera()
                    bindCamera(imageCapture)
                } catch (e: Exception) {
                    Log.e("Xero Builder", "Use Case binding failed $e")
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }

    fun shutdownCamera() {
        cameraProvider.unbindAll()
    }

    private fun bindCamera(vararg useCase: UseCase) {
        camera = cameraProvider.bindToLifecycle(
            owner,
            bindCameraSelector(),
            bindPreview(),
            *useCase
        )
        setZoom(cameraConfig.zoomRatio)
    }

    fun getCamera(): Camera {
        return camera
    }

    private fun bindCameraSelector(): CameraSelector {
        return CameraSelector.Builder().also {
            it.requireLensFacing(cameraConfig.lensFacing)
        }.build()
    }

    private fun bindPreview(): Preview {
        return Preview.Builder().build().also {
            it.setSurfaceProvider(cameraPreview.surfaceProvider)
        }
    }

    private fun bindVideoCapture(): VideoCapture<Recorder> {
        return VideoCapture.withOutput(
            Recorder.Builder().also {
                it.setQualitySelector(
                    QualitySelector.from(
                        Quality.UHD,
                        FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
                    )
                )
                it.setAspectRatio(AspectRatio.RATIO_16_9)
                it.setTargetVideoEncodingBitRate(5000000)
            }.build()
        )
    }

    private fun setZoom(zoomRatio: Float) {
        camera.cameraControl.setZoomRatio(zoomRatio)
    }

}