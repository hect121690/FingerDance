package com.fingerdance

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.Choreographer
import android.view.View
import kotlin.math.abs

class BluetoothLatencyCalibrationView(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs), Choreographer.FrameCallback {

    private var running = false
    private var startTimeNs = 0L
    private var centerTimeNs = 0L
    private var currentFrameNs = 0L
    private var durationToCenterNs = 1_000_000_000L

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(15, 20, 34)
    }

    private val railPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(55, 65, 90)
        strokeWidth = dp(2f)
    }

    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(166, 158, 255)
        strokeWidth = dp(3f)
    }

    private val movingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(100, 87, 232)
    }

    private val targetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(225, 230, 255)
    }

    private val flashPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /*
     * Inicia una animación desde la izquierda hasta el centro.
     *
     * Devuelve el timestamp exacto en el que la bolita
     * debería llegar al centro.
     */
    fun startTrial(durationToCenterMs: Long = 1000L): Long {
        stop()

        durationToCenterNs = durationToCenterMs * 1_000_000L
        startTimeNs = System.nanoTime()
        centerTimeNs = startTimeNs + durationToCenterNs
        currentFrameNs = startTimeNs
        running = true

        Choreographer.getInstance().postFrameCallback(this)
        invalidate()

        return centerTimeNs
    }

    fun reset() {
        stop()
        startTimeNs = 0L
        centerTimeNs = 0L
        currentFrameNs = 0L
        invalidate()
    }

    fun stop() {
        running = false
        Choreographer.getInstance().removeFrameCallback(this)
    }

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return

        currentFrameNs = frameTimeNanos
        invalidate()

        /*
         * Dejamos correr unos milisegundos después del centro
         * para que termine el destello.
         */
        if (frameTimeNanos <= centerTimeNs + 140_000_000L) {
            Choreographer.getInstance().postFrameCallback(this)
        } else {
            running = false
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.drawRoundRect(
            0f, 0f,
            width.toFloat(), height.toFloat(),
            dp(18f), dp(18f),
            backgroundPaint
        )

        if (width <= 0 || height <= 0) return

        val centerX = width / 2f
        val centerY = height / 2f
        val leftX = dp(22f)

        canvas.drawLine(leftX, centerY, width - dp(22f), centerY, railPaint)
        canvas.drawLine(centerX, dp(25f), centerX, height - dp(25f), centerPaint)

        canvas.drawCircle(centerX, centerY, dp(8f), targetPaint)

        if (startTimeNs == 0L) {
            canvas.drawCircle(leftX, centerY, dp(10f), movingPaint)
            return
        }

        val elapsed = currentFrameNs - startTimeNs
        val progress = (elapsed.toDouble() / durationToCenterNs.toDouble())
            .coerceIn(0.0, 1.0)
            .toFloat()

        /*
         * progress 0 = izquierda
         * progress 1 = centro EXACTO.
         */
        val ballX = leftX + ((centerX - leftX) * progress)

        canvas.drawCircle(ballX, centerY, dp(10f), movingPaint)
        drawCenterFlash(canvas, centerX, centerY)
    }

    private fun drawCenterFlash(canvas: Canvas, centerX: Float, centerY: Float) {
        if (centerTimeNs == 0L) return

        val delta = abs(currentFrameNs - centerTimeNs)
        val flashWindowNs = 110_000_000L

        if (delta > flashWindowNs) return

        val progress = 1f - (delta.toFloat() / flashWindowNs.toFloat()).coerceIn(0f, 1f)
        val radius = dp(18f + (32f * progress))

        flashPaint.shader = RadialGradient(
            centerX,
            centerY,
            radius,
            intArrayOf(
                Color.argb((235 * progress).toInt(), 240, 245, 255),
                Color.argb((135 * progress).toInt(), 100, 87, 232),
                Color.TRANSPARENT
            ),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP
        )

        canvas.drawCircle(centerX, centerY, radius, flashPaint)
        flashPaint.shader = null
    }

    private fun dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }
}