package com.fingerdance

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.LruCache
import android.view.animation.Animation
import java.io.File
import java.io.FileInputStream

object AppResources {

    var arrayGrades: ArrayList<Bitmap>? = null
    var arrGradesDesc: ArrayList<Bitmap>? = null
    var arrGradesDescAbrev: ArrayList<Bitmap>? = null

    var logoTheme: Bitmap? = null
    var indicatorBitmap: Bitmap? = null

    lateinit var arrowNavIzq: Bitmap
    lateinit var arrowNavDer: Bitmap
    lateinit var arrowBackIzq: Bitmap
    lateinit var arrowBackDer: Bitmap
    lateinit var arrowBackIzqColor: Bitmap
    lateinit var arrowBackDerColor: Bitmap

    lateinit var bmCircle: Bitmap
    lateinit var bmFloor: Bitmap
    lateinit var bmFloor2: Bitmap
    lateinit var bmAceptar: Bitmap

    lateinit var bmBestScore : Bitmap
    lateinit var difficultedSelected: Bitmap
    lateinit var difficultedSelectedHD : Bitmap

    lateinit var bmCommandEmpty : Bitmap

    var soundSelectChannel: MediaPlayer? = null
    var soundPool: SoundPool? = null

    var channelMov = 0
    var channelBack = 0
    var upSound = 0
    var pressStart = 0

    var backgroundDrawable: Drawable? = null
    var hasBackgroundVideo = false
    var isLoaded = false

    var listSongsChannelKsf: ArrayList<SongKsf> = ArrayList()
    var frameDisc: Bitmap? = null

    lateinit var animPressNav: Animation
    var animNameSong: Animation? = null

    lateinit var bmSelected: Bitmap
    lateinit var bmCursor: Bitmap
    lateinit var bmIndicator: Bitmap
    lateinit var backgroundHorizontal: Bitmap

    lateinit var bitmapNumber : Bitmap
    lateinit var bitmapNumberMiss : Bitmap

    lateinit var numberBitmaps: List<Bitmap>
    lateinit var numberBitmapsMiss: List<Bitmap>

