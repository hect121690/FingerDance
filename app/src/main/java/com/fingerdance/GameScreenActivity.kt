package com.fingerdance

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import android.widget.VideoView
import androidx.core.view.isVisible
import com.badlogic.gdx.Game
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.io.File

private val thisHandler = Handler(Looper.getMainLooper())

private const val timeToPlay = 1000L
var countMiss = 0
var halfDouble = false

open class GameScreenActivity : AndroidApplication() {

    private lateinit var gdxContainer : RelativeLayout
    private var currentVideoPositionScreen : Int = 0

    lateinit var videoViewBgaoff : VideoView
    //lateinit var videoViewBgaOn : VideoView
    lateinit var videoBgaOn : TextureView
    lateinit var videoBgaOnPLayer: MediaPlayer

    private lateinit var imgEndSong : ImageView
    private lateinit var bitPerfectGame : Bitmap
    private lateinit var bitFullcombo : Bitmap
    private lateinit var bitNoMiss  : Bitmap

    private var isPlayingEndSong = 0
    private var isFirstPlay = true  // Bandera para la primera reproducción

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_screen)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        gdxContainer = findViewById(R.id.gdxContainer)
        gdxContainer.layoutParams = RelativeLayout.LayoutParams(width, height)
        halfDouble = intent.getBooleanExtra("IS_HALF_DOUBLE", false)
        readyPlay = false

        canGoBack = false
        thisHandler.postDelayed({
            canGoBack = true
        }, 3000)

        if(mediPlayer.isPlaying){
            mediPlayer.stop()
        }

        resultSong = ResultSong()

        val pathImgs = getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/game_play")!!.absolutePath
        bitPerfectGame = BitmapFactory.decodeFile("$pathImgs/perfect_game.png")
        bitPerfectGame = trimTransparentEdges(bitPerfectGame)
        bitFullcombo = BitmapFactory.decodeFile("$pathImgs/full_combo.png")
        bitFullcombo = trimTransparentEdges(bitFullcombo)
        bitNoMiss = BitmapFactory.decodeFile("$pathImgs/no_miss.png")
        bitNoMiss = trimTransparentEdges(bitNoMiss)

        imgEndSong = findViewById(R.id.imgEndSong)
        imgEndSong.layoutParams.width = (medidaFlechas * 5).toInt()
        imgEndSong.visibility = View.INVISIBLE

        val linearBGADark = findViewById<LinearLayout>(R.id.linearBGADark)
        addVideoBackground()

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
                videoBgaOnPLayer.start()
                if(playerSong.isBAGDark){
                    linearBGADark.visibility = View.VISIBLE
                }else {
                    linearBGADark.visibility = View.GONE
                }
            } else {
                videoViewBgaoff.start()
                videoViewBgaoff.setOnCompletionListener {
                    videoViewBgaoff.start()
                }
                if(playerSong.isBAGDark){
                    linearBGADark.visibility = View.VISIBLE
                }else {
                    linearBGADark.visibility = View.GONE
                }
            }
            mediaPlayer.start()

        }, timeToPlay)
        if(!isOnline){
            if(!isOffline){
                checkedValues = generateCheckedValues(File(playerSong.rutaKsf)) + "|" + File(playerSong.rutaCancion!!).length()
            }
        }
    }

    private fun trimTransparentEdges(source: Bitmap): Bitmap {
        val width = source.width
        val height = source.height
        var top = 0
        var left = 0
        var right = width - 1
        var bottom = height - 1

        // Buscar primer píxel visible arriba
        loop@ for (y in 0 until height) {
            for (x in 0 until width) {
                if ((source.getPixel(x, y) shr 24) != 0) {
                    top = y
                    break@loop
                }
            }
        }

        // Buscar primer píxel visible abajo
        loop@ for (y in height - 1 downTo 0) {
            for (x in 0 until width) {
                if ((source.getPixel(x, y) shr 24) != 0) {
                    bottom = y
                    break@loop
                }
            }
        }

        // Buscar primer píxel visible a la izquierda
        loop@ for (x in 0 until width) {
            for (y in 0 until height) {
                if ((source.getPixel(x, y) shr 24) != 0) {
                    left = x
                    break@loop
                }
            }
        }

        // Buscar primer píxel visible a la derecha
        loop@ for (x in width - 1 downTo 0) {
            for (y in 0 until height) {
                if ((source.getPixel(x, y) shr 24) != 0) {
                    right = x
                    break@loop
                }
            }
        }

        if (right < left || bottom < top) return source

        return Bitmap.createBitmap(source, left, top, right - left + 1, bottom - top + 1)
    }

    private fun isFileExists(file: File): Boolean {
        return file.exists() && !file.isDirectory
    }

    private fun addVideoBackground() {
        videoViewBgaoff = findViewById(R.id.videoViewBgaoff)
        videoViewBgaoff.isVisible = false
        videoBgaOnPLayer = MediaPlayer()

        videoBgaOn = findViewById(R.id.videoViewBgaOn)
        videoBgaOn.y = medidaFlechas * 2
        val newWidth = (width * 1.25).toInt()
        val newHeight = (newWidth * 9 / 16).toInt()

        videoBgaOn.layoutParams = videoBgaOn.layoutParams.apply {
            width = newWidth
            height = newHeight
        }

        videoBgaOn.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, w: Int, h: Int) {
                videoBgaOnPLayer.setSurface(Surface(surface))
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, w: Int, h: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }


        if(isFileExists(File(playerSong.rutaVideo!!))){
            if(playerSong.isBGAOff == false){
                videoViewBgaoff.isVisible = false
                videoBgaOn.isVisible = true
                videoBgaOnPLayer.reset()

                // Preparar MediaPlayer en hilo separado para evitar ANR
                Thread {
                    try {
                        videoBgaOnPLayer.apply {
                            setDataSource(playerSong.rutaVideo)
                            prepare()
                        }
                        isVideo = true
                    } catch (e: Exception) {
                        Log.e("GameScreenActivity", "Error preparando video: ${e.message}")
                        isVideo = false
                    }
                }.start()
            }else{
                videoViewBgaoff.isVisible = true
                videoBgaOn.isVisible = false
                videoViewBgaoff.setVideoPath(bgaOff)
                isVideo = false
            }
        }else{
            videoViewBgaoff.isVisible = true
            videoBgaOn.isVisible = false
            videoViewBgaoff.setVideoPath(bgaOff)
            isVideo = false
        }

        mediaPlayer.isLooping = false
        mediaPlayer.setOnPreparedListener {
            isMediaPlayerPrepared = true
        }

        mediaPlayer.setOnCompletionListener {
            resultSong.banner = playerSong.rutaBanner!!
            if(currentChannel == "06-FAVORITES"){
                val nameChannels = listSongsChannelKsf.find { it.title == currentSong }?.channel
                listenScoreChannel(nameChannels.toString()) { listaCanciones ->
                    listGlobalRanking = listaCanciones
                }
                thisHandler.postDelayed({
                    val rankingItem = listGlobalRanking.find { it.cancion == currentSong }
                    if(rankingItem != null) {
                        currentWorldScore = if(rankingItem.niveles[positionActualLvs].fisrtRank.isNotEmpty()) {
                            listOf(
                                rankingItem.niveles[positionActualLvs].fisrtRank[0].puntaje,
                                rankingItem.niveles[positionActualLvs].fisrtRank[1].puntaje,
                                rankingItem.niveles[positionActualLvs].fisrtRank[2].puntaje
                            )
                        }else{
                            listOf("1000000", "1000000", "1000000")
                        }
                    }
                }, 7000)
            }
            thisHandler.postDelayed({
                getEndSong()
            },1000)
            thisHandler.postDelayed({
                val intent = Intent(this, DanceGrade()::class.java)
                startActivity(intent)
                this.finish()
            }, 4000)
        }

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

    private fun getEndSong(){
        if(resultSong.miss == 0 && resultSong.bad == 0 && resultSong.good == 0
            && resultSong.great == 0 && resultSong.perfect == 0) {
            return

        }
        if(resultSong.miss == 0 && resultSong.bad == 0 && resultSong.good == 0) {
            if (resultSong.great == 0) {
                imgEndSong.setImageBitmap(bitPerfectGame)
                imgEndSong.visibility = View.VISIBLE
                imgEndSong.startAnimation(AnimationUtils.loadAnimation(this, R.anim.stamp_effect))
                isPlayingEndSong = soundPoolSelectSongKsf.play(perfect_game, 1.0f, 1.0f, 1, 0, 1.0f)
            } else {
                imgEndSong.setImageBitmap(bitFullcombo)
                imgEndSong.visibility = View.VISIBLE
                imgEndSong.startAnimation(AnimationUtils.loadAnimation(this, R.anim.stamp_effect))
                isPlayingEndSong = soundPoolSelectSongKsf.play(full_combo, 1.0f, 1.0f, 1, 0, 1.0f)
            }
        }else if(resultSong.miss == 0){
            imgEndSong.setImageBitmap(bitNoMiss)
            imgEndSong.visibility = View.VISIBLE
            imgEndSong.startAnimation(AnimationUtils.loadAnimation(this, R.anim.stamp_effect))
            isPlayingEndSong = soundPoolSelectSongKsf.play(no_miss, 1.0f, 1.0f, 1, 0, 1.0f)
        }
        imgEndSong.bringToFront()
    }

    private fun generateCheckedValues(file: File): String {
        var inStepBlock = false
        var count1 = 0
        var count4 = 0

        file.forEachLine { line ->
            if (!inStepBlock && line.startsWith("#STEP:")) {
                inStepBlock = true
                return@forEachLine
            }

            if (inStepBlock) {
                if (line.startsWith("22222")) return@forEachLine
                if (line.startsWith("|")) return@forEachLine
                line.forEach { char ->
                    when (char) {
                        '1' -> count1++
                        '4' -> count4++
                    }
                }
            }
        }

        return "$count1|$count4"
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            gdxContainer.removeAllViews()
        } catch (e: Exception) {
            Log.e("GameScreenActivity", "Error al remover vistas: ${e.message}")
        }

        currentVideoPositionScreen = 0
        isFirstPlay = true

        // Liberar MediaPlayer del audio
        try {
            mediaPlayer.setOnCompletionListener(null)
            if(mediaPlayer.isPlaying){
                mediaPlayer.stop()
            }
            mediaPlayer.release()
        } catch (e: Exception) {
            Log.e("GameScreenActivity", "Error al liberar mediaPlayer: ${e.message}")
        }

        // Liberar MediaPlayer del video BGA
        if (isVideo) {
            try {
                if (::videoBgaOnPLayer.isInitialized) {
                    if (videoBgaOnPLayer.isPlaying) {
                        videoBgaOnPLayer.stop()
                    }
                    videoBgaOnPLayer.seekTo(0)
                    videoBgaOnPLayer.release()
                }
            } catch (e: Exception) {
                Log.e("GameScreenActivity", "Error al liberar videoBgaOnPLayer: ${e.message}")
            }
        } else {
            try {
                if (::videoViewBgaoff.isInitialized) {
                    videoViewBgaoff.seekTo(0)
                    videoViewBgaoff.suspend()
                }
            } catch (e: Exception) {
                Log.e("GameScreenActivity", "Error al suspender videoViewBgaoff: ${e.message}")
            }
        }

        // Reciclar Bitmaps
        try {
            if (::bitPerfectGame.isInitialized && !bitPerfectGame.isRecycled) {
                bitPerfectGame.recycle()
            }
        } catch (e: Exception) {
            Log.e("GameScreenActivity", "Error al reciclar bitPerfectGame: ${e.message}")
        }

        try {
            if (::bitFullcombo.isInitialized && !bitFullcombo.isRecycled) {
                bitFullcombo.recycle()
            }
        } catch (e: Exception) {
            Log.e("GameScreenActivity", "Error al reciclar bitFullcombo: ${e.message}")
        }

        try {
            if (::bitNoMiss.isInitialized && !bitNoMiss.isRecycled) {
                bitNoMiss.recycle()
            }
        } catch (e: Exception) {
            Log.e("GameScreenActivity", "Error al reciclar bitNoMiss: ${e.message}")
        }

        // Limpiar ImageView
        try {
            if (::imgEndSong.isInitialized) {
                imgEndSong.setImageBitmap(null)
            }
        } catch (e: Exception) {
            Log.e("GameScreenActivity", "Error al limpiar imgEndSong: ${e.message}")
        }

        // Cancelar handlers
        try {
            thisHandler.removeCallbacksAndMessages(null)
        } catch (e: Exception) {
            Log.e("GameScreenActivity", "Error al cancelar handlers: ${e.message}")
        }
    }

    fun breakDance(){

        this.finish()
        countMiss = 0
        val intent = Intent(this, BreakDance::class.java)
        startActivity(intent)
    }

    private var backPressedTime: Long = 0
    private lateinit var backToast: Toast
    private var canGoBack = false
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (!canGoBack) {
            Toast.makeText(this, "Espera 3 segundos para regresar al Select Song", Toast.LENGTH_SHORT).show()
            return
        }

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
            videoBgaOnPLayer.pause()
        } else {
            videoViewBgaoff.pause()
        }
    }

    private var hasWaitedForDelay = false
    override fun onResume() {
        super.onResume()

        // Si es la primera reproducción, no hacer nada
        // El video ya se inició en onCreate
        if (isFirstPlay) {
            isFirstPlay = false
            return
        }

        // Para las demás veces que se reanuda (después de pause), restaurar la posición
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
            videoBgaOnPLayer.seekTo(currentVideoPositionScreen)
            if (!videoBgaOnPLayer.isPlaying) {
                videoBgaOnPLayer.start()
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
    private var gameScreenHD: GameScreenKsfHD? = null
    override fun create() {
        if(halfDouble){
            gameScreenHD = GameScreenKsfHD(gsa)
            setScreen(gameScreenHD)
        }else{
            gameScreen = GameScreenKsf(gsa)
            setScreen(gameScreen)
        }

    }

    override fun dispose() {
        super.dispose()
        gameScreen?.dispose()
    }
}


