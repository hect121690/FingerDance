package com.fingerdance.ssc

import kotlin.math.abs
import kotlin.math.round

class ParserKsf {

    companion object {
        private const val EPSILON_BEAT = 0.0000001
        private const val EPSILON_MS = 0.001
    }

    // =====================================================
    // DATOS INTERNOS
    // =====================================================

    private enum class KsfNoteType {
        NORMAL,
        FAKE,
        PHANTOM
    }

    private data class ActiveHold(
        val startBeat: Double,
        var lastBeat: Double,
        val type: KsfNoteType
    )

    private data class ParsedHeader(
        val values: Map<String, String>
    ) {

        fun get(tag: String): String? {
            return values[tag.uppercase()]
        }

        fun getDouble(
            tag: String,
            default: Double = 0.0
        ): Double {
            return get(tag)
                ?.trim()
                ?.toDoubleOrNull()
                ?: default
        }

        fun getInt(
            tag: String,
            default: Int = 0
        ): Int {
            return get(tag)
                ?.trim()
                ?.toDoubleOrNull()
                ?.toInt()
                ?: default
        }
    }

    /**
     * Representa una sección KSF cuya duración temporal es negativa.
     *
     * Los beats del Chart siempre continúan hacia delante.
     * Cuando el tiempo positivo posterior compensa la duración negativa,
     * se crea un Warp para eliminar del timeline SSC el tiempo agregado
     * artificialmente por la normalización de BPM.
     */
    private data class ReverseTimingSection(
        val startBeat: Double,
        var negativeDurationMs: Double = 0.0,
        var recoveryDurationMs: Double = 0.0
    )

    private data class ParsingState(
        var currentBeat: Double,

        /*
         * Valores normalizados para Parser.Chart.
         * Siempre se almacenan como positivos.
         */
        var currentBpm: Double,
        var currentTickcount: Int,

        /*
         * Valores originales del archivo KSF.
         * Sus signos se conservan para interpretar los efectos.
         */
        var rawBpm: Double,
        var rawTickcount: Int,

        var reverseSection: ReverseTimingSection? = null
    )

    private data class ParsedRow(
        val taps: List<Parser.Note>,
        val activeLongColumns: Set<Int>
    )

    // =====================================================
    // API PÚBLICA
    // =====================================================

