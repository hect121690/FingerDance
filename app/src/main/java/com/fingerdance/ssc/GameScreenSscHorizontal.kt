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
import com.fingerdance.GameScreenActivityHorizontal
import com.fingerdance.OrientationMode
import com.fingerdance.aBatch
import com.fingerdance.alphaPadB
import com.fingerdance.bBatch
import com.fingerdance.chart
import com.fingerdance.durationSong
import com.fingerdance.endingFadeAlpha
import com.fingerdance.height
import com.fingerdance.heightBtnsHorizontal
import com.fingerdance.hideImagesPadA
import com.fingerdance.isEndingFade
import com.fingerdance.loadTexture
import com.fingerdance.luaRecepts
import com.fingerdance.medidaFlechasHorizontal
import com.fingerdance.orientationMode
import com.fingerdance.padPositionsHorizontal
import com.fingerdance.playerSong
import com.fingerdance.ruta
import com.fingerdance.showPadB
import com.fingerdance.skinPad
import com.fingerdance.spaceInitHorizontal
import com.fingerdance.ssc.attacks.AttackEffects
import com.fingerdance.ssc.attacks.AttackEngine
import com.fingerdance.ssc.attacks.AttackFeatureFlags
import com.fingerdance.ssc.attacks.AttackState
import com.fingerdance.ssc.attacks.FieldMetrics
import com.fingerdance.tema
import com.fingerdance.typePadD
import com.fingerdance.width
import com.fingerdance.widthBtnsHorizontal
import kotlin.math.abs

open class GameScreenSscHorizontal(activity: GameScreenActivityHorizontal) : Screen {
    val a = activity

    private lateinit var batch: SpriteBatch
    lateinit var stage: Stage

    val rutaPads = "/FingerDance/Themes/${tema}/GraphicsStatics/game_play"
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

    private val backgroundTexture = Texture(Gdx.files.external("FingerDance/Themes/${tema}/GraphicsStatics/game_play/barLife0.png"))
    private val barBlackTexture = Texture(Gdx.files.external("FingerDance/Themes/${tema}/GraphicsStatics/game_play/barLife1.png"))
    private val barRedTexture = Texture(Gdx.files.external("FingerDance/Themes/${tema}/GraphicsStatics/game_play/barLife2.png"))
    private val barLifeTexture = Texture(Gdx.files.external("FingerDance/Themes/${tema}/GraphicsStatics/game_play/barLife3.png"))

    val barFrame = Sprite(backgroundTexture)
    val barBlack = Sprite(barBlackTexture)
    val barRed = Sprite(barRedTexture)
    val barColors = Sprite(barLifeTexture)

    private val barTipTexture = Texture(Gdx.files.external("FingerDance/Themes/${tema}/GraphicsStatics/game_play/bar_tip.png"))
    val barTip = Sprite(barTipTexture)

    private lateinit var padB : TextureRegion
    lateinit var spritePadB: Sprite

    data class OverlayMetrics(
        val widthRatio: Float = 1f,
        val heightRatio: Float = 1f,
        val offsetXRatio: Float = 0f,
        val offsetYRatio: Float = 0f
    )

    private val receptorOverlayMetrics = Array(5) { OverlayMetrics() }

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

    private fun getStepManiaMetrics(): FieldMetrics = FieldMetrics.create(
        isVertical = orientationMode == OrientationMode.VERTICAL,
        halfDouble = false,
        screenWidth = Gdx.graphics.width.toFloat(),
        screenHeight = Gdx.graphics.height.toFloat(),
        arrowSizePx = medidaFlechasHorizontal
    )

    fun getAttackStepManiaFieldScaleY(): Float = getStepManiaMetrics().fieldScaleY
    fun getAttackStepManiaArrowScale(): Float = getStepManiaMetrics().arrowScale

