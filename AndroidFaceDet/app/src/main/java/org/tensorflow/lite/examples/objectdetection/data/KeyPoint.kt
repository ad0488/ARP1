package org.tensorflow.lite.examples.objectdetection.data

import android.graphics.PointF

data class KeyPoint(
    var coordinate: PointF,
    val score: Float
)
