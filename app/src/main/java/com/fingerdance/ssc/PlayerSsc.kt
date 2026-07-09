package com.fingerdance.ssc

import LuaEngine
import android.os.SystemClock
import android.util.Log
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils.sin
import com.fingerdance.GameScreenActivity
import com.fingerdance.aBatch
import com.fingerdance.bBatch
import com.fingerdance.breakSong
import com.fingerdance.chart
import com.fingerdance.decimoHeigtn
import com.fingerdance.displayBPM
import com.fingerdance.heightBtns
import com.fingerdance.heightJudges
import com.fingerdance.isMidLine
import com.fingerdance.isOnline
import com.fingerdance.luaFlare
import com.fingerdance.luaNotes
import com.fingerdance.luaRecepts
import com.fingerdance.medidaFlechas
import com.fingerdance.padPositions
import com.fingerdance.playerSong
import com.fingerdance.resultSong
import com.fingerdance.ruta
import com.fingerdance.showPadB
import com.fingerdance.soundPoolSelectSong
import com.fingerdance.sound_mine
import com.fingerdance.valueOffset
import com.fingerdance.widthBtns
import com.fingerdance.widthJudges
import java.io.File
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class PlayerSsc(
    val screen: GameScreenSsc,
    private val batch: SpriteBatch,
    private val activity: GameScreenActivity
) {

    private val bpms = chart.bpms
    private val tickcounts = chart.tickcounts
    private val stops = chart.stops
    private val delays = chart.delays
    private val warps = chart.warps
    private val notes = chart.notes
    private val speeds = chart.speeds
    private val scrolls = chart.scrolls
    private val combos = chart.combos

    private val sizeScale = medidaFlechas * 1.2f
    private val topPos = medidaFlechas * 0.9f
    private val posX = medidaFlechas * 0.1f

    private val xFlare1 = medidaFlechas * 2.1f
    private val xFlare2 = medidaFlechas * 2.15f
    private val xFlare3 = medidaFlechas * 2.1f
    private val xFlare4 = medidaFlechas * 2.05f
    private val xFlare5 = medidaFlechas * 2.05f
    private val animationDuration: Long = 300L
    private var multiplierCombo = 1
    private var comboSegmentIndex = 0

    companion object {
        val STEPSIZE = medidaFlechas.toInt()

        const val MINUTE = 60000f

        private var ZONE_PERFECT: Long = if (playerSong.hj) 25 else 50
        private var ZONE_GREAT: Long = if (playerSong.hj) 41 else 82
        private var ZONE_GOOD: Long = if (playerSong.hj) 115 else 115
        private var ZONE_BAD: Long = if (playerSong.hj) 140 else 150

        const val JUDGE_PERFECT = 0
        const val JUDGE_GREAT = 1
        const val JUDGE_GOOD = 2
        const val JUDGE_BAD = 3
        const val JUDGE_MISS = 4

        const val KEY_NONE = 0
        const val KEY_DOWN = 1
        const val KEY_PRESS = 2
        const val KEY_UP = 3

        const val MINE_PENALTY = 0.025f
        const val WINDOW_BEAT_ALLOW = 8.0

        private lateinit var mine: Texture
        private lateinit var downLeftTap: Texture
        private lateinit var upLeftTap: Texture
        private lateinit var centerTap: Texture
        private lateinit var upRightTap: Texture
        private lateinit var downRightTap: Texture

        private lateinit var downLeftBody: Texture
        private lateinit var upLeftBody: Texture
        private lateinit var centerBody: Texture
        private lateinit var upRightBody: Texture
        private lateinit var downRightBody: Texture

        private lateinit var downLeftBottom: Texture
        private lateinit var upLeftBottom: Texture
        private lateinit var centerBottom: Texture
        private lateinit var upRightBottom: Texture
        private lateinit var downRightBottom: Texture

        private lateinit var arrMines: Array<TextureRegion>

        private lateinit var arrArrows: Array<Array<TextureRegion>>
        private lateinit var arrArrowsBody: Array<Array<TextureRegion>>
        private lateinit var arrArrowsBottom: Array<Array<TextureRegion>>

        private lateinit var sprFlare: Texture

        private lateinit var flareArrowFrame: Array<TextureRegion>

        private val LONGNOTE = Array(5) { LongNotePress() }

        private lateinit var whiteTex: Texture
    }

    data class PlayerFlare(var startTime: Long = 0)
    data class PlayerJudge(var startTime: Long = 0, var judge: Int = 0)
    data class JudgePos(
        val x: Int = widthJudges - (widthJudges / 2),
        val y: Int = Gdx.graphics.height / 2 - heightJudges * 6
    )

    private val baseSpeed = playerSong.speed.replace("X", "").toFloat() + 1f

    private val timingData = TimmingData(
        bpms = bpms,
        stops = stops,
        delays = delays,
        warps = warps,
        speeds = speeds,
        scrolls = scrolls,
        offsetMs = chart.offset * 1000.0,
        userOffsetMs = valueOffset * 10.0
    )

    private val hitNotes = mutableSetOf<Parser.Note>()
    private val finishedHolds = mutableSetOf<Parser.Note>()

    private var m_fGauge = 0.35f
    var m_fCurBPM = displayBPM
    private var arrowFrame = 0
    private var m_iStepWidth = 5
    var curCombo = 0
    var curComboMiss = 0
    private val flare = Array(5) { PlayerFlare() }
    private var m_judge = PlayerJudge()
    private var noEffects = false

    private var mineFlashStartTime: Long = 0L
    private val MINE_FLASH_DURATION: Long = 100L

    private val widthFlare = medidaFlechas * 5f
    private var yFlare = medidaFlechas - (medidaFlechas * 2f)

    private val inputProcessor = InputProcessorSsc()

    private var currentTimeToExpands = 0L

    private val judgePos = JudgePos()
    private val x = judgePos.x.toFloat()
    private val y = judgePos.y.toFloat()
    private var digitWidth = medidaFlechas * 0.7f
    private var digitHeight = heightJudges * 1.3f

    private val gaugeInc = if (playerSong.hj) screen.gaugeIncHJ else screen.gaugeIncNormal
    val tipWidth = (medidaFlechas / 4f)
    val tipHeight = medidaFlechas / 1.5f
    val tipY = 0f - (medidaFlechas * 0.05f)

    private val columnNotes = Array(5) { mutableListOf<Parser.Note>() }
    private val columnIndex = IntArray(5) { 0 }
    private var lastStepSongTimeMs: Long = 0L

    private var isVanish = playerSong.vanish
    private var isAp = playerSong.ap

    private var luaFlashStartTime = 0L
    private var luaFlashDuration = 0L
    lateinit var luaEngine: LuaEngine

    init {
        currentTimeToExpands = timeGetTime()
        initCommonInfo()

        Gdx.input.inputProcessor = inputProcessor

        for (x in 0 until 5) {
            LONGNOTE[x].pressed = false
        }
        if (isAp || isVanish) {
            noEffects = true
        }
    }

    private fun initCommonInfo() {
        mine = Texture(Gdx.files.absolute("${File(ruta).parent}/Tap Mine 3x2.png"))
        downLeftTap = Texture(Gdx.files.absolute("$ruta/DownLeft Tap Note 3x2.png"))
        upLeftTap = Texture(Gdx.files.absolute("$ruta/UpLeft Tap Note 3x2.png"))
        centerTap = Texture(Gdx.files.absolute("$ruta/Center Tap Note 3x2.png"))
        upRightTap = upLeftTap
        downRightTap = downLeftTap

        downLeftBody = Texture(Gdx.files.absolute("$ruta/DownLeft Hold Body Active 6x1.png"))
        upLeftBody = Texture(Gdx.files.absolute("$ruta/UpLeft Hold Body Active 6x1.png"))
        centerBody = Texture(Gdx.files.absolute("$ruta/Center Hold Body Active 6x1.png"))
        upRightBody = upLeftBody
        downRightBody = downLeftBody

        downLeftBottom = Texture(Gdx.files.absolute("$ruta/DownLeft Hold BottomCap Active 6x1.png"))
        upLeftBottom = Texture(Gdx.files.absolute("$ruta/UpLeft Hold BottomCap Active 6x1.png"))
        centerBottom = Texture(Gdx.files.absolute("$ruta/Center Hold BottomCap Active 6x1.png"))
        upRightBottom = upLeftBody
        downRightBottom = downLeftBottom

        val ldArrowFrame = getArrows3x2(downLeftTap)
        val luArrowFrame = getArrows3x2(upLeftTap)
        val ceArrowFrame = getArrows3x2(centerTap)
        val ruArrowFrame = getArrows3x2(upLeftTap, true)
        val rdArrowFrame = getArrows3x2(downLeftTap, true)

        arrArrows = arrayOf(ldArrowFrame, luArrowFrame, ceArrowFrame, ruArrowFrame, rdArrowFrame)

        val ldBodyArrowFrame = getArrows6x1(downLeftBody)
        val luBodyArrowFrame = getArrows6x1(upLeftBody)
        val ceBodyArrowFrame = getArrows6x1(centerBody)
        val ruBodyArrowFrame = getArrows6x1(upLeftBody, true)
        val rdBodyArrowFrame = getArrows6x1(downLeftBody, true)

        arrArrowsBody = arrayOf(ldBodyArrowFrame, luBodyArrowFrame, ceBodyArrowFrame, ruBodyArrowFrame, rdBodyArrowFrame)

        val ldBottomArrowFrame = getArrows6x1(downLeftBottom)
        val luBottomArrowFrame = getArrows6x1(upLeftBottom)
        val ceBottomArrowFrame = getArrows6x1(centerBottom)
        val ruBottomArrowFrame = getArrows6x1(upLeftBottom, true)
        val rdBottomArrowFrame = getArrows6x1(downLeftBottom, true)

        arrArrowsBottom = arrayOf(ldBottomArrowFrame, luBottomArrowFrame, ceBottomArrowFrame, ruBottomArrowFrame, rdBottomArrowFrame)

        sprFlare = Texture(Gdx.files.absolute("$ruta/Flare 6x1.png"))
        flareArrowFrame = getArrows6x1(sprFlare)

        arrMines = getArrows3x2(mine)
        createWhiteTexture()
        initColumnNotes()
        inputProcessor.resetState()
        luaEngine = LuaEngine(playerSsc = this, widthNotes = medidaFlechas * 5f)
    }

    private data class LongNotePress(
        var pressed: Boolean = false,
        var lastTickBeat: Double = 0.0,
        var nextTickBeat: Double = 0.0,
        var note: Parser.Note? = null,
        var timeStarted: Long = 0L,
    )

    private fun initColumnNotes() {
        for (col in 0 until columnNotes.size) columnNotes[col].clear()
        for (n in notes) {
            if (n.column in 0 until columnNotes.size) {
                columnNotes[n.column].add(n)
            }
        }
        for (c in 0 until 5) {
            columnNotes[c].sortBy { it.beat }
            columnIndex[c] = 0
        }
    }
    var beatToShow = 0.0
    private var beatWindow = WINDOW_BEAT_ALLOW
    private val iLongTop = LongArray(5)
    fun render(songTimeMs: Long) {
        val timeCom = timeGetTime()
        val nowMs = songTimeMs.toDouble()
        val currentBeat = timeToBeat(nowMs)
        beatToShow = currentBeat
        if (showPadB == 0) {
            inputProcessor.render(batch)
        }

        updateComboMultiplier(currentBeat)

        val currentBpm = bpms.lastOrNull { it.beat <= currentBeat }?.bpm ?: bpms.firstOrNull()?.bpm ?: 120.0
        m_fCurBPM = currentBpm.toFloat()
        val msPorBeat = MINUTE / m_fCurBPM.coerceIn(1f, 999f)
        val msPorFrame = msPorBeat / 5f
        arrowFrame = ((timeCom % msPorBeat.toLong()) / msPorFrame.toLong()).toInt()
        updateFGChanges(currentBeat)

        // Encuentra la próxima nota hacia adelante y atrás
        val prevNoteBeat = notes.lastOrNull { it.beat <= currentBeat }?.beat
        val nextNoteBeat = notes.firstOrNull { it.beat >= currentBeat }?.beat

        // Checa todas las holds activas que empiezan antes y terminan después
        var maxHoldDuration = 0.0
        for (n in notes) {
            if (n.type == Parser.NoteType.HOLD && currentBeat >= n.beat && n.endBeat != null) {
                maxHoldDuration = maxOf(maxHoldDuration, n.endBeat - n.beat)
            }
        }

        // Dinámica por: HOLD larga, nota previa o siguiente lejana
        val distToPrev = if (prevNoteBeat != null) currentBeat - prevNoteBeat else 8.0
        val distToNext = if (nextNoteBeat != null) nextNoteBeat - currentBeat else 8.0

        val neededWindow = maxOf(
            WINDOW_BEAT_ALLOW,
            maxHoldDuration,
            distToPrev + 1,  // +1 (o +0.5) para margen
            distToNext + 1
        )

        beatWindow = neededWindow + 8.0

        val minBeat = currentBeat - beatWindow
        val maxBeat = currentBeat + beatWindow

        val firstIdx = notes.binarySearchIndexFrom(
            selector = { it.beat },
            value = minBeat
        )
        var dynamicWindow = WINDOW_BEAT_ALLOW
        for (i in firstIdx until notes.size) {
            val n = notes[i]
            if (n.beat > maxBeat) break
            if(n.isPhantom) continue
            //if (timingData.isBeatInWarp(n.beat)) continue

            when (n.type) {
                Parser.NoteType.TAP -> {
                    if (hitNotes.contains(n)) continue
                    val y = yForBeat(n.beat, currentBeat, nowMs)
                    if (y > -STEPSIZE && y < screen.gdxHeight + STEPSIZE) {
                        if(n.isMine){
                            drawMines(n.column, y, n)
                        }else {
                            drawNote(n.column, y, n)
                        }
                    }
                }

                Parser.NoteType.HOLD -> {
                    val endBeat = n.endBeat ?: continue
                    if (currentBeat >= n.beat && currentBeat <= endBeat + 4.0) {
                        val duration = endBeat - n.beat
                        dynamicWindow = max(dynamicWindow, duration)
                    }

                    if (endBeat < minBeat) continue

                    var yHead = yForBeat(n.beat, currentBeat, nowMs)
                    val yTail = yForBeat(endBeat, currentBeat, nowMs)

                    if (yHead < screen.gdxHeight + STEPSIZE && yTail > -STEPSIZE) {
                        val col = n.column
                        val locked = LONGNOTE[col].pressed && currentBeat in n.beat..endBeat

                        if (locked) yHead = medidaFlechas.toInt()
                        if (finishedHolds.contains(n)) continue

                        if(n.isFake && n.isPressed){
                            drawLongNote(col, medidaFlechas.toInt(), yTail, n)
                        }else{
                            drawLongNote(col, yHead, yTail, n)
                        }
                    }
                }
            }
        }
        beatWindow = dynamicWindow

        if (m_fGauge > 1.0f) m_fGauge = 1.0f
        if (m_fGauge < -0.5f) m_fGauge = 0.0f

        var gaugeFind = (MINUTE / m_fCurBPM.coerceIn(1f, 999f))
        val phase = (nowMs % gaugeFind).toFloat()
        gaugeFind = (phase / gaugeFind) * 0.1f

        if (m_fGauge >= 1.0f) m_fGauge = 1.0f
        if (m_fGauge < -0.5f) m_fGauge = 0.0f

        val gaugeVisual = if (m_fGauge == 1.0f) {
            var v = gaugeFind * 3.0f + 0.7f
            2.0f + v
        } else {
            var v = gaugeFind + m_fGauge
            if (v > 1.0f) v = 1.0f
            v
        }

        drawGauge(gaugeVisual)

        for (iStepNo in 0 until m_iStepWidth) {
            if (flare[iStepNo].startTime == 0L) continue

            iLongTop[iStepNo] = ((timeCom - flare[iStepNo].startTime) shr 6)
            if (iLongTop[iStepNo] >= 6) {
                flare[iStepNo].startTime = 0
                continue
            }

            drawFlare(iStepNo, iLongTop[iStepNo].toInt())
        }

        drawMineFlash(timeCom)
        drawLuaFlash(timeCom)

        if (m_judge.startTime == 0L) return
        if (m_judge.startTime + 2500 < timeCom) {
            m_judge.startTime = 0
            return
        }
        drawJudge(timeCom - m_judge.startTime)
    }

    inline fun <T> List<T>.binarySearchIndexFrom(
        from: Int = 0, to: Int = size,
        crossinline selector: (T) -> Double, value: Double
    ): Int {
        var low = from
        var high = to - 1
        while (low <= high) {
            val mid = (low + high).ushr(1)
            val cmp = selector(this[mid]).compareTo(value)
            when {
                cmp < 0 -> low = mid + 1
                cmp > 0 -> high = mid - 1
                else -> return mid
            }
        }
        return low
    }

    private val key = IntArray(m_iStepWidth)
    fun updateStepData(songTimeMs: Long) {
        val keyBoard = inputProcessor.getKeyBoard
        //val key = IntArray(m_iStepWidth)
        for (x in 0 until m_iStepWidth) {
            key[x] = keyBoard[x]
        }
        val prevSongTimeMs = lastStepSongTimeMs
        lastStepSongTimeMs = songTimeMs

        val prevBeat = timeToBeat(prevSongTimeMs.toDouble())
        val nowBeat  = timeToBeat(songTimeMs.toDouble())
        for (col in 0 until m_iStepWidth) {

            when (key[col]) {
                KEY_DOWN -> {
                    showExpand(col)
                    processTapAndHeadOnColumn(col, songTimeMs)
                    tryAutoStartHoldOnPress(col, songTimeMs)
                }
                KEY_PRESS -> {
                    showExpand(col)
                    tryAutoStartHoldOnPress(col, songTimeMs)
                    processLongNoteTick(col, songTimeMs)
                }
                KEY_UP -> {
                    if (LONGNOTE[col].pressed) {
                        endLongNote(col, songTimeMs)
                    }
                }
            }
        }
        for (col in 0 until m_iStepWidth) {
            val notesInCol = columnNotes[col]

            // Si NO hay hold activa y hay pad presionado actualmente
            if (!LONGNOTE[col].pressed && (key[col] == KEY_PRESS || key[col] == KEY_DOWN)) {
                for (i in columnIndex[col] until notesInCol.size) {
                    val n = notesInCol[i]
                    if(n.isFake) continue
                    if (n.type != Parser.NoteType.HOLD) continue
                    if (finishedHolds.contains(n)) continue
                    val headBeat = n.beat
                    //val endBeat = n.endBeat ?: n.beat

                    val isPressing = (key[col] == KEY_PRESS || key[col] == KEY_DOWN)
                    val lateCatch = isPressing && nowBeat > n.beat //&& nowBeat > endBeat

                    // ¿El head fue "cruzado" entre frames?
                    if (
                        (prevBeat <= headBeat && headBeat < nowBeat) ||
                        (nowBeat <= headBeat && headBeat < prevBeat) ||
                        (abs(nowBeat - headBeat) < 0.001)  ||
                        lateCatch
                    ) {
                        // Engancha el hold automáticamente
                        applyJudge(col, JUDGE_PERFECT, isFromInput = true, note = n)
                        startLongNote(col, n, songTimeMs)
                        LONGNOTE[col].lastTickBeat = nowBeat
                        columnIndex[col] = i + 1
                        break // solo uno por frame
                    }
                    // Si el head está adelante del receptor, ya no busques más
                    if (headBeat > nowBeat && headBeat > prevBeat) break
                }
            }
        }
        updateAutoMisses(songTimeMs, prevSongTimeMs)
        inputProcessor.update()

        if (m_fGauge > 1.0f) {
            m_fGauge = 1.0f
        } else if (m_fGauge < -0.5f) {
            if (!isOnline && breakSong) {
                activity.breakDance()
            }
        }
    }

    private fun updateAutoMisses(songTimeMs: Long, prevSongTimeMs: Long) {
        for (col in 0 until m_iStepWidth) {
            val notesInCol = columnNotes[col]
            var idx = columnIndex[col]
            // No missear si hay un hold activo
            if (LONGNOTE[col].pressed) continue

            while (idx < notesInCol.size) {
                val n = notesInCol[idx]
                if (n.isFake) {
                    idx++
                    columnIndex[col] = idx
                    continue
                }
                if (hitNotes.contains(n)) {
                    idx++
                    columnIndex[col] = idx
                    continue
                }

                if (timingData.isBeatInWarp(n.beat)) {
                    idx++
                    columnIndex[col] = idx
                    continue
                }
                // Si es hold y estamos activos la ignoramos (ya lo hace el check anterior)
                val deltaMs = getDeltaMsForNote(n.beat, songTimeMs)
                // Si se pasó de la zona BAD por mucho, marcamos miss y saltamos a la siguiente
                if (deltaMs > ZONE_BAD) {
                    if (n.isMine){
                        idx++
                        columnIndex[col] = idx
                        continue
                    }
                    applyJudge(col, JUDGE_MISS, isFromInput = false, note = n)
                    columnIndex[col] = idx + 1
                    idx++
                    continue
                }
                break // No hay más que auto-missear
            }
        }
    }

    private fun processTapAndHeadOnColumn(col: Int, timeMs: Long) {
        val notesInCol = columnNotes[col]
        val nowBeat = timeToBeat(timeMs.toDouble())

        val idxStart = columnIndex[col]
        var bestIdx = -1
        var bestJudge = -1
        var minAbsDelta = Long.MAX_VALUE

        for (i in idxStart until min(idxStart + 8, notesInCol.size)) {

            val note = notesInCol[i]

            // IGNORAR FAKES COMPLETAMENTE
            if (note.isFake) {
                continue
            }

            // IGNORAR WARPS
            if (timingData.isBeatInWarp(note.beat)) {
                continue
            }

            val deltaMs = getDeltaMsForNote(note.beat, timeMs)

            // La nota todavía está muy adelante
            if (deltaMs < -ZONE_BAD) {
                break
            }

            // =========================
            // MINES
            // =========================
            if (note.isMine) {
                if (abs(deltaMs) <= ZONE_BAD) {
                    applyJudge(col, JUDGE_MISS, isFromInput = true, isMine = true, note = note)
                    hitNotes.add(note)
                    columnIndex[col] = i + 1
                    onMineHit(timeGetTime())
                }
                continue
            }

            // =========================
            // SOLO TAP / HOLD
            // =========================
            if (note.type != Parser.NoteType.TAP &&
                note.type != Parser.NoteType.HOLD) {
                continue
            }

            // Ya hay hold activa en esta columna
            if (note.type == Parser.NoteType.HOLD &&
                LONGNOTE[col].pressed) {
                continue
            }

            val judge = getJudgeFromDelta(deltaMs)

            if (judge >= 0 && abs(deltaMs) < minAbsDelta) {
                bestIdx = i
                bestJudge = judge
                minAbsDelta = abs(deltaMs)
            }
        }

        // No encontró nada válido
        if (bestIdx == -1) {
            return
        }

        val n = notesInCol[bestIdx]

        // =========================
        // HOLD HEAD
        // =========================
        if (n.type == Parser.NoteType.HOLD) {
            applyJudge(col, JUDGE_PERFECT, isFromInput = true, note = n)
            startLongNote(col, n, timeMs)
            LONGNOTE[col].lastTickBeat = nowBeat
            columnIndex[col] = bestIdx + 1

        } else {
            applyJudge(col, bestJudge, isFromInput = true, note = n)
            hitNotes.add(n)
            columnIndex[col] = bestIdx + 1
        }
    }

    private fun startLongNote(col: Int, note: Parser.Note, timeMs: Long) {
        val ln = LONGNOTE[col]
        val nowBeat = timeToBeat(timeMs.toDouble())

        ln.pressed = true
        ln.note = note
        ln.timeStarted = timeMs

        val fromBeat = max(note.beat, nowBeat)
        ln.lastTickBeat = fromBeat
        ln.nextTickBeat = getNextHoldTickBeat(fromBeat)
    }

    private fun getNextHoldTickBeat(fromBeat: Double): Double {
        val ticksPerBeat = findCurrentTick(fromBeat).coerceAtLeast(1.0)
        val separation = 1.0 / ticksPerBeat

        return kotlin.math.floor(fromBeat / separation) * separation + separation
    }

    private fun endLongNote(col: Int, timeMs: Long) {
        val ln = LONGNOTE[col]
        val note = ln.note ?: return
        val endBeat = note.endBeat ?: return
        val nowBeat = timeToBeat(timeMs.toDouble())
        val tailTolBeats = ((endBeat - note.beat) * 0.1).coerceAtLeast(0.12) // 10% o mínimo
        val remaining = endBeat - nowBeat

        if (remaining <= tailTolBeats) {
            // Bien
            applyJudge(col, JUDGE_PERFECT, isFromInput = true, isBodyLongNote = true, note = note)
            finishedHolds.add(note)
            ln.pressed = false
            ln.note = null
        } else {
            // Muy temprano (MISS)
            applyJudge(col, JUDGE_MISS, isFromInput = true, note = note)
            ln.pressed = false
            ln.note = null
        }
    }

    private fun tryAutoStartHoldOnPress(col: Int, timeMs: Long) {
        if (LONGNOTE[col].pressed) return // ya hay una nota larga activa

        val nowBeat = timeToBeat(timeMs.toDouble())
        val notesInCol = columnNotes[col]

        // buscar cualquier HOLD 'viva' bajo el receptor
        for (i in notesInCol.indices) {
            val n = notesInCol[i]
            if(n.isFake) continue
            if (n.type != Parser.NoteType.HOLD) continue
            if (finishedHolds.contains(n)) continue

            val endBeat = n.endBeat ?: continue

            if (nowBeat in n.beat..endBeat) {
                // Recaptura (desde el head o cuerpo)
                applyJudge(col, JUDGE_PERFECT, isFromInput = true, note = n)
                startLongNote(col, n, timeMs)
                LONGNOTE[col].lastTickBeat = nowBeat
                // (Sólo avanza el índice si el head nunca fue juzgado)
                if (columnIndex[col] <= i) {
                    columnIndex[col] = i + 1
                }
                return
            }
            // Si la siguiente nota ya está lejos, salimos
            if (n.beat - nowBeat > 2.0) break
        }
    }

    private fun processLongNoteTick(col: Int, timeMs: Long) {
        if (!LONGNOTE[col].pressed) return

        val ln = LONGNOTE[col]
        val note = ln.note ?: return
        val nowBeat = timeToBeat(timeMs.toDouble())
        val endBeat = note.endBeat ?: return

        if (nowBeat > endBeat) {
            finishedHolds.add(note)
            ln.pressed = false
            ln.note = null
            return
        }

        while (nowBeat >= ln.nextTickBeat && ln.nextTickBeat <= endBeat) {
            applyJudge(
                col,
                JUDGE_PERFECT,
                isBodyLongNote = true,
                isFromInput = true,
                note = note
            )

            ln.lastTickBeat = ln.nextTickBeat

            val ticksPerBeat = findCurrentTick(ln.nextTickBeat).coerceAtLeast(1.0)
            val separation = 1.0 / ticksPerBeat

            ln.nextTickBeat += separation
        }
    }

    private fun findCurrentTick(nowBeat: Double): Double{
        return if (tickcounts.isEmpty()) {
            4.0 // default a 1/4
        } else {
            tickcounts.lastOrNull { it.beat <= nowBeat }?.tickcount?.toDouble() ?: 4.0
        }
    }

    private fun updateComboMultiplier(nowBeat: Double) {
        while (
            comboSegmentIndex < combos.size &&
            nowBeat >= combos[comboSegmentIndex].beat
        ) {
            multiplierCombo = combos[comboSegmentIndex].number
            comboSegmentIndex++
        }
    }

    private fun getDeltaMsForNote(noteBeat: Double, timeMs: Long): Long {
        val noteTimeMs = beatToTime(noteBeat)
        return (timeMs - noteTimeMs).toLong()
    }

    private fun getJudgeFromDelta(judgeTime: Long): Int {
        val absDelta = abs(judgeTime)
        return if(absDelta <= ZONE_PERFECT){
            JUDGE_PERFECT
        }else if(absDelta <= ZONE_GREAT){
            JUDGE_GREAT
        }else if(absDelta <= ZONE_GOOD){
            JUDGE_GOOD
        }else {
            JUDGE_BAD
        }
    }

    private fun updateFGChanges(currentBeat: Double) {
        for (event in chart.fgChanges) {
            if (event.executed) continue
            if (currentBeat >= event.beat) {
                event.executed = true
                val stepsFolder = File(chart.chartPath).parentFile
                val target = File(stepsFolder, event.script)
                val luaFile = if (target.isDirectory) {
                    target.listFiles()?.firstOrNull { it.isFile && it.extension.equals("lua", true) }
                } else {
                    target
                }
                if (luaFile != null && luaFile.exists()) {
                    luaEngine.executeLua(luaFile.absolutePath)
                } else {
                    Log.d("LUA_DEBUG", "Lua no encontrado: $target")
                }
            }
        }
    }

    private fun applyJudge(col: Int, judge: Int, isBodyLongNote: Boolean = false, isFromInput: Boolean, isMine: Boolean = false, note: Parser.Note? = null) {
        if (note != null && note.isFake) return

        when (judge) {
            JUDGE_PERFECT -> {
                resultSong.perfect++
                curCombo += multiplierCombo
                curComboMiss = 0
            }

            JUDGE_GREAT -> {
                resultSong.great++
                curCombo += multiplierCombo
                curComboMiss = 0
            }

            JUDGE_GOOD -> {
                resultSong.good++
                curComboMiss = 0
            }

            JUDGE_BAD -> {
                resultSong.bad++
                curCombo = 0
                curComboMiss = 0
            }

            JUDGE_MISS -> {
                resultSong.miss++
                curCombo = 0
                curComboMiss += multiplierCombo
            }
        }

        if (resultSong.maxCombo < curCombo) {
            resultSong.maxCombo = curCombo
        }

        when (judge) {
            JUDGE_PERFECT -> {
                m_fGauge += if(isBodyLongNote){
                    (gaugeInc[judge] * 0.002f) // los ticks de hold dan muy poco
                }else {
                    gaugeInc[judge]
                }
            }

            JUDGE_GREAT, JUDGE_GOOD -> {
                m_fGauge += gaugeInc[judge]
            }
            JUDGE_BAD -> {
                m_fGauge += gaugeInc[judge]
                if (m_fGauge < 0f) m_fGauge = 0f
            }
            JUDGE_MISS -> {
                m_fGauge += if(isMine){
                    (gaugeInc[judge] + MINE_PENALTY)
                }else{
                    gaugeInc[judge]
                }
                if (m_fGauge < 0f) m_fGauge = 0f
            }
        }

        val now = timeGetTime()
        m_judge.judge = judge
        m_judge.startTime = now

        // Flare: siempre que no sea MISS. (incluye ticks)
        if (isFromInput && judge != JUDGE_MISS) {
            flare[col].startTime = now
            //showExpand(col)
        }
    }


    private fun yForBeat(beat: Double, currentBeat: Double, songTimeMs: Double): Int {
        val baseOffset = timingData.getYOffsetForBeat(
            noteBeat = beat,
            songVisibleBeat = currentBeat,
            songVisibleTimeMs = songTimeMs,
            stepSize = medidaFlechas
        )

        val totalOffset = baseOffset * baseSpeed
        val y = medidaFlechas + totalOffset
        return y.toInt()
    }

    private fun timeToBeat(timeMs: Double): Double = timingData.timeToBeat(timeMs)
    private fun beatToTime(beat: Double): Double = timingData.beatToTime(beat)

    private val MEASURE = (decimoHeigtn * 2.5).toDouble()
    private val MEASUREVANISH = if(isMidLine) (decimoHeigtn * 2.375).toDouble() else (decimoHeigtn * 3.5).toDouble()
    private val initArrow = (screen.gdxHeight * 0.575)
    private var rangeAlpha = (screen.gdxHeight * 0.1)
    private val segmentHeight = screen.gdxHeight * 0.005f
    private var heightBodyHead = (medidaFlechas * 0.3f)
    private var middleSizeFlechas = medidaFlechas * 0.5f
    private val amplitude = medidaFlechas / 3f
    private val frequency = 0.01f
    private val fadeDistance = medidaFlechas
    private var offsetX = 0f

    private fun drawLongNote(x: Int, y: Int, y2: Int, note: Parser.Note) {
        when {
            isAp -> drawLongNoteAp(x, y, y2)
            isVanish || note.isVanish -> drawLongNoteVanish(x, y, y2)
            else -> drawLongNoteNormal(x, y, y2)
        }

    }

    private fun drawMines(x: Int, y: Int, note: Parser.Note) {
        when {
            isAp -> drawNoteMineAp(x, y)
            isVanish || note.isVanish -> drawNoteMineVanish(x, y)
            else -> drawNoteMine(x, y)
        }
    }

    private fun drawNote(x: Int, y: Int, note: Parser.Note) {
        when {
            isAp -> drawNoteAp(x, y)
            isVanish || note.isVanish -> drawNoteVanish(x, y)
            else -> drawNoteNormal(x, y)
        }
    }

    private fun drawLongNoteNormal(x: Int, y: Int, y2: Int){
        val left = computeLeft(x, y)
        val posY = y.toFloat() + middleSizeFlechas
        var heightBody = (y2 - y).toFloat() - middleSizeFlechas

        if (isMidLine) {
            if (posY < initArrow) {
                if (posY + heightBody > initArrow) {
                    heightBody = (initArrow - posY).toFloat()
                }

                var currentY = posY
                while (currentY < posY + heightBody) {
                    val alphaSegment = getAlpha(currentY, initArrow)
                    val drawHeight = minOf(segmentHeight, posY + heightBody - currentY)
                    batch.setColor(1f, 1f, 1f, alphaSegment)
                    batch.draw(arrArrowsBody[x][arrowFrame], left, currentY, medidaFlechas, drawHeight)
                    currentY += drawHeight
                }

                if (y2 < initArrow) {
                    batch.setColor(1f, 1f, 1f, getAlpha(y2.toFloat(), initArrow))
                    val shouldDrawBottom = (y2 - y) > (heightBodyHead)
                    if (shouldDrawBottom) {
                        batch.draw(arrArrowsBottom[x][arrowFrame], left, y2.toFloat(), medidaFlechas, medidaFlechas)
                    }
                }

                if (y > 0) {
                    batch.setColor(1f, 1f, 1f, getAlpha(y.toFloat(), initArrow))
                    batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                }

                batch.setColor(1f, 1f, 1f, 1f)
            }
        } else {
            if(heightBody > middleSizeFlechas - 1){
                batch.draw(arrArrowsBody[x][arrowFrame], left, posY, medidaFlechas, heightBody)
                batch.draw(arrArrowsBottom[x][arrowFrame], left, y2.toFloat(), medidaFlechas, medidaFlechas)
            }

            if (y > 0) {
                batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
            }
        }
    }

    private fun drawLongNoteVanish(x: Int, y: Int, y2: Int){
        val left = computeLeft(x, y)
        val posY = y.toFloat() + middleSizeFlechas
        var heightBody = (y2 - y).toFloat() - middleSizeFlechas
        if (isMidLine) {
            if (posY + heightBody > MEASUREVANISH && posY < initArrow){
                var currentY = posY
                while (currentY < posY + heightBody) {
                    val drawHeight = minOf(segmentHeight, posY + heightBody - currentY)

                    val alphaSegment = when {
                        currentY > MEASUREVANISH + (medidaFlechas * 2) -> 1f
                        currentY >= MEASUREVANISH + (medidaFlechas * 2) - rangeAlpha -> {
                            ((currentY - (MEASUREVANISH + (medidaFlechas * 2) - rangeAlpha)) / rangeAlpha)
                                .toFloat()
                                .coerceIn(0f, 1f)
                        }
                        else -> 0f
                    }

                    if (alphaSegment > 0f) {
                        batch.setColor(1f, 1f, 1f, alphaSegment)
                        batch.draw(arrArrowsBody[x][arrowFrame], left, currentY, medidaFlechas, drawHeight)
                    }

                    currentY += drawHeight
                }
                batch.setColor(1f, 1f, 1f, 1f)

                if (posY + heightBody > MEASUREVANISH) {
                    batch.setColor(1f, 1f, 1f, getVanishAlpha(y2.toFloat()))

                    val shouldDrawBottom = (y2 - y) > (heightBodyHead)
                    if (shouldDrawBottom) {
                        batch.draw(arrArrowsBottom[x][arrowFrame], left, y2.toFloat(), medidaFlechas, medidaFlechas)
                    }
                }
                if (posY > MEASUREVANISH) {
                    if (y > 0) {
                        batch.setColor(1f, 1f, 1f, getVanishAlpha(y.toFloat()))
                        //batch.draw(arrArrowsBody[x][arrowFrame], left, y + heightBodyHead, medidaFlechas, heightBodyHead)
                        batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                    }

                    batch.setColor(1f, 1f, 1f, 1f)
                }
            }
        } else {
            var currentY = posY
            while (currentY < posY + heightBody) {
                val drawHeight = minOf(segmentHeight, posY + heightBody - currentY)

                val alphaSegment = when {
                    currentY > MEASUREVANISH + (medidaFlechas * 2) -> 1f
                    currentY >= MEASUREVANISH + (medidaFlechas * 2) - rangeAlpha -> {
                        ((currentY - (MEASUREVANISH + (medidaFlechas * 2) - rangeAlpha)) / rangeAlpha)
                            .toFloat()
                            .coerceIn(0f, 1f)
                    }
                    else -> 0f
                }

                if (alphaSegment > 0f) {
                    batch.setColor(1f, 1f, 1f, alphaSegment)
                    batch.draw(arrArrowsBody[x][arrowFrame], left, currentY, medidaFlechas, drawHeight)
                }

                currentY += drawHeight
            }
            batch.setColor(1f, 1f, 1f, 1f)

            if (posY + heightBody > MEASUREVANISH) {
                batch.setColor(1f, 1f, 1f, getVanishAlpha(y2.toFloat()))

                val shouldDrawBottom = (y2 - y) > (heightBodyHead)
                if (shouldDrawBottom) {
                    batch.draw(arrArrowsBottom[x][arrowFrame], left, y2.toFloat(), medidaFlechas, medidaFlechas)
                }
            }
            if (posY > MEASUREVANISH) {
                if (y > 0) {
                    batch.setColor(1f, 1f, 1f, getVanishAlpha(y.toFloat()))
                    batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                }

                batch.setColor(1f, 1f, 1f, 1f)
            }
        }
    }

    private fun drawLongNoteAp(x: Int, y: Int, y2: Int){
        val left = computeLeft(x, y)
        val posY = y.toFloat() + middleSizeFlechas
        var heightBody = (y2 - y).toFloat() - middleSizeFlechas
        if (posY < MEASURE) {
            if (posY + heightBody > MEASURE) {
                heightBody = (MEASURE - posY).toFloat()
            }
            var currentY = posY
            while (currentY < posY + heightBody) {
                val alphaSegment = getAlpha(currentY, MEASURE)
                val drawHeight = minOf(segmentHeight, posY + heightBody - currentY)
                batch.setColor(1f, 1f, 1f, alphaSegment)
                batch.draw(arrArrowsBody[x][arrowFrame], left, currentY, medidaFlechas, drawHeight)
                currentY += drawHeight
            }

            if (y2 < MEASURE) {
                batch.setColor(1f, 1f, 1f, getAlpha(y2.toFloat(), MEASURE))
                val shouldDrawBottom = (y2 - y) > (heightBodyHead)
                if (shouldDrawBottom) {
                    batch.draw(arrArrowsBottom[x][arrowFrame], left, y2.toFloat(), medidaFlechas, medidaFlechas)
                }
            }

            if (y > 0) {
                batch.setColor(1f, 1f, 1f, getAlpha(y.toFloat(), MEASURE))
                batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
            }
            batch.setColor(1f, 1f, 1f, 1f)
        }
    }

    private fun drawNoteNormal(x: Int, y: Int){
        val left = computeLeft(x, y)
        if (isMidLine) {
            if (y < initArrow) {
                batch.setColor(1f, 1f, 1f, getAlpha(y.toFloat(), initArrow))
                batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                batch.setColor(1f, 1f, 1f, 1f)
            }
        } else {
            batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
        }
    }

    private fun drawNoteVanish(x: Int, y: Int){
        val left = computeLeft(x, y)
        if(isMidLine){
            if (y < initArrow && y > MEASUREVANISH) {
                batch.setColor(1f, 1f, 1f, getVanishAlpha(y.toFloat()))
                batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                batch.setColor(1f, 1f, 1f, 1f)
            }
        }else {
            if (y > MEASUREVANISH) {
                batch.setColor(1f, 1f, 1f, getVanishAlpha(y.toFloat()))
                batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                batch.setColor(1f, 1f, 1f, 1f)
            }
        }
    }

    private fun drawNoteAp(x: Int, y: Int){
        val left = computeLeft(x, y)
        if (y < MEASURE) {
            batch.setColor(1f, 1f, 1f, getAlpha(y.toFloat(), MEASURE))
            batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
            batch.setColor(1f, 1f, 1f, 1f)
        }
    }

    private fun drawNoteMine(x: Int, y: Int){
        val left = computeLeft(x, y)
        if (isMidLine) {
            if (y < initArrow) {
                batch.setColor(1f, 1f, 1f, getAlpha(y.toFloat(), initArrow))
                batch.draw(arrMines[arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                batch.setColor(1f, 1f, 1f, 1f)
            }
        } else {
            batch.draw(arrMines[arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
        }
    }

    private fun drawNoteMineVanish(x: Int, y: Int){
        val left = computeLeft(x, y)
        if(isMidLine){
            if (y < initArrow && y > MEASUREVANISH) {
                batch.setColor(1f, 1f, 1f, getVanishAlpha(y.toFloat()))
                batch.draw(arrMines[arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                batch.setColor(1f, 1f, 1f, 1f)
            }
        }else {
            if (y > MEASUREVANISH) {
                batch.setColor(1f, 1f, 1f, getVanishAlpha(y.toFloat()))
                batch.draw(arrMines[arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                batch.setColor(1f, 1f, 1f, 1f)
            }
        }
    }

    private fun drawNoteMineAp(x: Int, y: Int){
        val left = computeLeft(x, y)
        if (y < MEASURE) {
            batch.setColor(1f, 1f, 1f, getAlpha(y.toFloat(), MEASURE))
            batch.draw(arrMines[arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
            batch.setColor(1f, 1f, 1f, 1f)
        }
    }

    private fun computeLeft(x: Int, y: Int): Float {
        val baseX = medidaFlechas * (x + 1)
        if (playerSong.snake) {
            offsetX = (sin(y * frequency) * amplitude)
            if (y <= medidaFlechas + fadeDistance) {
                val factor = (y - medidaFlechas) / fadeDistance
                offsetX *= factor.coerceIn(0f, 1f)
            }
        }

        return baseX + offsetX + luaNotes.screenX
    }

    private fun getAlpha(y: Float, init: Double): Float {
        return ((init - y) / rangeAlpha).toFloat().coerceIn(0f, 1f)
    }

    private fun getVanishAlpha(y: Float): Float {
        return ((y - MEASUREVANISH) / rangeAlpha).toFloat().coerceIn(0f, 1f)
    }

    private fun drawFlare(x: Int, frame: Int) {
        var left = 0f
        when (x) {
            0 -> left = (medidaFlechas * (x + 1) - xFlare1) + luaFlare.screenX
            1 -> left = (medidaFlechas * (x + 1) - xFlare2) + luaFlare.screenX
            2 -> left = (medidaFlechas * (x + 1) - xFlare3) + luaFlare.screenX
            3 -> left = (medidaFlechas * (x + 1) - xFlare4) + luaFlare.screenX
            4 -> left = (medidaFlechas * (x + 1) - xFlare5) + luaFlare.screenX
        }
        val flareSprite = flareSprites[frame]
        flareSprite.setBounds(left, yFlare, widthFlare, widthFlare)
        aBatch = batch.blendSrcFunc
        bBatch = batch.blendDstFunc
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        flareSprite.draw(batch)
        batch.setBlendFunction(aBatch, bBatch)

        val elapsed = timeGetTime() - flare[x].startTime

        val (alpha, zoom) = calculateAlphaAndZoom(elapsed % animationDuration)

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        batch.color.a = alpha

        batch.draw(
            arrArrows[x][arrowFrame],
            ((medidaFlechas * (x + 1)) - ((medidaFlechas * zoom) - medidaFlechas) / 2) + luaFlare.screenX,
            STEPSIZE.toFloat() - ((medidaFlechas * zoom) - medidaFlechas) / 2,
            medidaFlechas * zoom,
            medidaFlechas * zoom
        )

        batch.color.a = 1f
        batch.setBlendFunction(aBatch, bBatch)
    }

    private val flareSprites = Array(flareArrowFrame.size) { i ->
        Sprite(flareArrowFrame[i])
    }

    private fun calculateAlphaAndZoom(elapsedTime: Long): Pair<Float, Float> {
        val phaseDuration = 360 / 3
        return when {
            elapsedTime < phaseDuration -> {
                val progress = elapsedTime / phaseDuration.toFloat()
                1.0f - 0.4f * progress to 1.0f - 0.2f * progress
            }
            elapsedTime < 2 * phaseDuration -> {
                val progress = (elapsedTime - phaseDuration) / phaseDuration.toFloat()
                0.6f - 0.3f * progress to 0.8f + 0.2f * progress
            }
            else -> {
                val progress = (elapsedTime - 2 * phaseDuration) / phaseDuration.toFloat()
                0.3f - 0.3f * progress to 1.0f - 0.2f * progress
            }
        }
    }

    private fun showExpand(position: Int) {
        batch.setColor(1f, 1f, 1f, 0.7f)
        when (position) {
            0 -> {
                batch.draw(screen.recept0Frames[2], (medidaFlechas - posX) + luaRecepts.screenX, topPos, sizeScale, sizeScale)
                if (showPadB == 2) {
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(
                        screen.arrPadsC[position][arrowFrame],
                        screen.padPositionsC[position].x,
                        screen.padPositionsC[position].y,
                        screen.padPositionsC[position].size,
                        screen.padPositionsC[position].size
                    )
                }
                if (showPadB == 3) {
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(
                        screen.arrayPad4[position],
                        padPositions[0][0],
                        padPositions[0][1],
                        widthBtns,
                        heightBtns
                    )
                }
            }
            1 -> {
                batch.draw(screen.recept1Frames[2], ((medidaFlechas * 2) - posX) + luaRecepts.screenX, topPos, sizeScale, sizeScale)
                if (showPadB == 2) {
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(
                        screen.arrPadsC[position][arrowFrame],
                        screen.padPositionsC[position].x,
                        screen.padPositionsC[position].y,
                        screen.padPositionsC[position].size,
                        screen.padPositionsC[position].size
                    )
                }
                if (showPadB == 3) {
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(
                        screen.arrayPad4[position],
                        padPositions[1][0],
                        padPositions[1][1],
                        widthBtns,
                        heightBtns
                    )
                }
            }
            2 -> {
                batch.draw(screen.recept2Frames[2], ((medidaFlechas * 3) - posX) + luaRecepts.screenX, topPos, sizeScale, sizeScale)
                if (showPadB == 2) {
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(
                        screen.arrPadsC[position][arrowFrame],
                        screen.padPositionsC[position].x,
                        screen.padPositionsC[position].y,
                        screen.padPositionsC[position].size,
                        screen.padPositionsC[position].size
                    )
                }
                if (showPadB == 3) {
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(
                        screen.arrayPad4[position],
                        padPositions[2][0],
                        padPositions[2][1],
                        widthBtns,
                        heightBtns
                    )
                }
            }
            3 -> {
                batch.draw(screen.recept3Frames[2], ((medidaFlechas * 4) - posX) + luaRecepts.screenX, topPos, sizeScale, sizeScale)
                if (showPadB == 2) {
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(
                        screen.arrPadsC[position][arrowFrame],
                        screen.padPositionsC[position].x,
                        screen.padPositionsC[position].y,
                        screen.padPositionsC[position].size,
                        screen.padPositionsC[position].size
                    )
                }
                if (showPadB == 3) {
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(
                        screen.arrayPad4[position],
                        padPositions[3][0],
                        padPositions[3][1],
                        widthBtns,
                        heightBtns
                    )
                }
            }
            4 -> {
                batch.draw(screen.recept4Frames[2], ((medidaFlechas * 5) - posX) + luaRecepts.screenX, topPos, sizeScale, sizeScale)
                if (showPadB == 2) {
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(
                        screen.arrPadsC[position][arrowFrame],
                        screen.padPositionsC[position].x,
                        screen.padPositionsC[position].y,
                        screen.padPositionsC[position].size,
                        screen.padPositionsC[position].size
                    )
                }
                if (showPadB == 3) {
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(
                        screen.arrayPad4[position],
                        padPositions[4][0],
                        padPositions[4][1],
                        widthBtns,
                        heightBtns
                    )
                }
            }
        }
        batch.setColor(1f, 1f, 1f, 1f)
    }

    private val judgeSprite = Sprite()
    private val comboSprite = Sprite()
    private val digitSprite = Sprite()
    private fun drawJudge(time: Long) {
        val ftX: Float
        val ftY: Float
        val alpha: Float

        if (time < 100) {
            ftX = (time / 300f) + 1.0f
            ftY = ftX
            alpha = 1f
        } else if (time > 1200) {
            val progress = ((time - 1200) / 300f).coerceIn(0f, 1f)
            ftX = 1.0f + 0.6f * progress
            ftY = 1.0f - 0.8f * progress
            alpha = (1.0f - progress).coerceIn(0f, 1f)
        } else {
            ftX = 1.0f
            ftY = 1.0f
            alpha = 1.0f
        }

        judgeSprite.setRegion(screen.imgsJudge[m_judge.judge])
        val judgeW = widthJudges * ftX
        val judgeH = heightJudges * ftY

        judgeSprite.setSize(judgeW, judgeH)
        judgeSprite.setOriginCenter()
        judgeSprite.setCenter(x + widthJudges / 2f, y + heightJudges / 2f)
        judgeSprite.setColor(1f, 1f, 1f, alpha)
        judgeSprite.draw(batch)

        val comboWidth = (widthJudges * 0.5f) * ftX
        val comboHeight = (heightJudges * 0.5f) * ftY
        val comboY = y + heightJudges + 5f
        val comboX = widthJudges - comboWidth / 2

        val digitW = digitWidth * ftX
        val digitH = digitHeight * ftY

        if (curCombo >= 4 || curComboMiss >= 4) {
            val isMiss = curComboMiss >= 4
            comboSprite.setRegion(if (isMiss) screen.imgsTypeCombo[1] else (screen.imgsTypeCombo[0]))
            val count = if (isMiss) curComboMiss else curCombo
            val numberList = if (isMiss) screen.listNumbersMiss else screen.listNumbers

            comboSprite.setSize(comboWidth, comboHeight)
            comboSprite.setOriginCenter()
            comboSprite.setCenter(comboX + comboWidth / 2f, comboY + comboHeight / 2f)
            comboSprite.setColor(1f, 1f, 1f, alpha)
            comboSprite.draw(batch)

            val numStr = if (count < 100) count.toString().padStart(3, '0') else count.toString()

            val totalWidth = numStr.length * digitW
            var startX = widthJudges - (totalWidth / 2)
            val digitY = comboY + comboHeight - 10f

            //digitSprite = Sprite()
            digitSprite.setColor(1f, 1f, 1f, alpha)
            for (char in numStr) {
                val digit = char.digitToInt()
                digitSprite.setRegion(numberList[digit])
                digitSprite.setBounds(startX, digitY, digitW, digitH)
                digitSprite.draw(batch)
                startX += digitW
            }
        }
    }

    private fun drawGauge(gauge: Float) {
        val previousSrcFunc = batch.blendSrcFunc
        val previousDstFunc = batch.blendDstFunc

        val barToDraw = if (gauge <= 0.2f) screen.barRed else screen.barBlack
        barToDraw.setSize(screen.maxWidth, screen.maxlHeight)
        barToDraw.setPosition(medidaFlechas, 0f)
        barToDraw.draw(batch)

        val visibleWidth = screen.maxWidth * gauge
        val regionWidth = (screen.barColors.texture.width * gauge).toInt()

        if (regionWidth > 0.1 && visibleWidth > 0.1f) {
            screen.barColors.setRegion(0, 0, regionWidth, screen.barColors.texture.height)
            screen.barColors.setSize(visibleWidth, screen.maxlHeight)
            screen.barColors.setPosition(medidaFlechas, 0f)
            screen.barColors.draw(batch)
        }

        if (gauge >= 1.0f) {
            val currentTime = (timeGetTime() / 100L) % 2 == 0L

            if (currentTime) {
                val time = (timeGetTime() % 200L) / 200f
                val shine = 1f + 0.5f * Math.sin(time * Math.PI).toFloat()
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)

                screen.barColors.setColor(shine, shine, shine, 1f)
                screen.barColors.draw(batch)
                batch.setBlendFunction(previousSrcFunc, previousDstFunc)
            } else {
                screen.barColors.setColor(1f, 1f, 1f, 1f)
                screen.barColors.draw(batch)
            }
        } else {
            screen.barColors.setColor(1f, 1f, 1f, 1f)
        }
        screen.barFrame.setSize(screen.maxWidth, screen.maxlHeight)
        screen.barFrame.setPosition(medidaFlechas, 0f)
        screen.barFrame.draw(batch)

        if (gauge <= 0.99f && gauge > 0f) {
            val tipX = visibleWidth + medidaFlechas
            screen.barTip.setSize(tipWidth, tipHeight)
            screen.barTip.setPosition(tipX, tipY)
            screen.barTip.draw(batch)
        }
    }

    private fun onMineHit(timeCom: Long) {
        mineFlashStartTime = timeCom
        soundPoolSelectSong.play(sound_mine, 1f, 1f, 1, 0, 1f)
    }

    private fun getArrows3x2(arrow: Texture, isMirror: Boolean = false): Array<TextureRegion> {
        val tmp = TextureRegion.split(arrow, arrow.width / 3, arrow.height / 2)

        return if (!isMirror) {
            val frames = arrayOf(
                tmp[0][0], tmp[0][1], tmp[0][2],
                tmp[1][0], tmp[1][1], tmp[1][2]
            )
            frames.forEach { it.flip(false, true) }
            frames
        } else {
            val frames = arrayOf(
                tmp[0][0], tmp[0][1], tmp[0][2],
                tmp[1][0], tmp[1][1], tmp[1][2]
            )
            frames.forEach { it.flip(true, true) }
            frames
        }
    }

    private fun getArrows6x1(arrow: Texture, isMirror: Boolean = false): Array<TextureRegion> {
        arrow.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        val tmp = TextureRegion.split(arrow, arrow.width / 6, arrow.height)

        return if (!isMirror) {
            val frames = arrayOf(
                tmp[0][0], tmp[0][1], tmp[0][2],
                tmp[0][3], tmp[0][4], tmp[0][5]
            )
            frames.forEach { it.flip(false, true) }
            frames
        } else {
            val frames = arrayOf(
                tmp[0][0], tmp[0][1], tmp[0][2],
                tmp[0][3], tmp[0][4], tmp[0][5]
            )
            frames.forEach { it.flip(true, true) }
            frames
        }
    }

    fun disposePlayer() {
        arrMines.forEach { it.texture.dispose() }

        arrArrows.forEach { frameArray ->
            frameArray.forEach { it.texture.dispose() }
        }

        arrArrowsBody.forEach { frameArray ->
            frameArray.forEach { it.texture.dispose() }
        }

        arrArrowsBottom.forEach { frameArray ->
            frameArray.forEach { it.texture.dispose() }
        }
        sprFlare.dispose()
        whiteTex.dispose()
        curCombo = 0
        inputProcessor.dispose()
    }

    fun createWhiteTexture() {
        val pixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888)
        pixmap.setColor(Color.WHITE)
        pixmap.fill()
        whiteTex = Texture(pixmap)
        pixmap.dispose()
    }

    private fun drawMineFlash(timeCom: Long) {
        if (mineFlashStartTime == 0L) return

        val elapsed = timeCom - mineFlashStartTime
        if (elapsed >= MINE_FLASH_DURATION) {
            mineFlashStartTime = 0L
            return
        }
        val t = elapsed.toFloat() / MINE_FLASH_DURATION
        val alpha = 1f - (t * t)

        batch.setColor(1f, 1f, 1f, alpha)
        batch.draw(whiteTex, 0f, 0f, screen.gdxWidth.toFloat(), screen.gdxHeight.toFloat())
        batch.setColor(Color.WHITE)
    }

    fun triggerLuaFlash(duration: Long) {
        luaFlashDuration = duration
        luaFlashStartTime = timeGetTime()
    }

    private fun drawLuaFlash(timeCom: Long) {
        if (luaFlashStartTime == 0L) return
        val elapsed = timeCom - luaFlashStartTime
        if (elapsed >= luaFlashDuration) {
            luaFlashStartTime = 0L
            return
        }
        val t = elapsed.toFloat() / luaFlashDuration
        val alpha = 1f - (t * t)
        batch.setColor(1f, 1f, 1f, alpha)
        batch.draw(whiteTex, 0f, 0f, screen.gdxWidth.toFloat(), screen.gdxHeight.toFloat())
        batch.setColor(Color.WHITE)
    }

    fun timeGetTime(): Long{
        return SystemClock.uptimeMillis()
    }
}
