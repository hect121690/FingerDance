package com.fingerdance

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private lateinit var video_fondo : VideoView
private lateinit var mediaPlayerBreak : MediaPlayer
private var currentVideoPosition : Int = 0

private lateinit var linearLeft: LinearLayout

class BreakDance : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        getSupportActionBar()?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_break_dance)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        onWindowFocusChanged(true)

        linearLeft = findViewById(R.id.linearLeft)
        linearLeft.setBackgroundColor(Color.BLACK)

        video_fondo = findViewById(R.id.videoBreak)
        video_fondo.setOnPreparedListener { mp ->
            mediaPlayerBreak = mp
            mediaPlayerBreak.isLooping = false
            mediaPlayerBreak.setOnCompletionListener {
                this.finish()
            }
        }
        video_fondo.setVideoPath(getExternalFilesDir("/FingerDance/Themes/$tema/BGAs/StageBreak.mp4").toString())
        video_fondo.start()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        windowInsetsController.let { controller ->
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }

    }
}