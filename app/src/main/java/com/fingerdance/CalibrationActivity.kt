package com.fingerdance

import android.animation.Animator
import android.animation.ValueAnimator
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.TypedValue
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.abs

class CalibrationActivity : AppCompatActivity() {

    private lateinit var soundPool: SoundPool
    private var soundId: Int = 0
    private lateinit var tapButton: Button
    private lateinit var resultTextView: TextView
    private val handler = Handler()
    private var soundPlayTime: Long = 0
    private lateinit var outerCircle: View
    private lateinit var animator :Animator
    private var difference = 0L
    private lateinit var btnSaveLatency: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibration)

        // Inicializar componentes
        tapButton = findViewById(R.id.tapButton)
        resultTextView = findViewById(R.id.resultTextView)
        outerCircle = findViewById(R.id.outerCircle)
        btnSaveLatency = findViewById(R.id.btnSaveLatency)

        // Configurar SoundPool para el sonido
        soundPool = SoundPool.Builder().setMaxStreams(1).build()
        soundId = soundPool.load(this, R.raw.drum, 1)

        animator = ValueAnimator.ofFloat(250f, 150f).apply {
            duration = 4000 // 2 segundos para que se sincronice con el sonido
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE // Repetir indefinidamente
            addUpdateListener { animation ->
                val size = (animation.animatedValue as Float).toInt()
                val params = outerCircle.layoutParams as ConstraintLayout.LayoutParams
                params.width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size.toFloat(), resources.displayMetrics).toInt()
                params.height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size.toFloat(), resources.displayMetrics).toInt()
                outerCircle.layoutParams = params
            }
        }

        startSoundAndAnimationLoop()

        tapButton.setOnClickListener {
            startCircleAnimation()
            val tapTime = SystemClock.elapsedRealtime()
            difference = abs(tapTime - soundPlayTime) // diferencia entre tap y sonido

            //tapTimes.add(difference)
            resultTextView.text = "Latency: $difference ms"
        }

        btnSaveLatency.setOnClickListener {
            latency = calculateAverageLatency()
        }
    }

    private fun startCircleAnimation() {
        animator.start()
    }

    private fun startSoundAndAnimationLoop() {
        startCircleAnimation() // Iniciar la animación del círculo

        handler.postDelayed(object : Runnable {
            override fun run() {
                playSound() // Reproducir el sonido
                soundPlayTime = SystemClock.elapsedRealtime() // Guardar el tiempo en que suena

                handler.postDelayed(this, 2000) // Repetir cada 2 segundos
            }
        }, 2000)
    }

    private fun playSound() {
        soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
    }

    private fun calculateAverageLatency(): Long {
        resultTextView.text = "Saving Latency: $difference ms"
        return difference
    }

    override fun onDestroy() {
        super.onDestroy()
        soundPool.release() // Liberar recursos de SoundPool
        handler.removeCallbacksAndMessages(null) // Detener el handler
    }
}
