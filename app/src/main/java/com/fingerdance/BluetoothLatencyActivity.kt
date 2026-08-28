package com.fingerdance

import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import kotlin.math.roundToInt

class BluetoothLatencyActivity : AppCompatActivity() {

    private lateinit var themes: SharedPreferences

    private lateinit var txtDeviceModel: TextView
    private lateinit var txtSavedLatency: TextView
    private lateinit var txtStatus: TextView
    private lateinit var txtProgress: TextView
    private lateinit var txtCountdown: TextView
    private lateinit var txtLatency: TextView

    private lateinit var btnTap: MaterialButton
    private lateinit var btnSave: MaterialButton
    private lateinit var calibrationView: BluetoothLatencyCalibrationView

    private var currentDevice: BluetoothLatencyProfileManager.BluetoothAudioDevice? = null
    private var existingProfile: BluetoothLatencyProfileManager.BluetoothLatencyProfile? = null

    private var mediaPlayer: MediaPlayer? = null
    private var audioPrepared = false

    private val handler = Handler(Looper.getMainLooper())

    /*
     * Nuestro MP3 tiene beats en:
     *
     * 1s, 3s, 5s, 7s...
     *
     * Por ahora usamos siempre el primer beat:
     * 1000 ms.
     */
    private val firstBeatMs = 1000L

    /*
     * La animación tarda exactamente lo mismo
     * en llegar desde izquierda hasta el centro.
     */
    private val timeToCenterMs = 1000L

    private val totalSamples = 8
    private val pauseBetweenTrialsMs = 900L

    /*
     * No aplicamos el error completo para evitar
     * que la calibración oscile demasiado.
     */
    private val correctionStrength = 0.65

    private var estimatedLatencyMs = 0.0
    private var centerTimeNs = 0L

    private var waitingForTap = false
    private var calibrationFinished = false
    private var calibrationStarted = false

    private val errors = mutableListOf<Double>()

