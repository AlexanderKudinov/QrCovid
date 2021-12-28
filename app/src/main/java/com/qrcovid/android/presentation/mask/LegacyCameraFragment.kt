package com.qrcovid.android.presentation.mask

import android.content.Context
import android.content.Context.CAMERA_SERVICE
import android.hardware.Camera
import android.hardware.Camera.PreviewCallback
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.Image.Plane
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Trace
import android.util.Size
import android.view.Surface
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import com.qrcovid.mask.utils.ImageUtils
import android.R
import android.graphics.Matrix


//open abstract class LegacyCameraFragment(layoutId: Int) : Fragment(layoutId), OnImageAvailableListener,
//    PreviewCallback, CompoundButton.OnCheckedChangeListener {
//    protected var previewWidth = 0
//    protected var previewHeight = 0
//    val isDebug = false
//    private var handler: Handler? = null
//    private var handlerThread: HandlerThread? = null
//    private var useCamera2API = false
//    private var isProcessingFrame = false
//    private val yuvBytes = arrayOfNulls<ByteArray>(3)
//    private var rgbBytes: IntArray = intArrayOf()
//    protected var luminanceStride = 0
//        private set
//    private var postInferenceCallback: Runnable? = null
//    private var imageConverter: Runnable? = null
//    private var apiSwitchCompat: SwitchCompat? = null
//    protected var cameraFacing: Int? = null
//        private set
//
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        println("zzzzzzzz onCreate $this")
//        cameraFacing = arguments?.getInt(KEY_USE_FACING, CameraCharacteristics.LENS_FACING_BACK)
//    }
//
//    protected fun getRgbBytes(): IntArray? {
//        imageConverter!!.run()
//        return rgbBytes
//    }
//
//    /**
//     * Callback for android.hardware.Camera API
//     */
//    override fun onPreviewFrame(bytes: ByteArray, camera: Camera) {
//        if (isProcessingFrame) {
//            println("zzzzzzz Dropping frame!")
//            return
//        }
//        try {
//            // Initialize the storage bitmaps once when the resolution is known.
//        } catch (e: Exception) {
//            println("zzzzzzz Exception!")
//            return
//        }
//        isProcessingFrame = true
//        yuvBytes[0] = bytes
//        luminanceStride = previewWidth
//        imageConverter = Runnable {
//            ImageUtils.convertYUV420SPToARGB8888(
//                bytes,
//                previewWidth,
//                previewHeight,
//                rgbBytes
//            )
//        }
//        postInferenceCallback = Runnable {
//            camera.addCallbackBuffer(bytes)
//            isProcessingFrame = false
//        }
//        processImage()
//    }
//
//    /**
//     * Callback for Camera2 API
//     */
//    override fun onImageAvailable(reader: ImageReader) {
//        // We need wait until we have some size from onPreviewSizeChosen
//        if (previewWidth == 0 || previewHeight == 0) {
//            return
//        }
//        try {
//            val image = reader.acquireLatestImage() ?: return
//            if (isProcessingFrame) {
//                image.close()
//                return
//            }
//            isProcessingFrame = true
//            Trace.beginSection("imageAvailable")
//            val planes = image.planes
//            fillBytes(planes, yuvBytes)
//            luminanceStride = planes[0].rowStride
//            val uvRowStride = planes[1].rowStride
//            val uvPixelStride = planes[1].pixelStride
//            imageConverter = Runnable {
//                ImageUtils.convertYUV420ToARGB8888(
//                    yuvBytes.getOrNull(0) ?: byteArrayOf(),
//                    yuvBytes.getOrNull(1) ?: byteArrayOf(),
//                    yuvBytes.getOrNull(2) ?: byteArrayOf(),
//                    previewWidth,
//                    previewHeight,
//                    luminanceStride,
//                    uvRowStride,
//                    uvPixelStride,
//                    rgbBytes
//                )
//            }
//            postInferenceCallback = Runnable {
//                image.close()
//                isProcessingFrame = false
//            }
//            processImage()
//        } catch (e: Exception) {
//            println("zzzzzzz Exception!")
//            Trace.endSection()
//            return
//        }
//        Trace.endSection()
//    }
//
//    @Synchronized
//    override fun onStart() {
//        println("zzzzzzzzz onStart $this")
//        super.onStart()
//    }
//
//    @Synchronized
//    override fun onResume() {
//        println("zzzzzzzz onResume $this")
//        super.onResume()
//        handlerThread = HandlerThread("inference")
//        handlerThread!!.start()
//        handler = Handler(handlerThread!!.looper)
//    }
//
//    @Synchronized
//    override fun onPause() {
//        println("zzzzzzz onPause $this")
//        handlerThread!!.quitSafely()
//        try {
//            handlerThread!!.join()
//            handlerThread = null
//            handler = null
//        } catch (e: InterruptedException) {
//            println("zzzzzzz Exception!")
//        }
//        super.onPause()
//    }
//
//    @Synchronized
//    protected fun runInBackground(r: Runnable?) {
//        if (handler != null) {
//            handler!!.post(r!!)
//        }
//    }
//
//    // Returns true if the device supports the required hardware level, or better.
//    private fun isHardwareLevelSupported(
//        characteristics: CameraCharacteristics, requiredLevel: Int
//    ): Boolean {
//        val deviceLevel: Int =
//            characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)!!
//        return if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
//            requiredLevel == deviceLevel
//        } else requiredLevel <= deviceLevel
//        // deviceLevel is not LEGACY, can use numerical sort
//    }
//
//    private fun chooseCamera(): String? {
//        val manager = requireContext().getSystemService(CAMERA_SERVICE) as CameraManager
//        try {
//            for (cameraId in manager.cameraIdList) {
//                val characteristics = manager.getCameraCharacteristics(cameraId)
//                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
//                if (cameraFacing != null && facing != null &&
//                    facing != cameraFacing
//                ) {
//                    continue
//                }
//                useCamera2API = (facing == CameraCharacteristics.LENS_FACING_EXTERNAL
//                        || isHardwareLevelSupported(
//                    characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL
//                ))
//                println("zzzzzzz Camera API lv2?: $useCamera2API")
//                return cameraId
//            }
//        } catch (e: CameraAccessException) {
//            println("zzzzzzz Not allowed to access camera")
//        }
//        return null
//    }
//
//    private fun fillBytes(planes: Array<Plane>, yuvBytes: Array<ByteArray?>) {
//        // Because of the variable row stride it's not possible to know in
//        // advance the actual necessary dimensions of the yuv planes.
//        for (i in planes.indices) {
//            val buffer = planes[i].buffer
//            if (yuvBytes[i] == null) {
//                println("zzzzzzzz Initializing buffer $i at size ${buffer.capacity()}")
//                yuvBytes[i] = ByteArray(buffer.capacity())
//            }
//            buffer[yuvBytes[i]]
//        }
//    }
//
//    protected fun readyForNextImage() {
//        if (postInferenceCallback != null) {
//            postInferenceCallback!!.run()
//        }
//    }
//
//    protected val screenOrientation: Int
//        protected get() = when (requireActivity().windowManager.defaultDisplay.rotation) {
//            Surface.ROTATION_270 -> 270
//            Surface.ROTATION_180 -> 180
//            Surface.ROTATION_90 -> 90
//            else -> 0
//        }
//
//    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
//        setUseNNAPI(isChecked)
//        if (isChecked) apiSwitchCompat!!.text = "NNAPI" else apiSwitchCompat!!.text = "TFLITE"
//    }
//
//    protected open fun getOrientation(): Int {
//        return when (requireActivity().windowManager.defaultDisplay.rotation) {
//            Surface.ROTATION_270 -> 270
//            Surface.ROTATION_180 -> 180
//            Surface.ROTATION_90 -> 90
//            else -> 0
//        }
//    }
//
//
//    protected open fun setFragment() {
//        val cameraId = chooseCamera()
//        val fragment: Fragment
//        if (useCamera2API) {
//            val camera2Fragment: CameraConnectionFragment = CameraConnectionFragment.newInstance(
//                object : ConnectionCallback() {
//                    fun onPreviewSizeChosen(size: Size, rotation: Int) {
//                        previewHeight = size.height
//                        previewWidth = size.width
//                        this@CameraActivity.onPreviewSizeChosen(size, rotation)
//                    }
//                },
//                this,
//                getLayoutId(),
//                getDesiredPreviewFrameSize()
//            )
//            camera2Fragment.setCamera(cameraId)
//            fragment = camera2Fragment
//        } else {
//            val facing = if (cameraFacing == CameraCharacteristics.LENS_FACING_BACK) {
//                Camera.CameraInfo.CAMERA_FACING_BACK
//            } else {
//                Camera.CameraInfo.CAMERA_FACING_FRONT
//            }
//            val frag = LegacyCameraConnectionFragment(
//                this,
//                getLayoutId(),
//                getDesiredPreviewFrameSize(), facing
//            )
//            fragment = frag
//        }
//        fragmentManager!!.beginTransaction().replace(R.id.container, fragment).commit()
//    }
//
//
//
//    protected abstract fun processImage()
//    protected abstract fun onPreviewSizeChosen(size: Size, rotation: Int)
//
//    protected abstract fun setNumThreads(numThreads: Int)
//    protected abstract fun setUseNNAPI(isChecked: Boolean)
//
//    companion object {
//        private const val KEY_USE_FACING = "use_facing"
//    }
//}