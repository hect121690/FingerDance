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
import android.view.MotionEvent
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
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.fingerdance.CustomAdapter.ViewHolder.Companion.md5
import com.fingerdance.MainActivity.VideosDrive
import com.fingerdance.OptionsActivity
import com.fingerdance.ssc.ChartOffsetAdapter
import com.fingerdance.ssc.Parser
import com.fingerdance.ssc.ParserKsf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
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
private lateinit var carouselSong: RecyclerView
private lateinit var recyclerLvs: RecyclerView
private lateinit var recyclerLvsVacios: RecyclerView
private lateinit var recyclerCommands: ViewPager2
private lateinit var recyclerCommandsValues: ViewPager2

private var animOn: Animation? = null
private var animOff: Animation? = null

private val sequence = mutableListOf<Boolean>()
private val sequencePattern = listOf(false, true, false, true, false, true)

private var contador = 0
private var handlerSelectSongOnline = Handler(Looper.getMainLooper())

private val startTimeMs = 30000

//private var idAdd = ""
//private var interstitialAd: InterstitialAd? = null


private var numberChannel = ""

//private lateinit var layoutManager : LinearLayoutManager

private lateinit var difficultySelected : Bitmap
private lateinit var difficultySelectedHD : Bitmap


class SelectSongOnline : AppCompatActivity() {
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


    private lateinit var video_fondo : TextureView
    private lateinit var imgPrev: ImageView

    private lateinit var prev: TextureView
    private lateinit var next: TextureView


    private lateinit var indicatorLayout: ImageView
    private lateinit var imageCircle : ImageView

    private lateinit var bgaSelectSong: VideoView
    private lateinit var overlayBG: View
    private lateinit var imgContador: ImageView

    private lateinit var tipsArray : Array<String>
    private lateinit var txTip : TextView