    private var startVisualRunnable: Runnable? = null
    private var timeoutRunnable: Runnable? = null
    private var nextTrialRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bluetooth_latency)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        themes = getSharedPreferences("themes", MODE_PRIVATE)

        bindViews()

        currentDevice = BluetoothLatencyProfileManager.getConnectedBluetoothDevice(this)

        if (currentDevice == null) {
            finish()
            return
        }

        loadProfile()
        setupListeners()
        initMediaPlayer()
    }

    private fun bindViews() {
        txtDeviceModel = findViewById(R.id.txtDeviceModel)
        txtSavedLatency = findViewById(R.id.txtSavedLatency)
        txtStatus = findViewById(R.id.txtStatus)
        txtProgress = findViewById(R.id.txtProgress)
        txtCountdown = findViewById(R.id.txtCountdown)
        txtLatency = findViewById(R.id.txtLatency)

        btnTap = findViewById(R.id.btnTap)
        btnSave = findViewById(R.id.btnSave)

        calibrationView = findViewById(R.id.calibrationView)
    }

    private fun loadProfile() {
        val device = currentDevice ?: return

        existingProfile = BluetoothLatencyProfileManager.getProfile(
            device.id
        )

        txtDeviceModel.text = device.model

        txtSavedLatency.text = existingProfile?.let {
            "${it.latencyMs} ms"
        } ?: "-- ms"

        txtLatency.text = "-- ms"
        txtProgress.text = "0 / $totalSamples"
        txtCountdown.visibility = View.INVISIBLE
    }

    // ============================================================
    // AUDIO
    // ============================================================

    private fun initMediaPlayer() {
        txtStatus.text = "Preparando audio..."

        try {
            val afd = assets.openFd("fingerdance_latency_16s.mp3")

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )

                setDataSource(
                    afd.fileDescriptor,
                    afd.startOffset,
                    afd.length
                )

                setVolume(1f, 1f)
                isLooping = false

                setOnPreparedListener {
                    audioPrepared = true
                    startCalibration()
                }

                setOnErrorListener { _, what, extra ->
                    txtStatus.text = "Error preparando audio ($what / $extra)"
                    true
                }

                prepareAsync()
            }

            afd.close()

        } catch (e: Exception) {
            txtStatus.text = "No se pudo abrir el audio de calibración"
            e.printStackTrace()
        }
    }

    private fun restartAudio(onStarted: () -> Unit) {
        val player = mediaPlayer ?: return

        if (!audioPrepared || calibrationFinished) return

        try {
            if (player.isPlaying) {
                player.pause()
            }

            /*
             * Esperamos realmente a que MediaPlayer haya
             * regresado al principio antes de iniciar.
             */
            player.setOnSeekCompleteListener {
                it.setOnSeekCompleteListener(null)

                if (calibrationFinished) return@setOnSeekCompleteListener

                it.start()

                /*
                 * Este callback significa:
                 * ya enviamos el comando START del audio.
                 *
                 * Desde aquí coordinamos el visual.
                 */
                onStarted()
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                player.seekTo(
                    0L,
                    MediaPlayer.SEEK_CLOSEST
                )
            } else {
                @Suppress("DEPRECATION")
                player.seekTo(0)
            }

        } catch (e: Exception) {
            e.printStackTrace()
            txtStatus.text = "Error reiniciando audio"
        }
    }

    // ============================================================
    // CALIBRACIÓN
    // ============================================================

    private fun startCalibration() {
        cancelCalibrationCallbacks()

        errors.clear()

        estimatedLatencyMs = 0.0
        centerTimeNs = 0L

        waitingForTap = false
        calibrationFinished = false
        calibrationStarted = true

        btnTap.isEnabled = false
        btnSave.isEnabled = false

        txtLatency.text = "-- ms"
        txtProgress.text = "0 / $totalSamples"
        txtStatus.text = "Presiona TAP cuando escuches el sonido"

        txtCountdown.animate().cancel()
        txtCountdown.visibility = View.INVISIBLE

        calibrationView.reset()

        startCountdown()
    }

    // ============================================================
    // COUNTDOWN
    // ============================================================

    private fun startCountdown() {
        btnTap.isEnabled = false
        waitingForTap = false

        txtStatus.text = "Presiona TAP cuando escuches el sonido"
        txtCountdown.visibility = View.VISIBLE

        showCountdownNumber(3) {
            showCountdownNumber(2) {
                showCountdownNumber(1) {
                    txtCountdown.visibility = View.INVISIBLE
                    startCalibrationTrial()
                }
            }
        }
    }

    private fun showCountdownNumber(number: Int, finished: () -> Unit) {
        txtCountdown.animate().cancel()

        txtCountdown.text = number.toString()
        txtCountdown.visibility = View.VISIBLE
        txtCountdown.alpha = 0f
        txtCountdown.scaleX = 0.70f
        txtCountdown.scaleY = 0.70f

        txtCountdown.animate()
            .alpha(1f)
            .scaleX(1.30f)
            .scaleY(1.30f)
            .setDuration(220L)
            .setInterpolator(DecelerateInterpolator())
            .withEndAction {
                handler.postDelayed({
                    txtCountdown.animate()
                        .alpha(0f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(220L)
                        .withEndAction {
                            if (!calibrationFinished) {
                                finished()
                            }
                        }
                        .start()
                }, 350L)
            }
            .start()
    }

    // ============================================================
    // PRUEBA
    // ============================================================

    private fun startCalibrationTrial() {
        if (!calibrationStarted || calibrationFinished) return

        if (errors.size >= totalSamples) {
            finishCalibration()
            return
        }

        cancelTrialCallbacks()

        waitingForTap = false
        btnTap.isEnabled = false

        calibrationView.reset()

        txtProgress.text = "${errors.size} / $totalSamples"
        txtStatus.text = "Prepárate..."

        /*
         * Reiniciamos el MP3.
         *
         * Cuando MediaPlayer comienza:
         *
         * audioStart
         *     |
         *     |  estimatedLatencyMs
         *     |
         *     +------> arranca visual
         *
         * Después el visual tarda 1000 ms en llegar
         * al centro.
         *
         * El MP3 también tiene su primer beat
         * a los 1000 ms.
         */
        restartAudio {
            val visualDelayMs = estimatedLatencyMs
                .roundToInt()
                .coerceIn(0, 800)
                .toLong()

            startVisualRunnable = Runnable {
                if (calibrationFinished) return@Runnable

                centerTimeNs = calibrationView.startTrial(timeToCenterMs)

                waitingForTap = true
                btnTap.isEnabled = true

                txtStatus.text = "Presiona TAP cuando escuches el beat"

                startTrialTimeout()
            }

            handler.postDelayed(
                startVisualRunnable!!,
                visualDelayMs
            )
        }
    }

    /*
     * Si el usuario no toca cerca del primer beat,
     * repetimos la prueba.
     */
    private fun startTrialTimeout() {
        timeoutRunnable?.let {
            handler.removeCallbacks(it)
        }

        timeoutRunnable = Runnable {
            if (!waitingForTap || calibrationFinished) {
                return@Runnable
            }

            waitingForTap = false
            btnTap.isEnabled = false

            txtStatus.text = "No se detectó toque, repetimos..."

            scheduleNextTrial()
        }

        /*
         * Centro = +1000 ms.
         * Damos 900 ms extra para aceptar el toque.
         */
        handler.postDelayed(
            timeoutRunnable!!,
            timeToCenterMs + 900L
        )
    }

    // ============================================================
    // TAP
    // ============================================================

    @Suppress("ClickableViewAccessibility")
    private fun setupListeners() {
        btnTap.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                registerTap()
                animateTap()
                true
            } else {
                false
            }
        }

        btnSave.setOnClickListener {
            saveCalibration()
        }
    }

    private fun registerTap() {
        if (!waitingForTap || calibrationFinished) return
        if (centerTimeNs == 0L) return

        val tapTimeNs = System.nanoTime()

        waitingForTap = false
        btnTap.isEnabled = false

        timeoutRunnable?.let {
            handler.removeCallbacks(it)
        }

        val errorMs = (
                tapTimeNs - centerTimeNs
                ) / 1_000_000.0

        /*
         * Ignoramos taps demasiado alejados del centro.
         */
        if (errorMs < -700.0 || errorMs > 700.0) {
            txtStatus.text = "Toque descartado"
            scheduleNextTrial()
            return
        }

        errors.add(errorMs)

        estimatedLatencyMs += errorMs * correctionStrength
        estimatedLatencyMs = estimatedLatencyMs.coerceIn(0.0, 800.0)

        txtLatency.text = "${estimatedLatencyMs.roundToInt()} ms"
        txtProgress.text = "${errors.size} / $totalSamples"

        txtStatus.text = when {
            errorMs > 80.0 -> "Tarde..."
            errorMs > 30.0 -> "Un poco tarde..."
            errorMs < -80.0 -> "Temprano..."
            errorMs < -30.0 -> "Un poco temprano..."
            else -> "¡Muy cerca!"
        }

        if (errors.size >= totalSamples) {
            handler.postDelayed({
                finishCalibration()
            }, 650L)
        } else {
            scheduleNextTrial()
        }
    }

    // ============================================================
    // SIGUIENTE PRUEBA
    // ============================================================

    private fun scheduleNextTrial() {
        nextTrialRunnable?.let {
            handler.removeCallbacks(it)
        }

        nextTrialRunnable = Runnable {
            startCalibrationTrial()
        }

        handler.postDelayed(
            nextTrialRunnable!!,
            pauseBetweenTrialsMs
        )
    }

    // ============================================================
    // FIN
    // ============================================================

    private fun finishCalibration() {
        if (calibrationFinished) return

        calibrationFinished = true
        waitingForTap = false

        cancelCalibrationCallbacks()

        btnTap.isEnabled = false
        btnSave.isEnabled = true

        calibrationView.stop()

        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.pause()
                }
            } catch (_: Exception) {
            }
        }

        txtProgress.text = "$totalSamples / $totalSamples"
        txtLatency.text = "${estimatedLatencyMs.roundToInt()} ms"
        txtStatus.text = "Calibración terminada"
    }

    private fun saveCalibration() {
        val device = currentDevice ?: return
        if (!calibrationFinished) return

        val finalLatency = estimatedLatencyMs
            .roundToInt()
            .coerceIn(0, 800)

        BluetoothLatencyProfileManager.saveProfile(
            device = device,
            latencyMs = finalLatency
        )

        existingProfile = BluetoothLatencyProfileManager.getProfile(
            device.id
        )

        txtSavedLatency.text = "$finalLatency ms"
        txtLatency.text = "$finalLatency ms"
        txtStatus.text = "Calibración guardada"
    }

    // ============================================================
    // ANIMACIÓN TAP
    // ============================================================

    private fun animateTap() {
        btnTap.animate().cancel()

        btnTap.scaleX = 1f
        btnTap.scaleY = 1f

        btnTap.animate()
            .scaleX(0.90f)
            .scaleY(0.90f)
            .setDuration(60L)
            .withEndAction {
                btnTap.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(100L)
                    .start()
            }
            .start()
    }

    // ============================================================
    // CALLBACKS
    // ============================================================

    private fun cancelTrialCallbacks() {
        startVisualRunnable?.let {
            handler.removeCallbacks(it)
        }

        timeoutRunnable?.let {
            handler.removeCallbacks(it)
        }

        startVisualRunnable = null
        timeoutRunnable = null
    }

    private fun cancelCalibrationCallbacks() {
        cancelTrialCallbacks()

        nextTrialRunnable?.let {
            handler.removeCallbacks(it)
        }

        nextTrialRunnable = null
    }

    // ============================================================
    // LIFECYCLE
    // ============================================================

    override fun onPause() {
        cancelCalibrationCallbacks()

        txtCountdown.animate().cancel()
        calibrationView.stop()

        waitingForTap = false
        btnTap.isEnabled = false

        mediaPlayer?.let {
            try {
                if (it.isPlaying) {
                    it.pause()
                }
            } catch (_: Exception) {
            }
        }

        super.onPause()
    }

    override fun onDestroy() {
        cancelCalibrationCallbacks()

        txtCountdown.animate().cancel()
        calibrationView.stop()

        mediaPlayer?.release()
        mediaPlayer = null

        super.onDestroy()
    }
}