    fun parseKSF(
        text: String,
        pathFile: String = "",
        timeAdjust: Double = 0.0,
        valueOffset: Double = 0.0
    ): Parser.Chart {

        val normalizedText = normalizeText(text)

        val header = parseHeader(normalizedText)
        val stepLines = extractStepLines(normalizedText)

        val rawInitialBpm = header.getDouble(
            tag = "BPM",
            default = 120.0
        )

        val rawInitialTickcount = header.getInt(
            tag = "TICKCOUNT",
            default = 4
        )

        val initialBpm = normalizeBpm(rawInitialBpm)

        val initialTickcount = normalizeTickcount(
            rawInitialTickcount
        )

        /*
         * KSF expresa STARTTIME en unidades de 10 ms.
         *
         * Se conserva el signo que actualmente utilizas:
         *
         * offset = STARTTIME * 10 / 1000
         */
        val startTimeValue = header.getDouble(
            tag = "STARTTIME",
            default = 0.0
        )

        val startTimeMs =
            (
                    round(startTimeValue) +
                            timeAdjust +
                            valueOffset
                    ) * 10.0

        val offsetSeconds = startTimeMs / 1000.0

        val notes = mutableListOf<Parser.Note>()

        val bpms = mutableListOf<Parser.BpmSegment>()
        val tickcounts = mutableListOf<Parser.TickCountSegment>()

        val stops = mutableListOf<Parser.Stop>()
        val delays = mutableListOf<Parser.Delay>()
        val warps = mutableListOf<Parser.Warp>()

        val speeds = mutableListOf<Parser.Speed>()
        val scrolls = mutableListOf<Parser.Scroll>()

        val activeHolds = mutableMapOf<Int, ActiveHold>()

        val state = ParsingState(
            currentBeat = 0.0,

            currentBpm = initialBpm,
            currentTickcount = initialTickcount,

            rawBpm = rawInitialBpm,
            rawTickcount = rawInitialTickcount,

            reverseSection = null
        )

        /*
         * Eventos iniciales.
         */
        bpms.add(
            Parser.BpmSegment(
                beat = 0.0,
                bpm = initialBpm
            )
        )

        tickcounts.add(
            Parser.TickCountSegment(
                beat = 0.0,
                tickcount = initialTickcount
            )
        )

        /*
         * La dirección visual depende solamente del BPM.
         */
        scrolls.add(
            Parser.Scroll(
                beat = 0.0,
                ratio = getScrollDirection(rawInitialBpm)
            )
        )

        for (rawLine in stepLines) {

            val line = rawLine.trim()

            if (line.isEmpty()) {
                continue
            }

            if (isEventLine(line)) {

                parseEvent(
                    line = line,
                    state = state,

                    bpms = bpms,
                    tickcounts = tickcounts,

                    stops = stops,
                    delays = delays,

                    speeds = speeds,
                    scrolls = scrolls
                )

                continue
            }

            if (!isStepRow(line)) {
                continue
            }

            val playableRow = extractPlayableColumns(
                row = line,
                player = header.get("PLAYER")
            )

            val parsedRow = parseStepRow(
                row = playableRow,
                beat = state.currentBeat,
                activeHolds = activeHolds
            )

            /*
             * Una long note KSF termina cuando la columna deja
             * de contener 4, L o H.
             */
            closeFinishedHolds(
                currentLongColumns = parsedRow.activeLongColumns,
                activeHolds = activeHolds,
                output = notes
            )

            notes.addAll(parsedRow.taps)

            /*
             * Analizamos primero la duración temporal real de la fila.
             */
            processRowTiming(
                state = state,
                warps = warps
            )

            /*
             * En Parser.Chart los beats siempre son monotónicos.
             * Nunca hacemos retroceder currentBeat.
             */
            state.currentBeat += getRowBeatLength(state)
        }

        closeAllHolds(
            activeHolds = activeHolds,
            output = notes
        )

        /*
         * Cierra cualquier sección temporal inversa que permanezca
         * abierta al terminar el archivo.
         */
        closePendingReverseSection(
            state = state,
            warps = warps
        )

        return Parser.Chart(
            chartPath = pathFile,

            offset = offsetSeconds,

            bpms = normalizeBpms(bpms),

            tickcounts = normalizeTickcounts(
                tickcounts
            ),

            stops = stops.sortedBy {
                it.beat
            },

            delays = delays.sortedBy {
                it.beat
            },

            warps = normalizeWarps(
                warps
            ),

            fakes = emptyList(),

            speeds = speeds.sortedBy {
                it.beat
            },

            scrolls = normalizeScrolls(
                scrolls
            ),

            combos = emptyList(),

            notes = notes.sortedWith(
                compareBy<Parser.Note> {
                    it.beat
                }.thenBy {
                    it.column
                }
            ),

            fgChanges = mutableListOf()
        )
    }

    // =====================================================
    // TIMING NEGATIVO
    // =====================================================

