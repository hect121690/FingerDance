package com.fingerdance

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.view.animation.TranslateAnimation
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
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
private val handlerDG = Handler(Looper.getMainLooper())
private val sizeLabels = (decimoHeigtn / 3.8).toInt()
private var rutaWin = ""
private var rutaDraw = ""

class DanceGrade : AppCompatActivity() {
    private var winP1 = false
    private var winP2 = false
    private var draw = false

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

        rutaWin = getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/player_win.png")!!.absolutePath
        rutaDraw = getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/draw.png")!!.absolutePath
        val rutaGrades = getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/dance_grade/").toString()

        val dbDG = DataBasePlayer(this)
        dbDG.writableDatabase

        val widthGrades = ((width / 10) * 4.5).toInt()
        val heigthGrades = (decimoHeigtn * 1.5).toInt()

        val imgGrade = findViewById<ImageView>(R.id.imgGrade)
        imgGrade.visibility = View.INVISIBLE
        imgGrade.layoutParams.width = widthGrades
        imgGrade.layoutParams.height = ((width / 10) * 6)

        val imgGradeP1 = findViewById<ImageView>(R.id.imgGradeP1)
        imgGradeP1.visibility = View.INVISIBLE
        imgGradeP1.layoutParams.width = widthGrades
        imgGradeP1.layoutParams.height = ((width / 10) * 6)

        val imgGradeP2 = findViewById<ImageView>(R.id.imgGradeP2)
        imgGradeP2.visibility = View.INVISIBLE
        imgGradeP2.layoutParams.width = widthGrades
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
        imgNewRecord.layoutParams.height = heigthGrades

        val txNameSong = findViewById<TextView>(R.id.txNameSong)
        val txNameChannel = findViewById<TextView>(R.id.txNameChannel)
        val txStepMaker = findViewById<TextView>(R.id.txStepMaker)

        txNameSong.background = Drawable.createFromPath("$rutaGrades/Evaluation_Top label name.png")
        txNameChannel.background = Drawable.createFromPath("$rutaGrades/Evaluation_Top label channel.png")

        txStepMaker.text = if(playerSong.stepMaker != "") "STEPMAKER: ${playerSong.stepMaker}" else ""

        txNameSong.apply {
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setTypeface(typeface, Typeface.BOLD)
            setShadowLayer(1.6f, 1.5f, 1.3f, Color.BLACK)
            layoutParams.height = sizeLabels
        }

