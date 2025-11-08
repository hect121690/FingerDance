package com.fingerdance

import android.animation.ArgbEvaluator
import android.content.Context
import android.graphics.Color
import android.os.*
import android.util.AttributeSet
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import kotlin.math.min

class HoldProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var handler: Handler? = null
    private var runnable: Runnable? = null
    private var progress = 0
    private val holdTime = 2000 // 2 segundos
    private val interval = 20   // frecuencia de actualización (ms)
    private var isHolding = false

    private val progressBar: View
    private val label: TextView

    var onHoldComplete: (() -> Unit)? = null

    init {
        // Fondo gris
        setBackgroundColor(Color.DKGRAY)

        // Barra verde que crece
        progressBar = View(context)
        progressBar.setBackgroundColor(Color.GREEN)
        progressBar.layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT)
        addView(progressBar)

        // Texto encima
        label = TextView(context)
        label.text = "Mantén presionado"
        label.setTextColor(Color.WHITE)
        label.textSize = 18f
        label.gravity = Gravity.CENTER
        label.layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
        addView(label)

        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> startHold()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> cancelHold()
            }
            true
        }
    }

    private fun startHold() {
        if (isHolding) return
        isHolding = true
        progress = 0

        handler = Handler(Looper.getMainLooper())
        runnable = object : Runnable {
            override fun run() {
                progress += interval
                val ratio = min(progress.toFloat() / holdTime, 1f)

                // Actualizar ancho de la barra
                val newWidth = (width * ratio).toInt()
                progressBar.layoutParams.width = newWidth
                progressBar.requestLayout()

                // Color dinámico (gris → verde)
                val color = ArgbEvaluator().evaluate(ratio, Color.DKGRAY, Color.GREEN) as Int
                progressBar.setBackgroundColor(color)

                if (progress >= holdTime) {
                    label.text = "Canales Reiniciados"
                    progressBar.setBackgroundColor(Color.DKGRAY)
                    isHolding = false
                    onHoldComplete?.invoke()
                } else {
                    handler?.postDelayed(this, interval.toLong())
                }
            }
        }
        handler?.post(runnable!!)
    }

    private fun cancelHold() {
        if (!isHolding) return
        handler?.removeCallbacks(runnable!!)
        handler = null
        isHolding = false
        progress = 0

        progressBar.layoutParams.width = 0
        progressBar.requestLayout()
        label.text = "Mantén presionado"
    }
}
