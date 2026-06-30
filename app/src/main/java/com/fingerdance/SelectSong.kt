package com.fingerdance

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.SurfaceTexture
import android.graphics.Typeface
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.Transformer.Listener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.fingerdance.CustomAdapter.ViewHolder.Companion.md5
import com.fingerdance.MainActivity.VideosDrive
import com.fingerdance.ssc.ChartOffsetAdapter
import com.fingerdance.ssc.Parser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random



private lateinit var mediaPlayerVideo : MediaPlayer
private lateinit var commandWindow: ConstraintLayout
private lateinit var linearLvs: ConstraintLayout

//private lateinit var recyclerView: RecyclerView
private lateinit var carouselSong: SongCarouselViewVertical
private lateinit var recyclerLvs: RecyclerView
private lateinit var recyclerLvsVacios: RecyclerView
private lateinit var recyclerCommands: ViewPager2
private lateinit var recyclerCommandsValues: ViewPager2

private var animOn: Animation? = null
private var animOff: Animation? = null

private val sequence = mutableListOf<Boolean>()
private val sequencePattern = listOf(false, true, false, true, false, true)

private var contador = 0

private val handler = Handler(Looper.getMainLooper())
private val handlerContador = Handler(Looper.getMainLooper())

private var reductor = 100

private val startTimeMs = 30000
private var timer: CountDownTimer? = null
private var isTimerRunning = false

var currentScore = ""
var currentWorldScore = listOf<String>()
lateinit var currentBestGrade : Bitmap

//private var idAdd = ""
//private var interstitialAd: InterstitialAd? = null

private lateinit var nivel: Nivel

var isMediaPlayerPrepared = false
val widthJudges = width / 2
val heightJudges = widthJudges / 6
lateinit var resultSong: ResultSong
private var numberChannel = ""

//private lateinit var layoutManager : LinearLayoutManager

private lateinit var difficultySelected : Bitmap
private lateinit var difficultySelectedHD : Bitmap
var checkedValuesLocal = ""
var isOficialSong = false

var isPrime = false

class SelectSong : AppCompatActivity() {
    private lateinit var linearBG: LinearLayout
    private lateinit var buttonLayout: LinearLayout
    private lateinit var constraintMain: ConstraintLayout
    private lateinit var progressLoading : ProgressBar
    private lateinit var lbNameSong: TextView
    private lateinit var lbArtist: TextView

    private lateinit var lbCurrentBpm: TextView
    private lateinit var txCurrentBpm: TextView
    private lateinit var lbBpm: TextView
    private lateinit var imgSelected: ImageView
    private lateinit var txInfoCW: TextView
    private lateinit var imgVelocidadActual: ImageView
    private lateinit var txVelocidadActual: TextView

    private lateinit var imgOffset: ImageView
    private lateinit var txOffset: TextView

    private lateinit var imgDisplay: ImageView
    private lateinit var imgJudge: ImageView
    private lateinit var imgNoteSkin: ImageView
    private lateinit var imgNoteSkinFondo: ImageView
    private lateinit var nav_izq: ImageView
    private lateinit var nav_der: ImageView
    private lateinit var nav_back_Izq: ImageView
    private lateinit var nav_back_der: ImageView
    private lateinit var commandWindowBG: LinearLayout
    private lateinit var linearMenus: LinearLayout
    private lateinit var linearTop: LinearLayout
    private lateinit var linearCurrent: LinearLayout
    private lateinit var linearValues: LinearLayout
    private lateinit var linearCommands: LinearLayout
    private lateinit var linearInfo: LinearLayout
    private lateinit var linearBottom: LinearLayout
    private lateinit var linearLoading: LinearLayout
    private lateinit var linearListSongs: ConstraintLayout
    private lateinit var imgLoading: ImageView
    private lateinit var imgAceptar: ImageView
    private lateinit var imgFloor: ImageView
    private lateinit var btnCommandWindow: ImageView

    private lateinit var txInfoCurrentSong: TextView
    private lateinit var imgLvSelected: ImageView
    private lateinit var lbLvActive: TextView

    private lateinit var imgBestScore: ImageView
    private lateinit var lbBestScore: TextView
    private lateinit var imgBestGrade: ImageView

    private lateinit var lbWorldName: TextView
    private lateinit var lbWorldScore: TextView
    private lateinit var imgWorldGrade: ImageView

    private lateinit var video_fondo : TextureView
    private lateinit var video_preview: MediaPlayer
    private lateinit var imgPrev: ImageView

    private lateinit var prev: TextureView
    private lateinit var next: TextureView

    private lateinit var prevPlayer: MediaPlayer
    private lateinit var nextPlayer: MediaPlayer

    private lateinit var indicatorLayout: ImageView
    private lateinit var imageCircle : ImageView

    private lateinit var bgaSelectSong: VideoView
    private lateinit var overlayBG: View
    private lateinit var imgContador: ImageView
    private lateinit var smoothScroller : RecyclerView. SmoothScroller
    private lateinit var imgFavorite : ImageView
    private lateinit var bitFavorite : Bitmap
    private lateinit var bitFavoriteListed: Bitmap
    private var saveFavorites = false

    private lateinit var tipsArray : Array<String>
    private lateinit var txTip : TextView
    private var modeSelected = false
    private var orientationMode = OrientationMode.VERTICAL
    private lateinit var selectModeContainer : FrameLayout

    private var selectedIndex = 0
    private val visibleItems = 9
    private var firstVisible = 0

    private var isRunning = false

    private val carouselRunnable = object : Runnable {
        override fun run() {
            carouselSong.update()
            handler.postDelayed(this, 16)
        }
    }

