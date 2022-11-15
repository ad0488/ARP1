/*
 * Copyright 2022 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.objectdetection

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import org.tensorflow.lite.examples.objectdetection.data.Face
import java.util.LinkedList
import kotlin.math.max
import org.tensorflow.lite.task.vision.detector.Detection

class OverlayView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private var result: Face? = null
    private var boxPaint = Paint()
    private var textBackgroundPaint = Paint()
    private var textPaint = Paint()

    private var scaleWidth: Float = 1f
    private var scaleHeight: Float = 1f

    init {
        initPaints()
    }

    fun clear() {
        boxPaint.reset()
        invalidate()
        initPaints()
    }

    private fun initPaints() {
        boxPaint.color = ContextCompat.getColor(context!!, R.color.bounding_box_color)
        boxPaint.strokeWidth = 8F
        boxPaint.style = Paint.Style.STROKE
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        if (result == null){
            return
        }
        else{
            val bbox = result!!.boundingBox

            for (i in bbox.indices){
                if (i%2 == 0){
                    bbox[i] *= scaleWidth
                }
                else{
                    bbox[i] *= scaleHeight
                }
            }
            val drawableRect = RectF(bbox[0], bbox[1], bbox[2], bbox[3])

            canvas.drawRect(drawableRect, boxPaint)

            for (keypoint in result!!.keyPoints){
                if (keypoint.score > 0.5){
                    val scaledX = keypoint.coordinate.x * scaleWidth
                    val scaledY = keypoint.coordinate.y * scaleHeight

                    canvas.drawCircle(scaledX, scaledY, 2f, boxPaint)
                }
            }
        }
    }

    fun setResults(
        detectionResults: Face?,
        imageHeight: Int,
        imageWidth: Int,
    ) {
        result = detectionResults
        // PreviewView is in FILL_START mode. So we need to scale up the bounding box to match with
        // the size that the captured images will be displayed.
        scaleWidth = width * 1f / imageWidth
        scaleHeight = height * 1f / imageHeight
    }

    companion object {
        private const val BOUNDING_RECT_TEXT_PADDING = 8
    }
}
