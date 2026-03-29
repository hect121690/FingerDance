package com.fingerdance.ssc

import kotlin.math.min

class ScrollEngine(
    speeds: List<Parser.Speed>,
    scrolls: List<Parser.Scroll>,
    private val timing: TimingEngine
) {

    data class Segment(
        val beatStart: Double,
        val beatEnd: Double,
        val ratioStart: Double,
        val ratioEnd: Double // para interpolación (speed mode 1)
    )

    private val segments: List<Segment>

    init {
        segments = buildSegments(speeds, scrolls)
    }

    // =========================================================
    // BUILD
    // =========================================================

    private fun buildSegments(
        speeds: List<Parser.Speed>,
        scrolls: List<Parser.Scroll>
    ): List<Segment> {

        data class Event(
            val beat: Double,
            val type: Int, // 0 scroll, 1 speed
            val ratio: Double,
            val duration: Double,
            val mode: Int
        )

        val events = mutableListOf<Event>()

        scrolls.forEach {
            events.add(Event(it.beat, 0, it.ratio, 0.0, 0))
        }

        speeds.forEach {
            val durationBeats = if (it.mode == 1) {
                val startMs = timing.beatToTime(it.beat)
                val endMs = startMs + it.duration * 1000.0
                val endBeat = timing.timeToBeat(endMs)
                maxOf(0.0, endBeat - it.beat)
            } else it.duration

            events.add(Event(it.beat, 1, it.ratio, durationBeats, it.mode))
        }

        events.sortBy { it.beat }

        val result = mutableListOf<Segment>()

        var currentBeat = 0.0
        var currentRatio = 1.0

        for (i in events.indices) {

            val e = events[i]
            val nextBeat = events.getOrNull(i + 1)?.beat ?: Double.POSITIVE_INFINITY

            if (e.beat > currentBeat) {
                result.add(
                    Segment(
                        beatStart = currentBeat,
                        beatEnd = e.beat,
                        ratioStart = currentRatio,
                        ratioEnd = currentRatio
                    )
                )
                currentBeat = e.beat
            }

            if (e.type == 0) {
                // SCROLL instantáneo
                currentRatio = e.ratio

            } else {
                // SPEED
                val endBeat = e.beat + e.duration

                result.add(
                    Segment(
                        beatStart = e.beat,
                        beatEnd = endBeat,
                        ratioStart = currentRatio,
                        ratioEnd = e.ratio
                    )
                )

                currentBeat = endBeat
                currentRatio = e.ratio
            }
        }

        result.add(
            Segment(
                beatStart = currentBeat,
                beatEnd = Double.POSITIVE_INFINITY,
                ratioStart = currentRatio,
                ratioEnd = currentRatio
            )
        )

        return result
    }

    // =========================================================
    // BEAT → VISUAL
    // =========================================================

    fun beatToVisual(beat: Double): Double {

        var visual = 0.0

        for (seg in segments) {

            if (beat <= seg.beatStart) break

            val end = min(beat, seg.beatEnd)
            val length = end - seg.beatStart
            if (length <= 0) continue

            if (seg.ratioStart == seg.ratioEnd) {
                // constante
                visual += length * seg.ratioStart
            } else {
                // interpolación lineal
                val avg = (seg.ratioStart + seg.ratioEnd) / 2.0
                visual += length * avg
            }

            if (beat <= seg.beatEnd) break
        }

        return visual
    }
}