    fun getAttackStepManiaYOffset(rawPixelYOffset: Float, baseScrollSpeed: Float): Float {
        val metrics = getStepManiaMetrics()
        val baseSpeedForMath = if (orientationMode == OrientationMode.VERTICAL) baseScrollSpeed.coerceAtLeast(0.0001f) else baseScrollSpeed
        val preSpeedSmOffset = metrics.toStepManiaY(rawPixelYOffset / baseSpeedForMath)
        val boostAmount = if (AttackFeatureFlags.AccelScroll.BOOST) currentAttackState.boost * attackEndResetFactor else 0f
        val brakeAmount = if (AttackFeatureFlags.AccelScroll.BRAKE) currentAttackState.brake * attackEndResetFactor else 0f
        val waveAmount = if (AttackFeatureFlags.AccelScroll.WAVE) currentAttackState.wave * attackEndResetFactor else 0f
        val boomerangAmount = if (AttackFeatureFlags.AccelScroll.BOOMERANG) currentAttackState.boomerang * attackEndResetFactor else 0f
        val expandAmount = if (AttackFeatureFlags.AccelScroll.EXPAND) currentAttackState.expand * attackEndResetFactor else 0f
        val effectiveScrollSpeed = if (AttackFeatureFlags.Speed.XMOD) baseSpeedForMath + (currentAttackState.xmod - baseSpeedForMath) * attackEndResetFactor else baseSpeedForMath
        val effectHeightSm = FieldMetrics.SM_HEIGHT + kotlin.math.abs(currentAttackState.perspectiveTilt * attackEndResetFactor) * 200f
        val attackEarthwormAmount = if (AttackFeatureFlags.AccelScroll.EARTHWORM) currentAttackState.earthworm * attackEndResetFactor else 0f
        val earthwormAmount = if (playerSong.isEw) 1f else attackEarthwormAmount
        var transformedYOffset = AttackEffects.transformAccelYOffsetSm(
            yOffsetSm = preSpeedSmOffset,
            effectHeightSm = effectHeightSm,
            expandSeconds = if (orientationMode == OrientationMode.VERTICAL) currentAttackSongTimeSeconds else attackExpandSeconds,
            boostAmount = boostAmount,
            brakeAmount = brakeAmount,
            waveAmount = waveAmount,
            boomerangAmount = boomerangAmount,
            expandAmount = expandAmount,
            baseScrollSpeed = effectiveScrollSpeed
        )
        if (earthwormAmount != 0f) transformedYOffset = AttackEffects.earthwormY(
            yOffsetSm = transformedYOffset,
            songTimeSeconds = currentAttackSongTimeSeconds,
            amount = earthwormAmount
        )
        return transformedYOffset
    }

    fun getAttackMiniScale(): Float = if (AttackFeatureFlags.Scale.MINI) AttackEffects.miniScale(currentAttackState.mini * attackEndResetFactor) else 1f
    fun getAttackTinyScale(): Float = if (AttackFeatureFlags.Scale.TINY) AttackEffects.tinyScale(currentAttackState.tiny * attackEndResetFactor) else 1f
    fun getAttackVisualScale(): Float = getAttackMiniScale() * getAttackTinyScale()

    fun getAttackReversePercentForColumn(column: Int): Float {
        val reverse = if (AttackFeatureFlags.DirectionColumn.REVERSE) currentAttackState.reverse * attackEndResetFactor else 0f
        val split = if (AttackFeatureFlags.DirectionColumn.SPLIT) currentAttackState.split * attackEndResetFactor else 0f
        val alternate = if (AttackFeatureFlags.DirectionColumn.ALTERNATE) currentAttackState.alternate * attackEndResetFactor else 0f
        val cross = if (AttackFeatureFlags.DirectionColumn.CROSS) currentAttackState.cross * attackEndResetFactor else 0f
        return AttackEffects.reversePercentForColumn(column, 5, reverse, split, alternate, cross)
    }

    private fun getAttackCenteredAmount(): Float = if (AttackFeatureFlags.DirectionColumn.CENTERED) currentAttackState.centered * attackEndResetFactor else 0f

    fun getAttackReceptorY(column: Int): Float {
        val reverseAmount = getAttackReversePercentForColumn(column)
        val centeredAmount = getAttackCenteredAmount()
        val reverseReceptorY = getReverseReceptorY()
        val miniZoom = getAttackMiniScale()
        var finalY = AttackEffects.directionY(targetTop, targetTop, reverseReceptorY, reverseAmount, centeredAmount, miniZoom)
        val tipsyAmount = if (AttackFeatureFlags.Position.TIPSY) currentAttackState.tipsy * attackEndResetFactor else 0f
        if (tipsyAmount != 0f) finalY += AttackEffects.tipsyY(column, getAttackModTimerSeconds(), medidaFlechasHorizontal, tipsyAmount) * miniZoom
        return finalY
    }

