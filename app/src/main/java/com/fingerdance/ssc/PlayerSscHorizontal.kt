package com.fingerdance.ssc

import LuaEngine
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
import com.fingerdance.GameScreenActivityHorizontal
import com.fingerdance.breakSong
import com.fingerdance.chart
import com.fingerdance.height
import com.fingerdance.heightBtnsHorizontal
import com.fingerdance.heightJudges
import com.fingerdance.hjBad
import com.fingerdance.hjGood
import com.fingerdance.hjGreat
import com.fingerdance.hjPerfect
import com.fingerdance.isOnline
import com.fingerdance.loadTexture
import com.fingerdance.luaFlare
import com.fingerdance.luaNotes
import com.fingerdance.luaRecepts
import com.fingerdance.medidaFlechasHorizontal
import com.fingerdance.nBad
import com.fingerdance.nGood
import com.fingerdance.nGreat
import com.fingerdance.nPerfect
import com.fingerdance.padPositionsHorizontal
import com.fingerdance.playerSong
import com.fingerdance.resultSong
import com.fingerdance.ruta
import com.fingerdance.showPadB
import com.fingerdance.soundPoolSelectSong
import com.fingerdance.sound_mine
import com.fingerdance.spaceInitHorizontal
import com.fingerdance.valueOffset
import com.fingerdance.widthBtnsHorizontal
import com.fingerdance.widthJudges
import java.io.File

