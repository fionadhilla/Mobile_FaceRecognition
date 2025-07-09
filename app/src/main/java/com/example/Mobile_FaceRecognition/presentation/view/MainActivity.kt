package com.example.Mobile_FaceRecognition.presentation.view


import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Typeface
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.util.TypedValue
import android.view.Surface
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.Mobile_FaceRecognition.presentation.view.visualization.BorderedText
import com.example.Mobile_FaceRecognition.presentation.view.visualization.MultiBoxTracker
import com.example.Mobile_FaceRecognition.presentation.view.visualization.OverlayView
import com.example.Mobile_FaceRecognition.presentation.view.camera.CameraConnectionFragment
import com.example.Mobile_FaceRecognition.R
import com.example.Mobile_FaceRecognition.DependencyInjector.AppModule
import com.example.Mobile_FaceRecognition.presentation.utils.FaceDetectorWrapper
import com.example.Mobile_FaceRecognition.presentation.viewmodel.MainViewModel
import com.example.Mobile_FaceRecognition.presentation.viewmodel.UiEvent
import com.example.Mobile_FaceRecognition.utils.ImageUtils
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), ImageReader.OnImageAvailableListener {

    // ─────────── konfigurasi umum ───────────
    private val TEXT_SIZE_DIP = 10f
    private val CROP_SIZE = 160 // Ukuran input untuk model TFLite FaceNet
    private val KEY_USE_FACING = "use_facing"

    // ─────────── kamera & tampilan ───────────
    private var useFacing: Int = CameraCharacteristics.LENS_FACING_FRONT
    private lateinit var cameraFragment: CameraConnectionFragment
    private lateinit var trackingOverlay: OverlayView
    private lateinit var tracker: MultiBoxTracker

    private var previewWidth = 0
    private var previewHeight = 0
    private var sensorOrientation = 0
    private var isFrontCameraActive = true

    // ─────────── bitmap buffer ───────────
    private var rgbBytes: IntArray? = null
    private var yuvBytes: Array<ByteArray?> = arrayOfNulls(3)
    private var yRowStride: Int = 0
    private lateinit var rgbFrameBitmap: Bitmap
    private lateinit var croppedBitmap: Bitmap
    private lateinit var frameToCropTransform: Matrix
    private lateinit var cropToFrameTransform: Matrix

    private var isProcessingFrame = false
    private lateinit var imageConverter: Runnable
    private lateinit var postInferenceCallback: Runnable

    // ─────────── ML Kit & model ───────────
    private lateinit var faceDetectorWrapper: FaceDetectorWrapper

    // ─────────── ViewModel ───────────
    private lateinit var viewModel: MainViewModel

    // ─────────── Android ───────────
    private val handler = Handler(Looper.getMainLooper())
    private var recognitionsObserverAttached = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ── inisialisasi ML Kit detector ──
        faceDetectorWrapper = AppModule.provideFaceDetector(this)

        // ── ViewModel ──
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(@NonNull model: Class<T>): T {
                val tfliteSrc = AppModule.provideTFLiteFaceRecognitionSource(applicationContext)
                val ws = AppModule.provideFaceRecognitionWebSocketService(object :
                    com.example.Mobile_FaceRecognition.data.remote.FaceRecognitionWebSocketService.WebSocketEventListener {
                    override fun onConnected() {}
                    override fun onRecognitionResult(match: Boolean, name: String, distance: Double) {}
                    override fun onMessageReceived(message: String) {}
                    override fun onError(message: String) {}
                })
                val repo = AppModule.provideFaceRecognitionRepository(tfliteSrc, ws)
                val recUseCase = AppModule.provideRecognizeFaceUseCase(repo)
                val regUseCase = AppModule.provideRegisterFaceUseCase(repo)

                return MainViewModel(recUseCase, regUseCase, faceDetectorWrapper.raw(), repo, CROP_SIZE) as T
            }
        })[MainViewModel::class.java]

        tracker = MultiBoxTracker(this)

        // ── permission kamera ──
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.CAMERA), 121)
            } else {
                setFragment() // permission sudah ada
            }
        } else {
            setFragment()
        }

        // ── tombol register & switch ──
        findViewById<View>(R.id.imageView4).setOnClickListener { viewModel.isRegisteringFace = true }
        findViewById<View>(R.id.imageView3).setOnClickListener { switchCamera() }

        // ── observe UI events ──
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.uiEvent.collect { e ->
                    when (e) {
                        is UiEvent.ShowToast ->
                            Toast.makeText(this@MainActivity, e.message, Toast.LENGTH_SHORT).show()
                        is UiEvent.ShowRegistrationDialog ->
                            registerFaceDialog(e.croppedFace, e.recognition)
                        UiEvent.Idle -> {}
                    }
                }
            }
        }
    }

    private fun setFragment() {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = manager.cameraIdList.firstOrNull { id ->
            manager.getCameraCharacteristics(id)
                .get(CameraCharacteristics.LENS_FACING) == useFacing
        } ?: run {
            Toast.makeText(this, "No camera found.", Toast.LENGTH_LONG).show(); return
        }

        cameraFragment = CameraConnectionFragment.newInstance(
            callback = object : CameraConnectionFragment.ConnectionCallback {
                override fun onPreviewSizeChosen(
                    size: Size,
                    rotation: Int,
                    overlay: OverlayView,
                    isFrontFacing: Boolean
                ) {
                    previewHeight = size.height
                    previewWidth = size.width
                    sensorOrientation = (rotation + 270) % 360 
                    isFrontCameraActive = isFrontFacing

                    val textSizePx = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, resources.displayMetrics
                    )
                    val borderedText = BorderedText(textSizePx).apply {
                        setTypeface(Typeface.MONOSPACE)
                    }

                    rgbFrameBitmap = Bitmap.createBitmap(
                        previewWidth, previewHeight, Bitmap.Config.ARGB_8888
                    )
                    croppedBitmap = Bitmap.createBitmap(CROP_SIZE, CROP_SIZE, Bitmap.Config.ARGB_8888)
                    frameToCropTransform = ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        CROP_SIZE, CROP_SIZE,
                        sensorOrientation, /*maintainAspectRatio=*/ true
                    )
                    cropToFrameTransform = Matrix().also { frameToCropTransform.invert(it) }

                    trackingOverlay = overlay
                    trackingOverlay.addCallback { canvas -> tracker.draw(canvas) }
                    tracker.setFrameConfiguration(
                        previewWidth, previewHeight, sensorOrientation, isFrontCameraActive
                    )

                    Log.d("DBG", "preview Width = $previewWidth")
                    Log.d("DBG", "preview Height = $previewHeight")

                    if (!recognitionsObserverAttached) {
                        viewModel.mappedRecognitions.observe(this@MainActivity) { recognitions ->
                            Log.d("DBG", "processResults size=${recognitions.size}")
                            tracker.trackResults(recognitions, System.currentTimeMillis())
                            trackingOverlay.postInvalidate()
                        }
                        recognitionsObserverAttached = true
                    }
                }
            },
            imageListener = this,
            layout = R.layout.camera_fragment,
            inputSize = Size(640, 480) // This is the preferred preview size from the camera, not the model input size.
        ).apply { setCamera(cameraId) }

        supportFragmentManager.beginTransaction()
            .replace(R.id.container, cameraFragment)
            .commit()
    }

    private fun switchCamera() {
        val old = faceDetectorWrapper.recreate()
        viewModel.updateDetector(faceDetectorWrapper.raw())
        old.close()
        useFacing = if (useFacing == CameraCharacteristics.LENS_FACING_FRONT) CameraCharacteristics.LENS_FACING_BACK else CameraCharacteristics.LENS_FACING_FRONT
        cameraFragment.toggleCamera()
    }

    override fun onImageAvailable(reader: ImageReader) {
        if (previewWidth == 0 || previewHeight == 0) {
            reader.acquireLatestImage()?.close(); return
        }
        val image = reader.acquireLatestImage() ?: return
        if (isProcessingFrame) { image.close(); return }

        isProcessingFrame = true

        if (rgbBytes == null) rgbBytes = IntArray(previewWidth * previewHeight)
        val planes = image.planes
        ImageUtils.fillBytes(planes, yuvBytes)
        yRowStride = planes[0].rowStride
        val uvRowStride = planes[1].rowStride
        val uvPixelStride = planes[1].pixelStride

        imageConverter = Runnable {
            ImageUtils.convertYUV420ToARGB8888(
                yuvBytes[0]!!, yuvBytes[1]!!, yuvBytes[2]!!,
                previewWidth, previewHeight,
                yRowStride, uvRowStride, uvPixelStride, rgbBytes!!
            )
        }

        postInferenceCallback = Runnable {
            image.close()
            isProcessingFrame = false
        }

        imageConverter.run()
        rgbFrameBitmap.setPixels(rgbBytes!!, 0, previewWidth, 0, 0, previewWidth, previewHeight)

        // Crop ke ukuran model dan terapkan rotasi sensor
        Canvas(croppedBitmap).drawBitmap(rgbFrameBitmap, frameToCropTransform, null)

        // Proses via ViewModel (akan memanggil FaceDetectorWrapper.process)
        viewModel.processImageForDetectionAndRecognition(
            croppedBitmap,
            previewWidth,
            previewHeight,
            frameToCropTransform,
            cropToFrameTransform,
            isFrontCameraActive,
            sensorOrientation
        )

        postInferenceCallback.run()
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        req: Int, @NonNull perms: Array<String>, @NonNull results: IntArray
    ) {
        if (req == 121 && results.isNotEmpty() && results[0] == PackageManager.PERMISSION_GRANTED) {
            setFragment()
        } else {
            Toast.makeText(this, "Camera permission is required.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun registerFaceDialog(face: Bitmap, rec: com.example.Mobile_FaceRecognition.domain.model.FaceRecognition) {
        val dlg = Dialog(this).apply { setContentView(R.layout.register_face_dialogue) }
        dlg.findViewById<ImageView>(R.id.dlg_image).setImageBitmap(face)
        val nameEd = dlg.findViewById<EditText>(R.id.dlg_input)
        dlg.findViewById<Button>(R.id.button2).setOnClickListener {
            val name = nameEd.text.toString()
            if (name.isBlank()) { nameEd.error = "Enter name"; return@setOnClickListener }
            rec.embedding?.let { viewModel.registerFace(name, it) }
            dlg.dismiss()
        }
        dlg.show()
    }

    override fun onDestroy() {
        faceDetectorWrapper.close()
        super.onDestroy()
    }
}