package com.fingerdance.ssc.attacks

import kotlin.math.abs

class AttackEngine(
    private val attacks: List<AttackEvent>
) {
    private var current = AttackState()
    private var xmodInitialized = false

    /**
     * Replica PlayerOptions::Approach() de StepMania:
     *
     *     fapproach(current, target, deltaSeconds * targetApproachSpeed)
     *
     * El valor por defecto de approachSpeed es 1.0. No existe un multiplicador
     * global x4 en StepMania. Los prefijos *N del modifier string cambian
     * targetApproachSpeed mediante AttackModifierParser.
     *
     * Importante: los ATTACKS cortos NO saltan instantaneamente a 100%;
     * tambien pasan por este approach, igual que PlayerOptions::Update().
     */
    fun update(
        songTimeMs: Double,
        deltaSeconds: Float,
        baseScrollSpeed: Float = 1f
    ): AttackState {
        val songSeconds = songTimeMs / 1000.0
        val safeBaseScrollSpeed = baseScrollSpeed.coerceAtLeast(EPSILON)

        // Antes del primer update, m_fScrollSpeed debe partir exactamente del XMod
        // elegido por el jugador, no de 1x.
        if (!xmodInitialized) {
            current.xmod = safeBaseScrollSpeed
            xmodInitialized = true
        }

        val target = buildTarget(songSeconds, safeBaseScrollSpeed)

        current.xmod = approach(
            current.xmod,
            target.xmod,
            getApproachAmount(target, AttackMod.XMOD, deltaSeconds)
        )

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
        current.tiny = approach(current.tiny, target.tiny, getApproachAmount(target, AttackMod.TINY, deltaSeconds))
        current.reverse = approach(current.reverse, target.reverse, getApproachAmount(target, AttackMod.REVERSE, deltaSeconds))
        current.split = approach(current.split, target.split, getApproachAmount(target, AttackMod.SPLIT, deltaSeconds))
        current.alternate = approach(current.alternate, target.alternate, getApproachAmount(target, AttackMod.ALTERNATE, deltaSeconds))
        current.cross = approach(current.cross, target.cross, getApproachAmount(target, AttackMod.CROSS, deltaSeconds))
        current.centered = approach(current.centered, target.centered, getApproachAmount(target, AttackMod.CENTERED, deltaSeconds))
        current.flip = approach(current.flip, target.flip, getApproachAmount(target, AttackMod.FLIP, deltaSeconds))
        current.invert = approach(current.invert, target.invert, getApproachAmount(target, AttackMod.INVERT, deltaSeconds))
        current.dark = approach(current.dark, target.dark, getApproachAmount(target, AttackMod.DARK, deltaSeconds))
        current.stealth = approach(current.stealth, target.stealth, getApproachAmount(target, AttackMod.STEALTH, deltaSeconds))
        current.hidden = approach(current.hidden, target.hidden, getApproachAmount(target, AttackMod.HIDDEN, deltaSeconds))
        current.sudden = approach(current.sudden, target.sudden, getApproachAmount(target, AttackMod.SUDDEN, deltaSeconds))
        current.blink = approach(current.blink, target.blink, getApproachAmount(target, AttackMod.BLINK, deltaSeconds))
        current.randomVanish = approach(current.randomVanish, target.randomVanish, getApproachAmount(target, AttackMod.RANDOM_VANISH, deltaSeconds))
        current.blind = approach(current.blind, target.blind, getApproachAmount(target, AttackMod.BLIND, deltaSeconds))

        current.skew = approach(current.skew, target.skew, deltaSeconds * target.skewApproachSpeed)
        current.perspectiveTilt = approach(current.perspectiveTilt, target.perspectiveTilt, deltaSeconds * target.perspectiveTiltApproachSpeed)

        // MoveZ necesita tween independiente por columna porque *N puede ser distinto
        // en MoveZ1, MoveZ2, MoveZ3, etc.
        val moveZColumns = (current.moveZ.keys + target.moveZ.keys).toSet()
        for (column in moveZColumns) {
            val currentValue = current.getMoveZ(column)
            val targetValue = target.getMoveZ(column)
            val speed = target.getMoveZApproachSpeed(column)
            val amount = deltaSeconds * speed
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

    private fun buildTarget(songSeconds: Double, baseScrollSpeed: Float): AttackState {
        // Sin un ATTACK XMod activo, el target vuelve al XMod base del jugador.
        val target = AttackState(xmod = baseScrollSpeed)

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
        return deltaSeconds * target.getApproachSpeed(mod)
    }

    private fun approach(current: Float, target: Float, amount: Float): Float {
        if (current == target) return target
        if (amount <= 0f) return current

        val delta = target - current
        if (abs(delta) <= amount) return target

        return current + if (delta > 0f) amount else -amount
    }

    private companion object {
        const val EPSILON = 0.0001f
    }
}
