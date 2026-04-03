package com.fingerdance.ssc

import com.fingerdance.chart
import com.fingerdance.ssc.Parser.BpmSegment
import com.fingerdance.ssc.Parser.Stop
import com.fingerdance.ssc.Parser.Warp
import com.fingerdance.ssc.Parser.Speed
import com.fingerdance.ssc.Parser.Scroll
import kotlin.math.max
import kotlin.math.min

class TimmingData(
    val bpms: List<BpmSegment>,
    val stops: List<Stop>,
    val warps: List<Warp>,
    val speeds: List<Speed>,
    val scrolls: List<Scroll>,
    val offsetMs: Double,
    val userOffsetMs: Double
) {
    data class TimeSegment(
        val beatStart: Double,
        val beatEnd: Double,
        val timeStartMs: Double,
        val bpm: Double,
        val isWarp: Boolean
    )

    // Segmentos "reales" para beat <-> tiempo (igual que en PlayerSsc3)
    private val timeSegments: List<TimeSegment> = buildTimeSegments()

    private fun buildTimeSegments(): List<TimeSegment> {
        data class Event(val beat: Double, val type: Int, val value: Double)

        val events = mutableListOf<Event>()
        bpms.forEach { events.add(Event(it.beat, 1, it.bpm)) }
        stops.forEach { events.add(Event(it.beat, 2, it.durationMs)) }
        warps.forEach { events.add(Event(it.beat, 0, it.duration)) }

        events.sortWith(compareBy<Event>({ it.beat }, { it.type }))

        val result = mutableListOf<TimeSegment>()

        var currentBeat = 0.0
        var currentTimeMs = (chart.offset * 1000.0) + offsetMs + userOffsetMs
        var currentBpm = bpms.firstOrNull()?.bpm ?: 120.0

        fun addSegment(nextBeat: Double) {
            if (nextBeat <= currentBeat) return
            result.add(
                TimeSegment(
                    beatStart = currentBeat,
                    beatEnd = nextBeat,
                    timeStartMs = currentTimeMs,
                    bpm = currentBpm,
                    isWarp = false
                )
            )
            val deltaBeat = nextBeat - currentBeat
            currentTimeMs += (deltaBeat / currentBpm) * 60000.0
            currentBeat = nextBeat
        }

        for (e in events) {
            addSegment(e.beat)

            when (e.type) {
                // Warp: salta beats sin consumir tiempo
                0 -> {
                    val warpEnd = e.beat + e.value
                    result.add(
                        TimeSegment(
                            beatStart = e.beat,
                            beatEnd = warpEnd,
                            timeStartMs = currentTimeMs,
                            bpm = currentBpm,
                            isWarp = true
                        )
                    )
                    currentBeat = warpEnd
                }
                // BPM change
                1 -> currentBpm = e.value
                // Stop
                2 -> currentTimeMs += e.value
            }
        }

        result.add(
            TimeSegment(
                beatStart = currentBeat,
                beatEnd = Double.POSITIVE_INFINITY,
                timeStartMs = currentTimeMs,
                bpm = currentBpm,
                isWarp = false
            )
        )

        return result
    }

    private fun findSegmentByBeat(beat: Double): TimeSegment {
        if (beat <= timeSegments.first().beatStart) return timeSegments.first()
        if (beat >= timeSegments.last().beatStart) return timeSegments.last()
        return timeSegments.last { beat >= it.beatStart }
    }

    private fun findSegmentByTime(timeMs: Double): TimeSegment {
        if (timeMs <= timeSegments.first().timeStartMs) return timeSegments.first()
        if (timeMs >= timeSegments.last().timeStartMs) return timeSegments.last()
        return timeSegments.last { timeMs >= it.timeStartMs }
    }

    /** Beat "real" → tiempo real en ms (aplica BPM, stops, warps, offset+userOffset). */
    fun beatToTime(beat: Double): Double {
        val seg = findSegmentByBeat(beat)
        if (seg.isWarp) {
            // dentro de un warp, el tiempo no avanza
            return seg.timeStartMs
        }
        val deltaBeat = beat - seg.beatStart
        return seg.timeStartMs + (deltaBeat / seg.bpm) * 60000.0
    }

    /** Tiempo real en ms → beat "real" (aplica BPM, stops, warps, offset+userOffset). */
    fun timeToBeat(timeMs: Double): Double {
        val seg = findSegmentByTime(timeMs)
        if (seg.isWarp) {
            // mientras dura el warp, el beat visible salta directamente al final
            return seg.beatEnd
        }
        val deltaMs = timeMs - seg.timeStartMs
        return seg.beatStart + (deltaMs / 60000.0) * seg.bpm
    }

    /** Beat mostrado (aplica SCROLL tipo ratio acumulado, como StepMania). */
    fun getDisplayedBeat(rawBeat: Double): Double {
        // scrollSegments: cada uno tiene beat y ratio. Ratio multiplica la "velocidad visual" del beat.
        if (scrolls.isEmpty()) return rawBeat

        var lastBeat = 0.0
        var displayed = 0.0
        var lastRatio = 1.0

        // Recorremos todos los scrolls <= rawBeat
        val sortedScrolls = scrolls.sortedBy { it.beat }
        for (s in sortedScrolls) {
            if (s.beat >= rawBeat) break
            val segmentStart = lastBeat
            val segmentEnd = min(rawBeat, s.beat)
            val delta = segmentEnd - segmentStart
            displayed += delta * lastRatio

            lastBeat = s.beat
            lastRatio = s.ratio
        }

        // Último tramo hasta rawBeat
        if (rawBeat > lastBeat) {
            val delta = rawBeat - lastBeat
            displayed += delta * lastRatio
        }

        return displayed
    }

    /**
     * Velocidad visible (XMod variable) en forma de porcentaje:
     *   - visibleBeat: beat ya transformado por scroll
     *   - visibleTimeMs: tiempo real en ms visible
     *   Retorna multiplicador de velocidad (1.0 = normal).
     */
    fun getDisplayedSpeedPercent(visibleBeat: Double, visibleTimeMs: Double): Double {
        if (speeds.isEmpty()) return 1.0

        // Similar a TimingData::GetDisplayedSpeedPercent en StepMania:
        // baseRate se acumula multiplicando rate según beat.
        var result = 1.0
        var lastBeat = 0.0
        var lastRate = 1.0

        val sortedSpeeds = speeds.sortedBy { it.beat }

        for (seg in sortedSpeeds) {
            if (seg.beat > visibleBeat) break

            lastBeat = seg.beat
            lastRate = seg.ratio
        }

        // En esta versión simple, devolvemos el último rate aplicable.
        // Si quieres algo más complejo (unit=SECOND vs BEAT), puedes extender:
        result = lastRate

        return result
    }

    /**
     * Helper principal para render:
     *   - noteBeat: beat de la nota (real)
     *   - songVisibleBeat: beat real actual de la canción
     *   - songVisibleTimeMs: tiempo actual de la canción en ms
     *   - stepSize: tamaño base por beat (ej: medidaFlechas)
     *
     * Devuelve el Y-offset (distancia vertical desde la línea objetivo,
     * sin sumar todavía la coordenada Y de los receptores).
     */
    fun getYOffsetForBeat(
        noteBeat: Double,
        songVisibleBeat: Double,
        songVisibleTimeMs: Double,
        stepSize: Float
    ): Float {
        // 1) Convertimos beat reales a beats "mostrados" (aplica scroll)
        val noteDispBeat = getDisplayedBeat(noteBeat)
        val songDispBeat = getDisplayedBeat(songVisibleBeat)

        // 2) Diferencia de beats mostrados
        val deltaBeatDisp = noteDispBeat - songDispBeat

        // 3) Velocidad visible actual (aplica speed mods)
        val speedPercent = getDisplayedSpeedPercent(songDispBeat, songVisibleTimeMs)
        val gapPerBeat = stepSize * speedPercent.toFloat()

        return (deltaBeatDisp * gapPerBeat).toFloat()
    }
}