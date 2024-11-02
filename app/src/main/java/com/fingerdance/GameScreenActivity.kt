package com.fingerdance

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PixelFormat
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
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
//var noteTime = 0f

lateinit var mediaPlayer : MediaPlayer
private var currentVideoPositionScreen : Int = 0
var isMediaPlayerPrepared = false

//private lateinit var customBackgroundView: CustomBackgroundView
//private lateinit var customButtons: RelativeLayout
/*
lateinit var leftDEscale : ImageView
lateinit var leftUEscale : ImageView
lateinit var centEscale : ImageView
lateinit var rightUEscale : ImageView
lateinit var rightDEscale : ImageView
val valueExpand = 1.3f

var alphaAnimLD : ObjectAnimator? = null
var leftDEscaleAnimX : ObjectAnimator? = null
var leftDEscaleAnimY : ObjectAnimator? = null

var alphaAnimLU : ObjectAnimator? = null
var leftUEscaleAnimX : ObjectAnimator? = null
var leftUEscaleAnimY : ObjectAnimator? = null

var alphaAnimCE : ObjectAnimator? = null
var centEscaleAnimX : ObjectAnimator? = null
var centEscaleAnimY : ObjectAnimator? = null

var alphaAnimRU : ObjectAnimator? = null
var rightUEscaleAnimX : ObjectAnimator? = null
var rightUEscaleAnimY : ObjectAnimator? = null

var alphaAnimRD : ObjectAnimator? = null
var rightDEscaleAnimX : ObjectAnimator? = null
var rightDEscaleAnimY : ObjectAnimator? = null
*/

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

//lateinit var gs: GameScreenKsf

private var fadeOutHandler: Handler? = null
private var fadeOutRunnable: Runnable? = null

lateinit var lifeBar: LifeBar

private val widthJudges = width / 2
private val heightJudges = widthJudges / 6

private val animatorSetLD = AnimatorSet()
private val animatorSetLU = AnimatorSet()
private val animatorSetCE = AnimatorSet()
private val animatorSetRU = AnimatorSet()
private val animatorSetRD = AnimatorSet()

