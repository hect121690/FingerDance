package com.fingerdance

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ScreenViewport

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

    private lateinit var padB : TextureRegion
    private lateinit var spritePadB: Sprite

    lateinit var padLefDownC : Array<TextureRegion>
    lateinit var padLeftUpC : Array<TextureRegion>
    lateinit var padCenterC : Array<TextureRegion>
    lateinit var padRightUpC : Array<TextureRegion>
    lateinit var padRightDownC : Array<TextureRegion>

    lateinit var arrPadsC : Array<Array<TextureRegion>>

    private val textureLD = Texture(Gdx.files.absolute("$ruta/DownLeft Ready Receptor 1x3.png"))
    private val textureLU = Texture(Gdx.files.absolute("$ruta/UpLeft Ready Receptor 1x3.png"))
    private val textureCE = Texture(Gdx.files.absolute("$ruta/Center Ready Receptor 1x3.png"))

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
        }
    }
    override fun show() {
        batch = SpriteBatch()
        stage = Stage(ScreenViewport())
        stage.addActor(lifeBar)

        camera = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.setToOrtho(true)

        player = Player(batch, a)
        rithymAnim = (60f / displayBPM)
        targetTop = medidaFlechas

        padLefDown.flip(false, true)
        padLeftUp.flip(false, true)
        padCenter.flip(false, true)
        padRightUp.flip(false, true)
        padRightDown.flip(false, true)

    }

    override fun render(delta: Float) {
        ScreenUtils.clear(0f, 0f, 0f, 0f)
        camera.update()
        batch.projectionMatrix = camera.combined
        if (!isPaused) {
            val currentTime = (elapsedTime * 1000).toLong()
            batch.begin()
            elapsedTime += delta
            intervalOverlay = 60 / player.m_fCurBPM
            if(!playerSong.fd){
                timer += delta
                if (timer >= intervalOverlay) {
                    timer -= intervalOverlay
                    showOverlay = !showOverlay
                }
                getReceptsAnimation()
            }
            showBgPads()
            player.updateStepData(currentTime)
            player.render(currentTime)
            batch.end()
            stage.act(delta)
        }

        stage.draw()
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
        }else{
            batch.draw(padB,width.toFloat() * 0.05f,  width.toFloat() * 1.1f, width.toFloat() * 0.9f, width.toFloat() * 0.9f)
        }
    }

    private fun getReceptsAnimation() {
        batch.draw(recept0Frames[0], medidaFlechas, targetTop, medidaFlechas, medidaFlechas)
        batch.draw(recept1Frames[0], medidaFlechas * 2, targetTop, medidaFlechas, medidaFlechas)
        batch.draw(recept2Frames[0], medidaFlechas * 3, targetTop, medidaFlechas, medidaFlechas)
        batch.draw(recept3Frames[0], medidaFlechas * 4, targetTop, medidaFlechas, medidaFlechas)
        batch.draw(recept4Frames[0], medidaFlechas * 5, targetTop, medidaFlechas, medidaFlechas)

        if (showOverlay) {
            batch.draw(recept0Frames[1], medidaFlechas, targetTop, medidaFlechas, medidaFlechas)
            batch.draw(recept1Frames[1], medidaFlechas * 2, targetTop, medidaFlechas, medidaFlechas)
            batch.draw(recept2Frames[1], medidaFlechas * 3, targetTop, medidaFlechas, medidaFlechas)
            batch.draw(recept3Frames[1], medidaFlechas * 4, targetTop, medidaFlechas, medidaFlechas)
            batch.draw(recept4Frames[1], medidaFlechas * 5, targetTop, medidaFlechas, medidaFlechas)
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
        player.disposePlayer()
        playerSong.values.notes.clear()
        elapsedTime = 0f
        rithymAnim = 0f

        textureLD.dispose()
        textureLU.dispose()
        textureCE.dispose()

        if(showPadB == 0){
            padLefDown.texture.dispose()
            padLeftUp.texture.dispose()
            padCenter.texture.dispose()
            padRightUp.texture.dispose()
            padRightDown.texture.dispose()
        }else if(showPadB ==  1){
            padB.texture.dispose()
        }else {
            padLefDownC.forEach { it.texture.dispose() }
            padLeftUpC.forEach { it.texture.dispose() }
            padCenterC.forEach { it.texture.dispose() }
            padRightUpC.forEach { it.texture.dispose() }
            padRightDownC.forEach { it.texture.dispose() }
        }
        recept0Frames.forEach { it.texture.dispose() }
        recept1Frames.forEach { it.texture.dispose() }
        recept2Frames.forEach { it.texture.dispose() }
        recept3Frames.forEach { it.texture.dispose() }
        recept4Frames.forEach { it.texture.dispose() }


    }
}