    fun getAttackNoteY(column: Int, y: Float, baseScrollSpeed: Float): Float {
        val metrics = getStepManiaMetrics()
        val smYOffset = getAttackStepManiaYOffset(y - targetTop, baseScrollSpeed)
        val reverseAmount = getAttackReversePercentForColumn(column)
        val centeredAmount = getAttackCenteredAmount()
        val reverseReceptorY = getReverseReceptorY()
        val miniZoom = getAttackMiniScale()
        var finalY = targetTop + metrics.toPixelsY(smYOffset)
        finalY = AttackEffects.directionY(finalY, targetTop, reverseReceptorY, reverseAmount, centeredAmount, miniZoom)
        val tipsyAmount = if (AttackFeatureFlags.Position.TIPSY) currentAttackState.tipsy * attackEndResetFactor else 0f
        if (tipsyAmount != 0f) finalY += AttackEffects.tipsyY(column, getAttackModTimerSeconds(), medidaFlechasHorizontal, tipsyAmount) * miniZoom
        val moveZScale = getAttackMoveZScale(column)
        if (moveZScale != 1f) {
            val noteTargetY = AttackEffects.directionY(targetTop, targetTop, reverseReceptorY, reverseAmount, centeredAmount, miniZoom)
            finalY = noteTargetY + (finalY - noteTargetY) * moveZScale
        }
        return finalY
    }

    private fun updateAttacks(songTimeMs: Double, delta: Float) {
        currentAttackState = attackEngine.update(songTimeMs = songTimeMs, deltaSeconds = delta, baseScrollSpeed = player.baseSpeed)
        currentAttackSongTimeSeconds = (songTimeMs / 1000.0).toFloat()
        if (orientationMode != OrientationMode.VERTICAL) {
            attackExpandSeconds += delta
            attackExpandSeconds %= (Math.PI.toFloat() * 2f)
        }
    }

    private fun beginAttackEndReset() {
        if (attackEndResetActive || attackEndResetFinished) return
        attackEndResetActive = true
        attackEndResetElapsed = 0f
        attackEndResetFactor = 1f
    }

    private fun updateAttackEndReset(delta: Float) {
        if (!attackEndResetActive) return
        attackEndResetElapsed += delta
        val t = (attackEndResetElapsed / ATTACK_END_RESET_DURATION).coerceIn(0f, 1f)
        attackEndResetFactor = 1f - t
        if (t >= 1f) {
            attackEndResetFactor = 0f
            attackEndResetActive = false
            attackEndResetFinished = true
        }
    }

    private fun getAttackColumnPositions(): FloatArray = FloatArray(5) { column ->
        spaceInitHorizontal + medidaFlechasHorizontal * (column + 1)
    }

