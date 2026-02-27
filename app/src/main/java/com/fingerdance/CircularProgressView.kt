package com.fingerdance

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var progress = 0f
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 20f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#E8E8E8")
        strokeWidth = 20f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2196F3")
        textSize = 80f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#BDBDBD")
        textSize = 20f
        textAlign = Paint.Align.CENTER
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setProgress(newProgress: Float) {
        progress = newProgress.coerceIn(0f, 100f)

        // Actualizar color según progreso
        if (progress < 100f) {
            // Gradiente magenta a azul
            val shader = LinearGradient(
                0f, 0f,
                width.toFloat(), height.toFloat(),
                Color.parseColor("#E91E63"),
                Color.parseColor("#2196F3"),
                Shader.TileMode.CLAMP
            )
            progressPaint.shader = shader
            textPaint.color = Color.parseColor("#2196F3")
        } else {
            // Verde cuando completa
            val shader = LinearGradient(
                0f, 0f,
                width.toFloat(), height.toFloat(),
                Color.parseColor("#66BB6A"),
                Color.parseColor("#2E7D32"),
                Shader.TileMode.CLAMP
            )
            progressPaint.shader = shader
            textPaint.color = Color.parseColor("#2E7D32")
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f
        val radius = (width / 2f) - 60f

        // Dibujar fondo del círculo
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint)

        // Dibujar progreso
        val sweepAngle = (progress / 100f) * 360f
        canvas.drawArc(
            centerX - radius,
            centerY - radius,
            centerX + radius,
            centerY + radius,
            -90f,
            sweepAngle,
            false,
            progressPaint
        )

        // Dibujar texto "PROGRESS"
        canvas.drawText("PROGRESS", centerX, centerY - 50f, labelPaint)

        // Dibujar porcentaje
        canvas.drawText("${progress.toInt()}%", centerX, centerY + 60f, textPaint)
    }
}