lateinit var resultSong: ResultSong
open class GameScreenActivity : AndroidApplication() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_gdx)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        gdxContainer = findViewById(R.id.gdxContainer)
        gdxContainer.layoutParams = RelativeLayout.LayoutParams(width, height)

        resultSong = ResultSong()
        addVideoBackground()
        //getExpandRecepts()

        val config = AndroidApplicationConfiguration()
        config.a = 8
        val gdxView = initializeForView(MyGameScreen(this), config)
        if(gdxView is SurfaceView){
            (gdxView).setZOrderOnTop(true)
            (gdxView).holder.setFormat(PixelFormat.TRANSLUCENT)
        }
        gdxContainer.addView(gdxView)

        initJugde()

        //getAnimatorsShirnk()
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

    private fun showJudge() {
        if(combo <= 3 ){
            img_combo.setImageBitmap(Bitmap.createBitmap(widthJudges, heightJudges, Bitmap.Config.ARGB_8888))
            img_count_combo.setImageBitmap(Bitmap.createBitmap(widthJudges, heightJudges, Bitmap.Config.ARGB_8888))
        }
        if(combo_miss < 3){
            img_combo.setImageBitmap(Bitmap.createBitmap(widthJudges, heightJudges, Bitmap.Config.ARGB_8888))
            img_count_combo.setImageBitmap(Bitmap.createBitmap(widthJudges, heightJudges, Bitmap.Config.ARGB_8888))
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
        imgJudgeCompleto.bringToFront()
    }

    private fun initJugde() {
        imgJudgeCompleto = findViewById(R.id.imgJudgeCombo)
        imgJudgeCompleto.layoutParams = RelativeLayout.LayoutParams(widthJudges, heightJudges * 3)

        img_JudgeLetter = ImageView(this)
        img_JudgeLetter.layoutParams = RelativeLayout.LayoutParams(widthJudges, heightJudges)
        img_combo = ImageView(this)
        img_combo.layoutParams = RelativeLayout.LayoutParams(widthJudges, heightJudges)

        img_count_combo = ImageView(this)

        imgJudgeCompleto.x = widthJudges - (widthJudges / 2f)
        imgJudgeCompleto.y = height / 2f - heightJudges * 3

        imgJudgeCompleto.addView(img_JudgeLetter)
        imgJudgeCompleto.addView(img_combo)
        imgJudgeCompleto.addView(img_count_combo)
        imgJudgeCompleto.visibility = View.INVISIBLE

        imgJudgeCompleto.bringToFront()
    }
/*
    private fun getExpandRecepts() {
        leftDEscale = ImageView(this)
        leftUEscale = ImageView(this)
        centEscale = ImageView(this)
        rightUEscale = ImageView(this)
        rightDEscale = ImageView(this)

        leftDEscale.setImageBitmap(leftDExpand)
        leftUEscale.setImageBitmap(leftUExpand)
        centEscale.setImageBitmap(centExpand)
        rightUEscale.setImageBitmap(rightUExpand)
        rightDEscale.setImageBitmap(rightDExpand)

        alphaAnimLD = ObjectAnimator.ofFloat(leftDEscale, "alpha", 0f)
        leftDEscaleAnimX = ObjectAnimator.ofFloat(leftDEscale, "scaleX", valueExpand)
        leftDEscaleAnimY = ObjectAnimator.ofFloat(leftDEscale, "scaleY", valueExpand)

        alphaAnimLU = ObjectAnimator.ofFloat(leftUEscale, "alpha", 0f)
        leftUEscaleAnimX = ObjectAnimator.ofFloat(leftUEscale, "scaleX", valueExpand)
        leftUEscaleAnimY = ObjectAnimator.ofFloat(leftUEscale, "scaleY", valueExpand)

        alphaAnimCE = ObjectAnimator.ofFloat(centEscale, "alpha", 0f)
        centEscaleAnimX = ObjectAnimator.ofFloat(centEscale, "scaleX", valueExpand)
        centEscaleAnimY = ObjectAnimator.ofFloat(centEscale, "scaleY", valueExpand)

        alphaAnimRU = ObjectAnimator.ofFloat(rightUEscale, "alpha", 0f)
        rightUEscaleAnimX = ObjectAnimator.ofFloat(rightUEscale, "scaleX", valueExpand)
        rightUEscaleAnimY = ObjectAnimator.ofFloat(rightUEscale, "scaleY", valueExpand)

        alphaAnimRD = ObjectAnimator.ofFloat(rightDEscale, "alpha", 0f)
        rightDEscaleAnimX = ObjectAnimator.ofFloat(rightDEscale, "scaleX", valueExpand)
        rightDEscaleAnimY = ObjectAnimator.ofFloat(rightDEscale, "scaleY", valueExpand)

        gdxContainer.addView(leftDEscale)
        gdxContainer.addView(leftUEscale)
        gdxContainer.addView(centEscale)
        gdxContainer.addView(rightUEscale)
        gdxContainer.addView(rightDEscale)

        leftDEscale.visibility = View.GONE
        leftUEscale.visibility = View.GONE
        centEscale.visibility = View.GONE
        rightUEscale.visibility = View.GONE
        rightDEscale.visibility = View.GONE

        leftDEscale.layoutParams = RelativeLayout.LayoutParams(medidaFlechas.toInt(), medidaFlechas.toInt())
        leftUEscale.layoutParams = RelativeLayout.LayoutParams(medidaFlechas.toInt(), medidaFlechas.toInt())
        centEscale.layoutParams = RelativeLayout.LayoutParams(medidaFlechas.toInt(), medidaFlechas.toInt())
        rightUEscale.layoutParams = RelativeLayout.LayoutParams(medidaFlechas.toInt(), medidaFlechas.toInt())
        rightDEscale.layoutParams = RelativeLayout.LayoutParams(medidaFlechas.toInt(), medidaFlechas.toInt())

        val xRecept = medidaFlechas
        val yRecept = medidaFlechas

        leftDEscale.x = xRecept
        leftDEscale.y = yRecept
        leftUEscale.x = xRecept * 2
        leftUEscale.y = yRecept
        centEscale.x = xRecept * 3
        centEscale.y = yRecept
        rightUEscale.x = xRecept * 4
        rightUEscale.y = yRecept
        rightDEscale.x = xRecept * 5
        rightDEscale.y = yRecept

        leftDEscale.bringToFront()
        leftUEscale.bringToFront()
        centEscale.bringToFront()
        rightUEscale.bringToFront()
        rightDEscale.bringToFront()
    }

    private fun getAnimatorsShirnk() {
        val duration = 500L
        animatorSetLD.duration = duration
        animatorSetLD.playTogether(leftDEscaleAnimX, leftDEscaleAnimY, alphaAnimLD)
        animatorSetLD.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                //leftDEscale.visibility = View.GONE
            }
        })
        animatorSetLU.duration = duration
        animatorSetLU.playTogether(leftUEscaleAnimX, leftUEscaleAnimY, alphaAnimLU)
        animatorSetLU.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                //leftUEscale.visibility = View.GONE
            }
        })
        animatorSetCE.duration = duration
        animatorSetCE.playTogether(centEscaleAnimX, centEscaleAnimY, alphaAnimCE)
        animatorSetCE.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                //centEscale.visibility = View.GONE
            }
        })
        animatorSetRU.duration = duration
        animatorSetRU.playTogether(rightUEscaleAnimX, rightUEscaleAnimY, alphaAnimRU)
        animatorSetRU.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                //rightUEscale.visibility = View.GONE
            }
        })
        animatorSetRD.duration = duration
        animatorSetRD.playTogether(rightDEscaleAnimX, rightDEscaleAnimY, alphaAnimRD)
        animatorSetRD.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                //rightDEscale.visibility = View.GONE
            }
        })
    }

    fun startShrinkAnimation(position: Int) {
        when (position) {
            1 -> {
                leftDEscale.visibility = View.VISIBLE
                if (animatorSetLD.isRunning) {
                    animatorSetLD.cancel()
                }
                animatorSetLD.start()
            }
            2 -> {
                leftUEscale.visibility = View.VISIBLE
                if (animatorSetLU.isRunning) {
                    animatorSetLU.cancel()
                }
                animatorSetLU.start()
            }
            3 -> {
                centEscale.visibility = View.VISIBLE
                if (animatorSetCE.isRunning) {
                    animatorSetCE.cancel()
                }
                animatorSetCE.start()
            }
            4 -> {
                rightUEscale.visibility = View.VISIBLE
                if (animatorSetRU.isRunning) {
                    animatorSetRU.cancel()
                }
                animatorSetRU.start()
            }
            5 -> {
                rightDEscale.visibility = View.VISIBLE
                if (animatorSetRD.isRunning) {
                    animatorSetRD.cancel()
                }
                animatorSetRD.start()
            }
        }

    }
*/
    private fun isFileExists(file: File): Boolean {
        return file.exists() && !file.isDirectory
    }

    private fun updateCombo(numero: Int, isMiss: Boolean = false) {
        val numeroStr = if (numero < 100) String.format("%03d", numero) else numero.toString()
        val digitos = numeroStr.map { it.toString().toInt() }
        val bitmaps = mutableListOf<Bitmap>()
        for (digito in digitos) {
            bitmaps.add(dividirPNG(digito, isMiss))
        }
        val bitmapNumeroCompleto = combinarBitmaps(bitmaps)
        img_count_combo.setImageBitmap(bitmapNumeroCompleto)
    }

    private fun dividirPNG(digito: Int, isMiss: Boolean): Bitmap {
        return if (isMiss) numberBitmapsMiss[digito] else numberBitmaps[digito]
    }

    private fun combinarBitmaps(bitmaps: List<Bitmap>): Bitmap {
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

        val expandX = ObjectAnimator.ofFloat(linearLayout, "scaleX", 1f, 1.3f)
        val contractX = ObjectAnimator.ofFloat(linearLayout, "scaleX", 1.3f, 1f)

        val expandY = ObjectAnimator.ofFloat(linearLayout, "scaleY", 1f, 1.3f)
        val contractY = ObjectAnimator.ofFloat(linearLayout, "scaleY", 1.3f, 1f)

        expandX.duration = 150
        contractX.duration = 150
        expandY.duration = 150
        contractY.duration = 150

        val interpolator = LinearInterpolator()

        expandX.interpolator = interpolator
        contractX.interpolator = interpolator
        expandY.interpolator = interpolator
        contractY.interpolator = interpolator

        val animatorSet = AnimatorSet()
        animatorSet.play(expandX).with(expandY)
        animatorSet.play(contractX).with(contractY).after(expandX)

        animatorSet.start()

        fadeOutHandler = Handler(Looper.getMainLooper())
        fadeOutRunnable = Runnable {
            val fadeOut = ObjectAnimator.ofFloat(linearLayout, "alpha", 1f, 0f)
            val flattenY = ObjectAnimator.ofFloat(linearLayout, "scaleY", 1f, 0f)

            fadeOut.duration = 1000
            flattenY.duration = 1000

            fadeOut.interpolator = AccelerateInterpolator()  // Hacer que el desvanecimiento sea suave
            flattenY.interpolator = AccelerateInterpolator()

            val fadeSet = AnimatorSet()
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

    private fun addVideoBackground() {
        videoViewBgaoff = findViewById(R.id.videoViewBgaoff)
        videoViewBgaoff.isVisible = false

        videoViewBgaOn = findViewById(R.id.videoViewBgaOn)
        videoViewBgaOn.y = medidaFlechas * 2.5f
        mediaPlayer = MediaPlayer.create(this, Uri.fromFile(File(playerSong.rutaCancion!!)))

        if(isFileExists(File(playerSong.rutaVideo!!))){
            if(playerSong.isBGAOff == false){
                videoViewBgaoff.isVisible = false
                videoViewBgaOn.isVisible = true
                videoViewBgaOn.setVideoPath(playerSong.rutaVideo)
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
            val intent = Intent(this, DanceGrade()::class.java)
            startActivity(intent)
            this.finish()

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        currentVideoPositionScreen = 0
        if (isVideo) {
            videoViewBgaOn.seekTo(0)
        } else {
            videoViewBgaoff.seekTo(0)
        }
    }

    fun breakDance(){
        this.finish()
        val intent = Intent(this, BreakDance::class.java)
        startActivity(intent)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        combo = 0
        combo_miss = 0
        curCombo = 0
        isVideo = false
        //gs.dispose()
        this.finish()
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

    override fun onResume() {
        super.onResume()
        mediaPlayer.start()
        if (isVideo) {
            videoViewBgaOn.seekTo(currentVideoPositionScreen)
            videoViewBgaOn.start()
        } else {
            videoViewBgaoff.seekTo(currentVideoPositionScreen)
            videoViewBgaoff.start()
        }
    }
}

class MyGameScreen(gameScreenActivity: GameScreenActivity) : Game() {
    val gsa = gameScreenActivity
    override fun create() {
        lifeBar = LifeBar(height.toFloat(), gsa)
        //Gdx.app.run {  }
        setScreen(GameScreenKsf(gsa))
    }
}


