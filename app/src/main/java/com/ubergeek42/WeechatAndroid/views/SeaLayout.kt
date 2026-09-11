package com.ubergeek42.WeechatAndroid.views

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.text.Layout
import android.text.Spannable
import android.text.StaticLayout
import android.text.TextPaint
import com.ubergeek42.WeechatAndroid.service.P
import com.ubergeek42.weechat.ColorScheme
import kotlin.math.roundToInt


class SeaLayout(
    val timeStamp: String,
    val prefix: Spannable,
    val message: Spannable,
    val width: Int,
    val colorScheme: ColorScheme,
    val textPaint: TextPaint,
) {
    val timeStampPaint: Paint get() = Paint(P.textPaint).apply { //-get
        //fontFeatureSettings = "tnum"
        color = colorScheme.chat_time[0] or -0x1000000// bg?
    }

    val prefixOffset: Float
    val messageOffset: Float

    val prefixLayout: Layout
    val messageLayout: Layout

    val height get() = messageLayout.height

    init {
        val spaceWidth = timeStampPaint.measureText("a")
        val timeStampWidth = timeStampPaint.measureText(timeStamp)

        val prefixWidth = timeStampPaint.measureText("a") * 3


        val tempLayout = StaticLayout.Builder.obtain(prefix, 0, prefix.length, P.textPaint, Int.MAX_VALUE)
                .build()

        var fittingChars = 0
        var consumedWidth = 0f
        for (i in prefix.indices) {
            val offsetForChar = tempLayout.getPrimaryHorizontal(i + 1)
            if (offsetForChar > prefixWidth) {
                break
            } else {
                consumedWidth = offsetForChar;
                fittingChars ++
            }
        }

        prefixOffset = timeStampWidth + spaceWidth + (prefixWidth - consumedWidth)

        println("   !!! ${prefix} ${message} :: fitting=$fittingChars, consumed=$consumedWidth ")

        prefixLayout = StaticLayout.Builder.obtain(prefix, 0, fittingChars, P.textPaint, consumedWidth.roundToInt())
                .build()


        messageOffset = consumedWidth + spaceWidth

        messageLayout = StaticLayout.Builder.obtain(message, 0, message.length, P.textPaint, width)
                .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NORMAL)
                .build()
    }

    fun draw(canvas: Canvas) {
        val fontMetrics = textPaint.fontMetrics
        val lineHeight = fontMetrics.bottom - fontMetrics.top + fontMetrics.leading
        println("!!! drawing: $timeStamp ${prefixLayout.text} w=${prefixLayout.width} [pref ofs: $prefixOffset, msg ofs: $messageOffset]")
        canvas.drawText(timeStamp, 0f, -fontMetrics.top, timeStampPaint)
        canvas.translate(prefixOffset, 0f)
        prefixLayout.drawText(canvas)
        canvas.translate(messageOffset, 0f)
        messageLayout.drawText(canvas)
    }
}