package com.fingerdance

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.Transformer.Listener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.CompositePageTransformer
import androidx.viewpager2.widget.MarginPageTransformer
import androidx.viewpager2.widget.ViewPager2
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.random.Random
import androidx.core.graphics.createBitmap

private lateinit var mediaPlayerVideo : MediaPlayer
private lateinit var commandWindow: ConstraintLayout
private lateinit var linearLvs: ConstraintLayout

private lateinit var recyclerView: RecyclerView
private lateinit var recyclerLvs: RecyclerView
private lateinit var recyclerLvsVacios: RecyclerView
private lateinit var recyclerCommands: ViewPager2
private lateinit var recyclerCommandsValues: ViewPager2

private lateinit var listItemsKsf: ArrayList<SongKsf>

private var middle: Int = 0
private var oldValue: Int = 0
private var oldValueCommand: Int = 0
private var oldValueCommandValues: Int = 0
private var ultimoLv: Int = 0

private var animPressNav: Animation? = null
private var animNameSong: Animation? = null
private var animOn: Animation? = null
private var animOff: Animation? = null

private val sequence = mutableListOf<Boolean>()
private val sequencePattern = listOf(false, true, false, true, false, true)

private lateinit var bmFloor: Bitmap
private lateinit var bmFloor2: Bitmap

private var contador = 0

private val handler = Handler(Looper.getMainLooper())
private val handlerContador = Handler(Looper.getMainLooper())

private var reductor = 100

private val startTimeMs = 30000
private var timer: CountDownTimer? = null
private var isTimerRunning = false

private var ready = 0

var currentScore = ""
var currentWorldScore = listOf<String>()
lateinit var currentBestGrade : Bitmap

//private var idAdd = ""
//private var interstitialAd: InterstitialAd? = null

lateinit var listSongScores: Array<ObjPuntaje>

private var currentPathSong: String = ""
private var niveles = arrayListOf<Nivel>()

var isVideo = false
var isMediaPlayerPrepared = false
val widthJudges = width / 2
val heightJudges = widthJudges / 6
lateinit var resultSong: ResultSong
private var numberChannel = ""

private lateinit var layoutManager : LinearLayoutManager

