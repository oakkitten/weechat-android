package com.ubergeek42.WeechatAndroid.views

import android.graphics.Canvas
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.Spanned
import android.text.StaticLayout
import android.text.style.LineHeightSpan
import com.ubergeek42.WeechatAndroid.upload.dp_to_px
import com.ubergeek42.WeechatAndroid.upload.f
import com.ubergeek42.WeechatAndroid.upload.i


class RoundedBackgroundColorSpan(val color: Int) : LineHeightSpan {
    override fun chooseHeight(text: CharSequence?, start: Int, end: Int, spanstartv: Int, lineHeight: Int, fm: Paint.FontMetricsInt) {
        fm.top = minOf(fm.top, fm.ascent - BLOOM.i)
        fm.bottom = maxOf(fm.bottom, fm.descent + BLOOM.i)
    }
}


fun StaticLayout.drawRoundedBackground(canvas: Canvas, paint: Paint) {
    val spanned = text as? Spanned ?: return

    val textStart = getLineStart(0)
    val textEnd = getLineEnd(lineCount - 1)

    spanned.getSpans(textStart, textEnd, RoundedBackgroundColorSpan::class.java)
            .forEach { span ->
                val start = maxOf(textStart, spanned.getSpanStart(span))
                val end = minOf(textEnd, spanned.getSpanEnd(span))
                drawRoundedBackground(canvas, paint, span.color, start, end)
            }
}

private fun StaticLayout.drawRoundedBackground(canvas: Canvas, paint: Paint, color: Int, start: Int, end: Int) {
    val lineStart = getLineForOffset(start)
    val lineEnd = getLineForOffset(end)

    val rects = (lineStart..lineEnd).mapNotNull { line ->
        val left = when (line) {
            lineStart -> getPrimaryHorizontal(start)
            else      -> getPrimaryHorizontal(getLineStart(line))
        }

        val right = when (line) {
            lineEnd -> getPrimaryHorizontal(end)
            else    -> getPrimaryHorizontal(getLineEnd(line) - 1)
        }

        val top = when (line) {
            0    -> getLineTop(line) - topPadding
            else -> getLineTop(line)
        }

        val bottom = when (line) {
            lineCount - 1 -> getLineBottom(line) - bottomPadding
            else          -> getLineBottom(line)
        }

        when {
            left < right -> RectF(left - BLOOM, top.f - BLOOM, right + BLOOM, bottom.f + BLOOM)
            left > right -> RectF(right - BLOOM, top.f - BLOOM, left + BLOOM, bottom.f + BLOOM)
            else -> null
        }
    }

    if (rects.isEmpty()) return

    val originalColor = paint.color
    paint.color = color

    if (rects.size == 1) {
        canvas.drawRoundRect(rects.first(), RADIUS, RADIUS, paint)
    } else {
        val originalPathEffect = paint.pathEffect
        paint.pathEffect = CORNER_PATH_EFFECT
        canvas.drawPath(rects.toUnionPath(), paint)
        paint.pathEffect = originalPathEffect

    }

    paint.color = originalColor
}


private fun RectF.toPath() = Path().apply { addRect(this@toPath, Path.Direction.CW) }

private fun List<RectF>.toUnionPath() = first().toPath()
        .also {
            for (i in 1 until size) {
                it.op(this[i].toPath(), Path.Op.UNION)
            }
        }


val RADIUS = 4.dp_to_px.f
val CORNER_PATH_EFFECT = CornerPathEffect(RADIUS)
val BLOOM = 1.dp_to_px.f