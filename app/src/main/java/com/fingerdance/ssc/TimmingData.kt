package com.fingerdance.ssc

import com.fingerdance.ssc.Parser.BpmSegment
import com.fingerdance.ssc.Parser.Delay
import com.fingerdance.ssc.Parser.Note
import com.fingerdance.ssc.Parser.Stop
import com.fingerdance.ssc.Parser.Warp
import com.fingerdance.ssc.Parser.Speed
import com.fingerdance.ssc.Parser.Scroll
import kotlin.math.min

class TimmingData(
    val bpms: List<BpmSegment>,
    val stops: List<Stop>,
    val delays: List<Delay>,
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
        val timeEndMs: Double,    // <-- NUEVO
        val bpm: Double,
        val isWarp: Boolean,
        val isStop: Boolean,
        val isDelay: Boolean
    )

    // Segmentos "reales" para beat <-> tiempo (igual que en PlayerSsc3)
    private val timeSegments: List<TimeSegment> = buildTimeSegments()

    private fun buildTimeSegments(): List<TimeSegment> {
        data class Event(val beat: Double, val type: Int, val value: Double)

        val events = mutableListOf<Event>()
        bpms.forEach { events.add(Event(it.beat, 1, it.bpm)) }
        stops.forEach { events.add(Event(it.beat, 2, it.durationMs)) }
        delays.forEach { events.add(Event(it.beat, 3, it.durationMs)) }
        warps.forEach { events.add(Event(it.beat, 0, it.duration)) }
        events.sortWith(compareBy<Event>({ it.beat }, { it.type }))

        val result = mutableListOf<TimeSegment>()

        var currentBeat = 0.0
        var currentTimeMs = offsetMs + userOffsetMs
        var currentBpm = bpms.firstOrNull()?.bpm ?: 120.0

        fun addSegment(nextBeat: Double) {
            if (nextBeat <= currentBeat) return
            val endTime = currentTimeMs + ((nextBeat-currentBeat)/currentBpm)*60000.0
            result.add(
                TimeSegment(
                    beatStart = currentBeat,
                    beatEnd = nextBeat,
                    timeStartMs = currentTimeMs,
                    timeEndMs = endTime,
                    bpm = currentBpm,
                    isWarp = false,
                    isStop = false,
                    isDelay = false
                )
            )
            currentTimeMs = endTime
            currentBeat = nextBeat
        }

        for (e in events) {
            addSegment(e.beat)

            when (e.type) {
                // Warp
                0 -> {
                    val warpEnd = e.beat + e.value
                    result.add(TimeSegment(
                        beatStart = e.beat,
                        beatEnd = warpEnd,
                        timeStartMs = currentTimeMs,
                        timeEndMs = currentTimeMs,
                        bpm = currentBpm,
                        isWarp = true,
                        isStop = false,
                        isDelay = false
                    ))
                    currentBeat = warpEnd
                    // currentTimeMs does NOT advance
                }
                // BPM
                1 -> currentBpm = e.value

                // STOP
                2 -> {
                    val durationMs = e.value
                    result.add(
                        TimeSegment(
                            beatStart = e.beat,
                            beatEnd = e.beat,
                            timeStartMs = currentTimeMs,
                            timeEndMs = currentTimeMs + durationMs,
                            bpm = currentBpm,
                            isWarp = false,
                            isStop = true,
                            isDelay = false
                        )
                    )
                    currentTimeMs += durationMs
                }
                3 -> {
                    val durationMs = e.value
                    result.add(
                        TimeSegment(
                            beatStart = e.beat,
                            beatEnd = e.beat,
                            timeStartMs = currentTimeMs,
                            timeEndMs = currentTimeMs + durationMs,
                            bpm = currentBpm,
                            isWarp = false,
                            isStop = false,
                            isDelay = true
                        )
                    )
                    currentTimeMs += durationMs
                }
            }
        }

        result.add(TimeSegment(
            beatStart = currentBeat,
            beatEnd = Double.POSITIVE_INFINITY,
            timeStartMs = currentTimeMs,
            timeEndMs = Double.POSITIVE_INFINITY,
            bpm = currentBpm,
            isWarp = false,
            isStop = false,
            isDelay = false
        ))
        return result
    }

    fun isBeatInWarp(beat: Double): Boolean {
        val seg = timeSegments.lastOrNull { beat >= it.beatStart }
        return seg?.isWarp == true && beat < seg.beatEnd
    }

    fun isBeatInStop(nowMs: Double): Boolean {
        val seg = timeSegments.lastOrNull { nowMs >= it.timeStartMs }
        return seg?.isStop == true && nowMs < seg.timeEndMs
    }
    fun isBeatInDelay(nowMs: Double): Boolean {
        val seg = timeSegments.lastOrNull { nowMs >= it.timeStartMs }
        return seg?.isDelay == true && nowMs < seg.timeEndMs
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

    // Dado un tiempo real -> calcula el beat
    fun timeToBeat(timeMs: Double): Double {
        val seg = findSegmentByTime(timeMs)
        return when {
            seg.isWarp -> seg.beatStart
            seg.isStop -> seg.beatStart
            seg.isDelay -> seg.beatStart
            else -> seg.beatStart + ((timeMs - seg.timeStartMs) / 60000.0) * seg.bpm
        }
    }

    // Dado un beat -> calcula el tiempo real
    fun beatToTime(beat: Double): Double {
        val seg = findSegmentByBeat(beat)
        return when {
            seg.isWarp -> seg.timeStartMs
            seg.isStop -> seg.timeStartMs
            seg.isDelay -> seg.timeStartMs
            else -> seg.timeStartMs + (beat - seg.beatStart) / seg.bpm * 60000.0
        }
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