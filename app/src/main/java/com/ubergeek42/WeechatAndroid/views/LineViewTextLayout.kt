package com.ubergeek42.WeechatAndroid.views

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RenderNode
import android.os.Build
import android.view.View
import androidx.core.graphics.withTranslation
import com.ubergeek42.WeechatAndroid.relay.PrecomputedLineLayout

class LineViewTextLayout(private val precomputedLineLayout: PrecomputedLineLayout, val viewWidth: Int) {
    val renderNode = RenderNode("LineViewTextLayout")
    val messageLayout = precomputedLineLayout.obtainMessageLayout(viewWidth)
    val height = messageLayout.height

    fun draw(canvas: Canvas, alpha: Float) {
        //require(canvas.isHardwareAccelerated())
        renderNode.recordWithAlpha(alpha) { recordingCanvas ->
            renderNode.setPosition(0, 0, canvas.width, canvas.height)
            precomputedLineLayout.draw(recordingCanvas)
            recordingCanvas.withTranslation(precomputedLineLayout.messageOffset, 0f) { messageLayout.draw(recordingCanvas) }
        }

        canvas.drawRenderNode(renderNode)
    }

    fun draw(canvas: Canvas) {
        precomputedLineLayout.draw(canvas)
        canvas.withTranslation(precomputedLineLayout.messageOffset, 0f) { messageLayout.draw(canvas) }
    }

    fun getMessageTextOffsetForCoordinates(x: Float, y: Float): Int {
        return 0
    }
}

//fun RenderNode.recordWithAlpha(alpha: Float, block: (recordingCanvas: Canvas) -> Unit) {
//    this.alpha = alpha
//    val recordingCanvas = beginRecording()
//    block(recordingCanvas)
//    endRecording()
//}


fun View.getAlphaDrawer(): AlphaDrawer =
    if (Build.VERSION.SDK_INT >= 29 && isHardwareAccelerated) {
        AlphaDrawerImpl29()
    } else {
        AlphaDrawerImpl26()
    }


interface AlphaDrawer {
    fun withAlpha(canvas: Canvas, alpha: Float, block: (recordingCanvas: Canvas) -> Unit)
    fun releaseResources()
}


class AlphaDrawerImpl29: AlphaDrawer {
    val renderNode = RenderNode("AlphaDrawerImpl29")

    // Canvas MUST be hardware accelerated!
    override fun withAlpha(canvas: Canvas, alpha: Float, block: (recordingCanvas: Canvas) -> Unit) {
        renderNode.setPosition(0, 0, canvas.width, canvas.height)
        val recordingCanvas = renderNode.beginRecording()

        try {
            block(recordingCanvas)
        } finally {
            renderNode.endRecording()
        }

        renderNode.alpha = alpha // 0..1f
        canvas.drawRenderNode(renderNode)
    }

    override fun releaseResources() {
        renderNode.discardDisplayList()
    }
}


class AlphaDrawerImpl26: AlphaDrawer {
    var bitmap: Bitmap? = null
    var bitmapCanvas: Canvas? = null

    // Canvas MUST be hardware accelerated!
    override fun withAlpha(canvas: Canvas, alpha: Float, block: (recordingCanvas: Canvas) -> Unit) {
        val bitmap = bitmap ?: Bitmap.createBitmap(canvas.width, canvas.height, Bitmap.Config.ARGB_8888)
                .also {
                    bitmap = it
                    bitmapCanvas = Canvas(it)
                }

        block(bitmapCanvas!!)

        alphaPaint.alpha = (alpha * 255).toInt()
        canvas.drawBitmap(bitmap, 0f, 0f, alphaPaint)
    }

    override fun releaseResources() {
        bitmap = null
        bitmapCanvas = null
    }
}

private val alphaPaint = Paint()
