package com.fingerdance.ssc.attacks

import kotlin.math.abs

object AttackModifierParser {

    private val speedRegex = Regex("""^\s*\*\s*([+-]?\d+(?:\.\d+)?)\s*(.*)$""")
    private val percentRegex = Regex("""^\s*([+-]?\d+(?:\.\d+)?)\s*%\s*(.+)$""")
    private val bareNumberRegex = Regex("""^\s*([+-]?\d+(?:\.\d+)?)\s+(.+)$""")
    private val noRegex = Regex("""^\s*No\s+(.+)$""", RegexOption.IGNORE_CASE)
    private val moveZRegex = Regex("""^movez(\d+)$""", RegexOption.IGNORE_CASE)

    fun parse(rawModifier: String): AttackModifier {
        val raw = rawModifier.trim()
        if (raw.isEmpty()) {
            return AttackModifier(AttackMod.UNKNOWN, raw = raw)
        }

        var remainder = raw
        var approachSpeed = 1f

        speedRegex.matchEntire(remainder)?.let { match ->
            approachSpeed = match.groupValues[1].toFloatOrNull() ?: 1f
            remainder = match.groupValues[2].trim()
        }

        var isNo = false
        noRegex.matchEntire(remainder)?.let { match ->
            isNo = true
            remainder = match.groupValues[1].trim()
        }

        var level = if (isNo) 0f else 1f

        percentRegex.matchEntire(remainder)?.let { match ->
            level = (match.groupValues[1].toFloatOrNull() ?: 100f) / 100f
            remainder = match.groupValues[2].trim()
        } ?: run {
            // Algunos charts traen, por ejemplo, "-100 Confusion" sin '%'.
            // Para valores con magnitud > 1 los interpretamos como porcentaje.
            bareNumberRegex.matchEntire(remainder)?.let { match ->
                val number = match.groupValues[1].toFloatOrNull()
                if (number != null) {
                    level = if (abs(number) > 1f) number / 100f else number
                    remainder = match.groupValues[2].trim()
                }
            }
        }

        if (isNo) level = 0f

        val moveZMatch = moveZRegex.matchEntire(remainder)
        if (moveZMatch != null) {
            val oneBasedColumn = moveZMatch.groupValues[1].toIntOrNull()
            val column = oneBasedColumn?.minus(1)
            return AttackModifier(
                type = if (column != null && column >= 0) AttackMod.MOVE_Z else AttackMod.UNKNOWN,
                level = level,
                approachSpeed = approachSpeed,
                raw = raw,
                column = column
            )
        }

        val type = when (remainder.lowercase()) {
            "boost" -> AttackMod.BOOST
            "brake" -> AttackMod.BRAKE
            "wave" -> AttackMod.WAVE
            "expand" -> AttackMod.EXPAND
            "boomerang" -> AttackMod.BOOMERANG
            "drunk" -> AttackMod.DRUNK
            "dizzy" -> AttackMod.DIZZY
            "confusion" -> AttackMod.CONFUSION
            "tornado" -> AttackMod.TORNADO
            "tipsy" -> AttackMod.TIPSY
            "bumpy" -> AttackMod.BUMPY
            "beat" -> AttackMod.BEAT
            "twirl" -> AttackMod.TWIRL
            "roll" -> AttackMod.ROLL
            "mini" -> AttackMod.MINI
            "overhead" -> AttackMod.OVERHEAD
            "hallway" -> AttackMod.HALLWAY
            "distant" -> AttackMod.DISTANT
            "incoming" -> AttackMod.INCOMING
            "space" -> AttackMod.SPACE
            "reverse" -> AttackMod.REVERSE
            "flip" -> AttackMod.FLIP
            "invert" -> AttackMod.INVERT
            "dark" -> AttackMod.DARK
            "stealth" -> AttackMod.STEALTH
            "hidden" -> AttackMod.HIDDEN
            "sudden" -> AttackMod.SUDDEN
            "blink" -> AttackMod.BLINK
            "blind" -> AttackMod.BLIND
            "normal" -> AttackMod.NORMAL
            else -> AttackMod.UNKNOWN
        }

        return AttackModifier(
            type = type,
            level = level,
            approachSpeed = approachSpeed,
            raw = raw
        )
    }
}
