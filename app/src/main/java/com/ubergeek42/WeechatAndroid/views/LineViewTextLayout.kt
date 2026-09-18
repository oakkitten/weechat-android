package com.ubergeek42.WeechatAndroid.views

import android.graphics.Canvas
import android.view.View
import androidx.core.graphics.withTranslation
import com.ubergeek42.WeechatAndroid.relay.PrecomputedLineLayout

class LineViewTextLayout(private val precomputedLineLayout: PrecomputedLineLayout, val viewWidth: Int, val view: View) {
    val messageLayout = precomputedLineLayout.obtainMessageLayout(viewWidth)
    val height = messageLayout.height

    val alphaDrawer = view.getAlphaDrawer().apply { record(viewWidth, height, ::draw) }

    fun draw(canvas: Canvas, alpha: Float) {
        alphaDrawer.drawWithAlpha(canvas, alpha)
    }

    fun draw(canvas: Canvas) {
        precomputedLineLayout.draw(canvas)
        canvas.withTranslation(precomputedLineLayout.messageOffset, 0f) { messageLayout.draw(canvas) }
    }

    fun getMessageTextOffsetForCoordinates(x: Float, y: Float): Int {
        return 0
    }
}
