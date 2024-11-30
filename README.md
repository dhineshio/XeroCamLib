[![](https://jitpack.io/v/dhineshio/XeroCamLib.svg)](https://jitpack.io/#dhineshio/XeroCamLib)

# XeroCam Library

XeroCam is a versatile camera library designed for Android applications, offering features like photo and video capture, barcode scanning, and extensive customization options. This README provides an overview of the library and instructions for integrating it into your project.

---

## Features

- **Photo Capture**: Capture high-resolution photos with custom directory and file naming options.
- **Video Recording**: Record videos with support for pausing and resuming.
- **Barcode Scanning**: Enable barcode scanning with animated overlays.
- **Customizable Camera Controls**: Zoom, flash, and camera lens toggling.
- **Lifecycle Management**: Seamlessly integrates with Android's lifecycle components.

---

## Installation

To integrate the XeroCam library into your project, add the following dependency to your `build.gradle`:

```groovy
dependencies {
    implementation 'com.xero:xerocamera:1.0.0'
}
```

---

## Usage

### 1. Initialize XeroCam

To initialize XeroCam, use the `Builder` class:

```kotlin
val xeroCamera = XeroCamera.builder()
    .setContext(this)
    .setActivity(this)
    .setLifecycleOwner(this)
    .setCameraPreview(binding.previewView) // Pass your PreviewView
    .enableScanner(isScanner = true, isLiveScan = false) // Optional
    .setLensFacing(CameraSelector.LENS_FACING_BACK)
    .build()
```

### 2. Start the Camera

To start the camera, call:

```kotlin
xeroCamera.startCamera()
```

### 3. Capture Photo

Use the `takePhoto` function to capture a photo:

```kotlin
xeroCamera.takePhoto(
    onSuccess = { imagePath ->
        Log.d("PhotoCapture", "Photo saved at: $imagePath")
    },
    onFailure = { exception ->
        Log.e("PhotoCapture", "Error capturing photo", exception)
    },
    enableSound = true,
    useCache = false,
    directoryName = "MyPhotos",
    fileName = "photo",
    fileNameType = FileNameType.TIMESTAMP,
    subDirectoryName = "SubFolder"
)
```

### 4. Record Video

Start and stop video recording as follows:

```kotlin
xeroCamera.startVideo(
    onStart = {
        Log.d("VideoCapture", "Recording started")
    },
    onSuccess = { videoPath ->
        Log.d("VideoCapture", "Video saved at: $videoPath")
    },
    onError = {
        Log.e("VideoCapture", "Error recording video")
    },
    timerView = binding.timerView, // Optional timer TextView
    directoryName = "MyVideos",
    fileName = "video",
    fileNameType = FileNameType.TIMESTAMP,
    subDirectoryName = "SubFolder"
)

xeroCamera.stopVideo()
```

### 5. Enable Barcode Scanning

Enable or disable the barcode scanner dynamically:

```kotlin
xeroCamera.enableScanner(true)
```

Retrieve scanned barcodes using:

```kotlin
xeroCamera.scannerBarcode.observe(this) { barcode ->
    Log.d("Barcode", "Scanned: $barcode")
}
```

### 6. Adjust Camera Settings

- **Zoom Control**:

```kotlin
xeroCamera.setZoomRatio(2.0f) // Zoom level between 0.0 and 4.0
```

- **Flash Mode**:

```kotlin
xeroCamera.setFlashMode(FlashMode.AUTO)
```

- **SeekBar Zoom**:

```kotlin
xeroCamera.setSeekBarZoom(
    seekBar = binding.zoomSeekBar,
    onStart = { Log.d("Zoom", "Started zooming") },
    onStop = { Log.d("Zoom", "Stopped zooming") }
)
```

---

## Customization

- **Scanner Overlay**: Customize the barcode scanner overlay by modifying the `ScannerOverlay` class.
- **File Naming**: Use `FileNameType` to define naming conventions (e.g., `TIMESTAMP`, `UUID`).

---

## Lifecycle Management

The library automatically manages the camera lifecycle. Ensure to call the respective lifecycle methods:

- `onResume`
- `onPause`
- `onDestroy`

Example:

```kotlin
override fun onResume() {
    super.onResume()
    xeroCamera.onResume(this)
}

override fun onPause() {
    super.onPause()
    xeroCamera.onPause(this)
}

override fun onDestroy() {
    super.onDestroy()
    xeroCamera.onDestroy(this)
}
```

---

## Contributions

Contributions are welcome! Please fork the repository, create a feature branch, and submit a pull request.

---

## License

This library is licensed under the MIT License. See the LICENSE file for details.

