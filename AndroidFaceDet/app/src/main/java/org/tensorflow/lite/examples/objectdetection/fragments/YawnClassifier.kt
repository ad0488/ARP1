package org.tensorflow.lite.examples.objectdetection.fragments

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.examples.objectdetection.YoloDetector
import org.tensorflow.lite.examples.objectdetection.data.Device
import org.tensorflow.lite.examples.objectdetection.data.Face
import org.tensorflow.lite.examples.objectdetection.utils.BoxUtils
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import java.util.*
import kotlin.collections.HashMap

class YawnClassifier(
    private val interpreter: Interpreter,
    private val gpuDelegate: GpuDelegate?,
    private val yawnClassifierListener: YawnClassifierListener?
) {
    companion object{
        private const val CPU_NUM_THREADS = 4
        private const val MEAN = 127.5f
        private const val STD = 127.5f
        private const val TAG = "YawnClassifier"
        private const val MODEL_FILENAME = "yawn_model.tflite"

        fun create(
            context: Context,
            device: Device,
            yawnClassifierListener: YawnClassifierListener?
        ): YawnClassifier {
            val options = Interpreter.Options()
            var gpuDelegate: GpuDelegate? = null
            options.numThreads = CPU_NUM_THREADS
            when (device) {
                Device.CPU -> {
                }
                Device.GPU -> {
                    gpuDelegate = GpuDelegate()
                    options.addDelegate(gpuDelegate)
                }
                Device.NNAPI -> options.useNNAPI = true
            }
            return YawnClassifier(
                Interpreter(
                    FileUtil.loadMappedFile(
                        context,
                        MODEL_FILENAME
                    ), options
                ),
                gpuDelegate,
                yawnClassifierListener
            )
        }
    }

    private var lastInferenceTimeNanos: Long = -1
    private val inputWidth = interpreter.getInputTensor(0).shape()[1]
    private val inputHeight = interpreter.getInputTensor(0).shape()[2]

    /**
     * Classify yawn
     */
    @Suppress("UNCHECKED_CAST")
    fun classify(bitmap: Bitmap, imageRotation: Int, face: Face?) {
        if(face == null){
            yawnClassifierListener?.onResultsYawn(
                result = -1,
                inferenceTime = 0
            )
        }
//        Log.d(TAG, "SIZE ${bitmap.width} ${bitmap.height}")
        val inputImage = processInputImage(bitmap, imageRotation, face!!.boundingBox)
//        Log.d(TAG, "Width: ${inputImage.width} Height ${inputImage.height}")
        val inputArray = arrayOf(inputImage.tensorBuffer.buffer)
////        Log.d(TAG, "SHAPE: ${Arrays.toString(outputShape)}")

        val outputMap = HashMap<Int, Any>()
        val outputShape = interpreter.getOutputTensor(0).shape()
//        Log.e(TAG, "OUTPUT SHAPE ${Arrays.toString(outputShape)}")
        outputMap[0] = Array(outputShape[0]){
            FloatArray(outputShape[1])
        }

        val inferenceStartTimeNanos = SystemClock.elapsedRealtimeNanos()
        interpreter.runForMultipleInputsOutputs(inputArray, outputMap)

        lastInferenceTimeNanos = SystemClock.elapsedRealtimeNanos() - inferenceStartTimeNanos
        Log.i(TAG, String.format("Yawn Classifier took %.2f ms", 1.0f * lastInferenceTimeNanos / 1_000_000))

        val output = outputMap[0] as Array<FloatArray>
        Log.e(TAG, "YAWN OUTPUT ${output[0][0]} ${output[0][1]}")
//        val face = postProcessModelOutput(output)
//
//        faceDetectorListener?.onResults(
//            result = face,
//            inferenceTime = lastInferenceTimeNanos / 1_000_000, // inference time in ms
//            imageHeight = 640,
//            imageWidth = 480
//        )

//        return face
    }

    /**
     * Resize the input image to a TensorImage
     */
    private fun processInputImage(bitmap: Bitmap, imageRotation: Int, bbox: Array<Float>): TensorImage {
        val imageProcessor = ImageProcessor.Builder().apply {
            add(Rot90Op(imageRotation / 90))
            add(ResizeOp(inputWidth, inputHeight, ResizeOp.ResizeMethod.BILINEAR))
//            add(NormalizeOp(MEAN, STD))
        }.build()
        val matrix = Matrix() // selfie camera returns mirrored image, we have to flip it back
        matrix.preScale(-1.0f, 1.0f)
        val mirroredBitmap = Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height, matrix, false
        )
        Log.e(TAG,"SIZE ${bitmap.width} ${bitmap.height}, BBOX: ${bbox.contentToString()}")

        val x = bbox[1].toInt()
        val y = bbox[0].toInt()
        var newWidth = (bbox[3]-bbox[1]).toInt()
        newWidth = if (x+newWidth <= bitmap.width) newWidth else bitmap.width-x
        var newHeight = (bbox[2]-bbox[0]).toInt()
        newHeight = if (y+newHeight <= bitmap.height) newHeight else bitmap.height-y
        Log.e(TAG, "NEW BBOX $newWidth $newHeight")
        val croppedBitmap = Bitmap.createBitmap(
            mirroredBitmap, x, y, newWidth, newHeight
        )

        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(croppedBitmap)
        return imageProcessor.process(tensorImage)
    }

    interface YawnClassifierListener {
        fun onErrorYawn(error: String)
        fun onResultsYawn(
            result: Int,
            inferenceTime: Long,
        )
    }
}