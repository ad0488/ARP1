package org.tensorflow.lite.examples.objectdetection.utils

import android.util.Log
import java.lang.Float.max
import java.lang.Float.min
import java.util.*

class BoxUtils(var confThreshold: Float, var iouThreshold: Float) {

    /**
     * Fake non max suppression, only takes bounding box of best prediction
     */
    fun nonMaxSuppression(boxes: Array<FloatArray>): FloatArray?{
        var bestScore: Float = -1f
        var bestIx: Int = -1
        for (i in boxes.indices){
            val currScore = boxes[i][4]
            if (currScore > bestScore){
                bestScore = currScore
                bestIx = i
            }
        }

        val boxOut = boxes[bestIx]
        if (boxOut[4] < confThreshold){
            return null
        }

        xywh2xyxy(boxOut)
        return boxOut
    }

    /**
     * Transform from center xywh to xyxy (inplace)
     */
    private fun xywh2xyxy(box: FloatArray) {
//        val xMin = max(box[0] - box[2]/2, 0f)
//        val yMin = max(box[1] - box[3]/2, 0f)
//        val xMax = min(box[0] + box[2]/2, 640f) // TODO: remove hard-coded values
//        val yMax = min(box[1] + box[3]/2, 640f)
        val xMin = box[0] - box[2]/2
        val yMin = box[1] - box[3]/2
        val xMax = box[0] + box[2]/2 // TODO: remove hard-coded values
        val yMax = box[1] + box[3]/2
        box[0] = xMin
        box[1] = yMin
        box[2] = xMax
        box[3] = yMax
    }

}