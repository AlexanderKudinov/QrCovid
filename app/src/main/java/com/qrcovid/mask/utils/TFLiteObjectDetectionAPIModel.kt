package com.qrcovid.mask.utils

import android.content.res.AssetManager
import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Trace
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*
import kotlin.math.min


/**
 * Wrapper for frozen detection models trained using the Tensorflow Object Detection API:
 * - https://github.com/tensorflow/models/tree/master/research/object_detection
 * where you can find the training code.
 *
 * To use pretrained models in the API or convert to TF Lite models, please see docs for details:
 * - https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/detection_model_zoo.md
 * - https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/running_on_mobile_tensorflowlite.md#running-our-model-on-android
 */
class TFLiteObjectDetectionAPIModel private constructor() {
    private var isModelQuantized = false

    // Config values.
    private var mInputSize = 0

    // Pre-allocated buffers.
    private val labels = Vector<String>()
    private var intValues: IntArray = intArrayOf()

    // outputLocations: array of shape [Batchsize, NUM_DETECTIONS,4]
    // contains the location of detected boxes
    private var outputLocations: Array<Array<FloatArray>> = arrayOf()

    // outputClasses: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the classes of detected boxes
    private var outputClasses: Array<FloatArray> = arrayOf()

    // outputScores: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the scores of detected boxes
    private var outputScores: Array<FloatArray> = arrayOf()

    // numDetections: array of shape [Batchsize]
    // contains the number of detected boxes
    private var numDetections: FloatArray = floatArrayOf()
    private var imgData: ByteBuffer? = null
    private var tfLite: Interpreter? = null

    // Face Mask Detector Output
    private var output: Array<FloatArray> = arrayOf()


    fun recognizeImage(bitmap: Bitmap?): List<Recognition?>? {
        // Preprocess the image data from 0-255 int to normalized float based on the provided parameters.
        bitmap!!.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        imgData!!.rewind()
        for (i in 0 until mInputSize) {
            for (j in 0 until mInputSize) {
                val pixelValue = intValues[i * mInputSize + j]
                if (isModelQuantized) {
                    // Quantized model
                    imgData!!.put((pixelValue shr 16 and 0xFF).toByte())
                    imgData!!.put((pixelValue shr 8 and 0xFF).toByte())
                    imgData!!.put((pixelValue and 0xFF).toByte())
                } else { // Float model
                    imgData!!.putFloat(((pixelValue shr 16 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    imgData!!.putFloat(((pixelValue shr 8 and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                    imgData!!.putFloat(((pixelValue and 0xFF) - IMAGE_MEAN) / IMAGE_STD)
                }
            }
        }

        // Copy the input data into TensorFlow.
        outputLocations = Array(1) { Array(NUM_DETECTIONS) { FloatArray(4) } }
        outputClasses = Array(1) { FloatArray(NUM_DETECTIONS) }
        outputScores = Array(1) { FloatArray(NUM_DETECTIONS) }
        numDetections = FloatArray(1)
        val inputArray = arrayOf<Any>(imgData ?: "")

        // Here outputMap is changed to fit the Face Mask detector
        val outputMap: MutableMap<Int, Any> = HashMap()
        output = Array(1) { FloatArray(2) }
        outputMap[0] = output

        // Run the inference call.
        Trace.beginSection("run");
        tfLite!!.runForMultipleInputsOutputs(inputArray, outputMap)
        val mask = output[0][0]
        val no_mask = output[0][1]
        val confidence: Float
        val id: String
        val label: String
        if (mask > no_mask) {
            label = "mask"
            confidence = mask
            id = "0"
        } else {
            label = "no mask"
            confidence = no_mask
            id = "1"
        }
        println("zzzzzzzz prediction: $mask, $no_mask")
        // Show the best detections.
        // after scaling them back to the input size.

        val numDetectionsOutput = min(NUM_DETECTIONS, numDetections[0].toInt())
        val recognitions = ArrayList<Recognition?>(numDetectionsOutput)
        recognitions.add(Recognition(id, label, confidence, RectF()))

//    for (int i = 0; i < numDetectionsOutput; ++i) {
//      final RectF detection =
//          new RectF(
//              outputLocations[0][i][1] * inputSize,
//              outputLocations[0][i][0] * inputSize,
//              outputLocations[0][i][3] * inputSize,
//              outputLocations[0][i][2] * inputSize);
//      // SSD Mobilenet V1 Model assumes class 0 is background class
//      // in label file and class labels start from 1 to number_of_classes+1,
//      // while outputClasses correspond to class index from 0 to number_of_classes
//      int labelOffset = 1;
//      recognitions.add(
//          new Recognition(
//              "" + i,
//              labels.get((int) outputClasses[0][i] + labelOffset),
//              outputScores[0][i],
//              detection));
//    }
        Trace.endSection() // "recognizeImage"
        return recognitions
    }


    companion object {
        // Only return this many results.
        private const val NUM_DETECTIONS = 10

        // Float model
        private const val IMAGE_MEAN = 128.0f
        private const val IMAGE_STD = 128.0f

        // Number of threads in the java app
        private const val NUM_THREADS = 4

        /** Memory-map the model file in Assets.  */
        @Throws(IOException::class)
        private fun loadModelFile(assets: AssetManager, modelFilename: String): MappedByteBuffer {
            val fileDescriptor = assets.openFd(modelFilename)
            val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
            val fileChannel = inputStream.channel
            val startOffset = fileDescriptor.startOffset
            val declaredLength = fileDescriptor.declaredLength
            return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        }

        /**
         * Initializes a native TensorFlow session for classifying images.
         *
         * @param assetManager The asset manager to be used to load assets.
         * @param modelFilename The filepath of the model GraphDef protocol buffer.
         * @param labelFilename The filepath of label file for classes.
         * @param inputSize The size of image input
         * @param isQuantized Boolean representing model is quantized or not
         */
        fun create(
            assetManager: AssetManager,
            modelFilename: String,
            labelFilename: String,
            inputSize: Int,
            isQuantized: Boolean
        ): Result<TFLiteObjectDetectionAPIModel> {
            return TFLiteObjectDetectionAPIModel().runCatching {
                val actualFilename = labelFilename.split("file:///android_asset/".toRegex()).toTypedArray()[1]
                val labelsInput = assetManager.open(actualFilename)
                BufferedReader(InputStreamReader(labelsInput)).use { bufferReader ->
                    var line: String?
                    while (bufferReader.readLine().also { line = it } != null) {
                        line?.let {
                            labels.add(line)
                        }
                    }
                }
                mInputSize = inputSize
                tfLite = Interpreter(loadModelFile(assetManager, modelFilename))
                isModelQuantized = isQuantized
                // Pre-allocate buffers.
                val numBytesPerChannel = if (isQuantized) {
                    1 // Quantized
                } else {
                    4 // Floating point
                }
                imgData = ByteBuffer.allocateDirect(1 * mInputSize * mInputSize * 3 * numBytesPerChannel)
                imgData?.order(ByteOrder.nativeOrder())
                intValues = IntArray(mInputSize * mInputSize)
                tfLite!!.setNumThreads(NUM_THREADS)
                outputLocations = Array(1) { Array(NUM_DETECTIONS) { FloatArray(4) } }
                outputClasses = Array(1) { FloatArray(NUM_DETECTIONS) }
                outputScores = Array(1) { FloatArray(NUM_DETECTIONS) }
                numDetections = FloatArray(1)
                this
            }
        }
    }
}