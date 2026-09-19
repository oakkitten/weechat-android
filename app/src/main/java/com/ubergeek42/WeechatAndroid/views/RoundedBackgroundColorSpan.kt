package com.ubergeek42.WeechatAndroid.views

import android.graphics.Canvas
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.Spanned
import android.text.StaticLayout
import android.text.style.ParagraphStyle
import com.ubergeek42.WeechatAndroid.upload.dp_to_px
import com.ubergeek42.WeechatAndroid.upload.f


class RoundedBackgroundColorSpan(val color: Int) : ParagraphStyle


fun StaticLayout.drawRoundedBackground(canvas: Canvas, paint: Paint) {
    val spanned = (text as Spanned)

    val textStart = getLineStart(0)
    val textEnd = getLineEnd(lineCount - 1)

    val spans = spanned.getSpans(textStart, textEnd, RoundedBackgroundColorSpan::class.java)

    spans.forEach { span ->
        val start = maxOf(textStart, spanned.getSpanStart(span))
        val end = minOf(textEnd, spanned.getSpanEnd(span))

        val startLine = getLineForOffset(start)
        val endLine = getLineForOffset(end)

        val rects = mutableListOf<RectF>()

        for (line in startLine..endLine) {
            val left = when (line) {
                startLine -> getPrimaryHorizontal(start)
                else      -> getLineLeft(line)
            }

            val right = when (line) {
                endLine -> getPrimaryHorizontal(end)
                else    -> getLineMax(line)
            }

            val top = when (line) {
                startLine -> getLineTop(line) - topPadding
                else      -> getLineTop(line)
            }

            val bottom = when (line) {
                endLine -> getLineBottom(line) - bottomPadding
                else    -> getLineBottom(line)
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
            paint.color = span.color

            canvas.drawPath(mergedPath, paint)

            paint.color = originalColor
            paint.pathEffect = originalPathEffect
        }
    }
}