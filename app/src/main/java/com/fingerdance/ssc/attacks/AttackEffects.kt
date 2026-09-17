package com.fingerdance.ssc.attacks

import com.fingerdance.isVertical
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.pow

object AttackEffects {

    // =========================================================
    // STEPMANIA FALLBACK METRICS
    // =========================================================

    private const val DRUNK_COLUMN_FREQUENCY = 0.2f
    private const val DRUNK_OFFSET_FREQUENCY = 10f
    private const val DRUNK_ARROW_MAGNITUDE = 0.5f

    private const val TIPSY_TIMER_FREQUENCY = 1.2f
    private const val TIPSY_COLUMN_FREQUENCY = 1.8f
    private const val TIPSY_ARROW_MAGNITUDE = 0.4f

    private const val TORNADO_POSITION_SCALE_LOW = -1f
    private const val TORNADO_POSITION_SCALE_HIGH = 1f
    private const val TORNADO_OFFSET_FREQUENCY = 6f
    private const val TORNADO_OFFSET_SCALE_LOW = -1f
    private const val TORNADO_OFFSET_SCALE_HIGH = 1f

    private const val MINI_PERCENT_BASE = 0.5f
    private const val MINI_MAX_SCALE_VERTICAL = 1.12f
    private const val MINI_MIN_SCALE_VERTICAL = 0.65f

    private const val TINY_PERCENT_BASE = 0.5f
    private const val TINY_PERCENT_GATE = 1f

    private const val BEAT_OFFSET_HEIGHT = 15f
    private const val BEAT_PI_HEIGHT = 2f

    // =========================================================
    // STEPMANIA 5.1 ACCEL FALLBACK METRICS
    // Themes/_fallback/metrics.ini
    // =========================================================

    private const val BOOST_MOD_MIN_CLAMP = -400f
    private const val BOOST_MOD_MAX_CLAMP = 400f
    private const val BRAKE_MOD_MIN_CLAMP = -400f
    private const val BRAKE_MOD_MAX_CLAMP = 400f
    private const val WAVE_MOD_MAGNITUDE = 20f
    private const val WAVE_MOD_HEIGHT = 38f
    private const val BOOMERANG_PEAK_PERCENTAGE = 0.75f

    private const val EXPAND_MULTIPLIER_FREQUENCY = 3f
    private const val EXPAND_MULTIPLIER_SCALE_TO_LOW = 0.75f
    private const val EXPAND_MULTIPLIER_SCALE_TO_HIGH = 1.75f

    // =========================================================
    // BOOST / BRAKE / WAVE / BOOMERANG / EXPAND
    // =========================================================

    /**
     * StepMania 5.1 ACCEL_BOOST.
     *
     * Devuelve el yOffset transformado por Boost únicamente. En el pipeline
     * combinado se usa la diferencia respecto al yOffset original para sumar
     * Boost + Brake + Wave exactamente como ArrowEffects::GetYOffset().
     */
    fun boostYOffset(
        yOffset: Float,
        effectHeight: Float,
        amount: Float
    ): Float {
        if (!AttackFeatureFlags.AccelScroll.BOOST) return yOffset
        if (amount == 0f || yOffset < 0f) return yOffset

        val safeEffectHeight = effectHeight.coerceAtLeast(1f)
        val denominator = (yOffset + safeEffectHeight / 1.2f) / safeEffectHeight

        if (kotlin.math.abs(denominator) < 0.0001f) return yOffset

        val newYOffset = yOffset * 1.5f / denominator
        val adjustment =
            (amount * (newYOffset - yOffset))
                .coerceIn(BOOST_MOD_MIN_CLAMP, BOOST_MOD_MAX_CLAMP)

        return yOffset + adjustment
    }