private lateinit var difficultySelected : Bitmap
private lateinit var difficultySelectedHD : Bitmap
var checkedValues = ""
var isOficialSong = false

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
    private lateinit var imgLoading: ImageView
    private lateinit var imgAceptar: ImageView
    private lateinit var imgFloor: ImageView
    private lateinit var imgLvSelected: ImageView
    private lateinit var lbLvActive: TextView

    private lateinit var imgBestScore: ImageView

    private lateinit var imgBestGrade: ImageView
    private lateinit var lbBestScore: TextView

    private lateinit var imgWorldGrade: ImageView
    private lateinit var lbWorldScore: TextView
    private lateinit var lbWorldName: TextView

    private lateinit var video_fondo : VideoView
    private lateinit var imgPrev: ImageView

    private lateinit var next : VideoView
    private lateinit var prev : VideoView

    private lateinit var indicatorLayout: ImageView
    private lateinit var imageCircle : ImageView

    private lateinit var bgaSelectSong: VideoView
    private lateinit var overlayBG: View
    private lateinit var btnAddPreview: Button
    private lateinit var btnAddBga: Button
    private lateinit var imgContador: ImageView
    private lateinit var smoothScroller : RecyclerView. SmoothScroller

    private val pickPreviewFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val namePreview = File(listItemsKsf[oldValue].rutaSong).name.replace(".mp3", "")
            saveFileToDestination(it, namePreview + "_p.mp4", false)
        }

    }

    private val pickBgaFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val nameBGA = File(listItemsKsf[oldValue].rutaSong).name.replace(".mp3", "")
            saveFileToDestination(it, nameBGA + ".mp4", true)
        }
    }
        @RequiresApi(Build.VERSION_CODES.S)
        override fun onCreate(savedInstanceState: Bundle?) {
            getSupportActionBar()?.hide()
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_select_song)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            onWindowFocusChanged(true)

            isOnline = false

            recyclerView = findViewById(R.id.recyclerView)//binding.recyclerView

            recyclerLvs = findViewById(R.id.recyclerLvs)//binding.recyclerLvs
            recyclerLvsVacios = findViewById(R.id.recyclerNoLvs)//binding.recyclerNoLvs

            recyclerCommands = findViewById(R.id.recyclerCommands)
            recyclerCommands.isUserInputEnabled = false
            recyclerCommandsValues = findViewById(R.id.recyclerValues)
            recyclerCommandsValues.isUserInputEnabled = false

            val levels = Levels()

            playerSong = PlayerSong("","", "",0.0,0.0, 0.0, "","",false,
                                     false,"", "", levels, "")

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
                linearBG.background = Drawable.createFromPath(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/bg_select_song.png")!!.absolutePath)
            }

            imgPrev = findViewById(R.id.imgPrev)
            val params = imgPrev.layoutParams as ConstraintLayout.LayoutParams
            params.width = width
            params.height = (height * 0.3).toInt()
            imgPrev.layoutParams = params

            animPressNav = AnimationUtils.loadAnimation(this, R.anim.press_nav)
            animNameSong = AnimationUtils.loadAnimation(this, R.anim.anim_name_song)
            animOn = AnimationUtils.loadAnimation(this, R.anim.anim_command_window_on)
            animOff = AnimationUtils.loadAnimation(this, R.anim.anim_command_window_off)

            commandWindow = findViewById(R.id.command_window)
            commandWindowBG = findViewById(R.id.command_window_bg)
            commandWindowBG.foreground = Drawable.createFromPath(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/command_window/Command_Frame.png")!!.absolutePath)
            mediPlayer = MediaPlayer()

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

            val fondos = Drawable.createFromPath(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/command_window/Command_Back.png")!!.absolutePath)
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
            //txAV = findViewById(R.id.txAV)
            //txAV.isVisible = false

            imgVelocidadActual = findViewById(R.id.imgVelocidadActual)
            val bmVA= BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/command_window/Command_Effect.png")!!.absolutePath)
            imgVelocidadActual.setImageBitmap(bmVA)
            txVelocidadActual = findViewById(R.id.txVelocidadActual)

            imgOffset = findViewById(R.id.imgOffsetActual)
            imgOffset.setImageBitmap(bmVA)
            txOffset = findViewById(R.id.txOffsetActual)
            txOffset.text = "0"

            imgDisplay = findViewById(R.id.imgDisplay)
            imgDisplay.isVisible=false
            imgJudge = findViewById(R.id.imgJudge)
            imgJudge.isVisible=false
            imgNoteSkin = findViewById(R.id.imgNoteSkin)
            imgNoteSkin.isVisible=false
            imgNoteSkinFondo = findViewById(R.id.imgNoteSkinFondo)
            imgNoteSkinFondo.foreground = Drawable.createFromPath(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/command_window/Command_Effect.png")!!.absolutePath)
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
            val bmIndicator= BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/indicator_lv.png")!!.absolutePath)
            indicatorLayout.setImageBitmap(bmIndicator)
            indicatorLayout.layoutParams.width = sizeLvs

            val anchoRecyclerCommands = linearMenus.layoutParams.width / 3
            recyclerCommands.layoutParams.width = anchoRecyclerCommands - (anchoRecyclerCommands / 20)
            recyclerCommandsValues.layoutParams.width = anchoRecyclerCommands

            val anchoTxInfo = linearMenus.layoutParams.width - linearMenus.layoutParams.width / 7
            showCommandWindow(false)

            lbArtist = findViewById(R.id.lbArtist)
            lbBpm = findViewById(R.id.lbBpm)
            lbNameSong = findViewById(R.id.lbNameSong)
            txInfoCW = findViewById(R.id.txInfo)
            txInfoCW.layoutParams.width = anchoTxInfo

            imgSelected = findViewById(R.id.imgSelected)
            val bmSelected = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/imgSelect.png")!!.absolutePath)
            imgSelected.setImageBitmap(bmSelected)
            imageCircle = findViewById(R.id.imageCircleSS)
            val bmCircle = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/preview_circle.png")!!.absolutePath)
            imageCircle.setImageBitmap(bmCircle)

            imgFloor = findViewById(R.id.floor_song)
            bmFloor = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/floor.png")!!.absolutePath)
            imgFloor.setImageBitmap(bmFloor)

            bmFloor2 = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/floor2.png")!!.absolutePath)

            imgAceptar = findViewById(R.id.floor_start)
            val bmAceptar = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/press_floor.png")!!.absolutePath)
            imgAceptar.setImageBitmap(bmAceptar)

            imgLvSelected = findViewById(R.id.imgLvSelected)
            difficultySelected = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/lv_active.png")!!.absolutePath)
            difficultySelectedHD = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/lv_active_hd.png")!!.absolutePath)
            imgLvSelected.isVisible = false
            lbLvActive = findViewById(R.id.lbLvActive)
            lbLvActive.isVisible = false
            var textSize = width / 10
            lbLvActive.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())

            val frameBestScoreWidth = (width * 0.6).toInt()

            imgBestScore = findViewById(R.id.imgBestScore)
            val bestScore = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/score_body_select_song.png")!!.absolutePath)
            imgBestScore.setImageBitmap(bestScore)
            imgBestScore.isVisible = false
            imgBestScore.layoutParams.width = frameBestScoreWidth

            lbBestScore = findViewById(R.id.lbBestScore)
            lbBestScore.isVisible = false
            lbBestScore.layoutParams.width = (frameBestScoreWidth * 0.4).toInt()

            imgBestGrade = findViewById(R.id.imgBestGrade)
            imgBestGrade.isVisible = false

            lbWorldName = findViewById(R.id.lbWorldName)
            lbWorldName.isVisible = false
            lbWorldName.layoutParams.width = (frameBestScoreWidth * 0.6).toInt()

            lbWorldScore = findViewById(R.id.lbWorldScore)
            lbWorldScore.isVisible = false
            lbWorldScore.layoutParams.width = (frameBestScoreWidth * 0.4).toInt()

            imgWorldGrade = findViewById(R.id.imgWorldGrade)
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

            bitmapNumber = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/game_play/numbersCombo.png").toString())
            bitmapNumberMiss = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/game_play/numbersComboMiss.png").toString())

            numberBitmaps = ArrayList<Bitmap>().apply {
                var i = 0
                for (a in 0 until 10) {
                    add(Bitmap.createBitmap(bitmapNumber, i, 0, bitmapNumber.width / 10, bitmapNumber.height))
                    i += bitmapNumber.width / 10
                }
            }

            numberBitmapsMiss = ArrayList<Bitmap>().apply {
                var i = 0
                for (a in 0 until 10) {
                    add(Bitmap.createBitmap(bitmapNumberMiss, i, 0, bitmapNumberMiss.width / 10, bitmapNumberMiss.height))
                    i += bitmapNumberMiss.width / 10
                }
            }

            imgSelected.layoutParams.height = width / 3
            imgSelected.layoutParams.width = width / 3
            val anim = AnimationUtils.loadAnimation(this, R.anim.anim_select);
            imgSelected.startAnimation(anim)

            nav_izq = findViewById(R.id.nav_izq_song)
            nav_der = findViewById(R.id.nav_der_song)
            nav_back_Izq = findViewById(R.id.back_izq)
            nav_back_der = findViewById(R.id.back_der)

            video_fondo = findViewById(R.id.videoPreview)

            next = findViewById(R.id.next)
            prev = findViewById(R.id.preview)

            next.layoutParams.height = (height * 0.3).toInt()
            prev.layoutParams.height = (height * 0.3).toInt()

            next.setVideoPath(getExternalFilesDir("/FingerDance/Themes/$tema/BGAs/next.mp4")!!.absolutePath)
            prev.setVideoPath(getExternalFilesDir("/FingerDance/Themes/$tema/BGAs/prev.mp4")!!.absolutePath)

            next.setOnPreparedListener { mp ->
                mp.isLooping = false
                mp.setVolume(0f, 0f)
            }
            next.visibility = View.GONE

            prev.setOnPreparedListener { mp ->
                mp.isLooping = false
                mp.setVolume(0f, 0f)
            }
            prev.visibility = View.GONE

            val arrowNavIzq = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/ArrowsNav/ArrowNavIzq.png")!!.absolutePath)
            val arrowNavDer = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/ArrowsNav/ArrowNavDer.png")!!.absolutePath)
            val arrowBackIzqColor = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/ArrowsNav/ArrowBackIzqColor.png")!!.absolutePath)
            val arrowBackDerColor = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/ArrowsNav/ArrowBackDerColor.png")!!.absolutePath)

            val spriteWidth = arrowNavIzq.width / 2
            val spriteHeight = arrowNavIzq.height / 2
            val frameDuration = 800

            val navIzq = animaNavs(arrowNavIzq, spriteWidth, spriteHeight, frameDuration)
            navIzq.start()
            val navDer = animaNavs(arrowNavDer, spriteWidth, spriteHeight, frameDuration)
            navDer.start()
            val navBackIzq = animaNavs(arrowBackIzqColor, spriteWidth, spriteHeight, frameDuration)
            navBackIzq.start()
            val navBackDer = animaNavs(arrowBackDerColor, spriteWidth, spriteHeight, frameDuration)
            navBackDer.start()

            nav_izq.setImageDrawable(navIzq)
            nav_der.setImageDrawable(navDer)
            nav_back_Izq.setImageDrawable(navBackIzq)
            nav_back_der.setImageDrawable(navBackDer)

            llenaCommands(listCommands)

            //Por ahora solo se enviaran KSF
            //val listVacios = ArrayList<Lvs>()
            val listVacios = ArrayList<Ksf>()
            val rutaLvSelected = getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/img_lv_back.png")!!.absolutePath

            for (index in 0..19) {
                listVacios.add(Ksf("", "",  rutaLvSelected))
            }
            llenaLvsVacios(null, listVacios, recyclerLvsVacios)

            if(listSongsChannel.size > 0){
                //listItems = createSongList()
            }else if (listSongsChannelKsf.size > 0){
                listItemsKsf = createSongListKsf()
            }

            setupRecyclerView((height * 0.06).toInt(), (width * 0.2).toInt())
            //var num = listItems.size / 2
            var num = listItemsKsf.size / 2
            if (num.toString().contains(".")) {
                num = Math.round(num.toDouble()).toInt()
                middle = num
            } else {
                middle = num
            }
            oldValue = middle
            recyclerView.scrollToPosition(middle)
            numberChannel = File(listItemsKsf[oldValue].rutaSong).parentFile?.name!!.substringBefore("-").trim()
            isFocus(middle)

            smoothScroller = CenterSmoothScroller(recyclerView.context)
            smoothScroller.targetPosition = middle
            recyclerView.layoutManager?.startSmoothScroll(smoothScroller)
            recyclerView.setOnTouchListener { _, _ -> true }
            layoutManager = recyclerLvs.layoutManager as LinearLayoutManager
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
            //lbBpm.layoutParams.width = (width * 0.2).toInt()

            textSize = width / 40
            lbCurrentBpm.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())

            imgLvSelected.layoutParams.width = (width / 4.1).toInt()

            imgFloor.layoutParams.width = (width * 0.6).toInt()
            imgAceptar.layoutParams.width = (width * 0.3).toInt()

            val rankingView = findViewById<TopRankingView>(R.id.topRankingView)
            rankingView.layoutParams.width = (width * 0.9).toInt()
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

            imgBestScore.setOnClickListener{
                if(niveles[positionActualLvs].fisrtRank.size > 0){
                    if(!commandWindow.isVisible){
                        soundPoolSelectSongKsf.play(selectKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                        rankingView.visibility = View.VISIBLE
                        rankingView.startAnimation(animOn)
                        rankingView.setIconDrawable(imgLvSelected.drawable)
                        rankingView.setNiveles(niveles[positionActualLvs])
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
                imgFloor.setImageBitmap(bmFloor)
                if (recyclerView.isVisible && !commandWindow.isVisible) {
                    Toast.makeText(this, "Manten presionado para volver al Selecet Channel", Toast.LENGTH_SHORT).show()
                    soundPoolSelectSongKsf.play(selectSong_movKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                }
                if (imgLvSelected.isVisible && !commandWindow.isVisible && !rankingView.isVisible) {
                    soundPoolSelectSongKsf.play(up_SelectSoundKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                    hideSelectLv(anim)
                }
                if(rankingView.visibility == View.VISIBLE){
                    soundPoolSelectSongKsf.play(up_SelectSoundKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                    rankingView.visibility = View.INVISIBLE
                    rankingView.startAnimation(animOff)
                    constraintMain.removeView(linearRanking)
                }
                if (commandWindow.isVisible && !linearValues.isVisible ) {
                    soundPoolSelectSongKsf.play(command_backKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                    showCommandWindow(false)
                }
                if (linearValues.isVisible) {
                    soundPoolSelectSongKsf.play(command_backKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                    linearCurrent.isVisible = false
                    linearValues.isVisible = false
                    isFocusCommandWindow(oldValueCommand)
                }
            }
            nav_back_der.setOnClickListener() {
                ready = 0
                imgFloor.setImageBitmap(bmFloor)
                if (recyclerView.isVisible && !commandWindow.isVisible) {
                    //goSelectChannel()
                    Toast.makeText(this, "Manten presionado para volver al Select Channel", Toast.LENGTH_SHORT).show()
                    soundPoolSelectSongKsf.play(selectSong_movKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                }
                if (imgLvSelected.isVisible && !commandWindow.isVisible && !rankingView.isVisible) {
                    soundPoolSelectSongKsf.play(up_SelectSoundKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                    hideSelectLv(anim)
                }
                if(rankingView.visibility == View.VISIBLE){
                    soundPoolSelectSongKsf.play(up_SelectSoundKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                    rankingView.visibility = View.INVISIBLE
                    rankingView.startAnimation(animOff)
                    constraintMain.removeView(linearRanking)
                }
                if (commandWindow.isVisible && !linearValues.isVisible) {
                    soundPoolSelectSongKsf.play(command_backKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                    showCommandWindow(false)
                }
                if (linearValues.isVisible) {
                    soundPoolSelectSongKsf.play(command_backKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                    linearValues.isVisible = false
                    linearCurrent.isVisible = false
                    isFocusCommandWindow(oldValueCommand)
                }
            }

            nav_izq.setOnClickListener() {
                ready = 0
                imgFloor.setImageBitmap(bmFloor)
                //handleButtonPress(false)
                if (recyclerView.isVisible && !commandWindow.isVisible) {
                    if (oldValue == 2) {
                        //oldValue = listItems.size - 3
                        oldValue = listItemsKsf.size - 3
                    } else {
                        oldValue -= 1
                    }
                    moverCanciones(nav_izq,animPressNav, oldValue)
                    moveIndicatorToPosition(0)
                }
                if (imgLvSelected.isVisible && !commandWindow.isVisible) {
                    handleButtonPress(false)
                    soundPoolSelectSongKsf.play(move_lvsKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                    if (positionActualLvs != 0) {
                        positionActualLvs -= 1
                        moverLvs(positionActualLvs)
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
            nav_der.setOnClickListener() {
                ready = 0
                imgFloor.setImageBitmap(bmFloor)
                //handleButtonPress(true)
                if (recyclerView.isVisible && !commandWindow.isVisible) {
                    //if (oldValue == listItems.size - 3) {
                    if (oldValue == listItemsKsf.size - 3) {
                        oldValue = 2
                    } else {
                        oldValue += 1
                    }
                    moverCanciones(nav_der, animPressNav, oldValue, true)
                    moveIndicatorToPosition(0)
                }
                if (imgLvSelected.isVisible && !commandWindow.isVisible) {
                    handleButtonPress(true)
                    soundPoolSelectSongKsf.play(move_lvsKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                    ultimoLv = (recyclerLvs.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                    if (positionActualLvs < ultimoLv) {
                        positionActualLvs += 1
                        moverLvs(positionActualLvs)
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

            val tipsArray = resources.getStringArray(R.array.tips_array)
            val txTip = findViewById<TextView>(R.id.txTip)

            imgAceptar.setOnClickListener() {
                if (recyclerView.isVisible && !commandWindow.isVisible) {
                    goSelectLevel()
                }
                if(imgLvSelected.isVisible && !commandWindow.isVisible){
                    if(ready == 1){
                        soundPoolSelectSongKsf.play(startKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                        imgAceptar.isEnabled = false

                        val bit = BitmapFactory.decodeFile(listItemsKsf[oldValue].rutaTitle)
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
                        playerSong.rutaBanner = listItemsKsf[oldValue].rutaTitle

                        txTip.text = tipsArray[Random.nextInt(tipsArray.size)]

                        playerSong.speed = txVelocidadActual.text.toString()
                        if(playerSong.rutaNoteSkin != ""){
                            ruta = playerSong.rutaNoteSkin!!
                        }else{
                            val directorioBase = getExternalFilesDir("/FingerDance/NoteSkins")!!.absolutePath
                            val directorios = File(directorioBase).listFiles { file ->
                                file.isDirectory && file.name.contains("default", ignoreCase = true)
                            }
                            if (directorios != null) {
                                ruta = directorios.firstOrNull().toString()
                                playerSong.rutaNoteSkin = ruta
                            }
                        }
                        hideSelectLv(anim)
                        playerSong.rutaVideo = listItemsKsf[oldValue].rutaBGA
                        playerSong.rutaCancion = listItemsKsf[oldValue].rutaSong
                        playerSong.rutaKsf = listItemsKsf[oldValue].listKsf[positionActualLvs].rutaKsf
                        //mediaPlayer = MediaPlayer.create(this, Uri.fromFile(File(playerSong.rutaCancion!!)))
                        mediaPlayer = MediaPlayer().apply {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_GAME)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build()
                            )
                            setDataSource(File(playerSong.rutaCancion!!).absolutePath)
                            prepare()
                            //seekTo(startTimeMs)
                            //start()
                        }
                        val isHalfDouble = listItemsKsf[oldValue].listKsf[positionActualLvs].typePlayer == "B"
                        load(playerSong.rutaKsf, isHalfDouble)

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
                        if(!isOnline){
                            if(!isOffline){
                                songIndex = listGlobalRanking.indexOfFirst{ it.cancion == currentSong }
                                levelIndex = positionActualLvs
                            }
                        }

                        handler.postDelayed({
                            val intent = Intent(this, GameScreenActivity()::class.java)
                            intent.putExtra("IS_HALF_DOUBLE", isHalfDouble)
                            startActivity(intent)
                            handler.postDelayed({
                                linearLoading.isVisible = false
                                imgLoading.isVisible = false
                            }, 1000L)
                            ready = 0
                            imgFloor.setImageBitmap(bmFloor)
                        }, 3000L)
                    }
                    imgAceptar.isEnabled = true
                    if(ready == 0){
                        ready = 1
                        imgFloor.setImageBitmap(bmFloor2)
                        soundPoolSelectSongKsf.play(selectKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                    }
                }
                val itemCommand = listCommands[oldValueCommand]
                if(linearValues.isVisible){
                    soundPoolSelectSongKsf.play(command_modKsf, 1.0f, 1.0f, 1, 0, 1.0f)
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

            if(isVideo){
                video_fondo.start()
            }
            mediPlayer.start()
        }

    private fun showOverlay(isBGA: Boolean) {
        overlayBG = View(this).apply {
            setBackgroundColor(0xAA000000.toInt()) // Oscurece la pantalla
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {}
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

        btnAddPreview = Button(this).apply {
            text = if(isBGA) "Replace Preview" else "Add Preview"
            setBackgroundResource(android.R.color.transparent)
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(20, 10, 20, 10)
            setOnClickListener {
                pickPreviewFile.launch(arrayOf("video/mp4"))
            }
        }
        buttonLayout.addView(btnAddPreview)

        btnAddBga = Button(this).apply {
            text = "Add BGA"
            setBackgroundResource(android.R.color.transparent)
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(20, 10, 20, 10)
            setOnClickListener {
                pickBgaFile.launch(arrayOf("video/mp4"))
            }
        }

        val btnCancel = Button(this).apply {
            text = "Cancel"
            setBackgroundResource(android.R.color.transparent)
            setTextColor(0xFFFF0000.toInt())
            setPadding(20, 10, 20, 10)
            setOnClickListener {

                constraintMain.removeView(overlayBG)
                constraintMain.removeView(buttonLayout)
            }
        }

        buttonLayout.addView(btnAddBga)
        val space = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                40
            )
        }
        buttonLayout.addView(space)
        buttonLayout.addView(btnCancel)

        constraintMain.addView(overlayBG)
        constraintMain.addView(buttonLayout)
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
            //.setVideoMimeType(MimeTypes.VIDEO_H265)
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

            /*
            when(countSongsPlayed){
                ONE_ADDITIONAL_NOTESKIN -> {
                    listCommands[0].listCommandValues.add(listNoteSkinAdditionals[0])
                    listCommands[0].listCommandValues.sortBy { it.value }
                    themes.edit().putString("efects", gson.toJson(listCommands)).apply()
                    showNewNoteSkin(listNoteSkinAdditionals[0])
                    countSongsPlayed++
                }
                TWO_ADDITIONAL_NOTESKIN -> {
                    listCommands[0].listCommandValues.add(listNoteSkinAdditionals[1])
                    listCommands[0].listCommandValues.sortBy { it.value }
                    themes.edit().putString("efects", gson.toJson(listCommands)).apply()
                    showNewNoteSkin(listNoteSkinAdditionals[1])
                    countSongsPlayed++
                }
                THREE_ADDITIONAL_NOTESKIN -> {
                    listCommands[0].listCommandValues.add(listNoteSkinAdditionals[2])
                    listCommands[0].listCommandValues.sortBy { it.value }
                    themes.edit().putString("efects", gson.toJson(listCommands)).apply()
                    showNewNoteSkin(listNoteSkinAdditionals[2])
                    countSongsPlayed++
                }
                FOUR_ADDITIONAL_NOTESKIN -> {
                    listCommands[0].listCommandValues.add(listNoteSkinAdditionals[3])
                    listCommands[0].listCommandValues.sortBy { it.value }
                    themes.edit().putString("efects", gson.toJson(listCommands)).apply()
                    showNewNoteSkin(listNoteSkinAdditionals[3])
                    countSongsPlayed++
                }
                FIVE_ADDITIONAL_NOTESKIN -> {
                    listCommands[0].listCommandValues.add(listNoteSkinAdditionals[4])
                    listCommands[0].listCommandValues.sortBy { it.value }
                    themes.edit().putString("efects", gson.toJson(listCommands)).apply()
                    showNewNoteSkin(listNoteSkinAdditionals[4])
                    countSongsPlayed++
                }
                SIX_ADDITIONAL_NOTESKIN -> {
                    listCommands[0].listCommandValues.add(listNoteSkinAdditionals[5])
                    listCommands[0].listCommandValues.sortBy { it.value }
                    themes.edit().putString("efects", gson.toJson(listCommands)).apply()
                    showNewNoteSkin(listNoteSkinAdditionals[5])
                    countSongsPlayed++
                }
            }
            */

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

    /*
    private fun showNewNoteSkin(newNoteSkin: CommandValues) {
        val linearNewNoteSkin = LinearLayout(this).apply {
            setBackgroundColor(0xAA000000.toInt()) // Oscurece la pantalla
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT)
            setOnTouchListener { _, _ -> true }
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
        }

        val imageView = ImageView(this).apply {
            setPadding(20, 10, 20, 10)
            setImageBitmap(BitmapFactory.decodeFile(newNoteSkin.rutaCommandImg))
            layoutParams = LinearLayout.LayoutParams(
                (medidaFlechas * 4).toInt(),
                (medidaFlechas * 4).toInt()
            )
        }

        val textView = TextView(this).apply {
            text = "Felicidades!!! \n Has desbloqueado un nuevo NoteSkin \n ${newNoteSkin.value}"
            setPadding(20, 30, 20, 30)
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setTextColor(Color.parseColor("#FFEB3B"))
            setTextIsSelectable(false)
            textSize = medidaFlechas / 10f
            setTypeface(typeface, Typeface.NORMAL)
            setShadowLayer(1.6f, 1.5f, 1.3f, Color.WHITE)
        }

        val btnAceptar = Button(this).apply {
            text = "Aceptar"
            setBackgroundResource(android.R.color.transparent)
            setTextColor(Color.WHITE)
            setPadding(20, 50, 20, 10)
            setOnClickListener {
                constraintMain.removeView(linearNewNoteSkin)
            }
        }

        linearNewNoteSkin.addView(imageView)
        linearNewNoteSkin.addView(textView)
        linearNewNoteSkin.addView(btnAceptar)
        constraintMain.addView(linearNewNoteSkin)
    }
    */

    private fun actualizarImagenNumero(numero: Int) {
            val unidad = numero % 10
            val decena = numero / 10
            val bitmapUnidad = dividirPNG(unidad)
            val bitmapDecena = dividirPNG(decena)
            val bitmapNumeroCompleto = combinarBitmaps(bitmapDecena, bitmapUnidad)

            imgContador.setImageBitmap(bitmapNumeroCompleto)
        }

    private fun dividirPNG(digito: Int): Bitmap {
        val anchoTotal = bitmapNumber.width
        val anchoDigito = anchoTotal / 10
        val x = anchoDigito * digito
        return Bitmap.createBitmap(bitmapNumber, x, 0, anchoDigito, bitmapNumber.height)
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
                imgDisplay.setImageURI(Uri.parse(listEfectsDisplay[contador].rutaCommandImg))
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

    private fun moveIndicatorToPosition(position: Int) {
        val layoutManager = recyclerLvs.layoutManager as? LinearLayoutManager
        val itemView = layoutManager?.findViewByPosition(position)
        val indicatorX = itemView?.left ?: 0
        indicatorLayout.x = indicatorX.toFloat()

    }

    private fun performAction() {
        openCommandWindow()
        sequence.clear()
    }

    private fun handleButtonPress(isLeft: Boolean) {
        sequence.add(isLeft)

        if (sequence.size >= sequencePattern.size) {
            val lastElements = sequence.takeLast(sequencePattern.size)
            if (lastElements == sequencePattern) {
                performAction()
            }
        }

        if (sequence != sequencePattern.take(sequence.size)) {
            sequence.clear()
        }
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
        soundPoolSelectSongKsf.play(selectKsf, 1.0f, 1.0f, 1, 0, 1.0f)
        recyclerView.startAnimation(animOff)
        recyclerView.isVisible = false
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

        moverLvs(positionActualLvs)
    }

    private fun getBitMapGrade(positionActualLvs: Int): Bitmap {
        var bestGrade = createBitmap(100, 100)
        when (listSongScores[positionActualLvs].grade){
            "SSS+" ->{bestGrade = arrayGrades[0]}
            "SSS" ->{bestGrade = arrayGrades[1]}
            "SS+" ->{bestGrade = arrayGrades[2]}
            "SS" ->{bestGrade = arrayGrades[3]}
            "S+" ->{bestGrade = arrayGrades[4]}
            "S" ->{bestGrade = arrayGrades[5]}
            "AAA+" ->{bestGrade = arrayGrades[6]}
            "AAA" ->{bestGrade = arrayGrades[7]}
            "AA+" ->{bestGrade = arrayGrades[8]}
            "AA" ->{bestGrade = arrayGrades[9]}
            "A+" ->{bestGrade = arrayGrades[10]}
            "A" ->{bestGrade = arrayGrades[11]}
            "B" ->{bestGrade = arrayGrades[12]}
            "C" ->{bestGrade = arrayGrades[13]}
            "D" ->{bestGrade = arrayGrades[14]}
            "F" ->{bestGrade = arrayGrades[15]}
        }
        return bestGrade
    }

    private fun getWorldBitMapGrade(positionActualLvs: Int): Bitmap {
        var bestGrade = createBitmap(100, 100)
        return if(niveles[positionActualLvs].fisrtRank.size > 0){
            when (niveles[positionActualLvs].fisrtRank[0].grade){
                "SSS+" ->{bestGrade = arrayGrades[0]}
                "SSS" ->{bestGrade = arrayGrades[1]}
                "SS+" ->{bestGrade = arrayGrades[2]}
                "SS" ->{bestGrade = arrayGrades[3]}
                "S+" ->{bestGrade = arrayGrades[4]}
                "S" ->{bestGrade = arrayGrades[5]}
                "AAA+" ->{bestGrade = arrayGrades[6]}
                "AAA" ->{bestGrade = arrayGrades[7]}
                "AA+" ->{bestGrade = arrayGrades[8]}
                "AA" ->{bestGrade = arrayGrades[9]}
                "A+" ->{bestGrade = arrayGrades[10]}
                "A" ->{bestGrade = arrayGrades[11]}
                "B" ->{bestGrade = arrayGrades[12]}
                "C" ->{bestGrade = arrayGrades[13]}
                "D" ->{bestGrade = arrayGrades[14]}
                "F" ->{bestGrade = arrayGrades[15]}
            }
            bestGrade
        }else{
            bestGrade
        }
    }

    private fun openCommandWindow() {
        if(!commandWindow.isVisible){
            showCommandWindow(true)
        }
    }

    private fun showCommandWindow(ver : Boolean){
        if(ver){
            commandWindow.visibility = View.VISIBLE
            commandWindowBG.visibility = View.VISIBLE
            linearMenus.visibility = View.VISIBLE
            linearTop.visibility = View.VISIBLE
            linearCommands.visibility = View.VISIBLE
            linearInfo.visibility = View.VISIBLE
            linearBottom.visibility = View.VISIBLE
            lbCurrentBpm.visibility = View.VISIBLE
            txCurrentBpm.visibility = View.VISIBLE

            commandWindow.startAnimation(animOn)
            commandWindowBG.startAnimation(animOn)
            linearCommands.startAnimation(animOn)
            linearInfo.startAnimation(animOn)
            soundPoolSelectSongKsf.play(command_switchKsf, 1.0f, 1.0f, 1, 0, 1.0f)
            //oldValueCommand = 0
            nav_izq.performClick()
            nav_izq.performClick()
            nav_izq.performClick()
            nav_izq.performClick()
            nav_izq.performClick()
            isFocusCommandWindow(0)
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
        soundPoolSelectSongKsf.play(selectSong_backKsf, 1.0f, 1.0f, 1, 0, 1.0f)
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
        recyclerView.isVisible = true
        recyclerView.startAnimation(animOn)
        imgSelected.visibility = View.VISIBLE
        imgSelected.startAnimation(anim)

        imgLvSelected.startAnimation(animOff)
        imgBestScore.startAnimation(animOff)
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

    private fun moverLvs(positionActualLvs: Int) {
        val lv = listItemsKsf[oldValue].listKsf[positionActualLvs]
        imgLvSelected.setImageBitmap(if(lv.typePlayer == "A") difficultySelected else difficultySelectedHD)

        lbLvActive.text = lv.level

        currentLevel = lv.level
        lbBestScore.text = listSongScores[positionActualLvs].puntaje
        currentScore = lbBestScore.text.toString()

        currentBestGrade = getBitMapGrade(positionActualLvs)
        imgBestGrade.setImageBitmap(currentBestGrade)
        val currentBestWorldGrade = getWorldBitMapGrade(positionActualLvs)
        imgWorldGrade.setImageBitmap(currentBestWorldGrade)

        if(niveles[positionActualLvs].fisrtRank.isNotEmpty()) {
            lbWorldName.text = if (niveles[positionActualLvs].fisrtRank[0].nombre != "") niveles[positionActualLvs].fisrtRank[0].nombre else "---------"
            lbWorldScore.text = if (niveles[positionActualLvs].fisrtRank[0].puntaje != "") niveles[positionActualLvs].fisrtRank[0].puntaje else "-"
            currentWorldScore = listOf(
                niveles[positionActualLvs].fisrtRank[0].puntaje,
                niveles[positionActualLvs].fisrtRank[1].puntaje,
                niveles[positionActualLvs].fisrtRank[2].puntaje
            )
        }else{
            lbWorldName.text = "---------"
            lbWorldScore.text = "0"
            currentWorldScore = listOf("1000000", "1000000", "1000000")
        }

        playerSong.level = lv.level
        playerSong.stepMaker = lv.stepmaker

        layoutManager = recyclerLvs.layoutManager as LinearLayoutManager

        recyclerLvs.post {
            layoutManager.scrollToPositionWithOffset(positionActualLvs, 0)
            recyclerLvs.post {
                moveIndicatorToPosition(positionActualLvs)
            }
        }
    }

    private fun showTransitionVideo(isNext: Boolean) {
        if(isNext){
            prev.visibility = View.GONE
            next.visibility = View.VISIBLE
            next.seekTo(0) // vuelve al inicio
            next.start()
            next.setOnCompletionListener {
                next.visibility = View.GONE
                mediPlayer.start()
                if(isVideo){
                    video_fondo.start()
                }
            }
        }else {
            next.visibility = View.GONE
            prev.visibility = View.VISIBLE
            prev.seekTo(0) // vuelve al inicio
            prev.start()
            prev.setOnCompletionListener {
                prev.visibility = View.GONE
                mediPlayer.start()
                if(isVideo){
                    video_fondo.start()
                }
            }
        }
    }


    private fun moverCanciones(flecha : ImageView, animation: Animation?, oldValue: Int, isNext: Boolean = false) {
        soundPoolSelectSongKsf.play(selectSong_movKsf, 0.5f, 0.5f, 1, 0, 1.0f)
        flecha.startAnimation(animation)
        recyclerView.scrollToPosition(oldValue)
        isFocus(oldValue)
        showTransitionVideo(isNext)
        val smoothScroller: RecyclerView.SmoothScroller = CenterSmoothScroller(recyclerView.context)
        smoothScroller.targetPosition = oldValue
        recyclerView.layoutManager?.startSmoothScroll(smoothScroller)
        positionActualLvs = 0
        lbArtist.isSelected = true
        lbNameSong.isSelected = true

        if(currentChannel == "03-SHORT CUT - V2" ||
            currentChannel == "04-REMIX - V2" ||
            currentChannel == "05-FULLSONGS - V2"){
            val currentNumberChannel = File(listItemsKsf[oldValue].rutaSong).parentFile?.name!!.substringBefore("-").trim()
            if(currentNumberChannel != numberChannel){
                numberChannel = currentNumberChannel
                when(currentNumberChannel){
                    "12" ->{
                        soundPoolSelectSongSound.play(st_zero, 1.0f, 1.0f, 1, 0, 1.0f)
                    }
                    "13" ->{
                        soundPoolSelectSongSound.play(nx_nxAbs, 1.0f, 1.0f, 1, 0, 1.0f)
                    }
                    "14" ->{
                        soundPoolSelectSongSound.play(fiesta_fiesta2, 1.0f, 1.0f, 1, 0, 1.0f)
                    }
                    "17" ->{
                        soundPoolSelectSongSound.play(prime, 1.0f, 1.0f, 1, 0, 1.0f)
                    }
                    "18" ->{
                        soundPoolSelectSongSound.play(prime2, 1.0f, 1.0f, 1, 0, 1.0f)
                    }
                    "19" ->{
                        soundPoolSelectSongSound.play(aniversary_xx, 1.0f, 1.0f, 1, 0, 1.0f)
                    }
                    "21" ->{
                        soundPoolSelectSongSound.play(phoenix, 1.0f, 1.0f, 1, 0, 1.0f)
                    }
                }
            }
        }
    }
    private var isVideo: Boolean = false
    private fun isFocus (position: Int){
        val item = listItemsKsf[position]
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
        listSongScores = db.getSongScores(db.readableDatabase, currentChannel, currentSong)
        if(listSongScores.find { it.cancion == item.title } != null){
            if(item.listKsf.size != listSongScores.size){
                db.deleteCancion(item.title)
                listSongScores = arrayOf()
            }
        }
        if(listSongScores.isEmpty()){
            for (nivel in item.listKsf) {
                db.insertNivel(
                    canal = currentChannel,
                    cancion = item.title,
                    nivel = nivel.level,
                    puntaje = "0",
                    grade = ""
                )
            }
            listSongScores = db.getSongScores(db.readableDatabase, currentChannel, currentSong)
        }

        val rankingItem = listGlobalRanking.find { it.cancion == item.title }
        if(rankingItem != null){
            niveles = rankingItem.niveles
            isOficialSong = true
        }else{
            niveles = ArrayList(List(listSongScores.size) { Nivel() })
            isOficialSong = false
        }
        //niveles = rankingItem?.niveles ?: ArrayList(List(listSongScores.size) { Nivel() })
        //val salaId = UUID.randomUUID().toString()

        //if(isFileExists(File(item.rutaPrevVideo))){
        if (isFileExists(File(item.rutaPreview))) {
            isVideo = !(item.rutaPreview.endsWith(".png", true)
                    || item.rutaPreview.endsWith(".jpg", true)
                    || item.rutaPreview.endsWith(".bpm", true)
                    || item.rutaPreview.endsWith(".mpg", true)
                    || item.rutaPreview.isEmpty())

            if (isVideo) {
                // Configurar video
                video_fondo.setVideoPath(item.rutaPreview)
                video_fondo.setOnPreparedListener { it.setVolume(0f, 0f) }
                video_fondo.visibility = View.VISIBLE
                imgPrev.visibility = View.INVISIBLE
                //video_fondo.start()
                video_fondo.setOnCompletionListener { video_fondo.start() }

                playMedia(item.rutaSong, startTimeMs) // Música
            } else {
                // Configurar imagen
                imgPrev.setImageBitmap(BitmapFactory.decodeFile(item.rutaDisc))
                video_fondo.visibility = View.GONE
                imgPrev.visibility = View.VISIBLE

                playMedia(item.rutaSong, startTimeMs) // Música
            }
        } else {
            // Si no existe preview → solo imagen
            imgPrev.setImageBitmap(BitmapFactory.decodeFile(item.rutaDisc))
            video_fondo.visibility = View.GONE
            imgPrev.visibility = View.VISIBLE

            playMedia(item.rutaSong, startTimeMs) // Música
        }

        /*
        if(currentVideoPosition != 0){
            mediPlayer.seekTo(currentVideoPosition)
            mediPlayer.start()
        }
        */
        if(item.title == ""){
            lbNameSong.text = "NO TITLE"
        }else{
            lbNameSong.text = item.title
        }
        lbNameSong.startAnimation(animNameSong)

        if(item.artist == ""){
            lbArtist.text = "NO ARTIST"
        }else{
            lbArtist.text = item.artist
        }

        lbBpm.text = "BPM:" + String.format("%.2f", item.displayBpm.toDouble())
        displayBPM = item.displayBpm.replace("BPM ", "").toFloat()
        recyclerLvs.removeAllViews()
        //llenaLvs(item.listKsf)
        llenaLvsKsf(item.listKsf)
    }

    private fun playMedia(path: String, startTimeMs: Int) {
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


    class CenterSmoothScroller(context: Context?) : LinearSmoothScroller(context) {
        override fun calculateDtToFit(
            viewStart: Int,
            viewEnd: Int,
            boxStart: Int,
            boxEnd: Int,
            snapPreference: Int,
        ): Int {
            return boxStart + (boxEnd - boxStart) / 2 - (viewStart + (viewEnd - viewStart) / 2)
        }
    }

    private fun isFocusCommandWindow (position: Int){
        soundPoolSelectSongKsf.play(command_moveKsf, 1.0f, 1.0f, 1, 0, 1.0f)
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
        soundPoolSelectSongKsf.play(command_moveKsf, 1.0f, 1.0f, 1, 0, 1.0f)
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

    private fun llenaLvs(listLvs : MutableList<Lvs>){
        recyclerLvs.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = LvsAdapter(listLvs, null, sizeLvs)
        }
        recyclerLvs.onFlingListener = null
    }

    private fun llenaLvsKsf(listLvs : MutableList<Ksf>){
        recyclerLvs.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = LvsAdapter(null, listLvs, sizeLvs)
        }
        recyclerLvs.onFlingListener = null
    }

    private fun llenaLvsVacios(
        listLvs: ArrayList<Lvs>? = arrayListOf(),
        listLvsKsf: ArrayList<Ksf>? = arrayListOf(),
        recyclerNoLvs: RecyclerView
    ){
        recyclerNoLvs.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            if(listLvs != null){
                adapter = LvsAdapter(listLvs, null, (sizeLvs))
            }else if(listLvsKsf != null){
                adapter = LvsAdapter(null, listLvsKsf, (sizeLvs))
            }

        }
    }

    private fun setupRecyclerView(heightBanner: Int, widhtBanner: Int) {
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = CustomAdapter(null, listItemsKsf, heightBanner, widhtBanner)
        }
    }

    private fun createSongList(): ArrayList<Song> {
        val arraylist=ArrayList<Song>()
        for(index in 0 until listSongsChannel.size) {
            arraylist.add(Song(
                listSongsChannel[index].name,
                listSongsChannel[index].artist,
                listSongsChannel[index].bpm,
                listSongsChannel[index].tickCount,
                listSongsChannel[index].prevVideo,
                listSongsChannel[index].rutaPrevVideo,
                listSongsChannel[index].video,
                listSongsChannel[index].song,
                listSongsChannel[index].rutaBanner,
                listSongsChannel[index].rutaCancion,
                listSongsChannel[index].rutaSteps,
                listSongsChannel[index].rutaVideo,
                listSongsChannel[index].listLvs))
        }

        if(arraylist.size > 50){
            arraylist.addAll(arraylist)
        }
        if(arraylist.size > 10 && arraylist.size <= 50){
            arraylist.addAll(arraylist)
            arraylist.addAll(arraylist)
        }
        if(arraylist.size > 5 && arraylist.size <= 10){
            arraylist.addAll(arraylist)
            arraylist.addAll(arraylist)
            arraylist.addAll(arraylist)
        }
        if(arraylist.size > 3 && arraylist.size <= 5){
            arraylist.addAll(arraylist)
            arraylist.addAll(arraylist)
            arraylist.addAll(arraylist)
            arraylist.addAll(arraylist)
        }
        if(arraylist.size <= 3){
            arraylist.addAll(arraylist)
            arraylist.addAll(arraylist)
            arraylist.addAll(arraylist)
            arraylist.addAll(arraylist)
            arraylist.addAll(arraylist)
        }
        return arraylist
    }

    private fun createSongListKsf(): ArrayList<SongKsf> {
        val arraylist=ArrayList<SongKsf>()
        for(index in 0 until listSongsChannelKsf.size) {
            arraylist.add(SongKsf(
                listSongsChannelKsf[index].title,
                listSongsChannelKsf[index].artist,
                listSongsChannelKsf[index].displayBpm,
                listSongsChannelKsf[index].rutaDisc,
                listSongsChannelKsf[index].rutaTitle,
                listSongsChannelKsf[index].rutaSong,
                listSongsChannelKsf[index].rutaPreview,
                listSongsChannelKsf[index].rutaBGA,
                listSongsChannelKsf[index].listKsf))
        }

        if(arraylist.size > 50){
            arraylist.addAll(arraylist)
            arraylist.addAll(arraylist)
        }
        if(arraylist.size in 11..50){
            arraylist.addAll(arraylist)
            arraylist.addAll(arraylist)
            arraylist.addAll(arraylist)
        }
        if(arraylist.size in 6..10){
            arraylist.addAll(arraylist)
            arraylist.addAll(arraylist)
            arraylist.addAll(arraylist)
            arraylist.addAll(arraylist)
        }
        if(arraylist.size in 4..5){
            arraylist.addAll(arraylist)
            arraylist.addAll(arraylist)
            arraylist.addAll(arraylist)
            arraylist.addAll(arraylist)
            arraylist.addAll(arraylist)
        }
        if(arraylist.size <= 3){
            arraylist.addAll(arraylist)
            arraylist.addAll(arraylist)
            arraylist.addAll(arraylist)
            arraylist.addAll(arraylist)
            arraylist.addAll(arraylist)
            arraylist.addAll(arraylist)
        }
        return arraylist
    }

    private fun animaNavs(bitmap : Bitmap, spriteWidth : Int, spriteHeight : Int, frameDuration : Int): AnimationDrawable{
        val arrowSpritesRD = arrayOf(
            Bitmap.createBitmap(bitmap, 0, 0, spriteWidth, spriteHeight),
            Bitmap.createBitmap(bitmap, spriteWidth, 0, spriteWidth, spriteHeight),
            Bitmap.createBitmap(bitmap, 0, spriteHeight, spriteWidth, spriteHeight),
            Bitmap.createBitmap(bitmap, spriteWidth, spriteHeight, spriteWidth, spriteHeight))
        val animation = AnimationDrawable().apply {
            arrowSpritesRD.forEach {
                addFrame(BitmapDrawable(it), frameDuration / 4)
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
        //mediPlayer.pause()
        if(bgaSelectSong.isPlaying){
            bgaSelectSong.pause()
        }
    }

    override fun onResume() {
        super.onResume()
        recyclerView.scrollToPosition(oldValue)
        isFocus(oldValue)
        val smoothScroller: RecyclerView.SmoothScroller = CenterSmoothScroller(recyclerView.context)
        smoothScroller.targetPosition = oldValue
        recyclerView.layoutManager?.startSmoothScroll(smoothScroller)
        resetRunnable()
        detenerContador()
        bgaSelectSong.start()
        if(listEfectsDisplay.size > 0) {
            handler.postDelayed(runnable, 1200)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handlerContador.removeCallbacksAndMessages(null)
        handler.removeCallbacksAndMessages(null)
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





