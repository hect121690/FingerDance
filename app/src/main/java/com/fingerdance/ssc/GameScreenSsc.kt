package com.fingerdance.ssc

import android.os.SystemClock
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.fingerdance.GameScreenActivity
import com.fingerdance.aBatch
import com.fingerdance.alphaPadB
import com.fingerdance.bBatch
import com.fingerdance.endingFadeAlpha
import com.fingerdance.height
import com.fingerdance.heightBtns
import com.fingerdance.hideImagesPadA
import com.fingerdance.isEndingFade
import com.fingerdance.loadTexture
import com.fingerdance.luaRecepts
import com.fingerdance.mediaPlayer
import com.fingerdance.medidaFlechas
import com.fingerdance.padPositions
import com.fingerdance.playerSong
import com.fingerdance.ruta
import com.fingerdance.showPadB
import com.fingerdance.showSongProgress
import com.fingerdance.skinPad
import com.fingerdance.tema
import com.fingerdance.typePadD
import com.fingerdance.width
import com.fingerdance.widthBtns
import java.io.File
import kotlin.math.abs

open class GameScreenSsc(activity: GameScreenActivity) : Screen {
    val a = activity

    private lateinit var batch: SpriteBatch
    lateinit var stage: Stage

    val rutaPads = "/FingerDance/Themes/$tema/GraphicsStatics/game_play"
    private val padLefDown = TextureRegion(Texture(Gdx.files.external("$rutaPads/left_down.png")))
    private val padLeftUp = TextureRegion(Texture(Gdx.files.external("$rutaPads/left_up.png")))
    private val padCenter = TextureRegion(Texture(Gdx.files.external("$rutaPads/center.png")))
    private val padRightUp = TextureRegion(Texture(Gdx.files.external("$rutaPads/right_up.png")))
    private val padRightDown = TextureRegion(Texture(Gdx.files.external("$rutaPads/right_down.png")))

    private val imgPerfect = TextureRegion(Texture(Gdx.files.external("$rutaPads/perfect.png")))
    private val imgGreat = TextureRegion(Texture(Gdx.files.external("$rutaPads/great.png")))
    private val imgGood = TextureRegion(Texture(Gdx.files.external("$rutaPads/good.png")))
    private val imgBad = TextureRegion(Texture(Gdx.files.external("$rutaPads/bad.png")))
    private val imgMiss = TextureRegion(Texture(Gdx.files.external("$rutaPads/miss.png")))

    val imgsJudge = arrayOf(imgPerfect, imgGreat, imgGood, imgBad, imgMiss)

    private val imgCombo = TextureRegion(Texture(Gdx.files.external("$rutaPads/combo.png")))
    private val imgComboMiss = TextureRegion(Texture(Gdx.files.external("$rutaPads/comboMiss.png")))

    val imgsTypeCombo = arrayOf(imgCombo, imgComboMiss)

    val imgNumbers = Texture(Gdx.files.external("$rutaPads/numbersCombo.png"))
    val imgNumbersMiss = Texture(Gdx.files.external("$rutaPads/numbersComboMiss.png"))

    val listNumbers = getListNumbers(imgNumbers)
    val listNumbersMiss = getListNumbers(imgNumbersMiss)

    private val backgroundTexture = Texture(Gdx.files.external("FingerDance/Themes/$tema/GraphicsStatics/game_play/barLife0.png"))
    private val barBlackTexture = Texture(Gdx.files.external("FingerDance/Themes/$tema/GraphicsStatics/game_play/barLife1.png"))
    private val barRedTexture = Texture(Gdx.files.external("FingerDance/Themes/$tema/GraphicsStatics/game_play/barLife2.png"))
    private val barLifeTexture = Texture(Gdx.files.external("FingerDance/Themes/$tema/GraphicsStatics/game_play/barLife3.png"))

    val barFrame = Sprite(backgroundTexture)
    val barBlack = Sprite(barBlackTexture)
    val barRed = Sprite(barRedTexture)
    val barColors = Sprite(barLifeTexture)