    /**
     * StepMania 5.1 ACCEL_BRAKE.
     *
     * fScale = SCALE(yOffset, 0, effectHeight, 0, 1)
     * fNewYOffset = yOffset * fScale
     * adjustment = amount * (fNewYOffset - yOffset), clamp -400..400.
     */
    fun brakeYOffset(
        yOffset: Float,
        effectHeight: Float,
        amount: Float
    ): Float {
        if (!AttackFeatureFlags.AccelScroll.BRAKE) return yOffset
        if (amount == 0f || yOffset < 0f) return yOffset

        val safeEffectHeight = effectHeight.coerceAtLeast(1f)
        val scale = yOffset / safeEffectHeight
        val newYOffset = yOffset * scale
        val adjustment =
            (amount * (newYOffset - yOffset))
                .coerceIn(BRAKE_MOD_MIN_CLAMP, BRAKE_MOD_MAX_CLAMP)

        return yOffset + adjustment
    }

    /**
     * StepMania 5.1 ACCEL_WAVE.
     *
     * Devuelve sólo el ajuste que Wave suma a fYAdjust.
     */
    fun waveYOffsetAdjustment(
        yOffset: Float,
        amount: Float,
        period: Float = 0f
    ): Float {
        if (!AttackFeatureFlags.AccelScroll.WAVE) return 0f
        if (amount == 0f || yOffset < 0f) return 0f

        val denominator = (period * WAVE_MOD_HEIGHT) + WAVE_MOD_HEIGHT
        if (kotlin.math.abs(denominator) < 0.0001f) return 0f

        return amount *
                WAVE_MOD_MAGNITUDE *
                kotlin.math.sin(yOffset / denominator)
    }

    /**
     * StepMania 5.1 ACCEL_BOOMERANG.
     *
     * En ArrowEffects.cpp la cantidad no escala la parábola: cualquier valor
     * distinto de cero activa la transformación completa. Esto replica ese
     * comportamiento. Se aplica DESPUÉS de Boost + Brake + Wave y ANTES del
     * scroll speed / Expand.
     */
    fun boomerangYOffset(
        yOffset: Float,
        screenHeight: Float,
        amount: Float
    ): Float {
        if (!AttackFeatureFlags.AccelScroll.BOOMERANG) return yOffset
        if (amount == 0f || yOffset < 0f) return yOffset

        val safeScreenHeight = screenHeight.coerceAtLeast(1f)
        return (-yOffset * yOffset / safeScreenHeight) + (1.5f * yOffset)
    }

    /**
     * StepMania 5.1 ACCEL_EXPAND.
     *
     * Expand NO cambia el tamaño de la flecha. Modifica el scroll speed
     * mediante un multiplicador oscilante.
     */
    fun expandScrollMultiplier(
        expandSeconds: Float,
        amount: Float,
        period: Float = 0f
    ): Float {
        if (!AttackFeatureFlags.AccelScroll.EXPAND) return 1f
        if (amount == 0f) return 1f

        val phase =
            expandSeconds *
                    EXPAND_MULTIPLIER_FREQUENCY *
                    (period + 1f)

        val cosine = kotlin.math.cos(phase)

        val expandMultiplier =
            EXPAND_MULTIPLIER_SCALE_TO_LOW +
                    ((cosine + 1f) * 0.5f) *
                    (EXPAND_MULTIPLIER_SCALE_TO_HIGH - EXPAND_MULTIPLIER_SCALE_TO_LOW)

        return 1f + amount * (expandMultiplier - 1f)
    }

