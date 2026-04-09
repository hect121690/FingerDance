package com.fingerdance.ssc

import com.fingerdance.ssc.Parser.BpmSegment
import com.fingerdance.ssc.Parser.Stop
import com.fingerdance.ssc.Parser.Warp
import com.fingerdance.ssc.Parser.Speed
import com.fingerdance.ssc.Parser.Scroll
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
        var currentTimeMs = offsetMs + userOffsetMs
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

    fun isBeatInWarp(beat: Double): Boolean {
        val seg = timeSegments.lastOrNull { beat >= it.beatStart }
        return seg?.isWarp == true && beat < seg.beatEnd
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

    data class ScrollSegmentInternal(
        val beatStart: Double,
        val beatEnd: Double,
        val ratio: Double
    )

    private val scrollSegments: List<ScrollSegmentInternal> = buildScrollSegments()

    private fun buildScrollSegments(): List<ScrollSegmentInternal> {
        if (scrolls.isEmpty()) {
            // un único segmento [0, ∞) con ratio 1
            return listOf(ScrollSegmentInternal(0.0, Double.POSITIVE_INFINITY, 1.0))
        }

        val sorted = scrolls.sortedBy { it.beat }
        val result = mutableListOf<ScrollSegmentInternal>()

        var prevBeat = 0.0
        var prevRatio = 1.0

        for (s in sorted) {
            val beat = s.beat
            if (beat > prevBeat) {
                result.add(
                    ScrollSegmentInternal(
                        beatStart = prevBeat,
                        beatEnd = beat,
                        ratio = prevRatio
                    )
                )
            }
            prevBeat = beat
            prevRatio = s.ratio
        }

        result.add(
            ScrollSegmentInternal(
                beatStart = prevBeat,
                beatEnd = Double.POSITIVE_INFINITY,
                ratio = prevRatio
            )
        )

        return result
    }

    /** Beat mostrado (aplica SCROLL tipo ratio acumulado, como StepMania). */
    fun getDisplayedBeat(rawBeat: Double): Double {
        var displayed = 0.0
        val b = rawBeat

        for (seg in scrollSegments) {
            if (b <= seg.beatStart) break

            val segEnd = min(b, seg.beatEnd)
            if (segEnd > seg.beatStart) {
                val delta = segEnd - seg.beatStart
                displayed += delta * seg.ratio
            }

            if (b <= seg.beatEnd) break
        }

        return displayed
    }

    fun getDisplayedSpeedPercent(rawBeat: Double, rawTimeMs: Double): Double {
        if (speeds.isEmpty()) return 1.0

        val sorted = speeds.sortedBy { it.beat }

        // encontrar el último SPEED cuyo beat <= rawBeat
        var idx = -1
        for (i in sorted.indices) {
            if (sorted[i].beat <= rawBeat) idx = i else break
        }
        if (idx == -1) return 1.0

        val seg = sorted[idx]
        val length = seg.duration
        val mode = seg.mode

        // valor "anterior" (lo que había justo antes de este segmento)
        val prevRatio = if (idx > 0) sorted[idx - 1].ratio else 1.0
        val targetRatio = seg.ratio

        // cambios instantáneos (length == 0): salto directo al targetRatio
        if (length <= 0.0) {
            return targetRatio
        }

        // rampas: desde prevRatio hacia targetRatio durante 'length'
        val t = when (mode) {
            0 -> {
                val startBeat = seg.beat
                val endBeat = startBeat + length
                when {
                    rawBeat <= startBeat -> 0.0
                    rawBeat >= endBeat   -> 1.0
                    else                 -> (rawBeat - startBeat) / length
                }
            }
            1 -> {
                val startTime = beatToTime(seg.beat)
                val endTime = startTime + length * 1000.0
                when {
                    rawTimeMs <= startTime -> 0.0
                    rawTimeMs >= endTime   -> 1.0
                    else                   -> (rawTimeMs - startTime) / (length * 1000.0)
                }
            }
            else -> 1.0
        }.coerceIn(0.0, 1.0)

        return prevRatio + (targetRatio - prevRatio) * t
    }

    /**
     * YOffset base en unidades de stepSize:
     * (DisplayedBeat(note) - DisplayedBeat(song)) * DisplayedSpeedPercent(song).
     * NO mete aquí baseSpeed (XMod), eso se hace fuera.
     */
    fun getYOffsetForBeat(
        noteBeat: Double,
        songVisibleBeat: Double,
        songVisibleTimeMs: Double,
        stepSize: Float
    ): Float {
        val noteDispBeat = getDisplayedBeat(noteBeat)
        val songDispBeat = getDisplayedBeat(songVisibleBeat)
        val deltaBeatDisp = noteDispBeat - songDispBeat

        val speedPercent = getDisplayedSpeedPercent(songVisibleBeat, songVisibleTimeMs)

        val gapPerBeat = stepSize * speedPercent.toFloat()
        return (deltaBeatDisp * gapPerBeat).toFloat()
    }
}