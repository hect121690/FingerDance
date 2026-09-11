package com.fingerdance

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class HelpTutorialActivity : AppCompatActivity() {

    private lateinit var imgBackground: ImageView
    private lateinit var imgCharacter: ImageView
    private lateinit var imgNext: ImageView
    private lateinit var darkLayer: View

    private var mediaPlayer: MediaPlayer? = null
    private val handler = Handler(Looper.getMainLooper())

    private var helpType = -1
    private var currentStep = 0
    private var checkpointReached = false
    private var currentPauseIndex = 0

    private lateinit var tutorial: TutorialConfig

    data class TutorialStep(
        val backgroundRes: Int,
        val characterRes: Int = -1,
        val pauseAtMs: ArrayList<Int> = arrayListOf()
    )

    data class TutorialConfig(
        val audioRes: Int,
        val steps: List<TutorialStep>
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        setContentView(R.layout.activity_help_tutorial)

        imgBackground = findViewById(R.id.imgBackground)
        imgCharacter = findViewById(R.id.imgCharacter)
        imgNext = findViewById(R.id.imgNext)
        darkLayer = findViewById(R.id.darkLayer)

        helpType = intent.getIntExtra(
            HelpActivity.EXTRA_HELP_TYPE,
            -1
        )

        if (helpType == -1) {
            finish()
            return
        }

        tutorial = createTutorial(helpType)

        applyDynamicButtonSizes()
        setupListeners()
        startTutorial()
    }

    private fun createTutorial(type: Int): TutorialConfig {
        return when (type) {

            /*
            HelpActivity.HelpType.ONLINE -> {
                TutorialConfig(
                    audioRes = R.raw.dialog_online_mode,
                    steps = listOf(
                        TutorialStep(
                            backgroundRes = R.drawable.help_online_bg_1,
                            characterRes = R.drawable.help_online_overlay_1,
                            pauseAtMs =
                                arrayListOf(

                                )
                        ),
                        TutorialStep(
                            backgroundRes = R.drawable.help_online_bg_2,
                            characterRes = R.drawable.help_online_overlay_2,
                        ),
                        TutorialStep(
                            backgroundRes = R.drawable.help_online_bg_3,
                            characterRes = R.drawable.help_online_overlay_3,
                        ),
                        TutorialStep(
                            backgroundRes = R.drawable.help_online_bg_4,
                            characterRes = R.drawable.help_online_overlay_4,
                        ),
                        TutorialStep(
                            backgroundRes = R.drawable.help_online_bg_5,
                            characterRes = R.drawable.help_online_overlay_5
                        )
                    )
                )
            }
            */
            HelpActivity.HelpType.PREVIEW_BGA -> {
                TutorialConfig(
                    audioRes = R.raw.dialog_show_download_preview_bga,
                    steps = listOf(
                        TutorialStep(
                            backgroundRes = R.drawable.bg_show_download_preview_bga_1,
                            characterRes = R.drawable.chica_show_dowload_preview_bga_1,
                            pauseAtMs = arrayListOf(4250)
                        ),
                        TutorialStep(
                            backgroundRes = R.drawable.bg_show_download_preview_bga_2,
                            characterRes = R.drawable.chica_show_dowload_preview_bga_2
                        )
                    )
                )
            }

            HelpActivity.HelpType.GLOBAL_RANKING -> {
                TutorialConfig(
                    audioRes = R.raw.dialog_show_ranking,
                    steps = listOf(
                        TutorialStep(
                            backgroundRes = R.drawable.bg_show_ranking_1,
                            characterRes = R.drawable.chica_show_ranking_1,
                            pauseAtMs = arrayListOf(7000)
                        ),
                        TutorialStep(
                            backgroundRes = R.drawable.bg_show_ranking_2,
                            characterRes = R.drawable.chica_show_ranking_2
                        )
                    )
                )
            }

            HelpActivity.HelpType.SETTINGS -> {
                TutorialConfig(
                    audioRes = R.raw.dialog_ajustes,
                    steps = listOf(
                        TutorialStep(
                            backgroundRes = R.drawable.bg_show_ajustes,
                            characterRes = R.drawable.chica_show_ajustes
                        )
                    )
                )
            }

            HelpActivity.HelpType.COMMAND_WINDOW -> {
                TutorialConfig(
                    audioRes = R.raw.dialog_command_window,
                    steps = listOf(
                        TutorialStep(
                            backgroundRes = R.drawable.chica_show_command_window
                        )
                    )
                )
            }

            HelpActivity.HelpType.LOAD_ZIP -> {
                TutorialConfig(
                    audioRes = R.raw.dialog_cargar_zip,
                    steps = listOf(
                        TutorialStep(
                            backgroundRes = R.drawable.bg_show_cargar_zip_1,
                            characterRes = R.drawable.chica_cargar_zip_1,
                            pauseAtMs = arrayListOf(6000)
                        ),
                        TutorialStep(
                            backgroundRes = R.drawable.bg_show_cargar_zip_2,
                            characterRes = R.drawable.chica_cargar_zip_2,
                            pauseAtMs = arrayListOf(16000)
                        ),
                        TutorialStep(
                            backgroundRes = R.drawable.bg_show_cargar_zip_3,
                            characterRes = R.drawable.chica_cargar_zip_3,
                            pauseAtMs = arrayListOf(25000)
                        ),
                        TutorialStep(
                            backgroundRes = R.drawable.bg_show_cargar_zip_4,
                            characterRes = R.drawable.chica_cargar_zip_4
                        )
                    )
                )
            }

            HelpActivity.HelpType.CREATE_CHANNELS -> {
                TutorialConfig(
                    audioRes = R.raw.dialog_crear_canal,
                    steps = listOf(
                        TutorialStep(
                            backgroundRes = R.drawable.bg_show_crear_canal_1,
                            characterRes = R.drawable.chica_crear_canal_1,
                            pauseAtMs = arrayListOf(5000)
                        ),
                        TutorialStep(
                            backgroundRes = R.drawable.bg_show_crear_canal_2,
                            characterRes = R.drawable.chica_crear_canal_2,
                            pauseAtMs = arrayListOf(13000)
                        ),
                        TutorialStep(
                            backgroundRes = R.drawable.bg_show_crear_canal_3,
                            characterRes = R.drawable.chica_crear_canal_3,
                            pauseAtMs = arrayListOf(17500)
                        ),
                        TutorialStep(
                            backgroundRes = R.drawable.bg_show_crear_canal_4,
                            characterRes = R.drawable.chica_crear_canal_4,
                            pauseAtMs = arrayListOf(29000)
                        ),
                        TutorialStep(
                            backgroundRes = R.drawable.bg_show_crear_canal_5,
                            characterRes = R.drawable.chica_crear_canal_5,
                            pauseAtMs = arrayListOf(35000)
                        ),
                        TutorialStep(
                            backgroundRes = R.drawable.bg_show_crear_canal_6,
                            characterRes = R.drawable.chica_crear_canal_6,
                            pauseAtMs = arrayListOf(39500)
                        ),
                        TutorialStep(
                            backgroundRes = R.drawable.bg_show_crear_canal_7,
                            characterRes = R.drawable.chica_crear_canal_7,
                            pauseAtMs = arrayListOf(42500)
                        ),
                        TutorialStep(
                            backgroundRes = R.drawable.bg_show_crear_canal_1,
                            characterRes = R.drawable.chica_crear_canal_8
                        )
                    )
                )
            }

            else -> {
                finish()
                throw IllegalArgumentException(
                    "HELP_TYPE no soportado: $type"
                )
            }
        }
    }

    private fun startTutorial() {
        currentStep = 0
        currentPauseIndex = 0
        checkpointReached = false

        showStep(currentStep)

        musicHelp?.setVolume(0.20f, 0.20f)

        mediaPlayer = MediaPlayer.create(this, tutorial.audioRes)
        mediaPlayer?.setOnCompletionListener {
            handler.removeCallbacks(audioWatcher)

            musicHelp?.setVolume(0.5f, 0.5f)

            imgNext.setImageResource(R.drawable.btn_finalizar_help)
            imgNext.visibility = View.VISIBLE
        }

        mediaPlayer?.start()
        startAudioWatcher()
    }

    private val audioWatcher = object : Runnable {
        override fun run() {
            val mp = mediaPlayer ?: return

            if (currentStep < tutorial.steps.size && !checkpointReached) {

                val pauses = tutorial.steps[currentStep].pauseAtMs
                val pauseAt = pauses.getOrNull(currentPauseIndex)

                if (pauseAt != null && mp.isPlaying && mp.currentPosition >= pauseAt) {
                    checkpointReached = true
                    mp.pause()
                    musicHelp?.setVolume(0.5f, 0.5f)
                    imgNext.setImageResource(R.drawable.btn_siguiente_help)
                    imgNext.visibility = View.VISIBLE

                    return
                }
            }

            handler.postDelayed(this, 40)
        }
    }

    private fun startAudioWatcher() {
        handler.removeCallbacks(audioWatcher)
        handler.post(audioWatcher)
    }

    private fun setupListeners() {
        imgNext.setOnClickListener {

            val pauses = tutorial.steps[currentStep].pauseAtMs
            val hasAnotherPause = currentPauseIndex + 1 < pauses.size
            if (hasAnotherPause) {
                currentPauseIndex++
                checkpointReached = false

                imgNext.visibility = View.GONE
                musicHelp?.setVolume(0.20f, 0.20f)

                mediaPlayer?.start()
                startAudioWatcher()

                return@setOnClickListener
            }

            val isLastStep = currentStep >= tutorial.steps.lastIndex

            if (isLastStep) {
                finish()
                return@setOnClickListener
            }

            currentStep++
            currentPauseIndex = 0
            checkpointReached = false

            showStep(currentStep)

            imgNext.visibility = View.GONE
            musicHelp?.setVolume(0.20f, 0.20f)

            mediaPlayer?.start()
            startAudioWatcher()
        }
    }

    private fun showStep(step: Int) {
        val data = tutorial.steps[step]
        imgBackground.setImageResource(data.backgroundRes)
        if(data.characterRes != -1) {
            imgCharacter.setImageResource(data.characterRes)
        }

        when (helpType) {

            HelpActivity.HelpType.ONLINE -> {

            }

            HelpActivity.HelpType.PREVIEW_BGA -> {
                applyCharacterPreviewBgaSize(step)
            }

            HelpActivity.HelpType.GLOBAL_RANKING -> {
                applyCharacterRankingSize()
            }

            HelpActivity.HelpType.SETTINGS -> {
                applyCharacterSettingsSize()
            }

            HelpActivity.HelpType.COMMAND_WINDOW -> {
                // No se necesita cambiar el tamaño del personaje para este tipo de ayuda
            }

            HelpActivity.HelpType.LOAD_ZIP -> {
                applyCharacterLoadZipSize(step)
            }

            HelpActivity.HelpType.CREATE_CHANNELS -> {
                applyCharacterCreateChannelSize(step)
            }

            HelpActivity.HelpType.DOWNLOAD_CONTENT -> {

            }
        }
    }

    private fun applyCharacterPreviewBgaSize(step: Int) {
        val params = imgCharacter.layoutParams as FrameLayout.LayoutParams
        val drawable = imgCharacter.drawable
        val ratio = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight.toFloat()
        when (step) {

            // Mantén presionada la imagen de la canción
            0 -> {
                val targetHeight = (height * 0.45f).toInt()
                params.height = targetHeight
                params.width = (targetHeight * ratio).toInt()
                params.gravity = Gravity.BOTTOM or Gravity.END
                params.leftMargin = 0
                params.rightMargin = 0
                params.topMargin = 0
                params.bottomMargin = 0
            }

            // Menú de contenido multimedia
            1 -> {
                val targetHeight = (height * 0.40f).toInt()
                params.height = targetHeight
                params.width = (targetHeight * ratio).toInt()
                params.gravity = Gravity.BOTTOM or Gravity.END
                params.leftMargin = 0
                params.rightMargin = 0
                params.topMargin = 0
                params.bottomMargin = 0
            }
        }

        imgCharacter.layoutParams = params
    }

    private fun applyCharacterRankingSize() {
        val params = imgCharacter.layoutParams as FrameLayout.LayoutParams
        val targetHeight = (height * 0.45f).toInt()
        val drawable = imgCharacter.drawable
        val ratio = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight.toFloat()
        params.height = targetHeight
        params.width = (targetHeight * ratio).toInt()
        params.gravity = Gravity.BOTTOM or Gravity.START
        params.leftMargin = 0
        params.bottomMargin = 0

        imgCharacter.layoutParams = params
    }

    private fun applyCharacterSettingsSize() {
        val params = imgCharacter.layoutParams as FrameLayout.LayoutParams
        val targetHeight = (height * 0.40f).toInt()
        val drawable = imgCharacter.drawable
        val ratio = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight.toFloat()
        params.height = targetHeight
        params.width = (targetHeight * ratio).toInt()
        params.gravity = Gravity.BOTTOM or Gravity.START
        params.leftMargin = 0
        params.rightMargin = 0
        params.topMargin = 0
        params.bottomMargin = 0
        imgCharacter.layoutParams = params
    }

    private fun applyCharacterLoadZipSize(step: Int) {
        val params = imgCharacter.layoutParams as FrameLayout.LayoutParams
        val drawable = imgCharacter.drawable
        val ratio = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight.toFloat()

        when (step) {
            0 -> {
                val targetHeight = (height * 0.57f).toInt()

                params.height = targetHeight
                params.width = (targetHeight * ratio).toInt()

                params.gravity = Gravity.BOTTOM or Gravity.END

                params.leftMargin = 0
                params.rightMargin = (width * 0.12f).toInt()
                params.topMargin = 0
                params.bottomMargin = 0
            }
            1 -> {
                val targetHeight = (height * 0.52f).toInt()

                params.height = targetHeight
                params.width = (targetHeight * ratio).toInt()

                params.gravity = Gravity.BOTTOM or Gravity.START

                params.leftMargin = -(width * 0.025f).toInt()
                params.rightMargin = 0
                params.topMargin = 0
                params.bottomMargin = 0
            }
            2 -> {
                val targetHeight = (height * 0.54f).toInt()

                params.height = targetHeight
                params.width = (targetHeight * ratio).toInt()

                params.gravity = Gravity.BOTTOM or Gravity.END

                params.leftMargin = 0
                params.rightMargin = -(width * 0.02f).toInt()
                params.topMargin = 0
                params.bottomMargin = 0
            }
            3 -> {
                val targetHeight = (height * 0.50f).toInt()

                params.height = targetHeight
                params.width = (targetHeight * ratio).toInt()

                params.gravity = Gravity.BOTTOM or Gravity.START

                params.leftMargin = -(width * 0.015f).toInt()
                params.rightMargin = 0
                params.topMargin = 0
                params.bottomMargin = 0
            }
        }

        imgCharacter.layoutParams = params
    }

    private fun applyCharacterCreateChannelSize(step: Int) {
        val params = imgCharacter.layoutParams as FrameLayout.LayoutParams
        val drawable = imgCharacter.drawable ?: return
        val ratio = drawable.intrinsicWidth.toFloat() / drawable.intrinsicHeight.toFloat()

        when (step) {

            // Crear canal
            0 -> {
                val targetHeight = (height * 0.43f).toInt()
                params.height = targetHeight
                params.width = (targetHeight * ratio).toInt()

                params.gravity = Gravity.BOTTOM or Gravity.END
                params.leftMargin = 0
                params.rightMargin = (width * 0.45f).toInt()
                params.topMargin = 0
                params.bottomMargin = - (height * 0.045f).toInt()
            }

            // Elegir KSF / SSC
            1 -> {
                val targetHeight = (height * 0.40f).toInt()
                params.height = targetHeight
                params.width = (targetHeight * ratio).toInt()

                params.gravity = Gravity.BOTTOM or Gravity.START
                params.leftMargin = 0
                params.rightMargin = 0
                params.topMargin = 0
                params.bottomMargin = 0
            }

            // Nombre y descripción
            2 -> {
                val targetHeight = (height * 0.45f).toInt()
                params.height = targetHeight
                params.width = (targetHeight * ratio).toInt()

                params.gravity = Gravity.BOTTOM or Gravity.START
                params.leftMargin = 0
                params.rightMargin = 0
                params.topMargin = 0

                // La apoyamos justo arriba del teclado.
                params.bottomMargin = 0
            }

            // Icono del canal
            3 -> {
                val targetHeight = (height * 0.39f).toInt()
                params.height = targetHeight
                params.width = (targetHeight * ratio).toInt()

                params.gravity = Gravity.BOTTOM or Gravity.START
                params.leftMargin = 0
                params.rightMargin = 0
                params.topMargin = 0
                params.bottomMargin = 0
            }

            // Buscar PNG
            4 -> {
                val targetHeight = (height * 0.45f).toInt()
                params.height = targetHeight
                params.width = (targetHeight * ratio).toInt()

                params.gravity = Gravity.BOTTOM or Gravity.START
                params.leftMargin = 0
                params.rightMargin = 0
                params.topMargin = 0
                params.bottomMargin = 0
            }

            // Agregar canciones
            5 -> {
                val targetHeight = (height * 0.37f).toInt()
                params.height = targetHeight
                params.width = (targetHeight * ratio).toInt()

                params.gravity = Gravity.BOTTOM or Gravity.START
                params.leftMargin = 0
                params.rightMargin = 0
                params.topMargin = 0
                params.bottomMargin = 0
            }

            // Usar esta carpeta
            6 -> {
                val targetHeight = (height * 0.42f).toInt()
                params.height = targetHeight
                params.width = (targetHeight * ratio).toInt()

                params.gravity = Gravity.BOTTOM or Gravity.START
                params.leftMargin = 0
                params.rightMargin = 0
                params.topMargin = 0
                params.bottomMargin = (height * 0.07f).toInt()
            }
            //Finalizar
            7 -> {
                val targetHeight = (height * 0.42f).toInt()
                params.height = targetHeight
                params.width = (targetHeight * ratio).toInt()

                params.gravity = Gravity.BOTTOM or Gravity.START
                params.leftMargin = 0
                params.rightMargin = 0
                params.topMargin = 0
                params.bottomMargin = 0
            }

        }

        imgCharacter.layoutParams = params
    }

    private fun applyDynamicButtonSizes() {
        val nextWidth = (width * 0.40f).toInt()
        val nextParams = imgNext.layoutParams as FrameLayout.LayoutParams
        nextParams.width = nextWidth
        nextParams.height = (nextWidth * 0.30f).toInt()
        nextParams.gravity = Gravity.BOTTOM or Gravity.END
        nextParams.rightMargin = (width * 0.025f).toInt()
        nextParams.bottomMargin = (height * 0.025f).toInt()
        imgNext.layoutParams = nextParams
    }

    override fun onDestroy() {
        handler.removeCallbacks(audioWatcher)

        mediaPlayer?.release()
        mediaPlayer = null

        musicHelp?.setVolume(0.5f, 0.5f)

        super.onDestroy()
    }
}