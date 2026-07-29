package com.fingerdance.ssc

import com.badlogic.gdx.Gdx
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private const val holdReleaseTolerance: Double = 0.15
class SscGameplayEngine(
    notes: List<Parser.Note>,
    private val timingData: TimmingData,
    tickSegments: List<TickSegment>,
    comboSegments: List<ComboSegment>,
    private val config: Config,
    private val listener: Listener
) {
    data class Config(
        val columnCount: Int,
        val activeColumns: IntRange,
        val stepSize: Float,
        val screenHeight: Float,
        val baseSpeed: Float,
        val isEW: Boolean,
        val zonePerfectMs: Long,
        val zoneGreatMs: Long,
        val zoneGoodMs: Long,
        val zoneBadMs: Long,
        val drawDistanceAfterTargetsPx: Double = -(stepSize * 2.0),
        val drawDistanceBeforeTargetsPx: Double = screenHeight.toDouble(),
        val maxNotesAfterTarget: Int = 256,
        val firstBeatSearchIterations: Int = 24,
        val lastBeatSearchIterations: Int = 20,

        val lastBeatInitialDistance: Double = 32.0,
        val lowSpeedMaxBeatsAhead: Double = 64.0,
        val lowSpeedThreshold: Double = 0.75,
        val tapSearchLimit: Int = 8,
        val lateHoldSearchBeats: Double = 2.0
    ) {
        init {
            require(columnCount > 0)
            require(activeColumns.first >= 0 && activeColumns.last < columnCount)
            require(stepSize > 0f)
            require(screenHeight > 0f)
            require(baseSpeed > 0f)
            require(holdReleaseTolerance in 0.0..1.0)
        }
    }

    data class TickSegment(val beat: Double, val tickCount: Double)
    data class ComboSegment(val beat: Double, val multiplier: Int)

    interface Renderer {
        fun drawTap(note: Parser.Note, column: Int, y: Int)
        fun drawMine(note: Parser.Note, column: Int, y: Int)
        fun drawHold(note: Parser.Note, column: Int, yHead: Int, yTail: Int)
    }

    interface Listener {
        fun onColumnActive(column: Int)
        fun onJudge(
            column: Int,
            judge: Int,
            comboMultiplier: Int,
            isBodyLongNote: Boolean,
            isFromInput: Boolean,
            isMine: Boolean,
            note: Parser.Note?
        )
        fun onMineHit(column: Int, note: Parser.Note)
        fun onHoldStarted(column: Int, note: Parser.Note) = Unit
        fun onHoldReleased(
            column: Int,
            note: Parser.Note,
            releaseBeat: Double,
            acceptedAsComplete: Boolean
        ) = Unit
        fun onHoldFinished(column: Int, note: Parser.Note) = Unit
    }

    companion object {
        const val JUDGE_PERFECT = 0
        const val JUDGE_GREAT = 1
        const val JUDGE_GOOD = 2
        const val JUDGE_BAD = 3
        const val JUDGE_MISS = 4
        const val KEY_NONE = 0
        const val KEY_DOWN = 1
        const val KEY_PRESS = 2
        const val KEY_UP = 3
    }

    private data class LongNoteState(
        var pressed: Boolean = false,
        var lastTickBeat: Double = 0.0,
        var nextTickBeat: Double = 0.0,
        var note: Parser.Note? = null,
        var timeStartedMs: Double = 0.0
    )

    private val allNotes = notes.sortedBy { it.beat }
    private val ticks = tickSegments.sortedBy { it.beat }
    private val combos = comboSegments.sortedBy { it.beat }

    private val hitNotes = mutableSetOf<Parser.Note>()
    private val finishedHolds = mutableSetOf<Parser.Note>()
    private val releasedHoldBeat = mutableMapOf<Parser.Note, Double>()
    private val longNotes = Array(config.columnCount) { LongNoteState() }
    private val columnNotes = Array(config.columnCount) { mutableListOf<Parser.Note>() }
    private val columnIndex = IntArray(config.columnCount)
    private val keyState = IntArray(config.columnCount)
    private val holdCompletedThisFrame = mutableSetOf<Parser.Note>()

    private val renderNotes = allNotes.filter { it.column in config.activeColumns }

    private val renderNoteBeats =
        renderNotes.asSequence()
            .filterNot { it.isPhantom || it.isHide }
            .map { it.beat }
            .toList()
            .toDoubleArray()

    private val renderHoldsByStart =
        renderNotes.filter {
            it.type == Parser.NoteType.HOLD && it.endBeat != null
        }

    private val renderHoldPrefixMaxEnd =
        DoubleArray(renderHoldsByStart.size).also { prefix ->
            var maximum = Double.NEGATIVE_INFINITY
            for (index in renderHoldsByStart.indices) {
                maximum = maxOf(
                    maximum,
                    renderHoldsByStart[index].endBeat ?: Double.NEGATIVE_INFINITY
                )
                prefix[index] = maximum
            }
        }

    private var lastStepSongTimeMs = 0.0
    private var comboSegmentIndex = 0

    var comboMultiplier: Int = 1
        private set

    var currentBeat: Double = 0.0
        private set

    init {
        for (note in allNotes) {
            if (note.column in 0 until config.columnCount) {
                columnNotes[note.column].add(note)
            }
        }
        for (column in 0 until config.columnCount) {
            columnNotes[column].sortBy { it.beat }
        }
    }

    fun render(songTimeMs: Double, renderer: Renderer) {
        val nowMs = songTimeMs
        val beat = timingData.timeToBeat(nowMs)
        currentBeat = beat
        updateComboMultiplier(beat)

        val firstBeatToDraw = findFirstDisplayedBeat(beat, nowMs)
        val lastBeatToDraw = findLastDisplayedBeat(beat, nowMs)
        val renderedHolds = HashSet<Parser.Note>()

        renderHoldsCrossingFirstBeat(
            firstBeatToDraw, beat, nowMs, renderedHolds, renderer
        )

        val firstIndex = renderNotes.lowerBoundByBeat(firstBeatToDraw)
        for (index in firstIndex until renderNotes.size) {
            val note = renderNotes[index]
            if (note.beat > lastBeatToDraw) break
            if (note.isPhantom || note.isHide || note.column !in config.activeColumns) continue

            when (note.type) {
                Parser.NoteType.TAP -> {
                    renderTapCandidate(note, beat, nowMs, renderer)
                }

                Parser.NoteType.HOLD ->
                    if (renderedHolds.add(note)) {
                        renderHoldCandidate(note, beat, nowMs, renderer)
                    }
            }
        }
    }

    fun updateStepData(songTimeMs: Double, input: IntArray) {
        require(input.size >= config.columnCount) {
            "El input tiene ${input.size} columnas y el motor necesita ${config.columnCount}."
        }

        for (column in 0 until config.columnCount) keyState[column] = input[column]

        val previousSongTimeMs = lastStepSongTimeMs
        lastStepSongTimeMs = songTimeMs
        val previousBeat = timingData.timeToBeat(previousSongTimeMs.toDouble())
        val nowBeat = timingData.timeToBeat(songTimeMs.toDouble())
        currentBeat = nowBeat
        updateComboMultiplier(nowBeat)

        for (column in config.activeColumns) {
            when (keyState[column]) {
                KEY_DOWN -> {
                    listener.onColumnActive(column)
                    processTapAndHeadOnColumn(column, songTimeMs)
                    tryAutoStartHoldOnPress(column, songTimeMs)
                }
                KEY_PRESS -> {
                    listener.onColumnActive(column)
                    tryAutoStartHoldOnPress(column, songTimeMs)
                    processLongNoteTick(column, songTimeMs)
                }
                KEY_UP -> if (longNotes[column].pressed) {
                    endLongNote(column, songTimeMs)
                }
            }
        }

        for (column in config.activeColumns) {
            val notesInColumn = columnNotes[column]
            if (!longNotes[column].pressed &&
                (keyState[column] == KEY_PRESS || keyState[column] == KEY_DOWN)
            ) {
                for (index in columnIndex[column] until notesInColumn.size) {
                    val note = notesInColumn[index]
                    if (note.isFake) continue
                    if (note.type != Parser.NoteType.HOLD) continue
                    if (finishedHolds.contains(note)) continue

                    val headBeat = note.beat
                    val lateCatch = (keyState[column] == KEY_PRESS || keyState[column] == KEY_DOWN) && nowBeat > headBeat
                    if (timingData.isBeatInWarp(note.beat) && note.endBeat == null) {
                        continue
                    }
                    val crossedHead =
                        (previousBeat <= headBeat && headBeat < nowBeat) ||
                                (nowBeat <= headBeat && headBeat < previousBeat) ||
                                abs(nowBeat - headBeat) < 0.001 ||
                                lateCatch

                    if (crossedHead) {
                        emitJudge(column, JUDGE_PERFECT, isFromInput = true, note = note)
                        startLongNote(column, note, songTimeMs)
                        longNotes[column].lastTickBeat = nowBeat
                        columnIndex[column] = index + 1
                        break
                    }

                    if (headBeat > nowBeat && headBeat > previousBeat) break
                }
            }
        }

        updateReleasedHoldVisuals(songTimeMs)
        updateAutoMisses(songTimeMs)
    }

    fun reset() {
        hitNotes.clear()
        finishedHolds.clear()
        releasedHoldBeat.clear()
        for (column in 0 until config.columnCount) {
            columnIndex[column] = 0
            keyState[column] = KEY_NONE
            longNotes[column] = LongNoteState()
        }
        lastStepSongTimeMs = 0.0
        comboSegmentIndex = 0
        comboMultiplier = 1
        currentBeat = 0.0
    }

    private fun offsetForBeat(
        beat: Double,
        currentBeat: Double,
        songTimeMs: Double
    ): Float {
        return timingData.getYOffsetForBeat(
            noteBeat = beat,
            songVisibleBeat = currentBeat,
            songVisibleTimeMs = songTimeMs,
            stepSize = config.stepSize,
            isEW = config.isEW
        ) * config.baseSpeed
    }

    private fun findFirstDisplayedBeat(currentBeat: Double, songTimeMs: Double): Double {
        if (renderNotes.isEmpty()) return currentBeat
        val firstChartBeat = minOf(0.0, renderNotes.first().beat)
        if (currentBeat <= firstChartBeat) return firstChartBeat

        var low = firstChartBeat
        var high = currentBeat

        repeat(config.firstBeatSearchIterations) {
            val candidate = (low + high) * 0.5
            val offset = offsetForBeat(candidate, currentBeat, songTimeMs)
            val tooManyNotes =
                countRenderableNotes(candidate, currentBeat) > config.maxNotesAfterTarget

            if (offset < config.drawDistanceAfterTargetsPx || tooManyNotes) {
                low = candidate
            } else {
                high = candidate
            }
        }
        return high
    }

    private fun findLastDisplayedBeat(currentBeat: Double, songTimeMs: Double): Double {
        var searchDistance = config.lastBeatInitialDistance
        var candidate = currentBeat + searchDistance

        repeat(config.lastBeatSearchIterations) {
            val offset = offsetForBeat(candidate, currentBeat, songTimeMs)
            if (offset > config.drawDistanceBeforeTargetsPx) {
                candidate -= searchDistance
            } else {
                candidate += searchDistance
            }
            searchDistance *= 0.5
        }

        val displayedSpeed =
            timingData.getDisplayedSpeedPercent(
                rawBeat = currentBeat,
                rawTimeMs = songTimeMs,
                isEW = config.isEW
            ) * config.baseSpeed

        if (displayedSpeed < config.lowSpeedThreshold) {
            candidate = minOf(candidate, currentBeat + config.lowSpeedMaxBeatsAhead)
        }
        return candidate
    }

    private fun countRenderableNotes(fromBeat: Double, toBeat: Double): Int {
        if (renderNoteBeats.isEmpty()) return 0
        val lowBeat = minOf(fromBeat, toBeat)
        val highBeat = maxOf(fromBeat, toBeat)
        return (
                upperBound(renderNoteBeats, highBeat) -
                        lowerBound(renderNoteBeats, lowBeat)
                ).coerceAtLeast(0)
    }

    private fun renderHoldsCrossingFirstBeat(
        firstBeat: Double,
        currentBeat: Double,
        songTimeMs: Double,
        renderedHolds: MutableSet<Parser.Note>,
        renderer: Renderer
    ) {
        if (renderHoldsByStart.isEmpty()) return
        var index = renderHoldsByStart.lowerBoundByBeat(firstBeat) - 1

        while (index >= 0) {
            if (renderHoldPrefixMaxEnd[index] < firstBeat) break
            val hold = renderHoldsByStart[index]
            val endBeat = hold.endBeat

            if (endBeat != null &&
                endBeat >= firstBeat &&
                !hold.isPhantom &&
                !hold.isHide &&
                renderedHolds.add(hold)
            ) {
                renderHoldCandidate(hold, currentBeat, songTimeMs, renderer)
            }
            index--
        }
    }

    private fun renderTapCandidate(
        note: Parser.Note,
        currentBeat: Double,
        songTimeMs: Double,
        renderer: Renderer
    ) {
        if (hitNotes.contains(note)) return
        val offset = offsetForBeat(note.beat, currentBeat, songTimeMs)
        if (!isOffsetVisible(offset)) return
        val y = config.stepSize.toInt() + offset.toInt()
        if (note.isMine) renderer.drawMine(note, note.column, y)
        else renderer.drawTap(note, note.column, y)
    }

    private fun renderHoldCandidate(
        note: Parser.Note,
        currentBeat: Double,
        songTimeMs: Double,
        renderer: Renderer
    ) {
        val endBeat = note.endBeat ?: return
        val completedThisFrame = holdCompletedThisFrame.contains(note)
        if (finishedHolds.contains(note) && !completedThisFrame) return
        val column = note.column
        if (column !in config.activeColumns) return

        var headBeat = note.beat
        val releaseBeat = releasedHoldBeat[note]
        val isPressedHold =
            longNotes[column].pressed && longNotes[column].note === note

        val hasReleasedVisual = releaseBeat != null

        if (currentBeat >= endBeat && !isPressedHold && !completedThisFrame && !hasReleasedVisual) {
            return
        }

        if (releaseBeat != null && !isPressedHold) headBeat = releaseBeat

        var headOffset = offsetForBeat(headBeat, currentBeat, songTimeMs)
        var tailOffset = offsetForBeat(endBeat, currentBeat, songTimeMs)

        if (isPressedHold || (note.isFake && note.isPressed)) {
            headOffset = 0f
            tailOffset = maxOf(tailOffset, headOffset)
        }

        val shouldAnchor = isPressedHold ||
                    completedThisFrame ||
                    (note.isFake && note.isPressed)

        if (shouldAnchor) {
            headOffset = 0f
            tailOffset = maxOf(tailOffset, headOffset)
        }

        if (!isHoldOffsetVisible(headOffset, tailOffset)) {
            return
        }
        renderer.drawHold(note, column, config.stepSize.toInt() + headOffset.toInt(), config.stepSize.toInt() + tailOffset.toInt())
    }

    private fun isOffsetVisible(offset: Float) =
        offset > config.drawDistanceAfterTargetsPx &&
                offset < config.drawDistanceBeforeTargetsPx

    private fun isHoldOffsetVisible(headOffset: Float, tailOffset: Float): Boolean {
        val minimum = minOf(headOffset, tailOffset)
        val maximum = maxOf(headOffset, tailOffset)
        return maximum > config.drawDistanceAfterTargetsPx &&
                minimum < config.drawDistanceBeforeTargetsPx
    }

    private fun updateAutoMisses(songTimeMs: Double) {
        for (column in config.activeColumns) {
            val notesInColumn = columnNotes[column]
            var index = columnIndex[column]
            if (longNotes[column].pressed) continue

            while (index < notesInColumn.size) {
                val note = notesInColumn[index]

                if (note.isFake || hitNotes.contains(note)) {
                    index++
                    columnIndex[column] = index
                    continue
                }
                if (timingData.isBeatInWarp(note.beat) && note.endBeat == null) {
                    index++
                    columnIndex[column] = index
                    continue
                }


                val deltaMs = getDeltaMsForNote(note.beat, songTimeMs)
                if (deltaMs > config.zoneBadMs) {
                    if (note.isMine) {
                        index++
                        columnIndex[column] = index
                        continue
                    }
                    emitJudge(column, JUDGE_MISS, isFromInput = false, note = note)
                    columnIndex[column] = index + 1
                    index++
                    continue
                }
                break
            }
        }
    }

    private fun processTapAndHeadOnColumn(column: Int, timeMs: Double) {
        val notesInColumn = columnNotes[column]
        val nowBeat = timingData.timeToBeat(timeMs)
        val startIndex = columnIndex[column]
        var bestIndex = -1
        var bestJudge = -1
        var minimumAbsoluteDelta = Long.MAX_VALUE
        val endExclusive = min(startIndex + config.tapSearchLimit, notesInColumn.size)

        for (index in startIndex until endExclusive) {
            val note = notesInColumn[index]
            if (note.isFake) {
                continue
            }
            if (timingData.isBeatInWarp(note.beat) && note.endBeat == null) {
                continue
            }

            val deltaMs = getDeltaMsForNote(note.beat, timeMs)
            if (deltaMs < -config.zoneBadMs) break

            if (note.isMine) {
                if (abs(deltaMs) <= config.zoneBadMs) {
                    emitJudge(
                        column, JUDGE_MISS,
                        isFromInput = true,
                        isMine = true,
                        note = note
                    )
                    hitNotes.add(note)
                    columnIndex[column] = index + 1
                    listener.onMineHit(column, note)
                }
                continue
            }

            if (note.type != Parser.NoteType.TAP &&
                note.type != Parser.NoteType.HOLD
            ) continue

            if (note.type == Parser.NoteType.HOLD && longNotes[column].pressed) {
                continue
            }

            val judge = getJudgeFromDelta(deltaMs)
            if (judge >= 0 && abs(deltaMs) < minimumAbsoluteDelta) {
                bestIndex = index
                bestJudge = judge
                minimumAbsoluteDelta = abs(deltaMs)
            }
        }

        if (bestIndex == -1) return
        val note = notesInColumn[bestIndex]

        if (note.type == Parser.NoteType.HOLD) {
            emitJudge(column, JUDGE_PERFECT, isFromInput = true, note = note)
            startLongNote(column, note, timeMs)
            longNotes[column].lastTickBeat = nowBeat
            columnIndex[column] = bestIndex + 1
        } else {
            emitJudge(column, bestJudge, isFromInput = true, note = note)
            hitNotes.add(note)
            columnIndex[column] = bestIndex + 1
        }
    }

    private fun startLongNote(column: Int, note: Parser.Note, timeMs: Double) {
        val longNote = longNotes[column]
        val nowBeat = timingData.timeToBeat(timeMs)
        longNote.pressed = true
        longNote.note = note
        longNote.timeStartedMs = timeMs
        val fromBeat = max(note.beat, nowBeat)
        longNote.lastTickBeat = fromBeat
        longNote.nextTickBeat = getNextHoldTickBeat(fromBeat)
        listener.onHoldStarted(column, note)
    }

    private fun getNextHoldTickBeat(fromBeat: Double): Double {
        val ticksPerBeat = findCurrentTick(fromBeat).coerceAtLeast(1.0)
        val separation = 1.0 / ticksPerBeat
        return floor(fromBeat / separation) * separation + separation
    }

    private fun endLongNote(column: Int, timeMs: Double) {
        val longNote = longNotes[column]
        val note = longNote.note ?: return
        val endBeat = note.endBeat ?: return
        val releaseBeat = timingData.timeToBeat(timeMs)
        val totalHoldBeats = endBeat - note.beat
        val remainingBeats = endBeat - releaseBeat
        val releaseToleranceBeats = totalHoldBeats * holdReleaseTolerance
        val acceptedAsComplete = remainingBeats <= releaseToleranceBeats

        if (acceptedAsComplete) {
            emitJudge(
                column, JUDGE_PERFECT,
                isBodyLongNote = true,
                isFromInput = true,
                note = note
            )
            finishedHolds.add(note)
            releasedHoldBeat.remove(note)
        } else {
            emitJudge(column, JUDGE_MISS, isFromInput = true, note = note)
            releasedHoldBeat[note] = releaseBeat
        }

        listener.onHoldReleased(
            column, note, releaseBeat, acceptedAsComplete
        )
        longNote.pressed = false
        longNote.note = null
    }

    private fun tryAutoStartHoldOnPress(column: Int, timeMs: Double) {
        if (longNotes[column].pressed) return
        val nowBeat = timingData.timeToBeat(timeMs.toDouble())
        val notesInColumn = columnNotes[column]

        for (index in notesInColumn.indices) {
            val note = notesInColumn[index]
            if (note.isFake) continue
            if (note.type != Parser.NoteType.HOLD) continue
            if (finishedHolds.contains(note)) continue
            if (timingData.isBeatInWarp(note.beat) && note.endBeat == null) {
                continue
            }
            val endBeat = note.endBeat ?: continue

            if (nowBeat in note.beat..endBeat) {
                emitJudge(
                    column, JUDGE_PERFECT,
                    isBodyLongNote = true,
                    isFromInput = true,
                    note = note
                )
                releasedHoldBeat.remove(note)
                startLongNote(column, note, timeMs)
                longNotes[column].lastTickBeat = nowBeat
                if (columnIndex[column] <= index) {
                    columnIndex[column] = index + 1
                }
                return
            }

            if (note.beat - nowBeat > config.lateHoldSearchBeats) break
        }
    }

    private fun updateReleasedHoldVisuals(songTimeMs: Double) {
        if (releasedHoldBeat.isEmpty()) return
        val iterator = releasedHoldBeat.iterator()

        while (iterator.hasNext()) {
            val note = iterator.next().key
            val endBeat = note.endBeat ?: continue
            val deltaMs = getDeltaMsForNote(endBeat, songTimeMs)
            if (deltaMs >= config.zoneBadMs) {
                finishedHolds.add(note)
                iterator.remove()
            }
        }
    }

    private fun processLongNoteTick(column: Int, timeMs: Double) {
        val longNote = longNotes[column]
        if (!longNote.pressed) return
        val note = longNote.note ?: return
        val nowBeat = timingData.timeToBeat(timeMs)
        val endBeat = note.endBeat ?: return

        if (nowBeat >= endBeat) {
            completeLongNote(column, note)
            return
        }

        while (nowBeat >= longNote.nextTickBeat &&
            longNote.nextTickBeat <= endBeat
        ) {
            val tickBeat = longNote.nextTickBeat
            if (!timingData.isBeatInWarp(tickBeat)) {
                emitJudge(
                    column = column,
                    judge = JUDGE_PERFECT,
                    isBodyLongNote = true,
                    isFromInput = true,
                    note = note
                )
            }

            longNote.lastTickBeat = tickBeat
            val ticksPerBeat = findCurrentTick(tickBeat).coerceAtLeast(1.0)
            longNote.nextTickBeat += 1.0 / ticksPerBeat
        }
    }

    private fun completeLongNote(column: Int, note: Parser.Note) {
        if (finishedHolds.contains(note)) return

        emitJudge(
            column, JUDGE_PERFECT,
            isBodyLongNote = true,
            isFromInput = true,
            note = note
        )
        finishedHolds.add(note)
        releasedHoldBeat.remove(note)

        val longNote = longNotes[column]
        if (longNote.note === note) {
            longNote.pressed = false
            longNote.note = null
            val endBeat = note.endBeat ?: longNote.lastTickBeat
            longNote.lastTickBeat = endBeat
            longNote.nextTickBeat = endBeat
        }
        listener.onHoldFinished(column, note)
    }

    private fun findCurrentTick(nowBeat: Double) =
        ticks.lastOrNull { it.beat <= nowBeat }?.tickCount ?: 4.0

    private fun updateComboMultiplier(nowBeat: Double) {
        while (comboSegmentIndex < combos.size &&
            nowBeat >= combos[comboSegmentIndex].beat
        ) {
            comboMultiplier = combos[comboSegmentIndex].multiplier
            comboSegmentIndex++
        }
    }

    private fun getDeltaMsForNote(noteBeat: Double, timeMs: Double): Long {
        return (timeMs - timingData.beatToTime(noteBeat)).toLong()
    }

    private fun getJudgeFromDelta(judgeTimeMs: Long): Int {
        val delta = abs(judgeTimeMs)
        return when {
            delta <= config.zonePerfectMs -> JUDGE_PERFECT
            delta <= config.zoneGreatMs -> JUDGE_GREAT
            delta <= config.zoneGoodMs -> JUDGE_GOOD
            else -> JUDGE_BAD
        }
    }

    private fun emitJudge(
        column: Int,
        judge: Int,
        isBodyLongNote: Boolean = false,
        isFromInput: Boolean,
        isMine: Boolean = false,
        note: Parser.Note? = null
    ) {
        if (note != null && note.isFake) return
        listener.onJudge(
            column,
            judge,
            comboMultiplier,
            isBodyLongNote,
            isFromInput,
            isMine,
            note
        )
    }

    private fun lowerBound(values: DoubleArray, target: Double): Int {
        var low = 0
        var high = values.size
        while (low < high) {
            val middle = (low + high).ushr(1)
            if (values[middle] < target) low = middle + 1 else high = middle
        }
        return low
    }

    private fun upperBound(values: DoubleArray, target: Double): Int {
        var low = 0
        var high = values.size
        while (low < high) {
            val middle = (low + high).ushr(1)
            if (values[middle] <= target) low = middle + 1 else high = middle
        }
        return low
    }

    private fun List<Parser.Note>.lowerBoundByBeat(target: Double): Int {
        var low = 0
        var high = size
        while (low < high) {
            val middle = (low + high).ushr(1)
            if (this[middle].beat < target) low = middle + 1 else high = middle
        }
        return low
    }
}