    private val barTipTexture = Texture(Gdx.files.external("FingerDance/Themes/$tema/GraphicsStatics/game_play/bar_tip.png"))
    val barTip = Sprite(barTipTexture)

    private lateinit var padB : TextureRegion
    lateinit var spritePadB: Sprite

    lateinit var padLefDownC : Array<TextureRegion>
    lateinit var padLeftUpC : Array<TextureRegion>
    lateinit var padCenterC : Array<TextureRegion>
    lateinit var padRightUpC : Array<TextureRegion>
    lateinit var padRightDownC : Array<TextureRegion>

    lateinit var arrPadsC : Array<Array<TextureRegion>>

    data class NoteCellMetrics(
        val visibleWidthRatio: Float = 1f,
        val visibleHeightRatio: Float = 1f,
        val visibleOffsetXRatio: Float = 0f,
        val visibleOffsetYRatio: Float = 0f
    ) {
        fun drawWidth(logicalSize: Float) = logicalSize / visibleWidthRatio.coerceAtLeast(0.0001f)
        fun drawHeight(logicalSize: Float) = logicalSize / visibleHeightRatio.coerceAtLeast(0.0001f)
        fun drawX(logicalX: Float, logicalSize: Float): Float {
            val width = drawWidth(logicalSize)
            return logicalX - width * visibleOffsetXRatio
        }
        fun drawY(logicalY: Float, logicalSize: Float): Float {
            val height = drawHeight(logicalSize)
            return logicalY - height * visibleOffsetYRatio
        }
    }

    val receptorMetrics = Array(5) { NoteCellMetrics() }

    private val textureLD = loadTexture(ruta, "DownLeft Ready Receptor")
    private val textureLU = loadTexture(ruta, "UpLeft Ready Receptor")
    private val textureCE = loadTexture(ruta, "Center Ready Receptor")

    lateinit var arrayPad4Bg : Array<TextureRegion>
    lateinit var arrayPad4 : Array<TextureRegion>

    val recept0Frames = getReceptsTexture(textureLD, metricsColumn = 0)
    val recept1Frames = getReceptsTexture(textureLU, metricsColumn = 1)
    val recept2Frames = getReceptsTexture(textureCE, metricsColumn = 2)
    val recept3Frames = getReceptsTexture(textureLU, true, metricsColumn = 3)
    val recept4Frames = getReceptsTexture(textureLD, true, metricsColumn = 4)

    var targetTop = 0f
    private var elapsedTime = 0f
    private var rithymAnim = 0f

    private var isPaused = false
    lateinit var camera : OrthographicCamera
    lateinit var player: PlayerSsc
    private val posYpadB = height.toFloat() - (width.toFloat() * 1.1f)

    private var timer = 0f
    private var showOverlay = false
    private var intervalOverlay = 0f

    val gdxHeight = Gdx.graphics.height
    val gdxWidth = Gdx.graphics.width
    val maxWidth = medidaFlechas * 5f
    val maxlHeight = medidaFlechas / 2f

    val gaugeIncNormal = floatArrayOf(0.03f, 0.015f, 0.01f, -0.02f, -0.1f, 0.002f)
    val gaugeIncHJ = floatArrayOf(0.015f, 0.007f, 0.005f, -0.04f, -0.15f, 0.001f)

    private val lifeLightningTexture = Texture(Gdx.files.external("FingerDance/Themes/$tema/GraphicsStatics/game_play/barlife_electric 4x6.png"))
    val lifeLightningFrames: Array<TextureRegion> = getLifeLightningFrames(lifeLightningTexture)
    private val fadeTexture = Texture(Gdx.files.internal("black.png"))

    data class PadPositionC(val x: Float, val y: Float, val widthPad: Float, val heightPad: Float)