    /**
     * Procesa la duración real de cada fila según los signos originales.
     *
     * La dirección temporal depende del producto BPM × TICKCOUNT:
     *
     *  BPM + / TICK +  = tiempo hacia delante
     *  BPM - / TICK -  = tiempo hacia delante
     *  BPM - / TICK +  = tiempo hacia atrás
     *  BPM + / TICK -  = tiempo hacia atrás
     *
     * Esto es independiente de la dirección visual.
     */
    private fun processRowTiming(
        state: ParsingState,
        warps: MutableList<Parser.Warp>
    ) {

        val rowDurationMs = getAbsoluteRowDurationMs(state)

        if (rowDurationMs <= 0.0) {
            return
        }

        if (isReverseTiming(state)) {

            /*
             * Comienza o continúa una sección de tiempo negativo.
             */
            val section =
                state.reverseSection
                    ?: ReverseTimingSection(
                        startBeat = state.currentBeat
                    ).also {
                        state.reverseSection = it
                    }

            section.negativeDurationMs += rowDurationMs
            return
        }

        /*
         * Si no existe una sección negativa pendiente,
         * esta es simplemente una fila normal.
         */
        val section = state.reverseSection
            ?: return

        /*
         * La fila positiva está recuperando el tiempo negativo anterior.
         */
        section.recoveryDurationMs += rowDurationMs

        if (
            section.recoveryDurationMs + EPSILON_MS >=
            section.negativeDurationMs
        ) {

            /*
             * El warp incluye toda la sección leída:
             *
             * - filas negativas normalizadas como positivas;
             * - filas positivas utilizadas para recuperar ese tiempo.
             */
            val rowEndBeat =
                state.currentBeat + getRowBeatLength(state)

            val warpDuration =
                rowEndBeat - section.startBeat

            if (warpDuration > EPSILON_BEAT) {

                warps.add(
                    Parser.Warp(
                        beat = section.startBeat,
                        duration = warpDuration
                    )
                )
            }

            state.reverseSection = null
        }
    }

    /**
     * Indica si el tiempo de la fila KSF es negativo.
     *
     * Importante:
     *
     * Esto NO controla el scroll visual.
     * El scroll visual depende solamente del signo del BPM.
     */
    private fun isReverseTiming(
        state: ParsingState
    ): Boolean {

        if (
            state.rawBpm == 0.0 ||
            state.rawTickcount == 0
        ) {
            return false
        }

        return state.rawBpm *
                state.rawTickcount.toDouble() < 0.0
    }

    private fun getAbsoluteRowDurationMs(
        state: ParsingState
    ): Double {

        val bpm = abs(state.rawBpm)
        val tickcount = abs(state.rawTickcount)

        if (
            bpm <= 0.0 ||
            tickcount <= 0
        ) {
            return 0.0
        }

        return 60000.0 /
                bpm /
                tickcount.toDouble()
    }

    private fun getRowBeatLength(
        state: ParsingState
    ): Double {

        return 1.0 /
                state.currentTickcount
                    .coerceAtLeast(1)
                    .toDouble()
    }

    private fun closePendingReverseSection(
        state: ParsingState,
        warps: MutableList<Parser.Warp>
    ) {

        val section = state.reverseSection
            ?: return

        val warpDuration =
            state.currentBeat - section.startBeat

        if (warpDuration > EPSILON_BEAT) {

            warps.add(
                Parser.Warp(
                    beat = section.startBeat,
                    duration = warpDuration
                )
            )
        }

        state.reverseSection = null
    }

    // =====================================================
    // HEADER
    // =====================================================

    private fun normalizeText(
        text: String
    ): String {

        return text
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .removePrefix("\uFEFF")
    }

    private fun parseHeader(
        text: String
    ): ParsedHeader {

        val values = mutableMapOf<String, String>()

        val stepIndex = findStepTagIndex(text)

        val headerText =
            if (stepIndex >= 0) {

                text.substring(
                    startIndex = 0,
                    endIndex = stepIndex
                )

            } else {

                text
            }

        val regex = Regex(
            pattern = """#([A-Za-z0-9_]+)\s*:\s*(.*?);""",
            option = RegexOption.DOT_MATCHES_ALL
        )

        regex.findAll(headerText).forEach { match ->

            val tag = match.groupValues[1]
                .trim()
                .uppercase()

            val value = match.groupValues[2]
                .trim()

            values[tag] = value
        }

        return ParsedHeader(values)
    }

    private fun findStepTagIndex(
        text: String
    ): Int {

        val regex = Regex(
            pattern = """(?im)^\s*#STEP\s*:"""
        )

        return regex.find(text)
            ?.range
            ?.first
            ?: -1
    }

    private fun extractStepLines(
        text: String
    ): List<String> {

        val stepIndex = findStepTagIndex(text)

        if (stepIndex < 0) {
            return emptyList()
        }

        val colonIndex = text.indexOf(
            char = ':',
            startIndex = stepIndex
        )

        if (
            colonIndex < 0 ||
            colonIndex + 1 >= text.length
        ) {
            return emptyList()
        }

        return text
            .substring(colonIndex + 1)
            .lineSequence()
            .map {
                it.trim()
            }
            .filter {
                it.isNotEmpty()
            }
            .filterNot {
                it.startsWith("//")
            }
            .takeWhile {
                it != ";"
            }
            .toList()
    }

