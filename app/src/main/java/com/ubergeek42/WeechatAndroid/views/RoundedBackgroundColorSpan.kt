package com.ubergeek42.WeechatAndroid.views

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.text.style.LineBackgroundSpan
import androidx.core.text.PrecomputedTextCompat
import com.ubergeek42.WeechatAndroid.upload.dp_to_px
import com.ubergeek42.WeechatAndroid.upload.f

class RoundedBackgroundColorSpan(val start: Int, val end: Int) : LineBackgroundSpan {
    var previousLeft: Float = -1f
    var previousRight: Float = -1f

    val rects = mutableListOf<RectF>()

    override fun drawBackground(
        canvas: Canvas,
        paint: Paint,
        lineLeft: Int,
        lineRight: Int,
        lineTop: Int,
        lineBaseline: Int,
        lineBottom: Int,
        text: CharSequence,
        lineStart: Int,
        lineEnd: Int,
        lineNumber: Int,
    ) {
        val left = if (start >= lineEnd) {
            return
        } else if (start <= lineStart) {
            lineLeft.f
        } else {
            lineLeft + (text as PrecomputedTextCompat).precomputedText!!.getWidth(lineStart, start)
        }

        val right = if (end <= lineStart) {
            return
        } else if (end >= lineEnd) {
            lineRight.f
        } else {
            lineRight - (text as PrecomputedTextCompat).precomputedText!!.getWidth(end, lineEnd)
        }

        rects.add(RectF(left, lineTop.f, right, lineBottom.f))

        val draw = end <= lineEnd

        if (draw && rects.isNotEmpty()) {
            val mergedPath = Path()
            mergedPath.addRect(rects.first(), Path.Direction.CW)
            if (rects.size > 1) {
                for (i in 1 until rects.size) {
                    mergedPath.op(Path().apply { addRect(rects[i], Path.Direction.CW) }, Path.Op.UNION)
                }
            }

            val originalColor = paint.color
            paint.pathEffect = CornerPathEffect(4.dp_to_px.f)
            paint.color = Color.RED

            canvas.drawPath(mergedPath, paint)

            paint.color = originalColor
        }
    }

//    fun foo() {
//        val originalColor = paint.color
//        paint.pathEffect = CornerPathEffect(4.dp_to_px.f)
//        paint.color = Color.RED
//
//        if (lineNumber == 0) {
//            previousLeft = left.f
//            previousRight = right.f
//        }
//
//        val path = Path().apply {
//            moveTo(left.f, top.f)
//            lineTo(left.f, bottom.f)
//            lineTo(right.f, bottom.f)
//            lineTo(right.f, top.f)
//
//            close() // Connects back to the starting point (creates a triangle)
//        }
//
//        paint.color = originalColor
//    }
}