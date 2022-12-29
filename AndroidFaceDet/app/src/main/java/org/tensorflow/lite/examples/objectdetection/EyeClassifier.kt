package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.graphics.*
import android.os.SystemClock
import android.util.Log
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.examples.objectdetection.data.Device
import org.tensorflow.lite.examples.objectdetection.data.Face
import org.tensorflow.lite.examples.objectdetection.data.KeyPoint
import org.tensorflow.lite.examples.objectdetection.data.KeypointType
import org.tensorflow.lite.examples.objectdetection.utils.booleanToInt
import org.tensorflow.lite.examples.objectdetection.utils.scaleBbox
import org.tensorflow.lite.examples.objectdetection.utils.scaleKeypoint
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp


class EyeClassifier (
    private val interpreter: Interpreter,
    private val gpuDelegate: GpuDelegate?,
    private val eyeClassifierListener: EyeClassifierListener?
) {
    companion object{
        private const val CPU_NUM_THREADS = 4
        private const val MEAN = 127.5f
        private const val STD = 127.5f
        private const val TAG = "EyeClassifier"
        private const val MODEL_FILENAME = "eye.tflite"

        fun create(
            context: Context,
            device: Device,
            eyeClassifierListener: EyeClassifierListener?
        ): EyeClassifier {
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
            return EyeClassifier(
                Interpreter(
                    FileUtil.loadMappedFile(
                        context,
                        MODEL_FILENAME
                    ), options
                ),
                gpuDelegate,
                eyeClassifierListener
            )
        }
    }
    private var lastInferenceTimeNanos: Long = -1
    private val inputWidth = interpreter.getInputTensor(0).shape()[1]
    private val inputHeight = interpreter.getInputTensor(0).shape()[2]

    // filter to generate grayscale images
    private val paint = Paint()

    init {
        initGrayscaleFilter()
    }

    private fun initGrayscaleFilter(){
        val matrixColor = ColorMatrix()
        matrixColor.setSaturation(0f)
        val filter = ColorMatrixColorFilter(matrixColor)
        paint.colorFilter = filter
    }

    /**
     * Classify yawn
     */
    @Suppress("UNCHECKED_CAST")
    fun classify(bitmap: Bitmap, face: Face?, predictLeftEye: Boolean) {
        if(face == null){
            eyeClassifierListener?.onResultsEye(
                result = -1,
                inferenceTime = 0,
                predictLeftEye = predictLeftEye
            )
            return
        }

        var type = KeypointType.LEFT_EYE
        if (!predictLeftEye){
            type = KeypointType.RIGHT_EYE
        }

        var eye: KeyPoint? = null
        for(i in face!!.keyPoints.indices){
            if (face.keyPoints[i].type == type && face.keyPoints[i].score > 0.5){
                eye = face.keyPoints[i]
            }
        }

        if (eye == null){
            eyeClassifierListener?.onResultsEye(
                result = -1,
                inferenceTime = 0,
                predictLeftEye = predictLeftEye
            )
            return
        }

        val inputImage = processInputImage(bitmap, face, eye!!)
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
        Log.i(TAG, String.format("Eye Classifier took %.2f ms", 1.0f * lastInferenceTimeNanos / 1_000_000))

        val output = outputMap[0] as Array<FloatArray>
        val result = booleanToInt(output[0][0] > 0.5)
        if(predictLeftEye){
            Log.e(TAG, "Left eye $result")
        }
        else{
            Log.e(TAG, "Right eye $result")
        }

        eyeClassifierListener?.onResultsEye(
            result = result,
            inferenceTime = lastInferenceTimeNanos / 1_000_000, // inference time in ms
            predictLeftEye = predictLeftEye
        )
    }

    /**
     * Resize the input image to a TensorImage
     */
    private fun processInputImage(bitmap: Bitmap, face: Face, keypoint: KeyPoint): TensorImage {
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

        val keypointScaled = scaleKeypoint(keypoint, bitmap.width, bitmap.height)
        val bboxScaled = scaleBbox(face.boundingBox, bitmap.width, bitmap.height)

        val x = keypointScaled.coordinate.x
        val y = keypointScaled.coordinate.y

        val faceWidth = bboxScaled[3]-bboxScaled[1]
        val faceHeight = bboxScaled[2]-bboxScaled[0]
        val offsetWidth = faceWidth * 0.15
        val offsetHeight = faceHeight * 0.08

        val croppedBitmap = Bitmap.createBitmap(
            mirroredBitmap,
            (x-offsetWidth).toInt(),
            (y-offsetHeight).toInt(),
            (offsetWidth*2).toInt(),
            (offsetHeight*2).toInt()
        )

        val c =  Canvas(croppedBitmap)
        c.drawBitmap(croppedBitmap, 0f, 0f, paint)

        val tensorImage = TensorImage(DataType.FLOAT32)
        tensorImage.load(croppedBitmap)
        return imageProcessor.process(tensorImage)
    }


    interface EyeClassifierListener {
        fun onErrorEye(error: String)
        fun onResultsEye(
            result: Int,
            inferenceTime: Long,
            predictLeftEye: Boolean
        )
    }
}