    private val widthPad = width * 0.43f
    private val heightPad = heightBtns * 0.87f
    val padPositionsC = listOf(
        PadPositionC(width.toFloat() * 0.01f, height.toFloat() * 0.803f, widthPad, heightPad),
        PadPositionC(width.toFloat() * 0.01f, height.toFloat() * 0.53f, widthPad, heightPad),
        PadPositionC(width.toFloat() * 0.285f, height.toFloat() * 0.665f, widthPad, heightPad),
        PadPositionC(width.toFloat() * 0.56f, height.toFloat() * 0.53f, widthPad, heightPad),
        PadPositionC(width.toFloat() * 0.56f, height.toFloat() * 0.803f, widthPad, heightPad)
    )

    private lateinit var font: BitmapFont


    // -------------------------------------------------------------------------
    // SONG PROGRESS + BUBBLES
    // -------------------------------------------------------------------------

    /**
     * bubble_music.png va en:
     * app/src/main/assets/bubble_music.png
     *
     * 250x250 px está perfecto para este efecto.
     */
    private val bubbleMusicTexture = Texture(Gdx.files.internal("bubble_music.png"))

    /**
     * Pixel blanco de 1x1 para dibujar la barra sin cargar otra textura.
     */
    private val progressPixelTexture: Texture = createProgressPixelTexture()

    /**
     * Pool fijo: no creamos BubbleParticle durante gameplay.
     */
    private data class BubbleParticle(
        var active: Boolean = false,
        var x: Float = 0f,
        var y: Float = 0f,
        var size: Float = 0f,
        var speedY: Float = 0f,
        var driftX: Float = 0f,
        var wobblePhase: Float = 0f,
        var wobbleSpeed: Float = 0f,
        var life: Float = 0f,
        var maxLife: Float = 0f,
        var baseAlpha: Float = 1f
    )

    private val musicBubbles =
        Array(MAX_MUSIC_BUBBLES) {
            BubbleParticle()
        }

    private var bubbleSpawnTimer = 0f
    private var nextBubbleSpawnTime = 0.16f

    /**
     * Posición de la barra:
     * cámara Y-down, por eso una Y cercana a gdxHeight queda abajo.
     *
     * La barra se dibuja DESPUÉS de player.render(), por lo que queda
     * visualmente enfrente de los pads.
     */
    private val songProgressBarWidth get() = (medidaFlechas * 0.10f)
    private val songProgressBarHeight get() = gdxHeight * 0.42f
    private val songProgressBarX get() = medidaFlechas * 0.08f
    private val songProgressBarY get() = padPositions[1][1] - songProgressBarHeight


    init {
        if(showPadB == 1){
            padB = TextureRegion(Texture(Gdx.files.external("/FingerDance/PadsB/$skinPad.png")))
            spritePadB = Sprite(padB).apply { flip(false, true) }
        }else if(showPadB == 2){
            padB = TextureRegion(Texture(Gdx.files.external("/FingerDance/PadsC/$skinPad/BG.png")))
            padB.flip(false, true)

            padLefDownC = getPadC(Texture(Gdx.files.external("/FingerDance/PadsC/$skinPad/DownLeft.png")))
            padLeftUpC = getPadC(Texture(Gdx.files.external("/FingerDance/PadsC/$skinPad/UpLeft.png")))
            padCenterC = getPadC(Texture(Gdx.files.external("/FingerDance/PadsC/$skinPad/Center.png")))
            padRightUpC = getPadC(Texture(Gdx.files.external("/FingerDance/PadsC/$skinPad/UpRight.png")))
            padRightDownC = getPadC(Texture(Gdx.files.external("/FingerDance/PadsC/$skinPad/DownRight.png")))

            arrPadsC = arrayOf(padLefDownC, padLeftUpC, padCenterC, padRightUpC, padRightDownC)
        }else if(showPadB == 3){
            when(typePadD){
                0 -> {
                    arrayPad4Bg = getTexturePad4(Texture(Gdx.files.external("/FingerDance/PadsD/arrows_pad_bg.png")))
                    arrayPad4 = getTexturePad4(Texture(Gdx.files.external("/FingerDance/PadsD/arrows_pad.png")))
                }
                1 -> {
                    arrayPad4Bg = getTexturePad4(Texture(Gdx.files.external("/FingerDance/PadsD/arrows_pad_bg_m.png")))
                    arrayPad4 = getTexturePad4(Texture(Gdx.files.external("/FingerDance/PadsD/arrows_pad_m.png")))
                }
                2 -> {
                    arrayPad4Bg = getTexturePad4(Texture(Gdx.files.external("/FingerDance/PadsD/arrows_pad_bg_n.png")))
                    arrayPad4 = getTexturePad4(Texture(Gdx.files.external("/FingerDance/PadsD/arrows_pad_n.png")))
                }
            }
        }
        imgsJudge.forEach { it.flip(false, true) }
        imgsTypeCombo.forEach { it.flip(false, true) }
    }
    override fun show() {
        batch = SpriteBatch()
        font = BitmapFont()
        font.color = Color.WHITE
        font.data.setScale(2f, -2f)
        stage = Stage(ScreenViewport())
        camera = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.setToOrtho(true)

        player = PlayerSsc(this, batch, a)
        rithymAnim = (60f / player.m_fCurBPM)
        targetTop = medidaFlechas

        if(showPadB == 0){
            padLefDown.flip(false, true)
            padLeftUp.flip(false, true)
            padCenter.flip(false, true)
            padRightUp.flip(false, true)
            padRightDown.flip(false, true)
        }
    }

