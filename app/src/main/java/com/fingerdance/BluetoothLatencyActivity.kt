package com.fingerdance

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import kotlin.math.abs
import kotlin.math.roundToInt

class BluetoothLatencyActivity :
    AppCompatActivity() {

    private lateinit var themes:
            SharedPreferences

    private lateinit var txtStatus:
            TextView

    private lateinit var txtCountdown:
            TextView

    private lateinit var txtProgress:
            TextView

    private lateinit var txtTiming:
            TextView

    private lateinit var txtResult:
            TextView

    private lateinit var txtDeviceModel:
            TextView

    private lateinit var txtSavedLatency:
            TextView

    private lateinit var btnTap:
            MaterialButton

    private lateinit var btnStart:
            MaterialButton

    private lateinit var ringView:
            ClosingRingView


    // ========================================================================
    // DISPOSITIVO / PERFIL
    // ========================================================================

    private var currentDevice:
            BluetoothLatencyProfileManager
            .BluetoothAudioDevice? = null

    private var existingProfile:
            BluetoothLatencyProfileManager
            .BluetoothLatencyProfile? = null


    // ========================================================================
    // AUDIO
    // ========================================================================

    private lateinit var soundPool:
            SoundPool

    private var drumSoundId =
        0

    private var soundLoaded =
        false

    private var lastSoundTimeNs =
        0L

    private var waitingForTap =
        false


    // ========================================================================
    // TIMING
    // ========================================================================

    private val handler =
        Handler(
            Looper.getMainLooper()
        )

    private var calibrationRunning =
        false

    private val totalSamples =
        12

    private val samples =
        mutableListOf<Double>()

    private var estimatedOffsetMs =
        0.0

    private val cycleDurationMs =
        2000L

    private var visualCycleNumber =
        0

    private var scheduledSoundRunnable:
            Runnable? = null


    // ========================================================================
    // CREATE
    // ========================================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_bluetooth_latency
        )

        /*
         * Usa la misma inicialización de themes
         * que ya tienes en tu proyecto.
         */
        themes =
            getSharedPreferences(
                "themes",
                MODE_PRIVATE
            )

        bindViews()

        /*
         * Tenemos que tener Bluetooth conectado.
         */
        currentDevice =
            BluetoothLatencyProfileManager
                .getConnectedBluetoothDevice(
                    this
                )

        if (currentDevice == null) {

            /*
             * Esta Activity nunca debería abrirse sin Bluetooth.
             */
            finish()

            return
        }

        loadExistingProfile()

        initSoundPool()

        setupListeners()

        updateProgress()
    }


    private fun bindViews() {

        txtStatus =
            findViewById(
                R.id.txtStatus
            )

        txtCountdown =
            findViewById(
                R.id.txtCountdown
            )

        txtProgress =
            findViewById(
                R.id.txtProgress
            )

        txtTiming =
            findViewById(
                R.id.txtTiming
            )

        txtResult =
            findViewById(
                R.id.txtResult
            )

        txtDeviceModel =
            findViewById(
                R.id.txtDeviceModel
            )

        txtSavedLatency =
            findViewById(
                R.id.txtSavedLatency
            )

        btnTap =
            findViewById(
                R.id.btnTap
            )

        btnStart =
            findViewById(
                R.id.btnStart
            )

        ringView =
            findViewById(
                R.id.ringView
            )
    }


    // ========================================================================
    // PERFIL
    // ========================================================================

    private fun loadExistingProfile() {

        val device =
            currentDevice
                ?: return

        existingProfile =
            BluetoothLatencyProfileManager
                .getProfile(
                    themes,
                    device.id
                )

        txtDeviceModel.text =
            device.model

        val profile =
            existingProfile

        if (profile != null) {

            txtSavedLatency.text =
                "${profile.latencyMs} ms"

            btnStart.text =
                "VOLVER A CALIBRAR"

            /*
             * También mostramos inicialmente
             * la calibración existente.
             */
            txtTiming.text =
                "${profile.latencyMs} ms"

        } else {

            txtSavedLatency.text =
                "0 ms"

            btnStart.text =
                "INICIAR CALIBRACIÓN"

            txtTiming.text =
                "-- ms"
        }

        txtStatus.text =
            "Toca el botón cuando escuches el sonido"

        btnTap.isEnabled =
            false

        txtCountdown.visibility =
            View.INVISIBLE
    }


    // ========================================================================
    // SOUNDPOOL
    // ========================================================================

    private fun initSoundPool() {

        val attributes =
            AudioAttributes.Builder()
                .setUsage(
                    AudioAttributes.USAGE_GAME
                )
                .setContentType(
                    AudioAttributes.CONTENT_TYPE_SONIFICATION
                )
                .build()

        soundPool =
            SoundPool.Builder()
                .setMaxStreams(1)
                .setAudioAttributes(
                    attributes
                )
                .build()

        btnStart.isEnabled =
            false

        soundPool.setOnLoadCompleteListener {
                _,
                sampleId,
                status ->

            if (
                status == 0 &&
                sampleId == drumSoundId
            ) {

                soundLoaded =
                    true

                btnStart.isEnabled =
                    true
            }
        }

        drumSoundId =
            soundPool.load(
                this,
                R.raw.drum,
                1
            )
    }


    // ========================================================================
    // LISTENERS
    // ========================================================================

    @Suppress("ClickableViewAccessibility")
    private fun setupListeners() {

        btnStart.setOnClickListener {

            startCalibration()
        }

        btnTap.setOnTouchListener {
                _,
                event ->

            if (
                event.action ==
                MotionEvent.ACTION_DOWN
            ) {

                animateTapButton()

                registerTap()

                true

            } else {

                false
            }
        }
    }


    // ========================================================================
    // START
    // ========================================================================

    private fun startCalibration() {

        if (!soundLoaded)
            return

        /*
         * Comprobamos nuevamente que sigan conectados.
         */
        val connected =
            BluetoothLatencyProfileManager
                .getConnectedBluetoothDevice(
                    this
                )

        if (connected == null) {

            txtStatus.text =
                "Audífonos Bluetooth desconectados"

            return
        }

        /*
         * Si cambió el dispositivo entre que abrió
         * la Activity y pulsó iniciar:
         *
         * trabajamos con el nuevo.
         */
        currentDevice =
            connected

        existingProfile =
            BluetoothLatencyProfileManager
                .getProfile(
                    themes,
                    connected.id
                )

        txtDeviceModel.text =
            connected.model

        cancelCalibrationJobs()

        samples.clear()

        estimatedOffsetMs =
            0.0

        visualCycleNumber =
            0

        waitingForTap =
            false

        calibrationRunning =
            true

        txtResult.text =
            ""

        txtTiming.text =
            "-- ms"

        btnStart.isEnabled =
            false

        btnTap.isEnabled =
            false

        ringView.reset()

        updateProgress()

        txtStatus.text =
            "Toca el botón cuando escuches el sonido"

        startCountdown()
    }


    // ========================================================================
    // COUNTDOWN
    // ========================================================================

    private fun startCountdown() {

        txtCountdown.visibility =
            View.VISIBLE

        showCountdownNumber(3) {

            showCountdownNumber(2) {

                showCountdownNumber(1) {

                    finishCountdown()
                }
            }
        }
    }


    private fun showCountdownNumber(
        number: Int,
        finished: () -> Unit
    ) {

        txtCountdown.animate()
            .cancel()

        txtCountdown.text =
            number.toString()

        txtCountdown.visibility =
            View.VISIBLE

        txtCountdown.alpha =
            0f

        txtCountdown.scaleX =
            0.75f

        txtCountdown.scaleY =
            0.75f

        txtCountdown.animate()
            .alpha(1f)
            .scaleX(1.35f)
            .scaleY(1.35f)
            .setDuration(250L)
            .setInterpolator(
                DecelerateInterpolator()
            )
            .setListener(null)
            .withEndAction {

                handler.postDelayed({

                    if (!calibrationRunning)
                        return@postDelayed

                    txtCountdown.animate()
                        .alpha(0f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(250L)
                        .setListener(null)
                        .withEndAction {

                            if (
                                calibrationRunning
                            ) {

                                finished()
                            }
                        }
                        .start()

                }, 250L)
            }
            .start()
    }


    private fun finishCountdown() {

        txtCountdown.visibility =
            View.INVISIBLE

        btnTap.isEnabled =
            true

        /*
         * Primer sonido inmediatamente.
         */
        playCalibrationSound()

        /*
         * Y comienza el primer círculo.
         */
        startVisualCycle()
    }


    // ========================================================================
    // CICLO
    // ========================================================================

    private fun startVisualCycle() {

        if (!calibrationRunning)
            return

        if (
            samples.size >=
            totalSamples
        ) {

            finishCalibration()

            return
        }

        visualCycleNumber++

        ringView.reset()

        /*
         * El primer círculo queda sin compensación.
         *
         * Después usamos el valor aprendido.
         */
        val compensationMs =
            if (
                visualCycleNumber == 1
            ) {

                0.0

            } else {

                estimatedOffsetMs.coerceIn(
                    0.0,
                    800.0
                )
            }

        val soundDelayMs =
            (
                    cycleDurationMs -
                            compensationMs
                    )
                .roundToInt()
                .toLong()
                .coerceAtLeast(0L)

        scheduledSoundRunnable
            ?.let {

                handler.removeCallbacks(
                    it
                )
            }

        scheduledSoundRunnable =
            Runnable {

                if (!calibrationRunning)
                    return@Runnable

                /*
                 * Evita asociar un TAP
                 * al sonido incorrecto.
                 */
                if (!waitingForTap) {

                    playCalibrationSound()
                }
            }

        handler.postDelayed(
            scheduledSoundRunnable!!,
            soundDelayMs
        )

        ringView.startClosing(
            cycleDurationMs
        ) {

            if (!calibrationRunning)
                return@startClosing

            ringView.pulseTarget()

            startVisualCycle()
        }
    }


    // ========================================================================
    // SOUND
    // ========================================================================

    private fun playCalibrationSound() {

        if (!calibrationRunning)
            return

        lastSoundTimeNs =
            SystemClock
                .elapsedRealtimeNanos()

        soundPool.play(
            drumSoundId,
            1f,
            1f,
            1,
            0,
            1f
        )

        waitingForTap =
            true

        txtStatus.text =
            "¡Toca cuando escuches el sonido!"
    }


    // ========================================================================
    // TAP
    // ========================================================================

    private fun registerTap() {

        if (!calibrationRunning)
            return

        if (!waitingForTap)
            return

        val tapTimeNs =
            SystemClock
                .elapsedRealtimeNanos()

        waitingForTap =
            false

        val differenceMs =
            (
                    tapTimeNs -
                            lastSoundTimeNs
                    ) / 1_000_000.0

        /*
         * Filtro básico.
         */
        if (
            differenceMs < 40.0 ||
            differenceMs > 1200.0
        ) {

            txtStatus.text =
                "Toque descartado"

            ringView.flashMiss()

            return
        }

        samples.add(
            differenceMs
        )

        updateEstimatedOffset()

        txtTiming.text =
            "${differenceMs.roundToInt()} ms"

        txtStatus.text =
            "Sincronizando..."

        updateProgress()

        ringView.flashSuccess()

        if (
            samples.size >=
            totalSamples
        ) {

            finishCalibration()
        }
    }


    // ========================================================================
    // OFFSET ADAPTATIVO
    // ========================================================================

    private fun updateEstimatedOffset() {

        if (samples.isEmpty()) {

            estimatedOffsetMs =
                0.0

            return
        }

        var recent =
            samples.takeLast(7)

        if (
            recent.size >= 5
        ) {

            val median =
                calculateMedian(
                    recent
                )

            recent =
                recent.filter {

                    abs(
                        it - median
                    ) <= 180.0
                }
        }

        if (recent.isNotEmpty()) {

            estimatedOffsetMs =
                calculateMedian(
                    recent
                )
        }
    }


    // ========================================================================
    // FINISH
    // ========================================================================

    private fun finishCalibration() {

        if (!calibrationRunning)
            return

        calibrationRunning =
            false

        waitingForTap =
            false

        scheduledSoundRunnable
            ?.let {

                handler.removeCallbacks(
                    it
                )
            }

        ringView.stop()

        btnTap.isEnabled =
            false

        btnStart.isEnabled =
            true

        txtStatus.text =
            "Calibración terminada"

        if (
            samples.size < 3
        ) {

            txtResult.text =
                "No hay suficientes mediciones"

            return
        }

        var validSamples =
            if (
                samples.size > 4
            ) {

                samples.drop(2)

            } else {

                samples.toList()
            }

        if (
            validSamples.size >= 6
        ) {

            validSamples =
                validSamples
                    .sorted()
                    .drop(1)
                    .dropLast(1)
        }

        val median =
            calculateMedian(
                validSamples
            )

        val average =
            validSamples.average()

        val finalOffset =
            median.roundToInt()

        txtTiming.text =
            "$finalOffset ms"

        txtResult.text =
            buildString {

                append(
                    "Calibración guardada"
                )

                append("\n")

                append(
                    "Mediana: %.1f ms"
                        .format(median)
                )

                append("\n")

                append(
                    "Promedio: %.1f ms"
                        .format(average)
                )
            }

        /*
         * ==========================================================
         * GUARDAR EN themes
         * ==========================================================
         */

        val device =
            currentDevice

        if (device != null) {

            BluetoothLatencyProfileManager
                .saveProfile(
                    themes = themes,
                    device = device,
                    latencyMs = finalOffset
                )

            /*
             * Refrescamos la sección superior.
             */
            existingProfile =
                BluetoothLatencyProfileManager
                    .getProfile(
                        themes,
                        device.id
                    )

            txtDeviceModel.text =
                device.model

            txtSavedLatency.text =
                "$finalOffset ms"

            btnStart.text =
                "VOLVER A CALIBRAR"
        }
    }


    // ========================================================================
    // MEDIANA
    // ========================================================================

    private fun calculateMedian(
        values: List<Double>
    ): Double {

        if (values.isEmpty())
            return 0.0

        val sorted =
            values.sorted()

        val middle =
            sorted.size / 2

        return if (
            sorted.size % 2 == 0
        ) {

            (
                    sorted[middle - 1] +
                            sorted[middle]
                    ) / 2.0

        } else {

            sorted[middle]
        }
    }


    // ========================================================================
    // UI
    // ========================================================================

    private fun updateProgress() {

        txtProgress.text =
            "${samples.size} / $totalSamples"
    }


    private fun animateTapButton() {

        btnTap.animate()
            .cancel()

        btnTap.scaleX =
            1f

        btnTap.scaleY =
            1f

        btnTap.animate()
            .scaleX(0.90f)
            .scaleY(0.90f)
            .setDuration(60L)
            .setListener(
                object :
                    AnimatorListenerAdapter() {

                    override fun onAnimationEnd(
                        animation: Animator
                    ) {

                        btnTap.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(110L)
                            .setListener(null)
                            .start()
                    }
                }
            )
            .start()
    }


    // ========================================================================
    // CLEANUP
    // ========================================================================

    private fun cancelCalibrationJobs() {

        handler.removeCallbacksAndMessages(
            null
        )

        scheduledSoundRunnable =
            null

        txtCountdown.animate()
            .cancel()

        btnTap.animate()
            .cancel()

        ringView.stop()
    }


    override fun onDestroy() {

        calibrationRunning =
            false

        cancelCalibrationJobs()

        if (
            ::soundPool.isInitialized
        ) {

            soundPool.release()
        }

        super.onDestroy()
    }
}