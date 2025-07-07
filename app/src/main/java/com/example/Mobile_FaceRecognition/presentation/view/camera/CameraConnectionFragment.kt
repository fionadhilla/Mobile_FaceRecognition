package com.example.Mobile_FaceRecognition.presentation.view.camera

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.DialogFragment
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.hardware.camera2.params.StreamConfigurationMap
import android.media.ImageReader
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.example.Mobile_FaceRecognition.R
import com.example.Mobile_FaceRecognition.presentation.view.visualization.OverlayView
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.Comparator
import java.util.List
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit

/**
 * Camera Connection Fragment that captures images from camera.
 *
 * <p>Instantiated by newInstance.</p>
 */
@SuppressLint("ValidFragment")
@Suppress("FragmentNotInstantiable")
class CameraConnectionFragment private constructor(
    private val cameraConnectionCallback: ConnectionCallback,
    private val imageListener: ImageReader.OnImageAvailableListener,
    private val layout: Int,
    private val inputSize: Size,
    private var isSessionCreated: Boolean = false

) : Fragment() {

    private val cameraOpenCloseLock = Semaphore(1)

    private val captureCallback: CameraCaptureSession.CaptureCallback =
        object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureProgressed(
                session: CameraCaptureSession,
                request: CaptureRequest,
                partialResult: CaptureResult
            ) {
            }

            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                totalResult: TotalCaptureResult
            ) {
            }
        }

    private var cameraId: String? = null
    private lateinit var textureView: AutoFitTextureView
    private var trackingOverlayFragment: OverlayView? = null
    private var captureSession: CameraCaptureSession? = null
    private var cameraDevice: CameraDevice? = null
    private var sensorOrientation: Int? = null
    private var previewSize: Size? = null
    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null
    private var frontId: String? = null
    private var backId: String? = null

    private val surfaceTextureListener: TextureView.SurfaceTextureListener =
        object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                texture: SurfaceTexture, width: Int, height: Int
            ) {
                openCamera(width, height)
            }

            override fun onSurfaceTextureSizeChanged(
                texture: SurfaceTexture, width: Int, height: Int
            ) {
                configureTransform(width, height)
            }

            override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
                return true
            }

            override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {}
        }
    private var previewReader: ImageReader? = null
    private lateinit var previewRequestBuilder: CaptureRequest.Builder
    private lateinit var previewRequest: CaptureRequest
    private val stateCallback: CameraDevice.StateCallback =
        object : CameraDevice.StateCallback() {
            override fun onOpened(cd: CameraDevice) {
                cameraOpenCloseLock.release()
                cameraDevice = cd
                if (!isSessionCreated && cameraDevice != null) {
                    isSessionCreated = true
                    createCameraPreviewSession()
                } else {
                    showToast("Session already created or camera closed.")
                }
            }

            override fun onDisconnected(cd: CameraDevice) {
                cameraOpenCloseLock.release()
                cd.close()
                cameraDevice = null
            }

            override fun onError(cd: CameraDevice, error: Int) {
                cameraOpenCloseLock.release()
                cd.close()
                cameraDevice = null
                val activity = activity
                activity?.finish()
            }
        }

    companion object {
        private const val MINIMUM_PREVIEW_SIZE = 320
        private val ORIENTATIONS = SparseIntArray()
        private const val FRAGMENT_DIALOG = "dialog"

        init {
            ORIENTATIONS.append(Surface.ROTATION_0, 90)
            ORIENTATIONS.append(Surface.ROTATION_90, 0)
            ORIENTATIONS.append(Surface.ROTATION_180, 270)
            ORIENTATIONS.append(Surface.ROTATION_270, 180)
        }

        /**
         * Given `choices` of `Size`s supported by a camera, chooses the smallest one whose
         * width and height are at least as large as the minimum of both, or an exact match if possible.
         *
         * @param choices The list of sizes that the camera supports for the intended output class
         * @param width The minimum desired width
         * @param height The minimum desired height
         * @return The optimal `Size`, or an arbitrary one if none were big enough
         */
        protected fun chooseOptimalSize(choices: Array<Size>, width: Int, height: Int): Size {
            val minSize = Math.max(Math.min(width, height), MINIMUM_PREVIEW_SIZE)
            val desiredSize = Size(width, height)

            // Collect the supported resolutions that are at least as big as the preview Surface
            var exactSizeFound = false
            val bigEnough: MutableList<Size> = ArrayList()
            val tooSmall: MutableList<Size> = ArrayList()
            for (option in choices) {
                if (option == desiredSize) {
                    // Set the size but don't return yet so that remaining sizes will still be logged.
                    exactSizeFound = true
                }

                if (option.height >= minSize && option.width >= minSize) {
                    bigEnough.add(option)
                } else {
                    tooSmall.add(option)
                }
            }

            if (exactSizeFound) {
                return desiredSize
            }

            // Pick the smallest of those, assuming we found any
            return if (bigEnough.size > 0) {
                val chosenSize = Collections.min(bigEnough, CompareSizesByArea())
                // LOGGER.i("Chosen size: " + chosenSize.getWidth() + "x" + chosenSize.getHeight());
                chosenSize
            } else {
                // LOGGER.e("Couldn't find any suitable preview size");
                choices[0]
            }
        }

        @JvmStatic
        fun newInstance(
            callback: ConnectionCallback,
            imageListener: ImageReader.OnImageAvailableListener,
            layout: Int,
            inputSize: Size
        ): CameraConnectionFragment {
            return CameraConnectionFragment(callback, imageListener, layout, inputSize)
        }
    }

    private fun showToast(text: String) {
        val activity = activity
        activity?.runOnUiThread { Toast.makeText(activity, text, Toast.LENGTH_SHORT).show() }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        textureView = view.findViewById(R.id.texture)
        trackingOverlayFragment = view.findViewById(R.id.tracking_overlay)
    }

    fun toggleCamera() {
        cameraId = if (cameraId == backId) frontId else backId
        closeCamera()
        openCamera(textureView.width, textureView.height)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        startBackgroundThread()

        if (textureView.isAvailable) {
            openCamera(textureView.width, textureView.height)
        } else {
            textureView.surfaceTextureListener = surfaceTextureListener
        }
    }

    override fun onPause() {
        closeCamera()
        stopBackgroundThread()
        super.onPause()
    }

    fun setCamera(cameraId: String) {
        this.cameraId = cameraId
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun setUpCameraOutputs() {
        val activity = activity ?: return
        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager

        if (frontId == null || backId == null) {
            for (id in manager.cameraIdList) {
                val facing = manager.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) frontId = id
                if (facing == CameraCharacteristics.LENS_FACING_BACK) backId = id
            }
        }

        val characteristics = manager.getCameraCharacteristics(cameraId!!)

        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            ?: throw RuntimeException("Cannot get available preview/video sizes")
        sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0

        previewSize = chooseOptimalSize(
            map.getOutputSizes(SurfaceTexture::class.java),
            inputSize.width, inputSize.height
        )

        // Rasio untuk TextureView
        val orientation = resources.configuration.orientation
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            textureView.setAspectRatio(previewSize!!.width, previewSize!!.height)
        } else {
            textureView.setAspectRatio(previewSize!!.height, previewSize!!.width)
        }

        val isFront = characteristics.get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_FRONT

        cameraConnectionCallback.onPreviewSizeChosen(
            previewSize!!, sensorOrientation!!, trackingOverlayFragment!!, isFront
        )
    }

    @SuppressLint("MissingPermission")
    private fun openCamera(width: Int, height: Int) {
        setUpCameraOutputs()
        configureTransform(width, height)
        val activity = activity ?: return
        val manager = activity.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw RuntimeException("Time out waiting to lock camera opening.")
            }
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            manager.openCamera(cameraId!!, stateCallback, backgroundHandler)
        } catch (e: CameraAccessException) {
            // LOGGER.e(e, "Exception!");
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera opening.", e)
        }
    }

    private fun closeCamera() {
        try {
            cameraOpenCloseLock.acquire()
            if (captureSession != null) {
                captureSession!!.close()
                captureSession = null
            }
            if (cameraDevice != null) {
                cameraDevice!!.close()
                cameraDevice = null
            }
            if (previewReader != null) {
                previewReader!!.close()
                previewReader = null
            }
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted while trying to lock camera closing.", e)
        } finally {
            cameraOpenCloseLock.release()
        }
        isSessionCreated = false
    }

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("ImageListener").also { it.start() }
        backgroundHandler = Handler(backgroundThread!!.looper)
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            //    LOGGER.e(e, "Exception!");
        }
    }

    private fun createCameraPreviewSession() {
        try {
            if (cameraDevice == null) {
                showToast("cameraDevice is null. Skip creating session.")
                return
            }

            val texture = textureView.surfaceTexture
            texture?.setDefaultBufferSize(previewSize!!.width, previewSize!!.height)

            val surface = Surface(texture)

            previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewRequestBuilder.addTarget(surface)

            previewReader = ImageReader.newInstance(
                previewSize!!.width, previewSize!!.height, ImageFormat.YUV_420_888, 2
            )

            previewReader!!.setOnImageAvailableListener(imageListener, backgroundHandler)
            previewRequestBuilder.addTarget(previewReader!!.surface)

            cameraDevice!!.createCaptureSession(
                Arrays.asList(surface, previewReader!!.surface),
                object : CameraCaptureSession.StateCallback() {

                    override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                        if (cameraDevice == null) {
                            return
                        }

                        captureSession = cameraCaptureSession
                        try {
                            previewRequestBuilder.set(
                                CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                            )
                            previewRequestBuilder.set(
                                CaptureRequest.CONTROL_AE_MODE,
                                CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                            )

                            previewRequest = previewRequestBuilder.build()
                            captureSession!!.setRepeatingRequest(
                                previewRequest, captureCallback, backgroundHandler
                            )
                        } catch (e: CameraAccessException) {
                            //       LOGGER.e(e, "Exception!");
                        }
                    }

                    override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
                        showToast("Failed")
                    }
                },
                null
            )
        } catch (e: CameraAccessException) {
            //        LOGGER.e(e, "Exception!");
        }
    }

    private fun configureTransform(viewWidth: Int, viewHeight: Int) {
        val activity = activity ?: return
        if (previewSize == null) {
            return
        }
        val rotation = activity.windowManager.defaultDisplay.rotation
        val matrix = Matrix()
        val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize!!.height.toFloat(), previewSize!!.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
            val scale = Math.max(
                viewHeight.toFloat() / previewSize!!.height,
                viewWidth.toFloat() / previewSize!!.width
            )
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate(90f * (rotation - 2), centerX, centerY)
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }
        textureView.setTransform(matrix)
    }

    /**
     * Callback for Activities to use to initialize their data once the selected preview size is
     * known.
     */
    interface ConnectionCallback {
        fun onPreviewSizeChosen(size: Size, cameraRotation: Int, overlayView: OverlayView, isFrontFacing: Boolean)
    }

    /** Compares two [Size]s based on their areas. */
    internal class CompareSizesByArea : Comparator<Size> {
        override fun compare(lhs: Size, rhs: Size): Int {
            // We cast here to ensure the multiplications won't overflow
            return java.lang.Long.signum(
                lhs.width.toLong() * lhs.height - rhs.width.toLong() * rhs.height
            )
        }
    }

    /** Shows an error message dialog. */
    class ErrorDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val activity = activity
            return AlertDialog.Builder(activity)
                .setMessage(arguments?.getString(ARG_MESSAGE))
                .setPositiveButton(
                    android.R.string.ok
                ) { dialogInterface: DialogInterface?, i: Int -> activity?.finish() }
                .create()
        }

        companion object {
            private const val ARG_MESSAGE = "message"

            fun newInstance(message: String?): ErrorDialog {
                val dialog = ErrorDialog()
                val args = Bundle()
                args.putString(ARG_MESSAGE, message)
                dialog.arguments = args
                return dialog
            }
        }
    }
}