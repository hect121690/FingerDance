package com.fingerdance

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
import kotlinx.coroutines.*
import java.io.File

private lateinit var bgBanner: ImageView
private lateinit var bgConstraint: ConstraintLayout
private lateinit var mediaPlayerEvaluation: MediaPlayer

var totalScore = 0.0

class DanceGrade : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dance_grade)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        onWindowFocusChanged(true)

        bgConstraint = findViewById(R.id.bgContraint)

        bgBanner = findViewById(R.id.bgImage)
        val bitmap = BitmapFactory.decodeFile(playerSong.rutaBanner)
        val bitmapDrawable = BitmapDrawable(resources, bitmap)
        bgBanner.background = bitmapDrawable
        bgBanner.layoutParams.height = height
        bgBanner.layoutParams.width = height + (height / 2)

        bgBanner.alpha = 0.5f

        val perfect = 1 * resultSong.perfect
        val great = 0.6 * resultSong.great
        val good = 0.2 * resultSong.good
        val bad = 0.1 * resultSong.bad
        val miss = 0
        val totalNotes = resultSong.perfect + resultSong.great + resultSong.good + resultSong.bad + resultSong.miss
        val noteWeighs = perfect + great + good + bad + miss
        totalScore = (((0.995 * noteWeighs) + (0.005 * resultSong.maxCombo)) / totalNotes) * 1000000

        //val sss = totalNotes * 1
        //val scoreSSS = (((0.995 * sss) + (0.005 * resultSong.maxCombo)) / totalNotes) * 1000000

        mediaPlayerEvaluation = MediaPlayer.create(this, Uri.fromFile(File(getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/Evaluation.mp3")!!.absolutePath)))
        if(!mediaPlayerEvaluation.isPlaying){
            mediaPlayerEvaluation.start()
        }
        mediaPlayerEvaluation.setOnCompletionListener {
            mediaPlayerEvaluation.start()
        }

        val bitmapEvalutaionLables = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/dance_grade/Evaluation_labels 1x8.png")!!.absolutePath)
        val partHeight = bitmapEvalutaionLables.height / 8
        val imageViews = listOf(
            findViewById(R.id.imgPerfect),
            findViewById(R.id.imgGreat),
            findViewById(R.id.imgGood),
            findViewById(R.id.imgBad),
            findViewById(R.id.imgMiss),
            findViewById(R.id.imgMaxCombo),
            findViewById<ImageView>(R.id.imgScore)
        )

        val textViews = listOf(
            findViewById(R.id.txPerfect),
            findViewById(R.id.txGreat),
            findViewById(R.id.txGood),
            findViewById(R.id.txBad),
            findViewById(R.id.txMiss),
            findViewById(R.id.txMaxCombo),
            findViewById<TextView>(R.id.txScore)
        )

        for (i in 0 until 7) {
            val partBitmap = Bitmap.createBitmap(bitmapEvalutaionLables, 0, i * partHeight, bitmapEvalutaionLables.width, partHeight)
            imageViews[i].setImageBitmap(partBitmap)
        }

        for (i in 0 until 7) {
            imageViews[i].layoutParams.width = width
            imageViews[i].layoutParams.height = (medidaFlechas / 3).toInt()
            imageViews[i].scaleType = ImageView.ScaleType.CENTER_CROP

            imageViews[i].isVisible = false
            textViews[i].textSize = 15f
            textViews[i].apply {
                setTextColor(Color.WHITE)
                textAlignment = TextView.TEXT_ALIGNMENT_VIEW_START
                isAllCaps = true
                setShadowLayer(1.6f, 1.5f, 1.3f, Color.BLACK)
                layoutParams.height = (medidaFlechas / 3).toInt()
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            val jobs = mutableListOf<Job>()
            for (bar in imageViews) {
                jobs.add(animateBar(bar))
                delay(200)
            }

            jobs.joinAll()
            setCounts(resultSong, textViews)
        }
    }

    private fun animateBar(bar: ImageView): Job {
        return CoroutineScope(Dispatchers.Main).launch {
            bar.isVisible = true  // Hacer visible la barra
            val animation = ValueAnimator.ofInt(0, width)
            animation.duration = 500
            animation.interpolator = LinearInterpolator()

            val completableDeferred = CompletableDeferred<Unit>()

            animation.addUpdateListener { animator ->
                val value = animator.animatedValue as Int
                val layoutParams = bar.layoutParams
                layoutParams.width = value
                bar.layoutParams = layoutParams
            }

            animation.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    completableDeferred.complete(Unit)  // Completar el deferred al finalizar la animación
                }
            })

            animation.start()
            completableDeferred.await()  // Esperar hasta que termine la animación
        }
    }

    private suspend fun setCounts(resultSong: ResultSong, textViews: List<TextView>) {

        coroutineScope {
            textViews[0].text = resultSong.perfect.toString()
            textViews[1].text = resultSong.great.toString()
            textViews[2].text = resultSong.good.toString()
            textViews[3].text = resultSong.bad.toString()
            textViews[4].text = resultSong.miss.toString()
            textViews[5].text = resultSong.maxCombo.toString()
            textViews[6].text = totalScore.toInt().toString()

            for (textView in textViews){
                soundPoolSelectSongKsf.play(tick, 1.0f, 1.0f, 1, 0, 1.0f)
                delay(100)
            }

        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(mediaPlayerEvaluation.isPlaying){
            mediaPlayerEvaluation.stop()
            //mediaPlayerEvaluation.release()
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        mediaPlayerEvaluation.stop()
        this.finish()

    }

    override fun onPause() {
        super.onPause()
        mediaPlayerEvaluation.pause()
    }

    override fun onResume() {
        super.onResume()
        mediaPlayerEvaluation.start()
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