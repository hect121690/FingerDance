package com.fingerdance

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.util.TypedValue
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import kotlin.math.abs

class CalibrationActivity : AppCompatActivity() {

    private lateinit var mediaPlayer: MediaPlayer
    private lateinit var tapButton: Button
    private lateinit var resultTextView: TextView
    private val handler = Handler()
    private val tapTimes = mutableListOf<Long>()
    private var soundPlayTime: Long = 0
    private lateinit var outerCircle: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calibration)

        // Inicializar componentes
        tapButton = findViewById(R.id.tapButton)
        resultTextView = findViewById(R.id.resultTextView)
        outerCircle = findViewById(R.id.outerCircle)

        // Configurar el sonido que se reproducirá cada 2 segundos
        mediaPlayer = MediaPlayer.create(this, R.raw.drum)

        startSoundAndAnimationLoop()

        // Configurar el evento del botón de tap
        tapButton.setOnClickListener {
            val tapTime = System.currentTimeMillis()
            val difference = abs(tapTime - soundPlayTime) // diferencia entre tap y sonido

            tapTimes.add(difference)
            resultTextView.text = "Diferencia: $difference ms"
        }
    }

    private fun startCircleAnimation() {
        // Animador para cambiar el tamaño del círculo de 250dp a 150dp
        val animator = ValueAnimator.ofFloat(250f, 150f).apply {
            duration = 2000 // 2 segundos para que se sincronice con el sonido
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val size = (animation.animatedValue as Float).toInt()
                val params = outerCircle.layoutParams as ConstraintLayout.LayoutParams
                params.width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size.toFloat(), resources.displayMetrics).toInt()
                params.height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size.toFloat(), resources.displayMetrics).toInt()
                outerCircle.layoutParams = params
            }
        }
        animator.start()
    }

    private fun startSoundAndAnimationLoop() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                playSound() // Reproducir el sonido
                soundPlayTime = System.currentTimeMillis() // Guardar el tiempo en que suena

                startCircleAnimation() // Iniciar la animación del círculo

                handler.postDelayed(this, 2000) // Repetir cada 2 segundos
            }
        }, 2000)
    }
    private fun playSound() {
        if (mediaPlayer.isPlaying) {
            mediaPlayer.stop()
            mediaPlayer.prepare()
        }
        mediaPlayer.start()
    }

    private fun calculateAverageLatency(): Long {
        if (tapTimes.isEmpty()) return 0
        val averageLatency = tapTimes.average().toLong()
        resultTextView.text = "Latencia promedio: $averageLatency ms"

        // Aquí puedes guardar el promedio para usarlo en el juego
        return averageLatency
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer.release() // Liberar recursos de mediaPlayer
        handler.removeCallbacksAndMessages(null) // Detener el handler
    }
}
