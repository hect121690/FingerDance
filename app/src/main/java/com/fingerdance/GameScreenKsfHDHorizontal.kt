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

open class GameScreenKsfHDHorizontal(activity: GameScreenActivityHorizontal) : Screen {
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

    private val textureLD = Texture(Gdx.files.absolute("$ruta/DownLeft Ready Receptor 1x3.png"))
    private val textureLU = Texture(Gdx.files.absolute("$ruta/UpLeft Ready Receptor 1x3.png"))
    private val textureCE = Texture(Gdx.files.absolute("$ruta/Center Ready Receptor 1x3.png"))

    lateinit var arrayPad4Bg : Array<TextureRegion>
    lateinit var arrayPad4 : Array<TextureRegion>

    val receptLD = getReceptsTexture(textureLD)
    val receptLU = getReceptsTexture(textureLU)
    val receptCE = getReceptsTexture(textureCE)
    val receptRU = getReceptsTexture(textureLU, true)
    val receptRD = getReceptsTexture(textureLD, true)

    var targetTop = 0f
    private var elapsedTime = 0f
    private var rithymAnim = 0f

    private var isPaused = false
    lateinit var camera : OrthographicCamera
    lateinit var player: PlayerHDHorizontal
    private val posYpadB = (medidaFlechasHorizontal * 2)
    private val posXpadRight = height - (widthBtnsHorizontal * 3)

    private var timer = 0f
    private var showOverlay = false
    private var intervalOverlay = 60000 / displayBPM

    val gdxHeight = Gdx.graphics.height
    val gdxWidth = Gdx.graphics.width
    val maxWidth = medidaFlechasHorizontal * 7f
    val maxlHeight = medidaFlechasHorizontal / 2f

    val gaugeIncNormal = floatArrayOf(0.03f, 0.015f, 0.01f, -0.02f, -0.1f, 0.002f)
    val gaugeIncHJ = floatArrayOf(0.015f, 0.007f, 0.005f, -0.04f, -0.15f, 0.001f)
    val posYGauje = medidaFlechasHorizontal / 4f