    /**
     * Orden equivalente a ArrowEffects::GetYOffset() para los ACCEL soportados:
     *
     * 1) Boost, Brake y Wave calculan ajustes usando el MISMO yOffset original
     *    y esos ajustes se suman.
     * 2) Boomerang transforma el yOffset ya ajustado.
     * 3) Expand modifica fScrollSpeed.
     * 4) Al final se multiplica yOffset * fScrollSpeed.
     *
     * Finger Dance ya trae su scroll base convertido a píxeles antes de entrar
     * aquí; GameScreenSsc lo deshace y vuelve a aplicarlo en este punto.
     */
    fun transformAccelYOffsetSm(
        yOffsetSm: Float,
        effectHeightSm: Float,
        expandSeconds: Float,
        boostAmount: Float,
        brakeAmount: Float,
        waveAmount: Float,
        boomerangAmount: Float,
        expandAmount: Float,
        baseScrollSpeed: Float
    ): Float {
        val safeBaseSpeed = baseScrollSpeed.coerceAtLeast(0.0001f)

        // StepMania: no aplica ACCEL después de cruzar el receptor.
        if (yOffsetSm < 0f) return yOffsetSm * safeBaseSpeed

        val originalYOffset = yOffsetSm
        var yAdjust = 0f

        if (AttackFeatureFlags.AccelScroll.BOOST && boostAmount != 0f) {
            yAdjust +=
                boostYOffset(
                    yOffset = originalYOffset,
                    effectHeight = effectHeightSm,
                    amount = boostAmount
                ) - originalYOffset
        }

        if (AttackFeatureFlags.AccelScroll.BRAKE && brakeAmount != 0f) {
            yAdjust +=
                brakeYOffset(
                    yOffset = originalYOffset,
                    effectHeight = effectHeightSm,
                    amount = brakeAmount
                ) - originalYOffset
        }

        if (AttackFeatureFlags.AccelScroll.WAVE && waveAmount != 0f) {
            yAdjust +=
                waveYOffsetAdjustment(
                    yOffset = originalYOffset,
                    amount = waveAmount
                )
        }

        var result = originalYOffset + yAdjust

        if (AttackFeatureFlags.AccelScroll.BOOMERANG && boomerangAmount != 0f) {
            result = boomerangYOffset(
                yOffset = result,
                screenHeight = FieldMetrics.SM_HEIGHT,
                amount = boomerangAmount
            )
        }

        var scrollSpeed = safeBaseSpeed

        if (AttackFeatureFlags.AccelScroll.EXPAND && expandAmount != 0f) {
            scrollSpeed *= expandScrollMultiplier(
                expandSeconds = expandSeconds,
                amount = expandAmount
            )
        }

        return result * scrollSpeed
    }

    // =========================================================
    // DRUNK
    // =========================================================

    fun drunkX(
        column: Int,
        yOffset: Float,
        songTimeSeconds: Float,
        arrowSize: Float,
        screenHeight: Float,
        amount: Float,
        speed: Float = 0f,
        offset: Float = 0f,
        period: Float = 0f
    ): Float {
        if (!AttackFeatureFlags.Position.DRUNK) return 0f


        if (amount == 0f) {
            return 0f
        }

        val angle =
            calculateDrunkAngle(
                songTimeSeconds = songTimeSeconds,
                speed = speed,
                column = column,
                offset = offset,
                columnFrequency = DRUNK_COLUMN_FREQUENCY,
                yOffset = yOffset,
                period = period,
                offsetFrequency = DRUNK_OFFSET_FREQUENCY,
                screenHeight = screenHeight
            )

        return amount *
                cos(angle) *
                arrowSize *
                DRUNK_ARROW_MAGNITUDE
    }

    private fun calculateDrunkAngle(
        songTimeSeconds: Float,
        speed: Float,
        column: Int,
        offset: Float,
        columnFrequency: Float,
        yOffset: Float,
        period: Float,
        offsetFrequency: Float,
        screenHeight: Float
    ): Float {

        val safeHeight = screenHeight.coerceAtLeast(1f)

        return songTimeSeconds * (1f + speed) +
                column * ((offset * columnFrequency) +
                columnFrequency) +
                yOffset * ((period * offsetFrequency) +
                offsetFrequency) / safeHeight
    }

    // =========================================================
    // TIPSY
    // =========================================================

