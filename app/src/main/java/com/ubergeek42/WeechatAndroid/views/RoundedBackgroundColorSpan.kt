package com.ubergeek42.WeechatAndroid.views

import android.graphics.Canvas
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.text.Spanned
import android.text.StaticLayout
import android.text.style.LineHeightSpan
import com.ubergeek42.WeechatAndroid.upload.dp_to_pxf
import com.ubergeek42.WeechatAndroid.upload.f


class RoundedBackgroundColorSpan(val color: Int) : LineHeightSpan {
    override fun chooseHeight(text: CharSequence, start: Int, end: Int, spanstartv: Int, lineHeight: Int, fm: Paint.FontMetricsInt) {
        // TODO This is not needed
        //fm.top = minOf(fm.top, fm.ascent - V_BLOOM.i - 50)
        //fm.bottom = maxOf(fm.bottom, fm.descent + V_BLOOM.i)
    }
}

// todo remove default?
fun StaticLayout.drawRoundedBackground(canvas: Canvas, paint: Paint, firstLineIndent: Float) {
    val spanned = text as? Spanned ?: return
    val textStart = getLineStart(0)
    val textEnd = getLineEnd(lineCount - 1)

    val rectsToColor = spanned.getSpans(textStart, textEnd, RoundedBackgroundColorSpan::class.java)
            .mapNotNullTo(mutableListOf()) { span ->
                val start = maxOf(textStart, spanned.getSpanStart(span))
                val end = minOf(textEnd, spanned.getSpanEnd(span))
                val rects = collectRects(start, end, firstLineIndent)
                if (rects.isNotEmpty()) RectsToColor(rects, span.color) else null
            }

    rectsToColor.zipWithNextInPlace { a, b ->
        if (a.color == b.color) {
            a.rects.addAll(b.rects)
            remove()
        }
    }

    rectsToColor.zipWithNextInPlace { a, b ->
        val lastRect = a.rects.last()
        val nextRect = b.rects.first()
        if (lastRect.top == nextRect.top) {
            if (lastRect.right > nextRect.left) {
                lastRect.right += RADIUS
            }
        }
    }

    rectsToColor.forEach { (rects, color) ->
        paintRects(canvas, paint, color, rects)
    }
}


private data class RectsToColor(val rects: MutableList<RectF>, val color: Int)

fun paintRects(canvas: Canvas, paint: Paint, color: Int, rects: List<RectF>) {
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

private fun StaticLayout.collectRects(start: Int, end: Int, firstLineIndent: Float): MutableList<RectF> {
    val startingLineIndex = getLineForOffset(start)
    val endingLineIndex = getLineForOffset(end)

    val rects = (startingLineIndex..endingLineIndex).mapNotNullTo(mutableListOf()) { lineIndex ->
        val isLtr = getParagraphDirection(lineIndex) > 0

        val left: Float
        val right: Float

        if (isLtr) {
            left = when (lineIndex) {
                startingLineIndex -> getPrimaryHorizontal(start)
                else              -> 0f // = getLineLeft(lineIndex). Can't be the first line
            }

            right = when (lineIndex) {
                endingLineIndex -> getPrimaryHorizontal(end)
                0               -> getLineMax(lineIndex) + firstLineIndent
                else            -> getLineMax(lineIndex) // = getLineRight(lineIndex)
            }
        } else {
            right = when (lineIndex) {
                startingLineIndex -> getPrimaryHorizontal(start)
                else              -> width.f
            }

            left = when (lineIndex) {
                endingLineIndex -> getPrimaryHorizontal(end)
                else            -> width - getLineMax(lineIndex) // = getLineLeft(lineIndex)
            }
        }

        val top = when (lineIndex) {
            0    -> getLineTop(lineIndex) - topPadding // top padding is negative
            else -> getLineTop(lineIndex)
        }

        val bottom = when (lineIndex) {
            lineCount - 1 -> getLineBottom(lineIndex) - bottomPadding
            else          -> getLineBottom(lineIndex)
        }

        when {
            left < right -> RectF(left - H_BLOOM, top.f - V_BLOOM, minOf(width.f, right + H_BLOOM), bottom.f + V_BLOOM)
            left > right -> RectF(right - H_BLOOM, top.f - V_BLOOM, minOf(width.f, left + H_BLOOM), bottom.f + V_BLOOM)
            else -> null // there was null! but why? using backticks
        }
    }

    return rects
}


private fun RectF.toPath() = Path().apply { addRect(this@toPath, Path.Direction.CW) }

private fun List<RectF>.toUnionPath() = first().toPath()
        .also {
            for (i in 1 until size) {
                it.op(this[i].toPath(), Path.Op.UNION)
            }
        }


val RADIUS = 5.dp_to_pxf
val V_BLOOM = 1.dp_to_pxf * 0.5f
val H_BLOOM = 1.dp_to_pxf * 1.33f

val CORNER_PATH_EFFECT = CornerPathEffect(RADIUS)


fun <T> MutableCollection<T>.zipWithNextInPlace(process: MutableIterator<T>.(a: T, b: T) -> Unit) {
    if (size < 2) return
    val iterator = iterator()

    var current = iterator.next()
    while (iterator.hasNext()) {
        val next = iterator.next()
        iterator.process(current, next)
        current = next
    }
}
