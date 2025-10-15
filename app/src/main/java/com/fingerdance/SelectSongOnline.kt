package com.fingerdance

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.TranslateAnimation
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.isVisible
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

private lateinit var linearBG: LinearLayout
private lateinit var buttonLayout: LinearLayout
private lateinit var constraintMain: ConstraintLayout
private lateinit var lbNameSong: TextView
private lateinit var lbArtist: TextView
private lateinit var lbLvActive: TextView

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
private lateinit var video_fondo : VideoView
private lateinit var imgPrev: ImageView
private lateinit var indicatorLayout: ImageView
private lateinit var imageCircle : ImageView

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

private val handler = Handler(Looper.getMainLooper())

private lateinit var imgContador: ImageView

private val startTimeMs = 30000
private var timer: CountDownTimer? = null
private var isTimerRunning = false

private var ready = 0

private lateinit var overlayBG: View
private lateinit var btnAddPreview: Button
private lateinit var btnAddBga: Button
private var currentPathSong: String = ""

private lateinit var difficultySelected : Bitmap
private lateinit var difficultySelectedHD : Bitmap

class SelectSongOnline : AppCompatActivity() {
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
            setContentView(R.layout.activity_select_song_online)
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            onWindowFocusChanged(true)

            recyclerView = findViewById(R.id.recyclerViewSelectSongOnline) //binding.recyclerView

            recyclerLvs = findViewById(R.id.recyclerLvsOnline) //binding.recyclerLvs
            recyclerLvsVacios = findViewById(R.id.recyclerNoLvsOnline) //binding.recyclerNoLvs

            val txPlayer1 = findViewById<TextView>(R.id.txPlayer1SelectSongOnline)
            val txPlayer2 = findViewById<TextView>(R.id.txPlayer2SelectSongOnline)

            if(isPlayer1){
                txPlayer1.text = "Player 1 \n $userName"
                txPlayer2.text = "Player 2 \n ${activeSala.jugador2.id}"
            }else{
                txPlayer1.text = "Player 1 \n ${activeSala.jugador1.id}"
                txPlayer2.text = "Player 2 \n $userName"
            }

            recyclerCommands = findViewById(R.id.recyclerCommands)
            recyclerCommands.isUserInputEnabled = false
            recyclerCommandsValues = findViewById(R.id.recyclerValues)
            recyclerCommandsValues.isUserInputEnabled = false

            mediaPlayer = MediaPlayer()

            val levels = Levels()

            playerSong = PlayerSong("","", "",0.0,0.0, 0.0, "","",false,
                                     false,"", "", levels, "")