    private val pickPreviewFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val namePreview = "song_p.mp4"
            saveFileToDestination(it, namePreview, false)
        }
    }

    private val pickBgaFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val nameBGA = "song.mp4"
            saveFileToDestination(it, nameBGA, true)
        }
    }
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        getSupportActionBar()?.hide()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_song)
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        onWindowFocusChanged(true)

        isPrime = tema != "Prime"

        isOnline = false
        val txNameChannel = findViewById<TextView>(R.id.txCurrentChannel)
        txNameChannel.text = currentChannel.substringAfter("-")
        val txPlayerName = findViewById<TextView>(R.id.txPlayerName)
        txPlayerName.text = userName
        txNameChannel.layoutParams.width = (width * 0.35).toInt()
        txPlayerName.layoutParams.width = (width * 0.35).toInt()

        nivel = Nivel()
        //recyclerView = findViewById(R.id.recyclerView)
        carouselSong = findViewById(R.id.recyclerView)

        recyclerLvs = findViewById(R.id.recyclerLvs)
        recyclerLvsVacios = findViewById(R.id.recyclerNoLvs)
        linearListSongs = findViewById(R.id.linearListSongs)

        recyclerCommands = findViewById(R.id.recyclerCommands)
        recyclerCommands.isUserInputEnabled = false
        recyclerCommandsValues = findViewById(R.id.recyclerValues)
        recyclerCommandsValues.isUserInputEnabled = false

        constraintMain = findViewById(R.id.constraintMain)
        progressLoading = findViewById(R.id.progressLoading)
        linearBG = findViewById(R.id.linearBG)
        bgaSelectSong = findViewById(R.id.bgaSelectSong)
        bgaSelectSong.visibility = View.GONE
        if (isFileExists(File(bgaPathSelectSong))) {
            bgaSelectSong.visibility = View.VISIBLE
            bgaSelectSong.setVideoPath(bgaPathSelectSong)
            bgaSelectSong.setOnPreparedListener { md ->
                md.setVolume(0f, 0f)
            }
            bgaSelectSong.start()
            bgaSelectSong.setOnCompletionListener {
                bgaSelectSong.start()
            }


        }else{
            linearBG.background = Drawable.createFromPath("$rutaBase/FingerDance/Themes/$tema/GraphicsStatics/bg_select_song.png")
        }
        prepareGradeBitmaps()
        imgPrev = findViewById(R.id.imgPrev)
        val params = imgPrev.layoutParams as ConstraintLayout.LayoutParams
        params.width = width
        params.height = (height * 0.3).toInt()
        imgPrev.layoutParams = params


        animOn = AnimationUtils.loadAnimation(this, R.anim.anim_command_window_on)
        animOff = AnimationUtils.loadAnimation(this, R.anim.anim_command_window_off)

        commandWindow = findViewById(R.id.command_window)
        commandWindowBG = findViewById(R.id.command_window_bg)
        commandWindowBG.foreground = Drawable.createFromPath("$rutaBase/FingerDance/Themes/$tema/GraphicsStatics/command_window/Command_Frame.png")

        mediaPlayerVideo = MediaPlayer()
        mediaPlayerVideo.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )

        commandWindow.layoutParams.height = height / 2
        commandWindow.layoutParams.width = (width / 1.5).roundToInt()

        commandWindowBG.layoutParams.height = commandWindow.layoutParams.height
        commandWindowBG.layoutParams.width = commandWindow.layoutParams.width

        val fondos = Drawable.createFromPath("$rutaBase/FingerDance/Themes/$tema/GraphicsStatics/command_window/Command_Back.png")
        linearTop = findViewById(R.id.linearTop)
        linearMenus = findViewById(R.id.linearMenus)

        linearCurrent = findViewById(R.id.linearCurrent)
        linearCurrent.background = fondos
        linearValues = findViewById(R.id.linearValues)
        linearValues.background = fondos
        linearCommands = findViewById(R.id.linearCommands)
        linearCommands.background = fondos
        linearInfo = findViewById(R.id.linearInfo)
        linearInfo.background = fondos
        linearBottom = findViewById(R.id.linearBottom)

        linearLvs = findViewById(R.id.linearLvs)

        linearLoading = findViewById(R.id.linearLoading)
        imgLoading = findViewById(R.id.imgLoading)
        imgLoading.layoutParams.width = width
        imgLoading.layoutParams.height = (width * 0.7).toInt()

        linearLoading.isVisible = false
        imgLoading.isVisible = false

        lbCurrentBpm = findViewById(R.id.lbCurrentBpm)
        txCurrentBpm = findViewById(R.id.txCurrentBpm)

        imgVelocidadActual = findViewById(R.id.imgVelocidadActual)
        imgVelocidadActual.setImageBitmap(AppResources.bmCommandEmpty)
        txVelocidadActual = findViewById(R.id.txVelocidadActual)

        imgOffset = findViewById(R.id.imgOffsetActual)
        imgOffset.setImageBitmap(AppResources.bmCommandEmpty)
        txOffset = findViewById(R.id.txOffsetActual)
        txOffset.text = "0"

        imgDisplay = findViewById(R.id.imgDisplay)
        imgDisplay.isVisible=false
        imgJudge = findViewById(R.id.imgJudge)
        imgJudge.isVisible=false
        imgNoteSkin = findViewById(R.id.imgNoteSkin)
        imgNoteSkin.isVisible=false
        imgNoteSkinFondo = findViewById(R.id.imgNoteSkinFondo)
        imgNoteSkinFondo.foreground = Drawable.createFromPath("$rutaBase/FingerDance/Themes/$tema/GraphicsStatics/command_window/Command_Effect.png")
        imgNoteSkinFondo.isVisible=false

        linearTop.layoutParams.height = (commandWindow.layoutParams.height / 4.2).roundToInt()
        linearCurrent.layoutParams.height = (commandWindow.layoutParams.height / 8)
        linearValues.layoutParams.height = (commandWindow.layoutParams.height / 8)
        linearCommands.layoutParams.height = (commandWindow.layoutParams.height / 8)
        linearInfo.layoutParams.height = (commandWindow.layoutParams.height / 8)
        linearBottom.layoutParams.height = (commandWindow.layoutParams.height / 4)

        val ancho = commandWindow.layoutParams.width - commandWindow.layoutParams.width / 8
        linearTop.layoutParams.width = ancho
        linearMenus.layoutParams.width = ancho
        linearBottom.layoutParams.width = ancho

        linearLvs.layoutParams.width = (commandWindow.layoutParams.width / 10) * 11
        sizeLvs = linearLvs.layoutParams.width / 9

        imgContador = findViewById(R.id.imgContador)
        imgContador.layoutParams.height = (sizeLvs * .45).toInt()

        iniciarContador()

        indicatorLayout = findViewById(R.id.indicatorImageView)
        indicatorLayout.setImageBitmap(AppResources.bmIndicator)
        indicatorLayout.layoutParams.width = sizeLvs

        bitFavorite = BitmapFactory.decodeFile("$rutaBase/FingerDance/Themes/$tema/GraphicsStatics/favorite.png")
        bitFavoriteListed = BitmapFactory.decodeFile("$rutaBase/FingerDance/Themes/$tema/GraphicsStatics/favorite_listed.png")
        imgFavorite = findViewById(R.id.imgFavorite)
        imgFavorite.layoutParams.width = (medidaFlechas).toInt()

        val anchoRecyclerCommands = linearMenus.layoutParams.width / 3
        recyclerCommands.layoutParams.width = anchoRecyclerCommands - (anchoRecyclerCommands / 20)
        recyclerCommandsValues.layoutParams.width = anchoRecyclerCommands

        btnCommandWindow = findViewById<ImageView>(R.id.btnCommandWindowVertical)
        btnCommandWindow.apply {
            layoutParams.width = (width * 0.3).toInt()
            layoutParams.height = medidaFlechas.toInt()
            setImageDrawable(Drawable.createFromPath("$rutaBase/FingerDance/Themes/$tema/GraphicsStatics/command_window/btnShowCW.png"))
            visibility = View.GONE
            setOnClickListener {
                showCommandWindow(true)
            }
        }

        val anchoTxInfo = linearMenus.layoutParams.width - linearMenus.layoutParams.width / 7
        showCommandWindow(false)

        lbArtist = findViewById(R.id.lbArtist)
        lbBpm = findViewById(R.id.lbBpm)
        lbNameSong = findViewById(R.id.lbNameSong)
        txInfoCW = findViewById(R.id.txInfo)
        txInfoCW.layoutParams.width = anchoTxInfo

        imgSelected = findViewById(R.id.imgSelected)
        imgSelected.setImageBitmap(AppResources.bmSelected)
        imageCircle = findViewById(R.id.imageCircleSS)

        imageCircle.setImageBitmap(AppResources.bmCircle)

        imgFloor = findViewById(R.id.floor_song)
        imgFloor.setImageBitmap(AppResources.bmFloor)

        imgAceptar = findViewById(R.id.floor_start)
        imgAceptar.setImageBitmap(AppResources.bmAceptar)

        txInfoCurrentSong = findViewById(R.id.txInfoCurrentSong)
        imgLvSelected = findViewById(R.id.imgLvSelected)
        difficultySelected = AppResources.difficultedSelected
        difficultySelectedHD = AppResources.difficultedSelectedHD
        imgLvSelected.isVisible = false
        lbLvActive = findViewById(R.id.lbLvActive)
        lbLvActive.isVisible = false
        var textSize = width / 10
        lbLvActive.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())

        val frameBestScoreWidth = (width * 0.6).toInt()

        imgBestScore = findViewById(R.id.imgBestScore)
        imgBestScore.setImageBitmap(AppResources.bmBestScore)
        imgBestScore.isVisible = false
        imgBestScore.layoutParams.width = frameBestScoreWidth

        lbBestScore = findViewById(R.id.lbBestScore)
        lbBestScore.isVisible = false
        lbBestScore.layoutParams.width = (frameBestScoreWidth * 0.4).toInt()

        imgBestGrade = findViewById(R.id.imgBestGrade)
        imgBestGrade.layoutParams.height = (decimoHeigtn * 0.25).toInt()
        imgBestGrade.layoutParams.width = (medidaFlechas * 1.4).toInt()
        imgBestGrade.isVisible = false

        lbWorldName = findViewById(R.id.lbWorldName)
        lbWorldName.isVisible = false
        lbWorldName.layoutParams.width = (frameBestScoreWidth * 0.6).toInt()

        lbWorldScore = findViewById(R.id.lbWorldScoreHorizontal)
        lbWorldScore.isVisible = false
        lbWorldScore.layoutParams.width = (frameBestScoreWidth * 0.4).toInt()

        imgWorldGrade = findViewById(R.id.imgWorldGrade)
        imgWorldGrade.layoutParams.height = (decimoHeigtn * 0.4).toInt()
        imgWorldGrade.layoutParams.width = (medidaFlechas * 1.5).toInt()
        imgWorldGrade.isVisible = false

        val yDelta = width / 40
        val animateSetTraslation = TranslateAnimation(0f, 0f, -yDelta.toFloat(), (yDelta * 2).toFloat())
        animateSetTraslation.duration = 500
        animateSetTraslation.repeatCount = Animation.INFINITE
        animateSetTraslation.repeatMode = Animation.REVERSE
        imgAceptar.startAnimation(animateSetTraslation)
        imgAceptar.bringToFront()

        val animatorSetRotation = AnimationUtils.loadAnimation(this, R.anim.animator_set_rotation)
        imageCircle.startAnimation(animatorSetRotation)

        imgSelected.layoutParams.height = (decimoHeigtn * 1.3).toInt()
        imgSelected.layoutParams.width = (width * 0.6).toInt()
        val anim = AnimationUtils.loadAnimation(this, R.anim.anim_select)
        imgSelected.startAnimation(anim)

        selectModeContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.BLACK)
            setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    // No hace nada
                }
            })
            visibility = View.INVISIBLE
        }

        buildSelectMode(animateSetTraslation)
        constraintMain.addView(selectModeContainer)

        nav_izq = findViewById(R.id.nav_izq_song)
        nav_der = findViewById(R.id.nav_der_song)
        nav_back_Izq = findViewById(R.id.back_izq)
        nav_back_der = findViewById(R.id.back_der)

        video_fondo = findViewById(R.id.videoPreview)
        video_fondo.layoutParams.height = (height * 0.3).toInt()
        video_preview = MediaPlayer()
        video_fondo.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, w: Int, h: Int) {
                video_preview.setSurface(Surface(surface))
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, w: Int, h: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }

        next = findViewById(R.id.next)
        prev = findViewById(R.id.preview)

        next.layoutParams.height = (height * 0.3).toInt()
        prev.layoutParams.height = (height * 0.3).toInt()

        next.visibility = View.GONE
        prev.visibility = View.GONE

        nextPlayer = MediaPlayer().apply {
            setDataSource("$rutaBase/FingerDance/Themes/$tema/BGAs/next.mp4")
            isLooping = false
            setVolume(0f, 0f)
            prepare()
        }

        next.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, w: Int, h: Int) {
                nextPlayer.setSurface(Surface(surface))
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, w: Int, h: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
        prevPlayer = MediaPlayer().apply {
            setDataSource("$rutaBase/FingerDance/Themes/$tema/BGAs/prev.mp4")
            isLooping = false
            setVolume(0f, 0f)
            prepare()
        }
        prev.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, w: Int, h: Int) {
                prevPlayer.setSurface(Surface(surface))
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, w: Int, h: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

        }

        val spriteWidth = AppResources.arrowNavIzq.width / 2
        val spriteHeight = AppResources.arrowNavIzq.height / 2

        val navIzq = animaNavs(AppResources.arrowNavIzq, spriteWidth, spriteHeight)
        navIzq.start()
        val navDer = animaNavs(AppResources.arrowNavDer, spriteWidth, spriteHeight)
        navDer.start()
        val navBackIzq = animaNavs(AppResources.arrowBackIzqColor, spriteWidth, spriteHeight)
        navBackIzq.start()
        val navBackDer = animaNavs(AppResources.arrowBackDerColor, spriteWidth, spriteHeight)
        navBackDer.start()

        nav_izq.setImageDrawable(navIzq)
        nav_der.setImageDrawable(navDer)
        nav_back_Izq.setImageDrawable(navBackIzq)
        nav_back_der.setImageDrawable(navBackDer)

        llenaCommands(listCommands)

        //Por ahora solo se enviaran KSF
        //val listVacios = ArrayList<Lvs>()
        val listVacios = ArrayList<Ksf>()
        val rutaLvSelected = "$rutaBase/FingerDance/Themes/$tema/GraphicsStatics/img_lv_back.png"

        repeat(20) {
            listVacios.add(Ksf(steps = 0, rutaBitActive = rutaLvSelected))
        }
        llenaLvsVacios(listVacios)


        //setupRecyclerView((height * 0.06).toInt(), (width * 0.2).toInt())

        carouselSong.setSongs(AppResources.listSongsChannelKsf)
        oldValue = carouselSong.getSelectedIndex()
        isFocus(oldValue)


        //layoutManager = recyclerLvs.layoutManager as LinearLayoutManager
        imageCircle.layoutParams.width = (width * 0.95).toInt()
        imageCircle.layoutParams.height = imageCircle.layoutParams.width

        val medidaNavs = height / 8

        nav_back_Izq.layoutParams.width = medidaNavs
        nav_back_Izq.layoutParams.height = medidaNavs

        nav_back_der.layoutParams.width = medidaNavs
        nav_back_der.layoutParams.height = medidaNavs

        nav_izq.layoutParams.width = medidaNavs
        nav_izq.layoutParams.height = medidaNavs

        nav_der.layoutParams.width = medidaNavs
        nav_der.layoutParams.height = medidaNavs

        textSize = width / 15
        txCurrentBpm.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())

        textSize = width / 25
        lbNameSong.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
        lbNameSong.layoutParams.width = (width /2)

        textSize = width / 32
        lbBestScore.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
        lbWorldScore.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
        lbWorldName.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())

        textSize = width / 28

        lbArtist.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
        lbArtist.layoutParams.width = (width * 0.5).toInt()
        txInfoCW.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())

        lbBpm.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())

        textSize = width / 40
        lbCurrentBpm.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())

        imgLvSelected.layoutParams.width = (width / 4.1).toInt()

        imgFloor.layoutParams.width = (width * 0.6).toInt()
        imgAceptar.layoutParams.width = (width * 0.3).toInt()

        val rankingView = findViewById<TopRankingView>(R.id.topRankingView)
        rankingView.layoutParams.width = width.toInt()
        rankingView.visibility = View.INVISIBLE

        val linearRanking = LinearLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(0xAA000000.toInt())
            setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    // No hace nada
                }
            })
        }

        imgFavorite.setOnClickListener {
            saveFavorites = true
            val song = AppResources.listSongsChannelKsf[oldValue]
            val favChannel = listChannels.find { it.nombre == "06-FAVORITES" }

            if (song.isFavorite) {
                // Quitar de favoritos
                song.isFavorite = false
                imgFavorite.setImageBitmap(bitFavorite)

                val fav = listFavorites.find { it.title == song.title }
                if (fav != null) listFavorites.remove(fav)

                favChannel?.listCanciones = listFavorites

            } else {
                // Agregar a favoritos
                song.isFavorite = true
                imgFavorite.setImageBitmap(bitFavoriteListed)
                imgFavorite.startAnimation(AnimationUtils.loadAnimation(this, R.anim.stamp_effect))

                listFavorites.add(song)  // se agrega la MISMA referencia del objeto
                listFavorites.sortBy { it.channel }

                favChannel?.listCanciones = listFavorites
            }
        }

        imgBestScore.setOnClickListener{
            if(nivel.firstRank.isNotEmpty()){
                if(!commandWindow.isVisible){
                    soundPoolSelectSong.play(selectKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                    rankingView.visibility = View.VISIBLE
                    rankingView.startAnimation(animOn)
                    rankingView.setIconDrawable(imgLvSelected.drawable)
                    rankingView.setNiveles(nivel)
                    constraintMain.addView(linearRanking)
                    nav_back_der.bringToFront()
                    nav_back_Izq.bringToFront()
                    rankingView.bringToFront()
                }
            }
        }

        if(skinSelected != ""){
            if(!imgNoteSkin.isVisible){
                imgNoteSkin.isVisible=true
                imgNoteSkinFondo.isVisible=true
                val bm= BitmapFactory.decodeFile(skinSelected)
                if(bm!=null){
                    imgNoteSkin.setImageBitmap(bm)
                    playerSong.rutaNoteSkin = getRutaNoteSkin(skinSelected)
                }
            }
        }
        if(speedSelected != ""){
            txVelocidadActual.text = speedSelected
        }else{
            txVelocidadActual.text = "2.0X"
        }
        if(listEfectsDisplay.isNotEmpty()) {
            imgDisplay.isVisible = true
            listEfectsDisplay.forEach { effect ->
                when(effect.value){
                    "BGAOFF" -> playerSong.isBGAOff = true
                    "BGADARK" -> playerSong.isBAGDark = true
                    "FD" -> playerSong.fd = true
                    "V" -> playerSong.vanish = true
                    "AP" -> playerSong.ap = true
                    "RS" -> playerSong.rs = true
                    "M" -> playerSong.mirror = true
                    "SN" -> playerSong.snake = true
                }
            }
        }

        txOffset.text = valueOffset.toString()

        nav_back_Izq.setOnLongClickListener {
            ready = 0
            goSelectChannel()

            true
        }
        nav_back_der.setOnLongClickListener(){
            ready = 0
            goSelectChannel()

            true
        }

        nav_back_Izq.setOnClickListener() {
            ready = 0
            imgFloor.setImageBitmap(AppResources.bmFloor)
            if (carouselSong.isVisible && !commandWindow.isVisible) {
                Toast.makeText(this, "Manten presionado para volver al Selecet Channel", Toast.LENGTH_SHORT).show()
                soundPoolSelectSong.play(selectSong_movKsf, 1.0f, 1.0f, 1, 0, 1.0f)
            }
            if (imgLvSelected.isVisible && !commandWindow.isVisible && !rankingView.isVisible && !selectModeContainer.isVisible) {
                soundPoolSelectSong.play(up_SelectSoundKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                hideSelectLv(anim)
            }
            if(rankingView.isVisible){
                soundPoolSelectSong.play(up_SelectSoundKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                rankingView.visibility = View.INVISIBLE
                rankingView.startAnimation(animOff)
                constraintMain.removeView(linearRanking)
            }
            if (commandWindow.isVisible && !linearValues.isVisible ) {
                soundPoolSelectSong.play(command_backKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                showCommandWindow(false)
                oldValueCommand = 0
            }
            if (linearValues.isVisible) {
                soundPoolSelectSong.play(command_backKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                linearCurrent.isVisible = false
                linearValues.isVisible = false
                isFocusCommandWindow(oldValueCommand)
            }
            if (selectModeContainer.isVisible) {
                modeSelected = false
                selectModeContainer.visibility = View.INVISIBLE
                imgFloor.visibility = View.VISIBLE
                imgAceptar.visibility = View.VISIBLE
                txNameChannel.visibility = View.VISIBLE
                txPlayerName.visibility = View.VISIBLE
                imgAceptar.startAnimation(animateSetTraslation)
            }
        }
        nav_back_der.setOnClickListener() {
            ready = 0
            imgFloor.setImageBitmap(AppResources.bmFloor)
            if (carouselSong.isVisible && !commandWindow.isVisible) {
                //goSelectChannel()
                Toast.makeText(this, "Manten presionado para volver al Select Channel", Toast.LENGTH_SHORT).show()
                soundPoolSelectSong.play(selectSong_movKsf, 1.0f, 1.0f, 1, 0, 1.0f)
            }
            if (imgLvSelected.isVisible && !commandWindow.isVisible && !rankingView.isVisible && !selectModeContainer.isVisible) {
                soundPoolSelectSong.play(up_SelectSoundKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                hideSelectLv(anim)
            }
            if(rankingView.isVisible){
                soundPoolSelectSong.play(up_SelectSoundKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                rankingView.visibility = View.INVISIBLE
                rankingView.startAnimation(animOff)
                constraintMain.removeView(linearRanking)
            }
            if (commandWindow.isVisible && !linearValues.isVisible) {
                soundPoolSelectSong.play(command_backKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                showCommandWindow(false)
                oldValueCommand = 0
            }
            if (linearValues.isVisible) {
                soundPoolSelectSong.play(command_backKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                linearValues.isVisible = false
                linearCurrent.isVisible = false
                isFocusCommandWindow(oldValueCommand)
            }
            if (selectModeContainer.isVisible) {
                modeSelected = false
                selectModeContainer.visibility = View.INVISIBLE
                imgFloor.visibility = View.VISIBLE
                imgAceptar.visibility = View.VISIBLE
                txNameChannel.visibility = View.VISIBLE
                txPlayerName.visibility = View.VISIBLE
                imgAceptar.startAnimation(animateSetTraslation)
            }
        }

        nav_izq.setOnClickListener {
            ready = 0
            imgFloor.setImageBitmap(AppResources.bmFloor)
            if (carouselSong.isVisible && !commandWindow.isVisible) {

                carouselSong.moveLeft()
                oldValue = carouselSong.getSelectedIndex()
                moverCanciones(nav_izq, false)
            }
            if (imgLvSelected.isVisible && !commandWindow.isVisible) {
                if (handleButtonPress(false)) return@setOnClickListener
                soundPoolSelectSong.play(move_lvsKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                if (selectedIndex > 0) {
                    selectedIndex--
                    positionActualLvs = selectedIndex
                    moverLvs()
                    if (selectedIndex < firstVisible) {
                        firstVisible--
                    }
                    updateRecycler()

                }
            }
            if (commandWindow.isVisible && !linearValues.isVisible) {
                if (oldValueCommand == 0) {
                    isFocusCommandWindow(oldValueCommand)
                } else {
                    oldValueCommand--
                    isFocusCommandWindow(oldValueCommand)
                }
            }
            if (linearValues.isVisible) {
                if (oldValueCommandValues == 0) {
                    isFocusCommandWindowValues(oldValueCommandValues)
                } else {
                    oldValueCommandValues--
                    isFocusCommandWindowValues(oldValueCommandValues)
                }
            }
        }
        nav_der.setOnClickListener {
            ready = 0
            imgFloor.setImageBitmap(AppResources.bmFloor)
            if (carouselSong.isVisible && !commandWindow.isVisible) {

                carouselSong.moveRight()
                oldValue = carouselSong.getSelectedIndex()
                moverCanciones(nav_der, true)
            }
            if (imgLvSelected.isVisible && !commandWindow.isVisible) {
                if (handleButtonPress(true)) return@setOnClickListener
                soundPoolSelectSong.play(move_lvsKsf, 1.0f, 1.0f, 1, 0, 1.0f)

                val total = recyclerLvs.adapter!!.itemCount
                if (selectedIndex < total - 1) {
                    selectedIndex++
                    positionActualLvs = selectedIndex
                    moverLvs()
                    if (selectedIndex >= firstVisible + visibleItems) {
                        firstVisible++
                    }

                    updateRecycler()
                }
            }
            if (commandWindow.isVisible && !linearValues.isVisible) {
                if (oldValueCommand == listCommands.size - 1) {
                    isFocusCommandWindow(oldValueCommand)
                } else {
                    oldValueCommand++
                    isFocusCommandWindow(oldValueCommand)
                }
            }
            if (linearValues.isVisible) {
                if (oldValueCommandValues == listCommands[oldValueCommand].listCommandValues.size - 1) {
                    isFocusCommandWindowValues(oldValueCommandValues)
                } else {
                    oldValueCommandValues++
                    isFocusCommandWindowValues(oldValueCommandValues)

                }
            }
        }

        tipsArray = resources.getStringArray(R.array.tips_array)
        txTip = findViewById<TextView>(R.id.txTip)

        imgAceptar.setOnClickListener() {

            if (carouselSong.isVisible && !commandWindow.isVisible) {
                goSelectLevel()
            }
            if(imgLvSelected.isVisible && !commandWindow.isVisible){
                if(ready == 1 && !modeSelected){
                    soundPoolSelectSong.play(selectKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                    txNameChannel.visibility = View.INVISIBLE
                    txPlayerName.visibility = View.INVISIBLE
                    showSelectMode()
                }
                if(ready == 1 && modeSelected){
                    goGameScreenActivity(anim, txPlayerName, txNameChannel)
                }
                imgAceptar.isEnabled = true
                if(ready == 0){
                    ready = 1
                    imgFloor.setImageBitmap(AppResources.bmFloor2)
                    soundPoolSelectSong.play(selectKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                }
            }
            val itemCommand = listCommands[oldValueCommand]
            if(linearValues.isVisible){
                soundPoolSelectSong.play(command_modKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                val itemValues = listCommands[oldValueCommand].listCommandValues[oldValueCommandValues]
                if(itemCommand.value.contains("Speed", ignoreCase = true)){
                    if(itemValues.value == "0"){
                        txCurrentBpm.text = "2.0X"
                        txVelocidadActual.text = txCurrentBpm.text
                    }else{
                        val valorActual = if(txVelocidadActual.text != "2.0X") {
                            txVelocidadActual.text.toString().replace("X", "").toBigDecimal()
                        }else {
                            txCurrentBpm.text.toString().replace("X", "").toBigDecimal()
                        }
                        val result = valorActual + itemValues.value.toBigDecimal()
                        val formattedResult = result.stripTrailingZeros().toPlainString() + "X"

                        if(itemValues.value.toDouble()<0){
                            if(result <= BigDecimal(0.5)) {
                                txCurrentBpm.text = "0.5X"
                                //txVelocidadActual.text = txCurrentBpm.text
                            }else{
                                txCurrentBpm.text = formattedResult //result.toString() + "X"
                                //txVelocidadActual.text = txCurrentBpm.text
                            }
                        }else{
                            if(result >= BigDecimal(8.0)) {
                                txCurrentBpm.text = "8.0X"
                                //txVelocidadActual.text = txCurrentBpm.text
                            }else{
                                txCurrentBpm.text = formattedResult //result.toString() + "X"
                                //txVelocidadActual.text = txCurrentBpm.text
                            }
                        }
                        txVelocidadActual.text = txCurrentBpm.text
                        speedSelected = txVelocidadActual.text.toString()
                        themes.edit().putString("speed", txVelocidadActual.text.toString()).apply()
                        themes.edit().putString("typeSpeed", "").apply()
                    }
                }
                if(itemCommand.value.contains("Offset", ignoreCase = true)){
                    val valorActual = if(txOffset.text == "0") 0 else txOffset.text.toString().toLong()
                    txCurrentBpm.text = valorActual.toString()
                    if(itemValues.value == "0"){
                        txCurrentBpm.text = "0"
                        txOffset.text = txCurrentBpm.text
                    }else {
                        val result = valorActual.toInt() + itemValues.value.toLong()
                        txCurrentBpm.text = result.toString()
                        txOffset.text = txCurrentBpm.text
                    }
                    valueOffset = txOffset.text.toString().toLong()
                    themes.edit().putLong("valueOffset", valueOffset).apply()
                }
                if(itemCommand.value.contains("Display", ignoreCase = true)){
                    linearCurrent.isVisible = false
                    imgDisplay.isVisible=true
                    val existEffect = listEfectsDisplay.find { e -> e.value == itemValues.value }
                    if (existEffect != null) {
                        listEfectsDisplay.remove(existEffect)
                        when (existEffect.value) {
                            "BGAOFF" -> playerSong.isBGAOff = false
                            "BGADARK" -> playerSong.isBAGDark = false
                            "FD" -> playerSong.fd = false
                            "V" -> playerSong.vanish = false
                            "AP" -> playerSong.ap = false
                            "SN" -> playerSong.snake =  false
                        }
                    } else {
                        when (itemValues.value) {
                            "BGAOFF" -> {
                                listEfectsDisplay.add(itemValues)
                                playerSong.isBGAOff = true
                                val isBGADark = listEfectsDisplay.find { it.value == "BGADARK" }
                                if (isBGADark != null) {
                                    listEfectsDisplay.remove(isBGADark)
                                    playerSong.isBAGDark = false
                                }
                            }
                            "BGADARK" -> {
                                listEfectsDisplay.add(itemValues)
                                playerSong.isBAGDark = true
                                val isBGAOFF = listEfectsDisplay.find { it.value == "BGAOFF" }
                                if (isBGAOFF != null) {
                                    listEfectsDisplay.remove(isBGAOFF)
                                    playerSong.isBGAOff = false
                                }
                            }
                            "FD" -> {
                                listEfectsDisplay.add(itemValues)
                                playerSong.fd = true
                            }
                            "V" -> {
                                listEfectsDisplay.add(itemValues)
                                playerSong.vanish = true
                                val isAp = listEfectsDisplay.find { it.value == "AP" }
                                if (isAp != null) {
                                    listEfectsDisplay.remove(isAp)
                                    playerSong.ap = false
                                }
                                val isSnake = listEfectsDisplay.find { it.value == "SN" }
                                if (isSnake != null) {
                                    listEfectsDisplay.remove(isSnake)
                                    playerSong.snake = false
                                }
                            }
                            "AP" -> {
                                listEfectsDisplay.add(itemValues)
                                playerSong.ap = true
                                val isVanish = listEfectsDisplay.find { it.value == "V" }
                                if (isVanish != null) {
                                    listEfectsDisplay.remove(isVanish)
                                    playerSong.vanish = false
                                }
                                val isSnake = listEfectsDisplay.find { it.value == "SN" }
                                if (isSnake != null) {
                                    listEfectsDisplay.remove(isSnake)
                                    playerSong.snake = false
                                }
                            }
                        }
                        imgDisplay.setImageBitmap(BitmapFactory.decodeFile(itemValues.rutaCommandImg))
                    }
                    resetRunnable()
                    imgDisplay.isVisible = listEfectsDisplay.isNotEmpty()

                }
                if(itemCommand.value.contains("Alternate", ignoreCase = true)){
                    linearCurrent.isVisible = false
                    imgDisplay.isVisible=true
                    val existEffect = listEfectsDisplay.find { e -> e.value == itemValues.value }
                    if (existEffect != null) {
                        listEfectsDisplay.remove(existEffect)
                        when (existEffect.value) {
                            "RS" -> playerSong.rs = false
                            "M" -> playerSong.mirror = false
                        }
                    } else {
                        when (itemValues.value) {
                            "RS" -> {
                                listEfectsDisplay.add(itemValues)
                                playerSong.rs = true
                                val isMirror = listEfectsDisplay.find { it.value == "M" }
                                if (isMirror != null) {
                                    listEfectsDisplay.remove(isMirror)
                                    playerSong.mirror = false
                                }
                            }
                            "M" -> {
                                listEfectsDisplay.add(itemValues)
                                playerSong.mirror = true
                                val isRS = listEfectsDisplay.find { it.value == "RS" }
                                if (isRS != null) {
                                    listEfectsDisplay.remove(isRS)
                                    playerSong.rs = false
                                }
                            }
                        }
                        imgDisplay.setImageBitmap(BitmapFactory.decodeFile(itemValues.rutaCommandImg))
                    }
                    resetRunnable()
                    imgDisplay.isVisible = listEfectsDisplay.isNotEmpty()

                }
                if(itemCommand.value.contains("Path", ignoreCase = true)){
                    linearCurrent.isVisible = false
                    imgDisplay.isVisible=true
                    val existEffect = listEfectsDisplay.find { e -> e.value == itemValues.value }
                    if (existEffect != null) {
                        listEfectsDisplay.remove(existEffect)
                        when (existEffect.value) {
                            "SN" -> playerSong.snake = false
                        }
                    } else {
                        listEfectsDisplay.add(itemValues)
                        playerSong.snake = true
                        val isAP = listEfectsDisplay.find { it.value == "AP" }
                        if (isAP != null) {
                            listEfectsDisplay.remove(isAP)
                            playerSong.ap = false
                        }
                        val isVanish = listEfectsDisplay.find { it.value == "V" }
                        if (isVanish != null) {
                            listEfectsDisplay.remove(isVanish)
                            playerSong.vanish = false
                        }
                        imgDisplay.setImageBitmap(BitmapFactory.decodeFile(itemValues.rutaCommandImg))
                    }
                    resetRunnable()
                    imgDisplay.isVisible = listEfectsDisplay.isNotEmpty()

                }

                val bm= BitmapFactory.decodeFile(itemValues.rutaCommandImg)
                if(itemCommand.value.contains("NoteSkin", ignoreCase = true)){
                    linearCurrent.isVisible = false
                    if(!imgNoteSkin.isVisible){
                        imgNoteSkin.isVisible=true
                        imgNoteSkinFondo.isVisible=true
                        if(bm!=null){
                            imgNoteSkin.setImageBitmap(bm)
                            playerSong.rutaNoteSkin = getRutaNoteSkin(itemValues.rutaCommandImg)
                            //themes.edit().putString("skin", itemValues.rutaCommandImg).apply()
                        }
                    }else{
                        imgNoteSkin.setImageBitmap(bm)
                        playerSong.rutaNoteSkin = getRutaNoteSkin(itemValues.rutaCommandImg)

                    }
                    themes.edit().putString("skin", itemValues.rutaCommandImg).apply()
                    skinSelected = itemValues.rutaCommandImg
                }
                if(itemCommand.value.contains("Judge", ignoreCase = true)){
                    linearCurrent.isVisible = false
                    if(!imgJudge.isVisible){
                        imgJudge.isVisible=true
                        imgJudge.setImageBitmap(bm)
                        playerSong.hj = true
                        playerSong.pathImgHJ = itemValues.rutaCommandImg
                    }else{
                        imgJudge.isVisible=false
                        playerSong.hj = false
                        playerSong.pathImgHJ = ""
                    }
                }
            }
            if (commandWindow.isVisible && !linearValues.isVisible) {
                linearValues.isVisible = true
                linearCurrent.isVisible = true
                var size = 0
                if(itemCommand.value.contains("Offset", ignoreCase = true)
                    || itemCommand.value.contains("Speed", ignoreCase = true)){
                    size = (listCommands[oldValueCommand].listCommandValues.size - 1) / 2
                }
                if(size.toString().length == 2){
                    size.toDouble().roundToInt()
                    oldValueCommandValues = size
                }else{
                    oldValueCommandValues = size
                }
                isFocusCommandWindow(oldValueCommand)
                isFocusCommandWindowValues(size)
            }
        }

        imgPrev.setOnLongClickListener {
            showOverlay(false)
            true
        }
        video_fondo.setOnLongClickListener {
            showOverlay(true)
            true
        }

        imgOffset.setOnClickListener {

            //showModifyOffsetDialog()

        }

        if(isVideo){
            video_preview.start()
        }
        mediPlayer.start()
    }

    private fun showModifyOffsetDialog() {
        val fileSsc = File(AppResources.listSongsChannelKsf[oldValue % AppResources.listSongsChannelKsf.size].rutaSsc)
        val original = readFileSsc(fileSsc.absolutePath)
        val charts = parseSscCharts(original)
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_offsets, null)

        val recycler = dialogView.findViewById<RecyclerView>(R.id.recyclerCharts)
        val checkAll = dialogView.findViewById<CheckBox>(R.id.checkAll)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        val adapter = ChartOffsetAdapter(charts)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter
        checkAll.setOnCheckedChangeListener { _, checked ->
            charts.forEach {
                it.checked = checked
            }
            adapter.notifyDataSetChanged()
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnSave.setOnClickListener {

            val updated = processSscOffsets(
                original,
                charts.filter { it.checked },
                valueOffset.toInt()
            )

            fileSsc.writeText(updated)

            exportModifiedSscToPublicFingerDance(
                sourceSsc = fileSsc,
                updatedContent = updated
            )

            dialog.dismiss()
        }

        dialog.show()
    }

    private fun parseSscCharts(content: String): MutableList<SscChart> {

        val result = mutableListOf<SscChart>()

        val blocks = content.split("#NOTEDATA:;")

        blocks.forEachIndexed { index, block ->

            if (!block.contains("#STEPSTYPE:"))
                return@forEachIndexed

            fun get(tag: String): String {

                return Regex("#$tag:(.*?);").find(block)?.groupValues?.get(1)?.trim() ?: ""
            }

            val stepType = get("STEPSTYPE")

            if (stepType == "pump-double") return@forEachIndexed

            result.add(
                SscChart(
                    blockIndex = index,
                    stepType = stepType,
                    level = get("METER"),
                    difficulty = get("DIFFICULTY"),
                    description = get("DESCRIPTION"),
                    chartName = get("CHARTNAME"),
                    credit = get("CREDIT"),
                    offset = get("OFFSET").toDoubleOrNull() ?: 0.0
                )
            )
        }

        return result
    }

    private fun processSscOffsets(content: String, selected: List<SscChart>, valueOffset: Int): String {
        val blocks = content.split("#NOTEDATA:;").toMutableList()
        selected.forEach { chart ->
            val index = chart.blockIndex

            if (index >= blocks.size)
                return@forEach

            val block = blocks[index]
            val regex = Regex("#OFFSET:([-+]?[0-9]*\\.?[0-9]+);")

            val match = regex.find(block)
            val currentOffset = match?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0

            val newOffset = currentOffset + ((valueOffset * 10) / 1000.0)
            val formatted = String.format("%.6f", newOffset)
            val newBlock = if (match != null) {
                regex.replace(block) {
                    "#OFFSET:$formatted;"
                }
            } else {
                block + "\n#OFFSET:$formatted;"
            }
            blocks[index] = newBlock
        }

        return blocks.joinToString("#NOTEDATA:;")
    }

    private fun exportModifiedSscToPublicFingerDance(sourceSsc: File, updatedContent: String) {
        val normalizedPath = sourceSsc.absolutePath.replace("\\", "/")

        val marker = "/Channels/"
        val index = normalizedPath.indexOf(marker)

        if (index == -1) {
            throw IllegalStateException("No se encontró /Channels/ en la ruta: $normalizedPath")
        }

        val relativeAfterChannels = normalizedPath.substring(index + marker.length)
        val parts = relativeAfterChannels.split("/")

        if (parts.size < 3) {
            throw IllegalStateException("Ruta SSC inválida: $normalizedPath")
        }

        val channelName = parts[0]          // 17-PRIME
        val songFolderName = parts[1]       // 1401 - Nemesis
        val sscFileName = sourceSsc.name    // UCS Lv.19.ssc

        val targetDir = File(
            Environment.getExternalStorageDirectory(),
            "Finger Dance/Songs/Channels/$channelName/$songFolderName"
        )

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val targetFile = File(targetDir, sscFileName)

        targetFile.writeText(updatedContent)
        Toast.makeText(this, "Offset modificado y copia guardada", Toast.LENGTH_SHORT).show()
        txCurrentBpm.text = "0"
        txOffset.text = txCurrentBpm.text
        valueOffset = txOffset.text.toString().toLong()
        themes.edit().putLong("valueOffset", valueOffset).apply()
    }

    private fun showSelectMode() {
        // 🔥 sube los controles SOBRE el overlay
        selectModeContainer.visibility = View.VISIBLE
        imgAceptar.bringToFront()
        imgFloor.bringToFront()
        nav_back_Izq.bringToFront()
        nav_back_der.bringToFront()
        imgFloor.visibility = View.INVISIBLE
        imgAceptar.visibility = View.INVISIBLE
        imgAceptar.animation = null
    }

    private fun buildSelectMode(animateSetTraslation: Animation){
        val hand = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(medidaFlechas.toInt(), medidaFlechas.toInt(), Gravity.TOP or Gravity.CENTER_HORIZONTAL)
            setImageBitmap(BitmapFactory.decodeStream(assets.open("hand_tap_here.png"))) // tu imagen
            alpha = 0f
        }

        val aceptarTop = imgAceptar.top.takeIf { it > 0 }
            ?: (resources.displayMetrics.heightPixels * 0.8f).toInt()

        val availableHeight = aceptarTop

        val verticalLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                availableHeight
            ).apply {
                topMargin = (resources.displayMetrics.heightPixels * 0.06f).toInt()
            }
        }

        selectModeContainer.addView(verticalLayout)
        selectModeContainer.addView(hand)

        // 🔥 PRELOAD (IMPORTANTE)
        val hPrev = BitmapFactory.decodeStream(assets.open("horizontal_mode_prev.png"))
        val hSelect = BitmapFactory.decodeStream(assets.open("horizontal_mode_select.png"))

        val vPrev = BitmapFactory.decodeStream(assets.open("vertical_mode_prev.png"))
        val vSelect = BitmapFactory.decodeStream(assets.open("vertical_mode_select.png"))

        // 🔥 referencias
        lateinit var imgHorizontal: ImageView
        lateinit var imgVertical: ImageView

        fun createSection(
            initialBitmap: Bitmap,
            assignRef: (ImageView) -> Unit,
            onClick: () -> Unit
        ): FrameLayout {

            val section = FrameLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
            }

            val image = ImageView(this).apply {
                layoutParams = FrameLayout.LayoutParams(
                    (resources.displayMetrics.widthPixels * 0.75f).toInt(),
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    Gravity.CENTER
                )
                scaleType = ImageView.ScaleType.FIT_XY
                setImageBitmap(initialBitmap)
            }

            assignRef(image)

            image.setOnClickListener {
                onClick()
            }

            section.addView(image)
            return section
        }

        // 🔽 vertical
        val topSection = createSection(vPrev, { imgVertical = it }) {
            isHandRunning = false
            hand.animate().cancel()
            hand.visibility = View.GONE
            imgFloor.visibility = View.VISIBLE
            imgAceptar.visibility = View.VISIBLE
            imgAceptar.startAnimation(animateSetTraslation)
            modeSelected = true
            orientationMode = OrientationMode.VERTICAL

            // 🔥 actualizar imágenes
            imgVertical.setImageBitmap(vSelect)
            imgHorizontal.setImageBitmap(hPrev)
        }

        // 🔝 horizontal
        val bottomSection = createSection(hPrev, { imgHorizontal = it }) {
            isHandRunning = false
            hand.animate().cancel()
            hand.visibility = View.GONE
            imgFloor.visibility = View.VISIBLE
            imgAceptar.visibility = View.VISIBLE
            imgAceptar.startAnimation(animateSetTraslation)
            modeSelected = true
            orientationMode = OrientationMode.HORIZONTAL

            // 🔥 actualizar imágenes
            imgHorizontal.setImageBitmap(hSelect)
            imgVertical.setImageBitmap(vPrev)
        }

        val textInfo = TextView(this).apply {
            text = "Selecciona como quieres jugar"
            setTextColor(Color.WHITE)
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 20, 0, 20)
            setTypeface(typeface, Typeface.BOLD)

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        verticalLayout.addView(topSection)
        verticalLayout.addView(textInfo)
        verticalLayout.addView(bottomSection)

        selectModeContainer.post {
            isHandRunning = true
            startHandAnimation(hand, imgVertical, imgHorizontal)
        }
    }

    var isHandRunning = false
    private fun startHandAnimation(hand: View, top: View, bottom: View) {
        hand.x = (selectModeContainer.width / 2f) - (hand.width / 2f)
        hand.y = getCenterY(top) - hand.height / 2f

        fun moveAndTap(targetY: Float, onEnd: () -> Unit) {

            hand.animate().cancel()

            hand.animate()
                .alpha(1f)
                .y(targetY - hand.height / 2)
                .setDuration(1000)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .withEndAction {

                    // tap 1
                    hand.animate()
                        .scaleX(0.85f)
                        .scaleY(0.85f)
                        .setDuration(80)
                        .withEndAction {

                            // regreso
                            hand.animate()
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(80)
                                .withEndAction {

                                    // tap 2
                                    hand.animate()
                                        .scaleX(0.85f)
                                        .scaleY(0.85f)
                                        .setDuration(80)
                                        .withEndAction {

                                            hand.animate()
                                                .scaleX(1f)
                                                .scaleY(1f)
                                                .setDuration(80)
                                                .withEndAction {
                                                    hand.postDelayed({
                                                        onEnd()
                                                    }, 150) // 🔥 pausa natural
                                                }
                                        }
                                }
                        }
                }
        }
        fun loop() {
            if (!isHandRunning) return

            moveAndTap(getCenterY(top)) {
                if (!isHandRunning) return@moveAndTap

                moveAndTap(getCenterY(bottom)) {
                    loop()
                }
            }
        }

        loop()
    }

    private fun getCenterY(v: View): Float {
        val loc = IntArray(2)
        v.getLocationOnScreen(loc)

        val containerLoc = IntArray(2)
        selectModeContainer.getLocationOnScreen(containerLoc)

        return (loc[1] - containerLoc[1]) + v.height / 2f
    }

    private fun goGameScreenActivity(anim: Animation, txPlayerName: TextView, txNameChannel: TextView) {
        val real = getRealIndex(oldValue)
        val song = AppResources.listSongsChannelKsf[real]
        soundPoolSelectSong.play(startKsf, 1.0f, 1.0f, 1, 0, 1.0f)
        imgAceptar.isEnabled = false

        val bit = BitmapFactory.decodeFile(song.rutaDisc)
        imgLoading.setImageBitmap(bit)
        linearLoading.isVisible = true
        linearLoading.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                // No hace nada
            }
        })
        imgLoading.isVisible = true
        showProgressBar(3000L)
        mediPlayer.pause()
        playerSong.rutaBanner = song.rutaTitle

        txTip.text = tipsArray[Random.nextInt(tipsArray.size)]

        playerSong.speed = txVelocidadActual.text.toString()
        if(playerSong.rutaNoteSkin != ""){
            ruta = playerSong.rutaNoteSkin!!
        }else{
            val directorioBase = "$rutaBase/FingerDance/NoteSkins"
            val directorios = File(directorioBase).listFiles { file ->
                file.isDirectory && file.name.contains("default", ignoreCase = true)
            }
            if (directorios != null) {
                ruta = directorios.firstOrNull().toString()
                playerSong.rutaNoteSkin = ruta
            }
        }
        hideSelectLv(anim)
        playerSong.rutaVideo = song.rutaBGA
        playerSong.rutaCancion = song.rutaSong

        if(song.listKsf[positionActualLvs].songFile != ""){
            playerSong.rutaCancion = File(playerSong.rutaCancion!!).parent!! + "/" + song.listKsf[positionActualLvs].songFile
        }

        if(!isFileExists(File(playerSong.rutaCancion!!))) {
            val rs = File(song.rutaSong).name
            val sf = File(playerSong.rutaCancion!!).name
            playerSong.rutaCancion = playerSong.rutaCancion!!.replace(sf, rs, ignoreCase = true)
        }

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setDataSource(File(playerSong.rutaCancion!!).absolutePath)
            prepare()
        }

        val level = song.listKsf[positionActualLvs]
        val isHalfDouble = level.typePlayer == "B"
        playerSong.level = level.level
        playerSong.player = level.typePlayer
        playerSong.type = level.typeSteps
        playerSong.chartName = level.chartName
        playerSong.stepMaker = level.stepmaker
        playerSong.difficulty = level.difficulty
        if(song.isSSC){
            val ssc = readFileSsc(song.rutaSsc)
            val seccions = ssc.split("#NOTEDATA:;")
            chart = Parser().parseSSC(
                    "${seccions[0]}\n",
                    seccions[level.steps],
                    song.rutaSong
                )
            playerSong.isSSC = true
            val uniqueId = generateId("${playerSong.level}|${playerSong.type}|${playerSong.player}|${playerSong.chartName}|${playerSong.stepMaker}|${playerSong.difficulty}")
            checkedValuesLocal = generateCheckedValuesSsc(seccions[level.steps]) + "|" + File(playerSong.rutaCancion!!).length() + "-$uniqueId"
            playerSong.checkedValues = validateOficialSong(checkedValuesLocal)
            isOficialSong = playerSong.checkedValues != ""

            if(playerSong.mirror){
                if(!isHalfDouble){
                    chart.notes = Parser().makeMirror(chart.notes)
                }else{
                    chart.notes = Parser().makeMirrorHD(chart.notes)
                }
            }
            if(playerSong.rs){
                if(!isHalfDouble){
                    chart.notes = Parser().makeRandom(chart.notes)
                }else{
                    chart.notes = Parser().makeRandomHD(chart.notes)
                }
            }
        }else{
            playerSong.rutaKsf = level.rutaKsf
            playerSong.isSSC = false
            load(playerSong.rutaKsf, isHalfDouble)
            val uniqueId = generateId("${playerSong.level}|${playerSong.type}|${playerSong.player}|${playerSong.chartName}|${playerSong.stepMaker}|${playerSong.difficulty}")
            checkedValuesLocal = generateCheckedValuesKsf(File(playerSong.rutaKsf)) + "|" + File(playerSong.rutaCancion!!).length() + "-$uniqueId"
            playerSong.checkedValues = validateOficialSong(checkedValuesLocal)
            isOficialSong = playerSong.checkedValues != ""

            if(playerSong.mirror){
                if(!isHalfDouble){
                    ksf.makeMirror()
                }else{
                    ksfHD.makeMirror()
                }

            }
            if(playerSong.rs){
                if(!isHalfDouble){
                    ksf.makeRandom()
                }else{
                    ksfHD.makeRandom()
                }
            }
        }

        if(!isOnline){
            if(!isOffline){
                levelIndex = positionActualLvs
            }
        }

        handler.postDelayed({
            val intent = Intent(
                this,
                if (orientationMode == OrientationMode.VERTICAL)
                    GameScreenActivity()::class.java
                else
                    GameScreenActivityHorizontal()::class.java
            )

            intent.putExtra("IS_HALF_DOUBLE", isHalfDouble)
            intent.putExtra("IS_VERTICAL", true)
            startActivity(intent)

            handler.postDelayed({
                linearLoading.isVisible = false
                imgLoading.isVisible = false
                txNameChannel.visibility = View.VISIBLE
                txPlayerName.visibility = View.VISIBLE
            }, 1000L)
            initGameScreen = true
            ready = 0
            modeSelected = false
            selectModeContainer.visibility = View.INVISIBLE
            imgFloor.setImageBitmap(AppResources.bmFloor)

        }, 4000L)
    }

    private fun updateRecycler() {
        val lm = recyclerLvs.layoutManager as LinearLayoutManager
        recyclerLvs.post {
            lm.scrollToPositionWithOffset(firstVisible, 0)
        }
        val indicatorPosition = selectedIndex - firstVisible
        indicatorLayout.x = (indicatorPosition * sizeLvs).toFloat()
    }

    private fun getRealIndex(pos: Int): Int {
        val size = AppResources.listSongsChannelKsf.size
        return ((pos % size) + size) % size
    }

    private fun resetIndicatorPosition() {
        selectedIndex = 0
        firstVisible = 0
        positionActualLvs = 0
        val lm = recyclerLvs.layoutManager as LinearLayoutManager
        lm.scrollToPositionWithOffset(0, 0)
        indicatorLayout.x = 0f
    }

    private var downloadJob: Job? = null
    private fun showOverlay(isBGA: Boolean) {
        overlayBG = View(this).apply {
            setBackgroundColor(0xDD000000.toInt())
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    // No hace nada
                }
            })
        }

        buttonLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val dpToPx = { dp: Int ->
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp.toFloat(),
                resources.displayMetrics
            ).toInt()
        }

        val cardLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dpToPx(24), dpToPx(32), dpToPx(24), dpToPx(32))
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(340),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(20).toFloat()
                setColor(0xFF1A1A2E.toInt())
                setStroke(dpToPx(2), 0xFF00D9FF.toInt())
            }

            elevation = dpToPx(16).toFloat()
        }

        // ---------- TÍTULO ----------
        val titleText = TextView(this).apply {
            text = "CONTENIDO MULTIMEDIA"
            textSize = 18f
            setTextColor(0xFF00D9FF.toInt())
            gravity = Gravity.CENTER
            setTypeface(null, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dpToPx(24)
            }
        }

        // ---------- PROGRESS UI ----------
        val progressText = TextView(this).apply {
            text = "Descargando 0%"
            textSize = 16f
            setTextColor(0xFF00FFAA.toInt())
            gravity = Gravity.CENTER
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dpToPx(12)
            }
        }

        val progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            visibility = View.GONE
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(10)).apply {
                bottomMargin = dpToPx(20)
            }
            progressDrawable.setColorFilter(0xFF00D9FF.toInt(), PorterDuff.Mode.SRC_IN)
        }

        // ---------- CANCEL ----------
        val btnCancel = Button(this).apply {
            text = "CANCELAR"
            setTextColor(0xFFFF4444.toInt())
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            isAllCaps = false

            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(12).toFloat()
                setColor(0x33FF4444.toInt())
                setStroke(dpToPx(2), 0xFFFF4444.toInt())
            }

            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(48)
            )

            setOnClickListener {
                downloadJob?.cancel()
                setOverlayEnabled(true, cardLayout)
                constraintMain.removeView(overlayBG)
                constraintMain.removeView(buttonLayout)
            }
        }

        // ---------- DRIVE DATA ----------
        val videosDrive = getVideosPreview()
        var idPreview = ""
        var sizePreview = ""
        var idBga = ""
        var sizeBga = ""
        var existPreviewDrive = false
        var existBgaDrive = false
        var previewDowloaded = false
        var bgaDowloaded = false
        var textBtnDownloadPreview = ""
        var textBtnDownloadBga = ""

        if (videosDrive.isNotEmpty()) {
            existPreviewDrive = videosDrive.any { it.name == "song_p.mp4" }
            if (existPreviewDrive) {
                val p = videosDrive.first { it.name == "song_p.mp4" }
                idPreview = p.id
                sizePreview = p.size
            }

            existBgaDrive = videosDrive.any { it.name == "song.mp4" }
            if (existBgaDrive) {
                val b = videosDrive.first { it.name == "song.mp4" }
                idBga = b.id
                sizeBga = b.size
            }
        }

        if(File(AppResources.listSongsChannelKsf[oldValue].rutaPreview).exists() && AppResources.listSongsChannelKsf[oldValue].rutaPreview.endsWith(".mp4")){
            previewDowloaded = true
            textBtnDownloadPreview = "Eliminar Preview"
        }else{
            previewDowloaded = false
            textBtnDownloadPreview = "Descargar Preview " + if(sizePreview.isNotEmpty()) "(${ "%.2f".format(sizePreview.toLong() / (1024.0 * 1024.0))} MB)" else ""
        }

        if(File(AppResources.listSongsChannelKsf[oldValue].rutaBGA).exists() && AppResources.listSongsChannelKsf[oldValue].rutaPreview.endsWith(".mp4")){
            bgaDowloaded = true
            textBtnDownloadBga = "Eliminar BGA"
        }else{
            bgaDowloaded = false
            textBtnDownloadBga = "Descargar BGA " + if(sizeBga.isNotEmpty()) "(${ "%.2f".format(sizeBga.toLong() / (1024.0 * 1024.0))} MB)" else ""
        }

        // ---------- FILA PREVIEW ----------
        val previewRowData = createMediaRow(
            imageAsset = "preview.png",
            mainButtonText = if(previewDowloaded) "Reemplazar Preview" else "Agregar Preview",
            downloadButtonText = textBtnDownloadPreview,
            onMainClick = { pickPreviewFile.launch(arrayOf("video/mp4")) },
            existDownloaded = previewDowloaded,
            existInDrive = existPreviewDrive,
            onDownloadClick = if(previewDowloaded){
                {
                    File(AppResources.listSongsChannelKsf[oldValue].rutaPreview).delete()
                    isFocus(oldValue )
                    btnCancel.performClick()
                    mediPlayer.start()
                    Toast.makeText(this, "Preview eliminado", Toast.LENGTH_SHORT).show()
                }
            }else{
                {
                    downloadJob = lifecycleScope.launch {
                        setOverlayEnabled(false, cardLayout)
                        progressText.visibility = View.VISIBLE
                        progressBar.visibility = View.VISIBLE
                        val result = downloadVideoFromDrive(
                            fileId = idPreview,
                            isBGA = false
                        ) { progress ->
                            progressBar.progress = progress
                            progressText.text = "Descargando $progress%"
                        }

                        if (result != null) {
                            progressText.text = "¡Descarga completa!"
                            delay(600)
                            isFocus(oldValue)
                            btnCancel.performClick()
                            video_preview.start()
                            mediPlayer.start()
                        } else {
                            setOverlayEnabled(true, cardLayout)
                            progressText.text = "Error al descargar"
                        }
                    }
                }
            }
        )

        // ---------- FILA BGA ----------
        val bgaRowData = createMediaRow(
            imageAsset = "bga.png",
            mainButtonText = if(bgaDowloaded) "Reemplazar BGA" else "Agregar BGA",
            downloadButtonText = textBtnDownloadBga,
            onMainClick = { pickBgaFile.launch(arrayOf("video/mp4")) },
            existDownloaded = bgaDowloaded,
            existInDrive = existBgaDrive,
            onDownloadClick = if(bgaDowloaded){
                {
                    File(AppResources.listSongsChannelKsf[oldValue].rutaBGA).delete()
                    isFocus(oldValue)
                    btnCancel.performClick()
                    mediPlayer.start()
                    Toast.makeText(this, "BGA eliminado", Toast.LENGTH_SHORT).show()
                }
            }else{
                {
                    downloadJob = lifecycleScope.launch {
                        setOverlayEnabled(false, cardLayout)
                        progressText.visibility = View.VISIBLE
                        progressBar.visibility = View.VISIBLE

                        val result = downloadVideoFromDrive(
                            fileId = idBga,
                            isBGA = true
                        ) { progress ->
                            progressBar.progress = progress
                            progressText.text = "Descargando $progress%"
                        }

                        if (result != null) {
                            progressText.text = "¡Descarga completa!"
                            delay(600)
                            btnCancel.performClick()
                        } else {
                            setOverlayEnabled(true, cardLayout)
                            progressText.text = "Error al descargar"
                        }
                    }
                }
            }
        )

        // Separador
        val divider1 = View(this).apply {
            setBackgroundColor(0x33FFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1)
            ).apply {
                topMargin = dpToPx(16)
                bottomMargin = dpToPx(16)
            }
        }

        // Separador
        val divider2 = View(this).apply {
            setBackgroundColor(0x33FFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1)
            ).apply {
                topMargin = dpToPx(16)
                bottomMargin = dpToPx(16)
            }
        }

        // Separador
        val divider3 = View(this).apply {
            setBackgroundColor(0x33FFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dpToPx(1)
            ).apply {
                topMargin = dpToPx(16)
                bottomMargin = dpToPx(16)
            }
        }

        // ---------- ORDEN CORRECTO ----------
        cardLayout.addView(titleText)
        cardLayout.addView(previewRowData.first)
        cardLayout.addView(divider1)
        cardLayout.addView(bgaRowData.first)
        cardLayout.addView(divider2)
        cardLayout.addView(progressText)
        cardLayout.addView(progressBar)
        cardLayout.addView(divider3)
        cardLayout.addView(btnCancel)

        buttonLayout.addView(cardLayout)
        constraintMain.addView(overlayBG)
        constraintMain.addView(buttonLayout)
    }

    private fun setOverlayEnabled(enabled: Boolean, cardLayout: LinearLayout) {
        cardLayout.isEnabled = enabled
        cardLayout.alpha = if (enabled) 1f else 0.6f
        // Fila de Preview (índice 1)
        val rowPreview = cardLayout.getChildAt(1) as LinearLayout
        val buttonsContainerPreview = rowPreview.getChildAt(1) as LinearLayout // ImageView está en índice 0, buttons en 1
        buttonsContainerPreview.getChildAt(0).isEnabled = enabled // Main button
        buttonsContainerPreview.getChildAt(1).isEnabled = enabled // Download button

        // Fila de BGA (índice 3)
        val rowBGA = cardLayout.getChildAt(3) as LinearLayout
        val buttonsContainerBGA = rowBGA.getChildAt(1) as LinearLayout // ImageView está en índice 0, buttons en 1
        buttonsContainerBGA.getChildAt(0).isEnabled = enabled // Main button
        buttonsContainerBGA.getChildAt(1).isEnabled = enabled // Download button
    }

    private fun getVideosPreview(): ArrayList<VideosDrive> {
        val listSongsDrive = listChannelsDrive.find { it.name == currentChannel.replace("-SSC", "") }?.songs ?: emptyList()
        if(listSongsDrive.isNotEmpty()) {
            val rp = File(AppResources.listSongsChannelKsf[oldValue].rutaPreview).parentFile!!.name
            val songDrive = listSongsDrive.find { it.name == rp }

            return songDrive?.videos ?: ArrayList()
        }else{
            return ArrayList()
        }
    }

    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build()

    private suspend fun downloadVideoFromDrive(fileId: String, isBGA: Boolean, progressCallback: (Int) -> Unit): File? = withContext(Dispatchers.IO) {
        try {
            val url = "https://www.googleapis.com/drive/v3/files/$fileId?alt=media&key=$API_KEY"
            val request = Request.Builder()
                .url(url)
                .build()

            val response = client.newCall(request).execute()

            if (!response.isSuccessful) return@withContext null

            val body = response.body ?: return@withContext null
            val fileLength = body.contentLength()

            val outputFile = if (isBGA) {
                File(AppResources.listSongsChannelKsf[oldValue].rutaBGA).parentFile!!.resolve("song.mp4")
            } else {
                File(AppResources.listSongsChannelKsf[oldValue].rutaPreview).parentFile!!.resolve("song_p.mp4")
            }

            body.byteStream().use { input ->
                FileOutputStream(outputFile).use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var total = 0L
                    var read: Int

                    while (input.read(buffer).also { read = it } != -1) {

                        total += read

                        if (fileLength > 0) {
                            val progress = ((total * 100) / fileLength).toInt()
                            withContext(Dispatchers.Main) {
                                progressCallback(progress)
                            }
                        }

                        output.write(buffer, 0, read)
                    }
                }
            }

            outputFile

        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun createMediaRow(
        imageAsset: String,
        mainButtonText: String,
        downloadButtonText: String,
        onMainClick: () -> Unit,
        onDownloadClick: () -> Unit,
        existDownloaded: Boolean = false,
        existInDrive: Boolean = false,
    ): Pair<LinearLayout, Button> {
        val dpToPx = { dp: Int -> TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt() }

        var mainButton: Button? = null

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            // ImageView con la imagen del asset
            val imageView = ImageView(this@SelectSong).apply {
                try {
                    val bitmap = BitmapFactory.decodeStream(assets.open(imageAsset))
                    setImageBitmap(bitmap)
                } catch (_: Exception) {
                    // Si no se encuentra la imagen, usar un placeholder
                    setBackgroundColor(0xFF444444.toInt())
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
                layoutParams = LinearLayout.LayoutParams(dpToPx(64), dpToPx(64)).apply {
                    rightMargin = dpToPx(12)
                }

                // Borde redondeado para la imagen
                background = object : GradientDrawable() {
                    init {
                        shape = RECTANGLE
                        cornerRadius = dpToPx(8).toFloat()
                        setStroke(dpToPx(2), 0xFF00D9FF.toInt())
                    }
                }
                setPadding(dpToPx(2), dpToPx(2), dpToPx(2), dpToPx(2))
            }
            addView(imageView)

            // Container vertical para los botones
            val buttonsContainer = LinearLayout(this@SelectSong).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            // Botón principal (Agregar/Reemplazar)
            mainButton = Button(this@SelectSong).apply {
                text = mainButtonText
                setTextColor(0xFFFFFFFF.toInt())
                textSize = 12f
                setTypeface(null, Typeface.BOLD)
                isAllCaps = false
                background = object : GradientDrawable() {
                    init {
                        shape = RECTANGLE
                        cornerRadius = dpToPx(8).toFloat()
                        colors = intArrayOf(0xFF00D9FF.toInt(), 0xFF0099FF.toInt())
                        orientation = Orientation.LEFT_RIGHT
                    }
                }

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(40)
                ).apply {
                    bottomMargin = dpToPx(8)
                }

                setOnClickListener { onMainClick() }
            }
            buttonsContainer.addView(mainButton)

            // Botón de descarga
            val btnDownload = Button(this@SelectSong).apply {
                text = downloadButtonText
                setTextColor(0xFF00FF88.toInt())
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                isAllCaps = false
                isVisible = existDownloaded || existInDrive
                background = object : GradientDrawable() {
                    init {
                        shape = RECTANGLE
                        cornerRadius = dpToPx(8).toFloat()
                        setColor(0x3300FF88.toInt())
                        setStroke(dpToPx(1), 0xFF00FF88.toInt())
                    }
                }

                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dpToPx(36)
                )

                setOnClickListener { onDownloadClick() }
            }
            buttonsContainer.addView(btnDownload)

            addView(buttonsContainer)
        }

        return Pair(row, mainButton!!)
    }

    private fun saveFileToDestination(uri: Uri, fileName: String, isBGA: Boolean) {
        val destinationPath = File(currentPathSong.replace(File(currentPathSong).name, "", ignoreCase = true))
        val destinationFile = File(destinationPath, fileName.replace(".mp3", "", ignoreCase = true))
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(destinationFile)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            Toast.makeText(this, "Espere por favor, este proceso puede tomar varios segundos...", Toast.LENGTH_LONG).show()
            constraintMain.removeView(buttonLayout)
            progressLoading.visibility = View.VISIBLE
            removeAudioFromVideo(this, destinationFile, destinationFile.absolutePath.replace(".mp4", "_temp.mp4"), isBGA)


        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al guardar el archivo", Toast.LENGTH_SHORT).show()
        }
    }

    @OptIn(UnstableApi::class)
    private fun removeAudioFromVideo(context: Context, inputFile: File, outputFile: String, isBGA: Boolean) {
        val inputMediaItem = MediaItem.fromUri(Uri.fromFile(inputFile))
        val editedMediaItem = EditedMediaItem.Builder(inputMediaItem)
            .setRemoveAudio(true)
            .build()

        val transformerListener: Listener = object : Listener {
            override fun onCompleted(composition: Composition, result: ExportResult) {
                if (inputFile.exists()) inputFile.delete()
                File(outputFile).renameTo(File(inputFile.absolutePath))
                handler.postDelayed({
                    if(!isBGA){
                        Toast.makeText(context, "Se guardo el preview correctamente", Toast.LENGTH_SHORT).show()
                    }else{
                        Toast.makeText(context, "Se guardo el BGA correctamente", Toast.LENGTH_SHORT).show()
                    }
                    progressLoading.visibility = View.GONE
                    constraintMain.removeView(overlayBG)
                    constraintMain.removeView(buttonLayout)
                }, 1500L)
            }

            override fun onError(composition: Composition, result: ExportResult, exception: ExportException) {}
        }

        val transformer = Transformer.Builder(context)
            .addListener(transformerListener)
            .build()

        transformer.start(editedMediaItem, outputFile)

    }

    fun load(filename: String, isHalfDouble: Boolean = false) {
        if(!isHalfDouble){
            ksf = KsfProccess()
            ksf.load(filename)
        }else{
            ksfHD = KsfProccessHD()
            ksfHD.load(filename)
        }
    }

    private fun iniciarContador() {
        handlerContador.postDelayed(runnableContador, 0)
    }
    private val runnableContador: Runnable = object : Runnable {
        override fun run() {
            actualizarImagenNumero(reductor)
            reductor--
            if(isCounter){
                handlerContador.postDelayed(this, 1000)
                if(reductor < 0){
                    detenerContador()
                    if(ready == 1){
                        if(commandWindow.isVisible){
                            showCommandWindow(false)
                        }
                        imgAceptar.performClick()
                    }
                    if(ready == 0){
                        if(commandWindow.isVisible){
                            showCommandWindow(false)
                        }
                        imgAceptar.performClick()
                        imgAceptar.performClick()
                    }

                }
            }else{
                detenerContador()
            }
        }
    }

    private fun actualizarImagenNumero(numero: Int) {
            val unidad = numero % 10
            val decena = numero / 10
            val bitmapUnidad = dividirPNG(unidad)
            val bitmapDecena = dividirPNG(decena)
            val bitmapNumeroCompleto = combinarBitmaps(bitmapDecena, bitmapUnidad)

            imgContador.setImageBitmap(bitmapNumeroCompleto)
        }

    private fun dividirPNG(digito: Int): Bitmap {
        val anchoTotal = AppResources.bitmapNumber.width
        val anchoDigito = anchoTotal / 10
        val x = anchoDigito * digito
        return Bitmap.createBitmap(AppResources.bitmapNumber, x, 0, anchoDigito, AppResources.bitmapNumber.height)
    }

    private fun combinarBitmaps(bitmap1: Bitmap, bitmap2: Bitmap): Bitmap {
        val anchoTotal = bitmap1.width + bitmap2.width
        val bitmapCombinado = Bitmap.createBitmap(anchoTotal, bitmap1.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmapCombinado)
        canvas.drawBitmap(bitmap1, 0f, 0f, null)
        canvas.drawBitmap(bitmap2, bitmap1.width.toFloat(), 0f, null)
        return bitmapCombinado
    }

    private fun detenerContador() {
        handlerContador.removeCallbacks(runnableContador)
        handlerContador.postDelayed(runnableContador, 1000)
        reductor = 99
    }

    private val runnable: Runnable = object : Runnable {
        override fun run() {
            if (contador < listEfectsDisplay.size) {
                imgDisplay.setImageURI(listEfectsDisplay[contador].rutaCommandImg.toUri())
                contador++
            } else {
                contador = 0
            }
            handler.postDelayed(this, 1200)
        }
    }

    private fun resetRunnable() {
        handler.removeCallbacks(runnable)
        handler.postDelayed(runnable, 0)
    }

    private fun getRutaNoteSkin(rutaOriginal: String): String {
        return rutaOriginal.removeSuffix("_Icon.png")
    }

    private fun performAction() {
        openCommandWindow()
        sequence.clear()
    }

    private fun handleButtonPress(isLeft: Boolean): Boolean {
        sequence.add(isLeft)

        if (sequence.size >= sequencePattern.size) {
            val lastElements = sequence.takeLast(sequencePattern.size)
            if (lastElements == sequencePattern) {
                performAction()
                return true // 🔥 CLICK CONSUMIDO
            }
        }

        if (sequence != sequencePattern.take(sequence.size)) {
            sequence.clear()
        }

        return false
    }

    private fun showProgressBar(duration: Long) {
        var currentTime: Long

        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val maxProgress = progressBar.max

        val timer = object : CountDownTimer(duration, 1) {
            override fun onTick(millisUntilFinished: Long) {
                currentTime = duration - millisUntilFinished

                val progress = ((currentTime * maxProgress) / duration).toInt()
                progressBar.progress = progress
            }

            override fun onFinish() {
                currentTime = duration
                progressBar.progress = maxProgress
            }
        }
        timer.start()
    }

    private fun goSelectLevel() {
        soundPoolSelectSong.play(selectKsf, 1.0f, 1.0f, 1, 0, 1.0f)
        carouselSong.startAnimation(animOff)
        txInfoCurrentSong.startAnimation(animOff)
        carouselSong.isVisible = false
        txInfoCurrentSong.isVisible = false
        imgSelected.clearAnimation()
        imgSelected.visibility = View.INVISIBLE
        imgLvSelected.isVisible = true
        imgBestScore.isVisible = true
        imgBestGrade.isVisible = true
        lbLvActive.isVisible = true
        lbBestScore.isVisible = true

        imgWorldGrade.isVisible = true
        lbWorldScore.isVisible = true
        lbWorldName.isVisible = true

        imgLvSelected.startAnimation(animOn)
        imgBestScore.startAnimation(animOn)
        imgBestGrade.startAnimation(animOn)
        lbLvActive.startAnimation(animOn)
        lbBestScore.startAnimation(animOn)
        imgWorldGrade.startAnimation(animOn)
        lbWorldScore.startAnimation(animOn)
        lbWorldName.startAnimation(animOn)

        moverLvs()
    }

    private val scaledMainGrades = mutableMapOf<String, Bitmap>()
    private val scaledExtraGrades = mutableMapOf<String, Bitmap>()
    private val gradeCombinationCache = mutableMapOf<String, Bitmap>()

    private fun getBitMapGrade(checkedValues: String): Bitmap {

        val gradeFull = listSongScores.find { it.checkedValues == checkedValues }?.grade ?: return emptyBitmap

        if (gradeCombinationCache.containsKey(gradeFull)) {
            return gradeCombinationCache[gradeFull]!!
        }

        val gradeMain = gradeFull.substringBefore("|")
        val gradeExtra = gradeFull.substringAfter("|", "")

        val mainBitmap = scaledMainGrades[gradeMain] ?: return emptyBitmap

        if (gradeExtra.isEmpty()) {
            gradeCombinationCache[gradeFull] = mainBitmap
            return mainBitmap
        }

        val extraBitmap = scaledExtraGrades[gradeExtra] ?: return mainBitmap

        val spacing = 10
        val totalWidth = mainBitmap.width + spacing + extraBitmap.width
        val targetHeight = mainBitmap.height

        val result = createBitmap(totalWidth, targetHeight)

        val canvas = Canvas(result)

        canvas.drawBitmap(mainBitmap, 0f, 0f, null)
        canvas.drawBitmap(
            extraBitmap,
            mainBitmap.width + spacing.toFloat(),
            0f,
            null
        )

        gradeCombinationCache[gradeFull] = result

        return result
    }

    private fun getWorldBitMapGrade(): Bitmap {

        if (nivel.nivel == "?") return emptyBitmap

        val gradeFull = nivel.firstRank[0].grade

        // 🔥 Cache global reutilizable
        if (gradeCombinationCache.containsKey(gradeFull)) {
            return gradeCombinationCache[gradeFull]!!
        }

        val gradeMain = gradeFull.substringBefore("|")
        val gradeExtra = gradeFull.substringAfter("|", "")

        val mainBitmap = scaledMainGrades[gradeMain]
            ?: return emptyBitmap

        if (gradeExtra.isEmpty()) {
            gradeCombinationCache[gradeFull] = mainBitmap
            return mainBitmap
        }

        val extraBitmap = scaledExtraGrades[gradeExtra]
            ?: return mainBitmap

        val spacing = 10
        val totalWidth = mainBitmap.width + spacing + extraBitmap.width
        val targetHeight = mainBitmap.height

        val result = createBitmap(totalWidth, targetHeight)

        val canvas = Canvas(result)

        canvas.drawBitmap(mainBitmap, 0f, 0f, null)
        canvas.drawBitmap(
            extraBitmap,
            mainBitmap.width + spacing.toFloat(),
            0f,
            null
        )

        gradeCombinationCache[gradeFull] = result

        return result
    }

    private val emptyBitmap by lazy {
        createBitmap(1, 1)
    }

    private fun prepareGradeBitmaps() {

        val targetHeight = (medidaFlechas / 2).toInt()

        val gradeNames = listOf(
            "SSS+","SSS","SS+","SS","S+","S",
            "AAA+","AAA","AA+","AA","A+","A",
            "B","C","D","F"
        )

        gradeNames.forEachIndexed { index, grade ->
            scaledMainGrades[grade] = scaleToHeight(arrayGrades[index], targetHeight)
        }

        val extraNames = listOf("PG","UG","EG","SG","MG","TG","FG","RG")

        extraNames.forEachIndexed { index, grade ->
            scaledExtraGrades[grade] = scaleToHeight(arrGradesDescAbrev[index], targetHeight)
        }
    }

    private fun scaleToHeight(bitmap: Bitmap, targetHeight: Int): Bitmap {
        val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val targetWidth = (targetHeight * aspectRatio).toInt()
        return bitmap.scale(targetWidth, targetHeight)
    }

    private fun openCommandWindow() {
        if(!commandWindow.isVisible){
            showCommandWindow(true)
        }
    }

    private fun showCommandWindow(show : Boolean){
        if(show){
            commandWindow.visibility = View.VISIBLE
            commandWindowBG.visibility = View.VISIBLE
            linearMenus.visibility = View.VISIBLE
            linearTop.visibility = View.VISIBLE
            linearCommands.visibility = View.VISIBLE
            linearInfo.visibility = View.VISIBLE
            linearBottom.visibility = View.VISIBLE
            lbCurrentBpm.visibility = View.VISIBLE
            txCurrentBpm.visibility = View.VISIBLE
            btnCommandWindow.visibility = View.GONE

            commandWindow.startAnimation(animOn)
            commandWindowBG.startAnimation(animOn)
            linearCommands.startAnimation(animOn)
            linearInfo.startAnimation(animOn)
            soundPoolSelectSong.play(command_switchKsf, 1.0f, 1.0f, 1, 0, 1.0f)
            isFocusCommandWindow(oldValueCommand)
        }else{
            commandWindow.visibility = View.GONE
            commandWindowBG.visibility = View.GONE
            linearMenus.visibility = View.GONE
            linearTop.visibility = View.GONE
            linearCurrent.visibility = View.GONE
            linearValues.visibility = View.GONE
            linearCommands.visibility = View.GONE
            linearInfo.visibility = View.GONE
            linearBottom.visibility = View.GONE
            lbCurrentBpm.visibility = View.GONE
            txCurrentBpm.visibility = View.GONE
            btnCommandWindow.visibility = View.VISIBLE

            commandWindow.startAnimation(animOff)
            commandWindowBG.startAnimation(animOff)
            linearCurrent.startAnimation(animOff)
            linearValues.startAnimation(animOff)
            linearCommands.startAnimation(animOff)
            linearInfo.startAnimation(animOff)
            sequence.clear()
        }
    }

    private fun goSelectChannel(){
        soundPoolSelectSong.play(selectSong_backKsf, 1.0f, 1.0f, 1, 0, 1.0f)
        if(saveFavorites){
            listChannels.remove(channelFavorites)
            themes.edit().putString("allTunes", gson.toJson(listChannels)).apply()
            themes.edit().putString("favorites", gson.toJson(listFavorites)).apply()
            listChannels.add(channelFavorites)
            listChannels.sortBy { it.nombre.substringBefore("-").trim() }
        }

        nav_back_der.startAnimation(animOn)
        if (mediPlayer.isPlaying){
            mediPlayer.pause()
            mediPlayer.stop()
            mediPlayer.release()
            if(mediaPlayerVideo.isPlaying){
                mediaPlayerVideo.pause()
                mediaPlayerVideo.stop()
                mediaPlayerVideo.release()
            }
        }

        if (::prevPlayer.isInitialized) prevPlayer.release()
        if (::nextPlayer.isInitialized) nextPlayer.release()
        if (::video_preview.isInitialized) video_preview.release()
        handlerContador.removeCallbacksAndMessages(null)
        handler.removeCallbacksAndMessages(null)

        if(isTimerRunning()){
            timer?.cancel()
        }

        resetRunnable()
        detenerContador()
        this.finish()
        overridePendingTransition(0,R.anim.anim_command_window_off)
    }

    private fun isTimerRunning(): Boolean {
        return isTimerRunning
    }

    private fun hideSelectLv(anim: Animation) {
        carouselSong.isVisible = true
        carouselSong.startAnimation(animOn)
        txInfoCurrentSong.isVisible = true
        txInfoCurrentSong.startAnimation(animOn)
        imgSelected.visibility = View.VISIBLE
        imgSelected.startAnimation(anim)

        imgLvSelected.startAnimation(animOff)
        imgBestScore.startAnimation(animOff)
        imgBestGrade.startAnimation(animOff)
        lbLvActive.startAnimation(animOff)
        lbBestScore.startAnimation(animOff)

        imgWorldGrade.startAnimation(animOff)
        lbWorldScore.startAnimation(animOff)
        lbWorldName.startAnimation(animOff)

        imgLvSelected.isVisible = false
        imgBestScore.isVisible = false
        imgBestGrade.isVisible = false
        lbLvActive.isVisible = false
        lbBestScore.isVisible = false

        imgWorldGrade.isVisible = false
        lbWorldScore.isVisible = false
        lbWorldName.isVisible = false
    }

    private fun moverLvs() {
        val realPosition = getRealIndex(oldValue) // CLAVE
        val lv = AppResources.listSongsChannelKsf[realPosition].listKsf[positionActualLvs]
        imgLvSelected.setImageBitmap(if(lv.typePlayer == "A") difficultySelected else difficultySelectedHD)

        lbLvActive.text = lv.level

        currentLevel = lv.level
        lbBestScore.text = listSongScores.find { it.checkedValues == lv.checkedValues }?.puntaje ?: "0"
        currentScore = lbBestScore.text.toString()
        currentBestGrade = getBitMapGrade(lv.checkedValues)
        imgBestGrade.setImageBitmap(currentBestGrade)

        val rank = listGlobalRanking[lv.checkedValues]
        val ranking = rank ?: ArrayList(List(3) { FirstRank() })
        nivel = Nivel(lv.level, lv.checkedValues, ranking)

        val currentBestWorldGrade = getWorldBitMapGrade()
        imgWorldGrade.setImageBitmap(currentBestWorldGrade)

        if(listGlobalRanking.isNotEmpty()) {

            lbWorldName.text = if (ranking[0].nombre != "") ranking[0].nombre else "---------"
            lbWorldScore.text = if (ranking[0].puntaje != "") ranking[0].puntaje else "-"
            currentWorldScore = listOf(
                ranking[0].puntaje,
                ranking[1].puntaje,
                ranking[2].puntaje
            )
        }else{
            lbWorldName.text = "---------"
            lbWorldScore.text = "0"
            currentWorldScore = listOf("1000000", "1000000", "1000000")
        }
    }

    private fun moverCanciones(flecha: ImageView, isNext: Boolean = false) {
        val real = getRealIndex(oldValue) // 🔥 CLAVE
        resetIndicatorPosition()
        soundPoolSelectSong.play(selectSong_movKsf, 0.5f, 0.5f, 1, 0, 1.0f)
        flecha.startAnimation(AppResources.animPressNav)

        isFocus(oldValue)
        showTransitionVideo(isNext)

        lbArtist.isSelected = true
        lbNameSong.isSelected = true

        if (currentChannel == "03-SHORT CUT - V2" ||
            currentChannel == "04-REMIX - V2" ||
            currentChannel == "05-FULLSONGS - V2") {

            val currentNumberChannel = File(AppResources.listSongsChannelKsf[real].rutaSong).parentFile?.name!!.substringBefore("-").trim()
            if (currentNumberChannel != numberChannel) {
                numberChannel = currentNumberChannel

                when (currentNumberChannel) {
                    "12" -> soundPoolSelectSong.play(st_zero, 1.0f, 1.0f, 1, 0, 1.0f)
                    "13" -> soundPoolSelectSong.play(nx_nxAbs, 1.0f, 1.0f, 1, 0, 1.0f)
                    "14" -> soundPoolSelectSong.play(fiesta_fiesta2, 1.0f, 1.0f, 1, 0, 1.0f)
                    "17" -> soundPoolSelectSong.play(prime, 1.0f, 1.0f, 1, 0, 1.0f)
                    "18" -> soundPoolSelectSong.play(prime2, 1.0f, 1.0f, 1, 0, 1.0f)
                    "19" -> soundPoolSelectSong.play(aniversary_xx, 1.0f, 1.0f, 1, 0, 1.0f)
                    "21" -> soundPoolSelectSong.play(phoenix, 1.0f, 1.0f, 1, 0, 1.0f)
                }
            }
        }
    }

    private var isVideo: Boolean = false
    private fun isFocus (position: Int){
        ImageScheduler.newGeneration()

        val size = AppResources.listSongsChannelKsf.size

        fun get(i: Int) = AppResources.listSongsChannelKsf[((i % size) + size) % size]

        // 🔥 PRIORIDAD 3 → visible
        val center = get(position)
        preload(center.rutaDisc, priority = 3)

        // 🔥 PRIORIDAD 2 → cercanos
        for (i in 1..3) {
            preload(get(position + i).rutaDisc, 2)
            preload(get(position - i).rutaDisc, 2)
        }

        // 🔥 PRIORIDAD 1 → un poco más lejos
        for (i in 4..6) {
            preload(get(position + i).rutaDisc, 1)
            preload(get(position - i).rutaDisc, 1)
        }

        val real = getRealIndex(position)
        val item = AppResources.listSongsChannelKsf[real]
        currentPathSong = item.rutaSong
        timer?.cancel()
        timer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                mediPlayer.stop()
                isTimerRunning = false
            }
        }

        currentSong = item.title
        if(currentChannel == "06-FAVORITES") {
            val nameChannels = item.channel
            listSongScores = db.getSongScores(db.readableDatabase, nameChannels.toString(), currentSong)
        }else{
            listSongScores = db.getSongScores(db.readableDatabase, currentChannel, currentSong)
        }
        if(item.listKsf.size != listSongScores.size){
            db.deleteCancion(item.title)
            listSongScores = arrayOf()
        }

        if(listSongScores.isEmpty()){
            for (nivel in item.listKsf) {
                db.insertNivel(
                    canal = currentChannel,
                    cancion = currentSong,
                    checkedValues = nivel.checkedValues
                )
            }
            listSongScores = db.getSongScores(db.readableDatabase, currentChannel, currentSong)
        }

        if (isFileExists(File(item.rutaPreview))) {
            isVideo = !(item.rutaPreview.endsWith(".png", true)
                    || item.rutaPreview.endsWith(".jpg", true)
                    || item.rutaPreview.endsWith(".bpm", true)
                    || item.rutaPreview.endsWith(".mpg", true)
                    || item.rutaPreview.endsWith(".avi", true)
                    || item.rutaPreview.isEmpty())

            if (isVideo) {
                video_fondo.visibility = View.INVISIBLE

                video_preview.stopSafely()
                video_preview.reset()

                video_preview.apply {
                    setDataSource(item.rutaPreview)
                    isLooping = true
                    setVolume(0f, 0f)

                    setOnPreparedListener {
                        video_fondo.visibility = View.VISIBLE
                        start()
                    }
                    prepareAsync()
                }

                imgPrev.visibility = View.INVISIBLE

                playMedia(item.rutaSong)
            } else {
                setDiscImage(item.rutaDisc)

                video_fondo.visibility = View.GONE
                imgPrev.visibility = View.VISIBLE

                playMedia(item.rutaSong)
            }
        } else {
            setDiscImage(item.rutaDisc)

            video_fondo.visibility = View.GONE
            imgPrev.visibility = View.VISIBLE

            playMedia(item.rutaSong)
        }
        if(item.title == ""){
            lbNameSong.text = "NO TITLE"
        }else{
            lbNameSong.text = item.title
        }
        lbNameSong.startAnimation(AppResources.animNameSong)
        txInfoCurrentSong.text = String.format("%03d/%03d", real + 1, AppResources.listSongsChannelKsf.size)

        if(item.artist == ""){
            lbArtist.text = "NO ARTIST"
        }else{
            lbArtist.text = item.artist
        }

        val lbDbpm = "BPM ${item.displayBpm}"
        lbBpm.text = lbDbpm
        displayBPM = item.displayBpm.replace("BPM ", "").substringBefore("-").toFloat()
        recyclerLvs.adapter?.notifyDataSetChanged()
        llenaLvsKsf(item.listKsf)
    }

    private fun MediaPlayer.stopSafely() {
        try {
            if (isPlaying) stop()
        } catch (_: Exception) {
        }
    }

    fun preload(path: String, priority: Int) {
        val context = applicationContext

        val key = md5("$path-$640-$480")

        ImageScheduler.submit(key, priority) {

            val file = ImagePipeline.getOrCreateTrimmed(
                path,
                context,
                640,
                480
            )

            SongImageCache.memoryCache[key] = file
        }
    }

    object SongImageCache {

        val memoryCache = HashMap<String, File>()

    }

    object ImagePipeline {

        fun getTrimmedFile(originalPath: String, context: Context): File {
            val cacheDir = File(context.cacheDir, "trimmed")
            if (!cacheDir.exists()) cacheDir.mkdirs()

            val name = originalPath.hashCode().toString() + "_trim.png"
            return File(cacheDir, name)
        }
        @Synchronized
        fun getOrCreateTrimmed(
            originalPath: String,
            context: Context,
            reqWidth: Int,
            reqHeight: Int
        ): File {

            val trimmedFile = getTrimmedFile(originalPath, context)

            // 🔥 si ya existe → listo
            if (trimmedFile.exists()) return trimmedFile

            // 🔥 si no existe → generar
            val bitmap = decodeSampledBitmap(originalPath, reqWidth, reqHeight) ?: return trimmedFile
            val trimmed = trimTransparent(bitmap)

            trimmedFile.outputStream().use {
                trimmed.compress(Bitmap.CompressFormat.PNG, 100, it)
            }

            return trimmedFile
        }

        private fun decodeSampledBitmap(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            BitmapFactory.decodeFile(path, options)

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565

            return BitmapFactory.decodeFile(path, options)
        }

        private fun calculateInSampleSize(
            options: BitmapFactory.Options,
            reqWidth: Int,
            reqHeight: Int
        ): Int {
            val (height, width) = options.outHeight to options.outWidth
            var inSampleSize = 1

            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2

                while ((halfHeight / inSampleSize) >= reqHeight &&
                    (halfWidth / inSampleSize) >= reqWidth
                ) {
                    inSampleSize *= 2
                }
            }

            return inSampleSize
        }

        private fun trimTransparent(src: Bitmap): Bitmap {
            val width = src.width
            val height = src.height

            var minX = width
            var minY = height
            var maxX = -1
            var maxY = -1

            val pixels = IntArray(width * height)
            src.getPixels(pixels, 0, width, 0, 0, width, height)

            var index = 0
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val alpha = pixels[index] ushr 24
                    if (alpha != 0) {
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                    }
                    index++
                }
            }

            if (maxX < minX || maxY < minY) return src

            return Bitmap.createBitmap(
                src,
                minX,
                minY,
                maxX - minX + 1,
                maxY - minY + 1
            )
        }
    }

    private fun setDiscImage(path: String) {

        val key = md5("$path-$640-$480")

        val cached = SongImageCache.memoryCache[key]

        if (cached != null && cached.exists()) {
            Glide.with(imgPrev)
                .load(cached)
                .fitCenter()
                .into(imgPrev)
            return
        }

        imgPrev.setImageResource(R.drawable.placeholder)

        preload(path, priority = 100)
    }

    object ImageScheduler {

        private val executor = java.util.concurrent.ThreadPoolExecutor(
            2, 2,
            60L, TimeUnit.SECONDS,
            java.util.concurrent.PriorityBlockingQueue<Runnable>()
        )

        // evita duplicados
        private val inFlight = HashSet<String>()

        // invalida trabajos antiguos
        @Volatile private var generation = 0

        fun newGeneration() {
            generation++
        }

        fun submit(
            path: String,
            priority: Int,
            task: (gen: Int) -> Unit
        ) {
            synchronized(inFlight) {
                if (inFlight.contains(path)) return
                inFlight.add(path)
            }

            val genAtSubmit = generation

            executor.execute(PriorityTask(priority) {
                try {
                    // si cambió la generación, cancela silenciosamente
                    if (genAtSubmit != generation) return@PriorityTask
                    task(genAtSubmit)
                } finally {
                    synchronized(inFlight) {
                        inFlight.remove(path)
                    }
                }
            })
        }

        private class PriorityTask(
            private val priority: Int,
            private val block: () -> Unit
        ) : Runnable, Comparable<PriorityTask> {

            override fun run() = block()

            override fun compareTo(other: PriorityTask): Int {
                return other.priority - this.priority // mayor prioridad primero
            }
        }
    }

    private fun showTransitionVideo(isNext: Boolean) {
        if (isNext) {
            prev.visibility = View.GONE
            next.visibility = View.VISIBLE

            nextPlayer.seekTo(0)
            nextPlayer.start()

            nextPlayer.setOnCompletionListener {
                next.visibility = View.GONE
                mediPlayer.start()
                if (isVideo) video_preview.start()
            }

        } else {
            next.visibility = View.GONE
            prev.visibility = View.VISIBLE

            prevPlayer.seekTo(0)
            prevPlayer.start()

            prevPlayer.setOnCompletionListener {
                prev.visibility = View.GONE
                mediPlayer.start()
                if (isVideo) video_preview.start()
            }
        }
    }

    private fun playMedia(path: String) {
        mediPlayer.release()
        mediPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setDataSource(path)
            prepare()
            seekTo(startTimeMs)
            //start()
        }
        timer?.start()
        isTimerRunning = true
    }

    private fun isFocusCommandWindow (position: Int){
        soundPoolSelectSong.play(command_moveKsf, 1.0f, 1.0f, 1, 0, 1.0f)
        val item = listCommands[position]
        recyclerCommands.currentItem = position
        if(item.value.contains("Speed", ignoreCase = true)){
            lbCurrentBpm.text = "Velocidad"
            txCurrentBpm.text = txVelocidadActual.text
        }
        if(item.value.contains("Offset", ignoreCase = true)){
            lbCurrentBpm.text = "Offset"
            txCurrentBpm.text = txOffset.text
        }
        if(item.value.contains("NoteSkin", ignoreCase = true) ||
            item.value.contains("Display", ignoreCase = true) ||
            item.value.contains("Judge", ignoreCase = true) ||
            item.value.contains("Alternate", ignoreCase = true)||
            item.value.contains("Path", ignoreCase = true)){
            linearCurrent.isVisible = false
        }
        txInfoCW.text = item.descripcion
        llenaCommandsValues(listCommands[position].listCommandValues)
    }

    private fun isFocusCommandWindowValues (position: Int){
        soundPoolSelectSong.play(command_moveKsf, 1.0f, 1.0f, 1, 0, 1.0f)
        //listCommands[oldValueCommand].listCommandValues.sortedWith(compareBy { it.rutaCommandImg })
        val item = listCommands[oldValueCommand].listCommandValues[position]
        recyclerCommandsValues.setCurrentItem(position)
        val reset = "por defecto"

        if(!listCommands[oldValueCommand].value.contains("NoteSkins")){
            if(item.value.matches(Regex(".*[0-9].*"))){
                if(item.value == "0"){
                    txInfoCW.text = item.descripcion + reset
                }else {
                    txInfoCW.text = item.descripcion + item.value
                }
            }else{
                txInfoCW.text = item.descripcion
            }
        }else{
            txInfoCW.text = item.descripcion
        }
    }

    private fun llenaCommands( listCommands: ArrayList<Command>){
        recyclerCommands.adapter = CommandAdapter(listCommands)
        val compositePageTransformer = CompositePageTransformer()
        compositePageTransformer.addTransformer(MarginPageTransformer((10 * Resources.getSystem().displayMetrics.density).toInt()))
        recyclerCommands.setPageTransformer(compositePageTransformer)
        compositePageTransformer.addTransformer { page, position ->
            val r = 1 - abs(position)
            page.scaleY = (0.80f + r * 0.60f)
            page.scaleX = (0.80f + r * 0.60f)
        }
        recyclerCommands.setPageTransformer(compositePageTransformer)
        recyclerCommands.apply {
            clipChildren = false
            clipToPadding = false
            offscreenPageLimit = 3
            (getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        }
    }

    private fun llenaCommandsValues( listCommandsVales: ArrayList<CommandValues>){
        recyclerCommandsValues.adapter = CommandValuesAdapter(listCommandsVales)
        val compositePageTransformer = CompositePageTransformer()
        compositePageTransformer.addTransformer { page, position ->
            val r = 1 - abs(position)
            page.scaleY = (0.80f + r * 0.20f)
        }
        recyclerCommandsValues.setPageTransformer(compositePageTransformer)
        recyclerCommandsValues.apply {
            clipChildren = false
            clipToPadding = false
            offscreenPageLimit = 3
            (getChildAt(0) as RecyclerView).overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        }
    }

    private fun llenaLvsKsf(listLvs: MutableList<Ksf>) {

        if (recyclerLvs.layoutManager == null) {
            recyclerLvs.layoutManager = LinearLayoutManager(
                this,
                LinearLayoutManager.HORIZONTAL,
                false
            )
        }

        recyclerLvs.adapter = LvsAdapter(listLvs, sizeLvs)
        recyclerLvs.onFlingListener = null
    }

    private fun llenaLvsVacios(listLvsKsf: MutableList<Ksf> = mutableListOf<Ksf>()){
        recyclerLvsVacios.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = LvsAdapter(listLvsKsf, (sizeLvs))
        }
    }

    private fun animaNavs(bitmap : Bitmap, spriteWidth : Int, spriteHeight : Int): AnimationDrawable{
        val arrowSpritesRD = arrayOf(
            Bitmap.createBitmap(bitmap, 0, 0, spriteWidth, spriteHeight),
            Bitmap.createBitmap(bitmap, spriteWidth, 0, spriteWidth, spriteHeight),
            Bitmap.createBitmap(bitmap, 0, spriteHeight, spriteWidth, spriteHeight),
            Bitmap.createBitmap(bitmap, spriteWidth, spriteHeight, spriteWidth, spriteHeight))
        val animation = AnimationDrawable().apply {
            arrowSpritesRD.forEach {
                addFrame(BitmapDrawable(it), 200)
            }
            isOneShot = false
        }
        return animation
    }

    private fun isFileExists(file: File): Boolean {
        return file.exists() && !file.isDirectory
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        //super.onBackPressed()
        Toast.makeText(this, "Use los botones BACK", Toast.LENGTH_SHORT).show()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(runnable)
        handlerContador.removeCallbacks(runnableContador)
        isRunning = false
        handler.removeCallbacks(carouselRunnable)
        //mediPlayer.pause()
        if(bgaSelectSong.isPlaying){
            bgaSelectSong.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        isFocus(oldValue)
        resetRunnable()
        detenerContador()
        if (!isRunning) {
            isRunning = true
            handler.post(carouselRunnable)
        }
        bgaSelectSong.start()
        if(listEfectsDisplay.isNotEmpty()) {
            handler.postDelayed(runnable, 1200)
        }
        updateRecycler()
        // Reproducir el MediaPlayer después de que isFocus() haya preparado la canción
        handler.postDelayed({
            if (!mediPlayer.isPlaying) {
                mediPlayer.start()
                if (isVideo && ::video_preview.isInitialized) {
                    video_preview.start()
                }
            }
        }, 500)
        isEndingFade = false
        endingFadeAlpha = 0f
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        /*
        if (::prevPlayer.isInitialized) prevPlayer.release()
        if (::nextPlayer.isInitialized) nextPlayer.release()
        if (::video_preview.isInitialized) video_preview.release()
        handlerContador.removeCallbacksAndMessages(null)
        handler.removeCallbacksAndMessages(null)
        timer?.cancel()
        if(true){
            if(mediPlayer.isPlaying){
                mediPlayer.stop()
            }
        }
        */
        super.onDestroy()
        Log.d("ACTIVITY_DEBUG", "onDestroy")
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }

    private fun hideSystemUI() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        windowInsetsController.let { controller ->
            controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }

    }

}