    fun tipsyY(
        column: Int,
        songTimeSeconds: Float,
        arrowSize: Float,
        amount: Float,
        speed: Float = 0f,
        offset: Float = 0f
    ): Float {
        if (!AttackFeatureFlags.Position.TIPSY) return 0f

        if (amount == 0f) return 0f

        val time = songTimeSeconds
        val timeTimesTimer = time * ((speed * TIPSY_TIMER_FREQUENCY) + TIPSY_TIMER_FREQUENCY)
        val angle = timeTimesTimer + column * ((offset * TIPSY_COLUMN_FREQUENCY) + TIPSY_COLUMN_FREQUENCY)

        return amount * kotlin.math.cos(angle) * arrowSize * TIPSY_ARROW_MAGNITUDE
    }

    // =========================================================
    // FLIPX
    // =========================================================

    fun flipX(
        column: Int,
        columnCount: Int,
        arrowSize: Float,
        amount: Float
    ): Float {
        if (!AttackFeatureFlags.DirectionColumn.FLIP) return 0f

        if (amount == 0f || columnCount <= 1) return 0f

        val lastColumn = columnCount - 1
        val newColumn = lastColumn - column

        val oldX = arrowSize * (column + 1)
        val newX = arrowSize * (newColumn + 1)

        return (newX - oldX) * amount
    }

    // =========================================================
    // DIZZY
    // =========================================================

    fun dizzyRotation(
        noteBeat: Double,
        currentBeat: Double,
        amount: Float
    ): Float {
        if (!AttackFeatureFlags.Rotation3D.DIZZY) return 0f

        if (amount == 0f) return 0f

        var rotation = (noteBeat - currentBeat).toFloat()
        rotation *= amount
        rotation %= (2f * Math.PI.toFloat())

        return rotation * (180f / Math.PI.toFloat())
    }

    // =========================================================
    // CONFUCIONROTATION
    // =========================================================

    fun confusionRotation(
        currentBeat: Double,
        amount: Float
    ): Float {
        if (!AttackFeatureFlags.Rotation3D.CONFUSION) return 0f

        if (amount == 0f) return 0f

        var rotation = currentBeat.toFloat()
        rotation *= amount
        rotation %= (2f * Math.PI.toFloat())

        return rotation * (-180f / Math.PI.toFloat())
    }

    // =========================================================
    // TORNADO
    // =========================================================

    fun tornadoX(
        column: Int,
        columnCount: Int,
        yOffset: Float,
        arrowSize: Float,
        screenHeight: Float,
        amount: Float,
        effectOffset: Float = 0f,
        period: Float = 0f
    ): Float {
        if (!AttackFeatureFlags.Position.TORNADO) return 0f

        if (amount == 0f || columnCount <= 1 || column !in 0 until columnCount) return 0f

        /*
         * StepMania:
         * fields > 4 columnas => width = 2.
         *
         * En nuestro caso Pump 5 panel:
         *
         * col 0 -> columnas 0..2
         * col 1 -> columnas 0..3
         * col 2 -> columnas 0..4
         * col 3 -> columnas 1..4
         * col 4 -> columnas 2..4
         */
        val width = if (columnCount > 4) 2 else 3
        val startColumn = (column - width).coerceAtLeast(0)
        val endColumn = (column + width).coerceAtMost(columnCount - 1)

        val realPixelOffset = getTornadoColumnX(column, columnCount, arrowSize)
        val minPixelOffset = getTornadoColumnX(startColumn, columnCount, arrowSize)
        val maxPixelOffset = getTornadoColumnX(endColumn, columnCount, arrowSize)

        if (maxPixelOffset == minPixelOffset) return 0f

        val positionBetween = scale(
            value = realPixelOffset,
            fromLow = minPixelOffset,
            fromHigh = maxPixelOffset,
            toLow = TORNADO_POSITION_SCALE_LOW,
            toHigh = TORNADO_POSITION_SCALE_HIGH
        ).coerceIn(-1f, 1f)

        var radians = acos(positionBetween)

        val safeHeight = screenHeight.coerceAtLeast(1f)

        radians +=
            (yOffset + effectOffset) *
                    ((period * TORNADO_OFFSET_FREQUENCY) +
                            TORNADO_OFFSET_FREQUENCY) / safeHeight

        val processedRadians = cos(radians)

        val adjustedPixelOffset = scale(
            value = processedRadians,
            fromLow = TORNADO_OFFSET_SCALE_LOW,
            fromHigh = TORNADO_OFFSET_SCALE_HIGH,
            toLow = minPixelOffset,
            toHigh = maxPixelOffset
        )

        return (adjustedPixelOffset - realPixelOffset) * amount
    }