    // =====================================================
    // NOTAS
    // =====================================================

    private fun parseStepRow(
        row: String,
        beat: Double,
        activeHolds: MutableMap<Int, ActiveHold>
    ): ParsedRow {

        val taps = mutableListOf<Parser.Note>()
        val currentLongColumns = mutableSetOf<Int>()

        row.forEachIndexed { column, rawValue ->

            when (rawValue.uppercaseChar()) {

                /*
                 * TAP normal.
                 */
                '1' -> {

                    taps.add(
                        Parser.Note(
                            column = column,
                            beat = beat,
                            type = Parser.NoteType.TAP
                        )
                    )
                }

                /*
                 * TAP fake.
                 */
                'F' -> {

                    taps.add(
                        Parser.Note(
                            column = column,
                            beat = beat,
                            isFake = true,
                            type = Parser.NoteType.TAP
                        )
                    )
                }

                /*
                 * TAP phantom.
                 */
                'P' -> {

                    taps.add(
                        Parser.Note(
                            column = column,
                            beat = beat,
                            isPhantom = true,
                            type = Parser.NoteType.TAP
                        )
                    )
                }

                /*
                 * Mina.
                 */
                'M' -> {

                    taps.add(
                        Parser.Note(
                            column = column,
                            beat = beat,
                            isMine = true,
                            type = Parser.NoteType.TAP
                        )
                    )
                }

                /*
                 * Long normal.
                 */
                '4' -> {

                    registerLongBody(
                        column = column,
                        beat = beat,
                        type = KsfNoteType.NORMAL,
                        activeHolds = activeHolds
                    )

                    currentLongColumns.add(column)
                }

                /*
                 * Long fake.
                 */
                'L' -> {

                    registerLongBody(
                        column = column,
                        beat = beat,
                        type = KsfNoteType.FAKE,
                        activeHolds = activeHolds
                    )

                    currentLongColumns.add(column)
                }

                /*
                 * Long phantom.
                 */
                'H' -> {

                    registerLongBody(
                        column = column,
                        beat = beat,
                        type = KsfNoteType.PHANTOM,
                        activeHolds = activeHolds
                    )

                    currentLongColumns.add(column)
                }

                '0', '2' -> Unit
            }
        }

        return ParsedRow(
            taps = taps,
            activeLongColumns = currentLongColumns
        )
    }

    private fun registerLongBody(
        column: Int,
        beat: Double,
        type: KsfNoteType,
        activeHolds: MutableMap<Int, ActiveHold>
    ) {

        val currentHold = activeHolds[column]

        if (currentHold == null) {

            activeHolds[column] = ActiveHold(
                startBeat = beat,
                lastBeat = beat,
                type = type
            )

        } else {

            currentHold.lastBeat = beat
        }
    }

    private fun closeFinishedHolds(
        currentLongColumns: Set<Int>,
        activeHolds: MutableMap<Int, ActiveHold>,
        output: MutableList<Parser.Note>
    ) {

        val columnsToClose = activeHolds.keys
            .filterNot { column ->
                column in currentLongColumns
            }

        columnsToClose.forEach { column ->

            val hold = activeHolds.remove(column)
                ?: return@forEach

            output.add(
                createNoteFromHold(
                    column = column,
                    hold = hold
                )
            )
        }
    }

    private fun closeAllHolds(
        activeHolds: MutableMap<Int, ActiveHold>,
        output: MutableList<Parser.Note>
    ) {

        val remainingHolds = activeHolds.toMap()

        activeHolds.clear()

        remainingHolds.forEach { (column, hold) ->

            output.add(
                createNoteFromHold(
                    column = column,
                    hold = hold
                )
            )
        }
    }

