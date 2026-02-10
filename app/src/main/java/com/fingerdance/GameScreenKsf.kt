package com.fingerdance

import android.os.SystemClock
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ScreenViewport
import kotlin.math.abs

open class GameScreenKsf(activity: GameScreenActivity) : Screen {
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

    private val textureLD = Texture(Gdx.files.absolute("$ruta/DownLeft Ready Receptor 1x3.png"))
    private val textureLU = Texture(Gdx.files.absolute("$ruta/UpLeft Ready Receptor 1x3.png"))
    private val textureCE = Texture(Gdx.files.absolute("$ruta/Center Ready Receptor 1x3.png"))

    lateinit var arrayPad4Bg : Array<TextureRegion>
    lateinit var arrayPad4 : Array<TextureRegion>

    val recept0Frames = getReceptsTexture(textureLD)
    val recept1Frames = getReceptsTexture(textureLU)
    val recept2Frames = getReceptsTexture(textureCE)
    val recept3Frames = getReceptsTexture(textureLU, true)
    val recept4Frames = getReceptsTexture(textureLD, true)

    var targetTop = 0f
    private var elapsedTime = 0f
    private var rithymAnim = 0f

    private var isPaused = false
    lateinit var camera : OrthographicCamera
    lateinit var player: Player
    private val posYpadB = height.toFloat() - (width.toFloat() * 1.1f)

    private var timer = 0f
    private var showOverlay = false
    private var intervalOverlay = 60000 / displayBPM

    val gdxHeight = Gdx.graphics.height
    val gdxWidth = Gdx.graphics.width
    val maxWidth = medidaFlechas * 5f
    val maxlHeight = medidaFlechas / 2f

    val gaugeIncNormal = floatArrayOf(0.03f, 0.015f, 0.01f, -0.02f, -0.1f, 0.002f)
    val gaugeIncHJ = floatArrayOf(0.015f, 0.007f, 0.005f, -0.04f, -0.15f, 0.001f)

