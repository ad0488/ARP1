package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.examples.objectdetection.data.Device
import org.tensorflow.lite.examples.objectdetection.data.Face
import org.tensorflow.lite.examples.objectdetection.data.KeyPoint
import org.tensorflow.lite.examples.objectdetection.utils.BoxUtils
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.image.ops.Rot90Op
import java.util.*


class YoloDetector(
    private val interpreter: Interpreter,
    private val gpuDelegate: GpuDelegate?,
    private val faceDetectorListener: FaceDetectorListener?
    ) {

    companion object{
        private const val CPU_NUM_THREADS = 4
        private const val MEAN = 127.5f
        private const val STD = 127.5f
        private const val TAG = "YoloDetector"
        private const val MODEL_FILENAME = "model_float16.tflite"

        fun create(
            context: Context,
            device: Device,
            faceDetectorListener: FaceDetectorListener?
        ): YoloDetector{
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
            return YoloDetector(
                Interpreter(
                    FileUtil.loadMappedFile(
                        context,
                        MODEL_FILENAME
                    ), options
                ),
                gpuDelegate,
                faceDetectorListener
            )
        }
    }

    private var lastInferenceTimeNanos: Long = -1
    private val inputWidth = interpreter.getInputTensor(0).shape()[1]
    private val inputHeight = interpreter.getInputTensor(0).shape()[2]
    private val boxUtils: BoxUtils = BoxUtils(confThreshold = 0.45f, iouThreshold = 0.6f)

    /**
     * Detect face on image
     */
    @Suppress("UNCHECKED_CAST")
    fun detect(bitmap: Bitmap, imageRotation: Int): Face? {
//        Log.d(TAG, "SIZE ${bitmap.width} ${bitmap.height}")
        val inputImage = processInputImage(bitmap, imageRotation)
//        Log.d(TAG, "Width: ${inputImage.width} Height ${inputImage.height}")
        val inputArray = arrayOf(inputImage.tensorBuffer.buffer)
//        Log.d(TAG, "SHAPE: ${Arrays.toString(outputShape)}")
        val outputMap = initOutputMap(interpreter)

        val inferenceStartTimeNanos = SystemClock.elapsedRealtimeNanos()
        interpreter.runForMultipleInputsOutputs(inputArray, outputMap)

        lastInferenceTimeNanos = SystemClock.elapsedRealtimeNanos() - inferenceStartTimeNanos
        Log.i(TAG, String.format("Yolo FaceDetector took %.2f ms", 1.0f * lastInferenceTimeNanos / 1_000_000))

        val output = outputMap[3] as Array<Array<FloatArray>>
        val face = postProcessModelOutput(output)

        faceDetectorListener?.onResultsFace(
            result = face,
            inferenceTime = lastInferenceTimeNanos / 1_000_000, // inference time in ms
            imageHeight = 640,
            imageWidth = 480
        )

        return face
    }

    /**
     * Resize the input image to a TensorImage
     */
    private fun processInputImage(bitmap: Bitmap, imageRotation: Int): TensorImage {
        val imageProcessor = ImageProcessor.Builder().apply {
            add(Rot90Op(imageRotation / 90))
            add(ResizeOp(inputWidth, inputHeight, ResizeOp.ResizeMethod.BILINEAR))
            add(NormalizeOp(MEAN, STD))
        }.build()
        val matrix = Matrix() // selfie camera returns mirrored image, we have to flip it back
        matrix.preScale(-1.0f, 1.0f)
        val mirroredBitmap = Bitmap.createBitmap(
            bitmap, 0, 0,
            bitmap.width, bitmap.height, matrix, false
        )
        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(mirroredBitmap)
        return imageProcessor.process(tensorImage)
    }

    /**
     * Filter out detections to get single face
     */
    private fun postProcessModelOutput(output: Array<Array<FloatArray>>): Face? {
        val prediction = boxUtils.nonMaxSuppression(output[0])
        if (prediction == null){
            Log.d(TAG, "Face not detected")
            return null
        }

        val boundingBox: Array<Float> =
            arrayOf(prediction[0], prediction[1], prediction[2], prediction[3])
        val keypoints = mutableListOf<KeyPoint>()
        for (i in 0 until 5){
            val x = prediction[6 + 3*i]
            val y = prediction[6 + 3*i+1]
            val score = prediction[6 + 3*i+2]
            keypoints.add(KeyPoint(PointF(x,y), score))
        }
        Log.d(TAG, "Face detected")
        return Face(boundingBox, keypoints)
    }

    /**
     * Create output map for model
     */
    private fun initOutputMap(interpreter: Interpreter): HashMap<Int, Any>{
        val outputMap = HashMap<Int, Any>()

        val outputShape0 = interpreter.getOutputTensor(0).shape()
        outputMap[0] = Array(outputShape0[0]) {
            Array(outputShape0[1]) {
                Array(outputShape0[2]) {
                    Array(outputShape0[3]) { FloatArray(outputShape0[4]) }
                }
            }
        }

        val outputShape1 = interpreter.getOutputTensor(1).shape()
        outputMap[1] = Array(outputShape1[0]) {
            Array(outputShape1[1]) {
                Array(outputShape1[2]) {
                    Array(outputShape1[3]) { FloatArray(outputShape1[4]) }
                }
            }
        }

        val outputShape2 = interpreter.getOutputTensor(2).shape()
        outputMap[2] = Array(outputShape2[0]) {
            Array(outputShape2[1]) {
                Array(outputShape2[2]) {
                    Array(outputShape2[3]) { FloatArray(outputShape2[4]) }
                }
            }
        }

        /* actual output */
        val outputShape3 = interpreter.getOutputTensor(3).shape()
        outputMap[3] = Array(outputShape3[0]) {
            Array(outputShape3[1]) {
                FloatArray(outputShape3[2])
            }
        }

        return outputMap
    }

    interface FaceDetectorListener {
        fun onErrorFace(error: String)
        fun onResultsFace(
            result: Face?,
            inferenceTime: Long,
            imageHeight: Int,
            imageWidth: Int
        )
    }
}