package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.examples.objectdetection.data.Device
import org.tensorflow.lite.examples.objectdetection.data.Face
import org.tensorflow.lite.examples.objectdetection.utils.booleanToInt
import org.tensorflow.lite.examples.objectdetection.utils.scaleBbox
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
        private const val MEAN = 0f //127.5f
        private const val STD = 255f //127.5f
        private const val TAG = "YawnClassifier"
        private const val MODEL_FILENAME = "yawn.tflite"

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
    fun classify(bitmap: Bitmap, face: Face?) {
        if(face == null){
            yawnClassifierListener?.onResultsYawn(
                result = -1,
                inferenceTime = 0
            )
            return
        }
//        Log.d(TAG, "SIZE ${bitmap.width} ${bitmap.height}")
        val inputImage = processInputImage(bitmap, face!!.boundingBox)
//        Log.d(TAG, "Width: ${inputImage.width} Height ${inputImage.height}")
        val inputArray = arrayOf(inputImage.tensorBuffer.buffer)
////        Log.d(TAG, "SHAPE: ${Arrays.toString(outputShape)}")

        val outputMap = HashMap<Int, Any>()
        val outputShape = interpreter.getOutputTensor(0).shape()

        outputMap[0] = Array(outputShape[0]){
            FloatArray(outputShape[1])
        }

        val inferenceStartTimeNanos = SystemClock.elapsedRealtimeNanos()
        interpreter.runForMultipleInputsOutputs(inputArray, outputMap)

        lastInferenceTimeNanos = SystemClock.elapsedRealtimeNanos() - inferenceStartTimeNanos
        Log.i(TAG, String.format("Yawn Classifier took %.2f ms", 1.0f * lastInferenceTimeNanos / 1_000_000))

        val output = outputMap[0] as Array<FloatArray>
        val result = booleanToInt(output[0][0] > 0.5)
        Log.e(TAG, "YAWN OUT $result")

        yawnClassifierListener?.onResultsYawn(
            result = result,
            inferenceTime = lastInferenceTimeNanos / 1_000_000, // inference time in ms
        )
    }

    /**
     * Resize the input image to a TensorImage
     */
    private fun processInputImage(bitmap: Bitmap, bbox: Array<Float>): TensorImage {
        val imageProcessor = ImageProcessor.Builder().apply {
//            add(Rot90Op(imageRotation / 90))
            add(ResizeOp(inputWidth, inputHeight, ResizeOp.ResizeMethod.BILINEAR))
            add(NormalizeOp(MEAN, STD))
        }.build()

        val matrix = Matrix() // selfie camera returns mirrored image, we have to flip it back
        matrix.preScale(-1.0f, 1.0f)
        matrix.postRotate(90F)
        val mirroredBitmap = Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height, matrix, false
        )
//        Log.e(TAG,"SIZE ${bitmap.width} ${bitmap.height}, BBOX: ${bbox.contentToString()}")

        val bboxScaled = scaleBbox(bbox, bitmap.width, bitmap.height)
        val x = bboxScaled[0].toInt()
        val y = bboxScaled[1].toInt()
        val newWidth = (bboxScaled[2]-bboxScaled[0]).toInt()
        val newHeight = (bboxScaled[3]-bboxScaled[1]).toInt()

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