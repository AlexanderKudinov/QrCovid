package com.qrcovid.android.presentation.mask

import android.graphics.*
import android.graphics.Bitmap.Config
import android.graphics.Paint.Style
import android.hardware.camera2.CameraCharacteristics
import android.os.Bundle
import android.os.SystemClock
import android.util.Size
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import com.google.android.gms.tasks.OnSuccessListener
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.qrcovid.android.R
import com.qrcovid.android.databinding.FragmentMaskScanningBinding
import com.qrcovid.android.presentation.main.IMaskScanning
import com.qrcovid.mask.presentation.BorderedText
import com.qrcovid.mask.presentation.OverlayView
import com.qrcovid.mask.presentation.OverlayView.DrawCallback
import com.qrcovid.mask.utils.*
import com.qrcovid.mask.utils.Classifier.Recognition
import java.io.IOException


//class LegacyMaskScanningFragment: LegacyCameraFragment(R.layout.fragment_mask_scanning) {
//
//    var trackingOverlay: OverlayView? = null
//    private var sensorOrientation: Int? = null
//
//    private var detector: Classifier? = null
//
//    private var lastProcessingTimeMs: Long = 0
//    private var rgbFrameBitmap: Bitmap? = null
//    private var croppedBitmap: Bitmap? = null
//
//    private var computingDetection = false
//
//    private var timestamp: Long = 0
//
//    private var frameToCropTransform: Matrix? = null
//    private var cropToFrameTransform: Matrix? = null
//
//    private var tracker: MultiBoxTracker? = null
//
//    private var borderedText: BorderedText? = null
//
//    // Face detector
//    private var faceDetector: FaceDetector? = null
//
//    // here the preview image is drawn in portrait way
//    private var portraitBmp: Bitmap? = null
//
//    // here the face is cropped and drawn
//    private var faceBmp: Bitmap? = null
//
//    private var _binding: FragmentMaskScanningBinding? = null
//    private val binding get() = _binding!!
//    private val parent by lazy { requireActivity() as IMaskScanning }
//
//
//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//        _binding = FragmentMaskScanningBinding.bind(view)
//        createDetector()
//    }
//
//    override fun onDestroyView() {
//        super.onDestroyView()
//        _binding = null
//    }
//
//
//
//    private fun createDetector() {
//        // Real-time contour detection of multiple faces
//        val options = FaceDetectorOptions.Builder()
//            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
//            .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
//            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
//            .build()
//
//        faceDetector = FaceDetection.getClient(options)
//    }
//
//    override fun onPreviewSizeChosen(size: Size, rotation: Int) {
//        val textSizePx = TypedValue.applyDimension(
//            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, resources.displayMetrics
//        )
//        borderedText = BorderedText(textSize = textSizePx)
//        borderedText?.setTypeface(Typeface.MONOSPACE)
//        tracker = MultiBoxTracker(requireContext())
//        try {
//            detector = TFLiteObjectDetectionAPIModel.create(
//                requireContext().assets,
//                TF_OD_API_MODEL_FILE,
//                TF_OD_API_LABELS_FILE,
//                TF_OD_API_INPUT_SIZE,
//                TF_OD_API_IS_QUANTIZED
//            )
//        } catch (e: IOException) {
//            e.printStackTrace()
//            println("zzzzzzzz Exception initializing classifier!")
//            Toast.makeText(requireContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT).show()
//            parent.onMaskFailure()
//        }
//        previewWidth = size.width
//        previewHeight = size.height
//        val screenOrientation = getOrientation()
//        sensorOrientation = rotation - screenOrientation
//        println("zzzzzzz Camera orientation relative to screen canvas: $sensorOrientation")
//        println("zzzzzzzz Initializing at size ${previewWidth}x${previewHeight}")
//        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888)
//        val targetW: Int
//        val targetH: Int
//        if (sensorOrientation == 90 || sensorOrientation == 270) {
//            targetH = previewWidth
//            targetW = previewHeight
//        } else {
//            targetW = previewWidth
//            targetH = previewHeight
//        }
//        val cropW = (targetW / 2.0).toInt()
//        val cropH = (targetH / 2.0).toInt()
//        croppedBitmap = Bitmap.createBitmap(cropW, cropH, Config.ARGB_8888)
//        portraitBmp = Bitmap.createBitmap(targetW, targetH, Config.ARGB_8888)
//        faceBmp = Bitmap.createBitmap(TF_OD_API_INPUT_SIZE, TF_OD_API_INPUT_SIZE, Config.ARGB_8888)
//        frameToCropTransform = ImageUtils.getTransformationMatrix(
//            previewWidth, previewHeight,
//            cropW, cropH,
//            sensorOrientation ?: 0, MAINTAIN_ASPECT
//        )
//
//        cropToFrameTransform = Matrix().apply {
//            invert(cropToFrameTransform)
//        }
//
//        binding.trackingOverlay.addCallback(
//            object : DrawCallback {
//                override fun drawCallback(canvas: Canvas?) {
//                    canvas ?: return
//                    tracker?.draw(canvas)
//                    if (isDebug) {
//                        tracker?.drawDebug(canvas)
//                    }
//                }
//            })
//        tracker?.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation ?: 0)
//    }
//
//    override fun processImage() {
//        ++timestamp
//        val currTimestamp = timestamp
//        trackingOverlay!!.postInvalidate()
//
//        // No mutex needed as this method is not reentrant.
//        if (computingDetection) {
//            readyForNextImage()
//            return
//        }
//        computingDetection = true
//        println("zzzzzzzz Preparing image $currTimestamp for detection in bg thread.")
//        rgbFrameBitmap!!.setPixels(
//            getRgbBytes(),
//            0,
//            previewWidth,
//            0,
//            0,
//            previewWidth,
//            previewHeight
//        )
//        readyForNextImage()
//        val canvas = Canvas(croppedBitmap!!)
//        canvas.drawBitmap(rgbFrameBitmap!!, frameToCropTransform!!, null)
//        // For examining the actual TF input.
//        if (SAVE_PREVIEW_BITMAP) {
//            ImageUtils.saveBitmap(croppedBitmap!!)
//        }
//        val image = InputImage.fromBitmap(croppedBitmap!!, 0)
//        faceDetector?.process(image)
//            ?.addOnSuccessListener(OnSuccessListener { faces ->
//                if (faces.isEmpty()) {
//                    updateResults(currTimestamp, mutableListOf())
//                    return@OnSuccessListener
//                }
//                runInBackground { onFacesDetected(currTimestamp, faces) }
//            })
//    }
//
//    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
//    // checkpoints.
//    private enum class DetectorMode {
//        TF_OD_API
//    }
//
//    override fun setUseNNAPI(isChecked: Boolean) {
//        runInBackground { detector!!.setUseNNAPI(isChecked) }
//    }
//
//    override fun setNumThreads(numThreads: Int) {
//        runInBackground { detector!!.setNumThreads(numThreads) }
//    }
//
//    companion object {
//        private const val TF_OD_API_IS_QUANTIZED = false
//        private const val MAINTAIN_ASPECT = false
//        private const val SAVE_PREVIEW_BITMAP = false
//        private const val TEXT_SIZE_DIP = 10f
//
//        fun newInstance() = LegacyMaskScanningFragment()
//    }
//}