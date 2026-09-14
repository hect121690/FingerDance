package com.fingerdance.ssc.attacks

import kotlin.math.abs

class AttackEngine(
    private val attacks: List<AttackEvent>
) {
    private var current = AttackState()

    fun update(songTimeMs: Double, deltaSeconds: Float): AttackState {
        val songSeconds = songTimeMs / 1000.0
        val target = buildTarget(songSeconds)

        current.boost = approach(current.boost, target.boost, getApproachAmount(target, AttackMod.BOOST, deltaSeconds))
        current.brake = approach(current.brake, target.brake, getApproachAmount(target, AttackMod.BRAKE, deltaSeconds))
        current.wave = approach(current.wave, target.wave, getApproachAmount(target, AttackMod.WAVE, deltaSeconds))
        current.expand = approach(current.expand, target.expand, getApproachAmount(target, AttackMod.EXPAND, deltaSeconds))
        current.boomerang = approach(current.boomerang, target.boomerang, getApproachAmount(target, AttackMod.BOOMERANG, deltaSeconds))

        current.drunk = approach(current.drunk, target.drunk, getApproachAmount(target, AttackMod.DRUNK, deltaSeconds))
        current.dizzy = approach(current.dizzy, target.dizzy, getApproachAmount(target, AttackMod.DIZZY, deltaSeconds))
        current.confusion = approach(current.confusion, target.confusion, getApproachAmount(target, AttackMod.CONFUSION, deltaSeconds))
        current.tornado = approach(current.tornado, target.tornado, getApproachAmount(target, AttackMod.TORNADO, deltaSeconds))
        current.tipsy = approach(current.tipsy, target.tipsy, getApproachAmount(target, AttackMod.TIPSY, deltaSeconds))
        current.bumpy = approach(current.bumpy, target.bumpy, getApproachAmount(target, AttackMod.BUMPY, deltaSeconds))
        current.beat = approach(current.beat, target.beat, getApproachAmount(target, AttackMod.BEAT, deltaSeconds))

        current.twirl = approach(current.twirl, target.twirl, getApproachAmount(target, AttackMod.TWIRL, deltaSeconds))
        current.roll = approach(current.roll, target.roll, getApproachAmount(target, AttackMod.ROLL, deltaSeconds))
        current.mini = approach(current.mini, target.mini, getApproachAmount(target, AttackMod.MINI, deltaSeconds))
        current.reverse = approach(current.reverse, target.reverse, getApproachAmount(target, AttackMod.REVERSE, deltaSeconds))
        current.flip = approach(current.flip, target.flip, getApproachAmount(target, AttackMod.FLIP, deltaSeconds))
        current.invert = approach(current.invert, target.invert, getApproachAmount(target, AttackMod.INVERT, deltaSeconds))
        current.dark = approach(current.dark, target.dark, getApproachAmount(target, AttackMod.DARK, deltaSeconds))
        current.stealth = approach(current.stealth, target.stealth, getApproachAmount(target, AttackMod.STEALTH, deltaSeconds))

        current.skew = approach(current.skew, target.skew, deltaSeconds * DEFAULT_APPROACH_MULTIPLIER * target.skewApproachSpeed)
        current.perspectiveTilt = approach(current.perspectiveTilt, target.perspectiveTilt, deltaSeconds * DEFAULT_APPROACH_MULTIPLIER * target.perspectiveTiltApproachSpeed)

        // MoveZ necesita tween independiente por columna porque *N puede ser distinto
        // en MoveZ1, MoveZ2, MoveZ3, etc.
        val moveZColumns = (current.moveZ.keys + target.moveZ.keys).toSet()
        for (column in moveZColumns) {
            val currentValue = current.getMoveZ(column)
            val targetValue = target.getMoveZ(column)
            val speed = target.getMoveZApproachSpeed(column)
            val amount = deltaSeconds * DEFAULT_APPROACH_MULTIPLIER * speed
            val next = approach(currentValue, targetValue, amount)

            if (abs(next) < EPSILON && abs(targetValue) < EPSILON) {
                current.moveZ.remove(column)
                current.moveZApproachSpeeds.remove(column)
            } else {
                current.moveZ[column] = next
                current.moveZApproachSpeeds[column] = speed
            }
        }

        return current
    }

    private fun buildTarget(songSeconds: Double): AttackState {
        val target = AttackState()

        attacks.asSequence()
            .filter { it.isActive(songSeconds) }
            .sortedBy { it.startSecond }
            .forEach { event ->
                event.modifiers.forEach { raw ->
                    target.apply(AttackModifierParser.parse(raw))
                }
            }

        return target
    }

    private fun getApproachAmount(target: AttackState, mod: AttackMod, deltaSeconds: Float): Float {
        return deltaSeconds * DEFAULT_APPROACH_MULTIPLIER * target.getApproachSpeed(mod)
    }

    private fun approach(current: Float, target: Float, amount: Float): Float {
        if (current == target) return target
        if (amount <= 0f) return current

        val delta = target - current
        if (abs(delta) <= amount) return target

        return current + if (delta > 0f) amount else -amount
    }

    private companion object {
        const val DEFAULT_APPROACH_MULTIPLIER = 4f
        const val EPSILON = 0.0001f
    }
}
