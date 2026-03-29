package com.fingerdance

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.Screen
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.fingerdance.ssc.*
import java.io.File

class GameScreenSsc(
    private val activity: GameScreenActivity
) : Screen {

    private lateinit var batch: SpriteBatch

    // ENGINE
    private lateinit var parser: Parser
    private lateinit var timing: TimingEngine
    private lateinit var scroll: ScrollEngine
    private lateinit var audio: AudioEngine
    private lateinit var renderer: RenderSteps
    private lateinit var gameLoop: GameLoop
    private lateinit var input: InputProcessorSsc

    private lateinit var mine: Texture
    private lateinit var downLeftTap: Texture
    private lateinit var upLeftTap: Texture
    private lateinit var centerTap: Texture
    private lateinit var upRightTap: Texture
    private lateinit var downRightTap: Texture

    private lateinit var downLeftBody: Texture
    private lateinit var upLeftBody: Texture
    private lateinit var centerBody: Texture
    private lateinit var upRightBody: Texture
    private lateinit var downRightBody: Texture

    private lateinit var downLeftBottom: Texture
    private lateinit var upLeftBottom: Texture
    private lateinit var centerBottom: Texture
    private lateinit var upRightBottom: Texture
    private lateinit var downRightBottom: Texture

    private lateinit var arrMines : Array<TextureRegion>

    private lateinit var arrArrows : Array<Array<TextureRegion>>
    private lateinit var arrArrowsBody : Array<Array<TextureRegion>>
    private lateinit var arrArrowsBottom : Array<Array<TextureRegion>>
    private lateinit var sprFlare: Texture
    private lateinit var flareArrowFrame : Array<TextureRegion>

    private val textureLD = Texture(Gdx.files.absolute("$ruta/DownLeft Ready Receptor 1x3.png"))
    private val textureLU = Texture(Gdx.files.absolute("$ruta/UpLeft Ready Receptor 1x3.png"))
    private val textureCE = Texture(Gdx.files.absolute("$ruta/Center Ready Receptor 1x3.png"))

    private val recept0Frames = getReceptsTexture(textureLD)
    private val recept1Frames = getReceptsTexture(textureLU)
    private val recept2Frames = getReceptsTexture(textureCE)
    private val recept3Frames = getReceptsTexture(textureLU, true)
    private val recept4Frames = getReceptsTexture(textureLD, true)

    private lateinit var arrRecepts : Array<Array<TextureRegion>>

    // Judgment textures
    private lateinit var imgPerfect: TextureRegion
    private lateinit var imgGreat: TextureRegion
    private lateinit var imgGood: TextureRegion
    private lateinit var imgBad: TextureRegion
    private lateinit var imgMiss: TextureRegion
    private lateinit var arrJudgments: Array<TextureRegion>

    private lateinit var camera : OrthographicCamera
    init {
        initCommonInfo()
    }

    private fun initCommonInfo(){
         mine = Texture(Gdx.files.absolute("${File(ruta).parent}/Tap Mine 3x2.png"))
         downLeftTap = Texture(Gdx.files.absolute("$ruta/DownLeft Tap Note 3x2.png"))
         upLeftTap = Texture(Gdx.files.absolute("$ruta/UpLeft Tap Note 3x2.png"))
         centerTap = Texture(Gdx.files.absolute("$ruta/Center Tap Note 3x2.png"))
         upRightTap =  upLeftTap
         downRightTap =  downLeftTap

         downLeftBody = Texture(Gdx.files.absolute("$ruta/DownLeft Hold Body Active 6x1.png"))
         upLeftBody = Texture(Gdx.files.absolute("$ruta/UpLeft Hold Body Active 6x1.png"))
         centerBody = Texture(Gdx.files.absolute("$ruta/Center Hold Body Active 6x1.png"))
         upRightBody =  upLeftBody
         downRightBody =  downLeftBody

         downLeftBottom = Texture(Gdx.files.absolute("$ruta/DownLeft Hold BottomCap Active 6x1.png"))
         upLeftBottom = Texture(Gdx.files.absolute("$ruta/UpLeft Hold BottomCap Active 6x1.png"))
         centerBottom = Texture(Gdx.files.absolute("$ruta/Center Hold BottomCap Active 6x1.png"))
         upRightBottom =  upLeftBody
         downRightBottom =  downLeftBottom

        val ldArrowFrame = getArrows3x2( downLeftTap)
        val luArrowFrame = getArrows3x2( upLeftTap)
        val ceArrowFrame = getArrows3x2( centerTap)
        val ruArrowFrame = getArrows3x2( upLeftTap, true)
        val rdArrowFrame = getArrows3x2( downLeftTap, true)

         arrArrows = arrayOf(ldArrowFrame, luArrowFrame, ceArrowFrame, ruArrowFrame, rdArrowFrame)

        val ldBodyArrowFrame = getArrows6x1( downLeftBody)
        val luBodyArrowFrame = getArrows6x1( upLeftBody)
        val ceBodyArrowFrame = getArrows6x1( centerBody)
        val ruBodyArrowFrame = getArrows6x1( upLeftBody, true)
        val rdBodyArrowFrame = getArrows6x1( downLeftBody, true)

         arrArrowsBody = arrayOf(ldBodyArrowFrame, luBodyArrowFrame, ceBodyArrowFrame, ruBodyArrowFrame, rdBodyArrowFrame)

        val ldBottomArrowFrame = getArrows6x1( downLeftBottom)
        val luBottomArrowFrame = getArrows6x1( upLeftBottom)
        val ceBottomArrowFrame = getArrows6x1( centerBottom)
        val ruBottomArrowFrame = getArrows6x1( upLeftBottom, true)
        val rdBottomArrowFrame = getArrows6x1( downLeftBottom, true)

        arrArrowsBottom = arrayOf(ldBottomArrowFrame, luBottomArrowFrame, ceBottomArrowFrame, ruBottomArrowFrame, rdBottomArrowFrame)

        sprFlare = Texture(Gdx.files.absolute("$ruta/Flare 6x1.png"))
        flareArrowFrame = getArrows6x1(sprFlare)
        arrMines = getArrows3x2( mine)

        arrRecepts = arrayOf(recept0Frames, recept1Frames, recept2Frames, recept3Frames, recept4Frames)

        // Load judgment textures
        val rutaPads = "/FingerDance/Themes/$tema/GraphicsStatics/game_play"
        imgPerfect = TextureRegion(Texture(Gdx.files.external("$rutaPads/perfect.png")))
        imgGreat = TextureRegion(Texture(Gdx.files.external("$rutaPads/great.png")))
        imgGood = TextureRegion(Texture(Gdx.files.external("$rutaPads/good.png")))
        imgBad = TextureRegion(Texture(Gdx.files.external("$rutaPads/bad.png")))
        imgMiss = TextureRegion(Texture(Gdx.files.external("$rutaPads/miss.png")))

        arrJudgments = arrayOf(imgPerfect, imgGreat, imgGood, imgBad, imgMiss)
        arrJudgments.forEach { it.flip(false, true) }
    }

    override fun show() {

        batch = SpriteBatch()
        camera = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.setToOrtho(true)
        batch.setProjectionMatrix(camera.combined)

        // =========================
        // 1. PARSER
        // =========================

        parser = Parser()

        //val sscText = loadSSC() // 👈 tú ya sabes hacerlo
        val chart = parser.parseSSC(sscSong.listLvs[5].steps)

        // =========================
        // 2. TIMING
        // =========================

        timing = TimingEngine(
            chart.bpms,
            chart.stops,
            chart.warps
        )

        scroll = ScrollEngine(
            chart.speeds,
            chart.scrolls,
            timing
        )

        // =========================
        // 3. AUDIO
        // =========================

        val music: Music = Gdx.audio.newMusic(Gdx.files.absolute(playerSong.rutaCancion))
        music.stop()  // 🔥 ASEGURAR QUE NO SE REPRODUZCA AUTOMÁTICAMENTE
        audio = AudioEngine(music)
        audio.load(offsetMs = 0.0) // luego ajustas offset real

        // =========================
        // 4. TEXTURES
        // =========================

        val tex = RenderSteps.TextureSet(
            arrows = arrArrows,
            arrowsBody = arrArrowsBody,
            arrowsBottom = arrArrowsBottom,
            mines = arrMines,
            flare = flareArrowFrame,
            receptor = arrRecepts,
            judgments = arrJudgments
        )

        renderer = RenderSteps(
            batch = batch,
            textures = tex,
            showPadB = com.fingerdance.showPadB,
            hideImagesPadA = com.fingerdance.hideImagesPadA,
            arrPadsC = null, // No implementado en SSC por ahora
            padPositionsC = null,
            arrayPad4 = null,
            recept0Frames = recept0Frames,
            recept1Frames = recept1Frames,
            recept2Frames = recept2Frames,
            recept3Frames = recept3Frames,
            recept4Frames = recept4Frames
        )
        renderer.updateLayout(Gdx.graphics.width.toFloat())

        // =========================
        // 5. INPUT
        // =========================

        input = InputProcessorSsc(
            getTimeMs = { audio.currentTimeMs() }
        ) { column, isDown, time ->

            gameLoop.onInput(column, isDown)
        }

        Gdx.input.inputProcessor = InputMultiplexer(input)

        // =========================
        // 6. GAME LOOP
        // =========================

        gameLoop = GameLoop(
            chart = chart,
            audio = audio,
            timing = timing,
            scroll = scroll,
            renderer = renderer,
            input = input,
            batch = batch,
            onJudgment = { judgment, timeMs ->
                // Callback when judgment occurs
                renderer.setJudgment(judgment, timeMs)
            },
            timingOffsetMs = 360.0  // 🔥 OFFSET DE 360MS
        )

        gameLoop.start()
    }

    override fun render(delta: Float) {

        camera.update()
        batch.setProjectionMatrix(camera.combined)

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        gameLoop.update(Gdx.graphics.height.toFloat())
    }

    override fun resize(width: Int, height: Int) {
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
        batch.setProjectionMatrix(camera.combined)
        renderer.updateLayout(width.toFloat())
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

    private fun getArrows3x2(arrow: Texture, isMirror: Boolean = false) : Array<TextureRegion> {
        val tmp = TextureRegion.split(arrow, arrow.width / 3, arrow.height / 2)

        return if(!isMirror) {
            val frames = arrayOf(
                tmp[0][0], tmp[0][1], tmp[0][2],
                tmp[1][0], tmp[1][1], tmp[1][2]
            )
            frames[0].flip(false, true)
            frames[1].flip(false, true)
            frames[2].flip(false, true)
            frames[3].flip(false, true)
            frames[4].flip(false, true)
            frames[5].flip(false, true)
            frames
        }else{
            val frames = arrayOf(
                tmp[0][0], tmp[0][1], tmp[0][2],
                tmp[1][0], tmp[1][1], tmp[1][2]
            )
            frames[0].flip(true, true)
            frames[1].flip(true, true)
            frames[2].flip(true, true)
            frames[3].flip(true, true)
            frames[4].flip(true, true)
            frames[5].flip(true, true)
            frames
        }
    }

    private fun getArrows6x1(arrow: Texture, isMirror: Boolean = false) : Array<TextureRegion> {
        arrow.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        val tmp = TextureRegion.split(arrow, arrow.width / 6, arrow.height)

        return if(!isMirror) {
            val frames = arrayOf(
                tmp[0][0], tmp[0][1], tmp[0][2],
                tmp[0][3], tmp[0][4], tmp[0][5]
            )
            frames[0].flip(false, true)
            frames[1].flip(false, true)
            frames[2].flip(false, true)
            frames[3].flip(false, true)
            frames[4].flip(false, true)
            frames[5].flip(false, true)
            frames
        }else{
            val frames = arrayOf(
                tmp[0][0], tmp[0][1], tmp[0][2],
                tmp[0][3], tmp[0][4], tmp[0][5]
            )
            frames[0].flip(true, true)
            frames[1].flip(true, true)
            frames[2].flip(true, true)
            frames[3].flip(true, true)
            frames[4].flip(true, true)
            frames[5].flip(true, true)
            frames
        }
    }

    override fun dispose() {
        batch.dispose()
    }

    override fun pause() {}
    override fun resume() {}
    override fun hide() {}

    // =========================
    // HELPER
    // =========================

    private fun loadSSC(): String {
        // 👉 aquí usas tu lógica actual
        // ejemplo:
        return Gdx.files.absolute("").readString()
    }
}