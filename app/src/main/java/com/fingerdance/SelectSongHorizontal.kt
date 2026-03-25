package com.fingerdance

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
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
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.GradientDrawable.RECTANGLE
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.util.TypedValue
import android.view.Choreographer
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import androidx.core.net.toUri
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
import com.fingerdance.MainActivity.VideosDrive
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
import androidx.core.graphics.toColorInt

private val handlerSelectSongHorizontal = Handler(Looper.getMainLooper())

class SelectSongHorizontal : AppCompatActivity() {

    private var rutaBase = ""
    private lateinit var constraintMain: ConstraintLayout
    private lateinit var linearBackground: LinearLayout
    private lateinit var imgPreview: ImageView
    private lateinit var btnBackLeft: ImageView
    private lateinit var btnBackRight: ImageView
    private lateinit var btnMoveLeft: ImageView
    private lateinit var btnMoveRight: ImageView
    private lateinit var imgCircleRotate: ImageView
    private lateinit var imgCursor: ImageView

    private lateinit var lbNameSong: TextView
    private lateinit var lbArtist: TextView
    private lateinit var lbBpm: TextView

    private lateinit var commandWindow: ConstraintLayout
    private lateinit var linearTop: LinearLayout
    private lateinit var linearMenus: LinearLayout

    private lateinit var linearCurrent: LinearLayout
    private lateinit var linearValues: LinearLayout
    private lateinit var linearCommands: LinearLayout
    private lateinit var linearInfo: LinearLayout
    private lateinit var linearBottom: LinearLayout

    private lateinit var recyclerCommands: ViewPager2
    private lateinit var recyclerCommandsValues: ViewPager2

    private lateinit var lbCurrentBpm: TextView
    private lateinit var txCurrentBpm: TextView
    private lateinit var txInfoCW: TextView
    private lateinit var imgVelocidadActual: ImageView
    private lateinit var txVelocidadActual: TextView
    private lateinit var imgDisplay: ImageView
    private lateinit var imgJudge: ImageView
    private lateinit var imgNoteSkin: ImageView
    private lateinit var imgNoteSkinFondo: ImageView

    private lateinit var imgOffset: ImageView
    private lateinit var txOffset: TextView

    private lateinit var animOn: Animation
    private lateinit var animOff: Animation
    private lateinit var animPressNav: Animation
    private lateinit var animNameSong: Animation
    private lateinit var animSelect: Animation

    private lateinit var animNavIzq: SpriteAnimator
    private lateinit var animNavDer: SpriteAnimator
    private lateinit var animBackIzq: SpriteAnimator
    private lateinit var animBackDer: SpriteAnimator

    private lateinit var previewTextureView: TextureView
    private lateinit var previewMediaPlayer: MediaPlayer

    private lateinit var songCarouselSongs: SongsCarouselView

    private lateinit var constraintLvs: ConstraintLayout
    private lateinit var linearLvs : ConstraintLayout
    private lateinit var recyclerLvs : RecyclerView
    private lateinit var recyclerLvsEmpty: RecyclerView
    private lateinit var indicatorLayout: ImageView

    private lateinit var linearLvAndRanking: ConstraintLayout
    private lateinit var imgLvSelected: ImageView
    private lateinit var lbLvActive: TextView

    private lateinit var imgBestScore: ImageView
    private lateinit var lbBestScore: TextView
    private lateinit var imgBestGrade: ImageView

    private lateinit var lbWorldName: TextView
    private lateinit var lbWorldScore: TextView
    private lateinit var imgWorldGrade: ImageView

    private lateinit var linearPressStart: ConstraintLayout
    private lateinit var imgPressStart: ImageView
    private lateinit var imgFlowPressStart: ImageView

    private lateinit var linearLoading: ConstraintLayout
    private lateinit var imgLoading: ImageView

    private lateinit var txTip: TextView
    private lateinit var tipsArray: Array<String>

    private lateinit var buttonLayout: LinearLayout
    private lateinit var overlayBG: View

    private lateinit var animationPressStart: Animation

    private val difficultedSelected = AppResources.difficultedSelected
    private val difficultedSelectedHD = AppResources.difficultedSelectedHD

    private lateinit var niveles: ArrayList<Nivel>

    private var contador = 0