            constraintMain = findViewById(R.id.constraintMain)
            linearBG = findViewById(R.id.linearBG)
            linearBG.background = Drawable.createFromPath(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/bg_select_song.png")!!.absolutePath)

            imgPrev = findViewById(R.id.imgPrev)
            imgPrev.layoutParams.height = (width * 0.75).toInt()

            animPressNav = AnimationUtils.loadAnimation(this, R.anim.press_nav)
            animNameSong = AnimationUtils.loadAnimation(this, R.anim.anim_name_song)
            animOn = AnimationUtils.loadAnimation(this, R.anim.anim_command_window_on)
            animOff = AnimationUtils.loadAnimation(this, R.anim.anim_command_window_off)

            commandWindow = findViewById(R.id.command_window)
            commandWindowBG = findViewById(R.id.command_window_bg)
            commandWindowBG.foreground = Drawable.createFromPath(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/command_window/Command_Frame.png")!!.absolutePath)
            mediPlayer = MediaPlayer()
            mediaPlayerVideo = MediaPlayer()

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
            lbLvActive = findViewById(R.id.lbLvActive)
            lbLvActive.isVisible = false

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
            //val selected = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/lv_active.png")!!.absolutePath)
            difficultySelected = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/lv_active.png")!!.absolutePath)
            difficultySelectedHD = BitmapFactory.decodeFile(getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/lv_active_hd.png")!!.absolutePath)

            //imgLvSelected.setImageBitmap(selected)
            imgLvSelected.isVisible = false

            //val rutaGrades = getExternalFilesDir("/FingerDance/Themes/$tema/GraphicsStatics/dance_grade/").toString()
            //arrayBestGrades = getGrades(rutaGrades)

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

            imgSelected.layoutParams.height = width / 3
            imgSelected.layoutParams.width = width / 3
            val anim = AnimationUtils.loadAnimation(this, R.anim.anim_select);
            imgSelected.startAnimation(anim)

            nav_izq = findViewById(R.id.nav_izq_song)
            nav_der = findViewById(R.id.nav_der_song)
            nav_back_Izq = findViewById(R.id.back_izq)
            nav_back_der = findViewById(R.id.back_der)

            video_fondo = findViewById(R.id.videoPreview)

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
            llenaLvsVacios(null, listVacios)

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
            isFocus(middle)
            val smoothScroller: RecyclerView.SmoothScroller = CenterSmoothScroller(recyclerView.context)
            smoothScroller.targetPosition = middle
            recyclerView.layoutManager?.startSmoothScroll(smoothScroller)
            recyclerView.setOnTouchListener { _, _ -> true }

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

            var textSize = width / 10
            lbLvActive.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())

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
            //lbBpm.layoutParams.width = (width * 0.2).toInt()

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
                imgFloor.setImageBitmap(bmFloor)
                if (recyclerView.isVisible && !commandWindow.isVisible) {
                    Toast.makeText(this, "Manten presionado para volver al Selecet Channel", Toast.LENGTH_SHORT).show()
                    soundPoolSelectSongKsf.play(selectSong_movKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                }
                if (imgLvSelected.isVisible && !commandWindow.isVisible) {
                    soundPoolSelectSongKsf.play(up_SelectSoundKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                    hideSelectLv(anim)
                }
                if (commandWindow.isVisible && !linearValues.isVisible) {
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
                if (imgLvSelected.isVisible && !commandWindow.isVisible) {
                    soundPoolSelectSongKsf.play(up_SelectSoundKsf, 1.0f, 1.0f, 1, 0, 1.0f)
                    hideSelectLv(anim)
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
                    moverCanciones(nav_der, animPressNav, oldValue)
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

            //val tipsArray = resources.getStringArray(R.array.tips_array)
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

                        //selectionSongOnline = false
                        if(isPlayer1){
                            activeSala.jugador1.listo = true
                        }else{
                            activeSala.jugador2.listo = true
                        }

                        activeSala.cancion = CancionOnline(
                            rutaKsf = listItemsKsf[oldValue].listKsf[positionActualLvs].rutaKsf,
                            rutaCancion = listItemsKsf[oldValue].rutaSong,
                            rutaBanner = listItemsKsf[oldValue].rutaDisc,
                            rutaBGA = listItemsKsf[oldValue].rutaBGA,
                            rutaDisc = listItemsKsf[oldValue].rutaDisc,
                            rutaPreview = listItemsKsf[oldValue].rutaPreview,
                            nameSong = listItemsKsf[oldValue].title,
                            artists = listItemsKsf[oldValue].artist,
                            bpm = displayBPM.toString(),
                            nivel = lbLvActive.text.toString()
                        )
                        readyPlay = true
                        salaRef.setValue(activeSala)
                        txTip.text = "Espera por favor."

                        playerSong.speed = txVelocidadActual.text.toString()
                        if (playerSong.rutaNoteSkin != "") {
                            ruta = playerSong.rutaNoteSkin!!
                        } else {
                            val directorioBase = getExternalFilesDir("/FingerDance/NoteSkins")!!.absolutePath
                            val directorios = File(directorioBase).listFiles { file ->
                                file.isDirectory && file.name.contains(
                                    "default",
                                    ignoreCase = true
                                )
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

                        mediaPlayer = MediaPlayer().apply {
                            setAudioAttributes(
                                AudioAttributes.Builder()
                                    .setUsage(AudioAttributes.USAGE_GAME)
                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                    .build()
                            )
                            setDataSource(playerSong.rutaCancion!!)
                            prepare()
                        }
                        val isHalfDouble = listItemsKsf[oldValue].listKsf[positionActualLvs].typePlayer == "B"
                        load(playerSong.rutaKsf!!, isHalfDouble)
                        //readyPlay = true

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
                                }else{
                                    txCurrentBpm.text = formattedResult //result.toString() + "X"
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
                                "FD" -> playerSong.fd = false
                                "V" -> playerSong.vanish = false
                                "AP" -> playerSong.ap = false
                            }
                        } else {
                            when (itemValues.value) {
                                "BGAOFF" -> {
                                    listEfectsDisplay.add(itemValues)
                                    playerSong.isBGAOff = true
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
                                }
                                "AP" -> {
                                    listEfectsDisplay.add(itemValues)
                                    playerSong.ap = true
                                    val isVanish = listEfectsDisplay.find { it.value == "V" }
                                    if (isVanish != null) {
                                        listEfectsDisplay.remove(isVanish)
                                        playerSong.vanish = false
                                    }
                                }
                            }
                            imgDisplay.setImageBitmap(BitmapFactory.decodeFile(itemValues.rutaCommandImg))
                        }
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
                    if(itemCommand.value.contains("Offset", ignoreCase = true) || itemCommand.value.contains("Speed", ignoreCase = true)){
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
        }

    private fun showOverlay(isBGA: Boolean) {
        overlayBG = View(this).apply {
            setBackgroundColor(0xAA000000.toInt()) // Oscurece la pantalla
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
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
        val destinationFile = File(destinationPath, fileName)
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(destinationFile)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            Toast.makeText(this, "Espere por favor...", Toast.LENGTH_SHORT).show()
            handler.postDelayed({
                if(!isBGA){
                    Toast.makeText(this, "Se guardo el preview correctamente", Toast.LENGTH_SHORT).show()
                }else{
                    Toast.makeText(this, "Se guardo el BGA correctamente", Toast.LENGTH_SHORT).show()
                }
                constraintMain.removeView(overlayBG)
                constraintMain.removeView(buttonLayout)
            }, 1500L)

        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al guardar el archivo", Toast.LENGTH_SHORT).show()
        }
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
        lbLvActive.isVisible = true
        imgLvSelected.startAnimation(animOn)
        lbLvActive.startAnimation(animOn)

        moverLvs(positionActualLvs)
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
            oldValueCommand = 0
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
        lbLvActive.startAnimation(animOff)

        imgLvSelected.isVisible = false
        lbLvActive.isVisible = false

    }

    private fun moverCanciones(flecha : ImageView, animation: Animation?, oldValue: Int) {
        soundPoolSelectSongKsf.play(selectSong_movKsf, 1.0f, 1.0f, 1, 0, 1.0f)
        flecha.startAnimation(animation)
        recyclerView.scrollToPosition(oldValue)
        isFocus(oldValue)
        val smoothScroller: RecyclerView.SmoothScroller = CenterSmoothScroller(recyclerView.context)
        smoothScroller.targetPosition = oldValue
        recyclerView.layoutManager?.startSmoothScroll(smoothScroller)
        positionActualLvs = 0
        lbArtist.isSelected = true
        lbNameSong.isSelected = true
    }

    private fun moverLvs(positionActualLvs: Int) {
        val lv = listItemsKsf[oldValue].listKsf[positionActualLvs]
        imgLvSelected.setImageBitmap(if(lv.typePlayer == "A") difficultySelected else difficultySelectedHD)
        lbLvActive.text = lv.level
        currentLevel = lv.level
        playerSong.level = lv.level
        playerSong.stepMaker = lv.stepmaker

        val layoutManager = recyclerLvs.layoutManager as LinearLayoutManager

        recyclerLvs.post {
            layoutManager.scrollToPositionWithOffset(positionActualLvs, 0)
            recyclerLvs.post {
                moveIndicatorToPosition(positionActualLvs) // Asegura que el indicador se mueva después del desplazamiento
            }
        }
    }

    private fun isFocus (position: Int){
        val item = listItemsKsf[position]
        currentPathSong = item.rutaSong
        var isVideo = true
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

        //if(isFileExists(File(item.rutaPrevVideo))){
        if(isFileExists(File(item.rutaPreview))){
            if(item.rutaPreview.endsWith(".png", ignoreCase = true)
            || item.rutaPreview.endsWith(".jpg", ignoreCase = true)
            || item.rutaPreview.endsWith(".bpm", ignoreCase = true)
            || item.rutaPreview.endsWith(".mpg")
            || item.rutaPreview == "") {
                isVideo = false
            }
            if(isVideo){
                video_fondo.setVideoPath(item.rutaPreview)
                video_fondo.setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.setVolume(0f, 0f)
                }
                video_fondo.visibility = View.VISIBLE
                imgPrev.visibility = View.GONE

                //val retriever = MediaMetadataRetriever()
                //retriever.setDataSource(item.rutaPreview)
                video_fondo.start()
                //val hasAudio = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO)
                //if(hasAudio != null ){
                    //video_fondo.setOnPreparedListener { mp -> mp.setVolume(0.0f, 0.0f) }
                //}
                video_fondo.setOnCompletionListener {
                    video_fondo.start()
                }
                if (mediPlayer.isPlaying){
                    mediPlayer.release()
                    mediPlayer = MediaPlayer.create(this, Uri.fromFile(File(item.rutaSong)))
                    mediPlayer.seekTo(startTimeMs)
                    mediPlayer.start()
                    timer?.start()
                    isTimerRunning = true
                }else{
                    mediPlayer = MediaPlayer.create(this, Uri.fromFile(File(item.rutaPreview)))
                    mediPlayer.seekTo(startTimeMs)
                    mediPlayer.start()
                    timer?.start()
                    isTimerRunning = true
                }
            }else{
                val img = BitmapFactory.decodeFile(item.rutaDisc)
                imgPrev.setImageBitmap(img)
                video_fondo.visibility = View.GONE
                imgPrev.visibility = View.VISIBLE
                if (mediPlayer.isPlaying){
                    mediPlayer.release()
                    mediPlayer = MediaPlayer.create(this, Uri.fromFile(File(item.rutaSong)))
                    mediPlayer.seekTo(startTimeMs)
                    mediPlayer.start()
                    timer?.start()
                    isTimerRunning = true
                }else{
                    mediPlayer = MediaPlayer.create(this, Uri.fromFile(File(item.rutaSong)))
                    mediPlayer.seekTo(startTimeMs)
                    mediPlayer.start()
                    timer?.start()
                    isTimerRunning = true
                }
            }
        }else{
            val img = BitmapFactory.decodeFile(item.rutaDisc)
            imgPrev.setImageBitmap(img)
            video_fondo.visibility = View.GONE
            imgPrev.visibility = View.VISIBLE
            if (mediPlayer.isPlaying){
                mediPlayer.release()
                mediPlayer = MediaPlayer.create(this, Uri.fromFile(File(item.rutaSong)))
                mediPlayer.seekTo(startTimeMs)
                mediPlayer.start()
                timer?.start()
                isTimerRunning = true
            }else{
                mediPlayer = MediaPlayer.create(this, Uri.fromFile(File(item.rutaSong)))
                mediPlayer.seekTo(startTimeMs)
                mediPlayer.start()
                timer?.start()
                isTimerRunning = true
            }
        }

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
        llenaLvsKsf(item.listKsf)
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
            item.value.contains("Judge", ignoreCase = true)){
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

    private fun llenaLvsKsf(listLvs : MutableList<Ksf>){
        /*binding.*/recyclerLvs.apply {
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
            adapter = LvsAdapter(null, listLvs, sizeLvs)
        }
        recyclerLvs.onFlingListener = null
    }

    private fun llenaLvsVacios(listLvs : ArrayList<Lvs>? = arrayListOf(), listLvsKsf:  ArrayList<Ksf>? = arrayListOf()){
        recyclerLvsVacios.apply {
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
        //mediPlayer.pause()
    }

    override fun onResume() {
        super.onResume()
        recyclerView.scrollToPosition(oldValue)
        isFocus(oldValue)
        val smoothScroller: RecyclerView.SmoothScroller = CenterSmoothScroller(recyclerView.context)
        smoothScroller.targetPosition = oldValue
        recyclerView.layoutManager?.startSmoothScroll(smoothScroller)

        linearLoading.isVisible = false
        imgLoading.isVisible = false
        ready = 0
        imgFloor.setImageBitmap(bmFloor)

        if(getSelectChannel){
            this.finish()
            if(mediPlayer.isPlaying){
                mediPlayer.stop()
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
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