class PlayerSscHorizontal(
    val screen: GameScreenSscHorizontal,
    private val batch: SpriteBatch,
    private val activity: GameScreenActivityHorizontal
) : SscGameplayEngine.Renderer, SscGameplayEngine.Listener {

    private val bpms = chart.bpms
    private val tickcounts = chart.tickcounts
    private val stops = chart.stops
    private val delays = chart.delays
    private val warps = chart.warps
    private val notes = chart.notes
    private val speeds = chart.speeds
    private val scrolls = chart.scrolls
    private val combos = chart.combos

    private val sizeScale = medidaFlechasHorizontal
    private val topPos = medidaFlechasHorizontal
    private val posX = 0f
    private var aBatch = 0
    private var bBatch = 0
    private val xFlare1 = (medidaFlechasHorizontal * 2.1f)
    private val xFlare2 = (medidaFlechasHorizontal * 2.15f)
    private val xFlare3 = (medidaFlechasHorizontal * 2.1f)
    private val xFlare4 = (medidaFlechasHorizontal * 2.05f)
    private val xFlare5 = (medidaFlechasHorizontal * 2.05f)
    private val animationDuration: Long = 300L
    private var multiplierCombo = 1

    private var breakDanceTriggered = false

    companion object {
        val STEPSIZE = medidaFlechasHorizontal.toInt()

        const val MINUTE = 60000f

        private var ZONE_PERFECT: Long = if (playerSong.hj) hjPerfect else nPerfect
        private var ZONE_GREAT: Long = if (playerSong.hj) hjGreat else nGreat
        private var ZONE_GOOD: Long = if (playerSong.hj) hjGood else nGood
        private var ZONE_BAD: Long = if (playerSong.hj) hjBad else nBad

        const val JUDGE_PERFECT = 0
        const val JUDGE_GREAT = 1
        const val JUDGE_GOOD = 2
        const val JUDGE_BAD = 3
        const val JUDGE_MISS = 4

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

        const val LIFE_LIGHTNING_FPS = 16f
        const val LIFE_LIGHTNING_FRAME_DURATION = 1f / LIFE_LIGHTNING_FPS
    }

    data class PlayerFlare(var startTime: Long = 0)
    data class PlayerJudge(var startTime: Long = 0, var judge: Int = 0)
    data class JudgePos(
        val x: Float = Gdx.graphics.width / 2f,
        val y: Float = Gdx.graphics.height / 2f
    )

    data class HoldMetrics(
        val widthRatio: Float,
        val offsetRatio: Float
    )

    private val holdMetrics = Array(5) { Array(6) { HoldMetrics(1f, 0f) } }

    private val baseSpeed = playerSong.speed.replace("X", "").toFloat()

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

    private val gameplayEngineConfig =
        SscGameplayEngine.Config(
            columnCount = 5,
            activeColumns = 0..4,
            stepSize = medidaFlechasHorizontal,
            targetY = medidaFlechasHorizontal,
            screenHeight = screen.gdxHeight.toFloat(),
            baseSpeed = baseSpeed,
            isEW = playerSong.isEw,
            zonePerfectMs = ZONE_PERFECT,
            zoneGreatMs = ZONE_GREAT,
            zoneGoodMs = ZONE_GOOD,
            zoneBadMs = ZONE_BAD,
        )

    private val gameplayEngine =
        SscGameplayEngine(
            notes = notes,
            timingData = timingData,
            tickSegments = tickcounts.map {
                SscGameplayEngine.TickSegment(
                    beat = it.beat,
                    tickCount = it.tickcount.toDouble()
                )
            },
            comboSegments = combos.map {
                SscGameplayEngine.ComboSegment(
                    beat = it.beat,
                    multiplier = it.comboMultiplier
                )
            },
            config = gameplayEngineConfig,
            listener = this
        )

    private val currentChartLevel = playerSong.level.toInt()
    val barLifeCalculator = BarLifeCalculator(level = currentChartLevel)
    private var lifeLightningTime = 0f

    var m_fCurBPM = 0F
    private var arrowFrame = 0
    private var m_iStepWidth = 5
    var curCombo = 0
    var curComboMiss = 0
    private val flare = Array(5) { PlayerFlare() }
    private var m_judge = PlayerJudge()
    private var noEffects = false

    // Mine flash variables
    private var mineFlashStartTime: Long = 0L
    private val MINE_FLASH_DURATION: Long = 100L

    private val widthFlare = medidaFlechasHorizontal * 5f
    private var yFlare = medidaFlechasHorizontal - (medidaFlechasHorizontal * 2f)

    private val inputProcessor = InputProcessorSscHorizontal()

    private var currentTimeToExpands = 0L

    private val judgePos = JudgePos()
    private val x = judgePos.x.toFloat()
    private val y = judgePos.y.toFloat()
    private var digitWidth = medidaFlechasHorizontal * 0.7f
    private var digitHeight = heightJudges * 1.3f

    val tipWidth = (medidaFlechasHorizontal / 4f)
    val tipHeight = medidaFlechasHorizontal / 1.5f
    val tipY = 0f - (medidaFlechasHorizontal * 0.05f) + (screen.posYGauje)

    private val columnNotes = Array(5) { mutableListOf<Parser.Note>() }
    private val columnIndex = IntArray(5) { 0 }

    private var isVanish = playerSong.vanish
    private var isAp = playerSong.ap

    private var luaFlashStartTime = 0L
    private var luaFlashDuration = 0L
    lateinit var luaEngine: LuaEngine
    init {
        currentTimeToExpands = screen.timeGetTime()
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
        downLeftTap = loadTexture(ruta, "DownLeft Tap Note")
        upLeftTap = loadTexture(ruta, "UpLeft Tap Note")
        centerTap = loadTexture(ruta, "Center Tap Note")
        upRightTap = upLeftTap
        downRightTap = downLeftTap

        downLeftBody = loadTexture(ruta, "DownLeft Hold Body Active")
        upLeftBody = loadTexture(ruta, "UpLeft Hold Body Active")
        centerBody = loadTexture(ruta, "Center Hold Body Active")
        upRightBody = upLeftBody
        downRightBody = downLeftBody

        downLeftBottom = loadTexture(ruta, "DownLeft Hold BottomCap Active")
        upLeftBottom = loadTexture(ruta, "UpLeft Hold BottomCap Active")
        centerBottom = loadTexture(ruta, "Center Hold BottomCap Active")
        upRightBottom = upLeftBottom
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

        arrArrowsBody = arrayOf(
            ldBodyArrowFrame,
            luBodyArrowFrame,
            ceBodyArrowFrame,
            ruBodyArrowFrame,
            rdBodyArrowFrame
        )

        val ldBottomArrowFrame = getArrows6x1(downLeftBottom, metricsColumn = 0)
        val luBottomArrowFrame = getArrows6x1(upLeftBottom, metricsColumn = 1)
        val ceBottomArrowFrame = getArrows6x1(centerBottom, metricsColumn = 2)
        val ruBottomArrowFrame = getArrows6x1(upLeftBottom, true, metricsColumn = 3)
        val rdBottomArrowFrame = getArrows6x1(downLeftBottom, true, metricsColumn = 4)

        arrArrowsBottom = arrayOf(
            ldBottomArrowFrame,
            luBottomArrowFrame,
            ceBottomArrowFrame,
            ruBottomArrowFrame,
            rdBottomArrowFrame
        )

        sprFlare = Texture(Gdx.files.absolute("$ruta/Flare 6x1.png"))
        flareArrowFrame = getArrows6x1Flare(sprFlare)

        arrMines = getArrows3x2(mine)
        createWhiteTexture()
        initColumnNotes()
        inputProcessor.resetState()
        luaEngine = LuaEngine(playerSscHorizontal = this, widthNotes = medidaFlechasHorizontal * 5f)
        barLifeCalculator.reset()
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


    private val iLongTop = LongArray(5)
    fun render(songTimeMs: Double) {
        val timeCom = screen.timeGetTime()
        val delta = Gdx.graphics.deltaTime
        val nowMs = songTimeMs.toDouble()
        val currentBeat = timeToBeat(nowMs)

        if (showPadB == 0) {
            inputProcessor.render(batch)
        }

        val currentBpm = bpms.lastOrNull { it.beat <= currentBeat }?.bpm ?: bpms.firstOrNull()?.bpm ?: 120.0

        m_fCurBPM = currentBpm.toFloat()

        val msPorBeat = MINUTE / m_fCurBPM.coerceIn(1f, 999f)
        val msPorFrame = msPorBeat / 5f
        arrowFrame = ((timeCom % msPorBeat.toLong()) / msPorFrame.toLong()).toInt()

        updateFGChanges(currentBeat)

        gameplayEngine.render(
            songTimeMs = songTimeMs,
            renderer = this
        )

        updateExpandAnimations(delta)
        drawExpandEffects()

        val beatPhase = (currentBeat - kotlin.math.floor(currentBeat)).toFloat()
        val stretchProgress = beatPhase.coerceIn(0f, 1f)

        drawGauge(
            gauge = barLifeCalculator.visibleProgress,
            stretchProgress = stretchProgress
        )

        drawOverflowLightning(delta = Gdx.graphics.deltaTime)

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

    fun updateStepData(songTimeMs: Double) {
        inputProcessor.update()
        gameplayEngine.updateStepData(
            songTimeMs = songTimeMs,
            input = inputProcessor.logicalState
        )

        if (
            barLifeCalculator.state.failed &&
            !breakDanceTriggered &&
            !isOnline &&
            breakSong
        ) {
            breakDanceTriggered = true
            activity.breakDance()
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
                curComboMiss += 1
            }
        }

        if (resultSong.maxCombo < curCombo) {
            resultSong.maxCombo = curCombo
        }

        val lifeJudgment = when (judge) {
            JUDGE_PERFECT -> LifeJudgment.PERFECT
            JUDGE_GREAT -> LifeJudgment.GREAT
            JUDGE_GOOD -> LifeJudgment.GOOD
            JUDGE_BAD -> LifeJudgment.BAD
            JUDGE_MISS -> LifeJudgment.MISS
            else -> null
        }

        if (lifeJudgment != null) {
            barLifeCalculator.applyJudgment(
                judgment = lifeJudgment,
                isBodyLongNote = isBodyLongNote,
                isMine = isMine
            )
        }

        val now = screen.timeGetTime()
        m_judge.judge = judge
        m_judge.startTime = now

    }

    override fun onNoteFlare(column: Int) {
        if (column !in flare.indices) return
        flare[column].startTime = screen.timeGetTime()
    }

    private fun timeToBeat(timeMs: Double): Double = timingData.timeToBeat(timeMs)

    val MEASURE = height * 0.25
    val MEASUREVANISH = medidaFlechasHorizontal * 3
    private var rangeAlpha = (screen.gdxHeight * 0.1)
    private val segmentHeight = screen.gdxHeight * 0.001f
    private val heightBodyHead = medidaFlechasHorizontal * 0.3f
    private var middleSizeFlechas = medidaFlechasHorizontal * 0.5f
    private val amplitude = medidaFlechasHorizontal / 3f
    private val frequency = 0.01f
    private val fadeDistance = medidaFlechasHorizontal
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

    private fun drawLongNoteNormal(x: Int, y: Int, y2: Int) {
        val left = computeLeft(x, y)
        val posY = y.toFloat() + middleSizeFlechas
        val remainingLength = (y2 - y).toFloat()
        val heightBody = remainingLength - middleSizeFlechas

        val metric = holdMetrics[x][arrowFrame.coerceIn(0, 5)]
        val widthBody = medidaFlechasHorizontal * metric.widthRatio
        val leftBody = left + medidaFlechasHorizontal * metric.offsetRatio

        if (heightBody > 0f) {
            batch.draw(arrArrowsBody[x][arrowFrame], leftBody, posY, widthBody, heightBody)
        }

        if (remainingLength > heightBodyHead) {
            batch.draw(arrArrowsBottom[x][arrowFrame], left, y2.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
        }

        if (y > 0) {
            batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
        }
    }

    private fun drawLongNoteVanish(x: Int, y: Int, y2: Int){
        val left = computeLeft(x, y)
        val posY = y.toFloat() + middleSizeFlechas
        var heightBody = (y2 - y).toFloat() - middleSizeFlechas
        val metric = holdMetrics[x][arrowFrame.coerceIn(0, 5)]
        val widthBody = medidaFlechasHorizontal * metric.widthRatio
        val leftBody = left + medidaFlechasHorizontal * metric.offsetRatio
        var currentY = posY
        while (currentY < posY + heightBody) {
            val drawHeight = minOf(segmentHeight, posY + heightBody - currentY)

            val alphaSegment = when {
                currentY > MEASUREVANISH + (medidaFlechasHorizontal * 2) -> 1f
                currentY >= MEASUREVANISH + (medidaFlechasHorizontal * 2) - rangeAlpha -> {
                    ((currentY - (MEASUREVANISH + (medidaFlechasHorizontal * 2) - rangeAlpha)) / rangeAlpha)
                        .toFloat()
                        .coerceIn(0f, 1f)
                }
                else -> 0f
            }

            if (alphaSegment > 0f) {
                batch.setColor(1f, 1f, 1f, alphaSegment)
                batch.draw(arrArrowsBody[x][arrowFrame], leftBody, currentY, widthBody, drawHeight)
            }

            currentY += drawHeight
        }
        batch.setColor(1f, 1f, 1f, 1f)

        if (posY + heightBody > MEASUREVANISH) {
            batch.setColor(1f, 1f, 1f, getVanishAlpha(y2.toFloat()))

            val shouldDrawBottom = (y2 - y) > (heightBodyHead)
            if (shouldDrawBottom) {
                batch.draw(arrArrowsBottom[x][arrowFrame], left, y2.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
            }
        }
        if (posY > MEASUREVANISH) {
            if (y > 0) {
                batch.setColor(1f, 1f, 1f, getVanishAlpha(y.toFloat()))
                batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
            }

            batch.setColor(1f, 1f, 1f, 1f)
        }
    }

    private fun drawLongNoteAp(x: Int, y: Int, y2: Int){
        val left = computeLeft(x, y)
        val posY = y.toFloat() + middleSizeFlechas
        var heightBody = (y2 - y).toFloat() - middleSizeFlechas
        val metric = holdMetrics[x][arrowFrame.coerceIn(0, 5)]
        val widthBody = medidaFlechasHorizontal * metric.widthRatio
        val leftBody = left + medidaFlechasHorizontal * metric.offsetRatio
        if (posY < MEASURE) {
            if (posY + heightBody > MEASURE) {
                heightBody = (MEASURE - posY).toFloat()
            }
            var currentY = posY
            while (currentY < posY + heightBody) {
                val alphaSegment = getAlpha(currentY, MEASURE)
                val drawHeight = minOf(segmentHeight, posY + heightBody - currentY)
                batch.setColor(1f, 1f, 1f, alphaSegment)
                batch.draw(arrArrowsBody[x][arrowFrame], leftBody, currentY, widthBody, drawHeight)
                currentY += drawHeight
            }

            if (y2 < MEASURE) {
                batch.setColor(1f, 1f, 1f, getAlpha(y2.toFloat(), MEASURE))
                val shouldDrawBottom = (y2 - y) > (heightBodyHead)
                if (shouldDrawBottom) {
                    batch.draw(arrArrowsBottom[x][arrowFrame], left, y2.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
                }
            }

            if (y > 0) {
                batch.setColor(1f, 1f, 1f, getAlpha(y.toFloat(), MEASURE))
                batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
            }
            batch.setColor(1f, 1f, 1f, 1f)
        }
    }

    private fun drawNoteNormal(x: Int, y: Int){
        val left = computeLeft(x, y)
        batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
    }

    private fun drawNoteVanish(x: Int, y: Int){
        val left = computeLeft(x, y)
        if (y > MEASUREVANISH) {
            batch.setColor(1f, 1f, 1f, getVanishAlpha(y.toFloat()))
            batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
            batch.setColor(1f, 1f, 1f, 1f)
        }

    }

    private fun drawNoteAp(x: Int, y: Int){
        val left = computeLeft(x, y)
        if (y < MEASURE) {
            batch.setColor(1f, 1f, 1f, getAlpha(y.toFloat(), MEASURE))
            batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
            batch.setColor(1f, 1f, 1f, 1f)
        }
    }

    private fun drawNoteMine(x: Int, y: Int){
        val left = computeLeft(x, y)
        batch.draw(arrMines[arrowFrame], left, y.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)

    }

    private fun drawNoteMineVanish(x: Int, y: Int){
        val left = computeLeft(x, y)
        if (y > MEASUREVANISH) {
            batch.setColor(1f, 1f, 1f, getVanishAlpha(y.toFloat()))
            batch.draw(arrMines[arrowFrame], left, y.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
            batch.setColor(1f, 1f, 1f, 1f)
        }
    }

    private fun drawNoteMineAp(x: Int, y: Int){
        val left = computeLeft(x, y)
        if (y < MEASURE) {
            batch.setColor(1f, 1f, 1f, getAlpha(y.toFloat(), MEASURE))
            batch.draw(arrMines[arrowFrame], left, y.toFloat(), medidaFlechasHorizontal, medidaFlechasHorizontal)
            batch.setColor(1f, 1f, 1f, 1f)
        }
    }

    private fun computeLeft(x: Int, y: Int): Float {
        val baseX = medidaFlechasHorizontal * (x + 1) + spaceInitHorizontal
        if (playerSong.snake) {
            offsetX = (sin(y * frequency) * amplitude)
            if (y <= medidaFlechasHorizontal + fadeDistance) {
                val factor = (y - medidaFlechasHorizontal) / fadeDistance
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
        when(x){
            0->{left = (medidaFlechasHorizontal * (x + 1) - xFlare1) + spaceInitHorizontal + luaFlare.screenX}
            1->{left = (medidaFlechasHorizontal * (x + 1) - xFlare2) + spaceInitHorizontal + luaFlare.screenX}
            2->{left = (medidaFlechasHorizontal * (x + 1) - xFlare3) + spaceInitHorizontal + luaFlare.screenX}
            3->{left = (medidaFlechasHorizontal * (x + 1) - xFlare4) + spaceInitHorizontal + luaFlare.screenX}
            4->{left = (medidaFlechasHorizontal * (x + 1) - xFlare5) + spaceInitHorizontal + luaFlare.screenX}
        }
        val flareSprite = flareSprites[frame]
        flareSprite.setBounds(left, yFlare, widthFlare, widthFlare)
        aBatch = batch.blendSrcFunc
        bBatch = batch.blendDstFunc
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        flareSprite.draw(batch)
        batch.setBlendFunction(aBatch, bBatch)

        val elapsed = screen.timeGetTime() - flare[x].startTime

        val (alpha, zoom) = calculateAlphaAndZoom(elapsed % animationDuration)

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        batch.color.a = alpha // Establecer transparencia (alpha)

        batch.draw(
            arrArrows[x][arrowFrame],
            ((medidaFlechasHorizontal * (x + 1)) - ((medidaFlechasHorizontal * zoom) - medidaFlechasHorizontal) / 2) + spaceInitHorizontal + luaFlare.screenX,
            STEPSIZE.toFloat() - ((medidaFlechasHorizontal * zoom) - medidaFlechasHorizontal) / 2,
            medidaFlechasHorizontal * zoom,
            medidaFlechasHorizontal * zoom
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

    private val expandDuration = 0.18f
    private val expandMaximumScale = 1.22f
    private val expandElapsed = FloatArray(5) { expandDuration }

    private fun startExpand(position: Int) {
        if (position !in expandElapsed.indices) return
        expandElapsed[position] = 0f
    }

    private fun updateExpandAnimations(delta: Float) {
        for (position in expandElapsed.indices) {
            if (expandElapsed[position] < expandDuration) {
                expandElapsed[position] = (expandElapsed[position] + delta).coerceAtMost(expandDuration)
            }
        }
    }

    private fun getExpandReceptor(position: Int): TextureRegion? {
        return when (position) {
            0 -> screen.recept0Frames[2]
            1 -> screen.recept1Frames[2]
            2 -> screen.recept2Frames[2]
            3 -> screen.recept3Frames[2]
            4 -> screen.recept4Frames[2]
            else -> null
        }
    }

    private fun getReceptorX(position: Int): Float {
        return (medidaFlechasHorizontal * (position + 1) - posX) + spaceInitHorizontal + luaRecepts.screenX
    }

    private fun drawExpandEffect(position: Int) {
        if (position !in expandElapsed.indices) return
        val elapsed = expandElapsed[position]
        if (elapsed >= expandDuration) return
        val receptor = getExpandReceptor(position) ?: return
        val progress = (elapsed / expandDuration).coerceIn(0f, 1f)
        val scale = 1f + (expandMaximumScale - 1f) * progress
        val alpha = (0.8f - progress * progress).coerceIn(0f, 0.8f)
        val baseX = getReceptorX(position)
        val baseY = topPos

        val originX = when (position) {
            0 -> sizeScale * 0.30f
            4 -> sizeScale * 0.70f
            else -> sizeScale * 0.50f
        }

        val originY = sizeScale * 0.50f
        val previousSrc = batch.blendSrcFunc
        val previousDst = batch.blendDstFunc
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        batch.setColor(1f, 1f, 1f, alpha)
        batch.draw(receptor, baseX, baseY, originX, originY, sizeScale, sizeScale, scale, scale, 0f)

        batch.setColor(1f, 1f, 1f, 1f)
        batch.setBlendFunction(previousSrc, previousDst)
    }

    private fun drawExpandEffects() {
        for (position in expandElapsed.indices) {
            drawExpandEffect(position)
        }
        batch.setColor(1f, 1f, 1f, 1f)
    }

    private fun drawPressedPad(logical: Int, physical: Int) {
        if (logical !in 0..4 || physical !in padPositionsHorizontal.indices) return
        batch.setColor(1f, 1f, 1f, 1f)

        if (showPadB == 3) {
            batch.draw(
                screen.arrayPad4[logical],
                padPositionsHorizontal[physical][0],
                padPositionsHorizontal[physical][1],
                widthBtnsHorizontal,
                heightBtnsHorizontal
            )
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
        }
        else if (time > 1200) {
            val progress = ((time - 1200) / 300f).coerceIn(0f, 1f)
            ftX = 1.0f + 0.6f * progress
            ftY = 1.0f - 0.8f * progress
            alpha = (1.0f - progress).coerceIn(0f, 1f)
        }
        else {
            ftX = 1.0f
            ftY = 1.0f
            alpha = 1.0f
        }

        judgeSprite.setRegion(screen.imgsJudge[m_judge.judge])
        val judgeW = widthJudges * ftX
        val judgeH = heightJudges * ftY

        judgeSprite.setSize(judgeW, judgeH)
        judgeSprite.setOriginCenter()
        judgeSprite.setCenter(x, y)
        judgeSprite.setColor(1f, 1f, 1f, alpha)
        judgeSprite.draw(batch)

        val comboWidth = (widthJudges * 0.5f) * ftX
        val comboHeight = (heightJudges * 0.5f) * ftY
        val comboY = y + heightJudges + 5f
        val comboX = (Gdx.graphics.width / 2f) - (comboWidth / 2f)

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
            var startX = (Gdx.graphics.width / 2f) - (totalWidth / 2f)
            val digitY = comboY + comboHeight - 10f

            digitSprite.setColor(1f, 1f, 1f, alpha)
            for (char in numStr) {
                val digit = char.digitToInt()
                digitSprite.setRegion(numberList[digit])
                digitSprite.setBounds(startX, digitY, digitW, digitH)
                digitSprite.setColor(1f, 1f, 1f, alpha)
                digitSprite.draw(batch)
                startX += digitW
            }
        }
    }

    private fun drawGauge(gauge: Float, stretchProgress: Float) {
        val previousSrcFunc = batch.blendSrcFunc
        val previousDstFunc = batch.blendDstFunc
        val safeGauge = gauge.coerceIn(0f, 1f)
        val safeStretch = stretchProgress.coerceIn(0f, 1f)

        val barToDraw = if (safeGauge <= 0.2f) screen.barRed else screen.barBlack
        barToDraw.setSize(screen.maxWidth, screen.maxlHeight)
        barToDraw.setPosition(spaceInitHorizontal, screen.posYGauje)
        barToDraw.setColor(1f, 1f, 1f, 1f)
        barToDraw.draw(batch)

        val baseVisibleWidth = screen.maxWidth * safeGauge
        val maximumStretch = screen.maxWidth * 0.075f
        val stretchWidth = maximumStretch * safeStretch

        val visibleWidth = if (safeGauge >= 1f) {
            screen.maxWidth
        } else {
            (baseVisibleWidth + stretchWidth).coerceAtMost(screen.maxWidth)
        }

        val textureWidth = screen.barColors.texture.width
        val textureHeight = screen.barColors.texture.height
        val regionWidth = (textureWidth.toFloat() * safeGauge).toInt().coerceIn(0, textureWidth)

        if (regionWidth > 0 && visibleWidth > 0.1f) {
            screen.barColors.setRegion(0, 0, regionWidth, textureHeight)
            screen.barColors.setSize(visibleWidth, screen.maxlHeight)
            screen.barColors.setPosition(spaceInitHorizontal, screen.posYGauje)
            screen.barColors.setColor(1f, 1f, 1f, 1f)
            screen.barColors.draw(batch)

            if (barLifeCalculator.isVisibleBarFull) {
                val glowPulse = safeStretch * safeStretch
                val glowAlpha = 0.18f + 0.40f * glowPulse

                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
                screen.barColors.setColor(1f, 1f, 1f, glowAlpha)
                screen.barColors.draw(batch)
                batch.setBlendFunction(previousSrcFunc, previousDstFunc)

                screen.barColors.setColor(1f, 1f, 1f, 1f)
            }
        }

        screen.barFrame.setSize(screen.maxWidth, screen.maxlHeight)
        screen.barFrame.setPosition(spaceInitHorizontal, screen.posYGauje)
        screen.barFrame.setColor(1f, 1f, 1f, 1f)
        screen.barFrame.draw(batch)

        if (safeGauge < 0.98f && safeGauge > 0f) {
            val tipX = spaceInitHorizontal + visibleWidth
            screen.barTip.setSize(tipWidth, tipHeight)
            screen.barTip.setPosition(tipX, tipY)
            screen.barTip.setColor(1f, 1f, 1f, 1f)
            screen.barTip.draw(batch)
        }

        batch.setBlendFunction(previousSrcFunc, previousDstFunc)
    }

    private val lightningExtraHeight = screen.maxlHeight * 1.5f
    private val lightningExtraWidth = screen.maxWidth * 1.2f
    private val lightningX = spaceInitHorizontal - (screen.maxWidth * 0.1f)
    private val lightningY =
        screen.posYGauje - ((lightningExtraHeight - screen.maxlHeight) / 2f)

    private fun drawOverflowLightning(delta: Float) {
        if (!barLifeCalculator.isOverflowFull) {
            lifeLightningTime = 0f
            return
        }

        val frames = screen.lifeLightningFrames
        if (frames.isEmpty()) return

        lifeLightningTime += delta
        val frameIndex =
            (lifeLightningTime / LIFE_LIGHTNING_FRAME_DURATION).toInt() % frames.size

        val frame = frames[frameIndex]
        val previousSrcFunc = batch.blendSrcFunc
        val previousDstFunc = batch.blendDstFunc
        val oldColor = Color(batch.color)

        batch.setColor(1f, 1f, 1f, 1f)
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        batch.draw(
            frame,
            lightningX,
            lightningY,
            lightningExtraWidth,
            lightningExtraHeight
        )
        batch.setBlendFunction(previousSrcFunc, previousDstFunc)
        batch.color = oldColor
    }

    private fun onMineHit(timeCom: Long) {
        mineFlashStartTime = timeCom
        soundPoolSelectSong.play(sound_mine, 1f, 1f, 1, 0, 1f)
    }

    private fun getArrows3x2(arrow: Texture, isMirror: Boolean = false): Array<TextureRegion> {
        val tmp = TextureRegion.split(arrow, arrow.width / 3, arrow.height / 2)
        val textureData = arrow.textureData

        if (!textureData.isPrepared) textureData.prepare()
        val pixmap = textureData.consumePixmap()

        try {
            val frames = arrayOf(
                trimFrame(tmp[0][0], pixmap), trimFrame(tmp[0][1], pixmap), trimFrame(tmp[0][2], pixmap),
                trimFrame(tmp[1][0], pixmap), trimFrame(tmp[1][1], pixmap), trimFrame(tmp[1][2], pixmap)
            )
            frames.forEach { it.flip(isMirror, true) }
            return frames
        } finally {
            if (textureData.disposePixmap()) pixmap.dispose()
        }
    }

    private fun getArrows6x1Flare(arrow: Texture, isMirror: Boolean = false): Array<TextureRegion> {
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

    private fun getArrows6x1(
        arrow: Texture,
        isMirror: Boolean = false,
        metricsColumn: Int? = null
    ): Array<TextureRegion> {
        arrow.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        val tmp = TextureRegion.split(arrow, arrow.width / 6, arrow.height)
        val textureData = arrow.textureData

        if (!textureData.isPrepared) textureData.prepare()
        val pixmap = textureData.consumePixmap()

        try {
            val sourceFrames = arrayOf(
                tmp[0][0], tmp[0][1], tmp[0][2],
                tmp[0][3], tmp[0][4], tmp[0][5]
            )

            if (metricsColumn != null && metricsColumn in 0..4) {
                for (i in sourceFrames.indices) {
                    val metric = calculateHoldMetrics(sourceFrames[i], pixmap)
                    holdMetrics[metricsColumn][i] = if (isMirror) {
                        HoldMetrics(
                            widthRatio = metric.widthRatio,
                            offsetRatio = (1f - metric.offsetRatio - metric.widthRatio).coerceIn(0f, 1f)
                        )
                    } else {
                        metric
                    }
                }
            }

            val frames = Array(6) { i -> trimFrame(sourceFrames[i], pixmap) }
            frames.forEach { it.flip(isMirror, true) }
            return frames
        } finally {
            if (textureData.disposePixmap()) pixmap.dispose()
        }
    }

    private fun calculateHoldMetrics(
        sourceRegion: TextureRegion,
        pixmap: Pixmap,
        alphaThreshold: Int = 1
    ): HoldMetrics {
        val sourceX = sourceRegion.regionX
        val sourceY = sourceRegion.regionY
        val sourceWidth = sourceRegion.regionWidth
        val sourceHeight = sourceRegion.regionHeight

        var trimMinX = sourceWidth
        var trimMinY = sourceHeight
        var trimMaxX = -1
        var trimMaxY = -1

        for (y in 0 until sourceHeight) {
            for (x in 0 until sourceWidth) {
                val alpha = pixmap.getPixel(sourceX + x, sourceY + y) and 0xFF
                if (alpha >= alphaThreshold) {
                    if (x < trimMinX) trimMinX = x
                    if (y < trimMinY) trimMinY = y
                    if (x > trimMaxX) trimMaxX = x
                    if (y > trimMaxY) trimMaxY = y
                }
            }
        }

        if (trimMaxX < trimMinX || trimMaxY < trimMinY) return HoldMetrics(1f, 0f)

        val firstRow = trimMinY
        var lineMinX = sourceWidth
        var lineMaxX = -1

        for (x in trimMinX..trimMaxX) {
            val alpha = pixmap.getPixel(sourceX + x, sourceY + firstRow) and 0xFF
            if (alpha >= alphaThreshold) {
                if (x < lineMinX) lineMinX = x
                if (x > lineMaxX) lineMaxX = x
            }
        }

        if (lineMaxX < lineMinX) return HoldMetrics(1f, 0f)

        val trimmedWidth = trimMaxX - trimMinX + 1
        val lineWidth = lineMaxX - lineMinX + 1

        return HoldMetrics(
            widthRatio = (lineWidth.toFloat() / trimmedWidth).coerceIn(0f, 1f),
            offsetRatio = ((lineMinX - trimMinX).toFloat() / trimmedWidth).coerceIn(0f, 1f)
        )
    }

    private fun trimFrame(
        sourceRegion: TextureRegion,
        pixmap: Pixmap,
        alphaThreshold: Int = 1
    ): TextureRegion {
        val sourceX = sourceRegion.regionX
        val sourceY = sourceRegion.regionY
        val sourceWidth = sourceRegion.regionWidth
        val sourceHeight = sourceRegion.regionHeight

        var minX = sourceWidth
        var minY = sourceHeight
        var maxX = -1
        var maxY = -1

        for (y in 0 until sourceHeight) {
            for (x in 0 until sourceWidth) {
                val alpha = pixmap.getPixel(sourceX + x, sourceY + y) and 0xFF
                if (alpha >= alphaThreshold) {
                    if (x < minX) minX = x
                    if (y < minY) minY = y
                    if (x > maxX) maxX = x
                    if (y > maxY) maxY = y
                }
            }
        }

        if (maxX < minX || maxY < minY) return TextureRegion(sourceRegion, 0, 0, 1, 1)

        val trimmedWidth = maxX - minX + 1
        val trimmedHeight = maxY - minY + 1
        return TextureRegion(sourceRegion, minX, minY, trimmedWidth, trimmedHeight)
    }

    fun disposePlayer() {
        downLeftTap.dispose()
        upLeftTap.dispose()
        centerTap.dispose()
        downLeftBody.dispose()
        upLeftBody.dispose()
        centerBody.dispose()
        downLeftBottom.dispose()
        upLeftBottom.dispose()
        centerBottom.dispose()
        sprFlare.dispose()

        arrMines.forEach { it.texture.dispose() }

        arrArrows.forEach { frameArray ->
            frameArray.forEach { it.texture.dispose() }
        }

        /*
        arrArrowsHead.forEach { frameArray ->
            frameArray.forEach { it.texture.dispose() }
        }
        */

        arrArrowsBody.forEach { frameArray ->
            frameArray.forEach { it.texture.dispose() }
        }

        arrArrowsBottom.forEach { frameArray ->
            frameArray.forEach { it.texture.dispose() }
        }
        sprFlare.dispose()
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
        val alpha = 1f - (t * t) // caída más agresiva

        batch.setColor(1f, 1f, 1f, alpha)
        batch.draw(whiteTex, 0f, 0f, screen.gdxWidth.toFloat(), screen.gdxHeight.toFloat())
        batch.setColor(Color.WHITE)
    }
    fun triggerLuaFlash(duration: Long) {
        luaFlashDuration = duration
        luaFlashStartTime = screen.timeGetTime()
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




    override fun drawTap(note: Parser.Note, column: Int, y: Int) {
        drawNote(column, y, note)
    }

    override fun drawMine(note: Parser.Note, column: Int, y: Int) {
        drawMines(column, y, note)
    }

    override fun drawHold(note: Parser.Note, column: Int, yHead: Int, yTail: Int) {
        drawLongNote(column, yHead, yTail, note)
    }

    override fun onColumnActive(column: Int) {
        if (column !in 0..4) return
        startExpand(column)
        for (physical in inputProcessor.getPhysicalPadsForLogical(column)) {
            drawPressedPad(column, physical)
        }
    }

    override fun onColumnPressed(column: Int) {
        if (column !in 0..4) return
        for (physical in inputProcessor.getPhysicalPadsForLogical(column)) {
            drawPressedPad(column, physical)
        }
    }

    override fun onJudge(
        column: Int,
        judge: Int,
        comboMultiplier: Int,
        isBodyLongNote: Boolean,
        isFromInput: Boolean,
        isMine: Boolean,
        note: Parser.Note?
    ) {
        this.multiplierCombo = comboMultiplier

        applyJudge(
            col = column,
            judge = judge,
            isBodyLongNote = isBodyLongNote,
            isFromInput = isFromInput,
            isMine = isMine,
            note = note
        )
    }

    override fun onMineHit(column: Int, note: Parser.Note) {
        onMineHit(screen.timeGetTime())
    }

}
