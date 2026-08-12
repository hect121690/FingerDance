package com.fingerdance.ssc

import android.os.SystemClock
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.fingerdance.*
import com.fingerdance.ssc.GameScreenSsc.OverlayMetrics
import java.io.File
import kotlin.math.abs

open class GameScreenSscHD(activity: GameScreenActivity) : Screen {
    val a = activity

    private lateinit var batch: SpriteBatch
    lateinit var stage: Stage

    val rutaPads = "/FingerDance/Themes/$tema/GraphicsStatics/game_play"

    // ---------------------------------------------------
    // PADS
    // ---------------------------------------------------

    private val padLefDown = TextureRegion(Texture(Gdx.files.external("$rutaPads/left_down.png")))
    private val padLeftUp = TextureRegion(Texture(Gdx.files.external("$rutaPads/left_up.png")))
    private val padCenter = TextureRegion(Texture(Gdx.files.external("$rutaPads/center.png")))
    private val padRightUp = TextureRegion(Texture(Gdx.files.external("$rutaPads/right_up.png")))
    private val padRightDown = TextureRegion(Texture(Gdx.files.external("$rutaPads/right_down.png")))

    // ---------------------------------------------------
    // JUDGES
    // ---------------------------------------------------

    private val imgPerfect = TextureRegion(Texture(Gdx.files.external("$rutaPads/perfect.png")))
    private val imgGreat = TextureRegion(Texture(Gdx.files.external("$rutaPads/great.png")))
    private val imgGood = TextureRegion(Texture(Gdx.files.external("$rutaPads/good.png")))
    private val imgBad = TextureRegion(Texture(Gdx.files.external("$rutaPads/bad.png")))
    private val imgMiss = TextureRegion(Texture(Gdx.files.external("$rutaPads/miss.png")))

    val imgsJudge = arrayOf(imgPerfect, imgGreat, imgGood, imgBad, imgMiss)

    // ---------------------------------------------------
    // COMBO
    // ---------------------------------------------------

    private val imgCombo = TextureRegion(Texture(Gdx.files.external("$rutaPads/combo.png")))
    private val imgComboMiss = TextureRegion(Texture(Gdx.files.external("$rutaPads/comboMiss.png")))

    val imgsTypeCombo = arrayOf(imgCombo, imgComboMiss)

    val imgNumbers = Texture(Gdx.files.external("$rutaPads/numbersCombo.png"))
    val imgNumbersMiss = Texture(Gdx.files.external("$rutaPads/numbersComboMiss.png"))

    val listNumbers = getListNumbers(imgNumbers)
    val listNumbersMiss = getListNumbers(imgNumbersMiss)

    // ---------------------------------------------------
    // LIFE BAR
    // ---------------------------------------------------

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

    // ---------------------------------------------------
    // PAD B/C/D
    // ---------------------------------------------------

    private lateinit var padB: TextureRegion

    lateinit var spritePadB: Sprite

    lateinit var padLefDownC: Array<TextureRegion>
    lateinit var padLeftUpC: Array<TextureRegion>
    lateinit var padCenterC: Array<TextureRegion>
    lateinit var padRightUpC: Array<TextureRegion>
    lateinit var padRightDownC: Array<TextureRegion>

    lateinit var arrPadsC: Array<Array<TextureRegion>>

    lateinit var arrayPad4Bg: Array<TextureRegion>
    lateinit var arrayPad4: Array<TextureRegion>

    // ---------------------------------------------------
    // RECEPTORS
    // ---------------------------------------------------

    data class OverlayMetrics(
        val widthRatio: Float = 1f,
        val heightRatio: Float = 1f,
        val offsetXRatio: Float = 0f,
        val offsetYRatio: Float = 0f
    )

    private val receptorOverlayMetrics = Array(10) { OverlayMetrics() }

    private val textureLD = loadTexture(ruta, "DownLeft Ready Receptor")
    private val textureLU = loadTexture(ruta, "UpLeft Ready Receptor")
    private val textureCE = loadTexture(ruta, "Center Ready Receptor")

    val receptLD = getReceptsTexture(textureLD, metricsColumn = 0)
    val receptLU = getReceptsTexture(textureLU, metricsColumn = 1)
    val receptCE = getReceptsTexture(textureCE, metricsColumn = 2)
    val receptRU = getReceptsTexture(textureLU, true, metricsColumn = 3)
    val receptRD = getReceptsTexture(textureLD, true, metricsColumn = 4)

    // ---------------------------------------------------

    lateinit var camera: OrthographicCamera
    lateinit var player: PlayerSscHD

