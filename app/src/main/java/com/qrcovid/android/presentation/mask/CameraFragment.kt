package com.qrcovid.android.presentation.mask

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.os.Bundle
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.qrcovid.android.R
import com.qrcovid.android.databinding.FragmentMaskScanningBinding
import com.qrcovid.mask.utils.TFLiteObjectDetectionAPIModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraFragment: Fragment(R.layout.fragment_mask_scanning) {

    private lateinit var cameraExecutor: ExecutorService

    private var _binding: FragmentMaskScanningBinding? = null
    private val binding get() = _binding!!


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMaskScanningBinding.bind(view)
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    override fun onStart() {
        super.onStart()
        startCamera()
    }


    override fun onDestroyView() {
        cameraExecutor.shutdown()
        _binding = null
        super.onDestroyView()
    }


    /**
     * Usage of CameraX
     */
    @SuppressLint("UnsafeOptInUsageError")
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext().applicationContext)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraView.surfaceProvider)
            }
            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_BLOCK_PRODUCER)
                .build()
                .also { imageAnalysis ->
                    imageAnalysis.setAnalyzer(cameraExecutor, { imageProxy ->
                        imageProxy.image?.let { mediaImage ->
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            processImage(image = image) { faceDetected ->
                                imageProxy.close()
                                if (faceDetected) {
                                    imageAnalysis.clearAnalyzer()
                                }
                            }
                        }
                    })
                }
            val useCaseGroup = UseCaseGroup.Builder()
                .addUseCase(preview)
                .addUseCase(analyzer)
                .build()
            try {
                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(viewLifecycleOwner, cameraSelector, useCaseGroup)
            } catch(exc: Exception) {
                println("zzzzzzzz Use case binding failed " +  exc.message)
            }

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    /**
     * Usage of ML Kit
     */
    private fun processImage(image: InputImage, onComplete:(Boolean) -> Unit) {
        val detector = FaceDetection.getClient(getFaceOptions())
        detector.process(image)
            .addOnSuccessListener { faces ->
                onComplete(true)
                onFacesDetected(faces = faces)
            }
            .addOnFailureListener { e ->
                onComplete(false)
                println("zzzzzz ERROR " + e.message)
                e.printStackTrace()
            }
    }

    /**
     * Usage of ML Kit
     */
    private fun getFaceOptions(): FaceDetectorOptions {
        return FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()
    }

    private fun onFacesDetected(faces: List<Face>) {
        val sourceW = resources.displayMetrics.widthPixels
        val sourceH = resources.displayMetrics.heightPixels
        val targetW = sourceW
        val targetH = sourceH

        val faceBmp = Bitmap.createBitmap(TF_INPUT_SIZE, TF_INPUT_SIZE, Bitmap.Config.ARGB_8888);

        val transform = createTransform(sourceW, sourceH, targetW, targetH, 0)
        faces.forEach { face ->
            println("zzzzzzzz FACE$face")
            val boundingBox = RectF(face.boundingBox)
            // maps original coordinates to portrait coordinates
            val faceBB = RectF(boundingBox)
            transform.mapRect(faceBB)

            getDetector()?.recognizeImage(faceBmp)
        }
    }

    // Face Mask Processing
    private fun createTransform(srcWidth: Int, srcHeight: Int, dstWidth: Int, dstHeight: Int, applyRotation: Int): Matrix {
        val matrix = Matrix()
        if (applyRotation != 0) {
            if (applyRotation % 90 != 0) {
                println("zzzzzzzzz Rotation of $applyRotation % 90 != 0")
            }
            // Translate so center of image is at origin.
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f)
            // Rotate around origin.
            matrix.postRotate(applyRotation.toFloat())
        }
        if (applyRotation != 0) {
            // Translate back from origin centered reference to destination frame.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f)
        }
        return matrix
    }

    private fun getDetector(): TFLiteObjectDetectionAPIModel? {
        val model = TFLiteObjectDetectionAPIModel.create(
            assetManager = resources.assets,
            modelFilename = TF_MODEL_FILE,
            labelFilename = TF_LABELS_FILE,
            inputSize = TF_INPUT_SIZE,
            isQuantized = TF_IS_QUANTIZED
        )

        println("zzzzzzz success = " + model.isSuccess)
        println("zzzzzzz exception = " + model.exceptionOrNull()?.message)
        model.exceptionOrNull()?.printStackTrace()
        return model.getOrNull()
    }


    companion object {
        private const val TF_MODEL_FILE = "mask_detector.tflite"
        private const val TF_LABELS_FILE = "file:///android_asset/mask_labelmap.txt"
        private const val TF_IS_QUANTIZED = false
        private const val TF_INPUT_SIZE = 224
    }
}