        txNameChannel.apply {
            textAlignment = TextView.TEXT_ALIGNMENT_CENTER
            setTextColor(ContextCompat.getColor(context, R.color.white))
            setTypeface(typeface, Typeface.BOLD)
            setShadowLayer(1.6f, 1.5f, 1.3f, Color.BLACK)
            layoutParams.height = sizeLabels
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
        countMiss
        resultSong.miss // += countMiss
        val totalNotes = resultSong.perfect + resultSong.great + resultSong.good + resultSong.bad + resultSong.miss
        val noteWeighs = perfect + great + good + bad + miss
        val rawScore = ((((0.995 * noteWeighs) + (0.005 * resultSong.maxCombo)) / totalNotes) * 1000000)
        totalScore = if (rawScore > 999998) 1000000 else rawScore.roundToInt()

        val bitNR = BitmapFactory.decodeFile("$rutaGrades/new_record.png")
        imgNewRecord.setImageBitmap(bitNR)
        if(!isOnline){
            imgMyBestScore.setImageBitmap(BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/score_body_dance_grade.png").toString()))
            showGrade()
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
                salaRef.child("jugador1/result").setValue(activeSala.jugador1.result)/*.addOnSuccessListener {
                    getWinner(imgWinP1, imgWinP2, imgDraw, txNameChannel)
                }
                */
            }else{
                activeSala.jugador2.result.perfect = resultSong.perfect.toString()
                activeSala.jugador2.result.great = resultSong.great.toString()
                activeSala.jugador2.result.good = resultSong.good.toString()
                activeSala.jugador2.result.bad = resultSong.bad.toString()
                activeSala.jugador2.result.miss = resultSong.miss.toString()
                activeSala.jugador2.result.maxCombo = resultSong.maxCombo.toString()
                activeSala.jugador2.result.score = totalScore.toString()

                salaRef.child("jugador2/result").setValue(activeSala.jugador2.result)/*.addOnSuccessListener{
                    getWinner(imgWinP1, imgWinP2, imgDraw, txNameChannel)
                }
                */
            }
        }

        if(!isOnline) {
            val posicion = currentWorldScore.indexOfFirst{ totalScore > it.toInt() }
            if(!isOffline){
                if (posicion != -1) {
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

        //mediaPlayerEvaluation = MediaPlayer.create(this, Uri.fromFile(File(getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/Evaluation.mp3")!!.absolutePath)))

        mediaPlayerEvaluation = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setDataSource(getExternalFilesDir("/FingerDance/Themes/$tema/Sounds/Evaluation.mp3")!!.absolutePath)
            prepare()
            start()
        }


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
            imageViews[i].layoutParams.height = sizeLabels

            textViewsP1[i].apply {
                setTextColor(Color.WHITE)
                isAllCaps = true
                setShadowLayer(1.6f, 1.5f, 1.3f, Color.BLACK)
                layoutParams.height = (decimoHeigtn / 3.6).toInt()
            }
            textViewsP2[i].apply {
                setTextColor(Color.WHITE)
                isAllCaps = true
                setShadowLayer(1.6f, 1.5f, 1.3f, Color.BLACK)
                layoutParams.height = (decimoHeigtn / 3.6).toInt()
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
                        if(isPlayer1){
                            if(activeSala.jugador1.result.score.toInt() > activeSala.jugador2.result.score.toInt()){
                                winP1 = true
                                victoriesP1 ++
                            }else if(activeSala.jugador1.result.score.toInt() < activeSala.jugador2.result.score.toInt()){
                                winP2 = true
                                victoriesP2 ++
                            }else{
                                draw = true
                            }
                        }else{
                            if(activeSala.jugador2.result.score.toInt() > activeSala.jugador1.result.score.toInt()){
                                winP2 = true
                                victoriesP2 ++
                            }else if(activeSala.jugador2.result.score.toInt() < activeSala.jugador1.result.score.toInt()){
                                winP1 = true
                                victoriesP1 ++
                            }else{
                                draw = true
                            }
                        }
                        showGradeP1(activeSala.jugador1.result.score.toInt())
                        showGradeP2(activeSala.jugador2.result.score.toInt())
                        getWinner(imgGradeP1, imgGradeP2, imgWinP1, imgWinP2, imgDraw, txNameChannel)
                    }
                }, 1000)
            }
        },1000L)
    }

    private fun getWinner(imgGradeP1: ImageView, imgGradeP2: ImageView, imgWinP1: ImageView, imgWinP2: ImageView, imgDraw: ImageView, txNameChannel: TextView){
        handlerDG.postDelayed({
            txNameChannel.text = getString(R.string.victories_text, victoriesP1.toString(), victoriesP2.toString())
            imgGradeP1.setImageBitmap(gradeP1)
            animateImageViewP1(imgGradeP1)

            imgGradeP2.setImageBitmap(gradeP2)
            animateImageViewP2(imgGradeP2)
            handlerDG.postDelayed({
                if(winP1)imgWinP1.setImageBitmap(BitmapFactory.decodeFile(rutaWin))
                if(winP2)imgWinP2.setImageBitmap(BitmapFactory.decodeFile(rutaWin))
                if(draw)imgDraw.setImageBitmap(BitmapFactory.decodeFile(rutaDraw))
            }, 250)
            soundPoolSelectSongKsf.play(rank_sound, 1.0f, 1.0f, 1, 0, 1.0f)
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
        val databaseRef = firebaseDatabase!!.getReference("channels")
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
                                    if (nivel == level) {
                                        val rankList = mutableListOf<Map<String, Any>>()
                                        for (rankSnapshot in nivelSnapshot.child("fisrtRank").children) {
                                            val nombre = rankSnapshot.child("nombre").getValue(String::class.java) ?: ""
                                            val puntaje = rankSnapshot.child("puntaje").getValue(String::class.java)?.toIntOrNull() ?: 0
                                            val gradeValue = rankSnapshot.child("grade").getValue(String::class.java) ?: ""

                                            rankList.add(
                                                mapOf(
                                                    "nombre" to nombre,
                                                    "puntaje" to puntaje,
                                                    "grade" to gradeValue
                                                )
                                            )
                                        }
                                        rankList.add(
                                            mapOf(
                                                "nombre" to namePlayer,
                                                "puntaje" to newScore.toInt(),
                                                "grade" to grade
                                            )
                                        )

                                        val nuevosTop3 = rankList.sortedByDescending { it["puntaje"] as Int }.take(3)

                                        val nivelRef = databaseRef.child(canalSnapshot.key!!)
                                            .child("canciones").child(cancionSnapshot.key!!)
                                            .child("niveles").child(index.toString())
                                            .child("fisrtRank")
                                        val nuevosTop3Strings = nuevosTop3.map { rank ->
                                            mapOf(
                                                "nombre" to rank["nombre"],
                                                "puntaje" to rank["puntaje"].toString(),
                                                "grade" to rank["grade"]
                                            )
                                        }
                                        nivelRef.setValue(nuevosTop3Strings)
                                            .addOnSuccessListener {
                                                showNewRecord(imgNewRecord)
                                            }

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
                if(!isOffline){
                    escucharPuntajesPorNombre(currentChannel) { listaCanciones ->
                        listGlobalRanking = listaCanciones
                    }
                }
            }
            mediaPlayerEvaluation.stop()
            soundPoolSelectSongKsf.stop(isPlayingRankA)
            soundPoolSelectSongKsf.stop(isPlayingRankB)
            soundPoolSelectSongKsf.stop(isPlayingNewRecord)
            img_wait.isVisible = true
            bg_wait.isVisible = true
            countMiss = 0
            handlerDG.postDelayed({
                getSelectChannel = true
                this.finish()
            }, 2500)

        }
        imgFloor.setOnLongClickListener {
            imgAceptar.performClick()
        }
    }

    private fun escucharPuntajesPorNombre(canalNombre: String, callback: (ArrayList<Cancion>) -> Unit) {
        val databaseRef = firebaseDatabase!!.getReference("channels")
        val listResult = arrayListOf<Cancion>()
        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (canalSnapshot in snapshot.children) {
                    val canal = canalSnapshot.child("canal").getValue(String::class.java)
                    if (canal == canalNombre) {
                        for (cancionSnapshot in canalSnapshot.child("canciones").children) {
                            val nombreCancion = cancionSnapshot.child("cancion").getValue(String::class.java) ?: ""
                            val niveles = arrayListOf<Nivel>()
                            for (nivelesSnapshot in cancionSnapshot.child("niveles").children) {
                                val numberNivel = nivelesSnapshot.child("nivel").getValue(String::class.java) ?: ""
                                val rankings = arrayListOf<FirstRank>()
                                for(rankingSnapshot in  nivelesSnapshot.child("fisrtRank").children){
                                    val nombre = rankingSnapshot.child("nombre").getValue(String::class.java) ?: ""
                                    val puntaje = rankingSnapshot.child("puntaje").getValue(String::class.java) ?: "0"
                                    val grade = rankingSnapshot.child("grade").getValue(String::class.java) ?: ""
                                    rankings.add(FirstRank(nombre, puntaje, grade))
                                }
                                niveles.add(Nivel(numberNivel, rankings))
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
        val heightImages = (decimoHeigtn / 2).toInt()

        linearEfects.layoutParams.width = (medidaFlechas * 4).toInt()
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
        val bitActiveLvHD = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/lv_active_hd.png")!!.absolutePath)
        val imgBitActive = ImageView(this)
        if(halfDouble){
            imgBitActive.setImageBitmap(bitActiveLvHD)
        }else {
            imgBitActive.setImageBitmap(bitActiveLv)
        }
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

    private fun showGrade() {
        when {
            totalScore >= 999999 -> { // SSS+
                grade = arrayGrades[0]
                soundGrade = 1
                newGrade = "SSS+"
            }
            totalScore in 990000..999998 -> { // SSS
                grade = arrayGrades[1]
                soundGrade = 1
                newGrade = "SSS"
            }
            totalScore in 985000..989999 -> { // SS+
                grade = arrayGrades[2]
                soundGrade = 2
                newGrade = "SS+"
            }
            totalScore in 980000..984999 -> { // SS
                grade = arrayGrades[3]
                soundGrade = 2
                newGrade = "SS"
            }
            totalScore in 975000..979999 -> { // S+
                grade = arrayGrades[4]
                soundGrade = 3
                newGrade = "S+"
            }
            totalScore in 970000..974999 -> { // S
                grade = arrayGrades[5]
                soundGrade = 3
                newGrade = "S"
            }
            totalScore in 960000..969999 -> { // AAA+
                grade = arrayGrades[6]
                soundGrade = 4
                newGrade = "AAA+"
            }
            totalScore in 950000..959999 -> { // AAA
                grade = arrayGrades[7]
                soundGrade = 4
                newGrade = "AAA"
            }
            totalScore in 925000..949999 -> { // AA+
                grade = arrayGrades[8]
                soundGrade = 4
                newGrade = "AA+"
            }
            totalScore in 900000..924999 -> { // AA
                grade = arrayGrades[9]
                soundGrade = 4
                newGrade = "AA"
            }
            totalScore in 825000..899999 -> { // A+
                grade = arrayGrades[10]
                soundGrade = 4
                newGrade = "A+"
            }
            totalScore in 750000..824999 -> { // A
                grade = arrayGrades[11]
                soundGrade = 4
                newGrade = "A"
            }
            totalScore in 650000..749999 -> { // B
                grade = arrayGrades[12]
                soundGrade = 5
                newGrade = "B"
            }
            totalScore in 550000..649999 -> { // C
                grade = arrayGrades[13]
                soundGrade = 6
                newGrade = "C"
            }
            totalScore in 450000..549999 -> { // D
                grade = arrayGrades[14]
                soundGrade = 7
                newGrade = "D"
            }
            else -> { // F
                grade = arrayGrades[15]
                soundGrade = 8
                newGrade = "F"
            }
        }
    }

    private fun showGradeP1(totalScoreP1: Int) {
        gradeP1 = when {
            totalScoreP1 >= 999999 -> arrayGrades[0]               // SSS+
            totalScoreP1 in 990000..999998 -> arrayGrades[1] // SSS
            totalScoreP1 in 985000..989999 -> arrayGrades[2] // SS+
            totalScoreP1 in 980000..984999 -> arrayGrades[3] // SS
            totalScoreP1 in 975000..979999 -> arrayGrades[4] // S+
            totalScoreP1 in 970000..974999 -> arrayGrades[5] // S
            totalScoreP1 in 960000..969999 -> arrayGrades[6] // AAA+
            totalScoreP1 in 950000..959999 -> arrayGrades[7] // AAA
            totalScoreP1 in 925000..949999 -> arrayGrades[8] // AA+
            totalScoreP1 in 900000..924999 -> arrayGrades[9] // AA
            totalScoreP1 in 825000..899999 -> arrayGrades[10] // A+
            totalScoreP1 in 750000..824999 -> arrayGrades[11] // A
            totalScoreP1 in 650000..749999 -> arrayGrades[12] // B
            totalScoreP1 in 550000..649999 -> arrayGrades[13] // C
            totalScoreP1 in 450000..549999 -> arrayGrades[14] // D
            else -> arrayGrades[15]                                 // F
        }
    }

    private fun showGradeP2(totalScoreP2: Int) {
        gradeP2 = when {
            totalScoreP2 >= 999999 -> arrayGrades[0]               // SSS+
            totalScoreP2 in 990000..999998 -> arrayGrades[1] // SSS
            totalScoreP2 in 985000..989999 -> arrayGrades[2] // SS+
            totalScoreP2 in 980000..984999 -> arrayGrades[3] // SS
            totalScoreP2 in 975000..979999 -> arrayGrades[4] // S+
            totalScoreP2 in 970000..974999 -> arrayGrades[5] // S
            totalScoreP2 in 960000..969999 -> arrayGrades[6] // AAA+
            totalScoreP2 in 950000..959999 -> arrayGrades[7] // AAA
            totalScoreP2 in 925000..949999 -> arrayGrades[8] // AA+
            totalScoreP2 in 900000..924999 -> arrayGrades[9] // AA
            totalScoreP2 in 825000..899999 -> arrayGrades[10] // A+
            totalScoreP2 in 750000..824999 -> arrayGrades[11] // A
            totalScoreP2 in 650000..749999 -> arrayGrades[12] // B
            totalScoreP2 in 550000..649999 -> arrayGrades[13] // C
            totalScoreP2 in 450000..549999 -> arrayGrades[14] // D
            else -> arrayGrades[15]                                 // F
        }
    }

    private fun showNewRecord(imgNewRecord: ImageView) {
        handlerDG.postDelayed({
            imgNewRecord.visibility = View.VISIBLE
            imgNewRecord.startAnimation(AnimationUtils.loadAnimation(DGContext, R.anim.stamp_effect))
            isPlayingNewRecord = soundPoolSelectSongKsf.play(new_record, 1.0f, 1.0f, 1, 0, 1.0f)
        }, 3500)
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
        handlerDG.removeCallbacksAndMessages(null)
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