package com.ubergeek42.WeechatAndroid.views

import android.R
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.Spanned
import android.text.StaticLayout
import android.text.style.ParagraphStyle
import com.ubergeek42.WeechatAndroid.upload.dp_to_px
import com.ubergeek42.WeechatAndroid.upload.f


class RoundedNewt(val start: Int, val end: Int, val color: Int) : ParagraphStyle

fun StaticLayout.drawNewtBackground(canvas: Canvas, paint: Paint) {
    val spanned = (text as Spanned)
    val textStart = getLineStart(0)
    val textEnd = getLineEnd(lineCount - 1)
    val newts = spanned.getSpans(textStart , textEnd, RoundedNewt::class.java)
    newts.forEach { newt ->
        val start = maxOf(textStart, spanned.getSpanStart(newt))
        val end = minOf(textEnd, spanned.getSpanEnd(newt))

        val startLine = getLineForOffset(start)
        val endLine = getLineForOffset(end)

        val rects = mutableListOf<RectF>()

        for (i in startLine..endLine) {
            val left = if (i == startLine) {
                getPrimaryHorizontal(start)
            } else {
                getLineLeft(i)
            }

            val right = if (i == endLine) {
                getPrimaryHorizontal(end)
            } else {
                getLineMax(i)
            }

            //val top = getLineTop(i)

            val top = if (i == startLine) {
                getLineTop(i) - topPadding
            } else {
                getLineTop(i)
            }

            val bottom = if (i == endLine) {
                getLineBottom(i) - bottomPadding
            } else {
                getLineBottom(i)
            }

            rects.add(RectF(left, top.f, right, bottom.f))
        }

        if (rects.isNotEmpty()) {
            val mergedPath = Path()
            mergedPath.addRect(rects.first(), Path.Direction.CW)
            if (rects.size > 1) {
                for (i in 1 until rects.size) {
                    mergedPath.op(Path().apply { addRect(rects[i], Path.Direction.CW) }, Path.Op.UNION)
                }
            }

            val originalColor = paint.color
            val originalPathEffect = paint.pathEffect
            paint.pathEffect = CornerPathEffect(4.dp_to_px.f)
            paint.color = newt.color

            canvas.drawPath(mergedPath, paint)

            paint.color = originalColor
            paint.pathEffect = originalPathEffect
        }
    }
}