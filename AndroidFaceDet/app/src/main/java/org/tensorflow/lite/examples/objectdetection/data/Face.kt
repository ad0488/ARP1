package org.tensorflow.lite.examples.objectdetection.data

data class Face(
    val boundingBox: Array<Float>,
    val keyPoints: List<KeyPoint>
)
