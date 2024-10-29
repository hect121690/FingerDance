package com.fingerdance

import android.content.pm.ActivityInfo
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity

private lateinit var video_fondo : VideoView
private lateinit var mediPlayer : MediaPlayer
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
            mediPlayer = mp
            mediPlayer.isLooping = false
            mediPlayer.setOnCompletionListener {
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
        val decorView: View = window.decorView
        decorView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }
}