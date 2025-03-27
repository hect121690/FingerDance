package com.fingerdance

import android.animation.*
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.animation.*
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.*
import java.io.File
import kotlin.random.Random

private lateinit var bgConstraint: ConstraintLayout
private lateinit var mediaPlayerEvaluation: MediaPlayer
private lateinit var grade : Bitmap

private lateinit var gradeP1 : Bitmap
private lateinit var gradeP2 : Bitmap

private var totalScore = 0
private var soundGrade = 0
private var newGrade = ""
private var rankA = 0
private var rankB = 0

private var isPlayingRankA = 0
private var isPlayingRankB = 0
private var isPlayingNewRecord = 0

private lateinit var DGContext: Context
private lateinit var linearEfects: RelativeLayout
private val handlerDG = Handler()
private var readyToFireBase = false

class DanceGrade : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dance_grade)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        onWindowFocusChanged(true)

        DGContext = this
        bgConstraint = findViewById(R.id.bgContraint)
        linearEfects = findViewById(R.id.linearEfects)

        val txPlayer1 = findViewById<TextView>(R.id.txPlayer1)
        val txPlayer2 = findViewById<TextView>(R.id.txPlayer2)

        if(isOnline){
            if(isPlayer1){
                txPlayer1.text = userName
                txPlayer2.text = activeSala.jugador2.id
            }else{
                txPlayer1.text = activeSala.jugador1.id
                txPlayer2.text = userName
            }
        }else{
            txPlayer1.visibility = View.GONE
            txPlayer2.visibility = View.GONE
        }

        val bgBanner: ImageView = findViewById(R.id.bgImage)
        val bitmap = BitmapFactory.decodeFile(playerSong.rutaBanner)
        val bitmapDrawable = BitmapDrawable(resources, bitmap)
        bgBanner.background = bitmapDrawable
        bgBanner.layoutParams.height = height
        bgBanner.layoutParams.width = height + (height / 2)

        bgBanner.alpha = 0.5f

        val rutaGrades = getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/dance_grade/").toString()

        val dbDG = DataBasePlayer(this)
        dbDG.writableDatabase

        val imgGrade = findViewById<ImageView>(R.id.imgGrade)
        imgGrade.visibility = View.INVISIBLE
        imgGrade.layoutParams.width = ((width / 10) * 4.5).toInt()
        imgGrade.layoutParams.height = ((width / 10) * 6)

        val imgGradeP1 = findViewById<ImageView>(R.id.imgGradeP1)
        imgGradeP1.visibility = View.INVISIBLE
        imgGradeP1.layoutParams.width = ((width / 10) * 4.5).toInt()
        imgGradeP1.layoutParams.height = ((width / 10) * 6)

        val imgGradeP2 = findViewById<ImageView>(R.id.imgGradeP2)
        imgGradeP2.visibility = View.INVISIBLE
        imgGradeP2.layoutParams.width = ((width / 10) * 4.5).toInt()
        imgGradeP2.layoutParams.height = ((width / 10) * 6)

        val imgWinP1 = findViewById<ImageView>(R.id.imgWinP1)
        imgWinP1.layoutParams.width = ((width / 10) * 4.5).toInt()

        val imgWinP2 = findViewById<ImageView>(R.id.imgWinP2)
        imgWinP2.layoutParams.width = ((width / 10) * 4.5).toInt()

        val imgDraw = findViewById<ImageView>(R.id.imgDraw)
        imgDraw.layoutParams.width = ((width / 10) * 4.5).toInt()

        val imgMyBestScore = findViewById<ImageView>(R.id.imgBestScoreDG)
        val imgMyBestGrade = findViewById<ImageView>(R.id.imgBestGradeDG)
        val lbBestScoreDG = findViewById<TextView>(R.id.lbBestScoreDG)

        val imgNewRecord = findViewById<ImageView>(R.id.imgNewRecord)
        imgNewRecord.visibility = View.INVISIBLE

        val txNameSong = findViewById<TextView>(R.id.txNameSong)
        val txNameChannel = findViewById<TextView>(R.id.txNameChannel)
        val txStepMaker = findViewById<TextView>(R.id.txStepMaker)

        txNameSong.background = Drawable.createFromPath("$rutaGrades/Evaluation_Top label name.png")
        txNameChannel.background = Drawable.createFromPath("$rutaGrades/Evaluation_Top label channel.png")

        txStepMaker.text = if(playerSong.stepMaker != "") "STEPMAKER: ${playerSong.stepMaker}" else ""

        txNameSong.apply {
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setTextColor(ContextCompat.getColor(context, R.color.white))
            textSize = medidaFlechas / 10f
            setTypeface(typeface, Typeface.BOLD)
            setShadowLayer(1.6f, 1.5f, 1.3f, Color.BLACK)
        }

        txNameChannel.apply {
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setTextColor(ContextCompat.getColor(context, R.color.white))
            textSize = medidaFlechas / 10f
            setTypeface(typeface, Typeface.BOLD)
            setShadowLayer(1.6f, 1.5f, 1.3f, Color.BLACK)
        }

        txStepMaker.apply {
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setTextColor(ContextCompat.getColor(context, R.color.white))
            textSize = medidaFlechas / 10f
            setTypeface(typeface, Typeface.BOLD)
            setShadowLayer(1.6f, 1.5f, 1.3f, Color.BLACK)
        }

        val perfect = 1 * resultSong.perfect
        val great = 0.6 * resultSong.great
        val good = 0.2 * resultSong.good
        val bad = 0.1 * resultSong.bad
        val miss = 0
        val totalNotes = resultSong.perfect + resultSong.great + resultSong.good + resultSong.bad + resultSong.miss
        val noteWeighs = perfect + great + good + bad + miss
        totalScore = ((((0.995 * noteWeighs) + (0.005 * resultSong.maxCombo)) / totalNotes) * 1000000).toInt()

        val bitNR = BitmapFactory.decodeFile("$rutaGrades/new_record.png")
        imgNewRecord.setImageBitmap(bitNR)
        if(!isOnline){
            imgMyBestScore.setImageBitmap(BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/score_body_dance_grade.png").toString()))
            showGrade(arrayBestGrades)
            txNameSong.text = currentSong
            txNameChannel.text = if(currentChannel.contains("-")) currentChannel.split("-")[1] else currentChannel
        }else{
            txNameSong.text = activeSala.cancion.nameSong

            if(isPlayer1){
                activeSala.jugador1.result.perfect = resultSong.perfect.toString()
                activeSala.jugador1.result.great = resultSong.great.toString()
                activeSala.jugador1.result.good = resultSong.good.toString()
                activeSala.jugador1.result.bad = resultSong.bad.toString()
                activeSala.jugador1.result.miss = resultSong.miss.toString()
                activeSala.jugador1.result.maxCombo = resultSong.maxCombo.toString()
                activeSala.jugador1.result.score = totalScore.toString()
                salaRef.child("jugador1/result").setValue(activeSala.jugador1.result).addOnSuccessListener {
                    getWinner(imgWinP1, imgWinP2, imgDraw, txNameChannel)
                }
            }else{
                activeSala.jugador2.result.perfect = resultSong.perfect.toString()
                activeSala.jugador2.result.great = resultSong.great.toString()
                activeSala.jugador2.result.good = resultSong.good.toString()
                activeSala.jugador2.result.bad = resultSong.bad.toString()
                activeSala.jugador2.result.miss = resultSong.miss.toString()
                activeSala.jugador2.result.maxCombo = resultSong.maxCombo.toString()
                activeSala.jugador2.result.score = totalScore.toString()
                salaRef.child("jugador2/result").setValue(activeSala.jugador2.result).addOnSuccessListener{
                    getWinner(imgWinP1, imgWinP2, imgDraw, txNameChannel)
                }
            }
        }

        if(!isOnline) {
            if (totalScore > currentWorldScore.toInt() || currentScore.toInt() > currentWorldScore.toInt()) {
                updateRanking(
                    currentChannel,
                    currentSong,
                    currentLevel,
                    totalScore.toString(),
                    userName,
                    newGrade,
                    imgNewRecord
                )
            }
        }

        val bg_wait = findViewById<ConstraintLayout>(R.id.bg_wait)
        val img_wait = findViewById<ImageView>(R.id.img_wait)
        bg_wait.isVisible = false
        img_wait.isVisible = false
        bg_wait.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                // No hace nada
            }
        })
        getBtnAceptar(bg_wait, img_wait)

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
        val textViewsP1 = listOf(
            findViewById(R.id.txPerfectP1),
            findViewById(R.id.txGreatP1),
            findViewById(R.id.txGoodP1),
            findViewById(R.id.txBadP1),
            findViewById(R.id.txMissP1),
            findViewById(R.id.txMaxComboP1),
            findViewById<TextView>(R.id.txScoreP1)
        )

        val textViewsP2 = listOf(
            findViewById(R.id.txPerfectP2),
            findViewById(R.id.txGreatP2),
            findViewById(R.id.txGoodP2),
            findViewById(R.id.txBadP2),
            findViewById(R.id.txMissP2),
            findViewById(R.id.txMaxComboP2),
            findViewById<TextView>(R.id.txScoreP2)
        )

        for (i in 0 until 7) {
            val partBitmap = Bitmap.createBitmap(bitmapEvalutaionLables, 0, i * partHeight, bitmapEvalutaionLables.width, partHeight)
            imageViews[i].setImageBitmap(partBitmap)
        }

        for (i in 0 until 7) {
            imageViews[i].scaleType = ImageView.ScaleType.CENTER_CROP
            imageViews[i].isVisible = false

            textViewsP1[i].apply {
                setTextColor(Color.WHITE)
                isAllCaps = true
                setShadowLayer(1.6f, 1.5f, 1.3f, Color.BLACK)
            }
            textViewsP2[i].apply {
                setTextColor(Color.WHITE)
                isAllCaps = true
                setShadowLayer(1.6f, 1.5f, 1.3f, Color.BLACK)
            }
        }
        handlerDG.postDelayed({
            CoroutineScope(Dispatchers.Main).launch {
                val jobs = mutableListOf<Job>()
                for (bar in imageViews) {
                    jobs.add(animateBar(bar))
                    delay(200)
                }

                jobs.joinAll()

                if(!isOnline) {
                    setCountsP1(textViewsP1)
                }else{
                    activeSala.readyToResult = true
                    salaRef.child("readyToResult").setValue(activeSala.readyToResult).addOnSuccessListener {
                        CoroutineScope(Dispatchers.Main).launch {
                            setCountsP1(textViewsP1)
                            setCountsP2(textViewsP2)
                        }
                    }
                }

                handlerDG.postDelayed({
                    if(!isOnline){
                        imgGrade.setImageBitmap(grade)
                        animateImageView(imgGrade)
                        soundPoolSelectSongKsf.play(rank_sound, 1.0f, 1.0f, 1, 0, 1.0f)
                        rankA = getRankSound(soundGrade)
                        rankB = getRankSoundB(soundGrade)
                        isPlayingRankA = soundPoolSelectSongKsf.play(rankA, 1.0f, 1.0f, 1, 0, 1.0f)
                        isPlayingRankB = soundPoolSelectSongKsf.play(rankB, 1.0f, 1.0f, 1, 0, 1.0f)

                        if (totalScore > currentScore.toInt()) {
                            dbDG.updatePuntaje(
                                currentChannel,
                                currentSong,
                                currentLevel,
                                totalScore.toString(),
                                newGrade
                            )
                            imgMyBestGrade.setImageBitmap(grade)
                            lbBestScoreDG.text = totalScore.toString()
                        } else {
                            getBestScore(imgMyBestGrade, lbBestScoreDG)
                        }
                    }else{
                        imgGradeP1.setImageBitmap(gradeP1)
                        animateImageViewP1(imgGradeP1)

                        imgGradeP2.setImageBitmap(gradeP2)
                        animateImageViewP2(imgGradeP2)

                        soundPoolSelectSongKsf.play(rank_sound, 1.0f, 1.0f, 1, 0, 1.0f)
                    }
                }, 2000L)
            }
        },2000L)
    }

    private fun getWinner(imgWinP1: ImageView, imgWinP2: ImageView, imgDraw: ImageView, txNameChannel: TextView){
        handlerDG.postDelayed({
            if(isPlayer1){
                if(activeSala.jugador1.result.score.toInt() > activeSala.jugador2.result.score.toInt()){
                    imgWinP1.setImageBitmap(BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/player_win.png").toString()))
                    activeSala.jugador1.victories = (activeSala.jugador1.victories.toInt() + 1).toString()
                }else if(activeSala.jugador1.result.score.toInt() < activeSala.jugador2.result.score.toInt()){
                    imgWinP2.setImageBitmap(BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/player_win.png").toString()))
                }else{
                    imgDraw.setImageBitmap(BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/draw.png").toString()))
                }
                salaRef.child("jugador1/result/victories").setValue(activeSala.jugador1.victories)
            }else{
                if(activeSala.jugador2.result.score.toInt() > activeSala.jugador1.result.score.toInt()){
                    imgWinP2.setImageBitmap(BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/player_win.png").toString()))
                    activeSala.jugador2.victories = (activeSala.jugador2.victories.toInt() + 1).toString()
                }else if(activeSala.jugador2.result.score.toInt() < activeSala.jugador1.result.score.toInt()){
                    imgWinP1.setImageBitmap(BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/player_win.png").toString()))
                }else{
                    imgDraw.setImageBitmap(BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/draw.png").toString()))
                }
                salaRef.child("jugador2/result/victories").setValue(activeSala.jugador2.victories)
            }
            txNameChannel.text = "${activeSala.jugador1.victories}          VICTORIAS          ${activeSala.jugador2.victories}"
            showGradeP1(activeSala.jugador1.result.score.toInt())
            showGradeP2(activeSala.jugador2.result.score.toInt())
        }, 500L)
    }

    private fun updateRanking(
        channel: String,
        song: String,
        level: String,
        newScore: String,
        namePlayer: String,
        grade: String,
        imgNewRecord: ImageView
    ) {
        val databaseRef = firebaseDatabase.getReference("channels")
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (canalSnapshot in snapshot.children) {
                    val canal = canalSnapshot.child("canal").getValue(String::class.java)
                    if (canal == channel) {
                        for (cancionSnapshot in canalSnapshot.child("canciones").children) {
                            val cancion = cancionSnapshot.child("cancion").getValue(String::class.java)
                            if (cancion == song) {
                                for ((index, nivelSnapshot) in cancionSnapshot.child("niveles").children.withIndex()) {
                                    val nivel = nivelSnapshot.child("nivel").getValue(String::class.java)
                                    val puntajeActual = nivelSnapshot.child("puntaje").getValue(String::class.java)?.toIntOrNull() ?: 0

                                    if (nivel == level && newScore.toInt() > puntajeActual) {
                                        val nivelRef = databaseRef.child(canalSnapshot.key!!)
                                            .child("canciones").child(cancionSnapshot.key!!)
                                            .child("niveles").child(index.toString())

                                        val updateData = mapOf(
                                            "puntaje" to newScore,
                                            "nombre" to namePlayer,
                                            "grade" to grade
                                        )

                                        nivelRef.updateChildren(updateData)
                                            .addOnSuccessListener {
                                                showNewRecord(imgNewRecord)
                                                Log.d("Ranking", "Puntaje actualizado correctamente")
                                            }
                                            .addOnFailureListener { e -> Log.e("Ranking", "Error al actualizar ranking", e) }

                                        return
                                    }
                                }
                            }
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al leer datos", error.toException())
            }
        })
    }

    private fun getBtnAceptar(bg_wait: ConstraintLayout, img_wait: ImageView) {
        val imgFloor = findViewById<ImageView>(R.id.floorDG)
        val bmFloor = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/floor.png")!!.absolutePath)
        imgFloor.setImageBitmap(bmFloor)

        val imgAceptar = findViewById<ImageView>(R.id.aceptDG)
        val bm = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/press_floor.png")!!.absolutePath)
        imgAceptar.setImageBitmap(bm)

        val numberWait = Random.nextInt(1,10)
        img_wait.setImageBitmap(BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/dance_grade/img_dance_grade ($numberWait).png")!!.absolutePath))

        imgAceptar.layoutParams.width = (width / 2.5).toInt()

        val yDelta = width / 36
        val animateSetTraslation = TranslateAnimation(0f, 0f, -yDelta.toFloat(), (yDelta * 1.5).toFloat())
        animateSetTraslation.duration = 400
        animateSetTraslation.repeatCount = Animation.INFINITE
        animateSetTraslation.repeatMode = Animation.REVERSE
        imgAceptar.startAnimation(animateSetTraslation)
        imgAceptar.bringToFront()

        imgAceptar.setOnClickListener {
            soundPoolSelectSongKsf.play(startKsf, 1.0f, 1.0f, 1, 0, 1.0f)
            if(!isOnline) {
                escucharPuntajesPorNombre(currentChannel) { listaCanciones ->
                    listGlobalRanking = listaCanciones
                }
            }
            mediaPlayerEvaluation.stop()
            soundPoolSelectSongKsf.stop(isPlayingRankA)
            soundPoolSelectSongKsf.stop(isPlayingRankB)
            soundPoolSelectSongKsf.stop(isPlayingNewRecord)
            handlerDG.removeCallbacksAndMessages(null)
            img_wait.isVisible = true
            bg_wait.isVisible = true

            thisHandler.postDelayed({
                getSelectChannel = true
                this.finish()
            }, 2500)

        }
        imgFloor.setOnLongClickListener {
            imgAceptar.performClick()
        }
    }

    private fun escucharPuntajesPorNombre(canalNombre: String, callback: (ArrayList<Cancion>) -> Unit) {
        val databaseRef = firebaseDatabase.getReference("channels")
        val listResult = arrayListOf<Cancion>()
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (canalSnapshot in snapshot.children) {
                    val canal = canalSnapshot.child("canal").getValue(String::class.java)
                    if (canal == canalNombre) {
                        for (cancionSnapshot in canalSnapshot.child("canciones").children) {
                            val nombreCancion = cancionSnapshot.child("cancion").getValue(String::class.java) ?: ""
                            val niveles = arrayListOf<Nivel>()
                            for (nivelSnapshot in cancionSnapshot.child("niveles").children) {
                                val grade = nivelSnapshot.child("grade").getValue(String::class.java) ?: ""
                                val nivel = nivelSnapshot.child("nivel").getValue(String::class.java) ?: ""
                                val nombre = nivelSnapshot.child("nombre").getValue(String::class.java) ?: ""
                                val puntaje = nivelSnapshot.child("puntaje").getValue(String::class.java) ?: "0"

                                niveles.add(Nivel(nivel, nombre, puntaje, grade))
                            }

                            listResult.add(Cancion(nombreCancion, niveles))
                        }
                        callback(listResult)
                        return
                    }
                }
                callback(arrayListOf())
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al leer datos", error.toException())
            }
        })
    }

    private fun getEfects(pathCommandEffect: String) {
        showLevel()
        val posX = medidaFlechas
        val heightImages = ((medidaFlechas / 4) * 3).toInt()

        linearEfects.layoutParams.width = (medidaFlechas * 3).toInt()
        linearEfects.layoutParams.height = (heightImages * 2)

        val imgSpeed = ImageView(this).apply {
            setImageBitmap(BitmapFactory.decodeFile(pathCommandEffect))
        }
        linearEfects.addView(imgSpeed)

        imgSpeed.layoutParams.width = medidaFlechas.toInt()
        imgSpeed.layoutParams.height = heightImages

        val txtSpeed = TextView(this).apply {
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setTextColor(ContextCompat.getColor(context, R.color.white))
            textSize = medidaFlechas / 10f
            setTypeface(typeface, Typeface.BOLD_ITALIC)
            text = playerSong.speed
        }
        linearEfects.addView(txtSpeed)
        txtSpeed.layoutParams.width = medidaFlechas.toInt()
        txtSpeed.layoutParams.height = heightImages
        txtSpeed.y = linearEfects.y + (heightImages.toFloat() / 4)

        val imgNoteSkinBG = ImageView(this).apply {
            setImageBitmap(BitmapFactory.decodeFile(pathCommandEffect))
        }
        linearEfects.addView(imgNoteSkinBG)
        imgNoteSkinBG.x = posX
        imgNoteSkinBG.layoutParams.height = heightImages
        imgNoteSkinBG.layoutParams.width = medidaFlechas.toInt()

        val imgNoteSkinDG =  ImageView(this).apply {
            setImageBitmap(BitmapFactory.decodeFile(playerSong.rutaNoteSkin + "/_Icon.png"))
        }
        linearEfects.addView(imgNoteSkinDG)
        imgNoteSkinDG.x = posX
        imgNoteSkinDG.layoutParams.width = medidaFlechas.toInt()
        imgNoteSkinDG.layoutParams.height = heightImages

        if(playerSong.hj){
            val image = ImageView(this)
            image.setImageBitmap(BitmapFactory.decodeFile(playerSong.pathImgHJ))
            linearEfects.addView(image)
            image.x = posX * 2
            image.layoutParams.width = medidaFlechas.toInt()
            image.layoutParams.height = heightImages
        }
        for(i in 0 until listEfectsDisplay.size){
            val image = ImageView(this)
            image.setImageBitmap(BitmapFactory.decodeFile(listEfectsDisplay[i].rutaCommandImg))
            linearEfects.addView(image)
            image.x = (posX * i)
            image.y = linearEfects.y + heightImages.toFloat()
            image.layoutParams.width = medidaFlechas.toInt()
            image.layoutParams.height = heightImages
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
            setTypeface(typeface, Typeface.BOLD)
            if(!isOnline){
                text = playerSong.level
            }else{
                text = activeSala.cancion.nivel
            }
        }

        bgConstraint.addView(txtLv)

        val textSize = (imgBitActive.layoutParams.width / 2.5f)
        txtLv.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize)

        txtLv.layoutParams.width = imgBitActive.layoutParams.width
        txtLv.layoutParams.height = imgBitActive.layoutParams.height

        txtLv.x = imgBitActive.x + (imgBitActive.layoutParams.width - txtLv.layoutParams.width) / 2
        txtLv.y = imgBitActive.y + (txtLv.layoutParams.height / 4.5).toInt()
    }

    private fun getBestScore(imgMyBestGrade: ImageView, lbBestScoreDG: TextView) {
        imgMyBestGrade.setImageBitmap(currentBestGrade)
        lbBestScoreDG.text = currentScore
    }

    private fun showGrade(arrayGrades: ArrayList<Bitmap>) {
        if(totalScore >= 999999){ //SSS
            grade = arrayGrades[0]
            soundGrade = 1
            newGrade = "SSS"
        }
        if(totalScore in 950000..999998){ //S
            grade = arrayGrades[2]
            soundGrade = 3
            newGrade = "S"
        }
        if(totalScore in 950000..999998 && resultSong.bad == 0 && resultSong.miss == 0){ //SS
            grade = arrayGrades[1]
            soundGrade = 2
            newGrade = "SS"
        }
        if(totalScore in 900000..949000){ //A
            grade = arrayGrades[3]
            soundGrade = 4
            newGrade = "A"
        }
        if(totalScore in 800000..899999){ //B
            grade = arrayGrades[4]
            soundGrade = 5
            newGrade = "B"
        }
        if(totalScore in 700000..799999){ //C
            grade = arrayGrades[5]
            soundGrade = 6
            newGrade = "C"
        }
        if(totalScore in 600000..699999){ //D
            grade = arrayGrades[6]
            soundGrade = 7
            newGrade = "D"
        }
        if(totalScore <= 599999){ //F
            grade = arrayGrades[7]
            soundGrade = 8
            newGrade = "F"
        }
    }

    private fun showGradeP1(totalScoreP1: Int) {
        if(totalScoreP1 >= 999999){ //SSS
            gradeP1 = arrayBestGrades[0]
        }
        if(totalScoreP1 in 950000..999998){ //S
            gradeP1 = arrayBestGrades[2]
        }
        if(totalScoreP1 in 950000..999998 && resultSong.bad == 0 && resultSong.miss == 0){ //SS
            gradeP1 = arrayBestGrades[1]
        }
        if(totalScoreP1 in 900000..949000){ //A
            gradeP1 = arrayBestGrades[3]
        }
        if(totalScoreP1 in 800000..899999){ //B
            gradeP1 = arrayBestGrades[4]
        }
        if(totalScoreP1 in 700000..799999){ //C
            gradeP1 = arrayBestGrades[5]
        }
        if(totalScoreP1 in 600000..699999){ //D
            gradeP1 = arrayBestGrades[6]
        }
        if(totalScoreP1 <= 599999){ //F
            gradeP1 = arrayBestGrades[7]
        }
    }

    private fun showGradeP2(totalScoreP2: Int) {
        if(totalScoreP2 >= 999999){ //SSS
            gradeP2 = arrayBestGrades[0]
        }
        if(totalScoreP2 in 950000..999998){ //S
            gradeP2 = arrayBestGrades[2]
        }
        if(totalScoreP2 in 950000..999998 && resultSong.bad == 0 && resultSong.miss == 0){ //SS
            gradeP2 = arrayBestGrades[1]
        }
        if(totalScoreP2 in 900000..949000){ //A
            gradeP2 = arrayBestGrades[3]
        }
        if(totalScoreP2 in 800000..899999){ //B
            gradeP2 = arrayBestGrades[4]
        }
        if(totalScoreP2 in 700000..799999){ //C
            gradeP2 = arrayBestGrades[5]
        }
        if(totalScoreP2 in 600000..699999){ //D
            gradeP2 = arrayBestGrades[6]
        }
        if(totalScoreP2 <= 599999){ //F
            gradeP2 = arrayBestGrades[7]
        }
    }

    private fun showNewRecord(imgNewRecord: ImageView) {
        handlerDG.postDelayed({
            imgNewRecord.visibility = View.VISIBLE
            imgNewRecord.startAnimation(AnimationUtils.loadAnimation(DGContext, R.anim.stamp_effect))
            isPlayingNewRecord = soundPoolSelectSongKsf.play(new_record, 1.0f, 1.0f, 1, 0, 1.0f)
        }, 2000)
    }

    private fun animateImageView(view: ImageView) {
        view.visibility = View.VISIBLE
        view.alpha = 0f
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", width.toFloat() / view.width, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", height.toFloat() / view.height, 1f)

        val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, fadeIn)
        animatorSet.duration = 500
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.start()
    }

    private fun animateImageViewP1(view: ImageView) {
        view.visibility = View.VISIBLE
        view.alpha = 0f
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", width.toFloat() / view.width, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", height.toFloat() / view.height, 1f)

        val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, fadeIn)
        animatorSet.duration = 500
        animatorSet.interpolator = AccelerateDecelerateInterpolator()
        animatorSet.start()
    }

    private fun animateImageViewP2(view: ImageView) {
        view.visibility = View.VISIBLE
        view.alpha = 0f
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", width.toFloat() / view.width, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", height.toFloat() / view.height, 1f)

        val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, fadeIn)
        animatorSet.duration = 500
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

    private suspend fun setCountsP1(textViews: List<TextView>) {
        coroutineScope {
            if(isOnline){
                textViews[0].text = activeSala.jugador1.result.perfect
                textViews[1].text = activeSala.jugador1.result.great
                textViews[2].text = activeSala.jugador1.result.good
                textViews[3].text = activeSala.jugador1.result.bad
                textViews[4].text = activeSala.jugador1.result.miss
                textViews[5].text = activeSala.jugador1.result.maxCombo
                textViews[6].text = activeSala.jugador1.result.score
            }else{
                textViews[0].text = resultSong.perfect.toString()
                textViews[1].text = resultSong.great.toString()
                textViews[2].text = resultSong.good.toString()
                textViews[3].text = resultSong.bad.toString()
                textViews[4].text = resultSong.miss.toString()
                textViews[5].text = resultSong.maxCombo.toString()
                textViews[6].text = totalScore.toString()
            }

            for (textView in textViews){
                soundPoolSelectSongKsf.play(tick, 1.0f, 1.0f, 1, 0, 1.0f)
                delay(100)
            }
        }
    }

    private suspend fun setCountsP2(textViews: List<TextView>) {
        coroutineScope {
            textViews[0].text = activeSala.jugador2.result.perfect
            textViews[1].text = activeSala.jugador2.result.great
            textViews[2].text = activeSala.jugador2.result.good
            textViews[3].text = activeSala.jugador2.result.bad
            textViews[4].text = activeSala.jugador2.result.miss
            textViews[5].text = activeSala.jugador2.result.maxCombo
            textViews[6].text = activeSala.jugador2.result.score

            for (textView in textViews){
                soundPoolSelectSongKsf.play(tick, 1.0f, 1.0f, 1, 0, 1.0f)
                delay(100)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayerEvaluation.release()
    }
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        //super.onBackPressed()
        Toast.makeText(this, "Press Center Step", Toast.LENGTH_SHORT).show()
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