    private fun createNoteFromHold(
        column: Int,
        hold: ActiveHold
    ): Parser.Note {

        val isSingleRow = approximatelyEqual(
            first = hold.startBeat,
            second = hold.lastBeat
        )

        val isFake =
            hold.type == KsfNoteType.FAKE

        val isPhantom =
            hold.type == KsfNoteType.PHANTOM

        return if (isSingleRow) {

            Parser.Note(
                column = column,
                beat = hold.startBeat,

                isFake = isFake,
                isPhantom = isPhantom,

                type = Parser.NoteType.TAP
            )

        } else {

            Parser.Note(
                column = column,
                beat = hold.startBeat,
                endBeat = hold.lastBeat,

                isFake = isFake,
                isPhantom = isPhantom,

                type = Parser.NoteType.HOLD
            )
        }
    }

    // =====================================================
    // COLUMNAS JUGABLES
    // =====================================================

    private fun extractPlayableColumns(
        row: String,
        player: String?
    ): String {

        val normalizedPlayer = player
            ?.trim()
            ?.uppercase()
            .orEmpty()

        return when {

            normalizedPlayer.contains("HALF") -> {

                if (row.length >= 11) {

                    row.substring(
                        startIndex = 1,
                        endIndex = 11
                    )

                } else {

                    row
                        .take(10)
                        .padEnd(10, '0')
                }
            }

            normalizedPlayer.contains("DOUBLE") -> {

                if (row.length >= 13) {

                    row.substring(0, 5) +
                            row.substring(8, 13)

                } else {

                    row
                        .take(10)
                        .padEnd(10, '0')
                }
            }

            else -> {

                /*
                 * SINGLE:
                 * se usan las primeras cinco columnas.
                 */
                row
                    .take(5)
                    .padEnd(5, '0')
            }
        }
    }

    private fun isStepRow(
        line: String
    ): Boolean {

        if (line.length < 5) {
            return false
        }

        return line.all { character ->
            character.uppercaseChar() in VALID_STEP_CHARACTERS
        }
    }

    private val VALID_STEP_CHARACTERS = setOf(
        '0',
        '1',
        '2',
        '4',
        'F',
        'P',
        'M',
        'L',
        'H'
    )

    // =====================================================
    // EVENTOS KSF
    // =====================================================

    private fun isEventLine(
        line: String
    ): Boolean {

        return line.startsWith("|") &&
                line.endsWith("|")
    }

    private fun parseEvent(
        line: String,
        state: ParsingState,

        bpms: MutableList<Parser.BpmSegment>,
        tickcounts: MutableList<Parser.TickCountSegment>,

        stops: MutableList<Parser.Stop>,
        delays: MutableList<Parser.Delay>,

        speeds: MutableList<Parser.Speed>,
        scrolls: MutableList<Parser.Scroll>
    ) {

        if (line.length < 3) {
            return
        }

        val content = line
            .substring(
                startIndex = 1,
                endIndex = line.length - 1
            )
            .trim()

        if (content.isEmpty()) {
            return
        }

        val eventType = content
            .first()
            .uppercaseChar()

        val eventValue = content
            .substring(1)
            .trim()

        when (eventType) {

            /*
             * BPM:
             *
             * El valor absoluto se utiliza para el timing SSC.
             * El signo se utiliza exclusivamente para el Scroll.
             */
            'B' -> {

                val rawBpm = eventValue.toDoubleOrNull()
                    ?: return

                state.rawBpm = rawBpm
                state.currentBpm = normalizeBpm(rawBpm)

                addOrReplaceBpm(
                    list = bpms,
                    segment = Parser.BpmSegment(
                        beat = state.currentBeat,
                        bpm = state.currentBpm
                    )
                )

                /*
                 * TODOS los BPM negativos hacen retroceder las notas,
                 * sin importar el valor o el signo del tickcount.
                 */
                addOrReplaceScroll(
                    list = scrolls,
                    segment = Parser.Scroll(
                        beat = state.currentBeat,
                        ratio = getScrollDirection(rawBpm)
                    )
                )
            }

            /*
             * TICKCOUNT:
             *
             * Solo modifica la resolución temporal de las filas.
             * No cambia directamente la dirección visual.
             */
            'T' -> {

                val rawTickcount = eventValue
                    .toDoubleOrNull()
                    ?.toInt()
                    ?: return

                state.rawTickcount = rawTickcount

                state.currentTickcount = normalizeTickcount(
                    rawTickcount
                )

                addOrReplaceTickcount(
                    list = tickcounts,
                    segment = Parser.TickCountSegment(
                        beat = state.currentBeat,
                        tickcount = state.currentTickcount
                    )
                )
            }

            /*
             * Delay absoluto en milisegundos.
             */
            'D' -> {

                val durationMs = eventValue.toDoubleOrNull()
                    ?: return

                if (durationMs == 0.0) {
                    return
                }

                stops.add(
                    Parser.Stop(
                        beat = state.currentBeat,
                        durationMs = abs(durationMs)
                    )
                )
            }

            /*
             * Delay medido en ticks.
             */
            'E' -> {

                val tickUnits = eventValue.toDoubleOrNull()
                    ?: return

                val durationMs = calculateDelayBeatMs(
                    bpm = state.currentBpm,
                    tickcount = state.currentTickcount,
                    tickUnits = tickUnits
                )

                if (durationMs <= 0.0) {
                    return
                }

                delays.add(
                    Parser.Delay(
                        beat = state.currentBeat,
                        durationMs = durationMs
                    )
                )
            }

            /*
             * Cambio de velocidad.
             */
            'S' -> {

                parseSpeedEvent(
                    value = eventValue,
                    beat = state.currentBeat,
                    bpm = state.currentBpm
                )?.let {
                    speeds.add(it)
                }
            }
        }
    }

