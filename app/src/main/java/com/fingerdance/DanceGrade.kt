package com.fingerdance

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
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
import com.google.firebase.database.DatabaseReference
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
import androidx.core.graphics.drawable.toDrawable


private lateinit var bgConstraint: ConstraintLayout
private lateinit var mediaPlayerEvaluation: MediaPlayer
private lateinit var grade: Bitmap
private lateinit var gradeDescription: Bitmap

private lateinit var gradeP1: Bitmap
private lateinit var gradeP2: Bitmap

private var totalScore = 0
private var soundGrade = 0
private var newGrade = ""
private var rankA = 0
private var rankB = 0

private var isPlayingRankA = 0
private var isPlayingRankB = 0
private var isPlayingNewRecord = 0

private val handlerDG = Handler(Looper.getMainLooper())
private val sizeLabels = (decimoHeigtn / 3.8).toInt()
private var rutaWin = ""
private var rutaDraw = ""

class DanceGrade : AppCompatActivity() {
    private lateinit var DGContext: Context
    private lateinit var linearEfects: RelativeLayout
    private var winP1 = false
    private var winP2 = false
    private var draw = false
    private var isPoolLoaded = false
    private lateinit var imgAceptar: ImageView
    private lateinit var imgFloor: ImageView

    private lateinit var checkedValuesFirebase: Nivel
    private var enabledSaveScore = false
    private var rankList = mutableListOf<Map<String, Any>>()
    private lateinit var rankRef: DatabaseReference
    //private var firebaseDatabase: FirebaseDatabase? = null

    private lateinit var imgGradeDescription: ImageView
    private var resultListener: ValueEventListener? = null
    private var nivelChkValues = Nivel()

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

        imgAceptar = findViewById(R.id.aceptDG)
        imgAceptar.visibility = View.INVISIBLE
        imgAceptar.layoutParams.width = (width / 2.5).toInt()

        imgFloor = findViewById<ImageView>(R.id.floorDG)
        imgFloor.visibility = View.INVISIBLE

        val bmFloor = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/floor.png")!!.absolutePath)
        imgFloor.setImageBitmap(bmFloor)

        val bm = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/press_floor.png")!!.absolutePath)
        imgAceptar.setImageBitmap(bm)

        val bgWait = findViewById<ConstraintLayout>(R.id.bg_wait)
        val imgWait = findViewById<ImageView>(R.id.img_wait)
        bgWait.isVisible = false
        imgWait.isVisible = false
        bgWait.setOnClickListener { /* Prevent clicks */ }