    override fun render(delta: Float) {
        ScreenUtils.clear(0f, 0f, 0f, 0f)
        camera.update()
        batch.projectionMatrix = camera.combined

        if (!isPaused) {
            val songTimeMs = a.getSongTimeMs()
            elapsedTime += delta

            batch.begin()
            showBgPads()
            player.updateStepData(songTimeMs)
            //batch.color = Color(0f, 0f, 0f, 0f)

            if(!playerSong.fd){

                intervalOverlay = (60 / abs(player.m_fCurBPM)) / 2f
                timer += delta
                if (timer >= intervalOverlay) {
                    timer -= intervalOverlay
                    showOverlay = !showOverlay
                }
                drawRecepts()
            }

            player.render(songTimeMs)
            if(showSongProgress){
                drawSongProgress(songTimeMs = songTimeMs, delta = delta)
            }

            //font.draw(batch, "Beat: %.3f".format(player.beatToShow), 20f, 40f)

            barBlack.setSize(maxWidth, maxlHeight)
            barBlack.setPosition(medidaFlechas, 0f)

            barRed.setSize(maxWidth, maxlHeight)
            barRed.setPosition(medidaFlechas, 0f)

            if (isEndingFade) {
                endingFadeAlpha += delta * 1.8f
                if (endingFadeAlpha > 1f) {
                    endingFadeAlpha = 1f
                }
                batch.setColor(0f, 0f, 0f, endingFadeAlpha)
                batch.draw(fadeTexture, 0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
                batch.setColor(1f, 1f, 1f, 1f)
            }

            batch.end()
            stage.act(delta)
        }

        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        camera.setToOrtho(true, width.toFloat(), height.toFloat())
        camera.update()
    }

    private fun getLifeLightningFrames(texture: Texture): Array<TextureRegion> {
        val tmp = TextureRegion.split(texture, texture.width / 4, texture.height / 6)
        val frames = arrayOf(
            tmp[0][0], tmp[0][1], tmp[0][2], tmp[0][3],
            tmp[1][0], tmp[1][1], tmp[1][2], tmp[1][3],
            tmp[2][0], tmp[2][1], tmp[2][2], tmp[2][3],
            tmp[3][0], tmp[3][1], tmp[3][2], tmp[3][3],
            tmp[4][0], tmp[4][1], tmp[4][2], tmp[4][3],
            tmp[5][0], tmp[5][1], tmp[5][2], tmp[5][3]
        )
        frames.forEach { it.flip(false, true) }
        return frames
    }

    private fun getTexturePad4(texture: Texture): Array<TextureRegion> {
        val tmp = TextureRegion.split(texture, texture.width / 5, texture.height)
        val frames = arrayOf(
            tmp[0][0],
            tmp[0][1],
            tmp[0][2],
            tmp[0][3],
            tmp[0][4],
        )
        frames[0].flip(false, true)
        frames[1].flip(false, true)
        frames[2].flip(false, true)
        frames[3].flip(false, true)
        frames[4].flip(false, true)

        return frames
    }

    private fun getPadC(texture: Texture) : Array<TextureRegion>{
        val tmp = TextureRegion.split(texture, texture.width, texture.height / 6)
        val frames = arrayOf(
            tmp[0][0],
            tmp[1][0],
            tmp[2][0],
            tmp[3][0],
            tmp[4][0],
            tmp[5][0],
        )
        frames[0].flip(false, true)
        frames[1].flip(false, true)
        frames[2].flip(false, true)
        frames[3].flip(false, true)
        frames[4].flip(false, true)
        frames[5].flip(false, true)
        return frames
    }

    private fun showBgPads() {
        if(showPadB == 0){
            if(!hideImagesPadA){

                batch.draw(padLefDown, padPositions[0][0], padPositions[0][1], widthBtns, heightBtns)
                batch.draw(padLeftUp, padPositions[1][0], padPositions[1][1], widthBtns, heightBtns)
                batch.draw(padCenter, padPositions[2][0], padPositions[2][1], widthBtns, heightBtns)
                batch.draw(padRightUp, padPositions[3][0], padPositions[3][1], widthBtns, heightBtns)
                batch.draw(padRightDown, padPositions[4][0], padPositions[4][1], widthBtns, heightBtns)
            }
        }else if (showPadB == 1){
            spritePadB.setAlpha(alphaPadB)
            spritePadB.setBounds(0f, posYpadB, width.toFloat(), width.toFloat() * 1.1f)
            spritePadB.draw(batch)
        }else if (showPadB == 2){
            batch.draw(padB,width.toFloat() * 0.05f,  height.toFloat() * 0.55f, width.toFloat() * 0.9f, height.toFloat() * 0.45f)
        }else if (showPadB == 3){
            batch.draw(arrayPad4Bg[0], padPositions[0][0], padPositions[0][1], widthBtns, heightBtns)
            batch.draw(arrayPad4Bg[1], padPositions[1][0], padPositions[1][1], widthBtns, heightBtns)
            batch.draw(arrayPad4Bg[2], padPositions[2][0], padPositions[2][1], widthBtns, heightBtns)
            batch.draw(arrayPad4Bg[3], padPositions[3][0], padPositions[3][1], widthBtns, heightBtns)
            batch.draw(arrayPad4Bg[4], padPositions[4][0], padPositions[4][1], widthBtns, heightBtns)
        }
    }

    private fun drawRecepts() {
        drawReceptor(recept0Frames[0], 0)
        drawReceptor(recept1Frames[0], 1)
        drawReceptor(recept2Frames[0], 2)
        drawReceptor(recept3Frames[0], 3)
        drawReceptor(recept4Frames[0], 4)

        if (showOverlay) {
            aBatch = batch.blendSrcFunc
            bBatch = batch.blendDstFunc
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
            drawReceptor(recept0Frames[1], 0)
            drawReceptor(recept1Frames[1], 1)
            drawReceptor(recept2Frames[1], 2)
            drawReceptor(recept3Frames[1], 3)
            drawReceptor(recept4Frames[1], 4)
            batch.setBlendFunction(aBatch, bBatch)
        }
    }

    private fun drawReceptor(frame: TextureRegion, column: Int) {
        val logicalX = medidaFlechas * (column + 1) + luaRecepts.screenX
        val metrics = receptorMetrics[column]
        batch.draw(frame, metrics.drawX(logicalX, medidaFlechas), metrics.drawY(targetTop, medidaFlechas), metrics.drawWidth(medidaFlechas), metrics.drawHeight(medidaFlechas))
    }

    private fun getReceptsTexture(arrow: Texture, isMirror: Boolean = false, metricsColumn: Int? = null): Array<TextureRegion> {
        val tmp = TextureRegion.split(arrow, arrow.width, arrow.height / 3)
        val textureData = arrow.textureData
        if (!textureData.isPrepared) textureData.prepare()
        val pixmap = textureData.consumePixmap()

        try {
            if (metricsColumn != null) receptorMetrics[metricsColumn] = calculateNoteCellMetrics(tmp[0][0], pixmap, isMirror)
            val frames = arrayOf(tmp[0][0], tmp[1][0], tmp[2][0])
            frames.forEach { it.flip(isMirror, true) }
            return frames
        } finally {
            if (textureData.disposePixmap()) pixmap.dispose()
        }
    }

    private data class Bounds(val minX: Int, val minY: Int, val maxX: Int, val maxY: Int) {
        val width get() = maxX - minX + 1
        val height get() = maxY - minY + 1
    }

    private fun getVisibleBounds(region: TextureRegion, pixmap: Pixmap, alphaThreshold: Int = 1): Bounds {
        val sourceX = region.regionX
        val sourceY = region.regionY
        val sourceWidth = region.regionWidth
        val sourceHeight = region.regionHeight
        var minX = sourceWidth
        var minY = sourceHeight
        var maxX = -1
        var maxY = -1

        for (y in 0 until sourceHeight) {
            for (x in 0 until sourceWidth) {
                val alpha = pixmap.getPixel(sourceX + x, sourceY + y) and 0xFF
                if (alpha >= alphaThreshold) {
                    if (x < minX) minX = x
                    if (y < minY) minY = y
                    if (x > maxX) maxX = x
                    if (y > maxY) maxY = y
                }
            }
        }

        return if (maxX < minX || maxY < minY) Bounds(0, 0, sourceWidth - 1, sourceHeight - 1) else Bounds(minX, minY, maxX, maxY)
    }

    private fun calculateNoteCellMetrics(baseFrame: TextureRegion, pixmap: Pixmap, isMirror: Boolean): NoteCellMetrics {
        val bounds = getVisibleBounds(baseFrame, pixmap)
        val cellWidth = baseFrame.regionWidth.toFloat()
        val cellHeight = baseFrame.regionHeight.toFloat()
        val offsetX = if (isMirror) (baseFrame.regionWidth - bounds.maxX - 1).toFloat() / cellWidth else bounds.minX.toFloat() / cellWidth
        return NoteCellMetrics(
            visibleWidthRatio = bounds.width / cellWidth,
            visibleHeightRatio = bounds.height / cellHeight,
            visibleOffsetXRatio = offsetX,
            visibleOffsetYRatio = bounds.minY / cellHeight
        )
    }

    private fun getListNumbers(arrow: Texture) : Array<TextureRegion> {
        val tmp = TextureRegion.split(arrow, arrow.width / 10, arrow.height)
        val frames = arrayOf(
            tmp[0][0],
            tmp[0][1],
            tmp[0][2],
            tmp[0][3],
            tmp[0][4],
            tmp[0][5],
            tmp[0][6],
            tmp[0][7],
            tmp[0][8],
            tmp[0][9]
        )
        frames[0].flip(false, true)
        frames[1].flip(false, true)
        frames[2].flip(false, true)
        frames[3].flip(false, true)
        frames[4].flip(false, true)
        frames[5].flip(false, true)
        frames[6].flip(false, true)
        frames[7].flip(false, true)
        frames[8].flip(false, true)
        frames[9].flip(false, true)

        return frames
    }


    // -------------------------------------------------------------------------
    // SONG PROGRESS
    // -------------------------------------------------------------------------

    private fun drawSongProgress(songTimeMs: Double, delta: Float) {
        val durationMs = mediaPlayer.duration.toDouble()
        if (durationMs <= 0.0) return

        val rawProgress = (songTimeMs / durationMs).toFloat()
        val progress = if (rawProgress >= 0.985f) 1f else rawProgress.coerceIn(0f, 1f)

        updateMusicBubbles(progress, delta)
        drawProgressBar(progress)
        drawMusicBubbles()
    }

    private fun drawProgressBar(progress: Float) {
        val x = songProgressBarX
        val y = songProgressBarY
        val widthBar = songProgressBarWidth
        val heightBar = songProgressBarHeight

        val glow = widthBar * 0.55f
        val border = (widthBar * 0.18f).coerceAtLeast(1f)

        batch.setColor(0f, 0.85f, 1f, 0.10f)
        batch.draw(progressPixelTexture, x - glow, y - glow, widthBar + glow * 2f, heightBar + glow * 2f)

        batch.setColor(0.10f, 0.30f, 0.42f, 0.90f)
        batch.draw(progressPixelTexture, x, y, widthBar, heightBar)

        batch.setColor(0.25f, 0.95f, 1f, 0.95f)
        batch.draw(progressPixelTexture, x, y, widthBar, border)
        batch.draw(progressPixelTexture, x, y + heightBar - border, widthBar, border)
        batch.draw(progressPixelTexture, x, y, border, heightBar)
        batch.draw(progressPixelTexture, x + widthBar - border, y, border, heightBar)

        val innerX = x + border
        val innerY = y + border
        val innerWidth = widthBar - border * 2f
        val innerHeight = heightBar - border * 2f

        batch.setColor(0.01f, 0.03f, 0.07f, 0.90f)
        batch.draw(progressPixelTexture, innerX, innerY, innerWidth, innerHeight)

        val fillHeight = innerHeight * progress

        if (fillHeight > 0f) {
            val fillY = innerY + innerHeight - fillHeight

            batch.setColor(0f, 0.72f, 1f, 0.95f)
            batch.draw(progressPixelTexture, innerX, fillY, innerWidth, fillHeight)

            val highlightWidth = (innerWidth * 0.28f).coerceAtLeast(1f)

            batch.setColor(0.55f, 1f, 1f, 0.75f)
            batch.draw(progressPixelTexture, innerX, fillY, highlightWidth, fillHeight)

            val tipHeight = (medidaFlechas * 0.04f).coerceAtLeast(2f)
            val tipY = fillY - tipHeight * 0.5f

            batch.setColor(0f, 0.90f, 1f, 0.18f)
            batch.draw(progressPixelTexture, x - widthBar * 0.8f, tipY - tipHeight, widthBar * 2.6f, tipHeight * 3f)

            batch.setColor(0.55f, 1f, 1f, 1f)
            batch.draw(progressPixelTexture, x - border, tipY, widthBar + border * 2f, tipHeight)
        }

        resetProgressColor()
    }
    // -------------------------------------------------------------------------
    // BUBBLES
    // -------------------------------------------------------------------------

    private fun updateMusicBubbles(progress: Float, delta: Float) {
        bubbleSpawnTimer += delta
        if (progress > 0.001f && progress < 0.999f && bubbleSpawnTimer >= nextBubbleSpawnTime) {
            bubbleSpawnTimer -= nextBubbleSpawnTime
            spawnMusicBubble(progress)
            if (MathUtils.randomBoolean(0.20f)) spawnMusicBubble(progress)
            nextBubbleSpawnTime = MathUtils.random(0.13f, 0.22f)
        }

        for (bubble in musicBubbles) {
            if (!bubble.active) continue
            bubble.life += delta
            if (bubble.life >= bubble.maxLife) {
                bubble.active = false
                continue
            }
            bubble.y -= bubble.speedY * delta
            bubble.x += bubble.driftX * delta
            bubble.wobblePhase += bubble.wobbleSpeed * delta
            bubble.x += MathUtils.sin(bubble.wobblePhase) * medidaFlechas * 0.0025f
        }
    }

    private fun spawnMusicBubble(progress: Float) {
        var bubble: BubbleParticle? = null
        for (candidate in musicBubbles) {
            if (!candidate.active) {
                bubble = candidate
                break
            }
        }
        val b = bubble ?: return
        val border = (songProgressBarWidth * 0.20f).coerceAtLeast(1f)
        val innerY = songProgressBarY + border
        val innerHeight = (songProgressBarHeight - border * 2f).coerceAtLeast(0f)
        val fillHeight = innerHeight * progress
        val tipY = innerY + innerHeight - fillHeight
        val size = MathUtils.random(medidaFlechas * 0.07f, medidaFlechas * 0.24f)
        b.x = songProgressBarX + songProgressBarWidth * 0.5f - size * 0.5f + MathUtils.random(-medidaFlechas * 0.06f, medidaFlechas * 0.12f)
        b.y = tipY - size * MathUtils.random(0.45f, 0.85f)
        b.size = size
        b.speedY = MathUtils.random(medidaFlechas * 0.45f, medidaFlechas * 0.95f)
        b.driftX = MathUtils.random(0f, medidaFlechas * 0.12f)
        b.wobblePhase = MathUtils.random(0f, MathUtils.PI2)
        b.wobbleSpeed = MathUtils.random(2.2f, 5.0f)
        b.life = 0f
        b.maxLife = MathUtils.random(0.85f, 1.65f)
        b.baseAlpha = MathUtils.random(0.50f, 0.90f)
        b.active = true
    }

    private fun drawMusicBubbles() {
        val previousSrc = batch.blendSrcFunc
        val previousDst = batch.blendDstFunc
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        for (bubble in musicBubbles) {
            if (!bubble.active) continue
            val lifeProgress = (bubble.life / bubble.maxLife).coerceIn(0f, 1f)
            val lifeAlpha = when {
                lifeProgress < 0.12f -> lifeProgress / 0.12f
                lifeProgress > 0.68f -> 1f - ((lifeProgress - 0.68f) / 0.32f)
                else -> 1f
            }.coerceIn(0f, 1f)
            val drawSize = bubble.size * (0.88f + lifeProgress * 0.18f)
            val centerAdjust = (drawSize - bubble.size) * 0.5f
            batch.setColor(1f, 1f, 1f, bubble.baseAlpha * lifeAlpha)
            batch.draw(bubbleMusicTexture, bubble.x - centerAdjust, bubble.y - centerAdjust, drawSize, drawSize)
        }
        batch.setBlendFunction(previousSrc, previousDst)
        resetProgressColor()
    }

    private fun createProgressPixelTexture(): Texture {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(
            Color.WHITE
        )

        pixmap.fill()
        val texture = Texture(pixmap)

        pixmap.dispose()

        return texture
    }

    private fun resetProgressColor() {
        batch.setColor(1f, 1f, 1f, 1f)
    }

    override fun pause() {
        isPaused = true
    }

    override fun resume() {
        isPaused = false
    }

    override fun hide() {}

    override fun dispose() {
        batch.dispose()
        stage.dispose()

        padLefDown.texture.dispose()
        padLeftUp.texture.dispose()
        padCenter.texture.dispose()
        padRightUp.texture.dispose()
        padRightDown.texture.dispose()

        imgPerfect.texture.dispose()
        imgGreat.texture.dispose()
        imgGood.texture.dispose()
        imgBad.texture.dispose()
        imgMiss.texture.dispose()

        imgCombo.texture.dispose()
        imgComboMiss.texture.dispose()

        imgNumbers.dispose()
        imgNumbersMiss.dispose()

        textureLD.dispose()
        textureLU.dispose()
        textureCE.dispose()

        backgroundTexture.dispose()
        barBlackTexture.dispose()
        barRedTexture.dispose()
        barLifeTexture.dispose()
        barTipTexture.dispose()
        fadeTexture.dispose()
        font.dispose()

        if (showPadB == 1 || showPadB == 2) {
            padB.texture.dispose()
        }

        if (showPadB == 2) {
            arrPadsC.forEach { it[0].texture.dispose() }
        }

        if (showPadB == 3) {
            arrayPad4Bg[0].texture.dispose()
            arrayPad4[0].texture.dispose()
        }
        lifeLightningTexture.dispose()

        bubbleMusicTexture.dispose()
        progressPixelTexture.dispose()

        player.disposePlayer()
    }


    companion object {
        private const val MAX_MUSIC_BUBBLES = 14
    }

}