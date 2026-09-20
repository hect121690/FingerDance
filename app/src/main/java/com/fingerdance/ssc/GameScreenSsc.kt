package com.fingerdance.ssc

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.utils.ScreenUtils
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.fingerdance.GameScreenActivity
import com.fingerdance.aBatch
import com.fingerdance.alphaPadB
import com.fingerdance.bBatch
import com.fingerdance.chart
import com.fingerdance.durationSong
import com.fingerdance.endingFadeAlpha
import com.fingerdance.height
import com.fingerdance.heightBtns
import com.fingerdance.hideImagesPadA
import com.fingerdance.isVertical
import com.fingerdance.halfDouble
import com.fingerdance.isEndingFade
import com.fingerdance.loadTexture
import com.fingerdance.luaRecepts
import com.fingerdance.medidaFlechas
import com.fingerdance.padPositions
import com.fingerdance.playerSong
import com.fingerdance.ruta
import com.fingerdance.showPadB
import com.fingerdance.skinPad
import com.fingerdance.ssc.attacks.AttackFeatureFlags
import com.fingerdance.ssc.attacks.AttackEffects
import com.fingerdance.ssc.attacks.AttackEngine
import com.fingerdance.ssc.attacks.AttackState
import com.fingerdance.ssc.attacks.FieldMetrics
import com.fingerdance.tema
import com.fingerdance.typePadD
import com.fingerdance.width
import com.fingerdance.widthBtns
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

    /**
     * Espacio lógico de ArrowEffects para el modo visual actual.
     * Vertical usa el lado lógico de P1 (320x480).
     * Horizontal usa 640x480 dentro del 80% del ancho real.
     */
    private fun getStepManiaMetrics(): FieldMetrics =
        FieldMetrics.create(
            isVertical = isVertical,
            halfDouble = halfDouble,
            screenWidth = Gdx.graphics.width.toFloat(),
            screenHeight = Gdx.graphics.height.toFloat(),
            arrowSizePx = medidaFlechas
        )

    fun getAttackStepManiaFieldScaleY(): Float =
        getStepManiaMetrics().fieldScaleY

    fun getAttackStepManiaArrowScale(): Float =
        getStepManiaMetrics().arrowScale

    /**
     * Reconstruye el fYOffset equivalente de StepMania.
     * El yOffset que llega desde SscGameplayEngine ya incluye baseSpeed;
     * StepMania aplica Boost antes de m_fScrollSpeed, por eso primero lo quitamos.
     * El valor devuelto YA incluye Boost/Brake/Wave/Boomerang/Expand/baseSpeed, pero NO Reverse/Tipsy.
     */
    fun getAttackStepManiaYOffset(
        rawPixelYOffset: Float,
        baseScrollSpeed: Float
    ): Float {

        val metrics =
            getStepManiaMetrics()

        val safeBaseSpeed =
            baseScrollSpeed.coerceAtLeast(0.0001f)

        // SscGameplayEngine ya posicionó la nota usando la velocidad elegida por
        // el jugador. La quitamos primero para reconstruir el fYOffset lógico.
        val preSpeedPixelOffset =
            rawPixelYOffset / safeBaseSpeed

        val preSpeedSmOffset =
            metrics.toStepManiaY(
                preSpeedPixelOffset
            )

        val boostAmount =
            if (AttackFeatureFlags.AccelScroll.BOOST) {
                currentAttackState.boost *
                        attackEndResetFactor
            } else {
                0f
            }

        val brakeAmount =
            if (AttackFeatureFlags.AccelScroll.BRAKE) {
                currentAttackState.brake *
                        attackEndResetFactor
            } else {
                0f
            }

        val waveAmount =
            if (AttackFeatureFlags.AccelScroll.WAVE) {
                currentAttackState.wave *
                        attackEndResetFactor
            } else {
                0f
            }

        val boomerangAmount =
            if (AttackFeatureFlags.AccelScroll.BOOMERANG) {
                currentAttackState.boomerang *
                        attackEndResetFactor
            } else {
                0f
            }

        val expandAmount =
            if (AttackFeatureFlags.AccelScroll.EXPAND) {
                currentAttackState.expand *
                        AttackFeatureFlags.AccelScroll.EXPAND_INTENSITY *
                        attackEndResetFactor
            } else {
                0f
            }

        // StepMania usa m_fScrollSpeed DESPUÉS de
        // Boost/Brake/Wave/Boomerang.
        val effectiveScrollSpeed =
            if (AttackFeatureFlags.Speed.XMOD) {
                safeBaseSpeed +
                        (
                                currentAttackState.xmod -
                                        safeBaseSpeed
                                ) *
                        attackEndResetFactor
            } else {
                safeBaseSpeed
            }

        val effectHeightSm =
            FieldMetrics.SM_HEIGHT +
                    kotlin.math.abs(
                        currentAttackState.perspectiveTilt *
                                attackEndResetFactor
                    ) * 200f

        // -------------------------------------------------------------------------
        // EARTHWORM
        // -------------------------------------------------------------------------

        /*
         * Vía 1:
         * ATTACK del SSC:
         *
         * MODS=Earthworm
         */
        val attackEarthwormAmount =
            if (AttackFeatureFlags.AccelScroll.EARTHWORM) {
                currentAttackState.earthworm * attackEndResetFactor
            } else {
                0f
            }

        val earthwormAmount =
            if (playerSong.isEw) {
                1f
            } else {
                attackEarthwormAmount
            }

        // -------------------------------------------------------------------------
        // ACCEL / SPEED
        // -------------------------------------------------------------------------

        var transformedYOffset =
            AttackEffects.transformAccelYOffsetSm(
                yOffsetSm = preSpeedSmOffset,
                effectHeightSm = effectHeightSm,
                expandSeconds = currentAttackSongTimeSeconds,
                boostAmount = boostAmount,
                brakeAmount = brakeAmount,
                waveAmount = waveAmount,
                boomerangAmount = boomerangAmount,
                expandAmount = expandAmount,
                baseScrollSpeed = effectiveScrollSpeed.coerceAtLeast(0.0001f)
            )

        // -------------------------------------------------------------------------
        // EARTHWORM
        // -------------------------------------------------------------------------

        if (earthwormAmount != 0f) {

            transformedYOffset =
                AttackEffects.earthwormY(
                    yOffsetSm =
                        transformedYOffset,
                    songTimeSeconds =
                        currentAttackSongTimeSeconds,
                    amount =
                        earthwormAmount
                )
        }

        return transformedYOffset
    }
    private var elapsedTime = 0f
    private var rithymAnim = 0f

    private var isPaused = false
    lateinit var camera : OrthographicCamera
    lateinit var player: PlayerSsc
    private val posYpadB = height.toFloat() - (width.toFloat() * 1.1f)

    private var timer = 0f
    private var showOverlay = false
    private var intervalOverlay = 0f

    // Actívala para probar el playfield deformado con FrameBuffer + Mesh.
    private val attackPerspectiveActive: Boolean
        get() =
            AttackFeatureFlags.Perspective.ENABLED &&
                    (
                            abs(currentAttackState.skew * attackEndResetFactor) > 0.0001f ||
                                    abs(currentAttackState.perspectiveTilt * attackEndResetFactor) > 0.0001f
                            )

    val applyMesh: Boolean
        get() =
            nxProgress > 0f ||
                    attackPerspectiveActive


    private val baseNX = playerSong.nx

    var nxProgress = if (baseNX) 1f else 0f
        private set

    private var nxStartProgress = nxProgress
    private var nxTargetProgress = nxProgress

    private var nxTransitionStartBeat = 0.0
    private var nxTransitionBeats = 0.0

    private var nxTransitioning = false

    private var nxEffectDuration = 0.0
    private var nxDurationUnit = 0

    private var nxEffectStartBeat = 0.0
    private var nxEffectStartMs = 0.0

    private var nxWaitingReturn = false
    private var nxReturningToBase = false
    private var currentSongTimeMs = 0.0

    private lateinit var perspectiveRenderer: PerspectivePlayfieldRenderer

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

    val attackEngine = AttackEngine(chart.attacks)
    var currentAttackState = AttackState()
        private set
    var currentAttackSongTimeSeconds = 0f
        private set

    // =========================================================
    // FINAL SUAVE DE ATTACKS
    // =========================================================

    private companion object {
        const val ATTACK_END_RESET_DURATION = 1f
    }

    private var attackEndResetActive = false
    private var attackEndResetElapsed = 0f

    /**
     * 1f = estado ATTACK completo.
     * 0f = todos los ATTACKS visuales regresaron a neutral.
     */
    private var attackEndResetFactor = 1f

    /**
     * Evita iniciar varias veces el reset cuando songTimeMs permanece
     * en durationSong después de que MediaPlayer termina.
     */
    private var attackEndResetFinished = false

    override fun show() {
        batch = SpriteBatch()
        font = BitmapFont()
        font.color = Color.WHITE
        font.data.setScale(2f, -2f)
        stage = Stage(ScreenViewport())
        camera = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.setToOrtho(true)

        perspectiveRenderer =
            PerspectivePlayfieldRenderer(
                width = Gdx.graphics.width,
                height = Gdx.graphics.height,
                pivotX = medidaFlechas * 3.5f
            ).apply {

                topScaleX = 0.42f

                topShiftXPercent = 0.00f

                horizontalPower = 1.30f

                verticalPower = 1.80f

                meshOffsetY =
                    Gdx.graphics.height * 0.08f
            }

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
            currentSongTimeMs = songTimeMs

            /*
             * Mientras no haya terminado la canción, AttackEngine trabaja normal.
             *
             * Al alcanzar durationSong congelamos currentAttackState exactamente
             * en el último valor que tenía y hacemos que todos los efectos visuales
             * regresen a neutral durante 1 segundo.
             */
            if (!attackEndResetActive && !attackEndResetFinished) {
                if (durationSong > 0L && songTimeMs >= durationSong.toDouble()) {
                    beginAttackEndReset()
                } else {
                    updateAttacks(songTimeMs, delta)
                }
            }

            updateAttackEndReset(delta)

            elapsedTime += delta

            if (!applyMesh) {
                // Render original: se conserva igual, solamente sin la barra de progreso/burbujas.
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
                drawSongTime(songTimeMs)
                drawEndingFade(delta)
                batch.end()
            } else {
                batch.begin()
                showBgPads()
                player.updateStepData(songTimeMs)
                player.renderMeshInput()
                batch.end()

                if (!playerSong.fd) {
                    intervalOverlay = (60 / abs(player.m_fCurBPM)) / 2f
                    timer += delta
                    if (timer >= intervalOverlay) {
                        timer -= intervalOverlay
                        showOverlay = !showOverlay
                    }
                }

                /*
                 * 2) Sólo playfield -> FrameBuffer.
                 *    Receptores + TAP/MINE/HOLD + expand + flares.
                 */
                perspectiveRenderer.begin()
                batch.projectionMatrix = camera.combined
                batch.begin()

                if (!playerSong.fd) {
                    drawRecepts()
                }

                player.renderMeshPlayfield(songTimeMs)

                batch.end()
                perspectiveRenderer.end()

                /* 3) Deformamos el FrameBuffer completo con el Mesh. */
                perspectiveRenderer.progress = nxProgress
                perspectiveRenderer.attackSkew =
                    if (AttackFeatureFlags.Perspective.ENABLED) {
                        currentAttackState.skew * attackEndResetFactor
                    } else {
                        0f
                    }

                perspectiveRenderer.attackTilt =
                    if (AttackFeatureFlags.Perspective.ENABLED) {
                        currentAttackState.perspectiveTilt * attackEndResetFactor
                    } else {
                        0f
                    }
                perspectiveRenderer.draw(camera.combined)

                /*
                 * 4) HUD normal, sin Mesh: life bar, flashes, chibis, judge y combo.
                 */
                batch.projectionMatrix = camera.combined
                batch.begin()
                player.renderMeshHud()

                barBlack.setSize(maxWidth, maxlHeight)
                barBlack.setPosition(medidaFlechas, 0f)
                barRed.setSize(maxWidth, maxlHeight)
                barRed.setPosition(medidaFlechas, 0f)
                drawSongTime(songTimeMs)
                drawEndingFade(delta)
                batch.end()
            }

            stage.act(delta)
        }

        stage.draw()
    }

    private fun drawSongTime(songTimeMs: Double) {
        val totalSeconds = (songTimeMs / 1000.0).toInt().coerceAtLeast(0)
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val text = String.format("%02d:%02d", minutes, seconds)

        val layout = GlyphLayout(font, text)

        val x = Gdx.graphics.width - layout.width - 50f
        val y = 80f

        font.draw(batch, text, x, y)
    }

    fun getAttackMiniScale(): Float {
        if (!AttackFeatureFlags.Scale.MINI) return 1f

        return AttackEffects.miniScale(
            currentAttackState.mini * attackEndResetFactor
        )
    }

    fun getAttackTinyScale(): Float {
        if (!AttackFeatureFlags.Scale.TINY) return 1f

        return AttackEffects.tinyScale(
            currentAttackState.tiny * attackEndResetFactor
        )
    }

    fun getAttackVisualScale(): Float =
        getAttackMiniScale() * getAttackTinyScale()

    /**
     * Finger Dance adaptation for vertical screens.
     *
     * StepMania allows X modifiers to push notes outside the normal notefield.
     * We keep that behavior, but on a vertical phone we prevent the VISIBLE
     * portion of the note/receptor from leaving the physical screen.
     *
     * logicalX is the visible-left logical anchor used by NoteCellMetrics.
     * The calculation below also accounts for Mini because SpriteBatch scales
     * around the texture center, not around the visible pixels.
     */
    private fun clampAttackLogicalX(
        column: Int,
        logicalX: Float,
        visualScale: Float
    ): Float {
        if (!isVertical || column !in receptorMetrics.indices) return logicalX

        val metrics = receptorMetrics[column]
        val drawWidth = metrics.drawWidth(medidaFlechas)
        val drawX = metrics.drawX(logicalX, medidaFlechas)
        val originX = drawX + drawWidth * 0.5f

        val visibleLeftUnscaled = logicalX
        val visibleRightUnscaled = logicalX + medidaFlechas

        val visibleLeft =
            originX + (visibleLeftUnscaled - originX) * visualScale
        val visibleRight =
            originX + (visibleRightUnscaled - originX) * visualScale

        val minVisibleX = minOf(visibleLeft, visibleRight)
        val maxVisibleX = maxOf(visibleLeft, visibleRight)
        val screenWidth = Gdx.graphics.width.toFloat()

        return when {
            minVisibleX < 0f -> logicalX - minVisibleX
            maxVisibleX > screenWidth -> logicalX - (maxVisibleX - screenWidth)
            else -> logicalX
        }
    }

    fun getAttackReversePercentForColumn(column: Int): Float {
        val reverse =
            if (AttackFeatureFlags.DirectionColumn.REVERSE)
                currentAttackState.reverse * attackEndResetFactor
            else 0f

        val split =
            if (AttackFeatureFlags.DirectionColumn.SPLIT)
                currentAttackState.split * attackEndResetFactor
            else 0f

        val alternate =
            if (AttackFeatureFlags.DirectionColumn.ALTERNATE)
                currentAttackState.alternate * attackEndResetFactor
            else 0f

        val cross =
            if (AttackFeatureFlags.DirectionColumn.CROSS)
                currentAttackState.cross * attackEndResetFactor
            else 0f

        return AttackEffects.reversePercentForColumn(
            column = column,
            columnCount = 5,
            reverse = reverse,
            split = split,
            alternate = alternate,
            cross = cross
        )
    }

    private fun getAttackCenteredAmount(): Float =
        if (AttackFeatureFlags.DirectionColumn.CENTERED) {
            currentAttackState.centered * attackEndResetFactor
        } else {
            0f
        }

    fun getAttackReceptorY(column: Int): Float {
        val reverseAmount = getAttackReversePercentForColumn(column)
        val centeredAmount = getAttackCenteredAmount()
        val reverseY = AttackEffects.reverseReceptorY(
            screenHeight = Gdx.graphics.height.toFloat(),
            arrowSize = medidaFlechas
        )

        var finalY = AttackEffects.directionY(
            y = targetTop,
            normalReceptorY = targetTop,
            reverseReceptorY = reverseY,
            reverseAmount = reverseAmount,
            centeredAmount = centeredAmount
        )

        val tipsyAmount =
            if (AttackFeatureFlags.Position.TIPSY) {
                currentAttackState.tipsy * attackEndResetFactor
            } else {
                0f
            }

        if (tipsyAmount != 0f) {
            finalY += AttackEffects.tipsyY(
                column = column,
                songTimeSeconds = currentAttackSongTimeSeconds,
                arrowSize = medidaFlechas,
                amount = tipsyAmount
            )
        }

        return finalY
    }

    fun getAttackNoteY(
        column: Int,
        y: Float,
        baseScrollSpeed: Float
    ): Float {

        val metrics = getStepManiaMetrics()

        val rawPixelYOffset =
            y - targetTop

        val smYOffset =
            getAttackStepManiaYOffset(
                rawPixelYOffset = rawPixelYOffset,
                baseScrollSpeed = baseScrollSpeed
            )

        val reverseAmount =
            getAttackReversePercentForColumn(column)
                .coerceIn(0f, 1f)

        val centeredAmount =
            getAttackCenteredAmount()

        val reverseReceptorY =
            AttackEffects.reverseReceptorY(
                screenHeight = Gdx.graphics.height.toFloat(),
                arrowSize = medidaFlechas
            )

        /*
         * Finger Dance no lleva Reverse hasta el extremo inferior.
         * Nuestro recorrido Reverse disponible es aproximadamente media pantalla.
         */
        val normalTravelDistance =
            (Gdx.graphics.height.toFloat() - targetTop)
                .coerceAtLeast(1f)

        val reverseTravelDistance =
            reverseReceptorY
                .coerceAtLeast(1f)

        val fullReverseDistanceScale =
            (reverseTravelDistance / normalTravelDistance)
                .coerceAtLeast(0.0001f)

        val distanceScale =
            1f +
                    (fullReverseDistanceScale - 1f) *
                    reverseAmount

        var finalY =
            targetTop +
                    metrics.toPixelsY(smYOffset) *
                    distanceScale

        finalY =
            AttackEffects.directionY(
                y = finalY,
                normalReceptorY = targetTop,
                reverseReceptorY = reverseReceptorY,
                reverseAmount = reverseAmount,
                centeredAmount = centeredAmount
            )

        /*
         * IMPORTANTE:
         *
         * reverseReceptorY es el TOP desde donde dibujamos el receptor:
         *
         *     height/2 - medidaFlechas
         *
         * pero nuestro cero lógico Reverse debe quedar en:
         *
         *     height/2
         *
         * Por eso falta exactamente una medidaFlechas.
         */
        finalY +=
            medidaFlechas * reverseAmount

        val tipsyAmount =
            if (AttackFeatureFlags.Position.TIPSY) {
                currentAttackState.tipsy *
                        attackEndResetFactor
            } else {
                0f
            }

        if (tipsyAmount != 0f) {
            finalY += AttackEffects.tipsyY(
                column = column,
                songTimeSeconds = currentAttackSongTimeSeconds,
                arrowSize = medidaFlechas,
                amount = tipsyAmount
            )
        }

        val moveZScale =
            getAttackMoveZScale(column)

        if (moveZScale != 1f) {
            /*
             * OJO:
             * Para las NOTAS ahora el pivote lógico Reverse también necesita
             * la misma compensación.
             */
            val receptorY =
                getAttackReceptorY(column) +
                        medidaFlechas * reverseAmount

            finalY =
                receptorY +
                        (finalY - receptorY) *
                        moveZScale
        }

        return finalY
    }

    private fun updateAttacks(songTimeMs: Double, delta: Float) {
        currentAttackState = attackEngine.update(
            songTimeMs = songTimeMs,
            deltaSeconds = delta,
            baseScrollSpeed = player.baseSpeed
        )
        currentAttackSongTimeSeconds = (songTimeMs / 1000.0).toFloat()
    }

    private fun beginAttackEndReset() {
        if (attackEndResetActive || attackEndResetFinished) {
            return
        }

        attackEndResetActive = true
        attackEndResetElapsed = 0f
        attackEndResetFactor = 1f
    }

    private fun updateAttackEndReset(delta: Float) {
        if (!attackEndResetActive) {
            return
        }

        attackEndResetElapsed += delta

        val t =
            (attackEndResetElapsed / ATTACK_END_RESET_DURATION)
                .coerceIn(0f, 1f)

        attackEndResetFactor =
            1f - t

        if (t >= 1f) {
            attackEndResetFactor = 0f
            attackEndResetActive = false
            attackEndResetFinished = true

            onAttackEndResetFinished()
        }
    }

    /**
     * Aquí coloca la lógica que YA usas para abandonar gameplay:
     * readyToResult, cambio de Activity/Screen, DanceGrade, etc.
     *
     * Se ejecuta una sola vez y únicamente después de que el playfield
     * terminó de regresar a su posición normal.
     */
    private fun onAttackEndResetFinished() {
        // Ejemplo:
        // a.goToDanceGrade()
    }

    fun getAttackColumnOffsetX(
        column: Int,
        yOffset: Float,
        baseScrollSpeed: Float = 1f
    ): Float {
        var offsetX = 0f
        val metrics = getStepManiaMetrics()

        // GetXPos de StepMania recibe el fYOffset ya procesado por
        // Boost/Expand/scroll speed, pero todavía sin Reverse/Tipsy.
        val smYOffset =
            getAttackStepManiaYOffset(
                rawPixelYOffset = yOffset,
                baseScrollSpeed = baseScrollSpeed
            )

        if (AttackFeatureFlags.Position.TORNADO && currentAttackState.tornado != 0f) {
            offsetX += AttackEffects.tornadoX(
                column = column,
                columnCount = 5,
                yOffset = smYOffset,
                arrowSize = medidaFlechas,
                screenHeight = FieldMetrics.SM_HEIGHT,
                amount = currentAttackState.tornado * attackEndResetFactor
            )
        }

        if (AttackFeatureFlags.Position.DRUNK && currentAttackState.drunk != 0f) {
            offsetX += AttackEffects.drunkX(
                column = column,
                yOffset = smYOffset,
                songTimeSeconds = currentAttackSongTimeSeconds,
                arrowSize = medidaFlechas,
                screenHeight = FieldMetrics.SM_HEIGHT,
                amount = currentAttackState.drunk * attackEndResetFactor
            )
        }

        if (AttackFeatureFlags.DirectionColumn.FLIP && currentAttackState.flip != 0f) {
            offsetX += AttackEffects.flipX(
                column = column,
                columnCount = 5,
                arrowSize = medidaFlechas,
                amount = currentAttackState.flip * attackEndResetFactor
            )
        }

        if (AttackFeatureFlags.DirectionColumn.INVERT && currentAttackState.invert != 0f) {
            offsetX += AttackEffects.invertX(
                column = column,
                columnCount = 5,
                arrowSize = medidaFlechas,
                amount = currentAttackState.invert * attackEndResetFactor
            )
        }

        if (AttackFeatureFlags.Position.BEAT && currentAttackState.beat != 0f) {
            // Beat devuelve unidades lógicas StepMania (factor base 20).
            offsetX +=
                AttackEffects.beatX(
                    yOffset = smYOffset,
                    currentBeat = player.beatToShow,
                    amount = currentAttackState.beat * attackEndResetFactor
                ) * metrics.arrowScale
        }

        if (AttackFeatureFlags.Scale.MINI && currentAttackState.mini != 0f) {
            val baseX = medidaFlechas * (column + 1)
            val currentX = baseX + offsetX
            val centerX = medidaFlechas * 3f

            val miniX = AttackEffects.miniX(
                x = currentX,
                centerX = centerX,
                amount = currentAttackState.mini * attackEndResetFactor
            )

            offsetX = miniX - baseX
        }

        if (AttackFeatureFlags.Scale.TINY && currentAttackState.tiny != 0f) {
            val baseX = medidaFlechas * (column + 1)
            val currentX = baseX + offsetX
            val centerX = medidaFlechas * 3f

            val tinyX = AttackEffects.tinyX(
                x = currentX,
                centerX = centerX,
                amount = currentAttackState.tiny * attackEndResetFactor
            )

            offsetX = tinyX - baseX
        }

        val moveZScale = getAttackMoveZScale(column)
        if (moveZScale != 1f) {
            val baseX = medidaFlechas * (column + 1)
            val currentX = baseX + offsetX
            val fieldCenterX = medidaFlechas * 3.5f
            val projectedX = fieldCenterX + (currentX - fieldCenterX) * moveZScale
            offsetX = projectedX - baseX
        }

        if (isVertical) {
            val baseX = medidaFlechas * (column + 1)
            val unclampedX = baseX + offsetX
            val clampedX = clampAttackLogicalX(
                column = column,
                logicalX = unclampedX,
                visualScale = getAttackVisualScale()
            )
            offsetX = clampedX - baseX
        }

        return offsetX
    }

    fun getAttackConfusionRotation(currentBeat: Double): Float {
        if (!AttackFeatureFlags.Rotation3D.CONFUSION) return 0f

        val amount =
            currentAttackState.confusion * attackEndResetFactor

        if (amount == 0f) return 0f

        return AttackEffects.confusionRotation(
            currentBeat = currentBeat,
            amount = amount
        )
    }

    fun getAttackNoteRotation(noteBeat: Double, currentBeat: Double): Float {
        if (!AttackFeatureFlags.Rotation3D.DIZZY) return 0f

        val amount =
            currentAttackState.dizzy * attackEndResetFactor

        if (amount == 0f) return 0f

        return AttackEffects.dizzyRotation(
            noteBeat = noteBeat,
            currentBeat = currentBeat,
            amount = amount
        )
    }

    fun setNXFromLua(
        active: Boolean,
        transitionBeats: Double,
        effectDuration: Double,
        durationUnit: Int,
        startBeat: Double
    ) {
        nxStartProgress = nxProgress
        nxTargetProgress = if (active) 1f else 0f

        nxTransitionStartBeat = startBeat
        nxTransitionBeats = transitionBeats.coerceAtLeast(0.0)

        nxEffectDuration = effectDuration.coerceAtLeast(0.0)
        nxDurationUnit = durationUnit.coerceIn(0, 1)

        nxWaitingReturn = false
        nxReturningToBase = false

        if (nxTransitionBeats <= 0.0) {
            nxProgress = nxTargetProgress
            nxTransitioning = false
            beginNxHold(startBeat, currentSongTimeMs)
        } else {
            nxTransitioning = true
        }
    }

    fun updateNXEffect(currentBeat: Double, songTimeMs: Double) {
        if (nxTransitioning) {
            val elapsedBeats = currentBeat - nxTransitionStartBeat

            val t = if (nxTransitionBeats <= 0.0) {
                1f
            } else {
                (elapsedBeats / nxTransitionBeats)
                    .toFloat()
                    .coerceIn(0f, 1f)
            }

            nxProgress =
                nxStartProgress +
                        (nxTargetProgress - nxStartProgress) * t

            if (t >= 1f) {
                nxProgress = nxTargetProgress
                nxTransitioning = false

                if (nxReturningToBase) {
                    nxReturningToBase = false
                    nxWaitingReturn = false
                } else {
                    beginNxHold(
                        currentBeat = currentBeat,
                        songTimeMs = songTimeMs
                    )
                }
            }
        }

        if (nxWaitingReturn && !nxTransitioning) {
            val finished = when (nxDurationUnit) {
                1 -> songTimeMs - nxEffectStartMs >= nxEffectDuration
                else -> currentBeat - nxEffectStartBeat >= nxEffectDuration
            }

            if (finished) {
                returnNxToBase(currentBeat)
            }
        }
    }

    private fun beginNxHold(currentBeat: Double, songTimeMs: Double = 0.0) {
        if (nxEffectDuration <= 0.0) {
            nxWaitingReturn = false
            return
        }

        nxEffectStartBeat = currentBeat
        nxEffectStartMs = songTimeMs
        nxWaitingReturn = true
    }

    fun getAttackReverseScaleY(column: Int): Float {
        val reverse =
            getAttackReversePercentForColumn(column)
                .coerceIn(0f, 1f)

        return 1f - (2f * reverse)
    }

    private fun returnNxToBase(currentBeat: Double) {
        nxWaitingReturn = false
        nxReturningToBase = true

        nxStartProgress = nxProgress
        nxTargetProgress = if (baseNX) 1f else 0f

        nxTransitionStartBeat = currentBeat

        if (nxTransitionBeats <= 0.0) {
            nxProgress = nxTargetProgress
            nxTransitioning = false
            nxReturningToBase = false
        } else {
            nxTransitioning = true
        }
    }

    private fun drawEndingFade(delta: Float) {
        if (!isEndingFade) return

        endingFadeAlpha += delta * 1.8f
        if (endingFadeAlpha > 1f) {
            endingFadeAlpha = 1f
        }

        batch.setColor(0f, 0f, 0f, endingFadeAlpha)
        batch.draw(fadeTexture, 0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        batch.setColor(1f, 1f, 1f, 1f)
    }

    override fun resize(width: Int, height: Int) {
        camera.setToOrtho(true, width.toFloat(), height.toFloat())
        camera.update()
        if (::perspectiveRenderer.isInitialized) {
            perspectiveRenderer.resize(width, height, medidaFlechas * 3.5f)
        }
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


    fun getAttackDarkAlpha(): Float {
        if (!AttackFeatureFlags.Visibility.DARK) return 1f

        val darkAmount =
            currentAttackState.dark * attackEndResetFactor

        return (1f - darkAmount)
            .coerceIn(0f, 1f)
    }

    /**
     * Proyección 2D segura para MoveZ por columna.
     *
     * StepMania mueve la columna en Z real. Aquí proyectamos ese Z a una escala
     * perspectiva sin activar el renderer 3D experimental: TAP/MINE/HOLD siguen
     * siendo geometría 2D rígida y no se curvan.
     */
    fun getAttackMoveZAmount(column: Int): Float {
        if (!AttackFeatureFlags.Position.MOVE_Z) return 0f

        return currentAttackState.getMoveZ(column) * attackEndResetFactor
    }

    fun getAttackBumpyAmount(): Float {
        if (!AttackFeatureFlags.Rotation3D.BUMPY) return 0f
        return currentAttackState.bumpy * attackEndResetFactor
    }

    fun getAttackTwirlAmount(): Float {
        if (!AttackFeatureFlags.Rotation3D.TWIRL) return 0f
        return currentAttackState.twirl * attackEndResetFactor
    }

    fun getAttackRollAmount(): Float {
        if (!AttackFeatureFlags.Rotation3D.ROLL) return 0f
        return currentAttackState.roll * attackEndResetFactor
    }

    fun getAttackReceptorCenterY(column: Int): Float {
        return getAttackReceptorY(column) + (medidaFlechas * 0.5f)
    }

    fun getAttackMoveZScale(column: Int): Float {
        val amount = getAttackMoveZAmount(column)
        if (kotlin.math.abs(amount) < 0.0001f) return 1f

        // MoveZ 100% ~= una ArrowSize de profundidad.
        val zPixels = amount * medidaFlechas
        val focal = (Gdx.graphics.height.toFloat() * 0.75f).coerceAtLeast(medidaFlechas * 4f)
        val denominator = (focal - zPixels).coerceAtLeast(focal * 0.25f)

        return (focal / denominator).coerceIn(0.60f, 1.60f)
    }

    /**
     * STEALTH:
     * 0f -> notas completamente visibles.
     * 1f -> TAP/MINE/HOLD completamente invisibles.
     *
     * No afecta receptores, HUD, input ni scoring.
     * attackEndResetFactor hace que al final de la canción las notas
     * recuperen suavemente alpha=1 durante el reset de 1 segundo.
     */
    fun getAttackStealthAlpha(): Float {
        if (!AttackFeatureFlags.Visibility.STEALTH) return 1f

        val stealthAmount =
            currentAttackState.stealth * attackEndResetFactor

        return (1f - stealthAmount)
            .coerceIn(0f, 1f)
    }

    /**
     * True cuando Hidden/Sudden/Blink requieren alpha dependiente de posición/tiempo.
     * Se usa también para segmentar HOLD bodies sólo cuando realmente hace falta.
     */
    fun isAttackAppearanceActive(): Boolean {
        val hidden =
            AttackFeatureFlags.Visibility.HIDDEN &&
                    kotlin.math.abs(currentAttackState.hidden * attackEndResetFactor) > 0.0001f

        val sudden =
            AttackFeatureFlags.Visibility.SUDDEN &&
                    kotlin.math.abs(currentAttackState.sudden * attackEndResetFactor) > 0.0001f

        val blink =
            AttackFeatureFlags.Visibility.BLINK &&
                    kotlin.math.abs(currentAttackState.blink * attackEndResetFactor) > 0.0001f

        val randomVanish =
            AttackFeatureFlags.Visibility.RANDOM_VANISH &&
                    kotlin.math.abs(currentAttackState.randomVanish * attackEndResetFactor) > 0.0001f

        return hidden || sudden || blink || randomVanish
    }

    /**
     * Alpha de los ATTACKS de apariencia en el MISMO espacio lógico que usa
     * StepMania para ArrowEffects.
     *
     * sourceY es la posición ORIGINAL que entrega SscGameplayEngine, antes de
     * Reverse/Split/Alternate/Cross/Centered.  Esto es deliberado: StepMania
     * calcula GetAlpha() a partir de GetYPos(..., WithReverse=false).
     *
     * Para el modo vertical normal, FieldMetrics define:
     *
     *   targetTop (= medidaFlechas)                -> 0 SM
     *   targetTop + Gdx.graphics.height * 0.5f    -> 480 SM
     *
     * No hay clamp a 480; una nota que aún está debajo puede tener 600, 800,
     * etc.  isMidLine sigue siendo una regla NATIVA del renderer de Finger
     * Dance y no modifica este espacio de ATTACKS.
     */
    fun getAttackAppearanceAlpha(
        column: Int,
        sourceY: Float,
        baseScrollSpeed: Float
    ): Float {

        if (!isAttackAppearanceActive()) {
            return 1f
        }

        val hidden =
            if (AttackFeatureFlags.Visibility.HIDDEN) {
                (currentAttackState.hidden * attackEndResetFactor)
                    .coerceIn(0f, 1f)
            } else {
                0f
            }

        val sudden =
            if (AttackFeatureFlags.Visibility.SUDDEN) {
                (currentAttackState.sudden * attackEndResetFactor)
                    .coerceIn(0f, 1f)
            } else {
                0f
            }

        val blink =
            if (AttackFeatureFlags.Visibility.BLINK) {
                (currentAttackState.blink * attackEndResetFactor)
                    .coerceIn(0f, 1f)
            } else {
                0f
            }

        val randomVanish =
            if (AttackFeatureFlags.Visibility.RANDOM_VANISH) {
                (currentAttackState.randomVanish * attackEndResetFactor)
                    .coerceIn(0f, 1f)
            } else {
                0f
            }

        /*
         * Equivalente a ArrowEffects::GetYOffset(): incluye accel mods,
         * XMod/Expand y scroll speed, pero todavía NO Reverse ni Tipsy.
         */
        val fYOffset =
            getAttackStepManiaYOffset(
                rawPixelYOffset = sourceY - targetTop,
                baseScrollSpeed = baseScrollSpeed
            )

        /*
         * StepMania GetAlpha():
         *
         *   fYPosWithoutReverse = GetYPos(..., WithReverse=false)
         *
         * GetYPos(false) parte de fYOffset y sí añade Tipsy.  Lo hacemos en
         * unidades lógicas SM usando ARROW_SIZE=64, no medidaFlechas física.
         */
        var fYPosWithoutReverse = fYOffset

        val tipsyAmount =
            if (AttackFeatureFlags.Position.TIPSY) {
                currentAttackState.tipsy * attackEndResetFactor
            } else {
                0f
            }

        if (tipsyAmount != 0f) {
            fYPosWithoutReverse +=
                AttackEffects.tipsyY(
                    column = column,
                    songTimeSeconds = currentAttackSongTimeSeconds,
                    arrowSize = FieldMetrics.SM_ARROW_SIZE,
                    amount = tipsyAmount
                )
        }

        /* StepMania por defecto no aplica appearance después de cruzar receptor. */
        if (fYPosWithoutReverse < 0f) {
            return 1f
        }

        /*
         * ArrowEffects.cpp:
         * CENTER_LINE_Y = 160
         * FADE_DIST_Y   = 40
         *
         * El hack de Mini de StepMania NO usa pow(0.5, Mini) aquí; usa
         * fZoom = 1 - Mini*0.5 y divide CENTER_LINE_Y por ese zoom.
         */
        val miniPercent =
            if (AttackFeatureFlags.Scale.MINI) {
                currentAttackState.mini * attackEndResetFactor
            } else {
                0f
            }

        val miniZoom =
            (1f - miniPercent * 0.5f)
                .coerceAtLeast(0.10f)

        val centerLine = 160f / miniZoom
        val fadeDist = 40f

        val hiddenSudden =
            (hidden * sudden)
                .coerceIn(0f, 1f)

        fun lerp(a: Float, b: Float, t: Float): Float =
            a + (b - a) * t

        fun scale(
            value: Float,
            fromLow: Float,
            fromHigh: Float,
            toLow: Float,
            toHigh: Float
        ): Float {
            val denom = fromHigh - fromLow
            if (abs(denom) < 0.0001f) return toLow
            val t = (value - fromLow) / denom
            return toLow + (toHigh - toLow) * t
        }

        val hiddenEndLine =
            centerLine +
                    fadeDist * lerp(-1.0f, -1.25f, hiddenSudden)

        val hiddenStartLine =
            centerLine +
                    fadeDist * lerp(0.0f, -0.25f, hiddenSudden)

        val suddenEndLine =
            centerLine +
                    fadeDist * lerp(0.0f, 0.25f, hiddenSudden)

        val suddenStartLine =
            centerLine +
                    fadeDist * lerp(1.0f, 1.25f, hiddenSudden)

        var visibleAdjust = 0f

        if (hidden != 0f) {
            val hiddenAdjust =
                scale(
                    value = fYPosWithoutReverse,
                    fromLow = hiddenStartLine,
                    fromHigh = hiddenEndLine,
                    toLow = 0f,
                    toHigh = -1f
                ).coerceIn(-1f, 0f)

            visibleAdjust += hidden * hiddenAdjust
        }

        if (sudden != 0f) {
            val suddenAdjust =
                scale(
                    value = fYPosWithoutReverse,
                    fromLow = suddenStartLine,
                    fromHigh = suddenEndLine,
                    toLow = -1f,
                    toHigh = 0f
                ).coerceIn(-1f, 0f)

            visibleAdjust += sudden * suddenAdjust
        }

        if (blink != 0f) {
            val frequency = 0.3333f
            val raw =
                kotlin.math.sin(
                    currentAttackSongTimeSeconds * 10f
                )

            val quantized =
                (
                        kotlin.math.round(raw / frequency) * frequency
                        ).coerceIn(0f, 1f)

            /*
             * StepMania considera Blink encendido cuando su amount es distinto
             * de cero.  Conservamos la mezcla por amount para respetar el tween
             * del AttackEngine de Finger Dance al entrar/salir del ATTACK.
             */
            visibleAdjust +=
                blink * (quantized - 1f)
        }

        if (randomVanish != 0f) {
            val distFromCenterLine =
                abs(fYPosWithoutReverse - centerLine)

            val randomAdjust =
                scale(
                    value = distFromCenterLine,
                    fromLow = 80f,
                    fromHigh = 160f,
                    toLow = -1f,
                    toHigh = 0f
                ).coerceIn(-1f, 0f)

            visibleAdjust +=
                randomVanish * randomAdjust
        }

        return (1f + visibleAdjust)
            .coerceIn(0f, 1f)
    }

    /**
     * Blind de StepMania: oculta judgments y combo, no las notas/receptores.
     */
    fun getAttackBlindAlpha(): Float {
        if (!AttackFeatureFlags.Visibility.BLIND) return 1f

        val blind =
            currentAttackState.blind *
                    attackEndResetFactor

        return (1f - blind)
            .coerceIn(0f, 1f)
    }


    private fun drawReceptor(frame: TextureRegion, column: Int) {
        val attackX =
            getAttackColumnOffsetX(
                column = column,
                yOffset = 0f
            )

        var logicalY =
            getAttackReceptorY(column)

        val rotation =
            getAttackConfusionRotation(
                player.beatToShow
            )

        var logicalX =
            medidaFlechas *
                    (column + 1) +
                    attackX +
                    luaRecepts.screenX

        val metrics =
            receptorMetrics[column]

        val drawX =
            metrics.drawX(
                logicalX,
                medidaFlechas
            )

        val drawY =
            metrics.drawY(
                logicalY,
                medidaFlechas
            )

        val drawWidth =
            metrics.drawWidth(
                medidaFlechas
            )

        val drawHeight =
            metrics.drawHeight(
                medidaFlechas
            )

        val reverseScaleY =
            getAttackReverseScaleY(column)

        val miniScale =
            getAttackVisualScale() * getAttackMoveZScale(column)

        val darkAlpha =
            getAttackDarkAlpha()

        val oldColor =
            batch.color.cpy()

        batch.setColor(
            oldColor.r,
            oldColor.g,
            oldColor.b,
            oldColor.a * darkAlpha
        )

        batch.draw(
            frame,
            drawX,
            drawY,
            drawWidth * 0.5f,
            drawHeight * 0.5f,
            drawWidth,
            drawHeight,
            miniScale,
            miniScale * reverseScaleY,
            rotation
        )

        batch.color = oldColor
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
        if (::perspectiveRenderer.isInitialized) perspectiveRenderer.dispose()


        player.disposePlayer()
    }

}