    fun load() {
        if (isLoaded) return
        val themePath = "$rutaBase/FingerDance/Themes/$tema"

        // ---------- BITMAPS ----------
        logoTheme = BitmapFactory.decodeFile("$themePath/logo_theme.png")
        indicatorBitmap = BitmapFactory.decodeFile("$themePath/GraphicsStatics/indicator.png")
        arrowNavIzq = BitmapFactory.decodeFile("$themePath/ArrowsNav/ArrowNavIzq.png")
        arrowNavDer = BitmapFactory.decodeFile("$themePath/ArrowsNav/ArrowNavDer.png")
        arrowBackIzq = BitmapFactory.decodeFile("$themePath/ArrowsNav/ArrowBackIzq.png")
        arrowBackDer = BitmapFactory.decodeFile("$themePath/ArrowsNav/ArrowBackDer.png")
        arrowBackIzqColor = BitmapFactory.decodeFile("$themePath/ArrowsNav/ArrowBackIzqColor.png")
        arrowBackDerColor = BitmapFactory.decodeFile("$themePath/ArrowsNav/ArrowBackDerColor.png")

        frameDisc = BitmapFactory.decodeFile("$rutaBase/FingerDance/Themes/frame_disc.png")
        backgroundHorizontal = BitmapFactory.decodeFile("$rutaBase/FingerDance/Themes/background.png")

        bmCircle = BitmapFactory.decodeFile("$themePath/GraphicsStatics/preview_circle.png")
        bmFloor = BitmapFactory.decodeFile("$themePath/GraphicsStatics/floor.png")
        bmFloor2 = BitmapFactory.decodeFile("$themePath/GraphicsStatics/floor2.png")
        bmAceptar = BitmapFactory.decodeFile("$themePath/GraphicsStatics/press_floor.png")

        bmSelected = BitmapFactory.decodeFile("$themePath/GraphicsStatics/imgSelect.png")
        bmCursor = BitmapFactory.decodeFile("$themePath/GraphicsStatics/CURSOR1.png")
        bmIndicator = BitmapFactory.decodeFile("$themePath/GraphicsStatics/indicator_lv.png")

        bmBestScore  = BitmapFactory.decodeFile("$themePath/GraphicsStatics/score_body_select_song.png")
        difficultedSelected = BitmapFactory.decodeFile("$themePath/GraphicsStatics/lv_active.png")
        difficultedSelectedHD = BitmapFactory.decodeFile("$themePath/GraphicsStatics/lv_active_hd.png")

        bmCommandEmpty = BitmapFactory.decodeFile("$themePath/GraphicsStatics/command_window/Command_Effect.png")

        bitmapNumber = BitmapFactory.decodeFile("$themePath/GraphicsStatics/game_play/numbersCombo.png")
        bitmapNumberMiss = BitmapFactory.decodeFile("$themePath/GraphicsStatics/game_play/numbersComboMiss.png")

        numberBitmaps = ArrayList<Bitmap>().apply {
            val frameWidth = bitmapNumber.width / 10
            for (a in 0 until 10) {
                add(Bitmap.createBitmap(bitmapNumber, a * frameWidth, 0, frameWidth, bitmapNumber.height))
            }
        }

        numberBitmapsMiss = ArrayList<Bitmap>().apply {
            val frameWidth = bitmapNumberMiss.width / 10
            for (a in 0 until 10) {
                add(Bitmap.createBitmap(bitmapNumberMiss, a * frameWidth, 0, frameWidth, bitmapNumberMiss.height))
            }
        }

        // ---------- AUDIO ----------
        soundSelectChannel = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )

            setDataSource("$themePath/Sounds/channel_song.mp3")
            prepare()
            isLooping = true
        }

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(AudioAttributes.USAGE_GAME)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(audioAttributes)
            .build()

        val sp = soundPool!!

        fun loadSound(path: String): Int {
            val file = File(path)
            val fd = FileInputStream(file).fd
            return sp.load(fd, 0, file.length(), 1)
        }

        channelMov = loadSound("$themePath/Sounds/sound_navegation.mp3")
        channelBack = loadSound("$themePath/Sounds/exit_select_channel.ogg")
        upSound = loadSound("$themePath/Sounds/up_sound.ogg")
        pressStart = loadSound("$themePath/Sounds/start.ogg")

        // ---------- BACKGROUND ----------
        val bgaFile = File(bgaPathSelectChannel)

        hasBackgroundVideo = bgaFile.exists() && !bgaFile.isDirectory

        if (!hasBackgroundVideo) {
            backgroundDrawable = Drawable.createFromPath("$themePath/GraphicsStatics/bg_select_channel.png")
        }

        isLoaded = true
    }
}

object SpriteCache {

    private val cache = HashMap<Int, List<Bitmap>>()

    fun getFrames(sprite: Bitmap): List<Bitmap> {

        val key = sprite.hashCode()

        val cached = cache[key]
        if (cached != null) return cached

        val frameWidth = sprite.width / 2
        val frameHeight = sprite.height / 2

        val frames = mutableListOf<Bitmap>()

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

                frames.add(trimmed)
            }
        }

        cache[key] = frames

        return frames
    }
}

object BannerCache {

    private val cache = LruCache<String, Bitmap>(20)

    fun get(path: String): Bitmap? {
        return cache.get(path)
    }

    fun load(path: String): Bitmap? {

        val cached = cache.get(path)
        if (cached != null) return cached

        val file = File(path)
        if (!file.exists()) return null

        val bitmap = BitmapFactory.decodeFile(path)

        if (bitmap != null) {
            cache.put(path, bitmap)
        }

        return bitmap
    }
}