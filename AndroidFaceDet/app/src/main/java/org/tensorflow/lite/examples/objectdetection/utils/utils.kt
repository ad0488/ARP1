package org.tensorflow.lite.examples.objectdetection.utils

import android.graphics.*
import org.tensorflow.lite.examples.objectdetection.data.KeyPoint


fun booleanToInt(b: Boolean): Int {
    return if (b) 1 else 0
}

fun scaleBbox(bbox: Array<Float>, width: Int, height: Int): Array<Float>{
    val yoloOuptutSize = 640
    val scaleWidth = height * 1f / yoloOuptutSize
    val scaleHeight = width * 1f / yoloOuptutSize

    var bboxScaled = arrayOf<Float>()
    for (i in bbox.indices){
        bboxScaled += if (i%2 == 0){
            bbox[i] * scaleWidth
        } else{
            bbox[i] * scaleHeight
        }
    }
    return bboxScaled
}

fun scaleKeypoint(keypoint: KeyPoint, width: Int, height: Int): KeyPoint {
    val yoloOuptutSize = 640
    val scaleWidth = height * 1f / yoloOuptutSize
    val scaleHeight = width * 1f / yoloOuptutSize

    return KeyPoint(
        coordinate = PointF(
            keypoint.coordinate.x * scaleWidth,
            keypoint.coordinate.y * scaleHeight
        ),
        score = keypoint.score,
        type = keypoint.type
    )
}

//fun toGrayscale(bmpOriginal: Bitmap): Bitmap? {
//    val height: Int = bmpOriginal.height
//    val width: Int = bmpOriginal.width
//    val bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
//    val c = Canvas(bmpGrayscale)
//    val paint = Paint()
//    val cm = ColorMatrix()
//    cm.setSaturation(0f)
//    val f = ColorMatrixColorFilter(cm)
//    paint.colorFilter = f
//    c.drawBitmap(bmpOriginal, 0, 0, paint)
//    return bmpGrayscale
//}