    private var selectedIndex = 0
    private val visibleItems = 9
    private var firstVisible = 0
    private lateinit var artworkRepository: SongArtworkRepository
    private lateinit var carouselController: SongCarouselController
    private lateinit var previewController: SongPreviewController
    private lateinit var transitionController: SongTransitionController
    private lateinit var audioController: SongAudioController
    private var songSelectionJob: Job? = null
    private var songSelectionGeneration = 0L
    private var activityResumed = false
    private lateinit var txPlayer1Online: TextView
    private lateinit var txPlayer2Online: TextView
    private var roomListener: ValueEventListener? = null
    private var selectionSent = false
    private var gameStarted = false
    private var localReadySent = false
    private var roomLoadingStarted = false
    private var playingTransitionScheduled = false
    private var opponentLeftHandled = false
    private var loadingToGameTimer: CountDownTimer? = null

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
        getSelectChannel = false
        setContentView(R.layout.activity_select_song_online)

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                Toast.makeText(this@SelectSongOnline, "Use los botones BACK", Toast.LENGTH_SHORT).show()
            }
        })

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        onWindowFocusChanged(true)


        isOnline = true
        val txNameChannel = findViewById<TextView>(R.id.txCurrentChannel)
        txNameChannel.text = currentChannel.substringAfter("-")
        val txPlayerName = findViewById<TextView>(R.id.txPlayerName)
        txPlayerName.text = userName
        txNameChannel.layoutParams.width = (width * 0.35).toInt()
        txPlayerName.layoutParams.width = (width * 0.35).toInt()

        txPlayer1Online = findViewById(R.id.txPlayer1SelectSongOnline)
        txPlayer2Online = findViewById(R.id.txPlayer2SelectSongOnline)
        updatePlayerLabels(activeSala)

        //recyclerView = findViewById(R.id.recyclerView)
        carouselSong = findViewById(R.id.recyclerView)

        recyclerLvs = findViewById(R.id.recyclerLvs)
        recyclerLvs.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerLvs.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                // Retorna true para interceptar el toque y evitar el scroll
                return true
            }
        })
        recyclerLvsVacios = findViewById(R.id.recyclerNoLvs)
        recyclerLvsVacios.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        recyclerLvsVacios.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                // Retorna true para interceptar el toque y evitar el scroll
                return true
            }
        })
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

        indicatorLayout = findViewById(R.id.indicatorImageView)
        indicatorLayout.setImageBitmap(AppResources.bmIndicator)
        indicatorLayout.layoutParams.width = sizeLvs


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

        val yDelta = width / 40
        val animateSetTraslation = TranslateAnimation(0f, 0f, -yDelta.toFloat(), (yDelta * 2).toFloat())
        animateSetTraslation.duration = 500
        animateSetTraslation.repeatCount = Animation.INFINITE
        animateSetTraslation.repeatMode = Animation.REVERSE
        imgAceptar.startAnimation(animateSetTraslation)
        imgAceptar.bringToFront()

        val animatorSetRotation = AnimationUtils.loadAnimation(this, R.anim.animator_set_rotation)
        imageCircle.startAnimation(animatorSetRotation)

        imgSelected.layoutParams.height = (decimoHeigtn * 1.4).toInt()
        imgSelected.layoutParams.width = (width * 0.45).toInt()
        val anim = AnimationUtils.loadAnimation(this, R.anim.anim_select)
        imgSelected.startAnimation(anim)

        nav_izq = findViewById(R.id.nav_izq_song)
        nav_der = findViewById(R.id.nav_der_song)
        nav_back_Izq = findViewById(R.id.back_izq)
        nav_back_der = findViewById(R.id.back_der)

        video_fondo = findViewById(R.id.videoPreview)
        video_fondo.layoutParams.height = (height * 0.3).toInt()
        next = findViewById(R.id.next)
        prev = findViewById(R.id.preview)
        next.layoutParams.height = (height * 0.3).toInt()
        prev.layoutParams.height = (height * 0.3).toInt()
        next.visibility = View.GONE
        prev.visibility = View.GONE
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

        val listVacios = ArrayList<Ksf>()
        val rutaLvSelected = "$rutaBase/FingerDance/Themes/$tema/GraphicsStatics/img_lv_back.png"

        repeat(20) {
            listVacios.add(Ksf(steps = 0, rutaBitActive = rutaLvSelected))
        }
        llenaLvsVacios(listVacios)

        setupSongCarousel()
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
            if (imgLvSelected.isVisible && !commandWindow.isVisible) {
                soundPoolSelectSong.play(up_SelectSoundKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                hideSelectLv(anim)
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
        }
        nav_back_der.setOnClickListener() {
            ready = 0
            imgFloor.setImageBitmap(AppResources.bmFloor)
            if (carouselSong.isVisible && !commandWindow.isVisible) {
                Toast.makeText(this, "Manten presionado para volver al Select Channel", Toast.LENGTH_SHORT).show()
                soundPoolSelectSong.play(selectSong_movKsf, 1.0f, 1.0f, 1, 0, 1.0f)
            }
            if (imgLvSelected.isVisible && !commandWindow.isVisible) {
                soundPoolSelectSong.play(up_SelectSoundKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                hideSelectLv(anim)
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
        }

        nav_izq.setOnClickListener {
            ready = 0
            imgFloor.setImageBitmap(AppResources.bmFloor)
            if (carouselSong.isVisible && !commandWindow.isVisible) {

                carouselController.moveLeft()
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

                carouselController.moveRight()
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
                if (ready == 1) {
                    soundPoolSelectSong.play(selectKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                    goGameScreenActivity(anim, txPlayerName, txNameChannel)
                }

                imgAceptar.isEnabled = true

                if (ready == 0) {
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

        previewController.onResume()
        audioController.onResume()
    }

    private fun getSelectedLevelIsHalfDouble(): Boolean {
        val real = getRealIndex(oldValue)
        val song = AppResources.listSongsChannelKsf[real]
        val level = song.listKsf[positionActualLvs]

        return level.typePlayer == "B"
    }

    private fun goGameScreenActivity(anim: Animation, txPlayerName: TextView, txNameChannel: TextView) {
        if (selectionSent) return
        val real = getRealIndex(oldValue)
        val song = AppResources.listSongsChannelKsf[real]
        val level = song.listKsf[positionActualLvs]
        soundPoolSelectSong.play(startKsf, 1f, 1f, 1, 0, 1f)
        imgAceptar.isEnabled = false
        audioController.pause()
        playerSong.rutaBanner = song.rutaTitle
        playerSong.speed = txVelocidadActual.text.toString()
        if (playerSong.rutaNoteSkin != "") ruta = playerSong.rutaNoteSkin!! else {
            val directorioBase = "$rutaBase/FingerDance/NoteSkins"
            File(directorioBase).listFiles { file -> file.isDirectory && file.name.contains("default", true) }?.firstOrNull()?.let {
                ruta = it.toString()
                playerSong.rutaNoteSkin = ruta
            }
        }
        hideSelectLv(anim)
        playerSong.rutaVideo = song.rutaBGA
        playerSong.rutaCancion = song.rutaSong
        if (level.songFile != "") playerSong.rutaCancion = File(playerSong.rutaCancion!!).parent!! + "/" + level.songFile
        if (!isFileExists(File(playerSong.rutaCancion!!))) {
            val rs = File(song.rutaSong).name
            val sf = File(playerSong.rutaCancion!!).name
            playerSong.rutaCancion = playerSong.rutaCancion!!.replace(sf, rs, ignoreCase = true)
        }
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            setDataSource(File(playerSong.rutaCancion!!).absolutePath)
            prepare()
        }
        val isHalfDouble = level.typePlayer == "B"
        playerSong.level = level.level
        playerSong.player = level.typePlayer
        playerSong.type = level.typeSteps
        playerSong.chartName = level.chartName
        playerSong.stepMaker = level.stepmaker
        playerSong.difficulty = level.difficulty

        val ssc = readFileSsc(song.rutaSsc)
        val seccions = ssc.split("#NOTEDATA:;")
        chart = Parser().parseSSC("${seccions[0]}\n", seccions[level.steps], song.rutaSong)
        playerSong.isSSC = true
        if (playerSong.mirror) chart.notes = if (!isHalfDouble) Parser().makeMirror(chart.notes) else Parser().makeMirrorHD(chart.notes)
        if (playerSong.rs) chart.notes = if (!isHalfDouble) Parser().makeRandom(chart.notes) else Parser().makeRandomHD(chart.notes)

        currentSong = song.title
        currentLevel = level.level
        val selected = CancionOnline(
            rutaKsf = level.rutaKsf,
            rutaCancion = playerSong.rutaCancion ?: song.rutaSong,
            rutaBGA = song.rutaBGA,
            rutaPreview = song.rutaPreview,
            rutaBanner = song.rutaTitle,
            rutaDisc = song.rutaDisc,
            nivel = level.level,
            artists = song.artist,
            nameSong = song.title,
            bpm = displayBPM.toString(),
            isHalf = isHalfDouble
        )
        selectionSent = true
        localReadySent = true
        showWaitingForOpponentReady()
        val updates = hashMapOf<String, Any>("cancion" to selected)
        updates[if (isPlayer1) "jugador1/listo" else "jugador2/listo"] = true
        salaRef.updateChildren(updates).addOnFailureListener {
            selectionSent = false
            localReadySent = false
            imgAceptar.isEnabled = true
            Toast.makeText(this, "No se pudo guardar la selección", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updatePlayerLabels(sala: Sala) {
        txPlayer1Online.text = "Player 1\n${sala.jugador1.id}"
        txPlayer2Online.text = "Player 2\n${sala.jugador2.id}"
    }

    private fun isMyPlayerConnected(sala: Sala): Boolean {
        return if (isPlayer1) sala.jugador1.conectado else sala.jugador2.conectado
    }

    private fun isOpponentConnected(sala: Sala): Boolean {
        return if (isPlayer1) sala.jugador2.conectado else sala.jugador1.conectado
    }

    private fun bothPlayersReady(sala: Sala): Boolean {
        return sala.jugador1.listo && sala.jugador2.listo
    }

    private fun hasSelectedSong(sala: Sala): Boolean {
        return sala.cancion.nameSong.isNotBlank() &&
                sala.cancion.rutaCancion.isNotBlank() &&
                sala.cancion.rutaKsf.isNotBlank()
    }

    private fun myReady(sala: Sala): Boolean {
        return if (isPlayer1) sala.jugador1.listo else sala.jugador2.listo
    }

    private fun getOpponentName(sala: Sala): String {
        return if (isPlayer1) sala.jugador2.id else sala.jugador1.id
    }

    private fun showWaitingForOpponentReady() {
        linearLoading.isVisible = true
        imgLoading.isVisible = true
        if (activeSala.cancion.rutaBanner.isNotBlank()) {
            BitmapFactory.decodeFile(activeSala.cancion.rutaBanner)?.let {
                imgLoading.setImageBitmap(it)
            }
        }
        txTip.text = "${getOpponentName(activeSala)} preparándose, espera por favor"
        findViewById<ProgressBar>(R.id.progressBar).progress = 0
    }

    private fun showBothPlayersLoading() {
        if (roomLoadingStarted) return
        roomLoadingStarted = true
        showWaitingForOpponentReady()
        txTip.text = "Cargando partida..."
        showProgressBar(4000L)
    }

    private fun hideRoomLoading() {
        if (!linearLoading.isVisible) return
        linearLoading.isVisible = false
        imgLoading.isVisible = false
    }

    private fun handleOpponentLeftDuringOnline(sala: Sala) {
        if (opponentLeftHandled || isFinishing) return
        opponentLeftHandled = true

        OnlineRoomExitOverlay.show(
            activity = this,
            playerName = getOpponentName(sala)
        ) {
            cleanupRoomAndExitToMain()
        }
    }

    private fun cleanupRoomAndExitToMain() {
        detachRoomListener()
        loadingToGameTimer?.cancel()
        roomLoadingStarted = false
        playingTransitionScheduled = false

        val myPath = if (isPlayer1) "jugador1/conectado" else "jugador2/conectado"
        salaRef.child(myPath).onDisconnect().cancel()

        salaRef.removeValue().addOnCompleteListener {
            isOnline = false
            getSelectChannel = false
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
            finish()
        }
    }

    private fun exitOnlineToMainWithoutMessage() {
        detachRoomListener()
        loadingToGameTimer?.cancel()
        isOnline = false
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
    }

    private fun schedulePlayingTransitionIfNeeded(sala: Sala) {
        if (sala.turno != userName || playingTransitionScheduled) return
        playingTransitionScheduled = true
        loadingToGameTimer?.cancel()
        loadingToGameTimer = object : CountDownTimer(4000L, 4000L) {
            override fun onTick(millisUntilFinished: Long) = Unit

            override fun onFinish() {
                if (gameStarted || isFinishing) return
                salaRef.child("estado").setValue(RoomState.PLAYING.name).addOnFailureListener {
                    playingTransitionScheduled = false
                    roomLoadingStarted = false
                    Toast.makeText(this@SelectSongOnline, "No se pudo sincronizar el inicio", Toast.LENGTH_SHORT).show()
                }
            }
        }
        loadingToGameTimer?.start()
    }

    private fun attachRoomListener() {
        if (roomListener != null) return
        roomListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val sala = snapshot.getValue(Sala::class.java)
                if (sala == null) {
                    exitOnlineToMainWithoutMessage()
                    return
                }
                activeSala = sala
                updatePlayerLabels(sala)

                localReadySent = myReady(sala)

                if (!isMyPlayerConnected(sala)) {
                    exitOnlineToMainWithoutMessage()
                    return
                }

                if (!isOpponentConnected(sala)) {
                    handleOpponentLeftDuringOnline(sala)
                    return
                }

                opponentLeftHandled = false

                if (sala.estado == RoomState.SELECTING.name && !hasSelectedSong(sala)) {
                    roomLoadingStarted = false
                    playingTransitionScheduled = false
                    hideRoomLoading()
                    return
                }

                if (sala.estado == RoomState.SELECTING.name && !bothPlayersReady(sala) && localReadySent) {
                    roomLoadingStarted = false
                    showWaitingForOpponentReady()
                    return
                }

                if (sala.estado == RoomState.SELECTING.name && bothPlayersReady(sala) && hasSelectedSong(sala)) {
                    showBothPlayersLoading()
                    schedulePlayingTransitionIfNeeded(sala)
                    return
                }

                if (sala.estado == RoomState.PLAYING.name) {
                    startOnlineGame()
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("SelectSongOnline", "Sala: ${error.message}")
            }
        }
        salaRef.addValueEventListener(roomListener!!)
    }

    private fun detachRoomListener() {
        roomListener?.let { salaRef.removeEventListener(it) }
        roomListener = null
    }

    private fun startOnlineGame() {
        if (gameStarted || !bothPlayersReady(activeSala) || !hasSelectedSong(activeSala)) return
        gameStarted = true
        loadingToGameTimer?.cancel()
        val intent = Intent(this, GameScreenActivity::class.java)
        isVertical = true
        intent.putExtra("IS_HALF_DOUBLE", activeSala.cancion.isHalf)
        startActivity(intent)
        initGameScreen = true
        ready = 0
    }

    private fun updateRecycler() {
        val lm = recyclerLvs.layoutManager as? LinearLayoutManager ?: return
        val safeFirst = firstVisible.coerceAtLeast(0)
        recyclerLvs.post {
            if (!isFinishing && !isDestroyed && recyclerLvs.layoutManager === lm) lm.scrollToPositionWithOffset(safeFirst, 0)
        }
        indicatorLayout.x = ((selectedIndex - safeFirst) * sizeLvs).toFloat()
    }

    private fun getRealIndex(pos: Int): Int {
        val size = AppResources.listSongsChannelKsf.size
        return ((pos % size) + size) % size
    }

    private fun resetIndicatorPosition() {
        selectedIndex = 0
        firstVisible = 0
        positionActualLvs = 0
        (recyclerLvs.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(0, 0)
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
                    audioController.onResume()
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
                            previewController.onResume()
                            audioController.onResume()
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
                    audioController.onResume()
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
            val imageView = ImageView(this@SelectSongOnline).apply {
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
            val buttonsContainer = LinearLayout(this@SelectSongOnline).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            // Botón principal (Agregar/Reemplazar)
            mainButton = Button(this@SelectSongOnline).apply {
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
            val btnDownload = Button(this@SelectSongOnline).apply {
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
                handlerSelectSongOnline.postDelayed({
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

    private fun resetRunnable() {
        handlerSelectSongOnline.removeCallbacks(runnable)
        handlerSelectSongOnline.postDelayed(runnable, 0)
    }

    private val runnable: Runnable = object : Runnable {
        override fun run() {
            if (contador < listEfectsDisplay.size) {
                imgDisplay.setImageURI(listEfectsDisplay[contador].rutaCommandImg.toUri())
                contador++
            } else {
                contador = 0
            }
            handlerSelectSongOnline.postDelayed(this, 1200)
        }
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
        carouselController.stop()
        soundPoolSelectSong.play(selectKsf, 1f, 1f, 1, 0, 1f)
        carouselSong.startAnimation(animOff)
        txInfoCurrentSong.startAnimation(animOff)
        carouselSong.isVisible = false
        txInfoCurrentSong.isVisible = false
        imgSelected.clearAnimation()
        imgSelected.visibility = View.INVISIBLE
        imgLvSelected.isVisible = true
        lbLvActive.isVisible = true
        imgLvSelected.startAnimation(animOn)
        lbLvActive.startAnimation(animOn)
        moverLvs()
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
        soundPoolSelectSong.play(selectSong_backKsf, 1f, 1f, 1, 0, 1f)
        if(::audioController.isInitialized) audioController.stop()
        if(::previewController.isInitialized) previewController.onPause()
        if(::transitionController.isInitialized) transitionController.stop()
        runCatching {
            if(mediaPlayerVideo.isPlaying) mediaPlayerVideo.stop()
            mediaPlayerVideo.release()
        }
        detachRoomListener()
        handlerSelectSongOnline.removeCallbacksAndMessages(null)
        resetRunnable()
        finish()
        overridePendingTransition(0,R.anim.anim_command_window_off)
    }

    private fun hideSelectLv(anim: Animation) {
        carouselSong.isVisible = true
        carouselSong.startAnimation(animOn)
        txInfoCurrentSong.isVisible = true
        txInfoCurrentSong.startAnimation(animOn)
        imgSelected.visibility = View.VISIBLE
        imgSelected.startAnimation(anim)
        imgLvSelected.startAnimation(animOff)
        lbLvActive.startAnimation(animOff)
        imgLvSelected.isVisible = false
        lbLvActive.isVisible = false
    }

    private fun moverLvs(realPosition: Int = getRealIndex(oldValue)) {
        val songs = AppResources.listSongsChannelKsf
        if (songs.isEmpty()) return
        val safeRealPosition = realPosition.coerceIn(0, songs.lastIndex)
        val levels = songs[safeRealPosition].listKsf
        if (levels.isEmpty()) {
            resetIndicatorPosition()
            return
        }
        positionActualLvs = positionActualLvs.coerceIn(0, levels.lastIndex)
        selectedIndex = positionActualLvs
        val lv = levels[positionActualLvs]
        imgLvSelected.setImageBitmap(if(lv.typePlayer == "A") difficultySelected else difficultySelectedHD)
        lbLvActive.text = lv.level
        currentLevel = lv.level
    }

    private fun setupSongCarousel() {
        val itemWidth = width / 4.2f
        artworkRepository = SongArtworkRepository(applicationContext)
        val adapter = SongCarouselAdapter(this, artworkRepository, itemWidth.toInt(), (itemWidth * 0.75f).toInt())
        val layoutManager = SongCarouselLayoutManager(this, itemWidth)
        val snapHelper = SongCenterSnapHelper()
        carouselController = SongCarouselController(carouselSong, adapter, layoutManager, snapHelper)
        audioController = SongAudioController(startTimeMs)
        previewController = SongPreviewController(this, imgPrev, video_fondo, artworkRepository, width, (height * 0.3f).toInt())
        transitionController = SongTransitionController(prev, next, "$rutaBase/FingerDance/Themes/$tema/BGAs/prev.mp4", "$rutaBase/FingerDance/Themes/$tema/BGAs/next.mp4") { playing ->
            audioController.setTransitionPlaying(playing)
            previewController.setTransitionPlaying(playing)
        }
        carouselSong.addOnItemTouchListener(
            object : RecyclerView.SimpleOnItemTouchListener() {
                override fun onInterceptTouchEvent(
                    recyclerView: RecyclerView,
                    event: android.view.MotionEvent
                ): Boolean {
                    return true
                }
            }
        )
        carouselController.listener = object : SongCarouselController.Listener {
            override fun onTargetChanged(
                realIndex: Int,
                direction: SongCarouselController.Direction,
                newSequence: Boolean
            ) {
                oldValue = realIndex

                moverCanciones(
                    flecha = if (direction == SongCarouselController.Direction.NEXT) {
                        nav_der
                    } else {
                        nav_izq
                    },
                    isNext = direction == SongCarouselController.Direction.NEXT,
                    playTransition = newSequence
                )
            }

            override fun onSettled(
                realIndex: Int,
                direction: SongCarouselController.Direction
            ) {
                oldValue = realIndex
                isFocus(
                    position = realIndex,
                    resetLevelPosition = true
                )
            }
        }
        carouselController.setSongs(AppResources.listSongsChannelKsf, 0)
    }

    private fun moverCanciones(flecha: ImageView, isNext: Boolean = false, playTransition: Boolean = true) {
        val real = getRealIndex(oldValue)
        soundPoolSelectSong.play(selectSong_movKsf, 0.5f, 0.5f, 1, 0, 1.0f)
        flecha.startAnimation(AppResources.animPressNav)
        if (playTransition) {
            showTransitionVideo(isNext)
        }

        val item = AppResources.listSongsChannelKsf[real]
        lbNameSong.text = item.title.ifBlank { "NO TITLE" }
        lbArtist.text = item.artist.ifBlank { "NO ARTIST" }
        lbBpm.text = "BPM ${item.displayBpm}"
        txInfoCurrentSong.text = String.format("%03d/%03d", real + 1, AppResources.listSongsChannelKsf.size)
        lbArtist.isSelected = true
        lbNameSong.isSelected = true
        if (currentChannel == "03-SHORT CUT - V2" || currentChannel == "04-REMIX - V2" || currentChannel == "05-FULLSONGS - V2") {
            val currentNumberChannel = File(item.rutaSong).parentFile?.name?.substringBefore("-")?.trim().orEmpty()
            if (currentNumberChannel.isNotEmpty() && currentNumberChannel != numberChannel) {
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
    private fun isFocus(position: Int, resetLevelPosition: Boolean = true) {
        val real = getRealIndex(position)
        val item = AppResources.listSongsChannelKsf[real]
        val generation = ++songSelectionGeneration
        songSelectionJob?.cancel()
        currentPathSong = item.rutaSong
        currentSong = item.title

        songSelectionJob = lifecycleScope.launch {
            if (generation != songSelectionGeneration || isFinishing || isDestroyed) return@launch
            previewController.show(item, generation)
            audioController.prepare(item.rutaSong, generation)
            audioController.onResume()
            previewController.onResume()
            isVideo = File(item.rutaPreview).extension.lowercase() in setOf("mp4", "m4v", "3gp", "webm", "mkv", "mpg", "mpeg", "avi")
            lbNameSong.text = item.title.ifBlank { "NO TITLE" }
            lbNameSong.startAnimation(AppResources.animNameSong)
            lbArtist.text = item.artist.ifBlank { "NO ARTIST" }
            lbBpm.text = "BPM ${item.displayBpm}"
            txInfoCurrentSong.text = String.format("%03d/%03d", real + 1, AppResources.listSongsChannelKsf.size)
            displayBPM = item.displayBpm.replace("BPM ", "").substringBefore("-").toFloatOrNull() ?: 0f
            llenaLvsKsf(item.listKsf)
            if (resetLevelPosition){
                resetIndicatorPosition()
            } else {
                restoreLevelPosition(real, item.listKsf.size)
            }

        }
    }

    private fun restoreLevelPosition(realPosition: Int, levelCount: Int) {
        if (levelCount <= 0) {
            resetIndicatorPosition()
            return
        }

        positionActualLvs = positionActualLvs.coerceIn(0, levelCount - 1)
        selectedIndex = positionActualLvs

        val maxFirstVisible = (levelCount - visibleItems).coerceAtLeast(0)
        firstVisible = firstVisible.coerceIn(0, maxFirstVisible)

        when {
            selectedIndex < firstVisible -> {
                firstVisible = selectedIndex
            }

            selectedIndex >= firstVisible + visibleItems -> {
                firstVisible = (selectedIndex - visibleItems + 1).coerceAtLeast(0)
            }
        }

        updateRecycler()
        moverLvs(realPosition)
    }

    private fun showTransitionVideo(isNext: Boolean) {
        transitionController.play(if (isNext) SongCarouselController.Direction.NEXT else SongCarouselController.Direction.PREVIOUS)
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

    override fun onStart() {
        super.onStart()
        val myPath = if (isPlayer1) "jugador1/conectado" else "jugador2/conectado"
        salaRef.child(myPath).setValue(true)
        salaRef.child(myPath).onDisconnect().setValue(false)
        attachRoomListener()
    }

    override fun onStop() {
        detachRoomListener()
        super.onStop()
    }

    override fun onPause() {
        activityResumed = false
        songSelectionJob?.cancel()
        handlerSelectSongOnline.removeCallbacks(runnable)
        if (::audioController.isInitialized) audioController.onPause()
        if (::previewController.isInitialized) previewController.onPause()
        if (::transitionController.isInitialized) transitionController.stop()
        if (::bgaSelectSong.isInitialized && bgaSelectSong.isPlaying) bgaSelectSong.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        activityResumed = true
        resetRunnable()
        if (::audioController.isInitialized) audioController.onResume()
        if (::previewController.isInitialized) previewController.onResume()
        if (::carouselController.isInitialized) {
            val index = carouselController.selectedIndex()
            oldValue = index
            isFocus(
                position = index,
                resetLevelPosition = false
            )
        }
        if (::bgaSelectSong.isInitialized) runCatching { bgaSelectSong.start() }
        if(listEfectsDisplay.isNotEmpty()) handlerSelectSongOnline.postDelayed(runnable, 1200)

        isEndingFade = false
        endingFadeAlpha = 0f
    }

    override fun onDestroy() {
        songSelectionGeneration++
        songSelectionJob?.cancel()
        downloadJob?.cancel()
        loadingToGameTimer?.cancel()
        handlerSelectSongOnline.removeCallbacksAndMessages(null)
        if (::transitionController.isInitialized) transitionController.release()
        if (::previewController.isInitialized) previewController.release()
        if (::audioController.isInitialized) audioController.release()
        if (::bgaSelectSong.isInitialized) runCatching {
            bgaSelectSong.setOnPreparedListener(null)
            bgaSelectSong.setOnCompletionListener(null)
            bgaSelectSong.stopPlayback()
        }
        runCatching {
            if (::mediaPlayerVideo.isInitialized) {
                if (mediaPlayerVideo.isPlaying) mediaPlayerVideo.stop()
                mediaPlayerVideo.release()
            }
        }
        super.onDestroy()
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