    private fun getTornadoColumnX(column: Int, columnCount: Int, arrowSize: Float): Float {
        val center = (columnCount - 1) * 0.5f
        return (column - center) * arrowSize
    }

    private fun scale(value: Float, fromLow: Float, fromHigh: Float, toLow: Float, toHigh: Float): Float {
        if (fromHigh == fromLow) return toLow

        val percent = (value - fromLow) / (fromHigh - fromLow)
        return toLow + percent * (toHigh - toLow)
    }

    // =========================================================
    // REVERSE / SPLIT / ALTERNATE / CROSS / CENTERED
    // =========================================================

    /**
     * Replica PlayerOptions::GetReversePercentForColumn().
     * Los porcentajes de Reverse, Split, Alternate y Cross se suman por columna
     * y después se pliegan al rango 0..1 igual que StepMania.
     */
    fun reversePercentForColumn(
        column: Int,
        columnCount: Int,
        reverse: Float,
        split: Float,
        alternate: Float,
        cross: Float
    ): Float {
        if (column !in 0 until columnCount || columnCount <= 0) return 0f

        var value = reverse

        if (column >= columnCount / 2) value += split
        if ((column % 2) == 1) value += alternate

        val firstCrossColumn = columnCount / 4
        val lastCrossColumn = columnCount - 1 - firstCrossColumn
        if (column in firstCrossColumn..lastCrossColumn) value += cross

        if (value > 2f) value %= 2f
        if (value > 1f) value = 2f - value

        return value
    }

    /**
     * Equivalente 2D de ArrowGetReverseShiftAndScale + GetYPos.
     * CENTERED sólo reduce el shift hacia el punto medio; no modifica el scale
     * de Reverse, exactamente como StepMania.
     */
    fun directionY(
        y: Float,
        normalReceptorY: Float,
        reverseReceptorY: Float,
        reverseAmount: Float,
        centeredAmount: Float
    ): Float {
        val distance = reverseReceptorY - normalReceptorY
        val center = (normalReceptorY + reverseReceptorY) * 0.5f
        val scaleY = 1f - 2f * reverseAmount
        val shiftFromCenter =
            (reverseAmount - 0.5f) * distance * (1f - centeredAmount)
        val sourceOffset = y - normalReceptorY

        return center + shiftFromCenter + sourceOffset * scaleY
    }

    // =========================================================
    // REVERSE
    // =========================================================

    fun reverseReceptorY(screenHeight: Float, arrowSize: Float): Float {
        return if (isVertical) {
            (screenHeight / 2f) - arrowSize
        } else {
            screenHeight - arrowSize
        }
    }

    fun reverseY(y: Float, normalReceptorY: Float, reverseReceptorY: Float, amount: Float): Float {
        if (!AttackFeatureFlags.DirectionColumn.REVERSE) return y

        if (amount == 0f) return y

        val reversedY =
            reverseReceptorY - (y - normalReceptorY)

        return y + (reversedY - y) * amount
    }

    // =========================================================
    // INVERT
    // =========================================================

