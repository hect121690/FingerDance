package com.fingerdance.ssc

import com.badlogic.gdx.Gdx
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

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
        val targetY: Float = stepSize,
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
        val lateHoldSearchBeats: Double = 2.0,

        // StepMania 5.1 uses TW_Checkpoint = 0.1664 s for tick holds.
        val tickHoldsEnabled: Boolean = true,
        val checkpointWindowMs: Double = 166.4
    ) {
        init {
            require(columnCount > 0)
            require(activeColumns.first >= 0 && activeColumns.last < columnCount)
            require(stepSize > 0f)
            require(screenHeight > 0f)
            require(baseSpeed > 0f)
            require(checkpointWindowMs > 0.0)
        }
    }

    data class TickSegment(val beat: Double, val tickCount: Double)
    data class ComboSegment(val beat: Double, val multiplier: Int, val multiplierMiss: Int = 1)

    interface Renderer {
        fun drawTap(note: Parser.Note, column: Int, y: Int)
        fun drawMine(note: Parser.Note, column: Int, y: Int)
        fun drawHold(note: Parser.Note, column: Int, yHead: Int, yTail: Int)
    }

    interface Listener {
        fun onColumnActive(column: Int)
        fun onColumnPressed(column: Int) = Unit
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
        fun onNoteFlare(column: Int) = Unit
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
        private const val ROW_KEY_SCALE = 1_000_000.0
        private const val BEAT_EPSILON = 0.000001
        // Dos notas cuyo tiempo musical difiere menos que esto se consideran
        // co-temporales para una MISMA transición física de input.
        // 0.01 ms es muchísimo menor que cualquier chart humano normal, pero
        // cubre gimmicks como 100,000,000 BPM donde 0.25 beat = 0.00015 ms.
        private const val SAME_INPUT_TIME_EPSILON_MS = 0.01
    }

    private data class LongNoteState(
        // Activa lógicamente desde que se cacha/recacha hasta el tail.
        var pressed: Boolean = false,
        var note: Parser.Note? = null,
        var timeStartedMs: Double = 0.0,

        // Beat hasta el que ya inspeccionamos checkpoints para esta HOLD.
        // Así procesamos todas las subdivisiones cruzadas sin depender de FPS.
        var lastTickBeat: Double = 0.0,

        // StepMania/Pump checkpoint life. Mientras el panel está sostenido vuelve
        // a 1.0; soltado drena durante checkpointWindowMs (166.4 ms por defecto).
        var holdLife: Double = 1.0,
        var checkpointsHit: Int = 0,
        var checkpointsMissed: Int = 0,

        // Regla Finger Dance/Pump: un solo MISS por cada periodo realmente caído.
        // Al recatch se rearma para permitir un MISS en un drop posterior.
        var bodyMissEmitted: Boolean = false,

        // Blindaje: un checkpoint musical jamás se procesa dos veces.
        val processedTickRows: MutableSet<Long> = mutableSetOf()
    )

    private data class RowState(
        val beat: Double,
        val notes: List<Parser.Note>,
        val hitJudgments: MutableMap<Parser.Note, Int> = mutableMapOf(),
        var resolved: Boolean = false
    )

    private data class HoldRowEntry(
        val column: Int,
        val note: Parser.Note,
        val checkpointHit: Boolean = true,
        // Para un checkpoint/tail fallado, indica si este fallo debe emitir MISS.
        // Si la HOLD ya cobró su MISS por estar suelta, seguimos procesando
        // internamente la fila pero no volvemos a emitir otro MISS.
        val emitMiss: Boolean = true
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
    private val missedTapVisuals = mutableSetOf<Parser.Note>()

    private val noteRowKey = mutableMapOf<Parser.Note, Long>()
    private val rowStates = mutableMapOf<Long, RowState>()

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

        val rowCandidates = allNotes.filter { note ->
            note.column in config.activeColumns &&
                    !note.isFake &&
                    !note.isMine &&
                    timingData.isJudgableBeat(note.beat) &&
                    (note.type == Parser.NoteType.TAP || note.type == Parser.NoteType.HOLD)
        }

        for ((key, rowNotes) in rowCandidates.groupBy { rowKeyForBeat(it.beat) }) {
            val sortedRowNotes = rowNotes.sortedBy { it.column }
            rowStates[key] = RowState(beat = sortedRowNotes.first().beat, notes = sortedRowNotes)
            for (note in sortedRowNotes) noteRowKey[note] = key
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
        holdCompletedThisFrame.clear()
        require(input.size >= config.columnCount) {
            "El input tiene ${input.size} columnas y el motor necesita ${config.columnCount}."
        }

        for (column in 0 until config.columnCount) {
            keyState[column] = input[column]
        }

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
                    listener.onColumnPressed(column)
                    //tryAutoStartHoldOnPress(column, songTimeMs)
                }
                KEY_UP -> {
                    markHoldReleasedForVisual(column, songTimeMs)
                }
            }
        }

        // Activa pre-holds/recatches ANTES de recorrer checkpoints. Así, si un
        // frame cruza la cabeza y varios ticks (BPM extremo), no perdemos ticks.
        for (column in config.activeColumns) {
            val notesInColumn = columnNotes[column]

            if (
                !longNotes[column].pressed &&
                (keyState[column] == KEY_PRESS || keyState[column] == KEY_DOWN)
            ) {
                for (index in columnIndex[column] until notesInColumn.size) {
                    val note = notesInColumn[index]

                    if (note.isFake) continue
                    if (note.type != Parser.NoteType.HOLD) continue
                    if (finishedHolds.contains(note)) continue
                    if (!timingData.isJudgableBeat(note.beat)) continue

                    val endBeat = note.endBeat ?: continue
                    val headBeat = note.beat
                    if (nowBeat > endBeat) continue

                    val crossedHead =
                        (previousBeat <= headBeat && headBeat <= nowBeat) ||
                                (nowBeat <= headBeat && headBeat <= previousBeat) ||
                                abs(nowBeat - headBeat) < 0.001

                    val lateCatch = nowBeat > headBeat && nowBeat <= endBeat

                    if (crossedHead || lateCatch) {
                        listener.onNoteFlare(column)

                        val headResolved = isHoldHeadResolved(note)
                        if (!headResolved) {
                            registerRowHit(
                                column = column,
                                note = note,
                                judge = JUDGE_PERFECT,
                                isFromInput = true
                            )
                        }

                        startLongNote(
                            column = column,
                            note = note,
                            timeMs = songTimeMs,
                            scanFromBeat = if (!headResolved && crossedHead) headBeat else nowBeat
                        )

                        // Nunca saltes por encima de una nota anterior pendiente
                        // (por ejemplo un HIDE co-temporal). El cursor sólo avanza
                        // sobre notas realmente resueltas/no juzgables.
                        advanceColumnIndex(column)
                        break
                    }

                    if (headBeat > nowBeat && headBeat > previousBeat) break
                }
            }
        }

        updateHoldLife(songTimeMs, previousSongTimeMs)
        processLongNoteTicksByRow(songTimeMs)

        updateReleasedHoldVisuals(songTimeMs)
        updateAutoMisses(songTimeMs)
    }

    private fun processLongNoteTicksByRow(timeMs: Double) {
        val nowBeat = timingData.timeToBeat(timeMs)
        val tickRows = sortedMapOf<Long, MutableList<HoldRowEntry>>()
        val bottomRows = sortedMapOf<Long, MutableList<HoldRowEntry>>()

        for (column in config.activeColumns) {
            val longNote = longNotes[column]
            if (!longNote.pressed) continue

            val note = longNote.note ?: continue
            val endBeat = note.endBeat ?: continue
            val checkpointLimit = min(nowBeat, endBeat)

            // IMPORTANTE: usamos el cursor propio de la HOLD, no el previousBeat
            // global. Esto evita regalar ticks anteriores cuando se hace recatch.
            val scanFromBeat = maxOf(longNote.lastTickBeat, note.beat)

            if (checkpointLimit > scanFromBeat + BEAT_EPSILON) {
                val checkpoints = getCrossedHoldCheckpoints(
                    fromBeat = scanFromBeat,
                    toBeat = checkpointLimit,
                    holdStartBeat = note.beat,
                    holdEndBeat = endBeat
                )

                for (tickBeat in checkpoints) {
                    val rowKey = rowKeyForBeat(tickBeat)
                    if (!longNote.processedTickRows.add(rowKey)) continue

                    // Igual que StepMania: el checkpoint se considera vivo por
                    // HoldLife, no por una fotografía instantánea del input.
                    val checkpointHit = longNote.holdLife > 0.0

                    val emitMissForThisCheckpoint = if (checkpointHit) {
                        longNote.checkpointsHit++
                        true
                    } else {
                        longNote.checkpointsMissed++

                        // Regla Finger Dance/Pump: sólo UN MISS por este drop.
                        val shouldEmit = !longNote.bodyMissEmitted
                        if (shouldEmit) longNote.bodyMissEmitted = true
                        shouldEmit
                    }

                    tickRows.getOrPut(rowKey) { mutableListOf() }.add(
                        HoldRowEntry(
                            column = column,
                            note = note,
                            checkpointHit = checkpointHit,
                            emitMiss = emitMissForThisCheckpoint
                        )
                    )
                }

                // Ya inspeccionamos todo el intervalo aunque hubiera tick=0, fake
                // o warp. Nunca volvemos a recorrerlo en otro frame.
                longNote.lastTickBeat = checkpointLimit
            }

            // Tail: NO da PERFECT extra. Sólo cierra la HOLD.
            // Si la HOLD ya cayó (life=0) pero no hubo checkpoint posterior que
            // cobrara el MISS, el tail puede cobrar ese único MISS pendiente.
            if (nowBeat >= endBeat) {
                val tailAlive = longNote.holdLife > 0.0
                val rowKey = rowKeyForBeat(endBeat)

                bottomRows.getOrPut(rowKey) { mutableListOf() }.add(
                    HoldRowEntry(
                        column = column,
                        note = note,
                        checkpointHit = tailAlive,
                        emitMiss = !tailAlive && !longNote.bodyMissEmitted
                    )
                )
            }
        }

        // Una sola sentencia por ROW, aunque haya 2, 3 o más HOLDs simultáneas.
        for (entries in tickRows.values) {
            val representative = entries.firstOrNull() ?: continue
            val rowHit = entries.all { it.checkpointHit }

            if (rowHit) {
                for (entry in entries) listener.onNoteFlare(entry.column)

                emitJudge(
                    column = representative.column,
                    judge = JUDGE_PERFECT,
                    isBodyLongNote = true,
                    isFromInput = true,
                    note = representative.note
                )
            } else {
                val shouldEmitMiss = entries.any {
                    !it.checkpointHit && it.emitMiss
                }

                if (shouldEmitMiss) {
                    emitJudge(
                        column = representative.column,
                        judge = JUDGE_MISS,
                        isBodyLongNote = true,
                        isFromInput = false,
                        note = representative.note
                    )
                }
            }
        }

        for (entries in bottomRows.values) {
            val representative = entries.firstOrNull() ?: continue
            val rowAlive = entries.all { it.checkpointHit }

            if (!rowAlive) {
                val shouldEmitMiss = entries.any {
                    !it.checkpointHit && it.emitMiss
                }

                if (shouldEmitMiss) {
                    emitJudge(
                        column = representative.column,
                        judge = JUDGE_MISS,
                        isBodyLongNote = true,
                        isFromInput = false,
                        note = representative.note
                    )
                }
            }

            for (entry in entries) {
                finishLongNoteWithoutJudge(entry.column, entry.note)
            }
        }
    }

    /**
     * Devuelve TODOS los checkpoints de HOLD cruzados entre (fromBeat, toBeat].
     *
     * Inspirado en Player::CrossedRows de StepMania: se revisa el TICKCOUNT
     * vigente para cada región musical atravesada, no se limita a un tick/frame.
     *
     * A diferencia del ROWS_PER_BEAT=48 de StepMania, aquí usamos la fracción
     * exacta 1/tickCount para no limitar charts que traigan 64, 128, etc.
     */
    private fun getCrossedHoldCheckpoints(
        fromBeat: Double,
        toBeat: Double,
        holdStartBeat: Double,
        holdEndBeat: Double
    ): List<Double> {
        if (toBeat <= fromBeat + BEAT_EPSILON) return emptyList()

        val result = mutableListOf<Double>()
        val effectiveTicks = if (ticks.isEmpty()) {
            listOf(TickSegment(0.0, 4.0))
        } else {
            ticks
        }

        for (index in effectiveTicks.indices) {
            val segment = effectiveTicks[index]
            val tickCount = segment.tickCount
            val segmentStart = segment.beat
            val segmentEnd = effectiveTicks.getOrNull(index + 1)?.beat
                ?: Double.POSITIVE_INFINITY

            if (!tickCount.isFinite() || tickCount <= 0.0) continue
            if (segmentEnd <= fromBeat + BEAT_EPSILON) continue
            if (segmentStart > toBeat + BEAT_EPSILON) break

            val lower = maxOf(fromBeat, holdStartBeat, segmentStart)
            val upper = minOf(toBeat, holdEndBeat, segmentEnd)
            if (upper <= lower + BEAT_EPSILON) continue

            // Grid global: tick=16 => n/16 beat, tick=128 => n/128 beat.
            // Esto coincide con la semántica musical y permite procesar varios
            // checkpoints dentro del mismo frame.
            var n = kotlin.math.floor(lower * tickCount + BEAT_EPSILON).toLong() + 1L
            var candidate = n / tickCount
            var guard = 0

            // Si el inicio del segmento activo está después de fromBeat y cae
            // exactamente en su grid, también es checkpoint (ej. 0 -> 16).
            val segmentGrid = segmentStart * tickCount
            val segmentOnGrid = kotlin.math.abs(segmentGrid - kotlin.math.round(segmentGrid)) <= 0.000001
            if (
                segmentStart > fromBeat + BEAT_EPSILON &&
                segmentStart > holdStartBeat + BEAT_EPSILON &&
                segmentStart <= upper + BEAT_EPSILON &&
                segmentOnGrid
            ) {
                candidate = segmentStart
                n = kotlin.math.round(segmentGrid).toLong()
            }

            while (guard++ < 1_000_000) {
                if (candidate > upper + BEAT_EPSILON) break
                if (candidate >= segmentEnd - BEAT_EPSILON) break
                if (candidate >= holdEndBeat - BEAT_EPSILON) break

                if (
                    candidate > fromBeat + BEAT_EPSILON &&
                    candidate > holdStartBeat + BEAT_EPSILON &&
                    timingData.isJudgableBeat(candidate)
                ) {
                    result.add(candidate)
                }

                n++
                candidate = n / tickCount
            }
        }

        // Puede haber fronteras compartidas entre segmentos. La key discreta
        // garantiza una sola entrada por checkpoint musical.
        return result
            .distinctBy(::rowKeyForBeat)
            .sorted()
    }

    private fun finishLongNoteWithoutJudge(column: Int, note: Parser.Note) {
        if (finishedHolds.contains(note)) return

        finishedHolds.add(note)
        releasedHoldBeat.remove(note)
        holdCompletedThisFrame.add(note)

        val longNote = longNotes[column]

        if (longNote.note === note) {
            longNote.pressed = false
            longNote.note = null

            val endBeat = note.endBeat ?: longNote.lastTickBeat
            longNote.lastTickBeat = endBeat
        }

        listener.onHoldFinished(column, note)
    }

    private fun updateHoldLife(songTimeMs: Double, previousSongTimeMs: Double) {
        val deltaMs = (songTimeMs - previousSongTimeMs).coerceAtLeast(0.0)
        if (deltaMs <= 0.0) return

        for (column in config.activeColumns) {
            val longNote = longNotes[column]
            if (!longNote.pressed || longNote.note == null) continue

            if (isColumnPhysicallyHeld(column)) {
                val wasDropped = longNote.holdLife <= 0.0
                longNote.holdLife = 1.0

                // Recatch real: vuelve a habilitar un único MISS para un
                // eventual drop posterior.
                if (wasDropped) {
                    longNote.bodyMissEmitted = false
                }

                releasedHoldBeat.remove(longNote.note!!)
            } else {
                val drain = deltaMs / config.checkpointWindowMs
                longNote.holdLife =
                    (longNote.holdLife - drain).coerceAtLeast(0.0)
            }
        }
    }

    private fun isColumnPhysicallyHeld(column: Int): Boolean {
        return keyState[column] == KEY_DOWN || keyState[column] == KEY_PRESS
    }

    private fun markHoldReleasedForVisual(column: Int, timeMs: Double) {
        val longNote = longNotes[column]
        if (!longNote.pressed) return
        val note = longNote.note ?: return
        releasedHoldBeat[note] = timingData.timeToBeat(timeMs)
    }

    fun reset() {
        hitNotes.clear()
        missedTapVisuals.clear()
        finishedHolds.clear()
        releasedHoldBeat.clear()
        holdCompletedThisFrame.clear()
        for (rowState in rowStates.values) {
            rowState.hitJudgments.clear()
            rowState.resolved = false
        }
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
        val isMissVisual = missedTapVisuals.contains(note)
        if (hitNotes.contains(note) && !isMissVisual) return
        val offset = offsetForBeat(note.beat, currentBeat, songTimeMs)
        if (!isOffsetVisible(offset)) {
            if (isMissVisual && offset <= config.drawDistanceAfterTargetsPx) {
                missedTapVisuals.remove(note)
            }
            return
        }

        val y = config.targetY.toInt() + offset.toInt()
        if (note.isMine){
            renderer.drawMine(note, note.column, y)
        }
        else {
            renderer.drawTap(note, note.column, y)
        }
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
            longNotes[column].pressed &&
                    longNotes[column].note === note &&
                    isColumnPhysicallyHeld(column)

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
        renderer.drawHold(note, column, config.targetY.toInt() + headOffset.toInt(), config.targetY.toInt() + tailOffset.toInt())
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
                if (!timingData.isJudgableBeat(note.beat)) {
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
                    resolveRowAsMiss(note)
                    index = columnIndex[column]
                    continue
                }
                break
            }
        }
    }

    /**
     * Procesa una transición KEY_DOWN en una columna.
     *
     * Regla normal: encuentra la mejor nota dentro de la ventana de judgment.
     *
     * Regla para gimmicks de BPM extremo: una vez encontrada esa nota de referencia,
     * también procesa otras TAP/HOLD de la MISMA columna cuyo beatToTime() cae
     * prácticamente en el mismo instante físico. Esto NO usa un rango de 1 ms ni
     * "todas las notas cruzadas en el frame"; sólo agrupa tiempos verdaderamente
     * co-temporales (<= 0.01 ms), para no regalar notas en charts densos normales.
     *
     * WARP y FAKE siguen fuera del scoring mediante isJudgableBeat().
     * HIDE se juzga igual que TAP normal; únicamente se omite en render().
     */
    private fun processTapAndHeadOnColumn(column: Int, timeMs: Double) {
        val notesInColumn = columnNotes[column]
        val startIndex = columnIndex[column]

        var bestIndex = -1
        var bestJudge = -1
        var minimumAbsoluteDelta = Long.MAX_VALUE
        val endExclusive = min(startIndex + config.tapSearchLimit, notesInColumn.size)

        // 1) Encontrar la mejor nota principal para ESTA pulsación.
        for (index in startIndex until endExclusive) {
            val note = notesInColumn[index]

            if (note.isFake) continue
            if (hitNotes.contains(note)) continue
            if (!timingData.isJudgableBeat(note.beat)) continue

            val deltaMs = getDeltaMsForNote(note.beat, timeMs)

            if (deltaMs < -config.zoneBadMs) {
                break
            }

            if (note.isMine) {
                if (abs(deltaMs) <= config.zoneBadMs) {
                    emitJudge(
                        column = column,
                        judge = JUDGE_MISS,
                        isFromInput = true,
                        isMine = true,
                        note = note
                    )

                    hitNotes.add(note)
                    listener.onMineHit(column, note)
                    advanceColumnIndex(column)
                }
                continue
            }

            if (
                note.type != Parser.NoteType.TAP &&
                note.type != Parser.NoteType.HOLD
            ) {
                continue
            }

            if (
                note.type == Parser.NoteType.HOLD &&
                longNotes[column].pressed
            ) {
                continue
            }

            val judge = getJudgeFromDelta(deltaMs)

            if (
                judge >= 0 &&
                abs(deltaMs) < minimumAbsoluteDelta
            ) {
                bestIndex = index
                bestJudge = judge
                minimumAbsoluteDelta = abs(deltaMs)
            }
        }

        if (bestIndex == -1) return

        val referenceNote = notesInColumn[bestIndex]
        val referenceTimeMs = timingData.beatToTime(referenceNote.beat)

        // 2) Crear el cluster co-temporal. Siempre contiene la mejor nota.
        // Sólo recorremos el mismo límite corto usado para TAP search.
        val cluster = mutableListOf<Pair<Parser.Note, Int>>()

        for (index in startIndex until endExclusive) {
            val note = notesInColumn[index]

            if (note.isFake) continue
            if (hitNotes.contains(note)) continue
            if (!timingData.isJudgableBeat(note.beat)) continue
            if (note.isMine) continue

            if (
                note.type != Parser.NoteType.TAP &&
                note.type != Parser.NoteType.HOLD
            ) {
                continue
            }

            val noteTimeMs = timingData.beatToTime(note.beat)
            val samePhysicalInstant =
                abs(noteTimeMs - referenceTimeMs) <= SAME_INPUT_TIME_EPSILON_MS

            if (!samePhysicalInstant) continue

            val judge = getJudgeFromDelta(getDeltaMsForNote(note.beat, timeMs))
            if (judge < 0) continue

            cluster.add(note to judge)
        }

        if (cluster.isEmpty()) {
            // Blindaje: por redondeos extremos, la referencia siempre debe entrar;
            // si no ocurre, procesamos al menos la seleccionada originalmente.
            cluster.add(referenceNote to bestJudge)
        }

        // 3) Procesar en orden musical. En tu caso: HIDE 40.00 -> HOLD 40.25.
        for ((note, judge) in cluster.sortedBy { it.first.beat }) {
            if (hitNotes.contains(note)) continue

            when (note.type) {
                Parser.NoteType.TAP -> {
                    listener.onNoteFlare(column)

                    registerRowHit(
                        column = column,
                        note = note,
                        judge = judge,
                        isFromInput = true
                    )

                    hitNotes.add(note)
                }

                Parser.NoteType.HOLD -> {
                    if (longNotes[column].pressed) continue

                    listener.onNoteFlare(column)

                    val headResolved = isHoldHeadResolved(note)
                    if (!headResolved) {
                        registerRowHit(
                            column = column,
                            note = note,
                            judge = JUDGE_PERFECT,
                            isFromInput = true
                        )
                    }

                    startLongNote(
                        column = column,
                        note = note,
                        timeMs = timeMs,
                        scanFromBeat = note.beat
                    )
                }
            }
        }

        // El cursor avanza sólo sobre lo realmente resuelto.
        advanceColumnIndex(column)
    }

    private fun registerRowHit(
        column: Int,
        note: Parser.Note,
        judge: Int,
        isFromInput: Boolean
    ) {
        val key = noteRowKey[note]
        val rowState = key?.let(rowStates::get)

        if (rowState == null) {
            emitJudge(column, judge, isFromInput = isFromInput, note = note)
            return
        }
        if (rowState.resolved || rowState.hitJudgments.containsKey(note)) return

        rowState.hitJudgments[note] = judge
        if (rowState.hitJudgments.size == rowState.notes.size) {
            val rowJudge = rowState.hitJudgments.values.maxOrNull() ?: JUDGE_MISS
            resolveRow(rowState, rowJudge, column, isFromInput, note)
        }
    }

    private fun resolveRowAsMiss(note: Parser.Note) {
        val key = noteRowKey[note]
        val rowState = key?.let(rowStates::get)

        if (rowState == null) {
            emitJudge(note.column, JUDGE_MISS, isFromInput = false, note = note)
            if (note.type == Parser.NoteType.TAP) {
                missedTapVisuals.add(note)
            }
            consumeNote(note)
            return
        }
        if (rowState.resolved) {
            consumeRowNotes(rowState)
            return
        }

        resolveRow(
            rowState = rowState,
            judge = JUDGE_MISS,
            column = note.column,
            isFromInput = rowState.hitJudgments.isNotEmpty(),
            representativeNote = note
        )
    }

    private fun resolveRow(
        rowState: RowState,
        judge: Int,
        column: Int,
        isFromInput: Boolean,
        representativeNote: Parser.Note
    ) {
        if (rowState.resolved) return
        rowState.resolved = true

        emitJudge(
            column = column,
            judge = judge,
            isFromInput = isFromInput,
            note = representativeNote
        )

        if (judge == JUDGE_MISS) {
            for (note in rowState.notes) {
                if (
                    note.type == Parser.NoteType.TAP &&
                    !rowState.hitJudgments.containsKey(note)
                ) {
                    missedTapVisuals.add(note)
                }
            }

            consumeRowNotes(rowState)
        }
    }

    private fun advanceColumnIndex(column: Int) {
        val notesInColumn = columnNotes[column]
        var index = columnIndex[column]

        while (index < notesInColumn.size) {
            val note = notesInColumn[index]

            val rowHit = noteRowKey[note]
                ?.let(rowStates::get)
                ?.hitJudgments
                ?.containsKey(note) == true

            val resolved = note.isFake ||
                    !timingData.isJudgableBeat(note.beat) ||
                    hitNotes.contains(note) ||
                    finishedHolds.contains(note) ||
                    rowHit

            if (!resolved) break
            index++
        }

        columnIndex[column] = index
    }

    private fun consumeRowNotes(rowState: RowState) {
        for (note in rowState.notes) consumeNote(note)
    }

    private fun consumeNote(note: Parser.Note) {
        hitNotes.add(note)
        val column = note.column
        if (column !in 0 until config.columnCount) return

        val index = columnNotes[column].indexOf(note)
        if (index >= 0 && columnIndex[column] <= index) {
            columnIndex[column] = index + 1
        }
    }

    private fun rowKeyForBeat(beat: Double): Long {
        return round(beat * ROW_KEY_SCALE).toLong()
    }

    private fun startLongNote(
        column: Int,
        note: Parser.Note,
        timeMs: Double,
        scanFromBeat: Double = timingData.timeToBeat(timeMs)
    ) {
        val longNote = longNotes[column]

        longNote.pressed = true
        longNote.note = note
        longNote.timeStartedMs = timeMs
        longNote.holdLife = 1.0
        longNote.checkpointsHit = 0
        longNote.checkpointsMissed = 0
        longNote.bodyMissEmitted = false
        longNote.processedTickRows.clear()

        releasedHoldBeat.remove(note)

        // Si fue recatch después de una cabeza perdida, empezamos desde el beat
        // del recatch y jamás regalamos checkpoints anteriores.
        longNote.lastTickBeat = max(note.beat, scanFromBeat)

        listener.onHoldStarted(column, note)
    }

    private fun isHoldHeadResolved(note: Parser.Note): Boolean {
        if (hitNotes.contains(note)) return true
        if (finishedHolds.contains(note)) return true

        val rowState = noteRowKey[note]?.let(rowStates::get)
        return rowState?.resolved == true ||
                rowState?.hitJudgments?.containsKey(note) == true
    }

    private fun tryAutoStartHoldOnPress(column: Int, timeMs: Double) {
        if (longNotes[column].pressed) return

        val nowBeat = timingData.timeToBeat(timeMs)
        val notesInColumn = columnNotes[column]

        for (index in notesInColumn.indices) {
            val note = notesInColumn[index]

            if (note.isFake) continue
            if (note.type != Parser.NoteType.HOLD) continue
            if (finishedHolds.contains(note)) continue
            if (!timingData.isJudgableBeat(note.beat)) continue

            val endBeat = note.endBeat ?: continue

            if (nowBeat in note.beat..endBeat) {
                listener.onNoteFlare(column)

                val headResolved = isHoldHeadResolved(note)
                if (!headResolved) {
                    registerRowHit(
                        column = column,
                        note = note,
                        judge = JUDGE_PERFECT,
                        isFromInput = true
                    )
                }

                startLongNote(
                    column = column,
                    note = note,
                    timeMs = timeMs,
                    scanFromBeat = if (headResolved) nowBeat else note.beat
                )

                // Igual que arriba: nunca saltar una nota anterior pendiente.
                advanceColumnIndex(column)

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
            if (longNotes.getOrNull(note.column)?.note === note) {
                continue
            }
            val endBeat = note.endBeat ?: continue
            val deltaMs = getDeltaMsForNote(endBeat, songTimeMs)
            if (deltaMs >= config.zoneBadMs) {
                finishedHolds.add(note)
                iterator.remove()
            }
        }
    }


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
            delta <= config.zoneBadMs -> JUDGE_BAD
            else -> -1
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
