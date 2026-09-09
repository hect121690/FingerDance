package com.fingerdance

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

var musicHelp: MediaPlayer? = null

class HelpActivity : AppCompatActivity() {

    private lateinit var welcomeOverlay: View
    private lateinit var imgWelcomeGirl: ImageView

    private var dialogHelp: MediaPlayer? = null

    object HelpType {
        const val ONLINE = 1
        const val PREVIEW_BGA = 2
        const val GLOBAL_RANKING = 3
        const val SETTINGS = 4
        const val COMMAND_WINDOW = 5
        const val LOAD_ZIP = 6
        const val CREATE_CHANNELS = 7
        const val DOWNLOAD_CONTENT = 8
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        setContentView(R.layout.activity_help)

        welcomeOverlay = findViewById(R.id.welcomeOverlay)
        imgWelcomeGirl = findViewById(R.id.imgWelcomeGirl)

        setupWelcome()
        setupMusic()
        setupWelcomeDialog()

        findViewById<View>(R.id.btnAcceptHelp).setOnClickListener {
            closeWelcome()
        }

        findViewById<View>(R.id.cardOnline).setOnClickListener {
            openHelp(HelpType.ONLINE)
        }

        findViewById<View>(R.id.cardPreviewBga).setOnClickListener {
            openHelp(HelpType.PREVIEW_BGA)
        }

        findViewById<View>(R.id.cardRanking).setOnClickListener {
            openHelp(HelpType.GLOBAL_RANKING)
        }

        findViewById<View>(R.id.cardSettings).setOnClickListener {
            openHelp(HelpType.SETTINGS)
        }

        findViewById<View>(R.id.cardCommandWindow).setOnClickListener {
            openHelp(HelpType.COMMAND_WINDOW)
        }

        findViewById<View>(R.id.cardLoadZip).setOnClickListener {
            openHelp(HelpType.LOAD_ZIP)
        }

        findViewById<View>(R.id.cardCreateChannels).setOnClickListener {
            openHelp(HelpType.CREATE_CHANNELS)
        }

        findViewById<View>(R.id.cardDownloadContent).setOnClickListener {
            openHelp(HelpType.DOWNLOAD_CONTENT)
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun setupMusic() {
        musicHelp = MediaPlayer.create(this, R.raw.music_help)

        musicHelp?.apply {
            isLooping = true
            setVolume(0.5f, 0.5f)
            start()
        }
    }

    private fun setupWelcomeDialog() {
        dialogHelp = MediaPlayer.create(this, R.raw.dialog_welcome_help)

        dialogHelp?.apply {
            setVolume(1.0f, 1.0f)

            setOnCompletionListener { player ->
                player.release()
                dialogHelp = null
                musicHelp?.setVolume(0.5f, 0.5f)
            }

            start()
        }
    }

    private fun closeWelcome() {
        welcomeOverlay.visibility = View.GONE

        dialogHelp?.let { player ->
            if (player.isPlaying) {
                player.stop()
            }

            player.release()
        }

        dialogHelp = null
        musicHelp?.setVolume(0.5f, 0.5f)

    }

    private fun setupWelcome() {
        val targetHeight = (height * 0.65f).toInt()

        val drawable = imgWelcomeGirl.drawable
        val ratio =
            drawable.intrinsicWidth.toFloat() /
                    drawable.intrinsicHeight.toFloat()

        val params = imgWelcomeGirl.layoutParams

        params.height = targetHeight
        params.width = (targetHeight * ratio).toInt()

        imgWelcomeGirl.layoutParams = params
    }

    private fun openHelp(type: Int) {
        val intent = Intent(this, HelpTutorialActivity::class.java)
        intent.putExtra(EXTRA_HELP_TYPE, type)
        startActivity(intent)
    }

    override fun onDestroy() {

        dialogHelp?.let { player ->
            try {
                if (player.isPlaying) {
                    player.stop()
                }
            } catch (_: Exception) {
            }

            player.release()
        }

        dialogHelp = null

        if (musicHelp != null) {

            try {
                if (musicHelp!!.isPlaying) {
                    musicHelp!!.stop()
                }
            } catch (_: Exception) {
            }

            musicHelp!!.release()
        }

        super.onDestroy()
    }

    companion object {
        const val EXTRA_HELP_TYPE = "HELP_TYPE"
    }
}