    fun getAttackColumnOffsetX(column: Int, yOffset: Float, baseScrollSpeed: Float = 1f): Float {
        var offsetX = 0f
        val metrics = getStepManiaMetrics()
        val smYOffset = getAttackStepManiaYOffset(yOffset, baseScrollSpeed)
        val columnPositions = getAttackColumnPositions()
        if (AttackFeatureFlags.Position.TORNADO && currentAttackState.tornado != 0f) offsetX += AttackEffects.tornadoX(column, 5, smYOffset, medidaFlechasHorizontal, FieldMetrics.SM_HEIGHT, currentAttackState.tornado * attackEndResetFactor, columnPositions = columnPositions)
        if (AttackFeatureFlags.Position.DRUNK && currentAttackState.drunk != 0f) offsetX += AttackEffects.drunkX(column, smYOffset, getAttackModTimerSeconds(), medidaFlechasHorizontal, FieldMetrics.SM_HEIGHT, currentAttackState.drunk * attackEndResetFactor)
        if (AttackFeatureFlags.DirectionColumn.FLIP && currentAttackState.flip != 0f) offsetX += AttackEffects.flipX(column, 5, medidaFlechasHorizontal, currentAttackState.flip * attackEndResetFactor, columnPositions = columnPositions)
        if (AttackFeatureFlags.DirectionColumn.INVERT && currentAttackState.invert != 0f) offsetX += AttackEffects.invertX(column, 5, medidaFlechasHorizontal, currentAttackState.invert * attackEndResetFactor, columnPositions = columnPositions)
        if (AttackFeatureFlags.Position.BEAT && currentAttackState.beat != 0f) offsetX += AttackEffects.beatX(smYOffset, player.beatToShow, currentAttackState.beat * attackEndResetFactor) * metrics.fieldScaleX
        val baseX = columnPositions[column]
        val centerX = spaceInitHorizontal + medidaFlechasHorizontal * 3.5f
        if (AttackFeatureFlags.Scale.MINI && currentAttackState.mini != 0f) offsetX = AttackEffects.miniX(baseX + offsetX, centerX, currentAttackState.mini * attackEndResetFactor) - baseX
        if (AttackFeatureFlags.Scale.TINY && currentAttackState.tiny != 0f) offsetX = AttackEffects.tinyX(baseX + offsetX, centerX, currentAttackState.tiny * attackEndResetFactor) - baseX
        val moveZScale = getAttackMoveZScale(column)
        if (moveZScale != 1f) offsetX = centerX + (baseX + offsetX - centerX) * moveZScale - baseX
        return offsetX
    }

    fun getAttackConfusionRotation(currentBeat: Double): Float {
        if (!AttackFeatureFlags.Rotation3D.CONFUSION) return 0f
        val amount = currentAttackState.confusion * attackEndResetFactor
        return if (amount == 0f) 0f else AttackEffects.confusionRotation(currentBeat, amount)
    }

    fun getAttackNoteRotation(noteBeat: Double, currentBeat: Double): Float {
        if (!AttackFeatureFlags.Rotation3D.DIZZY) return 0f
        val amount = currentAttackState.dizzy * attackEndResetFactor
        return if (amount == 0f) 0f else AttackEffects.dizzyRotation(noteBeat, currentBeat, amount)
    }

    fun getAttackReverseScaleY(column: Int): Float = 1f
    fun getAttackDarkAlpha(): Float = if (AttackFeatureFlags.Visibility.DARK) (1f - currentAttackState.dark * attackEndResetFactor).coerceIn(0f, 1f) else 1f
    fun getAttackMoveZAmount(column: Int): Float = if (AttackFeatureFlags.Position.MOVE_Z) currentAttackState.getMoveZ(column) * attackEndResetFactor else 0f
    fun getAttackBumpyAmount(): Float = if (AttackFeatureFlags.Rotation3D.BUMPY) currentAttackState.bumpy * attackEndResetFactor else 0f
    fun getAttackTwirlAmount(): Float = if (AttackFeatureFlags.Rotation3D.TWIRL) currentAttackState.twirl * attackEndResetFactor else 0f
    fun getAttackRollAmount(): Float = if (AttackFeatureFlags.Rotation3D.ROLL) currentAttackState.roll * attackEndResetFactor else 0f
    fun getAttackReceptorCenterY(column: Int): Float = getAttackReceptorY(column) + medidaFlechasHorizontal * 0.5f

    fun getAttackMoveZScale(column: Int): Float {
        val amount = getAttackMoveZAmount(column)
        if (kotlin.math.abs(amount) < 0.0001f) return 1f
        val zPixels = amount * medidaFlechasHorizontal
        val focal = (Gdx.graphics.height.toFloat() * 0.75f).coerceAtLeast(medidaFlechasHorizontal * 4f)
        val denominator = (focal - zPixels).coerceAtLeast(focal * 0.25f)
        return (focal / denominator).coerceIn(0.60f, 1.60f)
    }

    fun getAttackStealthAlpha(): Float =
        if (orientationMode == OrientationMode.VERTICAL) {
        if (AttackFeatureFlags.Visibility.STEALTH) (1f - currentAttackState.stealth * attackEndResetFactor).coerceIn(0f, 1f) else 1f
    } else 1f

