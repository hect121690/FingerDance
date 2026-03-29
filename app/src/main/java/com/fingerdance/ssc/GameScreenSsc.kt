package com.fingerdance

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.Screen
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.fingerdance.ssc.*
import ssc.RenderSteps
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

    // Lua visual
    private val luaVisual = LuaVisualEngine()

    // Textures (notes)
    private lateinit var mine: Texture
    private lateinit var downLeftTap: Texture
    private lateinit var upLeftTap: Texture
    private lateinit var centerTap: Texture
    private lateinit var downLeftBody: Texture
    private lateinit var upLeftBody: Texture
    private lateinit var centerBody: Texture
    private lateinit var downLeftBottom: Texture
    private lateinit var upLeftBottom: Texture
    private lateinit var centerBottom: Texture

    private lateinit var arrMines: Array<TextureRegion>
    private lateinit var arrArrows: Array<Array<TextureRegion>>
    private lateinit var arrArrowsBody: Array<Array<TextureRegion>>
    private lateinit var arrArrowsBottom: Array<Array<TextureRegion>>
    private lateinit var sprFlare: Texture
    private lateinit var flareArrowFrame: Array<TextureRegion>

    // Receptors
    private val textureLD = Texture(Gdx.files.absolute("$ruta/DownLeft Ready Receptor 1x3.png"))
    private val textureLU = Texture(Gdx.files.absolute("$ruta/UpLeft Ready Receptor 1x3.png"))
    private val textureCE = Texture(Gdx.files.absolute("$ruta/Center Ready Receptor 1x3.png"))

    private val recept0Frames = getReceptsTexture(textureLD)
    private val recept1Frames = getReceptsTexture(textureLU)
    private val recept2Frames = getReceptsTexture(textureCE)
    private val recept3Frames = getReceptsTexture(textureLU, true)
    private val recept4Frames = getReceptsTexture(textureLD, true)

    private lateinit var arrRecepts: Array<Array<TextureRegion>>

    // Judgment textures
    private lateinit var arrJudgments: Array<TextureRegion>

    // Pads (como KSF)
    private lateinit var padsA: Array<TextureRegion>

    private var showPadBLocal = showPadB
    private var hideImagesPadALocal = hideImagesPadA

    // Pad B
    private var spritePadB: Sprite? = null
    private var padBRegion: TextureRegion? = null

    // Pad C
    private var padCBg: TextureRegion? = null
    private var arrPadsC: Array<Array<TextureRegion>>? = null
    private var padPositionsC: List<GameScreenKsf.PadPositionC>? = null

    // Pad D
    private var arrayPad4Bg: Array<TextureRegion>? = null
    private var arrayPad4: Array<TextureRegion>? = null

    private lateinit var camera: OrthographicCamera

    init {
        initCommonInfo()
        initPadsLikeKsf()
    }

    private fun initCommonInfo() {
        mine = Texture(Gdx.files.absolute("${File(ruta).parent}/Tap Mine 3x2.png"))
        downLeftTap = Texture(Gdx.files.absolute("$ruta/DownLeft Tap Note 3x2.png"))
        upLeftTap = Texture(Gdx.files.absolute("$ruta/UpLeft Tap Note 3x2.png"))
        centerTap = Texture(Gdx.files.absolute("$ruta/Center Tap Note 3x2.png"))

        downLeftBody = Texture(Gdx.files.absolute("$ruta/DownLeft Hold Body Active 6x1.png"))
        upLeftBody = Texture(Gdx.files.absolute("$ruta/UpLeft Hold Body Active 6x1.png"))
        centerBody = Texture(Gdx.files.absolute("$ruta/Center Hold Body Active 6x1.png"))

        downLeftBottom = Texture(Gdx.files.absolute("$ruta/DownLeft Hold BottomCap Active 6x1.png"))
        upLeftBottom = Texture(Gdx.files.absolute("$ruta/UpLeft Hold BottomCap Active 6x1.png"))
        centerBottom = Texture(Gdx.files.absolute("$ruta/Center Hold BottomCap Active 6x1.png"))

        val ldArrowFrame = getArrows3x2(downLeftTap)
        val luArrowFrame = getArrows3x2(upLeftTap)
        val ceArrowFrame = getArrows3x2(centerTap)
        val ruArrowFrame = getArrows3x2(upLeftTap, true)
        val rdArrowFrame = getArrows3x2(downLeftTap, true)

        arrArrows = arrayOf(ldArrowFrame, luArrowFrame, ceArrowFrame, ruArrowFrame, rdArrowFrame)

        val ldBodyArrowFrame = getArrows6x1(downLeftBody)
        val luBodyArrowFrame = getArrows6x1(upLeftBody)
        val ceBodyArrowFrame = getArrows6x1(centerBody)
        val ruBodyArrowFrame = getArrows6x1(upLeftBody, true)
        val rdBodyArrowFrame = getArrows6x1(downLeftBody, true)

        arrArrowsBody = arrayOf(ldBodyArrowFrame, luBodyArrowFrame, ceBodyArrowFrame, ruBodyArrowFrame, rdBodyArrowFrame)

        val ldBottomArrowFrame = getArrows6x1(downLeftBottom)
        val luBottomArrowFrame = getArrows6x1(upLeftBottom)
        val ceBottomArrowFrame = getArrows6x1(centerBottom)
        val ruBottomArrowFrame = getArrows6x1(upLeftBottom, true)
        val rdBottomArrowFrame = getArrows6x1(downLeftBottom, true)

        arrArrowsBottom = arrayOf(ldBottomArrowFrame, luBottomArrowFrame, ceBottomArrowFrame, ruBottomArrowFrame, rdBottomArrowFrame)

        sprFlare = Texture(Gdx.files.absolute("$ruta/Flare 6x1.png"))
        flareArrowFrame = getArrows6x1(sprFlare)
        arrMines = getArrows3x2(mine)

        arrRecepts = arrayOf(recept0Frames, recept1Frames, recept2Frames, recept3Frames, recept4Frames)

        // judgments
        val rutaPads = "/FingerDance/Themes/$tema/GraphicsStatics/game_play"
        val imgPerfect = TextureRegion(Texture(Gdx.files.external("$rutaPads/perfect.png")))
        val imgGreat = TextureRegion(Texture(Gdx.files.external("$rutaPads/great.png")))
        val imgGood = TextureRegion(Texture(Gdx.files.external("$rutaPads/good.png")))
        val imgBad = TextureRegion(Texture(Gdx.files.external("$rutaPads/bad.png")))
        val imgMiss = TextureRegion(Texture(Gdx.files.external("$rutaPads/miss.png")))

        arrJudgments = arrayOf(imgPerfect, imgGreat, imgGood, imgBad, imgMiss)
        arrJudgments.forEach { it.flip(false, true) }
    }

    private fun initPadsLikeKsf() {
        // Pad A (tema)
        val rutaPads = "/FingerDance/Themes/$tema/GraphicsStatics/game_play"
        val padLefDown = TextureRegion(Texture(Gdx.files.external("$rutaPads/left_down.png")))
        val padLeftUp = TextureRegion(Texture(Gdx.files.external("$rutaPads/left_up.png")))
        val padCenter = TextureRegion(Texture(Gdx.files.external("$rutaPads/center.png")))
        val padRightUp = TextureRegion(Texture(Gdx.files.external("$rutaPads/right_up.png")))
        val padRightDown = TextureRegion(Texture(Gdx.files.external("$rutaPads/right_down.png")))
        padsA = arrayOf(padLefDown, padLeftUp, padCenter, padRightUp, padRightDown)
        padsA.forEach { it.flip(false, true) }

        // Pad B / C / D (si están activos)
        if (showPadBLocal == 1) {
            padBRegion = TextureRegion(Texture(Gdx.files.external("/FingerDance/PadsB/$skinPad.png")))
            spritePadB = Sprite(padBRegion).apply { flip(false, true) }
        } else if (showPadBLocal == 2) {
            val bg = TextureRegion(Texture(Gdx.files.external("/FingerDance/PadsC/$skinPad/BG.png")))
            bg.flip(false, true)
            padCBg = bg

            val dl = getPadC(Texture(Gdx.files.external("/FingerDance/PadsC/$skinPad/DownLeft.png")))
            val ul = getPadC(Texture(Gdx.files.external("/FingerDance/PadsC/$skinPad/UpLeft.png")))
            val ce = getPadC(Texture(Gdx.files.external("/FingerDance/PadsC/$skinPad/Center.png")))
            val ur = getPadC(Texture(Gdx.files.external("/FingerDance/PadsC/$skinPad/UpRight.png")))
            val dr = getPadC(Texture(Gdx.files.external("/FingerDance/PadsC/$skinPad/DownRight.png")))
            arrPadsC = arrayOf(dl, ul, ce, ur, dr)

            // mismas posiciones que KSF
            padPositionsC = listOf(
                GameScreenKsf.PadPositionC(width.toFloat() * 0.015f, width.toFloat() * 1.61f, medidaFlechas * 3f),
                GameScreenKsf.PadPositionC(width.toFloat() * 0.015f, width.toFloat() * 1.063f, medidaFlechas * 3f),
                GameScreenKsf.PadPositionC(width.toFloat() * 0.283f, width.toFloat() * 1.334f, medidaFlechas * 3f),
                GameScreenKsf.PadPositionC(width.toFloat() * 0.558f, width.toFloat() * 1.063f, medidaFlechas * 3f),
                GameScreenKsf.PadPositionC(width.toFloat() * 0.558f, width.toFloat() * 1.61f, medidaFlechas * 3f)
            )
        } else if (showPadBLocal == 3) {
            // typePadD es global
            when (typePadD) {
                0 -> {
                    arrayPad4Bg = getTexturePad4(Texture(Gdx.files.external("/FingerDance/PadsD/arrows_pad_bg.png")))
                    arrayPad4 = getTexturePad4(Texture(Gdx.files.external("/FingerDance/PadsD/arrows_pad.png")))
                }
                1 -> {
                    arrayPad4Bg = getTexturePad4(Texture(Gdx.files.external("/FingerDance/PadsD/arrows_pad_bg_m.png")))
                    arrayPad4 = getTexturePad4(Texture(Gdx.files.external("/FingerDance/PadsD/arrows_pad_m.png")))
                }
                else -> {
                    arrayPad4Bg = getTexturePad4(Texture(Gdx.files.external("/FingerDance/PadsD/arrows_pad_bg_n.png")))
                    arrayPad4 = getTexturePad4(Texture(Gdx.files.external("/FingerDance/PadsD/arrows_pad_n.png")))
                }
            }
        }
    }

    override fun show() {
        batch = SpriteBatch()
        camera = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.setToOrtho(true)
        batch.projectionMatrix = camera.combined

        parser = Parser()
        val chart = parser.parseSSC(sscSong.listLvs[3].steps)

        timing = TimingEngine(chart.bpms, chart.stops, chart.warps)
        scroll = ScrollEngine(chart.speeds, chart.scrolls, timing)

        val music: Music = Gdx.audio.newMusic(Gdx.files.absolute(playerSong.rutaCancion))
        music.stop()
        audio = AudioEngine(music)
        audio.load(offsetMs = -50.0)

        val tex = RenderSteps.TextureSet(
            arrows = arrArrows,
            arrowsBody = arrArrowsBody,
            arrowsBottom = arrArrowsBottom,
            mines = arrMines,
            flare = flareArrowFrame,
            receptor = arrRecepts,
            judgments = arrJudgments,
            pads = padsA // ✅ AHORA SÍ hay pads A
        )

        renderer = RenderSteps(
            batch = batch,
            textures = tex,
            showPadB = showPadBLocal,
            hideImagesPadA = hideImagesPadALocal,
            spritePadB = spritePadB,
            padCBg = padCBg,
            arrPadsC = arrPadsC,
            padPositionsC = padPositionsC,
            arrayPad4Bg = arrayPad4Bg,
            arrayPad4 = arrayPad4,
            recept0Frames = recept0Frames,
            recept1Frames = recept1Frames,
            recept2Frames = recept2Frames,
            recept3Frames = recept3Frames,
            recept4Frames = recept4Frames
        )
        renderer.updateLayout(Gdx.graphics.width.toFloat())

        input = InputProcessorSsc(
            getTimeMs = { audio.currentTimeMs() }
        ) { column, isDown, _ ->
            gameLoop.onInput(column, isDown)
        }

        Gdx.input.inputProcessor = InputMultiplexer(input)

        gameLoop = GameLoop(
            chart = chart,
            audio = audio,
            timing = timing,
            scroll = scroll,
            renderer = renderer,
            input = input,
            batch = batch,
            onJudgment = { judgment, timeMs ->
                renderer.setJudgment(judgment, timeMs)
            },
            // (tu offset actual)
            timingOffsetMs = 360.0
        ) { currentBeat ->
            // hook para lua visual (si luego parseas eventos)
            luaVisual.update(currentBeat.toFloat(), emptyList())
            renderer.setLuaOffsets(luaVisual.luaReceptOffsetX, luaVisual.luaNoteOffsetX)
        }

        gameLoop.start()
    }

    override fun render(delta: Float) {
        camera.update()
        batch.projectionMatrix = camera.combined
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        gameLoop.update(Gdx.graphics.height.toFloat())
    }

    override fun resize(width: Int, height: Int) {
        camera.viewportWidth = width.toFloat()
        camera.viewportHeight = height.toFloat()
        camera.update()
        batch.projectionMatrix = camera.combined
        renderer.updateLayout(width.toFloat())
    }

    private fun getReceptsTexture(arrow: Texture, isMirror: Boolean = false): Array<TextureRegion> {
        val tmp = TextureRegion.split(arrow, arrow.width, arrow.height / 3)
        val frames = arrayOf(tmp[0][0], tmp[1][0], tmp[2][0])
        frames.forEach { it.flip(isMirror, true) }
        return frames
    }

    private fun getArrows3x2(arrow: Texture, isMirror: Boolean = false): Array<TextureRegion> {
        val tmp = TextureRegion.split(arrow, arrow.width / 3, arrow.height / 2)
        val frames = arrayOf(
            tmp[0][0], tmp[0][1], tmp[0][2],
            tmp[1][0], tmp[1][1], tmp[1][2]
        )
        frames.forEach { it.flip(isMirror, true) }
        return frames
    }

    private fun getArrows6x1(arrow: Texture, isMirror: Boolean = false): Array<TextureRegion> {
        arrow.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        val tmp = TextureRegion.split(arrow, arrow.width / 6, arrow.height)
        val frames = arrayOf(tmp[0][0], tmp[0][1], tmp[0][2], tmp[0][3], tmp[0][4], tmp[0][5])
        frames.forEach { it.flip(isMirror, true) }
        return frames
    }

    private fun getTexturePad4(texture: Texture): Array<TextureRegion> {
        val tmp = TextureRegion.split(texture, texture.width / 5, texture.height)
        val frames = arrayOf(tmp[0][0], tmp[0][1], tmp[0][2], tmp[0][3], tmp[0][4])
        frames.forEach { it.flip(false, true) }
        return frames
    }

    private fun getPadC(texture: Texture): Array<TextureRegion> {
        val tmp = TextureRegion.split(texture, texture.width, texture.height / 6)
        val frames = arrayOf(tmp[0][0], tmp[1][0], tmp[2][0], tmp[3][0], tmp[4][0], tmp[5][0])
        frames.forEach { it.flip(false, true) }
        return frames
    }

    override fun dispose() {
        batch.dispose()
    }

    override fun pause() {}
    override fun resume() {}
    override fun hide() {}
}