    init {
        if(showPadB == 1){
            padB = TextureRegion(Texture(Gdx.files.external("/FingerDance/PadsB/$skinPad.png")))
            spritePadB = Sprite(padB).apply { flip(false, true) }
        }else if(showPadB == 2){
            padB = TextureRegion(Texture(Gdx.files.external("/FingerDance/PadsC/$skinPad/BG.png")))
            padB.flip(false, true)

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
        stage = Stage(ScreenViewport())
        camera = OrthographicCamera()
        camera.setToOrtho(true, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

        player = PlayerHDHorizontal(batch, a)
        rithymAnim = (60f / displayBPM)
        targetTop = medidaFlechasHorizontal

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
            showBgPads()
            player.updateStepData(currentTime)

            elapsedTime += delta

            if(!playerSong.fd){
                intervalOverlay = (60 / abs(player.m_fCurBPM)) / 2f
                timer += delta
                if (timer >= intervalOverlay) {
                    timer -= intervalOverlay
                    showOverlay = !showOverlay
                }
                drawRecepts(player.luaReceptOffsetX)
            }

            player.render(currentTime)

            barBlack.setSize(maxWidth, maxlHeight)
            barBlack.setPosition(medidaFlechasHorizontal, posYGauje)

            barRed.setSize(maxWidth, maxlHeight)
            barRed.setPosition(medidaFlechasHorizontal, posYGauje)

            batch.end()
            stage.act(delta)
        }

        stage.draw()
    }

    override fun resize(width: Int, height: Int) {
        camera.setToOrtho(true, width.toFloat(), height.toFloat())
        camera.update()
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

    private fun showBgPads() {
        if(showPadB == 0){
            if(!hideImagesPadA){
                // 6 pads visibles (left a right, centrados)
                batch.draw(padLefDown, padPositionsHorizontalHD[2][0], padPositionsHorizontalHD[2][1], widthBtnsHorizontal, heightBtnsHorizontal)
                batch.draw(padLeftUp, padPositionsHorizontalHD[3][0], padPositionsHorizontalHD[3][1], widthBtnsHorizontal, heightBtnsHorizontal)
                batch.draw(padCenter, padPositionsHorizontalHD[4][0], padPositionsHorizontalHD[4][1], widthBtnsHorizontal, heightBtnsHorizontal)
                batch.draw(padRightUp, padPositionsHorizontalHD[5][0], padPositionsHorizontalHD[5][1], widthBtnsHorizontal, heightBtnsHorizontal)
                batch.draw(padRightDown, padPositionsHorizontalHD[6][0], padPositionsHorizontalHD[6][1], widthBtnsHorizontal, heightBtnsHorizontal)
                batch.draw(padCenter, padPositionsHorizontalHD[7][0], padPositionsHorizontalHD[7][1], widthBtnsHorizontal, heightBtnsHorizontal)
            }
        }else if (showPadB == 1){
            spritePadB.setAlpha(alphaPadB)
            // IZQUIERDA
            spritePadB.setBounds(0f, posYpadB, widthBtnsHorizontal * 3, width - (medidaFlechasHorizontal * 2))
            spritePadB.draw(batch)

            // DERECHA
            spritePadB.setBounds(posXpadRight, posYpadB, widthBtnsHorizontal * 3, width - (medidaFlechasHorizontal * 2))
            spritePadB.draw(batch)
        }else if (showPadB == 3){
            batch.draw(arrayPad4Bg[2], padPositionsHorizontalHD[2][0], padPositionsHorizontalHD[2][1], widthBtnsHorizontal, heightBtnsHorizontal)
            batch.draw(arrayPad4Bg[3], padPositionsHorizontalHD[3][0], padPositionsHorizontalHD[3][1], widthBtnsHorizontal, heightBtnsHorizontal)
            batch.draw(arrayPad4Bg[4], padPositionsHorizontalHD[4][0], padPositionsHorizontalHD[4][1], widthBtnsHorizontal, heightBtnsHorizontal)
            batch.draw(arrayPad4Bg[0], padPositionsHorizontalHD[5][0], padPositionsHorizontalHD[5][1], widthBtnsHorizontal, heightBtnsHorizontal)
            batch.draw(arrayPad4Bg[1], padPositionsHorizontalHD[6][0], padPositionsHorizontalHD[6][1], widthBtnsHorizontal, heightBtnsHorizontal)
            batch.draw(arrayPad4Bg[2], padPositionsHorizontalHD[7][0], padPositionsHorizontalHD[7][1], widthBtnsHorizontal, heightBtnsHorizontal)
        }
    }

    val spaceInitHorizontalHD = spaceInitHorizontal + (medidaFlechasHorizontal * 0.5f)
    private fun drawRecepts(luaReceptOffsetX: Float) {
        batch.draw(receptCE[0], luaReceptOffsetX + spaceInitHorizontalHD, targetTop, medidaFlechasHorizontal, medidaFlechasHorizontal)
        batch.draw(receptRU[0], (medidaFlechasHorizontal) + luaReceptOffsetX + spaceInitHorizontalHD, targetTop, medidaFlechasHorizontal, medidaFlechasHorizontal)
        batch.draw(receptRD[0], (medidaFlechasHorizontal * 2) + luaReceptOffsetX + spaceInitHorizontalHD, targetTop, medidaFlechasHorizontal, medidaFlechasHorizontal)
        batch.draw(receptLD[0], (medidaFlechasHorizontal * 3) + luaReceptOffsetX + spaceInitHorizontalHD, targetTop, medidaFlechasHorizontal, medidaFlechasHorizontal)
        batch.draw(receptLU[0], (medidaFlechasHorizontal * 4) + luaReceptOffsetX + spaceInitHorizontalHD, targetTop, medidaFlechasHorizontal, medidaFlechasHorizontal)
        batch.draw(receptCE[0], (medidaFlechasHorizontal * 5) + luaReceptOffsetX + spaceInitHorizontalHD, targetTop, medidaFlechasHorizontal, medidaFlechasHorizontal)

        if (showOverlay) {
            aBatch = batch.blendSrcFunc
            bBatch = batch.blendDstFunc
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
            batch.draw(receptCE[1], luaReceptOffsetX + spaceInitHorizontalHD, targetTop, medidaFlechasHorizontal, medidaFlechasHorizontal)
            batch.draw(receptRU[1], (medidaFlechasHorizontal) + luaReceptOffsetX + spaceInitHorizontalHD, targetTop, medidaFlechasHorizontal, medidaFlechasHorizontal)
            batch.draw(receptRD[1], (medidaFlechasHorizontal * 2) + luaReceptOffsetX + spaceInitHorizontalHD, targetTop, medidaFlechasHorizontal, medidaFlechasHorizontal)
            batch.draw(receptLD[1], (medidaFlechasHorizontal * 3) + luaReceptOffsetX + spaceInitHorizontalHD, targetTop, medidaFlechasHorizontal, medidaFlechasHorizontal)
            batch.draw(receptLU[1], (medidaFlechasHorizontal * 4) + luaReceptOffsetX + spaceInitHorizontalHD, targetTop, medidaFlechasHorizontal, medidaFlechasHorizontal)
            batch.draw(receptCE[1], (medidaFlechasHorizontal * 5) + luaReceptOffsetX + spaceInitHorizontalHD, targetTop, medidaFlechasHorizontal, medidaFlechasHorizontal)
            batch.setBlendFunction(aBatch, bBatch)
        }
    }

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

        if (showPadB == 3) {
            arrayPad4Bg[0].texture.dispose()
            arrayPad4[0].texture.dispose()
        }

        player.disposePlayer()
    }

}