    fun isAttackAppearanceActive(): Boolean {
        val hidden = AttackFeatureFlags.Visibility.HIDDEN && kotlin.math.abs(currentAttackState.hidden * attackEndResetFactor) > 0.0001f
        val sudden = AttackFeatureFlags.Visibility.SUDDEN && kotlin.math.abs(currentAttackState.sudden * attackEndResetFactor) > 0.0001f
        val stealth = AttackFeatureFlags.Visibility.STEALTH && kotlin.math.abs(currentAttackState.stealth * attackEndResetFactor) > 0.0001f
        val blink = AttackFeatureFlags.Visibility.BLINK && kotlin.math.abs(currentAttackState.blink * attackEndResetFactor) > 0.0001f
        val randomVanish = AttackFeatureFlags.Visibility.RANDOM_VANISH && kotlin.math.abs(currentAttackState.randomVanish * attackEndResetFactor) > 0.0001f
        return hidden || sudden || stealth || blink || randomVanish
    }

    fun getAttackAppearanceAlpha(column: Int, sourceY: Float, baseScrollSpeed: Float): Float {
        if (!isAttackAppearanceActive()) return 1f
        val hidden = if (AttackFeatureFlags.Visibility.HIDDEN) currentAttackState.hidden * attackEndResetFactor else 0f
        val sudden = if (AttackFeatureFlags.Visibility.SUDDEN) currentAttackState.sudden * attackEndResetFactor else 0f
        val stealth = if (AttackFeatureFlags.Visibility.STEALTH) currentAttackState.stealth * attackEndResetFactor else 0f
        val blink = if (AttackFeatureFlags.Visibility.BLINK) currentAttackState.blink * attackEndResetFactor else 0f
        val randomVanish = if (AttackFeatureFlags.Visibility.RANDOM_VANISH) currentAttackState.randomVanish * attackEndResetFactor else 0f
        val fYOffset = getAttackStepManiaYOffset(sourceY - targetTop, baseScrollSpeed)
        var fYPosWithoutReverse = fYOffset
        val tipsyAmount = if (AttackFeatureFlags.Position.TIPSY) currentAttackState.tipsy * attackEndResetFactor else 0f
        if (tipsyAmount != 0f) fYPosWithoutReverse += AttackEffects.tipsyY(column, getAttackModTimerSeconds(), FieldMetrics.SM_ARROW_SIZE, tipsyAmount)
        if (fYPosWithoutReverse < 0f) return 1f
        val miniPercent = if (AttackFeatureFlags.Scale.MINI) currentAttackState.mini * attackEndResetFactor else 0f
        val miniZoom = 1f - miniPercent * 0.5f
        val centerLine = 160f / miniZoom
        val fadeDist = 40f
        val hiddenSudden = hidden * sudden
        fun scaleValue(value: Float, fromLow: Float, fromHigh: Float, toLow: Float, toHigh: Float): Float = toLow + (toHigh - toLow) * ((value - fromLow) / (fromHigh - fromLow))
        val hiddenEndLine = centerLine + fadeDist * scaleValue(hiddenSudden, 0f, 1f, -1f, -1.25f)
        val hiddenStartLine = centerLine + fadeDist * scaleValue(hiddenSudden, 0f, 1f, 0f, -0.25f)
        val suddenEndLine = centerLine + fadeDist * scaleValue(hiddenSudden, 0f, 1f, 0f, 0.25f)
        val suddenStartLine = centerLine + fadeDist * scaleValue(hiddenSudden, 0f, 1f, 1f, 1.25f)
        var visibleAdjust = 0f
        if (hidden != 0f) visibleAdjust += hidden * scaleValue(fYPosWithoutReverse, hiddenStartLine, hiddenEndLine, 0f, -1f).coerceIn(-1f, 0f)
        if (sudden != 0f) visibleAdjust += sudden * scaleValue(fYPosWithoutReverse, suddenStartLine, suddenEndLine, -1f, 0f).coerceIn(-1f, 0f)
        if (stealth != 0f) visibleAdjust -= stealth
        if (blink != 0f) {
            val frequency = 0.3333f
            val raw = kotlin.math.sin(getAttackModTimerSeconds() * 10f)
            val quantized = kotlin.math.round(raw / frequency) * frequency
            visibleAdjust += scaleValue(quantized, 0f, 1f, -1f, 0f)
        }
        if (randomVanish != 0f) {
            val distFromCenterLine = abs(fYPosWithoutReverse - centerLine)
            visibleAdjust += scaleValue(distFromCenterLine, 80f, 160f, -1f, 0f) * randomVanish
        }
        return (1f + visibleAdjust).coerceIn(0f, 1f)
    }

