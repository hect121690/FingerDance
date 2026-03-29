package com.fingerdance.ssc

import kotlin.math.max

class TimingEngine(
    bpms: List<Parser.BpmSegment>,
    stops: List<Parser.Stop>,
    warps: List<Parser.Warp>
) {

    data class Segment(
        val beatStart: Double,
        val beatEnd: Double,
        val timeStart: Double,
        val bpm: Double,
        val isWarp: Boolean
    )

    private val segments: List<Segment>

    init {
        segments = buildSegments(bpms, stops, warps)
    }

    // =========================================================
    // BUILD
    // =========================================================

    private fun buildSegments(
        bpms: List<Parser.BpmSegment>,
        stops: List<Parser.Stop>,
        warps: List<Parser.Warp>
    ): List<Segment> {

        data class Event(
            val beat: Double,
            val type: Int, // 0 warp, 1 bpm, 2 stop
            val value: Double
        )

        val events = mutableListOf<Event>()

        bpms.forEach { events.add(Event(it.beat, 1, it.bpm)) }
        stops.forEach { events.add(Event(it.beat, 2, it.durationMs)) }
        warps.forEach { events.add(Event(it.beat, 0, it.duration)) }

        events.sortWith(compareBy<Event>({ it.beat }, { it.type }))

        val result = mutableListOf<Segment>()

        var currentBeat = 0.0
        var currentTime = 0.0
        var currentBpm = bpms.firstOrNull()?.bpm ?: 120.0

        fun addSegment(nextBeat: Double) {
            if (nextBeat <= currentBeat) return

            result.add(
                Segment(
                    beatStart = currentBeat,
                    beatEnd = nextBeat,
                    timeStart = currentTime,
                    bpm = currentBpm,
                    isWarp = false
                )
            )

            val deltaBeat = nextBeat - currentBeat
            currentTime += (deltaBeat / currentBpm) * 60000.0
            currentBeat = nextBeat
        }

        for (e in events) {

            addSegment(e.beat)

            when (e.type) {

                // =========================
                // WARP (REMOVE BEAT SPACE)
                // =========================
                0 -> {
                    val warpEnd = e.beat + e.value

                    result.add(
                        Segment(
                            beatStart = e.beat,
                            beatEnd = warpEnd,
                            timeStart = currentTime,
                            bpm = currentBpm,
                            isWarp = true
                        )
                    )

                    // avanzar beat SIN tiempo
                    currentBeat = warpEnd
                }

                // =========================
                // BPM
                // =========================
                1 -> {
                    currentBpm = e.value
                }

                // =========================
                // STOP
                // =========================
                2 -> {
                    currentTime += e.value
                }
            }
        }

        // segmento final abierto
        result.add(
            Segment(
                beatStart = currentBeat,
                beatEnd = Double.POSITIVE_INFINITY,
                timeStart = currentTime,
                bpm = currentBpm,
                isWarp = false
            )
        )

        return result
    }

    // =========================================================
    // BEAT → TIME
    // =========================================================

    fun beatToTime(beat: Double): Double {

        val seg = findSegmentByBeat(beat)

        if (seg.isWarp) {
            return seg.timeStart
        }

        val delta = beat - seg.beatStart
        return seg.timeStart + (delta / seg.bpm) * 60000.0
    }

    // =========================================================
    // TIME → BEAT
    // =========================================================

    fun timeToBeat(time: Double): Double {

        val seg = findSegmentByTime(time)

        if (seg.isWarp) {
            return seg.beatEnd
        }

        val deltaMs = time - seg.timeStart
        return seg.beatStart + (deltaMs / 60000.0) * seg.bpm
    }

    // =========================================================
    // SEARCH
    // =========================================================

    private fun findSegmentByBeat(beat: Double): Segment {
        return segments.binarySearch {
            when {
                beat < it.beatStart -> 1
                beat >= it.beatEnd -> -1
                else -> 0
            }
        }.let { if (it >= 0) segments[it] else segments[-it - 1] }
    }

    private fun findSegmentByTime(time: Double): Segment {
        return segments.binarySearch {
            when {
                time < it.timeStart -> 1
                else -> -1
            }
        }.let { idx ->
            val i = if (idx >= 0) idx else (-idx - 2)
            segments[max(0, i)]
        }
    }
}