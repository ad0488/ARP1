package org.tensorflow.lite.examples.objectdetection

import android.util.Log
import java.util.*

class MainClassifier {

    private val TAG = "MainClassifier"

    val nYawn = 5
    private val yawnPredictions = arrayListOf<Int>()
    val nLeftEye = 3
    private val leftEyePredictions = arrayListOf<Int>()
    val nRightEye = 3
    private val rightEyePredictions = arrayListOf<Int>()

    val f_fomPredictions = arrayListOf<Double>()
    val f_perclosPredictions = arrayListOf<Double>()
    val f_drowsyPredictions = arrayListOf<Double>()

    val windowsSize = 10
    val alpha = 0.30 // weight between FOM in PERCLOS
    private val samples = arrayListOf<Float>()
    var drowsiness_level = 0.0

    fun addPrediction(result: Int, type: String){
        when (type) {
            "yawn" -> {
                addToList(result, yawnPredictions, nYawn);
            }
            "leftEye" -> {
                addToList(result, leftEyePredictions, nLeftEye);
            }
            "rightEye" -> {
                addToList(result, rightEyePredictions, nRightEye);
            }
        }
    }

    private fun addToList(value: Int, list: ArrayList<Int>, maxSize: Int){
        if (list.size == maxSize){
            Collections.rotate(list, -1)
            list.removeLast()
            list.add(value)
        }
        else{
            list.add(value)
        }
    }

    private fun addToList(value: Float, list: ArrayList<Float>, maxSize: Int){
        if (list.size == maxSize){
            Collections.rotate(list, -1)
            list.removeLast()
            list.add(value)
        }
        else{
            list.add(value)
        }
    }

    fun predictFinal(){
        if (yawnPredictions.size < nYawn ||
            leftEyePredictions.size  < nLeftEye ||
            rightEyePredictions.size < nRightEye
        ){
            Log.e(TAG, "Not enough data for final prediction");
            return
        }

        // https://sci-hub.se/10.1007/s11042-021-10930-z
        // FOM is a percentage of the total frame that yawn is detected during a certain time interval
        // PERCLOS is a percentage of the total frame that eye is closed during a certain time interval

        val FOM = Collections.frequency(yawnPredictions, 1).toFloat() / nYawn
        val leftEyeProbability = Collections.frequency(leftEyePredictions, 0).toFloat() / nLeftEye
        val rightEyeProbability = Collections.frequency(rightEyePredictions, 0).toFloat() / nRightEye
        val PERCLOS = (leftEyeProbability+rightEyeProbability) / 2

        val fomPerclosRatio = ((alpha)*FOM + (1-alpha)*PERCLOS).toFloat()
        Log.e(TAG, "FOM: $FOM, PERCLOS: $PERCLOS, Average: $fomPerclosRatio")

        addToList(fomPerclosRatio, samples, windowsSize)
        if(samples.size == windowsSize){
            val average = samples.sum() / windowsSize
            Log.e(TAG, "Drowsiness level: $average")
            drowsiness_level = average.toDouble()
        }
        f_perclosPredictions.add(PERCLOS.toDouble())
        f_fomPredictions.add(FOM.toDouble())
        f_drowsyPredictions.add(drowsiness_level)
    }
}