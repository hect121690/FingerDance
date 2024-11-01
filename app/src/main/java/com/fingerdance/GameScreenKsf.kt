package com.fingerdance

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Animation
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ScreenViewport

open class GameScreenKsf(activity: GameScreenActivity) : Screen {
    val a = activity

    private lateinit var batch: SpriteBatch
    private lateinit var stage: Stage

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
    val textureRU = Texture(Gdx.files.absolute("$ruta/UpLeft Ready Receptor 1x3.png"))
    val textureRD = Texture(Gdx.files.absolute("$ruta/DownLeft Ready Receptor 1x3.png"))

    val textureRegionLD = getTextureRegion(textureLD)
    val textureRegionLU = getTextureRegion(textureLU)
    val textureRegionCE = getTextureRegion(textureCE)
    val textureRegionRU = getTextureRegion(textureRU, true)
    val textureRegionRD = getTextureRegion(textureRD, true)

    val imageLD = getImage(textureRegionLD, 1)
    val imageLU = getImage(textureRegionLU, 2)
    val imageCE = getImage(textureRegionCE, 3)
    val imageRU = getImage(textureRegionRU, 4)
    val imageRD = getImage(textureRegionRD, 5)

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
        stage.addActor(imageLD)
        stage.addActor(imageLU)
        stage.addActor(imageCE)
        stage.addActor(imageRU)
        stage.addActor(imageRD)
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
            val currentTime = (elapsedTime * 1000).toLong()
            batch.begin()
            elapsedTime += delta
            getReceptsAnimation()
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

    fun startShrinkAnimation(area: Int) {
        val image = when (area) {
            1 -> imageLD
            2 -> imageLU
            3 -> imageCE
            4 -> imageRU
            5 -> imageRD
            else -> return
        }

        // Asegúrate de que la imagen esté lista para animarse
        image.clearActions()
        image.color.a = 0f // Opacidad inicial a 0
        image.setScale(1f) // Tamaño inicial al 100%
        image.isVisible = true

        // Definir la animación
        val scaleAndFadeAction = Actions.sequence(
            Actions.parallel(
                Actions.scaleTo(1.3f, 1.3f, 0.5f),  // Escala a 1.3 en 0.5 segundos
                Actions.fadeIn(0.1f),               // Asegurar visibilidad inmediata
                Actions.fadeOut(0.5f)               // Desvanecer en 0.5 segundos
            ),
            Actions.run {
                image.isVisible = false  // Ocultar al terminar
                image.setScale(1f)       // Reiniciar tamaño
                image.color.a = 0f       // Reiniciar opacidad
            }
        )

        image.setPosition(medidaFlechas * area, Gdx.graphics.height / 2f)
        // Ejecuta la animación
        image.addAction(scaleAndFadeAction)
    }



    private fun getImage(textureRegion: TextureRegion, position: Int) : Image {
        return Image(textureRegion).apply {
            setSize(medidaFlechas, medidaFlechas)
            isVisible = false
        }
    }
    private fun getTextureRegion(texture: Texture, isMirror: Boolean = false) : TextureRegion {
        val textureRegion = TextureRegion(texture, 0, texture.height / 3 * 2, texture.width, texture.height / 3)
        if(!isMirror){
            textureRegion.flip(false, false)
        }else{
            textureRegion.flip(true, false)
        }
        return textureRegion
    }

    private fun getReceptsAnimTexture() {
        val recept0 = Texture(Gdx.files.absolute("$ruta/DownLeft Ready Receptor 1x3.png"))
        val recept1 = Texture(Gdx.files.absolute("$ruta/UpLeft Ready Receptor 1x3.png"))
        val recept2 = Texture(Gdx.files.absolute("$ruta/Center Ready Receptor 1x3.png"))

        val recept0Frames = getReceptsTexture(recept0)
        val recept1Frames = getReceptsTexture(recept1)
        val recept2Frames = getReceptsTexture(recept2)
        val recept3Frames = getReceptsTexture(recept1, true)
        val recept4Frames = getReceptsTexture(recept0, true)

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