    fun getAttackBlindAlpha(): Float = if (AttackFeatureFlags.Visibility.BLIND) (1f - currentAttackState.blind * attackEndResetFactor).coerceIn(0f, 1f) else 1f


    private var elapsedTime = 0f
    private var rithymAnim = 0f

    private var isPaused = false
    lateinit var camera : OrthographicCamera
    lateinit var player: PlayerSscHorizontal
    private val posYpadB = (medidaFlechasHorizontal * 2)
    private val posXpadRight = height - (widthBtnsHorizontal * 3)

    private var timer = 0f
    private var showOverlay = false
    private var intervalOverlay = 0f

    private val attackPerspectiveActive: Boolean
        get() = AttackFeatureFlags.Perspective.ENABLED && (abs(currentAttackState.skew * attackEndResetFactor) > 0.0001f || abs(currentAttackState.perspectiveTilt * attackEndResetFactor) > 0.0001f)

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
    val maxWidth = medidaFlechasHorizontal * 7f
    val maxlHeight = medidaFlechasHorizontal / 2f

    val gaugeIncNormal = floatArrayOf(0.03f, 0.015f, 0.01f, -0.02f, -0.1f, 0.002f)
    val gaugeIncHJ = floatArrayOf(0.015f, 0.007f, 0.005f, -0.04f, -0.15f, 0.001f)

    private val lifeLightningTexture = Texture(Gdx.files.external("FingerDance/Themes/$tema/GraphicsStatics/game_play/barlife_electric 4x6.png"))

    val lifeLightningFrames: Array<TextureRegion> = getLifeLightningFrames(lifeLightningTexture)


    val posYGauje = medidaFlechasHorizontal / 4f

    private val fadeTexture = Texture(Gdx.files.internal("black.png"))

