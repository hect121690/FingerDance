package com.fingerdance.ssc

import NoteCellMetrics
import android.os.SystemClock
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
import com.fingerdance.*
import com.fingerdance.ssc.attacks.AttackFeatureFlags
import com.fingerdance.ssc.attacks.AttackEffects
import com.fingerdance.ssc.attacks.AttackEngine
import com.fingerdance.ssc.attacks.AttackState
import com.fingerdance.ssc.attacks.FieldMetrics
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

    val receptorMetrics = Array(10) { NoteCellMetrics() }

    private val textureLD = loadTexture(ruta, "DownLeft Ready Receptor")
    private val textureLU = loadTexture(ruta, "UpLeft Ready Receptor")
    private val textureCE = loadTexture(ruta, "Center Ready Receptor")

    val receptLD = getReceptsTexture(textureLD, metricsColumn = 5)
    val receptLU = getReceptsTexture(textureLU, metricsColumn = 6)
    val receptCE = getReceptsTexture(textureCE, metricsColumn = 2)
    val receptRU = getReceptsTexture(textureLU, true, metricsColumn = 3)
    val receptRD = getReceptsTexture(textureLD, true, metricsColumn = 4)

    // ---------------------------------------------------

    lateinit var camera: OrthographicCamera
    lateinit var player: PlayerSscHD

    var targetTop = 0f

    private fun getStepManiaMetrics(): FieldMetrics =
        FieldMetrics.create(
            isVertical = true,
            halfDouble = true,
            screenWidth = Gdx.graphics.width.toFloat(),
            screenHeight = Gdx.graphics.height.toFloat(),
            arrowSizePx = arrowsSize
        )

    fun getAttackStepManiaFieldScaleY(): Float = getStepManiaMetrics().fieldScaleY
    fun getAttackStepManiaArrowScale(): Float = getStepManiaMetrics().arrowScale

    fun getAttackStepManiaYOffset(rawPixelYOffset: Float, baseScrollSpeed: Float): Float {
        val metrics = getStepManiaMetrics()
        val safeBaseSpeed = baseScrollSpeed.coerceAtLeast(0.0001f)
        val preSpeedPixelOffset = rawPixelYOffset / safeBaseSpeed
        val preSpeedSmOffset = metrics.toStepManiaY(preSpeedPixelOffset)

        val boostAmount = if (AttackFeatureFlags.AccelScroll.BOOST) currentAttackState.boost * attackEndResetFactor else 0f
        val brakeAmount = if (AttackFeatureFlags.AccelScroll.BRAKE) currentAttackState.brake * attackEndResetFactor else 0f
        val waveAmount = if (AttackFeatureFlags.AccelScroll.WAVE) currentAttackState.wave * attackEndResetFactor else 0f
        val boomerangAmount = if (AttackFeatureFlags.AccelScroll.BOOMERANG) currentAttackState.boomerang * attackEndResetFactor else 0f
        val expandAmount = if (AttackFeatureFlags.AccelScroll.EXPAND) currentAttackState.expand * AttackFeatureFlags.AccelScroll.EXPAND_INTENSITY * attackEndResetFactor else 0f
        val effectiveScrollSpeed = if (AttackFeatureFlags.Speed.XMOD) {
            safeBaseSpeed + (currentAttackState.xmod - safeBaseSpeed) * attackEndResetFactor
        } else safeBaseSpeed
        val effectHeightSm = FieldMetrics.SM_HEIGHT + kotlin.math.abs(currentAttackState.perspectiveTilt * attackEndResetFactor) * 200f
        val attackEarthwormAmount = if (AttackFeatureFlags.AccelScroll.EARTHWORM) currentAttackState.earthworm * attackEndResetFactor else 0f
        val earthwormAmount = if (playerSong.isEw) 1f else attackEarthwormAmount

        var transformedYOffset = AttackEffects.transformAccelYOffsetSm(
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
        if (earthwormAmount != 0f) {
            transformedYOffset = AttackEffects.earthwormY(
                yOffsetSm = transformedYOffset,
                songTimeSeconds = currentAttackSongTimeSeconds,
                amount = earthwormAmount
            )
        }
        return transformedYOffset
    }

    private var elapsedTime = 0f
    private var rithymAnim = 0f

    private var isPaused = false

    private val posYpadB = height.toFloat() - (width.toFloat() * 1.1f)

    private var timer = 0f
    private var showOverlay = false
    private var intervalOverlay = 0f

    private val attackPerspectiveActive: Boolean
        get() = AttackFeatureFlags.Perspective.ENABLED && (
            abs(currentAttackState.skew * attackEndResetFactor) > 0.0001f ||
                abs(currentAttackState.perspectiveTilt * attackEndResetFactor) > 0.0001f
        )

    val applyMesh: Boolean
        get() = nxProgress > 0f || attackPerspectiveActive

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

    val arrowsSize = width / 8f

    val maxWidth = medidaFlechas * 5f
    val maxlHeight = medidaFlechas / 2f

    val gaugeIncNormal = floatArrayOf(0.03f, 0.015f, 0.01f, -0.02f, -0.1f, 0.002f)
    val gaugeIncHJ = floatArrayOf(0.015f, 0.007f, 0.005f, -0.04f, -0.15f, 0.001f)

    private val lifeLightningTexture = Texture(Gdx.files.external("FingerDance/Themes/$tema/GraphicsStatics/game_play/barlife_electric 4x6.png"))
    val lifeLightningFrames: Array<TextureRegion> = getLifeLightningFrames(lifeLightningTexture)
    private val fadeTexture = Texture(Gdx.files.internal("black.png"))


    private lateinit var font: BitmapFont

    val attackEngine = AttackEngine(chart.attacks)
    var currentAttackState = AttackState()
        private set
    var currentAttackSongTimeSeconds = 0f
        private set

    private companion object {
        const val ATTACK_END_RESET_DURATION = 1f
    }
    private var attackEndResetActive = false
    private var attackEndResetElapsed = 0f
    private var attackEndResetFactor = 1f
    private var attackEndResetFinished = false

    // ---------------------------------------------------

    init {
        receptorMetrics[7] = receptorMetrics[2]

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
        font = BitmapFont()
        font.color = Color.WHITE
        font.data.setScale(2f, -2f)
        stage = Stage(ScreenViewport())
        camera = OrthographicCamera(Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.setToOrtho(true)

        perspectiveRenderer = PerspectivePlayfieldRenderer(
            width = Gdx.graphics.width,
            height = Gdx.graphics.height,
            pivotX = arrowsSize * 4f
        ).apply {
            topScaleX = 0.42f
            topShiftXPercent = 0.00f
            horizontalPower = 1.30f
            verticalPower = 1.80f
            meshOffsetY = Gdx.graphics.height * 0.08f
        }

        player = PlayerSscHD(this, batch, a)
        rithymAnim = (60f / player.m_fCurBPM)
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
            currentSongTimeMs = songTimeMs

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

                perspectiveRenderer.begin()
                batch.projectionMatrix = camera.combined
                batch.begin()

                if (!playerSong.fd) drawRecepts()
                player.renderMeshPlayfield(songTimeMs)

                batch.end()
                perspectiveRenderer.end()
                perspectiveRenderer.progress = nxProgress
                perspectiveRenderer.attackSkew = if (AttackFeatureFlags.Perspective.ENABLED) currentAttackState.skew * attackEndResetFactor else 0f
                perspectiveRenderer.attackTilt = if (AttackFeatureFlags.Perspective.ENABLED) currentAttackState.perspectiveTilt * attackEndResetFactor else 0f
                perspectiveRenderer.draw(camera.combined)

                batch.projectionMatrix = camera.combined
                batch.begin()
                player.renderMeshHud()

                barBlack.setSize(maxWidth, maxlHeight)
                barBlack.setPosition(medidaFlechas, 0f)
                barRed.setSize(maxWidth, maxlHeight)
                barRed.setPosition(medidaFlechas, 0f)

                drawEndingFade(delta)
                batch.end()
            }

            stage.act(delta)
        }

        stage.draw()
    }

    private fun getAttackLane(column: Int): Int = (column - 2).coerceIn(0, 5)

    private fun getHalfDoubleBaseX(column: Int): Float = when (column) {
        2 -> arrowsSize
        3 -> arrowsSize * 2f
        4 -> arrowsSize * 3f
        5 -> arrowsSize * 4f
        6 -> arrowsSize * 5f
        7 -> arrowsSize * 6f
        else -> -9999f
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
        val drawWidth = metrics.drawWidth(arrowsSize)
        val drawX = metrics.drawX(logicalX, arrowsSize)
        val originX = drawX + drawWidth * 0.5f

        val visibleLeftUnscaled = logicalX
        val visibleRightUnscaled = logicalX + arrowsSize

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
            column = getAttackLane(column),
            columnCount = 6,
            reverse = reverse,
            split = split,
            alternate = alternate,
            cross = cross
        )
    }

    private fun getAttackCenteredAmount(): Float =
        if (AttackFeatureFlags.DirectionColumn.CENTERED) {
            currentAttackState.centered * attackEndResetFactor
        }else {
            0f
        }

    private fun clampAttackLogicalY(
        column: Int,
        logicalY: Float,
        visualScaleY: Float
    ): Float {
        if (!isVertical || column !in receptorMetrics.indices) {
            return logicalY
        }

        val metrics = receptorMetrics[column]

        val drawHeight =
            metrics.drawHeight(arrowsSize)

        val drawY =
            metrics.drawY(
                logicalY,
                arrowsSize
            )

        val originY =
            drawY + drawHeight * 0.5f

        /*
         * logicalY representa el borde visible superior de la flecha y
         * logicalY + arrowsSize el inferior. SpriteBatch escala alrededor
         * del centro de la celda, por eso proyectamos ambos bordes.
         */
        val visibleTopUnscaled = logicalY
        val visibleBottomUnscaled = logicalY + arrowsSize

        val visibleTop =
            originY +
                    (visibleTopUnscaled - originY) *
                    visualScaleY

        val visibleBottom =
            originY +
                    (visibleBottomUnscaled - originY) *
                    visualScaleY

        val minVisibleY =
            minOf(
                visibleTop,
                visibleBottom
            )

        val maxVisibleY =
            maxOf(
                visibleTop,
                visibleBottom
            )

        val screenHeight =
            Gdx.graphics.height.toFloat()

        return when {
            minVisibleY < 0f ->
                logicalY - minVisibleY

            maxVisibleY > screenHeight ->
                logicalY - (maxVisibleY - screenHeight)

            else ->
                logicalY
        }
    }

    fun getAttackReceptorY(column: Int): Float {
        val reverseAmount = getAttackReversePercentForColumn(column)
        val centeredAmount = getAttackCenteredAmount()
        val reverseY = AttackEffects.reverseReceptorY(
            screenHeight = Gdx.graphics.height.toFloat(),
            arrowSize = arrowsSize
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
                column = getAttackLane(column),
                songTimeSeconds = currentAttackSongTimeSeconds,
                arrowSize = arrowsSize,
                amount = tipsyAmount
            )
        }

        /*
         * Igual que el notefield con isMidLine=true: el receptor jamás se
         * dispara fuera del viewport. Esta regla se aplica SIEMPRE.
         */
        val receptorScaleY =
            getAttackVisualScale() *
                    getAttackMoveZScale(column)

        return clampAttackLogicalY(
            column = column,
            logicalY = finalY,
            visualScaleY = receptorScaleY
        )
    }

    fun getAttackNoteY(column: Int, y: Float, baseScrollSpeed: Float): Float {
        val metrics = getStepManiaMetrics()
        val rawPixelYOffset = y - targetTop
        val smYOffset = getAttackStepManiaYOffset(rawPixelYOffset = rawPixelYOffset, baseScrollSpeed = baseScrollSpeed)
        val reverseAmount = getAttackReversePercentForColumn(column).coerceIn(0f, 1f)
        val centeredAmount = getAttackCenteredAmount()

        /*
         * TOP físico del receptor Reverse.
         *
         * Ejemplo:
         * height/2 - arrowsSize
         */
        val reverseReceptorY = AttackEffects.reverseReceptorY(screenHeight = Gdx.graphics.height.toFloat(), arrowSize = arrowsSize)

        /*
         * En NORMAL las notas se desplazan hacia arriba y el punto de contacto
         * está en targetTop.
         *
         * En REVERSE se desplazan hacia abajo. La referencia equivalente es
         * el borde inferior del receptor.
         *
         * reverseReceptorY + arrowsSize == height/2
         */
        val reverseJudgeY = reverseReceptorY + arrowsSize

        /*
         * Recorrido NORMAL:
         *
         * height -> arrowsSize
         *
         * Recorrido REVERSE:
         *
         * 0 -> height/2
         */
        val normalTravelDistance = (Gdx.graphics.height.toFloat() - targetTop).coerceAtLeast(1f)
        val reverseTravelDistance = reverseJudgeY.coerceAtLeast(1f)
        val fullReverseDistanceScale = (reverseTravelDistance / normalTravelDistance).coerceAtLeast(0.0001f)

        val distanceScale =
            1f +
                    (fullReverseDistanceScale - 1f) *
                    reverseAmount

        var finalY =
            targetTop +
                    metrics.toPixelsY(smYOffset) *
                    distanceScale

        /*
         * Para NOTAS usamos reverseJudgeY.
         *
         * getAttackReceptorY() sigue usando reverseReceptorY porque ése
         * continúa siendo el TOP desde donde se dibuja el sprite receptor.
         */
        finalY =
            AttackEffects.directionY(
                y = finalY,
                normalReceptorY = targetTop,
                reverseReceptorY = reverseJudgeY,
                reverseAmount = reverseAmount,
                centeredAmount = centeredAmount
            )

        val tipsyAmount =
            if (AttackFeatureFlags.Position.TIPSY) {
                currentAttackState.tipsy *
                        attackEndResetFactor
            } else {
                0f
            }

        if (tipsyAmount != 0f) {
            finalY +=
                AttackEffects.tipsyY(
                    column = getAttackLane(column),
                    songTimeSeconds = currentAttackSongTimeSeconds,
                    arrowSize = arrowsSize,
                    amount = tipsyAmount
                )
        }

        val moveZScale =
            getAttackMoveZScale(column)

        if (moveZScale != 1f) {
            /*
             * El pivote del recorrido de las NOTAS tiene que ser el mismo
             * punto donde termina el recorrido Reverse.
             *
             * Normal  -> targetTop
             * Reverse -> reverseJudgeY
             *
             * directionY nos da la interpolación para porcentajes parciales
             * de Reverse/Split/Alternate/Cross.
             */
            val noteTargetY =
                AttackEffects.directionY(
                    y = targetTop,
                    normalReceptorY = targetTop,
                    reverseReceptorY = reverseJudgeY,
                    reverseAmount = reverseAmount,
                    centeredAmount = centeredAmount
                )

            finalY =
                noteTargetY +
                        (finalY - noteTargetY) *
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
                column = getAttackLane(column),
                columnCount = 6,
                yOffset = smYOffset,
                arrowSize = arrowsSize,
                screenHeight = FieldMetrics.SM_HEIGHT,
                amount = currentAttackState.tornado * attackEndResetFactor
            )
        }

        if (AttackFeatureFlags.Position.DRUNK && currentAttackState.drunk != 0f) {
            offsetX += AttackEffects.drunkX(
                column = getAttackLane(column),
                yOffset = smYOffset,
                songTimeSeconds = currentAttackSongTimeSeconds,
                arrowSize = arrowsSize,
                screenHeight = FieldMetrics.SM_HEIGHT,
                amount = currentAttackState.drunk * attackEndResetFactor
            )
        }

        if (AttackFeatureFlags.DirectionColumn.FLIP && currentAttackState.flip != 0f) {
            offsetX += AttackEffects.flipX(
                column = getAttackLane(column),
                columnCount = 6,
                arrowSize = arrowsSize,
                amount = currentAttackState.flip * attackEndResetFactor
            )
        }

        if (AttackFeatureFlags.DirectionColumn.INVERT && currentAttackState.invert != 0f) {
            offsetX += AttackEffects.invertX(
                column = getAttackLane(column),
                columnCount = 6,
                arrowSize = arrowsSize,
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
            val baseX = getHalfDoubleBaseX(column)
            val currentX = baseX + offsetX
            val centerX = arrowsSize * 4f

            val miniX = AttackEffects.miniX(
                x = currentX,
                centerX = centerX,
                amount = currentAttackState.mini * attackEndResetFactor
            )

            offsetX = miniX - baseX
        }

        if (AttackFeatureFlags.Scale.TINY && currentAttackState.tiny != 0f) {
            val baseX = getHalfDoubleBaseX(column)
            val currentX = baseX + offsetX
            val centerX = arrowsSize * 4f

            val tinyX = AttackEffects.tinyX(
                x = currentX,
                centerX = centerX,
                amount = currentAttackState.tiny * attackEndResetFactor
            )

            offsetX = tinyX - baseX
        }

        val moveZScale = getAttackMoveZScale(column)
        if (moveZScale != 1f) {
            val baseX = getHalfDoubleBaseX(column)
            val currentX = baseX + offsetX
            val fieldCenterX = arrowsSize * 4f
            val projectedX = fieldCenterX + (currentX - fieldCenterX) * moveZScale
            offsetX = projectedX - baseX
        }

        if (isVertical) {
            val baseX = getHalfDoubleBaseX(column)
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
            val t = if (nxTransitionBeats <= 0.0) 1f else (elapsedBeats / nxTransitionBeats).toFloat().coerceIn(0f, 1f)
            nxProgress = nxStartProgress + (nxTargetProgress - nxStartProgress) * t

            if (t >= 1f) {
                nxProgress = nxTargetProgress
                nxTransitioning = false
                if (nxReturningToBase) {
                    nxReturningToBase = false
                    nxWaitingReturn = false
                } else {
                    beginNxHold(currentBeat, songTimeMs)
                }
            }
        }

        if (nxWaitingReturn && !nxTransitioning) {
            val finished = when (nxDurationUnit) {
                1 -> songTimeMs - nxEffectStartMs >= nxEffectDuration
                else -> currentBeat - nxEffectStartBeat >= nxEffectDuration
            }
            if (finished) returnNxToBase(currentBeat)
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

    fun getAttackReverseScaleY(column: Int): Float = 1f

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
        if (endingFadeAlpha > 1f) endingFadeAlpha = 1f
        batch.setColor(0f, 0f, 0f, endingFadeAlpha)
        batch.draw(fadeTexture, 0f, 0f, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        batch.setColor(1f, 1f, 1f, 1f)
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

    fun getAttackDarkAlpha(): Float {
        if (!AttackFeatureFlags.Visibility.DARK) return 1f

        val darkAmount =
            currentAttackState.dark * attackEndResetFactor

        return (1f - darkAmount)
            .coerceIn(0f, 1f)
    }

    fun getAttackMoveZAmount(column: Int): Float {
        if (!AttackFeatureFlags.Position.MOVE_Z) return 0f

        return currentAttackState.getMoveZ(getAttackLane(column)) * attackEndResetFactor
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
        return getAttackReceptorY(column) + (arrowsSize * 0.5f)
    }

    fun getAttackMoveZScale(column: Int): Float {
        val amount = getAttackMoveZAmount(column)
        if (kotlin.math.abs(amount) < 0.0001f) return 1f

        // MoveZ 100% ~= una ArrowSize de profundidad.
        val zPixels = amount * arrowsSize
        val focal = (Gdx.graphics.height.toFloat() * 0.75f).coerceAtLeast(arrowsSize * 4f)
        val denominator = (focal - zPixels).coerceAtLeast(focal * 0.25f)

        return (focal / denominator).coerceIn(0.60f, 1.60f)
    }

    fun getAttackStealthAlpha(): Float {
        if (!AttackFeatureFlags.Visibility.STEALTH) return 1f

        val stealthAmount =
            currentAttackState.stealth * attackEndResetFactor

        return (1f - stealthAmount)
            .coerceIn(0f, 1f)
    }

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
         * unidades lógicas SM usando ARROW_SIZE=64, no arrowsSize física.
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
                    column = getAttackLane(column),
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

    fun getAttackBlindAlpha(): Float {
        if (!AttackFeatureFlags.Visibility.BLIND) return 1f

        val blind =
            currentAttackState.blind *
                    attackEndResetFactor

        return (1f - blind)
            .coerceIn(0f, 1f)
    }

    private fun drawRecepts() {
        drawReceptor(receptCE[0], 2)
        drawReceptor(receptRU[0], 3)
        drawReceptor(receptRD[0], 4)
        drawReceptor(receptLD[0], 5)
        drawReceptor(receptLU[0], 6)
        drawReceptor(receptCE[0], 7)

        if (showOverlay) {
            aBatch = batch.blendSrcFunc
            bBatch = batch.blendDstFunc
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
            drawReceptor(receptCE[1], 2)
            drawReceptor(receptRU[1], 3)
            drawReceptor(receptRD[1], 4)
            drawReceptor(receptLD[1], 5)
            drawReceptor(receptLU[1], 6)
            drawReceptor(receptCE[1], 7)
            batch.setBlendFunction(aBatch, bBatch)
        }
    }

    // ---------------------------------------------------

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
            getHalfDoubleBaseX(column) +
                    attackX +
                    luaRecepts.screenX

        val metrics =
            receptorMetrics[column]

        val drawX =
            metrics.drawX(
                logicalX,
                arrowsSize
            )

        val drawY =
            metrics.drawY(
                logicalY,
                arrowsSize
            )

        val drawWidth =
            metrics.drawWidth(
                arrowsSize
            )

        val drawHeight =
            metrics.drawHeight(
                arrowsSize
            )

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
            miniScale,
            rotation
        )

        batch.color = oldColor
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

        return if (maxX < minX || maxY < minY) {
            Bounds(0, 0, sourceWidth - 1, sourceHeight - 1)
        } else {
            Bounds(minX, minY, maxX, maxY)
        }
    }


    // ---------------------------------------------------

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
        if (::perspectiveRenderer.isInitialized) {
            perspectiveRenderer.resize(width, height, arrowsSize * 4f)
        }
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

        font.dispose()
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

        if (::perspectiveRenderer.isInitialized) perspectiveRenderer.dispose()
        player.disposePlayer()
    }

    // ---------------------------------------------------

    fun timeGetTime(): Long {
        return SystemClock.uptimeMillis()
    }
}