        val numberWait = Random.nextInt(1, 10)
        imgWait.setImageBitmap(BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/dance_grade/img_dance_grade ($numberWait).png")!!.absolutePath))

        soundPoolSelectSongKsf.setOnLoadCompleteListener { _, _, _ ->
            isPoolLoaded = true
        }

        if(currentChannel == "06-FAVORITES"){
            nivelChkValues = mockListChannels[channelIndex].canciones[songIndex].niveles
                .find { it.nivel == currentLevel && it.type == playerSong.type && it.player == playerSong.player }!!

        }else {
            val channelMock = mockListChannels.find { it.canal.equals(currentChannel, ignoreCase = true) }
            if (channelMock != null) {
                val songChannel = channelMock.canciones.find { it.cancion.equals(currentSong, ignoreCase = true) }
                if (songChannel != null) {
                    nivelChkValues = songChannel.niveles.find { it.nivel == currentLevel && it.type == playerSong.type && it.player == playerSong.player } ?: Nivel()
                }
            }
        }

        resolveInitialFlow()

        if (isOnline) {
            if (isPlayer1) {
                txPlayer1.text = userName
                txPlayer2.text = activeSala.jugador2.id
            } else {
                txPlayer1.text = activeSala.jugador1.id
                txPlayer2.text = userName
            }
        } else {
            txPlayer1.visibility = View.GONE
            txPlayer2.visibility = View.GONE
        }

        val bgBanner: ImageView = findViewById(R.id.bgImage)
        val bitmap = BitmapFactory.decodeFile(playerSong.rutaBanner)
        val bitmapDrawable = bitmap.toDrawable(resources)
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
        imgGrade.layoutParams.height = (decimoHeigtn * 1.2).toInt()

        imgGradeDescription = findViewById(R.id.imgGradeDescription)
        imgGradeDescription.visibility = View.INVISIBLE
        imgGradeDescription.layoutParams.height = (medidaFlechas / 2).toInt()

        val imgGradeP1 = findViewById<ImageView>(R.id.imgGradeP1)
        imgGradeP1.visibility = View.INVISIBLE
        imgGradeP1.layoutParams.width = widthGrades
        imgGradeP1.layoutParams.height = (decimoHeigtn * 1.2).toInt()

        val imgGradeP2 = findViewById<ImageView>(R.id.imgGradeP2)
        imgGradeP2.visibility = View.INVISIBLE
        imgGradeP2.layoutParams.width = widthGrades
        imgGradeP2.layoutParams.height = (decimoHeigtn * 1.2).toInt()

        val imgWinP1 = findViewById<ImageView>(R.id.imgWinP1)
        imgWinP1.layoutParams.width = ((width / 10) * 4.5).toInt()

        val imgWinP2 = findViewById<ImageView>(R.id.imgWinP2)
        imgWinP2.layoutParams.width = ((width / 10) * 4.5).toInt()

        val imgDraw = findViewById<ImageView>(R.id.imgDraw)
        imgDraw.layoutParams.width = ((width / 10) * 4.5).toInt()

        val imgMyBestScore = findViewById<ImageView>(R.id.imgBestScoreDG)
        val imgMyBestGrade = findViewById<ImageView>(R.id.imgBestGradeDG)
        val lbBestScoreDG = findViewById<TextView>(R.id.lbBestScoreDG)

        imgMyBestScore.post {
            val newHeight = (imgMyBestScore.height * 0.6).toInt()
            val params = imgMyBestGrade.layoutParams
            params.height = newHeight
            imgMyBestGrade.layoutParams = params
        }


        val imgNewRecord = findViewById<ImageView>(R.id.imgNewRecord)
        imgNewRecord.visibility = View.INVISIBLE
        imgNewRecord.layoutParams.height = heigthGrades

        val txNameSong = findViewById<TextView>(R.id.txNameSong)
        val txNameChannel = findViewById<TextView>(R.id.txNameChannel)
        val txStepMaker = findViewById<TextView>(R.id.txStepMaker)

        txNameSong.background = Drawable.createFromPath("$rutaGrades/Evaluation_Top label name.png")
        txNameChannel.background = Drawable.createFromPath("$rutaGrades/Evaluation_Top label channel.png")

        txStepMaker.text = if (playerSong.stepMaker != "") "STEPMAKER: ${playerSong.stepMaker}" else ""

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
        val totalNotes = resultSong.perfect + resultSong.great + resultSong.good + resultSong.bad + resultSong.miss
        val noteWeighs = perfect + great + good + bad + miss
        val rawScore = ((((0.995 * noteWeighs) + (0.005 * resultSong.maxCombo)) / maxOf(1, totalNotes)) * 1000000)
        totalScore = if (rawScore > 999998) 1000000 else rawScore.roundToInt()
        if (totalScore == 1) {
            totalScore = 0
        }

        val bitNR = BitmapFactory.decodeFile("$rutaGrades/new_record.png")
        imgNewRecord.setImageBitmap(bitNR)

        showGrade()

        if (!isOnline) {
            imgMyBestScore.setImageBitmap(BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/score_body_dance_grade.png").toString()))
            txNameSong.text = currentSong
            txNameChannel.text = if (currentChannel.contains("-")) currentChannel.split("-")[1] else currentChannel
        } else {
            // Online mode - save results to Firebase
            txNameSong.text = activeSala.cancion.nameSong
            if (isPlayer1) {
                activeSala.jugador1.result.perfect = resultSong.perfect.toString()
                activeSala.jugador1.result.great = resultSong.great.toString()
                activeSala.jugador1.result.good = resultSong.good.toString()
                activeSala.jugador1.result.bad = resultSong.bad.toString()
                activeSala.jugador1.result.miss = resultSong.miss.toString()
                activeSala.jugador1.result.maxCombo = resultSong.maxCombo.toString()
                activeSala.jugador1.result.score = totalScore.toString()
                salaRef.child("jugador1/result").setValue(activeSala.jugador1.result)
            } else {
                activeSala.jugador2.result.perfect = resultSong.perfect.toString()
                activeSala.jugador2.result.great = resultSong.great.toString()
                activeSala.jugador2.result.good = resultSong.good.toString()
                activeSala.jugador2.result.bad = resultSong.bad.toString()
                activeSala.jugador2.result.miss = resultSong.miss.toString()
                activeSala.jugador2.result.maxCombo = resultSong.maxCombo.toString()
                activeSala.jugador2.result.score = totalScore.toString()
                salaRef.child("jugador2/result").setValue(activeSala.jugador2.result)
            }
        }

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
            textViewsP1[i].layoutParams.width = (width * 0.2).toInt()
            textViewsP2[i].layoutParams.width = (width * 0.2).toInt()
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
                // Animate bars
                val jobs = mutableListOf<Job>()
                for (bar in imageViews) {
                    jobs.add(animateBar(bar))
                    delay(200)
                }
                jobs.joinAll()

                if (!isOnline) {
                    // Solo/Offline mode
                    setCountsP1(textViewsP1)
                    handlerDG.postDelayed({
                        showGradeAnimation(imgGrade)
                        handleOfflineResult(imgMyBestGrade, lbBestScoreDG, imgNewRecord, dbDG)
                    }, 1000)
                } else {
                    // Online mode - wait for both players
                    activeSala.readyToResult = true
                    salaRef.child("readyToResult").setValue(activeSala.readyToResult).addOnSuccessListener {
                        listenForBothPlayersReady(
                            textViewsP1,
                            textViewsP2,
                            imgGradeP1,
                            imgGradeP2,
                            imgWinP1,
                            imgWinP2,
                            imgDraw,
                            txNameChannel
                        )
                    }
                }
            }
        }, 1000L)

        imgAceptar.setOnClickListener {
            handleAcceptClick(bgWait, imgWait)
        }

        imgFloor.setOnLongClickListener {
            imgAceptar.performClick()
            true
        }
    }

    private fun resolveInitialFlow() {
        if (isOffline || isOnline) {
            enabledSaveScore = false
            return
        }

        if (!isOficialSong) {
            enabledSaveScore = true
            return
        }

        getFirstRankFromFirebase()
    }

    private fun getFirstRankFromFirebase() {

        if(nivelChkValues.checkedValues.trim() != checkedValues.trim()){
            enabledSaveScore = false
            return
        }
        //validateCheckedValues()

        val nivelRef = firebaseDatabase!!
            .getReference("channels")
            .child(channelIndex.toString())
            .child("canciones")
            .child(songIndex.toString())
            .child("niveles")

        nivelRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var nivelEncontrado: DataSnapshot? = null

                for (nivelSnap in snapshot.children) {
                    val nivel = nivelSnap.child("nivel").getValue(String::class.java)
                    val type = nivelSnap.child("type").getValue(String::class.java)
                    val player = nivelSnap.child("player").getValue(String::class.java)

                    if (
                        nivel == currentLevel &&
                        type == playerSong.type &&
                        player == playerSong.player
                    ) {
                        nivelEncontrado = nivelSnap
                        break
                    }
                }

                if (nivelEncontrado == null) {
                    enabledSaveScore = false
                    return
                }

                checkedValuesFirebase = nivelEncontrado.getValue(Nivel::class.java)!!
                rankRef = nivelEncontrado.ref
                getRankList()
            }

            override fun onCancelled(error: DatabaseError) {
                enabledSaveScore = false
            }
        })
    }

    private fun getRankList() {
        enabledSaveScore = true

        rankList.clear()
        for (rank in checkedValuesFirebase.fisrtRank) {
            rankList.add(
                mapOf(
                    "nombre" to rank.nombre,
                    "puntaje" to rank.puntaje.toInt(),
                    "grade" to rank.grade
                )
            )
        }
    }

    private fun handleOfflineResult(
        imgMyBestGrade: ImageView,
        lbBestScoreDG: TextView,
        imgNewRecord: ImageView,
        dbDG: DataBasePlayer,
    ) {
        when (resolveSaveScenario()) {
            SaveResult.NONE -> {
                // Mostrar score existente
                getBestScore(imgMyBestGrade, lbBestScoreDG)
                getBtnAceptar()
            }

            SaveResult.LOCAL -> {
                // Guardar solo en local
                saveLocalScore(dbDG, imgMyBestGrade, lbBestScoreDG)
                getBtnAceptar()
            }

            SaveResult.LOCAL_AND_FIREBASE -> {
                // Guardar en local y Firebase
                saveLocalScore(dbDG, imgMyBestGrade, lbBestScoreDG)
                updateFirebaseRanking(imgNewRecord)
            }

            SaveResult.INVALID_LEVEL -> {
                // Nivel modificado
                getBestScore(imgMyBestGrade, lbBestScoreDG)
                showInvalidLevelWarning()
                getBtnAceptar()
            }
        }
    }

    private fun resolveSaveScenario(): SaveResult {
        // Offline u Online → No guardar
        if (isOffline || isOnline) return SaveResult.NONE

        // No oficial → solo local si supera puntaje
        if (!isOficialSong) {
            return if (totalScore > currentScore.toInt())
                SaveResult.LOCAL
            else
                SaveResult.NONE
        }

        // Oficial pero nivel modificado
        if (!enabledSaveScore) {
            return SaveResult.INVALID_LEVEL
        }

        // No supera el puntaje actual
        if (totalScore <= currentScore.toInt()) {
            return SaveResult.NONE
        }

        // Verificar si entra al ranking
        val entraRanking = rankList.any {
            totalScore > (it["puntaje"] as Int)
        }

        return if (entraRanking)
            SaveResult.LOCAL_AND_FIREBASE
        else
            SaveResult.LOCAL
    }

    private fun saveLocalScore(
        dbDG: DataBasePlayer,
        imgMyBestGrade: ImageView,
        lbBestScoreDG: TextView
    ) {
        var nameChannels = if (currentChannel == "06-FAVORITES") {
            listSongsChannelKsf.find { it.title == currentSong }?.channel.toString()
        } else {
            currentChannel
        }

        dbDG.updatePuntaje(
            canal = nameChannels,
            cancion = currentSong,
            nivel = currentLevel,
            type = playerSong.type,
            player = playerSong.player,
            nuevoPuntaje = totalScore.toString(),
            nuevoGrade = newGrade
        )

        imgMyBestGrade.setImageBitmap(grade)
        lbBestScoreDG.text = totalScore.toString()
    }

    private fun updateFirebaseRanking(imgNewRecord: ImageView) {
        handlerDG.postDelayed({
            rankList.add(
                mapOf(
                    "nombre" to userName,
                    "puntaje" to totalScore,
                    "grade" to newGrade
                )
            )

            val nuevosTop3 = rankList.sortedByDescending { it["puntaje"] as Int }.take(3)
            val nuevosTop3Strings = nuevosTop3.map { rank ->
                mapOf(
                    "nombre" to rank["nombre"],
                    "puntaje" to rank["puntaje"].toString(),
                    "grade" to rank["grade"]
                )
            }

            rankRef.child("fisrtRank").setValue(nuevosTop3Strings)
                .addOnSuccessListener {
                    showNewRecord(imgNewRecord)
                }
                .addOnFailureListener { e ->
                    Log.e("Firebase", "Error al actualizar ranking: ", e)
                    getBtnAceptar()
                }
        }, 1500L)
    }

    private fun showInvalidLevelWarning() {
        val warningMessage = getString(R.string.warning_message, userName)
        AlertDialog.Builder(this@DanceGrade)
            .setTitle("Nivel modificado")
            .setMessage(warningMessage)
            .setPositiveButton("Entiendo") { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()

        saveBannedDeviceInfo()
    }

    private fun saveBannedDeviceInfo() {
        try {
            // Limpiar userName y deviceIdFind: solo caracteres alfanuméricos
            val cleanUserName = userName.replace(Regex("[^a-zA-Z0-9]"), "")
            val key = "$cleanUserName-$deviceIdFind"
            val calendar = java.util.Calendar.getInstance()
            val sdf = java.text.SimpleDateFormat("dd-MMM-yyyy HH:mm", java.util.Locale("es", "ES"))
            val fechaHora = sdf.format(calendar.time)

            val banAttempt = mapOf(
                "canal" to currentChannel,
                "cancion" to currentSong,
                "nivel" to currentLevel,
                "checkedValuesLocal" to checkedValues,
                "checkedValuesFirebase" to nivelChkValues.checkedValues,
                "fecha" to fechaHora
            )

            firebaseDatabase!!
                .getReference("banDevices")
                .child(key)
                .get()
                .addOnSuccessListener { snapshot ->
                    val existingList = if (snapshot.exists()) {
                        val currentData = snapshot.value as? List<Map<String, Any>> ?: emptyList()
                        currentData.toMutableList()
                    } else {
                        mutableListOf()
                    }

                    existingList.add(banAttempt)

                    firebaseDatabase!!
                        .getReference("banDevices")
                        .child(key)
                        .setValue(existingList)
                        .addOnSuccessListener {
                            Log.d("Firebase", "Intento de hack guardado: $key (Total: ${existingList.size})")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firebase", "Error al guardar intento de hack: ", e)
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("Firebase", "Error al leer datos previos: ", e)
                }
        } catch (e: Exception) {
            Log.e("Firebase", "Error en saveBannedDeviceInfo: ", e)
        }
    }

    private fun showGradeAnimation(imgGrade: ImageView) {
        imgGrade.setImageBitmap(grade)
        imgGradeDescription.setImageBitmap(gradeDescription)
        animateImageView(imgGrade)
        soundPoolSelectSongKsf.play(rank_sound, 1.0f, 1.0f, 1, 0, 1.0f)
        rankA = getRankSound(soundGrade)
        rankB = getRankSoundB(soundGrade)
        isPlayingRankA = soundPoolSelectSongKsf.play(rankA, 1.0f, 1.0f, 1, 0, 1.0f)
        isPlayingRankB = soundPoolSelectSongKsf.play(rankB, 1.0f, 1.0f, 1, 0, 1.0f)
    }

    private fun listenForBothPlayersReady(
        textViewsP1: List<TextView>,
        textViewsP2: List<TextView>,
        imgGradeP1: ImageView,
        imgGradeP2: ImageView,
        imgWinP1: ImageView,
        imgWinP2: ImageView,
        imgDraw: ImageView,
        txNameChannel: TextView,
    ) {
        resultListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val player1Ready = snapshot.child("jugador1/result/score").exists()
                val player2Ready = snapshot.child("jugador2/result/score").exists()

                if (player1Ready && player2Ready) {
                    // Both players ready, remove listener
                    resultListener?.let { salaRef.removeEventListener(it) }
                    resultListener = null

                    // Update local activeSala with latest data
                    activeSala.jugador1.result.apply {
                        perfect = snapshot.child("jugador1/result/perfect").getValue(String::class.java) ?: "0"
                        great = snapshot.child("jugador1/result/great").getValue(String::class.java) ?: "0"
                        good = snapshot.child("jugador1/result/good").getValue(String::class.java) ?: "0"
                        bad = snapshot.child("jugador1/result/bad").getValue(String::class.java) ?: "0"
                        miss = snapshot.child("jugador1/result/miss").getValue(String::class.java) ?: "0"
                        maxCombo = snapshot.child("jugador1/result/maxCombo").getValue(String::class.java) ?: "0"
                        score = snapshot.child("jugador1/result/score").getValue(String::class.java) ?: "0"
                    }
                    activeSala.jugador2.result.apply {
                        perfect = snapshot.child("jugador2/result/perfect").getValue(String::class.java) ?: "0"
                        great = snapshot.child("jugador2/result/great").getValue(String::class.java) ?: "0"
                        good = snapshot.child("jugador2/result/good").getValue(String::class.java) ?: "0"
                        bad = snapshot.child("jugador2/result/bad").getValue(String::class.java) ?: "0"
                        miss = snapshot.child("jugador2/result/miss").getValue(String::class.java) ?: "0"
                        maxCombo = snapshot.child("jugador2/result/maxCombo").getValue(String::class.java) ?: "0"
                        score = snapshot.child("jugador2/result/score").getValue(String::class.java) ?: "0"
                    }

                    CoroutineScope(Dispatchers.Main).launch {
                        setCountsP1(textViewsP1)
                        setCountsP2(textViewsP2)

                        handlerDG.postDelayed({
                            determineWinner()
                            showGradeP1(activeSala.jugador1.result.score.toInt())
                            showGradeP2(activeSala.jugador2.result.score.toInt())
                            getWinner(imgGradeP1, imgGradeP2, imgWinP1, imgWinP2, imgDraw, txNameChannel)
                            getBtnAceptar()
                        }, 1000)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error listening for results: ${error.message}")
                getBtnAceptar()
            }
        }
        salaRef.addValueEventListener(resultListener!!)
    }

    private fun determineWinner() {
        val scoreP1 = activeSala.jugador1.result.score.toIntOrNull() ?: 0
        val scoreP2 = activeSala.jugador2.result.score.toIntOrNull() ?: 0

        if (isPlayer1) {
            when {
                scoreP1 > scoreP2 -> {
                    winP1 = true
                    victoriesP1++
                }
                scoreP1 < scoreP2 -> {
                    winP2 = true
                    victoriesP2++
                }
                else -> {
                    draw = true
                }
            }
        } else {
            when {
                scoreP2 > scoreP1 -> {
                    winP1 = true
                    victoriesP1++
                }
                scoreP2 < scoreP1 -> {
                    winP2 = true
                    victoriesP2++
                }
                else -> {
                    draw = true
                }
            }
        }
    }

    private fun handleAcceptClick(bgWait: ConstraintLayout, imgWait: ImageView) {
        soundPoolSelectSongKsf.play(startKsf, 1.0f, 1.0f, 1, 0, 1.0f)

        if (!isOnline && !isOffline) {
            if (currentChannel != "06-FAVORITES") {
                listenScoreChannel(currentChannel) { listaCanciones ->
                    listGlobalRanking = listaCanciones
                }
            } else {
                listGlobalRanking.clear()
            }
        }

        mediaPlayerEvaluation.stop()
        soundPoolSelectSongKsf.stop(isPlayingRankA)
        soundPoolSelectSongKsf.stop(isPlayingRankB)
        soundPoolSelectSongKsf.stop(isPlayingNewRecord)

        imgWait.isVisible = true
        bgWait.isVisible = true
        countMiss = 0

        handlerDG.postDelayed({
            getSelectChannel = true
            this.finish()
        }, 3500)
    }

    private fun getWinner(
        imgGradeP1: ImageView,
        imgGradeP2: ImageView,
        imgWinP1: ImageView,
        imgWinP2: ImageView,
        imgDraw: ImageView,
        txNameChannel: TextView,
    ) {
        handlerDG.postDelayed({
            txNameChannel.text = getString(R.string.victories_text, victoriesP1.toString(), victoriesP2.toString())
            imgGradeP1.setImageBitmap(gradeP1)
            animateImageViewP1(imgGradeP1)

            imgGradeP2.setImageBitmap(gradeP2)
            animateImageViewP2(imgGradeP2)

            handlerDG.postDelayed({
                if (winP1) imgWinP1.setImageBitmap(BitmapFactory.decodeFile(rutaWin))
                if (winP2) imgWinP2.setImageBitmap(BitmapFactory.decodeFile(rutaWin))
                if (draw) imgDraw.setImageBitmap(BitmapFactory.decodeFile(rutaDraw))
            }, 250)
            soundPoolSelectSongKsf.play(rank_sound, 1.0f, 1.0f, 1, 0, 1.0f)
        }, 500L)
    }


    private fun listenScoreChannel(canalNombre: String, callback: (ArrayList<Cancion>) -> Unit) {
        val canalRef = firebaseDatabase!!.getReference("channels").orderByChild("canal").equalTo(canalNombre)
        val listResult = arrayListOf<Cancion>()
        canalRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (canalSnapshot in snapshot.children) {
                    val cancionesSnapshot = canalSnapshot.child("canciones")
                    for (cancionSnapshot in cancionesSnapshot.children) {
                        val nombreCancion = cancionSnapshot.child("cancion").getValue(String::class.java) ?: ""
                        val niveles = arrayListOf<Nivel>()

                        for (nivelSnapshot in cancionSnapshot.child("niveles").children) {
                            val numberNivel = nivelSnapshot.child("nivel").getValue(String::class.java) ?: ""
                            val checkedValues = nivelSnapshot.child("checkedValues").getValue(String::class.java) ?: ""
                            val type = nivelSnapshot.child("type").getValue(String::class.java) ?: ""
                            val player = nivelSnapshot.child("player").getValue(String::class.java) ?: ""
                            val rankings = arrayListOf<FirstRank>()
                            for (rankingSnapshot in nivelSnapshot.child("fisrtRank").children) {
                                val nombre = rankingSnapshot.child("nombre").getValue(String::class.java) ?: ""
                                val puntaje = rankingSnapshot.child("puntaje").getValue(String::class.java) ?: "0"
                                val grade = rankingSnapshot.child("grade").getValue(String::class.java) ?: ""
                                rankings.add(FirstRank(nombre, puntaje, grade))
                            }
                            niveles.add(Nivel(numberNivel, checkedValues, type, player, rankings))
                        }

                        listResult.add(Cancion(nombreCancion, niveles))
                    }
                }

                callback(listResult)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error al leer canciones del canal $canalNombre", error.toException())
                callback(listGlobalRanking)
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

        val imgNoteSkinDG = ImageView(this).apply {
            setImageBitmap(BitmapFactory.decodeFile(playerSong.rutaNoteSkin + "/_Icon.png"))
        }
        linearEfects.addView(imgNoteSkinDG)
        imgNoteSkinDG.x = posX
        imgNoteSkinDG.layoutParams.width = medidaFlechas.toInt()
        imgNoteSkinDG.layoutParams.height = heightImages

        if (playerSong.hj) {
            val image = ImageView(this)
            image.setImageBitmap(BitmapFactory.decodeFile(playerSong.pathImgHJ))
            linearEfects.addView(image)
            image.x = posX * 2
            image.layoutParams.width = medidaFlechas.toInt()
            image.layoutParams.height = heightImages
        }

        for (i in 0 until listEfectsDisplay.size) {
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
        if (halfDouble) {
            imgBitActive.setImageBitmap(bitActiveLvHD)
        } else {
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
            if (!isOnline) {
                text = playerSong.level
            } else {
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
            totalScore >= 999999 -> {
                grade = arrayGrades[0]
                soundGrade = 1
                newGrade = "SSS+"
            }
            totalScore in 990000..999998 -> {
                grade = arrayGrades[1]
                soundGrade = 1
                newGrade = "SSS"
            }
            totalScore in 985000..989999 -> {
                grade = arrayGrades[2]
                soundGrade = 2
                newGrade = "SS+"
            }
            totalScore in 980000..984999 -> {
                grade = arrayGrades[3]
                soundGrade = 2
                newGrade = "SS"
            }
            totalScore in 975000..979999 -> {
                grade = arrayGrades[4]
                soundGrade = 3
                newGrade = "S+"
            }
            totalScore in 970000..974999 -> {
                grade = arrayGrades[5]
                soundGrade = 3
                newGrade = "S"
            }
            totalScore in 960000..969999 -> {
                grade = arrayGrades[6]
                soundGrade = 4
                newGrade = "AAA+"
            }
            totalScore in 950000..959999 -> {
                grade = arrayGrades[7]
                soundGrade = 4
                newGrade = "AAA"
            }
            totalScore in 925000..949999 -> {
                grade = arrayGrades[8]
                soundGrade = 4
                newGrade = "AA+"
            }
            totalScore in 900000..924999 -> {
                grade = arrayGrades[9]
                soundGrade = 4
                newGrade = "AA"
            }
            totalScore in 825000..899999 -> {
                grade = arrayGrades[10]
                soundGrade = 4
                newGrade = "A+"
            }
            totalScore in 750000..824999 -> {
                grade = arrayGrades[11]
                soundGrade = 4
                newGrade = "A"
            }
            totalScore in 650000..749999 -> {
                grade = arrayGrades[12]
                soundGrade = 5
                newGrade = "B"
            }
            totalScore in 550000..649999 -> {
                grade = arrayGrades[13]
                soundGrade = 6
                newGrade = "C"
            }
            totalScore in 450000..549999 -> {
                grade = arrayGrades[14]
                soundGrade = 7
                newGrade = "D"
            }
            else -> {
                grade = arrayGrades[15]
                soundGrade = 8
                newGrade = "F"
            }
        }
        if(totalScore == 0){
            newGrade = "$newGrade|RG"  // Rough Game
            gradeDescription = arrGradesDesc[7]
        } else {
            if (resultSong.miss == 0) {
                // Casos sin misses
                if (resultSong.bad == 0 && resultSong.good == 0 && resultSong.great == 0) {
                    newGrade = "$newGrade|PG"  // Perfect Game
                    gradeDescription = arrGradesDesc[0]
                } else if (resultSong.bad == 0 && resultSong.good == 0) {
                    newGrade = "$newGrade|UG"  // Ultimate Game
                    gradeDescription = arrGradesDesc[1]
                } else if (resultSong.bad == 0) {
                    newGrade = "$newGrade|EG"  // Extreme Game
                    gradeDescription = arrGradesDesc[2]
                } else {
                    newGrade = "$newGrade|SG"  // Superb Game
                    gradeDescription = arrGradesDesc[3]
                }
            } else {
                // Casos con misses
                when {
                    resultSong.miss <= 5 -> {
                        newGrade = "$newGrade|MG"  // Marvelous Game
                        gradeDescription = arrGradesDesc[4]
                    }

                    resultSong.miss <= 10 -> {
                        newGrade = "$newGrade|TG"  // Talented Game
                        gradeDescription = arrGradesDesc[5]
                    }

                    resultSong.miss <= 20 -> {
                        newGrade = "$newGrade|FG"  // Fair Game
                        gradeDescription = arrGradesDesc[6]
                    }

                    resultSong.miss > 20 -> {
                        newGrade = "$newGrade|RG"  // Rough Game
                        gradeDescription = arrGradesDesc[7]
                    }
                }
            }
        }
    }

    private fun showGradeP1(totalScoreP1: Int) {
        gradeP1 = when {
            totalScoreP1 >= 999999 -> arrayGrades[0]
            totalScoreP1 in 990000..999998 -> arrayGrades[1]
            totalScoreP1 in 985000..989999 -> arrayGrades[2]
            totalScoreP1 in 980000..984999 -> arrayGrades[3]
            totalScoreP1 in 975000..979999 -> arrayGrades[4]
            totalScoreP1 in 970000..974999 -> arrayGrades[5]
            totalScoreP1 in 960000..969999 -> arrayGrades[6]
            totalScoreP1 in 950000..959999 -> arrayGrades[7]
            totalScoreP1 in 925000..949999 -> arrayGrades[8]
            totalScoreP1 in 900000..924999 -> arrayGrades[9]
            totalScoreP1 in 825000..899999 -> arrayGrades[10]
            totalScoreP1 in 750000..824999 -> arrayGrades[11]
            totalScoreP1 in 650000..749999 -> arrayGrades[12]
            totalScoreP1 in 550000..649999 -> arrayGrades[13]
            totalScoreP1 in 450000..549999 -> arrayGrades[14]
            else -> arrayGrades[15]
        }
    }

    private fun showGradeP2(totalScoreP2: Int) {
        gradeP2 = when {
            totalScoreP2 >= 999999 -> arrayGrades[0]
            totalScoreP2 in 990000..999998 -> arrayGrades[1]
            totalScoreP2 in 985000..989999 -> arrayGrades[2]
            totalScoreP2 in 980000..984999 -> arrayGrades[3]
            totalScoreP2 in 975000..979999 -> arrayGrades[4]
            totalScoreP2 in 970000..974999 -> arrayGrades[5]
            totalScoreP2 in 960000..969999 -> arrayGrades[6]
            totalScoreP2 in 950000..959999 -> arrayGrades[7]
            totalScoreP2 in 925000..949999 -> arrayGrades[8]
            totalScoreP2 in 900000..924999 -> arrayGrades[9]
            totalScoreP2 in 825000..899999 -> arrayGrades[10]
            totalScoreP2 in 750000..824999 -> arrayGrades[11]
            totalScoreP2 in 650000..749999 -> arrayGrades[12]
            totalScoreP2 in 550000..649999 -> arrayGrades[13]
            totalScoreP2 in 450000..549999 -> arrayGrades[14]
            else -> arrayGrades[15]
        }
    }

    private fun showNewRecord(imgNewRecord: ImageView) {
        handlerDG.postDelayed({
            imgNewRecord.visibility = View.VISIBLE
            imgNewRecord.startAnimation(AnimationUtils.loadAnimation(DGContext, R.anim.stamp_effect))
            isPlayingNewRecord = soundPoolSelectSongKsf.play(new_record, 1.0f, 1.0f, 1, 0, 1.0f)
            getBtnAceptar()
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

        imgGradeDescription.visibility = View.VISIBLE
        imgGradeDescription.alpha = 0f

        val fadeInDesc = ObjectAnimator.ofFloat(imgGradeDescription, "alpha", 0f, 1f)

        val animatorSetDesc = AnimatorSet()
        animatorSetDesc.playTogether(scaleX, scaleY, fadeInDesc)
        animatorSetDesc.duration = 500
        animatorSetDesc.interpolator = AccelerateDecelerateInterpolator()
        animatorSetDesc.start()
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
            1 -> sss_rank
            2 -> ss_rank
            3 -> s_rank
            4 -> a_rank
            5 -> b_rank
            6 -> c_rank
            7 -> d_rank
            8 -> f_rank
            else -> 0
        }
    }

    private fun getRankSoundB(soundGrade: Int): Int {
        return when (soundGrade) {
            1 -> sss_rankB
            2 -> ss_rankB
            3 -> s_rankB
            4 -> a_rankB
            5 -> b_rankB
            6 -> c_rankB
            7 -> d_rankB
            8 -> f_rankB
            else -> 0
        }
    }

    private fun animateBar(bar: ImageView): Job {
        return CoroutineScope(Dispatchers.Main).launch {
            bar.isVisible = true
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
            if (isOnline) {
                val listScores = arrayListOf(
                    activeSala.jugador1.result.perfect,
                    activeSala.jugador1.result.great,
                    activeSala.jugador1.result.good,
                    activeSala.jugador1.result.bad,
                    activeSala.jugador1.result.miss,
                    activeSala.jugador1.result.maxCombo
                )
                for (i in 0 until 6) {
                    animatedNumber(listScores[i], ((i * 50)).toLong(), textViews[i]) { true }
                }
                animateScore(textViews[6], activeSala.jugador1.result.score) { true }
            } else {
                val listScores = arrayListOf(
                    resultSong.perfect.toString(),
                    resultSong.great.toString(),
                    resultSong.good.toString(),
                    resultSong.bad.toString(),
                    resultSong.miss.toString(),
                    resultSong.maxCombo.toString()
                )
                for (i in 0 until 6) {
                    animatedNumber(listScores[i], ((i * 50)).toLong(), textViews[i]) { true }
                }
                animateScore(textViews[6], totalScore.toString()) { true }
            }
        }
    }

    private suspend fun setCountsP2(textViews: List<TextView>) {
        coroutineScope {
            val listScores = arrayListOf(
                activeSala.jugador2.result.perfect,
                activeSala.jugador2.result.great,
                activeSala.jugador2.result.good,
                activeSala.jugador2.result.bad,
                activeSala.jugador2.result.miss,
                activeSala.jugador2.result.maxCombo
            )
            for (i in 0 until 6) {
                animatedNumber(listScores[i], ((i * 50)).toLong(), textViews[i]) { true }
            }
            animateScore(textViews[6], activeSala.jugador2.result.score) { true }
        }
    }

    private suspend fun animatedNumber(
        number: String,
        delayMs: Long,
        textView: TextView,
        isPoolLoaded: () -> Boolean,
    ) {
        delay(delayMs)
        if (!isPoolLoaded()) return

        var strNumber = number
        if (strNumber.length < 3) {
            strNumber = (strNumber.toInt() + 1000).toString().substring(1)
        }

        val reversed = strNumber.reversed()
        var currentDisplay = ""
        val timePerDigit = 30L / strNumber.length
        val tickInterval = 50L

        coroutineScope {
            var lastSoundTime = 0L

            for (digit in reversed) {
                repeat(6) {
                    textView.text = (0..9).random().toString() + currentDisplay
                    delay(timePerDigit)

                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastSoundTime > tickInterval && isPoolLoaded()) {
                        launch(Dispatchers.IO) {
                            soundPoolSelectSongKsf.play(tick, 1f, 1f, 1, 0, 1.0f)
                        }
                        lastSoundTime = currentTime
                    }
                }

                currentDisplay = digit + currentDisplay
                textView.text = currentDisplay
            }
        }

        textView.text = strNumber.toInt().toString()
    }

    private suspend fun animateScore(
        textView: TextView,
        score: String,
        isPoolLoaded: () -> Boolean,
    ) {
        val strScore = String.format("%07d", score.toInt())
        val reversed = strScore.reversed()
        var currentDisplay = ""
        val timePerDigit = 30L / strScore.length

        for (digit in reversed) {
            repeat(8) {
                textView.text = (0..9).random().toString() + currentDisplay
                delay(timePerDigit)
            }

            currentDisplay = digit + currentDisplay
            textView.text = currentDisplay
        }

        textView.text = (strScore).toInt().toString()
    }

    private fun getBtnAceptar() {
        imgAceptar.visibility = View.VISIBLE
        imgFloor.visibility = View.VISIBLE

        val yDelta = width / 36
        val animateSetTraslation = TranslateAnimation(0f, 0f, -yDelta.toFloat(), (yDelta * 1.5).toFloat())
        animateSetTraslation.duration = 400
        animateSetTraslation.repeatCount = Animation.INFINITE
        animateSetTraslation.repeatMode = Animation.REVERSE
        imgAceptar.startAnimation(animateSetTraslation)
        imgAceptar.bringToFront()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::mediaPlayerEvaluation.isInitialized) {
            mediaPlayerEvaluation.release()
        }
        resultListener?.let { salaRef.removeEventListener(it) }
        handlerDG.removeCallbacksAndMessages(null)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        Toast.makeText(this, "Press Center Step", Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        if (::mediaPlayerEvaluation.isInitialized && mediaPlayerEvaluation.isPlaying) {
            mediaPlayerEvaluation.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::mediaPlayerEvaluation.isInitialized && !mediaPlayerEvaluation.isPlaying) {
            mediaPlayerEvaluation.start()
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        val decorView: View = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }
}

enum class SaveResult {
    NONE,
    LOCAL,
    LOCAL_AND_FIREBASE,
    INVALID_LEVEL
}