    data class PadPositionC(val x: Float, val y: Float, val size: Float)
    val padPositionsC = listOf(
        PadPositionC(width.toFloat() * 0.015f, width.toFloat() * 1.61f, medidaFlechas * 3f),
        PadPositionC(width.toFloat() * 0.015f, width.toFloat() * 1.063f, medidaFlechas * 3f),
        PadPositionC(width.toFloat() * 0.283f, width.toFloat() * 1.334f, medidaFlechas * 3f),
        PadPositionC(width.toFloat() * 0.558f, width.toFloat() * 1.063f, medidaFlechas * 3f),
        PadPositionC(width.toFloat() * 0.558f, width.toFloat() * 1.61f, medidaFlechas * 3f)
    )

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
            arrayPad4Bg = getTexturePad4(Texture(Gdx.files.external("/FingerDance/PadsD/arrows_pad_bg.png")))
            arrayPad4 = getTexturePad4(Texture(Gdx.files.external("/FingerDance/PadsD/arrows_pad.png")))
        }
        imgsJudge.forEach { it.flip(false, true) }
        imgsTypeCombo.forEach { it.flip(false, true) }
    }
    override fun show() {
        batch = SpriteBatch()
        stage = Stage(ScreenViewport())
        camera = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.setToOrtho(true)

        player = Player(batch, a)
        rithymAnim = (60f / displayBPM)
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
            val currentTime = (elapsedTime * 1000).toLong() - 1000

            batch.begin()
            player.updateStepData(currentTime)
            //batch.color = Color(0f, 0f, 0f, 0f)

            elapsedTime += delta

            if(!playerSong.fd){
                intervalOverlay = (60 / abs(player.m_fCurBPM)) / 2f
                timer += delta
                if (timer >= intervalOverlay) {
                    timer -= intervalOverlay
                    showOverlay = !showOverlay
                }
                //ySpinAngle += Gdx.graphics.deltaTime * ySpinSpeed
                drawRecepts(player.luaReceptOffsetX)
            }
            showBgPads()

            player.render(currentTime)

            barBlack.setSize(maxWidth, maxlHeight)
            barBlack.setPosition(medidaFlechas, 0f)

            barRed.setSize(maxWidth, maxlHeight)
            barRed.setPosition(medidaFlechas, 0f)

            batch.end()
            stage.act(delta)
        }

        stage.draw()
    }

    fun timeGetTime(): Long{
        return SystemClock.uptimeMillis()
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
            batch.draw(padB,width.toFloat() * 0.05f,  width.toFloat() * 1.1f, width.toFloat() * 0.9f, width.toFloat() * 0.9f)
        }else if (showPadB == 3){
            batch.draw(arrayPad4Bg[0], padPositions[0][0], padPositions[0][1], widthBtns, heightBtns)
            batch.draw(arrayPad4Bg[1], padPositions[1][0], padPositions[1][1], widthBtns, heightBtns)
            batch.draw(arrayPad4Bg[2], padPositions[2][0], padPositions[2][1], widthBtns, heightBtns)
            batch.draw(arrayPad4Bg[3], padPositions[3][0], padPositions[3][1], widthBtns, heightBtns)
            batch.draw(arrayPad4Bg[4], padPositions[4][0], padPositions[4][1], widthBtns, heightBtns)
        }
    }
    private var aBatch = 0
    private var bBatch = 0

    private fun drawRecepts(luaReceptOffsetX: Float) {
        batch.draw(recept0Frames[0], (medidaFlechas) + luaReceptOffsetX, targetTop, medidaFlechas, medidaFlechas)
        batch.draw(recept1Frames[0], (medidaFlechas * 2) + luaReceptOffsetX, targetTop, medidaFlechas, medidaFlechas)
        batch.draw(recept2Frames[0], (medidaFlechas * 3) + luaReceptOffsetX, targetTop, medidaFlechas, medidaFlechas)
        batch.draw(recept3Frames[0], (medidaFlechas * 4) + luaReceptOffsetX, targetTop, medidaFlechas, medidaFlechas)
        batch.draw(recept4Frames[0], (medidaFlechas * 5) + luaReceptOffsetX, targetTop, medidaFlechas, medidaFlechas)

        if (showOverlay) {
            aBatch = batch.blendSrcFunc
            bBatch = batch.blendDstFunc
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
            batch.draw(recept0Frames[1], (medidaFlechas) + luaReceptOffsetX, targetTop, medidaFlechas, medidaFlechas)    
            batch.draw(recept1Frames[1], (medidaFlechas * 2) + luaReceptOffsetX, targetTop, medidaFlechas, medidaFlechas)
            batch.draw(recept2Frames[1], (medidaFlechas * 3) + luaReceptOffsetX, targetTop, medidaFlechas, medidaFlechas)
            batch.draw(recept3Frames[1], (medidaFlechas * 4) + luaReceptOffsetX, targetTop, medidaFlechas, medidaFlechas)
            batch.draw(recept4Frames[1], (medidaFlechas * 5) + luaReceptOffsetX, targetTop, medidaFlechas, medidaFlechas)
            batch.setBlendFunction(aBatch, bBatch)
        }
    }

    /*
    private var ySpinAngle = 0f
    private var ySpinSpeed = 3.5f
    private var ySpinEnabled = true

    private fun drawRecepts() {

        // 🔁 actualizar ángulo (puedes mover esto a render())
        if (ySpinEnabled) {
            ySpinAngle += Gdx.graphics.deltaTime * ySpinSpeed
        }

        val bases = floatArrayOf(
            medidaFlechas * 1f,
            medidaFlechas * 2f,
            medidaFlechas * 3f,
            medidaFlechas * 4f,
            medidaFlechas * 5f
        )

        val frames = arrayOf(
            recept0Frames,
            recept1Frames,
            recept2Frames,
            recept3Frames,
            recept4Frames
        )

        // 🎯 pivote = centro de las 5 columnas
        val centerX = medidaFlechas * 3f + medidaFlechas * 0.5f

        val cosA = MathUtils.cos(ySpinAngle)
        val sinA = MathUtils.sin(ySpinAngle)

        // ===== DRAW BASE =====

        for (i in 0 until 5) {

            val baseCenter = bases[i] + medidaFlechas * 0.5f
            val xLocal = baseCenter - centerX

            val depth = xLocal * sinA
            val projCenter = centerX + xLocal * cosA + depth * 0.30f

            val scaleX = abs(cosA).coerceAtLeast(0.2f)

            batch.draw(
                frames[i][0],
                projCenter - medidaFlechas/2f,
                targetTop,
                medidaFlechas/2f,
                medidaFlechas/2f,
                medidaFlechas,
                medidaFlechas,
                scaleX,
                1f,
                0f
            )
        }

        // ===== DRAW OVERLAY =====

        if (showOverlay) {

            aBatch = batch.blendSrcFunc
            bBatch = batch.blendDstFunc
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)

            for (i in 0 until 5) {

                val baseCenter = bases[i] + medidaFlechas * 0.5f
                val xLocal = baseCenter - centerX

                val depth = xLocal * sinA
                val projCenter = centerX + xLocal * cosA + depth * 0.30f

                val scaleX = abs(cosA).coerceAtLeast(0.2f)

                batch.draw(
                    frames[i][1],
                    projCenter - medidaFlechas/2f,
                    targetTop,
                    medidaFlechas/2f,
                    medidaFlechas/2f,
                    medidaFlechas,
                    medidaFlechas,
                    scaleX,
                    1f,
                    0f
                )
            }

            batch.setBlendFunction(aBatch, bBatch)
        }
    }
    */

    private fun getReceptsTexture(arrow: Texture, isMirror: Boolean = false) : Array<TextureRegion> {
        val tmp = TextureRegion.split(arrow, arrow.width, arrow.height / 3)
        val frames = arrayOf(
            tmp[0][0],
            tmp[1][0],
            tmp[2][0]
        )
        frames[0].flip(isMirror, true)
        frames[1].flip(isMirror, true)
        frames[2].flip(isMirror, true)
        return frames
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

    override fun resize(width: Int, height: Int) {}

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

        player.disposePlayer()
    }

}