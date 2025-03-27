package com.fingerdance

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.VideoView
import androidx.core.view.isVisible
import com.badlogic.gdx.Game
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import java.io.File

var isVideo = false

private var currentVideoPositionScreen : Int = 0
var isMediaPlayerPrepared = false

private lateinit var gdxContainer : RelativeLayout

lateinit var videoViewBgaoff : VideoView
lateinit var videoViewBgaOn : VideoView

var curCombo = 0
var combo = 0
var combo_miss = 0

private lateinit var img_JudgeLetter : ImageView
private lateinit var img_combo : ImageView
private lateinit var img_count_combo : ImageView
private lateinit var imgJudgeCompleto : LinearLayout

private var fadeOutHandler: Handler? = null
private var fadeOutRunnable: Runnable? = null

lateinit var lifeBar: LifeBar

private val widthJudges = width / 2
private val heightJudges = widthJudges / 6

lateinit var resultSong: ResultSong

val thisHandler = Handler(Looper.getMainLooper())

private val interpolator = LinearInterpolator()
private val animatorSet = AnimatorSet()
private lateinit var expandX : ObjectAnimator
private lateinit var expandY : ObjectAnimator
private var fadeSet = AnimatorSet()
private lateinit var fadeOut : ObjectAnimator
private lateinit var flattenY : ObjectAnimator

private val timeToPlay = 1000L

