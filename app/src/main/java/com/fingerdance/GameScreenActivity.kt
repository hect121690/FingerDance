package com.fingerdance

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PixelFormat
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.ViewPropertyAnimator
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.Toast
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import com.badlogic.gdx.Game
import com.badlogic.gdx.backends.android.AndroidApplication
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration
import com.fingerdance.ssc.GameScreenSsc
import com.fingerdance.ssc.GameScreenSscHD
import com.fingerdance.ssc.PlayerSsc
import com.fingerdance.ssc.SongClock
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import java.io.File

private val thisHandler = Handler(Looper.getMainLooper())

private const val timeToPlay = 1000L

open class GameScreenActivity : AndroidApplication() {

    private lateinit var gdxContainer : RelativeLayout
    private var currentVideoPositionScreen : Int = 0

    private lateinit var videoBgaOff: TextureView
    private lateinit var videoBgaOffPlayer: MediaPlayer

    private lateinit var videoBgaOn : TextureView
    private lateinit var videoBgaOnPLayer: MediaPlayer

    private lateinit var imgEndSong : ImageView
    private lateinit var bitPerfectGame : Bitmap
    private lateinit var bitFullcombo : Bitmap
    private lateinit var bitNoMiss  : Bitmap

    private var isPlayingEndSong = 0
    private var isFirstPlay = true  // Bandera para la primera reproducción

    private var backPressedTime: Long = 0
    private lateinit var backToast: Toast
    private var canGoBack = false
    private lateinit var songClock: SongClock