    // =====================================================
    // SPEED
    // =====================================================

    private fun parseSpeedEvent(
        value: String,
        beat: Double,
        bpm: Double
    ): Parser.Speed? {

        val parts = value
            .split(",")
            .map {
                it.trim()
            }

        val ratio = parts
            .getOrNull(0)
            ?.toDoubleOrNull()
            ?: return null

        val durationMs = parts
            .getOrNull(1)
            ?.toDoubleOrNull()
            ?: 0.0

        val durationBeats =
            if (
                durationMs == 0.0 ||
                bpm <= 0.0
            ) {

                0.0

            } else {

                durationMs / millisecondsPerBeat(bpm)
            }

        return Parser.Speed(
            beat = beat,
            ratio = ratio,
            duration = durationBeats,
            mode = 0
        )
    }

    // =====================================================
    // DELAYS
    // =====================================================

    private fun calculateDelayBeatMs(
        bpm: Double,
        tickcount: Int,
        tickUnits: Double
    ): Double {

        if (
            bpm <= 0.0 ||
            tickcount <= 0
        ) {
            return 0.0
        }

        return millisecondsPerBeat(bpm) /
                tickcount.toDouble() *
                abs(tickUnits)
    }

    private fun millisecondsPerBeat(
        bpm: Double
    ): Double {

        val normalizedBpm = abs(bpm)

        if (normalizedBpm <= EPSILON_BEAT) {
            return 0.0
        }

        return 60000.0 / normalizedBpm
    }

    // =====================================================
    // AGREGAR O REEMPLAZAR EVENTOS
    // =====================================================

    private fun addOrReplaceBpm(
        list: MutableList<Parser.BpmSegment>,
        segment: Parser.BpmSegment
    ) {

        val index = list.indexOfLast {
            approximatelyEqual(
                first = it.beat,
                second = segment.beat
            )
        }

        if (index >= 0) {
            list[index] = segment
        } else {
            list.add(segment)
        }
    }

    private fun addOrReplaceTickcount(
        list: MutableList<Parser.TickCountSegment>,
        segment: Parser.TickCountSegment
    ) {

        val index = list.indexOfLast {
            approximatelyEqual(
                first = it.beat,
                second = segment.beat
            )
        }

        if (index >= 0) {
            list[index] = segment
        } else {
            list.add(segment)
        }
    }