    fun invertX(column: Int, columnCount: Int, arrowSize: Float, amount: Float): Float {
        if (!AttackFeatureFlags.DirectionColumn.INVERT) return 0f

        if (amount == 0f || column !in 0 until columnCount || columnCount <= 1) return 0f

        val leftOfMiddle = (columnCount - 1) / 2
        val rightOfMiddle = (columnCount + 1) / 2

        val firstColumn: Int
        val lastColumn: Int

        if (column <= leftOfMiddle) {
            firstColumn = 0
            lastColumn = leftOfMiddle
        } else {
            firstColumn = rightOfMiddle
            lastColumn = columnCount - 1
        }

        val newColumn = firstColumn + lastColumn - column

        val oldX = arrowSize * column
        val newX = arrowSize * newColumn

        return (newX - oldX) * amount
    }

    // =========================================================
    // MINI
    // =========================================================

    fun miniScale(amount: Float): Float {
        if (!AttackFeatureFlags.Scale.MINI) return 1f

        val scale =
            1f - (amount * MINI_PERCENT_BASE)

        return if (isVertical) {
            scale.coerceIn(
                MINI_MIN_SCALE_VERTICAL,
                MINI_MAX_SCALE_VERTICAL
            )
        } else {
            scale
        }
    }

    fun miniX(
        x: Float,
        centerX: Float,
        amount: Float
    ): Float {
        if (!AttackFeatureFlags.Scale.MINI) return x

        val scale = miniScale(amount)

        return centerX +
                (x - centerX) * scale
    }

    // =========================================================
    // TINY
    // =========================================================

    /** StepMania GetZoom(): zoom *= pow(0.5, Tiny). */
    fun tinyScale(amount: Float): Float {
        if (!AttackFeatureFlags.Scale.TINY) return 1f
        return TINY_PERCENT_BASE.pow(amount)
    }

    /**
     * StepMania GetXPos(): Tiny puede juntar tracks pero no separarlos.
     * fTinyPercent = min(pow(TinyPercentBase, amount), TinyPercentGate).
     */
    fun tinyX(
        x: Float,
        centerX: Float,
        amount: Float
    ): Float {
        if (!AttackFeatureFlags.Scale.TINY) return x

        val xScale =
            kotlin.math.min(
                TINY_PERCENT_BASE.pow(amount),
                TINY_PERCENT_GATE
            )

        return centerX + (x - centerX) * xScale
    }


    // =========================================================
    // RECEPTOR ORBIT (ADAPTACION VISUAL FINGER DANCE)
    // =========================================================

// =========================================================
    // BEATX
    // =========================================================

    fun beatX(
        yOffset: Float,
        currentBeat: Double,
        amount: Float,
        period: Float = 0f,
        beatOffset: Float = 0f,
        beatMult: Float = 0f
    ): Float {
        if (!AttackFeatureFlags.Position.BEAT) return 0f

        if (amount == 0f) return 0f

        val accelTime = 0.2f
        val totalTime = 0.5f

        var beat =
            ((currentBeat.toFloat() + accelTime + beatOffset) *
                    (beatMult + 1f))

        val evenBeat =
            (beat.toInt() % 2) != 0

        if (beat < 0f) return 0f

        beat -= kotlin.math.truncate(beat)
        beat += 1f
        beat -= kotlin.math.truncate(beat)

        if (beat >= totalTime) {
            return 0f
        }

        var beatFactor =
            if (beat < accelTime) {
                val t =
                    scale(
                        value = beat,
                        fromLow = 0f,
                        fromHigh = accelTime,
                        toLow = 0f,
                        toHigh = 1f
                    )
                t * t
            } else {
                var t =
                    scale(
                        value = beat,
                        fromLow = accelTime,
                        fromHigh = totalTime,
                        toLow = 1f,
                        toHigh = 0f
                    )
                t = 1f - (1f - t) * (1f - t)
                t
            }

        if (evenBeat) {
            beatFactor *= -1f
        }

        beatFactor *= 20f

        val shift =
            beatFactor *
                    kotlin.math.sin(
                        yOffset / ((period * BEAT_OFFSET_HEIGHT) +
                                BEAT_OFFSET_HEIGHT) + Math.PI.toFloat() / BEAT_PI_HEIGHT)

        return amount * shift
    }
}
