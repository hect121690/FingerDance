package com.fingerdance

import android.animation.*
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import kotlinx.coroutines.*
import java.io.File

private lateinit var bgConstraint: ConstraintLayout
private lateinit var mediaPlayerEvaluation: MediaPlayer
private lateinit var grade : Bitmap


private var totalScore = 0.0
private var soundGrade = 0
private var newGrade = ""


private lateinit var DGContext: Context

class DanceGrade : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dance_grade)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        onWindowFocusChanged(true)

        DGContext = this
        bgConstraint = findViewById(R.id.bgContraint)

        val dbDG = DataBasePlayer(this)
        dbDG.writableDatabase
        val handlerDG = Handler()
        val imgGrade = findViewById<ImageView>(R.id.imgGrade)
        imgGrade.visibility = View.INVISIBLE
        imgGrade.layoutParams.width = ((width / 10) * 4.5).toInt()
        imgGrade.layoutParams.height = ((width / 10) * 6)

        val imgMyBestScore = findViewById<ImageView>(R.id.imgBestScoreDG)
        val imgMyBestGrade = findViewById<ImageView>(R.id.imgBestGradeDG)
        val lbBestScoreDG = findViewById<TextView>(R.id.lbBestScoreDG)

        val imgNewRecord = findViewById<ImageView>(R.id.imgNewRecord)
        imgNewRecord.visibility = View.INVISIBLE
        //imgNewRecord.layoutParams.width = ((width / 10) * 3.5).toInt()
        //imgNewRecord.layoutParams.height = ((width / 10) * 8.5).toInt()

        val bgBanner: ImageView = findViewById(R.id.bgImage)
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

        val rutaGrades = getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/dance_grade/").toString()
        val arrayGrades = getGrades(rutaGrades)
        imgMyBestScore.setImageBitmap(BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/score_body.png").toString()))

        val bitNR = BitmapFactory.decodeFile("$rutaGrades/new_record.png")
        imgNewRecord.setImageBitmap(bitNR)

        showGrade(arrayGrades)

        mediaPlayerEvaluation = MediaPlayer.create(this, Uri.fromFile(File(getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/Evaluation.mp3")!!.absolutePath)))
        mediaPlayerEvaluation.start()
        mediaPlayerEvaluation.setOnCompletionListener {
            mediaPlayerEvaluation.start()
        }
        val pathCommandEffect = getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/command_window/Command_Effect.png")!!.absolutePath
        getEfects(pathCommandEffect)

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
            imageViews[i].scaleType = ImageView.ScaleType.CENTER_CROP
            imageViews[i].isVisible = false
            textViews[i].apply {
                setTextColor(Color.WHITE)
                isAllCaps = true
                setShadowLayer(1.6f, 1.5f, 1.3f, Color.BLACK)
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

            handlerDG.postDelayed({
                imgGrade.setImageBitmap(grade)
                animateImageView(imgGrade)
                soundPoolSelectSongKsf.play(rank_sound, 1.0f, 1.0f, 1, 0, 1.0f)
                val rankA = getRankSound(soundGrade)
                val rankB = getRankSoundB(soundGrade)
                soundPoolSelectSongKsf.play(rankA,1.0f, 1.0f, 1, 0, 1.0f)
                soundPoolSelectSongKsf.play(rankB,1.0f, 1.0f, 1, 0, 1.0f)

                if(totalScore > currentScore.toInt()){
                    dbDG.updatePuntaje(currentChannel, currentSong, currentLevel, totalScore.toInt().toString(), newGrade)
                    imgMyBestGrade.setImageBitmap(grade)
                    lbBestScoreDG.text = totalScore.toInt().toString()
                    showNewRecord(imgNewRecord, handlerDG)
                }else{
                    getBestScore(imgMyBestGrade, lbBestScoreDG)
                }

            }, 500)
        }
    }

    private fun getEfects(pathCommandEffect: String) {
        showLevel()
        val posX = medidaFlechas / 10f

        val imgSpeed = ImageView(this).apply {
            setImageBitmap(BitmapFactory.decodeFile(pathCommandEffect))
        }
        bgConstraint.addView(imgSpeed)
        imgSpeed.x = posX
        imgSpeed.y = (height / 2f) + medidaFlechas * 0.7f
        imgSpeed.layoutParams.width = medidaFlechas.toInt()

        val txtSpeed = TextView(this).apply {
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setTextColor(ContextCompat.getColor(context, R.color.white))
            textSize = medidaFlechas / 10f
            setTypeface(typeface, Typeface.BOLD_ITALIC)
            text = playerSong.speed
        }
        bgConstraint.addView(txtSpeed)
        txtSpeed.layoutParams.width = medidaFlechas.toInt()
        txtSpeed.x = posX + (posX / 4f)
        txtSpeed.y = (height / 2f) + medidaFlechas * 0.95f

        val imgNoteSkinBG = ImageView(this).apply {
            setImageBitmap(BitmapFactory.decodeFile(pathCommandEffect))
        }
        bgConstraint.addView(imgNoteSkinBG)
        imgNoteSkinBG.x = medidaFlechas + posX
        imgNoteSkinBG.y = (height / 2f) + medidaFlechas * 0.7f
        imgNoteSkinBG.layoutParams.width = medidaFlechas.toInt()

        val imgNoteSkinDG =  ImageView(this).apply {
            setImageBitmap(BitmapFactory.decodeFile(playerSong.rutaNoteSkin + "/_Icon.png"))
        }
        bgConstraint.addView(imgNoteSkinDG)
        imgNoteSkinDG.x = medidaFlechas + posX
        imgNoteSkinDG.y = (height / 2f) + medidaFlechas * 0.8f
        imgNoteSkinDG.layoutParams.width = medidaFlechas.toInt()
        imgNoteSkinDG.layoutParams.height = (medidaFlechas.toInt() / 10) * 6

        if(playerSong.hj){
            val image = ImageView(this)
            image.setImageBitmap(BitmapFactory.decodeFile(playerSong.pathImgHJ))
            bgConstraint.addView(image)
            image.x = (medidaFlechas * 2) + posX
            image.y = (height / 2f) + medidaFlechas * 0.7f
            image.layoutParams.width = medidaFlechas.toInt()
        }
        for(i in 0 until listEfectsDisplay.size){
            val image = ImageView(this)
            image.setImageBitmap(BitmapFactory.decodeFile(listEfectsDisplay[i].rutaCommandImg))
            bgConstraint.addView(image)
            image.x = (medidaFlechas * i) + posX
            image.y = (height / 2f) + medidaFlechas * 1.6f
            image.layoutParams.width = medidaFlechas.toInt()
        }
    }

    private fun showLevel() {
        val bitActiveLv = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/lv_active.png")!!.absolutePath)
        val imgBitActive = ImageView(this)
        imgBitActive.setImageBitmap(bitActiveLv)
        bgConstraint.addView(imgBitActive)
        imgBitActive.layoutParams.height = medidaFlechas.toInt() * 2
        imgBitActive.layoutParams.width = medidaFlechas.toInt() * 2
        imgBitActive.x = medidaFlechas * 4.5f
        imgBitActive.y = (height / 2f)
        val txtLv = TextView(this).apply {
            setShadowLayer(1.6f, 1.5f, 1.3f, Color.BLACK)
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setTextColor(ContextCompat.getColor(context, R.color.white))
            textSize = medidaFlechas / 2.5f
            setTypeface(typeface, Typeface.BOLD)
            text = playerSong.level
        }
        bgConstraint.addView(txtLv)
        txtLv.layoutParams.width = medidaFlechas.toInt() * 2
        txtLv.x = medidaFlechas * 4.5f
        txtLv.y = (height / 2f) + (medidaFlechas / 3f)
    }

    private fun getBestScore(imgMyBestGrade: ImageView, lbBestScoreDG: TextView) {
        imgMyBestGrade.setImageBitmap(currentBestGrade)
        lbBestScoreDG.text = currentScore
    }

    private fun showGrade(arrayGrades: ArrayList<Bitmap>) {
        if(totalScore.toInt() >= 999999){ //SSS
            grade = arrayGrades[0]
            soundGrade = 1
            newGrade = "SSS"
        }
        if(totalScore.toInt() in 950000..999998){ //S
            grade = arrayGrades[2]
            soundGrade = 3
            newGrade = "S"
        }
        if(totalScore.toInt() in 950000..999998 && resultSong.bad == 0 && resultSong.miss == 0){ //SS
            grade = arrayGrades[1]
            soundGrade = 2
            newGrade = "SS"
        }
        if(totalScore.toInt() in 900000..949000){ //A
            grade = arrayGrades[3]
            soundGrade = 4
            newGrade = "A"
        }
        if(totalScore.toInt() in 800000..899999){ //B
            grade = arrayGrades[4]
            soundGrade = 5
            newGrade = "B"
        }
        if(totalScore.toInt() in 700000..799999){ //C
            grade = arrayGrades[5]
            soundGrade = 6
            newGrade = "C"
        }
        if(totalScore.toInt() in 600000..699999){ //D
            grade = arrayGrades[6]
            soundGrade = 7
            newGrade = "D"
        }
        if(totalScore.toInt() <= 599999){ //F
            grade = arrayGrades[7]
            soundGrade = 8
            newGrade = "F"
        }
    }

    private fun showNewRecord(imgNewRecord: ImageView, handlerDG: Handler) {
        handlerDG.postDelayed({
            imgNewRecord.visibility = View.VISIBLE
            imgNewRecord.startAnimation(AnimationUtils.loadAnimation(DGContext, R.anim.stamp_effect))
            soundPoolSelectSongKsf.play(new_record, 1.0f, 1.0f, 1, 0, 1.0f)
        }, 1500)
    }

    private fun animateImageView(view: ImageView) {
        view.visibility = View.VISIBLE
        view.alpha = 0f
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", width.toFloat() / view.width, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", height.toFloat() / view.height, 1f)

        val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, fadeIn)
        animatorSet.duration = 500 // Duración de la animación en milisegundos
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.start()
    }

    private fun getRankSound(soundGrade: Int): Int {
        return when (soundGrade) {
            1 -> {sss_rank}
            2 -> {ss_rank}
            3 -> {s_rank}
            4 -> {a_rank}
            5 -> {b_rank}
            6 -> {c_rank}
            7 -> {d_rank}
            8 -> {f_rank}
            else -> {0}
        }
    }
    private fun getRankSoundB(soundGrade: Int): Int {
        return when (soundGrade) {
            1 -> {sss_rankB}
            2 -> {ss_rankB}
            3 -> {s_rankB}
            4 -> {a_rankB}
            5 -> {b_rankB}
            6 -> {c_rankB}
            7 -> {d_rankB}
            8 -> {f_rankB}
            else -> {0}
        }
    }

    private fun getGrades(rutaGrades: String) : ArrayList<Bitmap>{
        val bit = BitmapFactory.decodeFile("$rutaGrades/evaluation_grades 1x8.png")
        return ArrayList<Bitmap>().apply {
            var i = 0
            for (a in 0 until 8) {
                add(Bitmap.createBitmap(bit, 0, i, bit.width, bit.height / 8))
                i += bit.height / 8
            }
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
                    completableDeferred.complete(Unit)
                }
            })

            animation.start()
            completableDeferred.await()
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