    init {
        if(showPadB == 1){
            padB = TextureRegion(Texture(Gdx.files.external("/FingerDance/PadsB/${skinPad}.png")))
            spritePadB = Sprite(padB).apply { flip(false, true) }
        }else if(showPadB == 2){
            padB = TextureRegion(Texture(Gdx.files.external("/FingerDance/PadsC/${skinPad}/BG.png")))
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

    val attackEngine = AttackEngine(chart.attacks)
    var currentAttackState = AttackState()
        private set
    var currentAttackSongTimeSeconds = 0f
        private set
    private val attackModTimerStartMs = SystemClock.uptimeMillis()
    private var attackExpandSeconds = 0f

    private fun getAttackModTimerSeconds(): Float =
        if (orientationMode == OrientationMode.VERTICAL) {
            currentAttackSongTimeSeconds
        }else {
            (SystemClock.uptimeMillis() - attackModTimerStartMs) / 1000f
        }
    private companion object {
        const val ATTACK_END_RESET_DURATION = 1f
    }

    private var attackEndResetActive = false
    private var attackEndResetElapsed = 0f
    private var attackEndResetFactor = 1f
    private var attackEndResetFinished = false

    override fun show() {
        batch = SpriteBatch()
        stage = Stage(ScreenViewport())
        camera = OrthographicCamera()
        camera.setToOrtho(true, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())

        perspectiveRenderer = PerspectivePlayfieldRenderer(
            width = Gdx.graphics.width,
            height = Gdx.graphics.height,
            pivotX = spaceInitHorizontal + (medidaFlechasHorizontal * 3.5f)
        ).apply {
            topScaleX = 0.42f
            topShiftXPercent = 0.00f
            horizontalPower = 1.30f
            verticalPower = 1.80f
            meshOffsetY = Gdx.graphics.height * 0.08f
        }

        targetTop = medidaFlechasHorizontal
        player = PlayerSscHorizontal(this, batch, a)
        rithymAnim = 60f / player.m_fCurBPM

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
            if (!attackEndResetActive && !attackEndResetFinished) {
                if (durationSong > 0L && songTimeMs >= durationSong.toDouble()) beginAttackEndReset() else updateAttacks(songTimeMs, delta)
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
                barBlack.setPosition(medidaFlechasHorizontal, posYGauje)
                barRed.setSize(maxWidth, maxlHeight)
                barRed.setPosition(medidaFlechasHorizontal, posYGauje)

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
                barBlack.setPosition(medidaFlechasHorizontal, posYGauje)
                barRed.setSize(maxWidth, maxlHeight)
                barRed.setPosition(medidaFlechasHorizontal, posYGauje)

                drawEndingFade(delta)
                batch.end()
            }

            stage.act(delta)
        }

        stage.draw()
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

    override fun resize(width: Int, height: Int) {
        camera.setToOrtho(true, width.toFloat(), height.toFloat())
        camera.update()
        if (::perspectiveRenderer.isInitialized) {
            perspectiveRenderer.resize(width, height, spaceInitHorizontal + (medidaFlechasHorizontal * 3.5f))
        }
    }

    fun timeGetTime(): Long{
        return SystemClock.uptimeMillis()
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

    private fun showBgPads() {
        if(showPadB == 0){
            if(!hideImagesPadA){
                batch.draw(padLefDown, padPositionsHorizontal[0][0], padPositionsHorizontal[0][1], widthBtnsHorizontal, heightBtnsHorizontal)
                batch.draw(padLeftUp, padPositionsHorizontal[1][0], padPositionsHorizontal[1][1], widthBtnsHorizontal, heightBtnsHorizontal)
                batch.draw(padCenter, padPositionsHorizontal[2][0], padPositionsHorizontal[2][1], widthBtnsHorizontal, heightBtnsHorizontal)
                batch.draw(padRightUp, padPositionsHorizontal[3][0], padPositionsHorizontal[3][1], widthBtnsHorizontal, heightBtnsHorizontal)
                batch.draw(padRightDown, padPositionsHorizontal[4][0], padPositionsHorizontal[4][1], widthBtnsHorizontal, heightBtnsHorizontal)

                batch.draw(padLefDown, padPositionsHorizontal[5][0], padPositionsHorizontal[5][1], widthBtnsHorizontal, heightBtnsHorizontal)
                batch.draw(padLeftUp, padPositionsHorizontal[6][0], padPositionsHorizontal[6][1], widthBtnsHorizontal, heightBtnsHorizontal)
                batch.draw(padCenter, padPositionsHorizontal[7][0], padPositionsHorizontal[7][1], widthBtnsHorizontal, heightBtnsHorizontal)
                batch.draw(padRightUp, padPositionsHorizontal[8][0], padPositionsHorizontal[8][1], widthBtnsHorizontal, heightBtnsHorizontal)
                batch.draw(padRightDown, padPositionsHorizontal[9][0], padPositionsHorizontal[9][1], widthBtnsHorizontal, heightBtnsHorizontal)


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
            batch.draw(arrayPad4Bg[0], padPositionsHorizontal[0][0], padPositionsHorizontal[0][1], widthBtnsHorizontal, heightBtnsHorizontal)
            batch.draw(arrayPad4Bg[1], padPositionsHorizontal[1][0], padPositionsHorizontal[1][1], widthBtnsHorizontal, heightBtnsHorizontal)
            batch.draw(arrayPad4Bg[2], padPositionsHorizontal[2][0], padPositionsHorizontal[2][1], widthBtnsHorizontal, heightBtnsHorizontal)
            batch.draw(arrayPad4Bg[3], padPositionsHorizontal[3][0], padPositionsHorizontal[3][1], widthBtnsHorizontal, heightBtnsHorizontal)
            batch.draw(arrayPad4Bg[4], padPositionsHorizontal[4][0], padPositionsHorizontal[4][1], widthBtnsHorizontal, heightBtnsHorizontal)

            batch.draw(arrayPad4Bg[0], padPositionsHorizontal[5][0], padPositionsHorizontal[5][1], widthBtnsHorizontal, heightBtnsHorizontal)
            batch.draw(arrayPad4Bg[1], padPositionsHorizontal[6][0], padPositionsHorizontal[6][1], widthBtnsHorizontal, heightBtnsHorizontal)
            batch.draw(arrayPad4Bg[2], padPositionsHorizontal[7][0], padPositionsHorizontal[7][1], widthBtnsHorizontal, heightBtnsHorizontal)
            batch.draw(arrayPad4Bg[3], padPositionsHorizontal[8][0], padPositionsHorizontal[8][1], widthBtnsHorizontal, heightBtnsHorizontal)
            batch.draw(arrayPad4Bg[4], padPositionsHorizontal[9][0], padPositionsHorizontal[9][1], widthBtnsHorizontal, heightBtnsHorizontal)
        }
    }

    private fun drawRecepts() {
        drawAttackReceptor(recept0Frames[0], 0)
        drawAttackReceptor(recept1Frames[0], 1)
        drawAttackReceptor(recept2Frames[0], 2)
        drawAttackReceptor(recept3Frames[0], 3)
        drawAttackReceptor(recept4Frames[0], 4)
        if (showOverlay) {
            aBatch = batch.blendSrcFunc
            bBatch = batch.blendDstFunc
            batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
            drawAttackOverlay(recept0Frames[1], 0)
            drawAttackOverlay(recept1Frames[1], 1)
            drawAttackOverlay(recept2Frames[1], 2)
            drawAttackOverlay(recept3Frames[1], 3)
            drawAttackOverlay(recept4Frames[1], 4)
            batch.setBlendFunction(aBatch, bBatch)
        }
    }

    private fun getReverseReceptorY(): Float {
        return Gdx.graphics.height.toFloat() -
                targetTop -
                medidaFlechasHorizontal
    }

    private fun getReceptorBaseX(column: Int): Float = spaceInitHorizontal + medidaFlechasHorizontal * (column + 1) + luaRecepts.screenX

    private fun drawAttackReceptor(frame: TextureRegion, column: Int) {
        val x = getReceptorBaseX(column) + getAttackColumnOffsetX(column, 0f)
        val y = getAttackReceptorY(column)
        val scale = getAttackVisualScale() * getAttackMoveZScale(column)
        val rotation = getAttackConfusionRotation(player.beatToShow)
        val oldColor = batch.color.cpy()
        batch.setColor(oldColor.r, oldColor.g, oldColor.b, oldColor.a * getAttackDarkAlpha())
        batch.draw(frame, x, y, medidaFlechasHorizontal * 0.5f, medidaFlechasHorizontal * 0.5f, medidaFlechasHorizontal, medidaFlechasHorizontal, scale, scale, rotation)
        batch.color = oldColor
    }

    private fun drawAttackOverlay(frame: TextureRegion, column: Int) {
        val metrics = receptorOverlayMetrics[column]
        val baseX = getReceptorBaseX(column) + getAttackColumnOffsetX(column, 0f)
        val baseY = getAttackReceptorY(column)
        val x = baseX + medidaFlechasHorizontal * metrics.offsetXRatio
        val y = baseY + medidaFlechasHorizontal * metrics.offsetYRatio
        val w = medidaFlechasHorizontal * metrics.widthRatio
        val h = medidaFlechasHorizontal * metrics.heightRatio
        val originX = baseX + medidaFlechasHorizontal * 0.5f - x
        val originY = baseY + medidaFlechasHorizontal * 0.5f - y
        val scale = getAttackVisualScale() * getAttackMoveZScale(column)
        val rotation = getAttackConfusionRotation(player.beatToShow)
        val oldColor = batch.color.cpy()
        batch.setColor(oldColor.r, oldColor.g, oldColor.b, oldColor.a * getAttackDarkAlpha())
        batch.draw(frame, x, y, originX, originY, w, h, scale, scale, rotation)
        batch.color = oldColor
    }

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
            baseX + medidaFlechasHorizontal * metrics.offsetXRatio,
            targetTop + medidaFlechasHorizontal * metrics.offsetYRatio,
            medidaFlechasHorizontal * metrics.widthRatio,
            medidaFlechasHorizontal * metrics.heightRatio
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

        if (showPadB == 1 || showPadB == 2) {
            padB.texture.dispose()
        }

        if (showPadB == 3) {
            arrayPad4Bg[0].texture.dispose()
            arrayPad4[0].texture.dispose()
        }

        if (::perspectiveRenderer.isInitialized) perspectiveRenderer.dispose()
        player.disposePlayer()
    }

}
