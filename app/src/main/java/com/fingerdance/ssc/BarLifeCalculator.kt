package com.fingerdance.ssc

enum class LifeJudgment {
    PERFECT,
    GREAT,
    GOOD,
    BAD,
    MISS
}

data class BarLifeState(
    var life: Int = BarLifeCalculator.INITIAL_LIFE,
    var gainMultiplier: Double = BarLifeCalculator.INITIAL_MULTIPLIER,
    var comboSinceBreak: Int = 0,
    var failed: Boolean = false
)

class BarLifeCalculator(
    level: Int
) {

    companion object {
        const val VISIBLE_LIFE_MAX = 1000
        const val INITIAL_LIFE = 300

        const val INITIAL_MULTIPLIER = 0.10
        const val MIN_MULTIPLIER = 0.00
        const val MAX_MULTIPLIER = 0.80
    }

    private val safeLevel = level.coerceAtLeast(1)
    private var holdLifeRemainder = 0.0
    val maximumOverflow: Int = 3 * safeLevel * safeLevel
    val maximumLife: Int = VISIBLE_LIFE_MAX + maximumOverflow
    val state = BarLifeState()

    val visibleLife: Int
        get() = state.life.coerceAtMost(VISIBLE_LIFE_MAX)

    val overflowLife: Int
        get() = (
                state.life - VISIBLE_LIFE_MAX
                ).coerceAtLeast(0)

    val visibleProgress: Float
        get() = (
                visibleLife.toFloat() /
                        VISIBLE_LIFE_MAX.toFloat()
                ).coerceIn(0f, 1f)

    val overflowProgress: Float
        get() {
            if (maximumOverflow <= 0) return 0f
            return (overflowLife.toFloat() / maximumOverflow.toFloat()).coerceIn(0f, 1f)
        }

    val isVisibleBarFull: Boolean
        get() = state.life >= VISIBLE_LIFE_MAX

    val isOverflowFull: Boolean
        get() = state.life >= maximumLife

    fun applyJudgment(
        judgment: LifeJudgment,
        isBodyLongNote: Boolean = false,
        isMine: Boolean = false
    ): Int {

        if (state.failed) return 0

        val currentLife = state.life
        val currentMultiplier = state.gainMultiplier

        val lifeChange = when (judgment) {
            LifeJudgment.PERFECT -> {
                if (isBodyLongNote) {
                    calculatePositiveGain(
                        weight = 4,
                        multiplier = currentMultiplier
                    )
                } else {
                    calculatePositiveGain(
                        weight = 12,
                        multiplier = currentMultiplier
                    )
                }
            }

            LifeJudgment.GREAT -> {
                calculatePositiveGain(
                    weight = 10,
                    multiplier = currentMultiplier
                )
            }

            LifeJudgment.GOOD -> {
                0
            }

            LifeJudgment.BAD -> {
                -50
            }

            LifeJudgment.MISS -> {
                if (isMine) {
                    -25
                } else {
                    -calculateMissLoss(currentLife)
                }
            }
        }

        state.life = (
                state.life + lifeChange
                ).coerceIn(
                minimumValue = 0,
                maximumValue = maximumLife
            )

        updateGainMultiplier(
            judgment = judgment,
            isBodyLongNote = isBodyLongNote,
            isMine = isMine
        )

        updateRecoveryCombo(
            judgment = judgment,
            isBodyLongNote = isBodyLongNote,
            isMine = isMine
        )

        state.failed = state.life <= 0

        return lifeChange
    }

    private fun calculatePositiveGain(
        weight: Int,
        multiplier: Double
    ): Int {
        return (weight * multiplier).toInt()
    }

    private fun calculateMissLoss(currentLife: Int): Int {
        val lifeForCalculation = minOf(currentLife, VISIBLE_LIFE_MAX)
        return (lifeForCalculation / 4.0 + 20.0).toInt()
    }

    private fun updateGainMultiplier(judgment: LifeJudgment, isBodyLongNote: Boolean, isMine: Boolean) {
        val change = when {
            isMine -> {
                -0.10
            }

            isBodyLongNote && judgment == LifeJudgment.PERFECT -> {
                0.0
            }

            else -> {
                when (judgment) {
                    LifeJudgment.PERFECT -> 0.020
                    LifeJudgment.GREAT -> 0.016
                    LifeJudgment.GOOD -> 0.0
                    LifeJudgment.BAD -> -0.35
                    LifeJudgment.MISS -> -0.70
                }
            }
        }

        state.gainMultiplier = (
                state.gainMultiplier + change
                ).coerceIn(
                minimumValue = MIN_MULTIPLIER,
                maximumValue = MAX_MULTIPLIER
            )
    }

    private fun calculateHoldTickGain(
        multiplier: Double
    ): Int {
        holdLifeRemainder += multiplier
        val wholeLife = holdLifeRemainder.toInt()
        if (wholeLife > 0) {
            holdLifeRemainder -= wholeLife
        }

        return wholeLife
    }

    private fun updateRecoveryCombo(
        judgment: LifeJudgment,
        isBodyLongNote: Boolean,
        isMine: Boolean
    ) {
        when {
            isMine -> {
                state.comboSinceBreak = 0
            }

            isBodyLongNote && judgment == LifeJudgment.PERFECT -> {
                /*
                 * Los ticks siguen contando en resultSong,
                 * pero no construyen el combo interno de recuperación.
                 */
            }

            judgment == LifeJudgment.PERFECT ||
                    judgment == LifeJudgment.GREAT -> {
                state.comboSinceBreak++
            }

            judgment == LifeJudgment.BAD ||
                    judgment == LifeJudgment.MISS -> {
                state.comboSinceBreak = 0
            }

            judgment == LifeJudgment.GOOD -> {
                // No construye ni destruye recuperación.
            }
        }
    }

    fun reset() {
        state.life = INITIAL_LIFE
        state.gainMultiplier = INITIAL_MULTIPLIER
        state.comboSinceBreak = 0
        state.failed = false
        holdLifeRemainder = 0.0
    }
}