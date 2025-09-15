package com.fingerdance

import android.content.Intent
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import android.view.WindowManager
import android.widget.RelativeLayout
import android.widget.Toast
import android.widget.VideoView
import androidx.core.view.isVisible
import com.badlogic.gdx.Game
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.fingerdance.Player.Companion.NOTE_LNOTE
import com.fingerdance.Player.Companion.NOTE_NONE
import java.io.File

private lateinit var gdxContainer : RelativeLayout
private var currentVideoPositionScreen : Int = 0

lateinit var videoViewBgaoff : VideoView
lateinit var videoViewBgaOn : VideoView


private val timeToPlay = 1000L
var countMiss = 0
open class GameScreenActivity : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_screen)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        gdxContainer = findViewById(R.id.gdxContainer)
        gdxContainer.layoutParams = RelativeLayout.LayoutParams(width, height)

        readyPlay = false

        if(mediPlayer.isPlaying){
            mediPlayer.stop()
        }

        resultSong = ResultSong()
        addVideoBackground()
        thisHandler.postDelayed({
            countAdd ++
            themes.edit().putInt("countAdd", countAdd).apply()
        }, 20000)

        val config = AndroidApplicationConfiguration()
        config.a = 8
        val gdxView = initializeForView(MyGameScreen(this), config)
        if(gdxView is SurfaceView){
            (gdxView).setZOrderOnTop(true)
            (gdxView).holder.setFormat(PixelFormat.TRANSLUCENT)
        }
        gdxContainer.addView(gdxView)
        thisHandler.postDelayed({
            if (isVideo) {
                videoViewBgaOn.start()
            } else {
                videoViewBgaoff.start()
                videoViewBgaoff.setOnCompletionListener {
                    videoViewBgaoff.start()
                }
            }
            mediaPlayer.start()
        }, timeToPlay)

    }

    private fun isFileExists(file: File): Boolean {
        return file.exists() && !file.isDirectory
    }

    private fun addVideoBackground() {
        videoViewBgaoff = findViewById(R.id.videoViewBgaoff)
        videoViewBgaoff.isVisible = false

        videoViewBgaOn = findViewById(R.id.videoViewBgaOn)
        videoViewBgaOn.y = medidaFlechas * 2
        val newWidth = (width * 1.2).toInt()
        val newHeight = (newWidth * 9 / 16).toInt()
        videoViewBgaOn.layoutParams = videoViewBgaOn.layoutParams.apply {
            width = newWidth
            height = newHeight
        }

        if(isFileExists(File(playerSong.rutaVideo!!))){
            if(playerSong.isBGAOff == false){
                videoViewBgaoff.isVisible = false
                videoViewBgaOn.isVisible = true
                videoViewBgaOn.setVideoPath(playerSong.rutaVideo)
                videoViewBgaOn.setOnPreparedListener { mp ->
                    mp.setVolume(0f, 0f)
                    /*
                    for (i in mp.trackInfo.indices) {
                        if (mp.trackInfo[i].trackType == MediaPlayer.TrackInfo.MEDIA_TRACK_TYPE_AUDIO) {
                            mp.deselectTrack(i)
                        }
                    }
                    */
                }
                isVideo = true
            }else{
                videoViewBgaoff.isVisible = true
                videoViewBgaOn.isVisible = false
                videoViewBgaoff.setVideoPath(bgaOff)
                isVideo = false
            }
        }else{
            videoViewBgaoff.isVisible = true
            videoViewBgaOn.isVisible = false
            videoViewBgaoff.setVideoPath(bgaOff)
            isVideo = false
        }

        mediaPlayer.isLooping = false
        mediaPlayer.setOnPreparedListener {
            isMediaPlayerPrepared = true
        }

        mediaPlayer.setOnCompletionListener {
            resultSong.banner = playerSong.rutaBanner!!
            ksf.patterns.forEach{ ptn ->
                ptn.vLine.forEach { line ->
                    line.step.forEach { step ->
                        if(step != NOTE_NONE && step != NOTE_LNOTE){
                            countMiss ++
                        }
                    }
                }
            }

            thisHandler.postDelayed({
                val intent = Intent(this, DanceGrade()::class.java)
                startActivity(intent)
                this.finish()
            }, 2500)
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        gdxContainer.removeAllViews()
        currentVideoPositionScreen = 0
        currentVideoPositionScreen = 0
        if (isVideo) {
            videoViewBgaOn.seekTo(0)
        } else {
            videoViewBgaoff.seekTo(0)
        }

        thisHandler.removeCallbacksAndMessages(null)
        mediaPlayer.setOnCompletionListener(null)
        if(mediaPlayer.isPlaying){
            mediaPlayer.stop()
        }
    }

    fun breakDance(){
        this.finish()
        val intent = Intent(this, BreakDance::class.java)
        startActivity(intent)
    }

    private var backPressedTime: Long = 0
    private lateinit var backToast: Toast
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!isOnline) {
            if (backPressedTime + 2000 > System.currentTimeMillis()) {
                backToast.cancel()
                isVideo = false
                this.finish()
                return
            } else {
                backToast = Toast.makeText(this, "Presiona nuevamente para salir", Toast.LENGTH_SHORT)
                backToast.show()
            }
            backPressedTime = System.currentTimeMillis()
        }
    }

    override fun onPause() {
        super.onPause()
        currentVideoPositionScreen = mediaPlayer.currentPosition
        mediaPlayer.pause()
        if (isVideo) {
            videoViewBgaOn.pause()
        } else {
            videoViewBgaoff.pause()
        }
    }

    private var hasWaitedForDelay = false
    override fun onResume() {
        super.onResume()

        if (!hasWaitedForDelay) {
            Handler(Looper.getMainLooper()).postDelayed({
                if (!mediaPlayer.isPlaying) {
                    mediaPlayer.start()
                }
                startVideoFromPosition()
                hasWaitedForDelay = true
            }, 2000)
        } else {
            if (!mediaPlayer.isPlaying) {
                mediaPlayer.start()
            }
            startVideoFromPosition()
        }
    }

    private fun startVideoFromPosition() {
        if (isVideo) {
            videoViewBgaOn.seekTo(currentVideoPositionScreen)
            if (!videoViewBgaOn.isPlaying) {
                videoViewBgaOn.start()
            }
        } else {
            videoViewBgaoff.seekTo(currentVideoPositionScreen)
            if (!videoViewBgaoff.isPlaying) {
                videoViewBgaoff.start()
            }
        }
    }
}

class MyGameScreen(gameScreenActivity: GameScreenActivity) : Game() {
    val gsa = gameScreenActivity
    private var gameScreen: GameScreenKsf? = null
    override fun create() {
        gameScreen = GameScreenKsf(gsa)
        setScreen(gameScreen)
    }

    override fun dispose() {
        super.dispose()
        gameScreen?.dispose()
    }
}


