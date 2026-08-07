package com.fingerdance

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import kotlin.math.min
import androidx.core.graphics.toColorInt

class ClosingRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : View(context, attrs) {

    // -------------------------------------------------------------------------
    // PAINTS
    // -------------------------------------------------------------------------

    /**
     * Línea que representa exactamente el perímetro
     * del botón.
     */
    private val targetPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {

            style = Paint.Style.STROKE

            strokeWidth =
                dp(2f)

            color =
                Color.parseColor("#55FFFFFF")
        }

    /**
     * Anillo principal.
     */
    private val ringPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {

            style = Paint.Style.STROKE

            strokeWidth =
                dp(5f)

            strokeCap =
                Paint.Cap.ROUND

            color =
                Color.parseColor("#8B7CFF")
        }

    /**
     * Halo exterior.
     */
    private val glowPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {

            style = Paint.Style.STROKE

            strokeWidth =
                dp(13f)

            color =
                Color.parseColor("#338B7CFF")
        }

    /**
     * Flash central.
     */
    private val flashPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {

            style = Paint.Style.STROKE

            strokeWidth =
                dp(12f)

            color =
                Color.WHITE
        }

    // -------------------------------------------------------------------------
    // ESTADO
    // -------------------------------------------------------------------------

    private var ringRadius = 0f

    private var outerRadius = 0f

    private var targetRadius = 0f

    private var flashRadius = 0f

    private var flashAlpha = 0f

    private var ringAnimator:
            ValueAnimator? = null

    private var flashAnimator:
            ValueAnimator? = null

    private var targetPulseAnimator:
            ValueAnimator? = null

    // -------------------------------------------------------------------------
    // ANIMACIÓN PRINCIPAL
    // -------------------------------------------------------------------------

    fun startClosing(
        durationMs: Long,
        onFinished: () -> Unit,
    ) {

        /*
         * Nos aseguramos de que la View ya tenga
         * dimensiones.
         */
        if (
            width <= 0 ||
            height <= 0
        ) {

            post {

                startClosing(
                    durationMs,
                    onFinished
                )
            }

            return
        }

        ringAnimator?.cancel()

        calculateDimensions()

        ringRadius =
            outerRadius

        invalidate()

        ringAnimator =
            ValueAnimator.ofFloat(
                outerRadius,
                targetRadius
            ).apply {

                duration =
                    durationMs

                /*
                 * LINEAR es importante.
                 *
                 * La posición visual representa tiempo.
                 */
                interpolator =
                    LinearInterpolator()

                addUpdateListener {

                    ringRadius =
                        it.animatedValue as Float

                    invalidate()
                }

                addListener(
                    object :
                        AnimatorListenerAdapter() {

                        private var cancelled =
                            false

                        override fun onAnimationCancel(
                            animation: Animator,
                        ) {

                            cancelled = true
                        }

                        override fun onAnimationEnd(
                            animation: Animator,
                        ) {

                            if (!cancelled) {

                                ringRadius =
                                    targetRadius

                                invalidate()

                                onFinished()
                            }
                        }
                    }
                )

                start()
            }
    }

    // -------------------------------------------------------------------------
    // EFECTOS
    // -------------------------------------------------------------------------

    /**
     * Pulso exactamente cuando el círculo
     * llega al botón.
     */
    fun pulseTarget() {

        targetPulseAnimator?.cancel()

        targetPulseAnimator =
            ValueAnimator.ofFloat(
                0f,
                1f,
                0f
            ).apply {

                duration =
                    220L

                addUpdateListener {

                    val value =
                        it.animatedValue as Float

                    targetPaint.alpha =
                        (
                                80 +
                                        (175 * value)
                                )
                            .toInt()
                            .coerceIn(
                                0,
                                255
                            )

                    invalidate()
                }

                start()
            }
    }

    fun flashSuccess() {

        startFlash(
            "#72FFFFFF".toColorInt()
        )
    }

    fun flashMiss() {

        startFlash(
            "#66FF5252".toColorInt()
        )
    }

    private fun startFlash(
        color: Int,
    ) {

        flashAnimator?.cancel()

        calculateDimensions()

        flashPaint.color =
            color

        flashRadius =
            targetRadius

        flashAlpha =
            1f

        flashAnimator =
            ValueAnimator.ofFloat(
                0f,
                1f
            ).apply {

                duration =
                    280L

                interpolator =
                    DecelerateInterpolator()

                addUpdateListener {

                    val value =
                        it.animatedValue as Float

                    flashRadius =
                        targetRadius +
                                dp(25f) * value

                    flashAlpha =
                        1f - value

                    invalidate()
                }

                start()
            }
    }

    // -------------------------------------------------------------------------
    // RESET
    // -------------------------------------------------------------------------

    fun reset() {

        ringAnimator?.cancel()

        ringRadius =
            0f

        flashAlpha =
            0f

        targetPaint.alpha =
            85

        calculateDimensions()

        invalidate()
    }

    fun stop() {

        ringAnimator?.cancel()
        flashAnimator?.cancel()
        targetPulseAnimator?.cancel()

        ringAnimator =
            null

        flashAnimator =
            null

        targetPulseAnimator =
            null

        ringRadius =
            0f

        flashAlpha =
            0f

        invalidate()
    }

    // -------------------------------------------------------------------------
    // DRAW
    // -------------------------------------------------------------------------

    override fun onDraw(
        canvas: Canvas,
    ) {

        super.onDraw(canvas)

        calculateDimensions()

        val centerX =
            width / 2f

        val centerY =
            height / 2f

        /*
         * Perímetro objetivo.
         */
        canvas.drawCircle(
            centerX,
            centerY,
            targetRadius,
            targetPaint
        )

        /*
         * Círculo animado.
         */
        if (ringRadius > 0f) {

            canvas.drawCircle(
                centerX,
                centerY,
                ringRadius,
                glowPaint
            )

            canvas.drawCircle(
                centerX,
                centerY,
                ringRadius,
                ringPaint
            )
        }

        /*
         * Flash del TAP.
         */
        if (flashAlpha > 0f) {

            flashPaint.alpha =
                (
                        255f *
                                flashAlpha
                        )
                    .toInt()
                    .coerceIn(
                        0,
                        255
                    )

            canvas.drawCircle(
                centerX,
                centerY,
                flashRadius,
                flashPaint
            )
        }
    }

    private fun calculateDimensions() {

        val size =
            min(
                width,
                height
            ).toFloat()

        if (size <= 0f)
            return

        /*
         * Nuestra View mide 340dp.
         *
         * El botón mide 180dp.
         *
         * Radio botón:
         * 90dp.
         *
         * 90 / 340 ≈ 0.265
         */
        targetRadius =
            size * 0.265f

        /*
         * Dejamos margen para que el glow
         * no se corte.
         */
        outerRadius =
            size * 0.46f
    }

    private fun dp(
        value: Float,
    ): Float {

        return value *
                resources
                    .displayMetrics
                    .density
    }
}