open class GameScreenActivity : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_screen)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        gdxContainer = findViewById(R.id.gdxContainer)
        gdxContainer.layoutParams = RelativeLayout.LayoutParams(width, height)

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
        initJugde()

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

    val uiHandler = Handler(Looper.getMainLooper()) { msg ->
        when (msg.what) {
            0 -> getPerfect()
            1 -> getGreat()
            2 -> getGood()
            3 -> getBad()
            4 -> getMiss()
        }
        true
    }

    fun getPerfect(){
        combo_miss = 0
        combo++
        img_JudgeLetter.setImageBitmap(bitPerfect)
        showJudge()
    }
    fun getGreat(){
        combo_miss = 0
        combo++
        img_JudgeLetter.setImageBitmap(bitGreat)
        showJudge()
    }
    fun getGood(){
        combo_miss = 0
        img_JudgeLetter.setImageBitmap(bitGood)
        showJudge()
    }
    fun getBad(){
        combo_miss = 0
        combo = 0
        img_JudgeLetter.setImageBitmap(bitBad)
        showJudge()
    }
    fun getMiss(){
        combo = 0
        combo_miss++
        img_JudgeLetter.setImageBitmap(bitMiss)
        showJudge()
    }

    val bitmapVacio = Bitmap.createBitmap(widthJudges, heightJudges, Bitmap.Config.ARGB_8888)
    private fun showJudge() {
        if(combo <= 3 ){
            img_combo.setImageBitmap(bitmapVacio)
            img_count_combo.setImageBitmap(bitmapVacio)
        }
        if(combo_miss < 3){
            img_combo.setImageBitmap(bitmapVacio)
            img_count_combo.setImageBitmap(bitmapVacio)
        }
        if(combo >= 4){
            img_combo.setImageBitmap(bitmapCombo)
            updateCombo(combo)
        }
        if(combo_miss >= 4){
            img_combo.setImageBitmap(bitmapComboMiss)
            updateCombo(combo_miss, true)
        }
        showExpandJudge(imgJudgeCompleto)

    }

    private fun updateCombo(numero: Int, isMiss: Boolean = false) {
        val numeroStr = if (numero < 100) String.format("%03d", numero) else numero.toString()
        val bitmaps = Array(numeroStr.length) { index ->
            val digito = numeroStr[index].toString().toInt()
            dividirPNG(digito, isMiss)
        }
        img_count_combo.setImageBitmap(combinarBitmaps(bitmaps))
    }

    private fun dividirPNG(digito: Int, isMiss: Boolean): Bitmap {
        return if (isMiss) numberBitmapsMiss[digito] else numberBitmaps[digito]
    }

    private fun combinarBitmaps(bitmaps: Array<Bitmap>): Bitmap {
        val anchoTotal = bitmaps.sumOf { it.width }
        val altura = bitmaps[0].height

        val bitmapCombinado = Bitmap.createBitmap(anchoTotal, altura, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmapCombinado)

        var posicionX = 0f
        for (bitmap in bitmaps) {
            canvas.drawBitmap(bitmap, posicionX, 0f, null)
            posicionX += bitmap.width
        }

        return bitmapCombinado
    }

    private fun showExpandJudge(linearLayout: LinearLayout) {
        linearLayout.visibility = View.VISIBLE
        linearLayout.alpha = 1f

        fadeOutHandler?.removeCallbacks(fadeOutRunnable!!)
        linearLayout.clearAnimation()

        expandX.duration = 150
        expandY.duration = 150

        expandX.interpolator = interpolator
        expandY.interpolator = interpolator

        animatorSet.play(expandX).with(expandY)

        animatorSet.start()

        fadeOutHandler = Handler(Looper.getMainLooper())
        fadeOutRunnable = Runnable {
            fadeOut.duration = 500
            flattenY.duration = 500
            fadeSet.playTogether(fadeOut, flattenY)
            fadeSet.start()

            fadeSet.addListener(object : Animator.AnimatorListener {
                override fun onAnimationEnd(animation: Animator) {
                    linearLayout.visibility = View.INVISIBLE
                }
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
        }
        fadeOutHandler?.postDelayed(fadeOutRunnable!!, 1000)
    }

    private fun initJugde() {
        imgJudgeCompleto = findViewById(R.id.imgJudgeCombo)
        imgJudgeCompleto.layoutParams = RelativeLayout.LayoutParams(widthJudges, heightJudges * 3)

        img_JudgeLetter = ImageView(this)
        img_combo = ImageView(this)
        img_count_combo = ImageView(this)

        img_JudgeLetter.layoutParams = RelativeLayout.LayoutParams(widthJudges, heightJudges)
        img_combo.layoutParams = RelativeLayout.LayoutParams(widthJudges, (heightJudges * 0.7).toInt())
        img_count_combo.layoutParams = RelativeLayout.LayoutParams(widthJudges, (heightJudges))

        imgJudgeCompleto.x = widthJudges - (widthJudges / 2f)
        imgJudgeCompleto.y = height / 2f - heightJudges * 6

        imgJudgeCompleto.addView(img_JudgeLetter)
        imgJudgeCompleto.addView(img_combo)
        imgJudgeCompleto.addView(img_count_combo)

        imgJudgeCompleto.visibility = View.INVISIBLE

        expandX = ObjectAnimator.ofFloat(imgJudgeCompleto, "scaleX", 1.4f, 1.0f)
        expandY = ObjectAnimator.ofFloat(imgJudgeCompleto, "scaleY", 1.4f, 1.0f)

        fadeOut = ObjectAnimator.ofFloat(imgJudgeCompleto, "scaleX", 1f, 1.5f)
        flattenY = ObjectAnimator.ofFloat(imgJudgeCompleto, "scaleY", 1f, 0f)

    }

    private fun isFileExists(file: File): Boolean {
        return file.exists() && !file.isDirectory
    }

    private fun addVideoBackground() {
        videoViewBgaoff = findViewById(R.id.videoViewBgaoff)
        videoViewBgaoff.isVisible = false

        videoViewBgaOn = findViewById(R.id.videoViewBgaOn)
        videoViewBgaOn.y = medidaFlechas
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
                videoViewBgaOn.setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.setVolume(0f, 0f)
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
        mediaPlayer.setOnPreparedListener { mp ->
            isMediaPlayerPrepared = true
        }
        mediaPlayer.setOnCompletionListener {
            resultSong.banner = playerSong.rutaBanner!!
            if(isOnline) {
                activeSala.jugador1.listo = false
                activeSala.jugador2.listo = false
                salaRef.setValue(activeSala)
                /*
                val currentTurn = activeSala.turno
                if(currentTurn != userName){
                    activeSala.turno = userName
                    salaRef.child("turno").setValue(activeSala.turno)
                }
                */
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
        curCombo = 0
        combo = 0
        combo_miss = 0

        uiHandler.removeCallbacksAndMessages(null)
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if(!isOnline) {
            combo = 0
            combo_miss = 0
            curCombo = 0
            isVideo = false
            this.finish()
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
            }, 2000) // 2 segundos
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
        lifeBar = LifeBar(height.toFloat(), gsa)
        gameScreen = GameScreenKsf(gsa)
        setScreen(gameScreen)
    }

    override fun dispose() {
        super.dispose()
        gameScreen?.dispose()
    }
}