    var targetTop = 0f

    private var elapsedTime = 0f
    private var rithymAnim = 0f

    private var isPaused = false

    private val posYpadB = height.toFloat() - (width.toFloat() * 1.1f)

    private var timer = 0f
    private var showOverlay = false
    private var intervalOverlay = 60000 / displayBPM

    val gdxHeight = Gdx.graphics.height
    val gdxWidth = Gdx.graphics.width

    val arrowsSize = width / 8f

    val maxWidth = medidaFlechas * 5f
    val maxlHeight = medidaFlechas / 2f

    val gaugeIncNormal = floatArrayOf(0.03f, 0.015f, 0.01f, -0.02f, -0.1f, 0.002f)
    val gaugeIncHJ = floatArrayOf(0.015f, 0.007f, 0.005f, -0.04f, -0.15f, 0.001f)

    private val lifeLightningTexture = Texture(Gdx.files.external("FingerDance/Themes/$tema/GraphicsStatics/game_play/barlife_electric 4x6.png"))
    val lifeLightningFrames: Array<TextureRegion> = getLifeLightningFrames(lifeLightningTexture)
    private val fadeTexture = Texture(Gdx.files.internal("black.png"))

    // ---------------------------------------------------

    init {

        if (showPadB == 1) {
            padB = TextureRegion(Texture(Gdx.files.external("/FingerDance/PadsB/$skinPad.png")))
            spritePadB = Sprite(padB).apply {
                flip(false, true)
            }

        } else if (showPadB == 2) {
            padB = TextureRegion(Texture(Gdx.files.external("/FingerDance/PadsC/$skinPad/BG.png")))
            padB.flip(false, true)
            padLefDownC = getPadC(Texture(Gdx.files.external("/FingerDance/PadsC/$skinPad/DownLeft.png")))
            padLeftUpC = getPadC(Texture(Gdx.files.external("/FingerDance/PadsC/$skinPad/UpLeft.png")))
            padCenterC = getPadC(Texture(Gdx.files.external("/FingerDance/PadsC/$skinPad/Center.png")))
            padRightUpC = getPadC(Texture(Gdx.files.external("/FingerDance/PadsC/$skinPad/UpRight.png")))
            padRightDownC = getPadC(Texture(Gdx.files.external("/FingerDance/PadsC/$skinPad/DownRight.png")))

            arrPadsC = arrayOf(padLefDownC, padLeftUpC, padCenterC, padRightUpC, padRightDownC)

        } else if (showPadB == 3) {
            when (typePadD) {
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

    // ---------------------------------------------------

    override fun show() {
        batch = SpriteBatch()
        stage = Stage(ScreenViewport())
        camera = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.setToOrtho(true)

        player = PlayerSscHD(this, batch, a)
        rithymAnim = (60f / displayBPM)
        targetTop = medidaFlechas

        if (showPadB == 0) {
            padLefDown.flip(false, true)
            padLeftUp.flip(false, true)
            padCenter.flip(false, true)
            padRightUp.flip(false, true)
            padRightDown.flip(false, true)
        }
    }

    // ---------------------------------------------------

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

            if (!playerSong.fd) {
                intervalOverlay = (60 / abs(player.m_fCurBPM)) / 2f
                timer += delta
                if (timer >= intervalOverlay) {
                    timer -= intervalOverlay
                    showOverlay = !showOverlay
                }

                drawRecepts()
            }

            player.render(songTimeMs)

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

    // ---------------------------------------------------

    private fun showBgPads() {

        when (showPadB) {

            0, 1, 2 -> {

                if (!hideImagesPadA) {

                    batch.draw(
                        padCenter,
                        padPositionsHD[2][0],
                        padPositionsHD[2][1],
                        colWidth,
                        heightBtns
                    )

                    batch.draw(
                        padRightUp,
                        padPositionsHD[3][0],
                        padPositionsHD[3][1],
                        colWidth,
                        heightBtns
                    )

                    batch.draw(
                        padRightDown,
                        padPositionsHD[4][0],
                        padPositionsHD[4][1],
                        colWidth,
                        heightBtns
                    )

                    batch.draw(
                        padLefDown,
                        padPositionsHD[5][0],
                        padPositionsHD[5][1],
                        colWidth,
                        heightBtns
                    )

                    batch.draw(
                        padLeftUp,
                        padPositionsHD[6][0],
                        padPositionsHD[6][1],
                        colWidth,
                        heightBtns
                    )

                    batch.draw(
                        padCenter,
                        padPositionsHD[7][0],
                        padPositionsHD[7][1],
                        colWidth,
                        heightBtns
                    )
                }
            }

            else -> {

                batch.draw(
                    arrayPad4Bg[2],
                    padPositionsHD[2][0],
                    padPositionsHD[2][1],
                    colWidth,
                    heightBtns
                )

                batch.draw(
                    arrayPad4Bg[3],
                    padPositionsHD[3][0],
                    padPositionsHD[3][1],
                    colWidth,
                    heightBtns
                )

                batch.draw(
                    arrayPad4Bg[4],
                    padPositionsHD[4][0],
                    padPositionsHD[4][1],
                    colWidth,
                    heightBtns
                )

                batch.draw(
                    arrayPad4Bg[0],
                    padPositionsHD[5][0],
                    padPositionsHD[5][1],
                    colWidth,
                    heightBtns
                )

                batch.draw(
                    arrayPad4Bg[1],
                    padPositionsHD[6][0],
                    padPositionsHD[6][1],
                    colWidth,
                    heightBtns
                )

                batch.draw(
                    arrayPad4Bg[2],
                    padPositionsHD[7][0],
                    padPositionsHD[7][1],
                    colWidth,
                    heightBtns
                )
            }
        }
    }

    // ---------------------------------------------------

    private fun drawRecepts() {
        batch.draw(receptCE[0], arrowsSize + luaRecepts.screenX, targetTop, arrowsSize, arrowsSize)
        batch.draw(receptRU[0], (arrowsSize * 2) + luaRecepts.screenX, targetTop, arrowsSize, arrowsSize)
        batch.draw(receptRD[0], (arrowsSize * 3) + luaRecepts.screenX, targetTop, arrowsSize, arrowsSize)
        batch.draw(receptLD[0], (arrowsSize * 4) + luaRecepts.screenX, targetTop, arrowsSize, arrowsSize)
        batch.draw(receptLU[0], (arrowsSize * 5) + luaRecepts.screenX, targetTop, arrowsSize, arrowsSize)
        batch.draw(receptCE[0], (arrowsSize * 6) + luaRecepts.screenX, targetTop, arrowsSize, arrowsSize)

        if (showOverlay) {
            aBatch = batch.blendSrcFunc
            bBatch = batch.blendDstFunc
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
            drawOverlay(receptCE[1], 0, arrowsSize + luaRecepts.screenX)
            drawOverlay(receptRU[1], 1, (arrowsSize * 2) + luaRecepts.screenX)
            drawOverlay(receptRD[1], 2, (arrowsSize * 3) + luaRecepts.screenX)
            drawOverlay(receptLD[1], 3, (arrowsSize * 4) + luaRecepts.screenX)
            drawOverlay(receptLU[1], 4, (arrowsSize * 5) + luaRecepts.screenX)
            drawOverlay(receptCE[1], 5, (arrowsSize * 6) + luaRecepts.screenX)

            batch.setBlendFunction(aBatch, bBatch)
        }
    }

    // ---------------------------------------------------

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

        val tmp = TextureRegion.split(
            texture,
            texture.width / 5,
            texture.height
        )

        val frames = arrayOf(
            tmp[0][0],
            tmp[0][1],
            tmp[0][2],
            tmp[0][3],
            tmp[0][4],
            tmp[0][0],
            tmp[0][1],
            tmp[0][2],
            tmp[0][3],
            tmp[0][4]
        )

        frames.forEach {
            it.flip(false, true)
        }

        return frames
    }

    // ---------------------------------------------------

    private fun getPadC(texture: Texture): Array<TextureRegion> {

        val tmp = TextureRegion.split(
            texture,
            texture.width,
            texture.height / 6
        )

        val frames = arrayOf(
            tmp[0][0],
            tmp[1][0],
            tmp[2][0],
            tmp[3][0],
            tmp[4][0],
            tmp[5][0]
        )

        frames.forEach {
            it.flip(false, true)
        }

        return frames
    }

    // ---------------------------------------------------

    private fun getReceptsTexture(arrow: Texture, isMirror: Boolean = false, metricsColumn: Int? = null): Array<TextureRegion> {
        val tmp = TextureRegion.split(arrow, arrow.width, arrow.height / 3)
        val textureData = arrow.textureData
        if (!textureData.isPrepared) textureData.prepare()
        val pixmap = textureData.consumePixmap()

        try {
            if (metricsColumn != null) {
                receptorOverlayMetrics[metricsColumn] = calculateOverlayMetrics(
                    baseFrame = tmp[0][0],
                    overlayFrame = tmp[1][0],
                    pixmap = pixmap,
                    isMirror = isMirror
                )
            }

            val frames = arrayOf(
                trimFrame(tmp[0][0], pixmap),
                trimFrame(tmp[1][0], pixmap),
                trimFrame(tmp[2][0], pixmap)
            )

            frames.forEach { it.flip(isMirror, true) }
            return frames
        } finally {
            if (textureData.disposePixmap()) pixmap.dispose()
        }
    }

    private fun drawOverlay(frame: TextureRegion, column: Int, baseX: Float) {
        val metrics = receptorOverlayMetrics[column]

        batch.draw(
            frame,
            baseX + arrowsSize * metrics.offsetXRatio,
            targetTop + arrowsSize * metrics.offsetYRatio,
            arrowsSize * metrics.widthRatio,
            arrowsSize * metrics.heightRatio
        )
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

        return if (maxX < minX || maxY < minY) {
            Bounds(0, 0, sourceWidth - 1, sourceHeight - 1)
        } else {
            Bounds(minX, minY, maxX, maxY)
        }
    }

    private fun calculateOverlayMetrics(baseFrame: TextureRegion, overlayFrame: TextureRegion, pixmap: Pixmap, isMirror: Boolean): OverlayMetrics {
        val base = getVisibleBounds(baseFrame, pixmap)
        val overlay = getVisibleBounds(overlayFrame, pixmap)

        val baseWidth = base.width.toFloat()
        val baseHeight = base.height.toFloat()

        val widthRatio = overlay.width / baseWidth
        val heightRatio = overlay.height / baseHeight

        val offsetX = if (!isMirror) {
            (overlay.minX - base.minX) / baseWidth
        } else {
            val sourceWidth = baseFrame.regionWidth

            val baseMirrorX = sourceWidth - base.maxX - 1
            val overlayMirrorX = sourceWidth - overlay.maxX - 1

            (overlayMirrorX - baseMirrorX) / baseWidth
        }

        val offsetY = (overlay.minY - base.minY) / baseHeight

        return OverlayMetrics(
            widthRatio = widthRatio,
            heightRatio = heightRatio,
            offsetXRatio = offsetX,
            offsetYRatio = offsetY
        )
    }

    private fun trimFrame(sourceRegion: TextureRegion, pixmap: Pixmap, alphaThreshold: Int = 1): TextureRegion {
        val sourceX = sourceRegion.regionX
        val sourceY = sourceRegion.regionY
        val sourceWidth = sourceRegion.regionWidth
        val sourceHeight = sourceRegion.regionHeight

        var minX = sourceWidth
        var minY = sourceHeight
        var maxX = -1
        var maxY = -1

        for (y in 0 until sourceHeight) {
            for (x in 0 until sourceWidth) {
                val pixel = pixmap.getPixel(sourceX + x, sourceY + y)
                val alpha = pixel and 0xFF
                if (alpha >= alphaThreshold) {
                    if (x < minX) minX = x
                    if (y < minY) minY = y
                    if (x > maxX) maxX = x
                    if (y > maxY) maxY = y
                }
            }
        }
        if (maxX < minX || maxY < minY) {
            return TextureRegion(sourceRegion, 0, 0, 1, 1)
        }

        val trimmedWidth = maxX - minX + 1
        val trimmedHeight = maxY - minY + 1
        return TextureRegion(sourceRegion, minX, minY, trimmedWidth, trimmedHeight)
    }


    // ---------------------------------------------------

    private fun getListNumbers(arrow: Texture): Array<TextureRegion> {

        val tmp = TextureRegion.split(
            arrow,
            arrow.width / 10,
            arrow.height
        )

        val frames = Array(10) {
            tmp[0][it]
        }

        frames.forEach {
            it.flip(false, true)
        }

        return frames
    }

    // ---------------------------------------------------

    override fun resize(width: Int, height: Int) {
        camera.setToOrtho(true, width.toFloat(), height.toFloat())
        camera.update()
    }

    override fun pause() {
        isPaused = true
    }

    override fun resume() {
        isPaused = false
    }

    override fun hide() {}

    // ---------------------------------------------------

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

        if (showPadB == 1 || showPadB == 2) {
            padB.texture.dispose()
        }

        if (showPadB == 2) {
            arrPadsC.forEach {
                it[0].texture.dispose()
            }
        }

        if (showPadB == 3) {
            arrayPad4Bg[0].texture.dispose()
            arrayPad4[0].texture.dispose()
        }

        player.disposePlayer()
    }

    // ---------------------------------------------------

    fun timeGetTime(): Long {
        return SystemClock.uptimeMillis()
    }
}