    private val choreographer = Choreographer.getInstance()

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (::songCarouselSongs.isInitialized) {
                songCarouselSongs.update()
            }
            choreographer.postFrameCallback(this)
        }
    }

    private val startTimeMs = 30000
    private var timer: CountDownTimer? = null
    private var isTimerRunning = false
    private var mediaPlayerSelectSongHorizontal: MediaPlayer = MediaPlayer()

    private var selectedIndex = 0
    private val visibleItems = 12
    private var firstVisible = 0
    private var sizeLvs = 0
    private val imageCache = mutableMapOf<String, Bitmap>()
    private lateinit var progressLoading : ProgressBar

    private val sequence = mutableListOf<Boolean>()
    private val sequencePattern = listOf(false, true, false, true, false, true)

    private val pickPreviewFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val namePreview = File(AppResources.listSongsChannelKsf[oldValue].rutaSong).name.replace(".mp3", "")
            saveFileToDestination(it, namePreview + "_p.mp4", false)
        }
    }

    private val pickBgaFile = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            val nameBGA = File(AppResources.listSongsChannelKsf[oldValue].rutaSong).name.replace(".mp3", "")
            saveFileToDestination(it, nameBGA + ".mp4", true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_select_song_horizontal)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        rutaBase = getExternalFilesDir(null)!!.absolutePath

        val screenWidth = resources.displayMetrics.widthPixels
        val screenHeight = resources.displayMetrics.heightPixels

        spaceInitHorizontal = (screenWidth / 2f) - (medidaFlechasHorizontal * 3.5f)

        mediPlayer = MediaPlayer()

        constraintMain = findViewById(R.id.main)
        progressLoading = findViewById(R.id.progressLoadingHorizontal)
        linearBackground = findViewById(R.id.linearBackground)
        animPressNav = AnimationUtils.loadAnimation(this, R.anim.press_nav)
        animNameSong = AnimationUtils.loadAnimation(this, R.anim.anim_name_song)
        animOn = AnimationUtils.loadAnimation(this, R.anim.anim_command_window_on)
        animOff = AnimationUtils.loadAnimation(this, R.anim.anim_command_window_off)

        recyclerCommands = findViewById(R.id.recyclerCommandsHorizontal)
        recyclerCommands.isUserInputEnabled = false
        recyclerCommandsValues = findViewById(R.id.recyclerValuesHorizontal)
        recyclerCommandsValues.isUserInputEnabled = false

        commandWindow = findViewById(R.id.command_windowHorizontal)
        commandWindow.visibility = View.GONE
        commandWindow.background = Drawable.createFromPath("${rutaBase}/FingerDance/Themes/$tema/GraphicsStatics/command_window/Command_Frame.png")
        commandWindow.layoutParams.height = (screenHeight * 0.85).toInt()
        commandWindow.layoutParams.width = (screenWidth * 0.25).toInt()

        val fondos = Drawable.createFromPath("$rutaBase/FingerDance/Themes/$tema/GraphicsStatics/command_window/Command_Back.png")
        linearTop = findViewById(R.id.linearTopHorizontal)
        linearMenus = findViewById(R.id.linearMenusHorizontal)

        linearCurrent = findViewById(R.id.linearCurrentHorizontal)
        linearCurrent.background = fondos
        linearValues = findViewById(R.id.linearValuesHorizontal)
        linearValues.background = fondos
        linearCommands = findViewById(R.id.linearCommandsHorizontal)
        linearCommands.background = fondos
        linearInfo = findViewById(R.id.linearInfoHorizontal)
        linearInfo.background = fondos
        linearBottom = findViewById(R.id.linearBottomHorizontal)

        lbCurrentBpm = findViewById(R.id.lbCurrentBpmHorizontal)
        txCurrentBpm = findViewById(R.id.txCurrentBpmHorizontal)

        imgVelocidadActual = findViewById(R.id.imgVelocidadActualHorizontal)
        imgVelocidadActual.setImageBitmap(AppResources.bmCommandEmpty)
        txVelocidadActual = findViewById(R.id.txVelocidadActualHorizontal)

        imgOffset = findViewById(R.id.imgOffsetActualHorizontal)
        imgOffset.setImageBitmap(AppResources.bmCommandEmpty)
        txOffset = findViewById(R.id.txOffsetActualHorizontal)
        txOffset.text = "0"

        imgDisplay = findViewById(R.id.imgDisplayHorizontal)
        imgDisplay.isVisible=false
        imgJudge = findViewById(R.id.imgJudgeHorizontal)
        imgJudge.isVisible=false
        imgNoteSkin = findViewById(R.id.imgNoteSkinHorizontal)
        imgNoteSkin.isVisible=false
        imgNoteSkinFondo = findViewById(R.id.imgNoteSkinFondoHorizontal)
        imgNoteSkinFondo.setImageBitmap(AppResources.bmCommandEmpty)
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

        val anchoRecyclerCommands = linearMenus.layoutParams.width / 3
        recyclerCommands.layoutParams.width = anchoRecyclerCommands - (anchoRecyclerCommands / 20)
        recyclerCommandsValues.layoutParams.width = anchoRecyclerCommands

        val anchoTxInfo = linearMenus.layoutParams.width - linearMenus.layoutParams.width / 5
        txInfoCW = findViewById(R.id.txInfoHorizontal)
        txInfoCW.layoutParams.width = anchoTxInfo

        showCommandWindow(false)

        linearPressStart = findViewById(R.id.linearPressStart)
        linearPressStart.visibility = View.GONE
        linearPressStart.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                // No hace nada
            }
        })
        imgPressStart = findViewById(R.id.imgPressStart)
        imgPressStart.layoutParams.width = (screenWidth * 0.3).toInt()
        imgPressStart.setImageBitmap(AppResources.bmAceptar)

        imgFlowPressStart = findViewById(R.id.imgFlowPressStart)
        imgFlowPressStart.layoutParams.width = (screenWidth * 0.3).toInt()
        imgFlowPressStart.setImageBitmap(AppResources.bmFloor2)

        val yDelta = width / 40
        animationPressStart = TranslateAnimation(0f, 0f, -yDelta.toFloat(), (yDelta * 2).toFloat())
        animationPressStart.duration = 500
        animationPressStart.repeatCount = Animation.INFINITE
        animationPressStart.repeatMode = Animation.REVERSE

        imgPreview = findViewById(R.id.imgPreview)
        previewTextureView = findViewById(R.id.bgaPreview)

        val paramsImgPreview = imgPreview.layoutParams
        paramsImgPreview.width = (screenWidth).toInt()
        paramsImgPreview.height = (screenWidth * 0.90).toInt()
        imgPreview.layoutParams = paramsImgPreview
        imgPreview.y = (screenHeight * 0.10).toFloat()

        val paramsTextureViewPreview = previewTextureView.layoutParams
        paramsTextureViewPreview.width = (screenWidth).toInt()
        paramsTextureViewPreview.height = (screenWidth * 0.90).toInt()
        previewTextureView.layoutParams = paramsTextureViewPreview
        previewTextureView.y = (screenHeight * 0.10).toFloat()

        imgCircleRotate = findViewById(R.id.imgCircleRotate)
        btnBackLeft = findViewById(R.id.btnBackLeftSong)
        btnBackRight = findViewById(R.id.btnBackRightSong)
        btnMoveLeft = findViewById(R.id.btnMoveLeftSong)
        btnMoveRight = findViewById(R.id.btnMoveRightSong)
        imgCursor = findViewById(R.id.imgSelectedHorizontal)

        lbNameSong = findViewById(R.id.lbNameSongHorizontal)
        lbArtist = findViewById(R.id.lbArtistHorizontal)
        lbBpm = findViewById(R.id.lbBpmHorizontal)

        songCarouselSongs = findViewById(R.id.songCarouselSongs)

        constraintLvs = findViewById(R.id.constraintLvs)
        linearLvs = findViewById(R.id.linearLvs)
        recyclerLvs = findViewById(R.id.recyclerLvsHorizontal)
        recyclerLvsEmpty = findViewById(R.id.recyclerLvsEmptyHorizontal)
        indicatorLayout = findViewById(R.id.indicatorCurrentLv)

        linearLvAndRanking = findViewById(R.id.linearLvAndRanking)
        linearLvAndRanking.visibility = View.GONE
        imgLvSelected = findViewById(R.id.imgLvSelectedHorizontal)
        lbLvActive = findViewById(R.id.lbLvActiveHorizontal)
        imgBestScore = findViewById(R.id.imgBestScoresHorizontal)
        lbBestScore = findViewById(R.id.lbMyBestScoreHorizontal)
        imgBestGrade = findViewById(R.id.imgMyBestGradeHorizontal)

        lbWorldName = findViewById(R.id.lbWorldNameHorizontal)
        lbWorldScore = findViewById(R.id.lbWorldScoreHorizontal)
        imgWorldGrade = findViewById(R.id.imgWorldGradeHorizontal)

        val valueExpand = (decimoHeigtn).toInt()
        val buttons = listOf(
            btnBackLeft,
            btnBackRight,
            btnMoveLeft,
            btnMoveRight
        )

        buttons.forEach { button ->
            button.layoutParams.width = valueExpand
            button.layoutParams.height = valueExpand
            button.requestLayout()
        }

        animNavIzq = SpriteAnimator(btnMoveLeft, AppResources.arrowNavIzq)
        animNavDer = SpriteAnimator(btnMoveRight, AppResources.arrowNavDer)
        animBackIzq = SpriteAnimator(btnBackLeft, AppResources.arrowBackIzqColor)
        animBackDer = SpriteAnimator(btnBackRight, AppResources.arrowBackDerColor)

        animNavIzq.start()
        animNavDer.start()
        animBackIzq.start()
        animBackDer.start()

        imgCursor.setImageBitmap(AppResources.bmCursor)
        indicatorLayout.setImageBitmap(AppResources.bmIndicator)

        niveles = arrayListOf<Nivel>()

        imgCircleRotate.setImageBitmap(AppResources.bmCircle)
        imgCircleRotate.layoutParams.width = (screenWidth * 0.8).toInt()
        imgCircleRotate.layoutParams.height = (screenWidth * 0.8).toInt()
        imgCircleRotate.requestLayout()

        val animator = ObjectAnimator.ofFloat(imgCircleRotate, "rotation", 0f, 360f)
        animator.duration = 180000
        animator.repeatCount = ValueAnimator.INFINITE
        animator.interpolator = LinearInterpolator()
        animator.start()

        sizeLvs = (screenWidth / 23.99).toInt()
        indicatorLayout.layoutParams.width = sizeLvs
        indicatorLayout.visibility = View.INVISIBLE
        linearLvs.layoutParams.height = (sizeLvs * 1.5).toInt()
        indicatorLayout.requestLayout()

        val drawable = Drawable.createFromPath("${rutaBase}/FingerDance/Themes/background.png")

        val color = when(tema){
            "Fiesta 2" -> "#FF4FA3".toColorInt() //Rosa
            "Phoenix" -> "#FF8800".toColorInt() //Naranja
            "Prime" ->  "#66B2FF".toColorInt() //Purpura
            else -> "#800020".toColorInt() //Guinda oscuro
        }

        drawable?.setColorFilter(color, PorterDuff.Mode.MULTIPLY)

        linearBackground.background = drawable

        val listVacios = ArrayList<Ksf>()
        val rutaLvSelected = "${rutaBase}/FingerDance/Themes/$tema/GraphicsStatics/img_lv_back.png"

        repeat(20) {
            listVacios.add(Ksf("", "", rutaLvSelected))
        }
        llenaLvsVacios(listVacios)
        llenaCommands(listCommands)
        prepareGradeBitmaps()

        val paramsSelected = imgCursor.layoutParams
        paramsSelected.height = (screenHeight * 0.25).toInt()
        paramsSelected.width = (screenWidth * 0.18).toInt()
        imgCursor.layoutParams = paramsSelected
        animSelect = AnimationUtils.loadAnimation(this, R.anim.anim_select)
        imgCursor.startAnimation(animSelect)

        previewMediaPlayer = MediaPlayer()
        previewTextureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, w: Int, h: Int) {
                previewMediaPlayer.setSurface(Surface(surface))
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, w: Int, h: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

        }

        songCarouselSongs.setSongs(AppResources.listSongsChannelKsf)

        playerSong = PlayerSong("","", "",0.0,0.0, 0.0, "","",false, false,"", "", "")

        var textSize = (screenWidth * 0.06).toInt()
        lbLvActive.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
        val frameBestScoreWidth = (screenWidth * 0.3).toInt()
        imgBestScore.setImageBitmap(AppResources.bmBestScore)
        imgBestScore.layoutParams.width = frameBestScoreWidth
        lbBestScore.layoutParams.width = (frameBestScoreWidth * 0.4).toInt()
        imgBestGrade.layoutParams.width = (screenWidth * 0.1).toInt()
        lbWorldName.layoutParams.width = (frameBestScoreWidth * 0.6).toInt()
        lbWorldScore.layoutParams.width = (frameBestScoreWidth * 0.4).toInt()
        imgWorldGrade.layoutParams.width = (screenWidth * 0.1).toInt()

        textSize = (screenWidth * 0.015).toInt()
        lbBestScore.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
        lbWorldScore.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
        lbWorldName.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())

        lbBpm.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
        lbCurrentBpm.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())

        linearLoading = findViewById(R.id.linearLoadingHorizontal)
        imgLoading = findViewById(R.id.imgLoadingHorizontal)
        imgLoading.layoutParams.width = width

        tipsArray = resources.getStringArray(R.array.tips_array)
        txTip = findViewById<TextView>(R.id.txTipHorizontal)

        linearLoading.isVisible = false

        focusOnSong()

        val rankingView = findViewById<TopRankingView>(R.id.topRankingViewHorizontal)
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

        btnBackLeft.setOnLongClickListener {
            ready = 0
            goSelectChannelHorizontal()

            true
        }
        btnBackRight.setOnLongClickListener(){
            ready = 0
            goSelectChannelHorizontal()

            true
        }

        imgPreview.setOnLongClickListener {
            showOverlay(false)
            true
        }
        previewTextureView.setOnLongClickListener {
            showOverlay(true)
            true
        }

        btnBackLeft.setOnClickListener {
            ready = 0
            it.startAnimation(animPressNav)
            if(songCarouselSongs.carouselVisible && !commandWindow.isVisible){
                Toast.makeText(this, "Manten presionado para volver al Selecet Channel", Toast.LENGTH_SHORT).show()
                soundPoolSelectSongKsf.play(selectSong_movKsf, 1.0f, 1.0f, 1, 0, 1.0f)
            }
            if (imgLvSelected.isVisible && !commandWindow.isVisible && !rankingView.isVisible && !linearPressStart.isVisible) {
                soundPoolSelectSongKsf.play(up_SelectSoundKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                hideLvs()
                if (!songCarouselSongs.carouselVisible) {
                    showCarouselSongs()
                }
            }
            if(rankingView.isVisible){
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
            if(linearPressStart.isVisible){
                soundPoolSelectSongKsf.play(up_SelectSoundKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                linearPressStart.visibility = View.GONE
            }
        }
        btnBackRight.setOnClickListener {
            ready = 0
            it.startAnimation(animPressNav)
            if(songCarouselSongs.carouselVisible && !commandWindow.isVisible){
                Toast.makeText(this, "Manten presionado para volver al Selecet Channel", Toast.LENGTH_SHORT).show()
                soundPoolSelectSongKsf.play(selectSong_movKsf, 1.0f, 1.0f, 1, 0, 1.0f)
            }
            if(songCarouselSongs.carouselVisible && !commandWindow.isVisible){
                Toast.makeText(this, "Manten presionado para volver al Selecet Channel", Toast.LENGTH_SHORT).show()
                soundPoolSelectSongKsf.play(selectSong_movKsf, 1.0f, 1.0f, 1, 0, 1.0f)
            }
            if (imgLvSelected.isVisible && !commandWindow.isVisible && !rankingView.isVisible && !linearPressStart.isVisible) {
                soundPoolSelectSongKsf.play(up_SelectSoundKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                hideLvs()
                if(!songCarouselSongs.carouselVisible){
                    showCarouselSongs()
                }
            }
            if(rankingView.isVisible){
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
            if(linearPressStart.isVisible){
                soundPoolSelectSongKsf.play(up_SelectSoundKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                linearPressStart.visibility = View.GONE
            }
        }

        btnMoveLeft.setOnClickListener {
            ready = 0
            if(songCarouselSongs.carouselVisible && !commandWindow.isVisible){
                songCarouselSongs.moveLeft()
                moveSongs(btnMoveLeft)
            }
            if(imgLvSelected.isVisible && !commandWindow.isVisible){
                soundPoolSelectSongKsf.play(move_lvsKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                if (handleButtonPress(false)) return@setOnClickListener
                if (selectedIndex > 0) {
                    selectedIndex--
                    positionActualLvs = selectedIndex
                    moveLvs()
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

        btnMoveRight.setOnClickListener {
            ready = 0
            if(songCarouselSongs.carouselVisible && !commandWindow.isVisible){
                songCarouselSongs.moveRight()
                moveSongs(btnMoveRight)
            }
            if(imgLvSelected.isVisible && !commandWindow.isVisible){
                soundPoolSelectSongKsf.play(move_lvsKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                if (handleButtonPress(true)) return@setOnClickListener
                val total = recyclerLvs.adapter!!.itemCount
                if (selectedIndex < total - 1) {
                    selectedIndex++
                    positionActualLvs = selectedIndex
                    moveLvs()
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

        imgCursor.setOnClickListener {
            hideCarouselSongs()
            showLvs()
        }

        commandWindow.setOnClickListener {
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
                                txVelocidadActual.text = txCurrentBpm.text
                            }else{
                                txCurrentBpm.text = formattedResult //result.toString() + "X"
                                txVelocidadActual.text = txCurrentBpm.text
                            }
                        }else{
                            if(result >= BigDecimal(8.0)) {
                                txCurrentBpm.text = "8.0X"
                                txVelocidadActual.text = txCurrentBpm.text
                            }else{
                                txCurrentBpm.text = formattedResult //result.toString() + "X"
                                txVelocidadActual.text = txCurrentBpm.text
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

        imgBestScore.setOnClickListener{
            if(niveles[positionActualLvs].fisrtRank.isNotEmpty()){
                if(!commandWindow.isVisible){
                    soundPoolSelectSongKsf.play(selectKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                    rankingView.visibility = View.VISIBLE
                    rankingView.startAnimation(animOn)
                    rankingView.setIconDrawable(imgLvSelected.drawable)
                    rankingView.setNiveles(niveles[positionActualLvs])
                    constraintMain.addView(linearRanking)
                    btnBackRight.bringToFront()
                    btnBackLeft.bringToFront()
                    rankingView.bringToFront()
                }
            }
        }

        imgPreview.setOnClickListener {
            if(linearLvAndRanking.isVisible && !commandWindow.isVisible && !rankingView.isVisible){
                ready = 1
                showPressStart()
            }
        }

        previewTextureView.setOnClickListener {
            if(linearLvAndRanking.isVisible && !commandWindow.isVisible && !rankingView.isVisible){
                ready = 1
                showPressStart()
            }
        }

        imgPressStart.setOnClickListener {
            goGameScreenActivity()
        }

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
                shape = RECTANGLE
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
            text = "✖ CANCELAR"
            setTextColor(0xFFFF4444.toInt())
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            isAllCaps = false

            background = GradientDrawable().apply {
                shape = RECTANGLE
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

        if(File(AppResources.listSongsChannelKsf[oldValue].rutaPreview).exists()){
            previewDowloaded = true
            textBtnDownloadPreview = "Eliminar Preview"
        }else{
            previewDowloaded = false
            textBtnDownloadPreview = "Descargar Preview " + if(sizePreview.isNotEmpty()) "(${ "%.2f".format(sizePreview.toLong() / (1024.0 * 1024.0))} MB)" else ""
        }

        if(File(AppResources.listSongsChannelKsf[oldValue].rutaBGA).exists()){
            bgaDowloaded = true
            textBtnDownloadBga = "Eliminar BGA"
        }else{
            bgaDowloaded = false
            textBtnDownloadBga = "Descargar BGA " + if(sizeBga.isNotEmpty()) "(${ "%.2f".format(sizeBga.toLong() / (1024.0 * 1024.0))} MB)" else ""
        }

        // ---------- FILA PREVIEW ----------
        val previewRowData = createMediaRow(
            imageAsset = "preview.png",
            mainButtonText = if(isBGA) "Reemplazar Preview" else "Agregar Preview",
            downloadButtonText = textBtnDownloadPreview,
            onMainClick = { pickPreviewFile.launch(arrayOf("video/mp4")) },
            existDownloaded = previewDowloaded,
            existInDrive = existPreviewDrive,
            onDownloadClick = if(previewDowloaded){
                {
                    File(AppResources.listSongsChannelKsf[oldValue].rutaPreview).delete()
                    focusOnSong()
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
                            focusOnSong()
                            btnCancel.performClick()
                            previewMediaPlayer.start()
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
            mainButtonText = "Agregar BGA",
            downloadButtonText = textBtnDownloadBga,
            onMainClick = { pickBgaFile.launch(arrayOf("video/mp4")) },
            existDownloaded = bgaDowloaded,
            existInDrive = existBgaDrive,
            onDownloadClick = if(bgaDowloaded){
                {
                    File(AppResources.listSongsChannelKsf[oldValue].rutaBGA).delete()
                    focusOnSong()
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
        val listSongsDrive = listChannelsDrive.find { it.name == currentChannel }?.songs ?: emptyList()
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
                File(AppResources.listSongsChannelKsf[oldValue].rutaBGA)
            } else {
                File(AppResources.listSongsChannelKsf[oldValue].rutaPreview)
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
            val imageView = ImageView(this@SelectSongHorizontal).apply {
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
            val buttonsContainer = LinearLayout(this@SelectSongHorizontal).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            // Botón principal (Agregar/Reemplazar)
            mainButton = Button(this@SelectSongHorizontal).apply {
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
            val btnDownload = Button(this@SelectSongHorizontal).apply {
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

    private fun goGameScreenActivity(){
        soundPoolSelectSongKsf.play(startKsf, 1.0f, 1.0f, 1, 0, 1.0f)

        val bit = BitmapFactory.decodeFile(AppResources.listSongsChannelKsf[oldValue].rutaDisc)
        imgLoading.setImageBitmap(bit)
        linearLoading.isVisible = true
        linearLoading.bringToFront()
        linearLoading.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                // No hace nada
            }
        })
        imgLoading.isVisible = true
        showProgressBar()
        mediPlayer.pause()
        playerSong.rutaBanner = AppResources.listSongsChannelKsf[oldValue].rutaDisc
        mediaPlayerSelectSongHorizontal.pause()
        txTip.text = tipsArray[Random.nextInt(tipsArray.size)]

        playerSong.speed = txVelocidadActual.text.toString()
        if(playerSong.rutaNoteSkin != ""){
            ruta = playerSong.rutaNoteSkin!!
        }else{
            val directorioBase = "${com.fingerdance.rutaBase}/FingerDance/NoteSkins"
            val directorios = File(directorioBase).listFiles { file ->
                file.isDirectory && file.name.contains("default", ignoreCase = true)
            }
            if (directorios != null) {
                ruta = directorios.firstOrNull().toString()
                playerSong.rutaNoteSkin = ruta
            }
        }
        //hideLvs()
        playerSong.rutaVideo = AppResources.listSongsChannelKsf[oldValue].rutaBGA
        playerSong.rutaCancion = AppResources.listSongsChannelKsf[oldValue].rutaSong

        if(AppResources.listSongsChannelKsf[oldValue].listKsf[positionActualLvs].songFile != ""){
            playerSong.rutaCancion = File(playerSong.rutaCancion!!).parent!! + "/" + AppResources.listSongsChannelKsf[oldValue].listKsf[positionActualLvs].songFile
        }

        if(!isFileExists(File(playerSong.rutaCancion!!))) {
            val rs = File(AppResources.listSongsChannelKsf[oldValue].rutaSong).name
            val sf = File(playerSong.rutaCancion!!).name
            playerSong.rutaCancion = playerSong.rutaCancion!!.replace(sf, rs, ignoreCase = true)
        }

        playerSong.rutaKsf = AppResources.listSongsChannelKsf[oldValue].listKsf[positionActualLvs].rutaKsf

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
        val isHalfDouble = AppResources.listSongsChannelKsf[oldValue].listKsf[positionActualLvs].typePlayer == "B"
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
                if(currentChannel == "06-FAVORITES"){
                    val nameChannels = AppResources.listSongsChannelKsf.find { it.title == lbNameSong.text.toString() }?.channel
                    channelIndex = validFolders.indexOf(nameChannels)
                    val canciones = mockListChannels.find { it.canal == nameChannels }?.canciones
                    songIndex = canciones?.indexOfFirst { it.cancion == currentSong } ?: -1
                }else {
                    channelIndex = validFolders.indexOf(currentChannel)
                    songIndex = listGlobalRanking.indexOfFirst{ it.cancion == currentSong }
                }
                levelIndex = positionActualLvs
            }
        }

        handlerSelectSongHorizontal.postDelayed({

            //mediPlayer.pause()
            val intent = Intent(this, GameScreenActivityHorizontal()::class.java)
            intent.putExtra("IS_HALF_DOUBLE", isHalfDouble)
            startActivity(intent)
            handlerSelectSongHorizontal.postDelayed({
                linearLoading.isVisible = false
                linearPressStart.isVisible = false
                imgLoading.isVisible = false
            }, 1000L)
            ready = 0
        }, 3000L)
    }

    private fun showPressStart(){
        linearPressStart.visibility = View.VISIBLE
        imgPressStart.startAnimation(animationPressStart)
        soundPoolSelectSongKsf.play(startKsf, 1.0f, 1.0f, 1, 0, 1.0f)
        imgPressStart.bringToFront()
        btnBackLeft.bringToFront()
        btnBackRight.bringToFront()
    }

    private fun handleButtonPress(isLeft: Boolean): Boolean {
        sequence.add(isLeft)
        if (sequence.size >= sequencePattern.size) {
            val lastElements = sequence.takeLast(sequencePattern.size)
            if (lastElements == sequencePattern) {
                openCommandWindow()
                return true
            }
        }
        if (sequence != sequencePattern.take(sequence.size)) {
            sequence.clear()
        }
        return false
    }

    private fun openCommandWindow() {
        if(!commandWindow.isVisible){
            showCommandWindow(true)
        }
        sequence.clear()
    }

    private fun showCommandWindow(show : Boolean) {
        if (show) {
            commandWindow.visibility = View.VISIBLE
            linearMenus.visibility = View.VISIBLE
            linearTop.visibility = View.VISIBLE
            linearCommands.visibility = View.VISIBLE
            linearInfo.visibility = View.VISIBLE
            linearBottom.visibility = View.VISIBLE
            lbCurrentBpm.visibility = View.VISIBLE
            txCurrentBpm.visibility = View.VISIBLE

            commandWindow.startAnimation(animOn)
            linearCommands.startAnimation(animOn)
            linearInfo.startAnimation(animOn)
            soundPoolSelectSongKsf.play(command_switchKsf, 1.0f, 1.0f, 1, 0, 1.0f)
            isFocusCommandWindow(1)
        } else {
            commandWindow.visibility = View.GONE
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
            linearCurrent.startAnimation(animOff)
            linearValues.startAnimation(animOff)
            linearCommands.startAnimation(animOff)
            linearInfo.startAnimation(animOff)
            sequence.clear()
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

    private fun getRutaNoteSkin(rutaOriginal: String): String {
        return rutaOriginal.removeSuffix("_Icon.png")
    }

    private fun showProgressBar() {
        var currentTime: Long

        val progressBar = findViewById<ProgressBar>(R.id.progressBarHorizontal)
        val maxProgress = progressBar.max

        val timer = object : CountDownTimer(4000, 1) {
            override fun onTick(millisUntilFinished: Long) {
                currentTime = 4000 - millisUntilFinished

                val progress = ((currentTime * maxProgress) / 4000).toInt()
                progressBar.progress = progress
            }

            override fun onFinish() {
                currentTime = 4000
                progressBar.progress = maxProgress
            }
        }
        timer.start()
    }

    private fun resetRunnable() {
        handlerSelectSongHorizontal.removeCallbacks(runnable)
        handlerSelectSongHorizontal.postDelayed(runnable, 0)
    }

    private val runnable: Runnable = object : Runnable {
        override fun run() {
            if (contador < listEfectsDisplay.size) {
                imgDisplay.setImageURI(listEfectsDisplay[contador].rutaCommandImg.toUri())
                contador++
            } else {
                contador = 0
            }
            handlerSelectSongHorizontal.postDelayed(this, 1200)
        }
    }

    private fun focusOnSong() {
        val focusedIndex = songCarouselSongs.getFocusedIndex()
        if (focusedIndex == -1) return
        val song = AppResources.listSongsChannelKsf[focusedIndex]
        oldValue = focusedIndex
        if (mediaPlayerSelectSongHorizontal.isPlaying) {
            mediaPlayerSelectSongHorizontal.stop()
        }

        timer?.cancel()

        isTimerRunning = false
        if (isFileExists(File(song.rutaPreview))) {
            previewTextureView.visibility = TextureView.VISIBLE
            previewMediaPlayer.apply {
                reset()
                setDataSource(song.rutaPreview)
                isLooping = true
                setVolume(0f, 0f)
                setOnPreparedListener {
                    start()
                }
                prepareAsync()
            }
            imgPreview.setImageDrawable(null)
        } else {
            previewTextureView.visibility = TextureView.INVISIBLE
            if (previewMediaPlayer.isPlaying) {
                previewMediaPlayer.pause()
            }
            setDiscImage(song.rutaDisc)
        }

        llenaLvsKsf(song.listKsf)
        //hideLvs()

        currentSong = song.title
        if(currentChannel == "06-FAVORITES") {
            val nameChannels = song.channel
            listSongScores = db.getSongScores(db.readableDatabase, nameChannels.toString(), currentSong)
        }else{
            listSongScores = db.getSongScores(db.readableDatabase, currentChannel, currentSong)
        }
        if(listSongScores.find { it.cancion == song.title } != null){
            if(song.listKsf.size != listSongScores.size){
                db.deleteCancion(song.title)
                listSongScores = arrayOf()
            }
        }
        if(listSongScores.isEmpty()){
            for (nivel in song.listKsf) {
                db.insertNivel(
                    canal = currentChannel,
                    cancion = currentSong,
                    nivel = nivel.level,
                    puntaje = "0",
                    grade = "",
                    type = if(nivel.typeSteps == "") "NORMAL" else nivel.typeSteps,
                    player = if(nivel.typePlayer == "") "A" else nivel.typePlayer
                )
            }
            listSongScores = db.getSongScores(db.readableDatabase, currentChannel, currentSong)
        }

        val rankingItem = listGlobalRanking.find { it.cancion == song.title }
        if(rankingItem != null && !isOffline){
            niveles = rankingItem.niveles
            isOficialSong = true
            if(song.isFavorite){
                //imgFavorite.setImageBitmap(bitFavoriteListed)
            }else{
                //imgFavorite.setImageBitmap(bitFavorite)
            }
        }else if (song.isFavorite) {
            niveles = ArrayList(List(listSongScores.size) { Nivel() })
            isOficialSong = true
        }else{
            niveles = ArrayList(List(listSongScores.size) { Nivel() })
            isOficialSong = false
        }

        lbNameSong.text = song.title
        lbArtist.text = song.artist
        lbBpm.text = "BPM: ${song.displayBpm}"
        lbNameSong.startAnimation(AppResources.animNameSong)
        startSongPlayback(song.rutaSong)
        recyclerLvs.adapter?.notifyDataSetChanged()
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

    private fun llenaLvsKsf(listLvs: MutableList<Ksf>) {
        if (recyclerLvs.layoutManager == null) {
            recyclerLvs.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        }
        recyclerLvs.setPadding(0,0,0,0)
        recyclerLvs.clipToPadding = false
        recyclerLvs.adapter = LvsAdapter(listLvs, sizeLvs)
        recyclerLvs.onFlingListener = null
    }

    private fun llenaLvsVacios(listLvsKsf: MutableList<Ksf> = mutableListOf<Ksf>()){
        recyclerLvsEmpty.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = LvsAdapter(listLvsKsf, (sizeLvs))
            setPadding(0,0,0,0)
            clipToPadding = false
        }
    }

    private fun moveSongs(arrow: ImageView){
        arrow.startAnimation(AppResources.animPressNav)
        resetIndicatorPosition()
        focusOnSong()
        soundPoolSelectSongKsf.play(selectSong_movKsf, 0.5f, 0.5f, 1, 0, 1.0f)
    }

    private fun moveLvs(){
        val lv = AppResources.listSongsChannelKsf[oldValue].listKsf[positionActualLvs]
        imgLvSelected.setImageBitmap(if(lv.typePlayer == "A") difficultedSelected else difficultedSelectedHD)

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
        playerSong.player = lv.typePlayer
        playerSong.type = lv.typeSteps
        playerSong.stepMaker = lv.stepmaker
    }

    private fun hideLvs() {
        indicatorLayout.visibility = View.INVISIBLE
        if(levelsShowing){
            animateLvAndRankingDown()
        }
    }

    private fun showLvs() {
        soundPoolSelectSongKsf.play(selectKsf, 1.0f, 1.0f, 1, 0, 1.0f)
        indicatorLayout.visibility = View.VISIBLE
        linearLvAndRanking.visibility = View.VISIBLE
        if(!levelsShowing){
            animateLvAndRankingUp()
        }
        moveLvs()

    }

    private fun playMedia(path: String) {
        mediaPlayerSelectSongHorizontal.release()
        mediaPlayerSelectSongHorizontal = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            setDataSource(path)
            prepare()
            seekTo(startTimeMs)
        }
    }

    private fun updateRecycler() {
        val lm = recyclerLvs.layoutManager as LinearLayoutManager
        recyclerLvs.post {
            lm.scrollToPositionWithOffset(firstVisible, 0)
        }
        val indicatorPosition = selectedIndex - firstVisible
        indicatorLayout.x = (indicatorPosition * sizeLvs).toFloat()
    }

    private fun resetIndicatorPosition() {
        selectedIndex = 0
        firstVisible = 0
        positionActualLvs = 0
        val lm = recyclerLvs.layoutManager as LinearLayoutManager
        lm.scrollToPositionWithOffset(0, 0)
        indicatorLayout.x = 0f
    }

    fun showCarouselSongs() {
        songCarouselSongs.showCarousel()
        imgCursor.clearAnimation()
        imgCursor.visibility = View.VISIBLE
        imgCursor.startAnimation(animSelect)
    }

    fun hideCarouselSongs() {
        songCarouselSongs.hideCarousel()
        imgCursor.clearAnimation()
        imgCursor.visibility = View.GONE
    }

    private fun startSongPlayback(path: String) {
        playMedia(path)
        timer?.cancel()
        isTimerRunning = false
        timer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {}
            override fun onFinish() {
                try {
                    if (mediaPlayerSelectSongHorizontal.isPlaying) {
                        mediaPlayerSelectSongHorizontal.stop()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                isTimerRunning = false
            }
        }
        timer?.start()
        isTimerRunning = true
        mediaPlayerSelectSongHorizontal.start()
    }

    private fun isFileExists(file: File): Boolean {
        return file.exists() && !file.isDirectory
    }

    class SpriteAnimator(imageView: ImageView, sprite: Bitmap) {
        private val frames: List<Bitmap>
        private var frame = 0
        init {
            val frameWidth = sprite.width / 2
            val frameHeight = sprite.height / 2
            val tempFrames = mutableListOf<Bitmap>()
            for (r in 0 until 2) {
                for (c in 0 until 2) {
                    val rawFrame = Bitmap.createBitmap(
                        sprite,
                        c * frameWidth,
                        r * frameHeight,
                        frameWidth,
                        frameHeight
                    )
                    val trimmed = trimTransparentEdges(rawFrame)
                    tempFrames.add(trimmed)
                }
            }
            frames = tempFrames
        }

        private val runnable = object : Runnable {
            override fun run() {
                imageView.setImageBitmap(frames[frame])
                frame = (frame + 1) % frames.size
                handlerSelectSongHorizontal.postDelayed(this, 200)
            }
        }

        fun start() {
            handlerSelectSongHorizontal.post(runnable)
        }

        fun stop() {
            handlerSelectSongHorizontal.removeCallbacks(runnable)
        }
    }

    private fun goSelectChannelHorizontal() {
        handlerSelectSongHorizontal.removeCallbacksAndMessages(null)
        if (mediaPlayerSelectSongHorizontal.isPlaying){
            mediaPlayerSelectSongHorizontal.pause()
            mediaPlayerSelectSongHorizontal.stop()
            mediaPlayerSelectSongHorizontal.release()
            if(previewMediaPlayer.isPlaying){
                previewMediaPlayer.pause()
                previewMediaPlayer.stop()
                previewMediaPlayer.release()
            }
        }

        this.finish()
        overridePendingTransition(0,R.anim.anim_command_window_off)
    }

    private val scaledMainGrades = mutableMapOf<String, Bitmap>()
    private val scaledExtraGrades = mutableMapOf<String, Bitmap>()
    private val gradeCombinationCache = mutableMapOf<String, Bitmap>()

    private fun getBitMapGrade(positionActualLvs: Int): Bitmap {

        val gradeFull = listSongScores[positionActualLvs].grade
        if (gradeFull.isEmpty()) return emptyBitmap

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

    private fun getWorldBitMapGrade(positionActualLvs: Int): Bitmap {

        val firstRankList = niveles[positionActualLvs].fisrtRank
        if (firstRankList.isEmpty()) return emptyBitmap

        val gradeFull = firstRankList[0].grade
        if (gradeFull.isEmpty()) return emptyBitmap

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

    private var levelsShowing = false
    private fun animateLvAndRankingUp() {
        levelsShowing = true
        val screenHeight = resources.displayMetrics.heightPixels

        // Duración de la animación en milisegundos
        val animationDuration = 500L

        // ===== Animación para linearLvAndRanking =====
        // Comienza desde translationY = 0 (posición normal) hasta screenHeight * 0.90
        val linearLvAnimatorY = ObjectAnimator.ofFloat(
            linearLvAndRanking,
            "translationY",
            0f,
            -(screenHeight * 0.10).toFloat()  // Sube 10% hacia arriba
        )
        linearLvAnimatorY.duration = animationDuration
        linearLvAnimatorY.interpolator = android.view.animation.AccelerateDecelerateInterpolator()

        // ===== Animación de escala para linearLvAndRanking (5% de crecimiento) =====
        val linearLvScaleX = ObjectAnimator.ofFloat(linearLvAndRanking, "scaleX", 1f, 1.05f)
        val linearLvScaleY = ObjectAnimator.ofFloat(linearLvAndRanking, "scaleY", 1f, 1.05f)
        linearLvScaleX.duration = animationDuration
        linearLvScaleY.duration = animationDuration
        linearLvScaleX.interpolator = android.view.animation.AccelerateDecelerateInterpolator()
        linearLvScaleY.interpolator = android.view.animation.AccelerateDecelerateInterpolator()

        // ===== Animación para constraintLvs =====
        // Comienza desde translationY = 0 (posición normal) hasta screenHeight * 0.80
        val constraintLvsAnimatorY = ObjectAnimator.ofFloat(
            constraintLvs,
            "translationY",
            0f,
            -(screenHeight * 0.25).toFloat()  // Sube 20% hacia arriba
        )
        constraintLvsAnimatorY.duration = animationDuration
        constraintLvsAnimatorY.interpolator = android.view.animation.AccelerateDecelerateInterpolator()

        // ===== Animación de escala para constraintLvs y todos sus hijos (5% de crecimiento) =====
        val constraintLvsScaleX = ObjectAnimator.ofFloat(constraintLvs, "scaleX", 1f, 1.05f)
        val constraintLvsScaleY = ObjectAnimator.ofFloat(constraintLvs, "scaleY", 1f, 1.05f)
        constraintLvsScaleX.duration = animationDuration
        constraintLvsScaleY.duration = animationDuration
        constraintLvsScaleX.interpolator = android.view.animation.AccelerateDecelerateInterpolator()
        constraintLvsScaleY.interpolator = android.view.animation.AccelerateDecelerateInterpolator()

        // ===== Ejecutar todas las animaciones juntas =====
        val animatorSet = android.animation.AnimatorSet()
        animatorSet.playTogether(
            linearLvAnimatorY,
            linearLvScaleX,
            linearLvScaleY,
            constraintLvsAnimatorY,
            constraintLvsScaleX,
            constraintLvsScaleY
        )
        animatorSet.start()
    }

    private fun animateLvAndRankingDown() {
        levelsShowing = false
        val screenHeight = resources.displayMetrics.heightPixels

        // Duración de la animación en milisegundos
        val animationDuration = 500L

        // ===== Animación para linearLvAndRanking =====
        // Baja desde translationY actual hasta screenHeight * 0.10 hacia abajo
        val linearLvAnimatorY = ObjectAnimator.ofFloat(
            linearLvAndRanking,
            "translationY",
            linearLvAndRanking.translationY,
            (screenHeight * 0.10).toFloat()  // Baja 10% hacia abajo
        )
        linearLvAnimatorY.duration = animationDuration
        linearLvAnimatorY.interpolator = android.view.animation.AccelerateDecelerateInterpolator()

        // ===== Animación de escala para linearLvAndRanking (5% de reducción) =====
        val linearLvScaleX = ObjectAnimator.ofFloat(linearLvAndRanking, "scaleX", 1.05f, 1f)
        val linearLvScaleY = ObjectAnimator.ofFloat(linearLvAndRanking, "scaleY", 1.05f, 1f)
        linearLvScaleX.duration = animationDuration
        linearLvScaleY.duration = animationDuration
        linearLvScaleX.interpolator = android.view.animation.AccelerateDecelerateInterpolator()
        linearLvScaleY.interpolator = android.view.animation.AccelerateDecelerateInterpolator()

        // ===== Animación para constraintLvs =====
        // Baja desde translationY actual hasta screenHeight * 0.20 hacia abajo
        val constraintLvsAnimatorY = ObjectAnimator.ofFloat(
            constraintLvs,
            "translationY",
            constraintLvs.translationY,
            (screenHeight * 0.20).toFloat()  // Baja 20% hacia abajo
        )
        constraintLvsAnimatorY.duration = animationDuration
        constraintLvsAnimatorY.interpolator = android.view.animation.AccelerateDecelerateInterpolator()

        // ===== Animación de escala para constraintLvs y todos sus hijos (5% de reducción) =====
        val constraintLvsScaleX = ObjectAnimator.ofFloat(constraintLvs, "scaleX", 1.05f, 1f)
        val constraintLvsScaleY = ObjectAnimator.ofFloat(constraintLvs, "scaleY", 1.05f, 1f)
        constraintLvsScaleX.duration = animationDuration
        constraintLvsScaleY.duration = animationDuration
        constraintLvsScaleX.interpolator = android.view.animation.AccelerateDecelerateInterpolator()
        constraintLvsScaleY.interpolator = android.view.animation.AccelerateDecelerateInterpolator()

        // ===== Ejecutar todas las animaciones juntas =====
        val animatorSet = android.animation.AnimatorSet()
        animatorSet.playTogether(
            linearLvAnimatorY,
            linearLvScaleX,
            linearLvScaleY,
            constraintLvsAnimatorY,
            constraintLvsScaleX,
            constraintLvsScaleY
        )

        // Ocultar después de que termine la animación
        animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                linearLvAndRanking.visibility = View.GONE
                // Resetear translationY para la siguiente vez
                linearLvAndRanking.translationY = 0f
                constraintLvs.translationY = 0f
            }
        })

        animatorSet.start()
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

    private fun setDiscImage(path: String) {
        val cached = imageCache[path]
        if (cached != null) {
            imgPreview.setImageBitmap(cached)
            return
        }

        imgPreview.post {
            val original = BitmapFactory.decodeFile(path) ?: return@post
            val trimmed = trimTransparentEdges(original)

            imageCache[path] = trimmed
            imgPreview.setImageBitmap(trimmed)
        }
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
                handlerSelectSongHorizontal.postDelayed({
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

    override fun onResume() {
        super.onResume()
        choreographer.postFrameCallback(frameCallback)

        resetRunnable()
        if(listEfectsDisplay.isNotEmpty()) {
            handlerSelectSongHorizontal.postDelayed(runnable, 1200)
        }

        handlerSelectSongHorizontal.postDelayed({
            if (!mediaPlayerSelectSongHorizontal.isPlaying) {
                mediaPlayerSelectSongHorizontal.start()
                if (::previewMediaPlayer.isInitialized) {
                    previewMediaPlayer.start()
                }
            }
        }, 500)
    }

    override fun onPause() {
        super.onPause()
        choreographer.removeFrameCallback(frameCallback)

        handlerSelectSongHorizontal.removeCallbacks(runnable)
        //handlerSelectSongHorizontal.removeCallbacks(runnableContador)

    }
}

object BannerCacheSelectSong {
    private val cache = LruCache<String, Bitmap>(20)
    fun get(path: String): Bitmap? {
        return cache.get(path)
    }
    fun load(path: String): Bitmap? {
        val cached = cache.get(path)
        if (cached != null) return cached
        val file = File(path)
        if (!file.exists()) return null
        val bitmap = BitmapFactory.decodeFile(path) ?: return null
        val trimmed = trimTransparentEdges(bitmap)
        if (trimmed !== bitmap) {
            bitmap.recycle()
        }
        cache.put(path, trimmed)
        return trimmed
    }
}