    private val backInvokedCallback =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            OnBackInvokedCallback {
                handleBackPressed()
            }
        } else {
            null
        }

    private var onlinePlayerSsc: PlayerSsc? = null
    private var opponentLiveListener: ValueEventListener? = null
    private var lastOpponentLive = LiveResult()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game_screen)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            backInvokedCallback?.let { callback ->
                onBackInvokedDispatcher.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT,
                    callback
                )
            }
        }
        canGoBack = false
        songClock = SongClock(mediaPlayer)
        backToast = Toast.makeText(this,"Presiona nuevamente para salir", Toast.LENGTH_SHORT)
        thisHandler.postDelayed({
            canGoBack = true
        }, 3000)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        gdxContainer = findViewById(R.id.gdxContainer)
        gdxContainer.layoutParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
        )
        halfDouble = intent.getBooleanExtra("IS_HALF_DOUBLE", false)
        //isVertical = true //intent.getBooleanExtra("IS_VERTICAL", true)
        readyPlay = false

        canGoBack = false
        thisHandler.postDelayed({
            canGoBack = true
        }, 3000)

        if(mediPlayer.isPlaying){
            mediPlayer.stop()
        }

        resultSong = ResultSong()
        if (isOnline) {
            resetMyOnlineLive()
            startOnlineLiveSync()
        }
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
        val gdxView = initializeForView(MyGameScreen(this, playerSong), config)
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
                videoBgaOffPlayer.start()
                if(playerSong.isBAGDark || bgaOffSelected == "aleatorio"){
                    linearBGADark.visibility = View.VISIBLE
                }else {
                    linearBGADark.visibility = View.GONE
                }
            }
            mediaPlayer.start()
            if(mediPlayer.isPlaying){
                mediPlayer.stop()
            }
        }, timeToPlay)
    }

    private fun resetMyOnlineLive() {
        val myLivePath = if (isPlayer1) {
            "jugador1/live"
        } else {
            "jugador2/live"
        }

        salaRef.child(myLivePath).setValue(
            LiveResult(
                score = 0
            )
        )
    }

    fun registerOnlinePlayerSsc(playerSsc: PlayerSsc) {
        onlinePlayerSsc = playerSsc

        if (isOnline) {
            playerSsc.updateOpponentLive(lastOpponentLive)
        }
    }

    private fun startOnlineLiveSync() {
        if (!isOnline) return
        val opponentPath = if (isPlayer1) {
            "jugador2/live"
        } else {
            "jugador1/live"
        }

        val opponentRef = salaRef.child(opponentPath)

        opponentLiveListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val live = snapshot.getValue(LiveResult::class.java) ?: LiveResult()
                lastOpponentLive = live
                onlinePlayerSsc?.updateOpponentLive(live)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("ONLINE_LIVE", "Error leyendo LiveResult rival: ${error.message}")
            }
        }

        opponentRef.addValueEventListener(opponentLiveListener!!)
    }

    private fun stopOnlineLiveSync() {
        val listener = opponentLiveListener ?: return
        val opponentPath = if (isPlayer1) {
            "jugador2/live"
        } else {
            "jugador1/live"
        }
        salaRef.child(opponentPath).removeEventListener(listener)
        opponentLiveListener = null
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

    @RequiresApi(Build.VERSION_CODES.Q)
    fun getSongTimeMs(): Double {
        return songClock.getPositionMs()
    }

    private fun handleBackPressed() {
        if (!canGoBack) {
            Toast.makeText(this, "Espera 3 segundos para regresar al Select Song", Toast.LENGTH_SHORT).show()
            return
        }

        if (isOnline) {
            Toast.makeText(this, "No puedes salir durante una partida en línea", Toast.LENGTH_SHORT).show()
            return
        }

        val currentTime = System.currentTimeMillis()
        if (currentTime - backPressedTime <= 2000L) {
            backToast.cancel()
            isVideo = false
            finish()
            return
        }

        backPressedTime = currentTime

        backToast.cancel()
        backToast.show()
    }

    private fun addVideoBackground() {
        videoBgaOn = findViewById(R.id.videoViewBgaOn)
        videoBgaOff = findViewById(R.id.videoViewBgaOff)

        videoBgaOnPLayer = MediaPlayer()
        videoBgaOnPLayer.setOnInfoListener { _, what, _ ->
            if (what == MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START) {
                openBgaOn()
                true
            } else {
                false
            }
        }
        videoBgaOffPlayer = MediaPlayer()

        // 🔹 Ajuste BGA ON (Configuración de dimensiones)
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

        videoBgaOff.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, w: Int, h: Int) {
                videoBgaOffPlayer.setSurface(Surface(surface))
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, w: Int, h: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }

        val customVideo = playerSong.rutaVideo
        val hasCustom = !customVideo.isNullOrEmpty() && isFileExists(File(customVideo))

        if (hasCustom && !playerSong.isBGAOff) {
            val bgBanner: ImageView = findViewById(R.id.bgImageForBga)
            val bitmap = BitmapFactory.decodeFile(playerSong.rutaBanner)
            val bitmapDrawable = bitmap.toDrawable(resources)
            bgBanner.background = bitmapDrawable
            bgBanner.layoutParams.height = height
            bgBanner.layoutParams.width = height + (height / 2)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                bgBanner.setRenderEffect(
                    RenderEffect.createBlurEffect(
                        20f, // radio X
                        20f, // radio Y
                        Shader.TileMode.CLAMP
                    )
                )
            }
            bgBanner.alpha = 0.5f

            // VIDEO GRANDE
            videoBgaOff.isVisible = false
            prepareVideo(videoBgaOnPLayer, customVideo)
            isVideo = true
        } else {
            // VIDEO FULL SCREEN
            videoBgaOn.isVisible = false
            videoBgaOff.isVisible = true
            if(bgaOffSelected == "aleatorio"){
                bgaOff = listBgas.random()
            }
            prepareVideo(videoBgaOffPlayer, bgaOff, isBgaOff = true)
            isVideo = false
        }

        mediaPlayer.isLooping = false
        mediaPlayer.setOnPreparedListener {
            isMediaPlayerPrepared = true
        }

        mediaPlayer.setOnCompletionListener {
            resultSong.banner = playerSong.rutaBanner!!

            closeBgaOn()
            thisHandler.postDelayed({
                getEndSong()
            }, 500)
            thisHandler.postDelayed({
                isEndingFade = true
            }, 2300)

            thisHandler.postDelayed({
                val intent = Intent(
                    this,
                    if(isVertical) {
                        DanceGrade()::class.java
                    } else {
                        DanceGradeHorizontal()::class.java
                    }
                )
                startActivity(intent)
                overridePendingTransition(0, 0)
                finish()
            }, 3500)
        }
    }

    private fun openBgaOn() {
        videoBgaOn.apply {
            post {
                pivotX = width / 2f
                pivotY = height / 2f
                // Estado inicial tipo CRT apagado
                scaleX = 0.05f
                scaleY = 0.008f
                alpha = 0f
                visibility = View.VISIBLE
                // Línea horizontal brillante
                animate()
                    .alpha(1f)
                    .scaleY(0.03f)
                    .scaleX(1.08f)
                    .setDuration(90)
                    .withEndAction {
                        // 🔥 FASE 2
                        // Explosión vertical CRT
                        animate()
                            .scaleY(1.05f)
                            .scaleX(1.02f)
                            .setDuration(160)
                            .withEndAction {
                                // 🔥 FASE 3
                                // Rebote analógico
                                animate()
                                    .scaleX(0.995f)
                                    .scaleY(0.985f)
                                    .setDuration(70)
                                    .withEndAction {
                                        // 🔥 FASE 4
                                        // Estabilización final
                                        animate()
                                            .scaleX(1f)
                                            .scaleY(1f)
                                            .setDuration(60)
                                            .start()
                                    }.start()
                            }.start()
                    }.start()
            }
        }
    }

    private fun closeBgaOn(){
        videoBgaOn.apply {
            pivotY = height / 2f
            animateIndependent(260) {
                scaleY(0.02f)
                alpha(0f)
                start()
            }
        }
    }

    private fun View.animateIndependent(duration: Long, block: ViewPropertyAnimator.() -> Unit) {
        val scale = try {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE
            )
        } catch (e: Exception) {
            1f
        }

        this.animate().apply {
            setDuration(if (scale <= 0f) duration else (duration / scale).toLong())
            block()
        }
    }

    private fun prepareVideo(player: MediaPlayer, path: String, isBgaOff: Boolean = false) {
        Thread {
            try {
                player.reset()
                player.setDataSource(path)
                player.isLooping = isBgaOff
                player.prepare()
            } catch (e: Exception) {
                Log.e("VIDEO", "Error al preparar video: ${e.message}")
            }
        }.start()
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
                isPlayingEndSong = soundPoolSelectSong.play(perfect_game, 1.0f, 1.0f, 1, 0, 1.0f)
            } else {
                imgEndSong.setImageBitmap(bitFullcombo)
                imgEndSong.visibility = View.VISIBLE
                imgEndSong.startAnimation(AnimationUtils.loadAnimation(this, R.anim.stamp_effect))
                isPlayingEndSong = soundPoolSelectSong.play(full_combo, 1.0f, 1.0f, 1, 0, 1.0f)
            }
        }else if(resultSong.miss == 0){
            imgEndSong.setImageBitmap(bitNoMiss)
            imgEndSong.visibility = View.VISIBLE
            imgEndSong.startAnimation(AnimationUtils.loadAnimation(this, R.anim.stamp_effect))
            isPlayingEndSong = soundPoolSelectSong.play(no_miss, 1.0f, 1.0f, 1, 0, 1.0f)
        }
        imgEndSong.bringToFront()
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        handleBackPressed()
    }

    override fun onDestroy() {
        if (isOnline) {
            stopOnlineLiveSync()
        }
        onlinePlayerSsc = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            backInvokedCallback?.let { callback ->
                onBackInvokedDispatcher.unregisterOnBackInvokedCallback(callback)
            }
        }
        backToast.cancel()
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
                if (::videoBgaOffPlayer.isInitialized) {
                    videoBgaOffPlayer.seekTo(0)
                    videoBgaOffPlayer.release()
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

    override fun onPause() {
        super.onPause()
        currentVideoPositionScreen = mediaPlayer.currentPosition
        mediaPlayer.pause()
        if (isVideo) {
            videoBgaOnPLayer.pause()
        } else {
            videoBgaOffPlayer.pause()
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
            videoBgaOffPlayer.seekTo(currentVideoPositionScreen)
            if (!videoBgaOffPlayer.isPlaying) {
                videoBgaOffPlayer.start()
            }
        }
    }
}

class MyGameScreen(gameScreenActivity: GameScreenActivity, playerSong: PlayerSong) : Game() {
    val gsa = gameScreenActivity
    val ps = playerSong

    private var gameScreenSsc: GameScreenSsc? = null
    private var gameScreenSscHD: GameScreenSscHD? = null
    override fun create() {
        playerSong = ps
        if(halfDouble){
            gameScreenSscHD = GameScreenSscHD(gsa)
            setScreen(gameScreenSscHD)
        }else {
            gameScreenSsc = GameScreenSsc(gsa)
            setScreen(gameScreenSsc)
        }
    }

    override fun dispose() {
        super.dispose()
        if(halfDouble){
            gameScreenSscHD?.dispose()
        }else {
            gameScreenSsc?.dispose()
        }
    }
}