    private fun addOrReplaceScroll(
        list: MutableList<Parser.Scroll>,
        segment: Parser.Scroll
    ) {

        val index = list.indexOfLast {
            approximatelyEqual(
                first = it.beat,
                second = segment.beat
            )
        }

        if (index >= 0) {
            list[index] = segment
        } else {
            list.add(segment)
        }
    }

    // =====================================================
    // NORMALIZACIÓN
    // =====================================================

    private fun normalizeBpms(
        source: List<Parser.BpmSegment>
    ): List<Parser.BpmSegment> {

        val sorted = source.sortedBy {
            it.beat
        }

        val result = mutableListOf<Parser.BpmSegment>()

        sorted.forEach { segment ->

            val last = result.lastOrNull()

            when {

                last == null -> {
                    result.add(segment)
                }

                approximatelyEqual(
                    first = last.beat,
                    second = segment.beat
                ) -> {
                    result[result.lastIndex] = segment
                }

                !approximatelyEqual(
                    first = last.bpm,
                    second = segment.bpm
                ) -> {
                    result.add(segment)
                }
            }
        }

        return result
    }

    private fun normalizeTickcounts(
        source: List<Parser.TickCountSegment>
    ): List<Parser.TickCountSegment> {

        val sorted = source.sortedBy {
            it.beat
        }

        val result = mutableListOf<Parser.TickCountSegment>()

        sorted.forEach { segment ->

            val last = result.lastOrNull()

            when {

                last == null -> {
                    result.add(segment)
                }

                approximatelyEqual(
                    first = last.beat,
                    second = segment.beat
                ) -> {
                    result[result.lastIndex] = segment
                }

                last.tickcount != segment.tickcount -> {
                    result.add(segment)
                }
            }
        }

        return result
    }

    private fun normalizeScrolls(
        source: List<Parser.Scroll>
    ): List<Parser.Scroll> {

        val sorted = source.sortedBy {
            it.beat
        }

        val result = mutableListOf<Parser.Scroll>()

        sorted.forEach { segment ->

            val last = result.lastOrNull()

            when {

                last == null -> {
                    result.add(segment)
                }

                approximatelyEqual(
                    first = last.beat,
                    second = segment.beat
                ) -> {
                    result[result.lastIndex] = segment
                }

                !approximatelyEqual(
                    first = last.ratio,
                    second = segment.ratio
                ) -> {
                    result.add(segment)
                }
            }
        }

        return result
    }

    private fun normalizeWarps(
        source: List<Parser.Warp>
    ): List<Parser.Warp> {
        val sorted = source.filter {
                it.duration > EPSILON_BEAT
            }.sortedBy { it.beat }

        if (sorted.isEmpty()) {
            return emptyList()
        }

        val result = mutableListOf<Parser.Warp>()

        for (warp in sorted) {

            val last = result.lastOrNull()

            if (last == null) {

                result.add(warp)
                continue
            }
            val lastEnd = last.beat + last.duration
            val warpEnd = warp.beat + warp.duration
            /*
             * Une warps traslapados o consecutivos.
             */
            if (warp.beat <= lastEnd + EPSILON_BEAT) {
                result[result.lastIndex] =
                    Parser.Warp(beat = last.beat, duration = maxOf(lastEnd, warpEnd) - last.beat)

            } else {

                result.add(warp)
            }
        }

        return result
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private fun normalizeBpm(
        bpm: Double
    ): Double {

        val normalized = abs(bpm)

        return if (normalized <= EPSILON_BEAT) {
            0.000001
        } else {
            normalized
        }
    }

    private fun normalizeTickcount(
        tickcount: Int
    ): Int {

        return abs(tickcount)
            .coerceAtLeast(1)
    }

    /**
     * Todo BPM negativo produce movimiento visual inverso.
     *
     * El tickcount no interviene en esta decisión.
     */
    private fun getScrollDirection(
        rawBpm: Double
    ): Double {

        return if (rawBpm < 0.0) {
            -1.0
        } else {
            1.0
        }
    }

    private fun approximatelyEqual(
        first: Double,
        second: Double,
        epsilon: Double = EPSILON_BEAT
    ): Boolean {

        return abs(first - second) <= epsilon
    }
}