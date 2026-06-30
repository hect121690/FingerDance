package com.fingerdance.ssc

import com.fingerdance.ssc.Parser.BpmSegment
import com.fingerdance.ssc.Parser.Delay
import com.fingerdance.ssc.Parser.Scroll
import com.fingerdance.ssc.Parser.Speed
import com.fingerdance.ssc.Parser.Stop
import com.fingerdance.ssc.Parser.Warp
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
    companion object {
        private const val EPS = 0.000001
    }

    data class TimeSegment(
        val beatStart: Double,
        val beatEnd: Double,
        val timeStartMs: Double,
        val timeEndMs: Double,
        val bpm: Double,
        val isWarp: Boolean = false,
        val isStop: Boolean = false,
        val isDelay: Boolean = false
    )

    private data class Event(
        val beat: Double,
        val type: EventType,
        val value: Double
    )

    private enum class EventType {
        WARP,
        BPM,
        DELAY,
        STOP
    }

    private val sortedBpms = bpms.sortedBy { it.beat }.ifEmpty {
        listOf(BpmSegment(0.0, 120.0))
    }

    private val sortedStops = stops.sortedBy { it.beat }
    private val sortedDelays = delays.sortedBy { it.beat }
    private val sortedWarps = warps.sortedBy { it.beat }
    private val sortedSpeeds = speeds.sortedBy { it.beat }
    private val sortedScrolls = scrolls.sortedBy { it.beat }

    private val timeSegments: List<TimeSegment> = buildTimeSegments()
    private val scrollSegments: List<ScrollSegmentInternal> = buildScrollSegments()

    private fun buildTimeSegments(): List<TimeSegment> {
        val events = mutableListOf<Event>()

        sortedBpms.forEach { events.add(Event(it.beat, EventType.BPM, it.bpm)) }
        sortedStops.forEach { events.add(Event(it.beat, EventType.STOP, it.durationMs)) }
        sortedDelays.forEach { events.add(Event(it.beat, EventType.DELAY, it.durationMs)) }
        sortedWarps.forEach { events.add(Event(it.beat, EventType.WARP, it.duration)) }

        events.sortWith(
            compareBy<Event> { it.beat }
                .thenBy {
                    when (it.type) {
                        EventType.BPM -> 0
                        EventType.DELAY -> 1
                        EventType.STOP -> 2
                        EventType.WARP -> 3
                    }
                }
        )

        val result = mutableListOf<TimeSegment>()

        var currentBeat = 0.0

        // StepMania usa -offset como tiempo de beat 0.
        var currentTimeMs = offsetMs + userOffsetMs

        var currentBpm = sortedBpms.firstOrNull()?.bpm ?: 120.0

        fun addNormalSegment(nextBeat: Double) {
            if (nextBeat <= currentBeat + EPS) return

            val durationMs = ((nextBeat - currentBeat) / currentBpm) * 60000.0
            val endTime = currentTimeMs + durationMs

            result.add(
                TimeSegment(
                    beatStart = currentBeat,
                    beatEnd = nextBeat,
                    timeStartMs = currentTimeMs,
                    timeEndMs = endTime,
                    bpm = currentBpm
                )
            )

            currentBeat = nextBeat
            currentTimeMs = endTime
        }

        var i = 0
        while (i < events.size) {
            val e = events[i]
            if (e.beat < currentBeat - EPS) {
                when (e.type) {
                    EventType.BPM -> {
                        currentBpm = e.value
                    }
                    EventType.DELAY -> Unit
                    EventType.STOP -> Unit
                    EventType.WARP -> Unit
                }

                i++
                continue
            }

            addNormalSegment(e.beat)

            val sameBeatEvents = mutableListOf<Event>()
            while (i < events.size && kotlin.math.abs(events[i].beat - e.beat) <= EPS) {
                sameBeatEvents.add(events[i])
                i++
            }

            // 1. BPM primero
            val bpmEvent = sameBeatEvents.lastOrNull { it.type == EventType.BPM }
            if (bpmEvent != null) {
                currentBpm = bpmEvent.value
            }

            // 2. DELAY
            val delayEvents = sameBeatEvents.filter { it.type == EventType.DELAY }
            for (d in delayEvents) {
                val start = currentTimeMs
                val end = currentTimeMs + d.value

                result.add(
                    TimeSegment(
                        beatStart = d.beat,
                        beatEnd = d.beat,
                        timeStartMs = start,
                        timeEndMs = end,
                        bpm = currentBpm,
                        isDelay = true
                    )
                )

                currentTimeMs = end
            }

            // 3. STOP
            val stopEvents = sameBeatEvents.filter { it.type == EventType.STOP }
            for (s in stopEvents) {
                val start = currentTimeMs
                val end = currentTimeMs + s.value

                result.add(
                    TimeSegment(
                        beatStart = s.beat,
                        beatEnd = s.beat,
                        timeStartMs = start,
                        timeEndMs = end,
                        bpm = currentBpm,
                        isStop = true
                    )
                )

                currentTimeMs = end
            }
            // 4. WARP al final
            val warpEvents = sameBeatEvents.filter { it.type == EventType.WARP }
            for (w in warpEvents) {
                val warpStart = w.beat
                val warpEnd = warpStart + w.value

                if (warpEnd > currentBeat + EPS) {
                    result.add(
                        TimeSegment(
                            beatStart = warpStart,
                            beatEnd = warpEnd,
                            timeStartMs = currentTimeMs,
                            timeEndMs = currentTimeMs,
                            bpm = currentBpm,
                            isWarp = true
                        )
                    )

                    currentBeat = warpEnd
                }
            }
        }

        result.add(
            TimeSegment(
                beatStart = currentBeat,
                beatEnd = Double.POSITIVE_INFINITY,
                timeStartMs = currentTimeMs,
                timeEndMs = Double.POSITIVE_INFINITY,
                bpm = currentBpm
            )
        )

        return result
    }

    fun isBeatInWarp(beat: Double): Boolean {
        val seg = findSegmentByBeat(beat)
        if (!seg.isWarp) return false
        if (beat < seg.beatStart || beat >= seg.beatEnd) return false

        // StepMania permite stop/delay dentro de warp.
        val hasStopHere = sortedStops.any { kotlin.math.abs(it.beat - beat) <= EPS }
        val hasDelayHere = sortedDelays.any { kotlin.math.abs(it.beat - beat) <= EPS }

        return !hasStopHere && !hasDelayHere
    }

    fun isBeatInStop(nowMs: Double): Boolean {
        val seg = findSegmentByTime(nowMs)
        return seg.isStop && nowMs < seg.timeEndMs
    }

    fun isBeatInDelay(nowMs: Double): Boolean {
        val seg = findSegmentByTime(nowMs)
        return seg.isDelay && nowMs < seg.timeEndMs
    }

    private fun findSegmentByBeat(beat: Double): TimeSegment {
        var low = 0
        var high = timeSegments.size - 1
        var best = timeSegments.first()

        while (low <= high) {
            val mid = (low + high).ushr(1)
            val seg = timeSegments[mid]

            if (seg.beatStart <= beat) {
                best = seg
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        return best
    }

    private fun findSegmentByTime(timeMs: Double): TimeSegment {
        var low = 0
        var high = timeSegments.size - 1
        var best = timeSegments.first()

        while (low <= high) {
            val mid = (low + high).ushr(1)
            val seg = timeSegments[mid]

            if (seg.timeStartMs <= timeMs) {
                best = seg
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        return best
    }

    fun timeToBeat(timeMs: Double): Double {
        val seg = findSegmentByTime(timeMs)

        return when {
            seg.isWarp -> seg.beatStart
            seg.isStop -> seg.beatStart
            seg.isDelay -> seg.beatStart
            else -> seg.beatStart + ((timeMs - seg.timeStartMs) / 60000.0) * seg.bpm
        }
    }

    fun beatToTime(beat: Double): Double {
        val seg = findSegmentByBeat(beat)

        return when {
            seg.isWarp -> seg.timeStartMs
            seg.isStop -> seg.timeStartMs
            seg.isDelay -> seg.timeStartMs
            else -> seg.timeStartMs + ((beat - seg.beatStart) / seg.bpm) * 60000.0
        }
    }

    private fun getDelayAtBeat(beat: Double): Double {
        return sortedDelays
            .firstOrNull { kotlin.math.abs(it.beat - beat) <= EPS }
            ?.durationMs
            ?: 0.0
    }

    data class ScrollSegmentInternal(
        val beatStart: Double,
        val beatEnd: Double,
        val ratio: Double
    )

    private fun buildScrollSegments(): List<ScrollSegmentInternal> {
        if (sortedScrolls.isEmpty()) {
            return listOf(ScrollSegmentInternal(0.0, Double.POSITIVE_INFINITY, 1.0))
        }

        val result = mutableListOf<ScrollSegmentInternal>()

        var prevBeat = 0.0
        var prevRatio = 1.0

        for (s in sortedScrolls) {
            if (s.beat > prevBeat) {
                result.add(
                    ScrollSegmentInternal(
                        beatStart = prevBeat,
                        beatEnd = s.beat,
                        ratio = prevRatio
                    )
                )
            }

            prevBeat = s.beat
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

    fun getDisplayedBeat(rawBeat: Double): Double {
        var displayed = 0.0
        val b = rawBeat

        for (seg in scrollSegments) {
            if (b <= seg.beatStart) break

            val segEnd = min(b, seg.beatEnd)

            if (segEnd > seg.beatStart) {
                displayed += (segEnd - seg.beatStart) * seg.ratio
            }

            if (b <= seg.beatEnd) break
        }

        return displayed
    }

    fun getDisplayedSpeedPercent(rawBeat: Double, rawTimeMs: Double): Double {
        if (sortedSpeeds.isEmpty()) return 1.0

        val idx = getSpeedIndexAtBeat(rawBeat)
        if (idx < 0) return 1.0

        val seg = sortedSpeeds[idx]
        val first = sortedSpeeds.first()

        val startBeat = seg.beat

        // StepMania: GetElapsedTimeFromBeat(startBeat) - GetDelayAtBeat(startBeat)
        val startTime = beatToTime(startBeat) - getDelayAtBeat(startBeat)

        val endTime = if (seg.mode == 1) {
            // UNIT_SECONDS
            startTime + seg.duration * 1000.0
        } else {
            // UNIT_BEATS
            val endBeat = startBeat + seg.duration
            beatToTime(endBeat) - getDelayAtBeat(endBeat)
        }

        val curTime = rawTimeMs

        if (idx == 0 && first.duration > 0.0 && curTime < startTime) {
            return 1.0
        }

        if (endTime >= curTime && (idx > 0 || first.duration > 0.0)) {
            val priorSpeed = if (idx == 0) 1.0 else sortedSpeeds[idx - 1].ratio

            val duration = endTime - startTime
            val timeUsed = curTime - startTime

            val ratioUsed = if (kotlin.math.abs(duration) <= EPS) {
                1.0
            } else {
                timeUsed / duration
            }.coerceIn(0.0, 1.0)

            val distance = priorSpeed - seg.ratio
            val ratioNeed = ratioUsed * -distance

            return priorSpeed + ratioNeed
        }

        return seg.ratio
    }

    private fun getSpeedIndexAtBeat(beat: Double): Int {
        if (sortedSpeeds.isEmpty()) return -1

        var low = 0
        var high = sortedSpeeds.size - 1
        var best = -1

        while (low <= high) {
            val mid = (low + high).ushr(1)
            val s = sortedSpeeds[mid]

            if (s.beat <= beat) {
                best = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }

        return best
    }

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

        return (deltaBeatDisp * stepSize * speedPercent).toFloat()
    }
}