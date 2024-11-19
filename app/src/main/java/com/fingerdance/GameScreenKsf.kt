package com.fingerdance

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ScreenViewport

open class GameScreenKsf(activity: GameScreenActivity) : Screen {
    val a = activity

    private lateinit var batch: SpriteBatch
    lateinit var stage: Stage

    private lateinit var animationReceptLeftDown: Animation<TextureRegion>
    private lateinit var animationReceptLeftUp: Animation<TextureRegion>
    private lateinit var animationReceptCenter: Animation<TextureRegion>
    private lateinit var animationReceptRightUp: Animation<TextureRegion>
    private lateinit var animationReceptRightDown: Animation<TextureRegion>

    private val rutaPads = "/FingerDance/Themes/$tema/GraphicsStatics/game_play"
    private val padLefDown = TextureRegion(Texture(Gdx.files.external("$rutaPads/left_down.png")))
    private val padLeftUp = TextureRegion(Texture(Gdx.files.external("$rutaPads/left_up.png")))
    private val padCenter = TextureRegion(Texture(Gdx.files.external("$rutaPads/center.png")))
    private val padRightUp = TextureRegion(Texture(Gdx.files.external("$rutaPads/right_up.png")))
    private val padRightDown = TextureRegion(Texture(Gdx.files.external("$rutaPads/right_down.png")))

    val textureLD = Texture(Gdx.files.absolute("$ruta/DownLeft Ready Receptor 1x3.png"))
    val textureLU = Texture(Gdx.files.absolute("$ruta/UpLeft Ready Receptor 1x3.png"))
    val textureCE = Texture(Gdx.files.absolute("$ruta/Center Ready Receptor 1x3.png"))
    //val textureRU = Texture(Gdx.files.absolute("$ruta/UpLeft Ready Receptor 1x3.png"))
    //val textureRD = Texture(Gdx.files.absolute("$ruta/DownLeft Ready Receptor 1x3.png"))

    var targetTop = 0f
    private var elapsedTime = 0f
    private var rithymAnim = 0f

    private var isPaused = false
    lateinit var camera : OrthographicCamera
    lateinit var player: Player

    override fun show() {
        batch = SpriteBatch()
        stage = Stage(ScreenViewport())
        stage.addActor(lifeBar)

        camera = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.setToOrtho(true)
        player = Player(batch, a)

        rithymAnim = (60f / displayBPM)

        targetTop = medidaFlechas

        getReceptsAnimTexture()

        padLefDown.flip(false, true)
        padLeftUp.flip(false, true)
        padCenter.flip(false, true)
        padRightUp.flip(false, true)
        padRightDown.flip(false, true)

        if (isVideo) {
            videoViewBgaOn.start()
        } else {
            videoViewBgaoff.start()
            videoViewBgaoff.setOnCompletionListener {
                videoViewBgaoff.start()
            }
        }
        mediaPlayer.start()
    }

    override fun render(delta: Float) {
        ScreenUtils.clear(0f, 0f, 0f, 0f)
        camera.update()
        batch.projectionMatrix = camera.combined
        if (!isPaused) {
            val currentTime = (elapsedTime * 1000).toLong() + latency
            batch.begin()
            elapsedTime += delta

            if(!playerSong.fd){
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

    private fun showBgPads(){
        batch.draw(padLefDown, padPositions[0][0], padPositions[0][1], widthBtns, heightBtns)
        batch.draw(padLeftUp, padPositions[1][0], padPositions[1][1], widthBtns, heightBtns)
        batch.draw(padCenter, padPositions[2][0], padPositions[2][1], widthBtns, heightBtns)
        batch.draw(padRightUp, padPositions[3][0], padPositions[3][1], widthBtns, heightBtns)
        batch.draw(padRightDown, padPositions[4][0], padPositions[4][1], widthBtns, heightBtns)
    }

    private fun getReceptsAnimTexture() {
        val recept0Frames = getReceptsTexture(textureLD)
        val recept1Frames = getReceptsTexture(textureLU)
        val recept2Frames = getReceptsTexture(textureCE)
        val recept3Frames = getReceptsTexture(textureLU, true)
        val recept4Frames = getReceptsTexture(textureLD, true)

        val frameDurationRecepts = rithymAnim / 2

        animationReceptLeftDown = Animation(frameDurationRecepts, *recept0Frames)
        animationReceptLeftUp = Animation(frameDurationRecepts, *recept1Frames)
        animationReceptCenter = Animation(frameDurationRecepts, *recept2Frames)
        animationReceptRightUp = Animation(frameDurationRecepts, *recept3Frames)
        animationReceptRightDown = Animation(frameDurationRecepts, *recept4Frames)
    }

    private fun getReceptsAnimation() {
        batch.draw(animationReceptLeftDown.getKeyFrame(elapsedTime, true), medidaFlechas, targetTop, medidaFlechas, medidaFlechas)
        batch.draw(animationReceptLeftUp.getKeyFrame(elapsedTime, true), medidaFlechas * 2, targetTop, medidaFlechas, medidaFlechas)
        batch.draw(animationReceptCenter.getKeyFrame(elapsedTime, true), medidaFlechas * 3, targetTop, medidaFlechas, medidaFlechas)
        batch.draw(animationReceptRightUp.getKeyFrame(elapsedTime, true), medidaFlechas * 4, targetTop, medidaFlechas, medidaFlechas)
        batch.draw(animationReceptRightDown.getKeyFrame(elapsedTime, true), medidaFlechas * 5, targetTop, medidaFlechas, medidaFlechas)
    }

    private fun getReceptsTexture(arrow: Texture, isMirror: Boolean = false) : Array<TextureRegion> {
        val tmp = TextureRegion.split(arrow, arrow.width, arrow.height / 3)

        return if(!isMirror) {
            val frames = arrayOf(tmp[0][0], tmp[1][0])
            frames[0].flip(false, true)
            frames[1].flip(false, true)
            frames
        }else{
            val frames = arrayOf(tmp[0][0], tmp[1][0])
            frames[0].flip(true, true)
            frames[1].flip(true, true)
            frames
        }
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
    }
}