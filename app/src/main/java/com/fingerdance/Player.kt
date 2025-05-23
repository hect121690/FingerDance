package com.fingerdance

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import java.lang.Math.abs
import kotlin.experimental.and

private val chkPtnNum = IntArray(5)
private val chkLineNum = IntArray(5)

class Player(private val batch: SpriteBatch, activity: GameScreenActivity) : GameScreenKsf(activity) {

    private val sizeScale = medidaFlechas * 1.2f
    private val topPos = medidaFlechas * 0.9f
    private val posX = medidaFlechas * 0.1f
    private var aBatch = 0
    private var bBatch = 0
    private val xFlare1 = medidaFlechas * 2.1f
    private val xFlare2 = medidaFlechas * 2.15f
    private val xFlare3 = medidaFlechas * 2.1f
    private val xFlare4 = medidaFlechas * 2.05f
    private val xFlare5 = medidaFlechas * 2.05f
    private val animationDuration: Long = 300L
    private var elapsedTimeToExpands = 0L

    private var timeToPresiscion = 360

    companion object {
        val STEPSIZE = medidaFlechas.toInt()
        val MEASURE = height * .35
        val MEASUREVANISH = (height * .5) - (medidaFlechas * 2)

        const val NOTE_NONE: Byte = 0
        const val NOTE_NOTE: Byte = 1
        const val NOTE_LSTART: Byte = 6
        const val NOTE_LNOTE: Byte = 2
        const val NOTE_LEND: Byte = 10
        const val NOTE_LSTART_PRESS: Byte = 22
        const val NOTE_LEND_PRESS: Byte = 26
        const val NOTE_NOTE_MISS: Byte =33
        const val NOTE_LSTART_MISS: Byte = 38
        const val NOTE_LEND_MISS: Byte = 42

        const val NOTE_NOTE_CHK = NOTE_NOTE
        const val NOTE_LONG_CHK = NOTE_LNOTE
        const val NOTE_START_CHK: Byte = 4
        const val NOTE_END_CHK: Byte = 8
        const val NOTE_PRESS_CHK: Byte = 16
        const val NOTE_MISS_CHK: Byte = 32

        private var ZONE_PERFECT: Long = if(playerSong.hj) 20 else 70
        private var ZONE_GREAT: Long = if(playerSong.hj) 40 else 100
        private var ZONE_GOOD: Long = if(playerSong.hj) 80 else 120
        private var ZONE_BAD: Long = if(playerSong.hj) 100 else 150

        const val JUDGE_PERFECT = 0
        const val JUDGE_GREAT = 1
        const val JUDGE_GOOD = 2
        const val JUDGE_BAD = 3
        const val JUDGE_MISS = 4

        const val KEY_NONE = 0
        const val KEY_DOWN = 1
        const val KEY_PRESS = 2
        const val KEY_UP = 3

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

        private lateinit var arrArrows : Array<Array<TextureRegion>>
        private lateinit var arrArrowsBody : Array<Array<TextureRegion>>
        private lateinit var arrArrowsBottom : Array<Array<TextureRegion>>

        private lateinit var sprFlare: Texture

        private lateinit var flareArrowFrame : Array<TextureRegion>

        private val LONGNOTE = Array(5) { LongNotePress() }
    }

    data class PlayerFlare(var startTime: Long = 0)
    data class PlayerJudge(var startTime: Long = 0, var judge: Int = 0)
    enum class SetSpeedType { SET, ADD, SUB }
    data class JudgePos(val x: Int = widthJudges - (widthJudges / 2), val y: Int = Gdx.graphics.height / 2 - heightJudges * 6)

    private var m_fGauge = 0.35f
    var m_fCurBPM  = 0F
    private var arrowFrame = 0
    private var mCurPtnNum = 0
    private var m_iStepWidth = 5
    var curCombo = 0
    var curComboMiss = 0
    private var bBpmFound = false
    private val flare = Array(5) { PlayerFlare() }
    private var m_judge = PlayerJudge()
    private var noEffects = false

    private val widthFlare = medidaFlechas * 5f
    private var yFlare = medidaFlechas - (medidaFlechas * 2f)

    private val inputProcessor = InputProcessor()

    private var speed = playerSong.speed.replace("X", "").toFloat() + 1f

    var baseSpeed = speed
    var speedChangeStartTime = 0L
    var activeSpeedDelta: Pair<Float, Long>? = null

    private var currentTimeToExpands = 0L

    private val judgePos = JudgePos()
    private val x = judgePos.x.toFloat()
    private val y = judgePos.y.toFloat()
    private var digitWidth = medidaFlechas * 0.7f
    private var digitHeight = heightJudges * 1.3f

    private var stepWidth = 5
    private val GaugeInc = if(playerSong.hj) gaugeIncHJ else gaugeIncNormal
    val tipWidth = (medidaFlechas / 4f)
    val tipHeight = medidaFlechas / 1.5f

    init {
        currentTimeToExpands = timeGetTime()
        initCommonInfo()
        setSpeed(SetSpeedType.SET, speed)
        Gdx.input.inputProcessor = inputProcessor

        for(x in 0 until 5){
            LONGNOTE[x].pressed = false
            chkPtnNum[x] = 0
            chkPtnNum[x] = 0
        }
        if(playerSong.ap || playerSong.vanish){
            noEffects = true
        }

    }
    private fun initCommonInfo() {

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

        sprFlare = Texture(Gdx.files.absolute("$ruta/Flare 6x1.png"))
        flareArrowFrame = getArrows6x1(sprFlare)

        arrArrowsBottom = arrayOf(ldBottomArrowFrame, luBottomArrowFrame, ceBottomArrowFrame, ruBottomArrowFrame, rdBottomArrowFrame)

        inputProcessor.resetState()
    }

    private data class LongNotePress(
        var pressed: Boolean = false,
        var time: Long = 0,
        var ptn: Int = 0,
        var line: Int = 0,
    )

    fun render(delta: Long) {
        var time = delta
        val timeCom = timeGetTime()
        val bDrawLong = BooleanArray(10) {false}
        val bDrawLongPress = BooleanArray(10){false}
        val iLongTop = LongArray(10){0}

        var iPtnNowNo = 0
        var iPtnNoGauge: Int

        if(showPadB == 0){
            inputProcessor.render(batch)
        }
        inputProcessor.update()

        arrowFrame = ((timeCom % 1000) / 200).toInt()

        val ptnFirst = ksf.patterns.first()
        val ptnLast = ksf.patterns.last()

        if(time < ptnFirst.timePos + timeToPresiscion) {
            m_fCurBPM  = ptnFirst.fBPM
            iPtnNowNo = 0
        }else if(time > ptnLast.timePos + timeToPresiscion){
            m_fCurBPM  = ptnLast.fBPM
            iPtnNowNo = ksf.patterns.size - 1
        } else {
            bBpmFound = false
            for (iPtnNo in mCurPtnNum until ksf.patterns.size) {
                val pPtnCur = ksf.patterns[iPtnNo]
                if (time >= pPtnCur.timePos + timeToPresiscion && time < pPtnCur.timePos + timeToPresiscion + pPtnCur.timeLen) {
                    var currentPattern = pPtnCur
                    var iPtnNoCopy = iPtnNo
                    while (currentPattern.fBPM == 0F) {
                        iPtnNoCopy--
                        currentPattern = ksf.patterns[iPtnNoCopy]
                    }
                    m_fCurBPM  = currentPattern.fBPM
                    iPtnNowNo = iPtnNoCopy
                    bBpmFound = true
                    break
                }
            }
            if(!bBpmFound){
                for(iPtnNo in 0 until ksf.patterns.size){
                    val pPtnCur = ksf.patterns[iPtnNo]
                    val pPtnNext = ksf.patterns[iPtnNo + 1]
                    if(time >=pPtnCur.timePos + timeToPresiscion && time < pPtnNext.timePos + timeToPresiscion){
                        var currentPattern = pPtnCur
                        var iPtnNoCopy = iPtnNo
                        while (currentPattern.fBPM == 0F){
                            iPtnNoCopy--
                            currentPattern = ksf.patterns[iPtnNoCopy]
                        }
                        m_fCurBPM = currentPattern.fBPM
                        iPtnNowNo = iPtnNoCopy
                        break
                    }
                }
            }
        }

        mCurPtnNum = iPtnNowNo

        val fGapPerStep  = (STEPSIZE * speed)
        var iPtnTop: Long
        var iPtnBottom: Long

        if(ksf.patterns[iPtnNowNo].timeDelay != 0L){
            time = ksf.patterns[iPtnNowNo].timePos + timeToPresiscion
        }

        if (ksf.patterns[iPtnNowNo].iSpeed.first != 0f && activeSpeedDelta == null) {
            speedChangeStartTime = time
            activeSpeedDelta = ksf.patterns[iPtnNowNo].iSpeed
        }else{
            speed = baseSpeed
        }
        if (activeSpeedDelta != null) {
            speed = baseSpeed * (1f + activeSpeedDelta!!.first)
            activeSpeedDelta = null
        }else {
            speed = baseSpeed
        }
        /*
        if (activeSpeedDelta != null) {
            val (valueSpeed, duration) = activeSpeedDelta!!
            val elapsed = (time - speedChangeStartTime).coerceAtLeast(0L)
            val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
            speed = baseSpeed * (1f + valueSpeed * progress)

            if (elapsed >= duration) {
                speed = baseSpeed
                activeSpeedDelta = null

            }
        }
        */

        iPtnTop = adaptValue((((ksf.patterns[iPtnNowNo].timePos + timeToPresiscion) - time) * m_fCurBPM * speed * 0.001f).toLong())

        for (iPtnNo in (iPtnNowNo - 1) downTo 0) {
            if (iPtnTop < -STEPSIZE) break
            val pPtnCur = ksf.patterns[iPtnNo]
            val fPtnTick = pPtnCur.iTick.toFloat()
            val iLineCnt = pPtnCur.vLine.size
            iPtnBottom = iPtnTop
            iPtnTop = iPtnBottom - (iLineCnt * fGapPerStep / fPtnTick).toLong().toInt()
            for (iLineNo in 0 until iLineCnt) {
                val iNoteTop = (iPtnTop + (iLineNo * fGapPerStep / fPtnTick) + STEPSIZE).toInt()
                if (iNoteTop > -STEPSIZE && iNoteTop < gdxHeight) {
                    for (iStepNo in 0 until m_iStepWidth) {
                        val nowstep = pPtnCur.vLine[iLineNo].step[iStepNo]
                        if (nowstep and NOTE_NOTE_CHK != 0.toByte()) {
                            drawNote(iStepNo, iNoteTop)
                        }else if (nowstep and NOTE_LONG_CHK != 0.toByte()) {
                            if (nowstep and NOTE_START_CHK != 0.toByte()) {
                                if (nowstep and NOTE_PRESS_CHK != 0.toByte()) {
                                    bDrawLong[iStepNo] = true
                                    iLongTop[iStepNo] = STEPSIZE.toLong()
                                    bDrawLongPress[iStepNo] = true
                                } else {
                                    bDrawLong[iStepNo] = true
                                    iLongTop[iStepNo] = iNoteTop.toLong()
                                }
                                drawNote(iStepNo, iLongTop[iStepNo].toInt())
                            } else if (nowstep and NOTE_END_CHK != 0.toByte()) {
                                if (nowstep and NOTE_PRESS_CHK != 0.toByte()) {
                                    drawLongNote(iStepNo, STEPSIZE, iNoteTop)
                                    bDrawLongPress[iStepNo] = true
                                } else {
                                    if (bDrawLong[iStepNo]) {
                                        bDrawLong[iStepNo] = false
                                    }
                                    drawLongNote(iStepNo, iLongTop[iStepNo].toInt(), iNoteTop)
                                }
                            } else {
                                if (!bDrawLong[iStepNo]) {
                                    bDrawLong[iStepNo] = true
                                    iLongTop[iStepNo] = -STEPSIZE.toLong()
                                }
                            }
                        }
                    }
                }else if (iNoteTop >= gdxHeight) {
                    break
                }
            }
        }

        iPtnBottom = adaptValue((((ksf.patterns[iPtnNowNo].timePos + timeToPresiscion) - time) * m_fCurBPM * speed * 0.001f).toLong())

        for (iPtnNo in iPtnNowNo until ksf.patterns.size) {
            if (iPtnBottom > gdxHeight) break
            val pPtnCur = ksf.patterns[iPtnNo]
            val fPtnTick = pPtnCur.iTick.toFloat()
            val iLineCnt = pPtnCur.vLine.size
            iPtnTop = iPtnBottom
            iPtnBottom = iPtnTop + (iLineCnt * fGapPerStep / fPtnTick).toLong().toInt()
            for (iLineNo in 0 until iLineCnt) {
                val iNoteTop = (iPtnTop + (iLineNo * fGapPerStep / fPtnTick) + STEPSIZE).toInt()
                if (iNoteTop > -STEPSIZE && iNoteTop < gdxHeight) {
                    for (iStepNo in 0 until m_iStepWidth) {
                        val nowstep = pPtnCur.vLine[iLineNo].step[iStepNo]
                        if (nowstep and NOTE_NOTE_CHK != 0.toByte()) {
                            drawNote(iStepNo, iNoteTop)
                        } else if (nowstep and NOTE_LONG_CHK != 0.toByte()) {
                            if (nowstep and NOTE_START_CHK != 0.toByte()) {
                                if (nowstep and NOTE_PRESS_CHK != 0.toByte()) {
                                    bDrawLong[iStepNo] = true
                                    iLongTop[iStepNo] = STEPSIZE.toLong()
                                    bDrawLongPress[iStepNo] = true
                                } else {
                                    bDrawLong[iStepNo] = true
                                    iLongTop[iStepNo] = iNoteTop.toLong()
                                }
                                drawNote(iStepNo, iLongTop[iStepNo].toInt())
                            } else if (nowstep and NOTE_END_CHK != 0.toByte()) {
                                if (nowstep and NOTE_PRESS_CHK != 0.toByte()) {
                                    drawLongNote(iStepNo, STEPSIZE, iNoteTop)
                                    bDrawLongPress[iStepNo] = true
                                } else {
                                    if (bDrawLong[iStepNo]) {
                                        bDrawLong[iStepNo] = false
                                    }
                                    drawLongNote(iStepNo, iLongTop[iStepNo].toInt(), iNoteTop)
                                }
                            } else {
                                if (!bDrawLong[iStepNo]) {
                                    bDrawLong[iStepNo] = true
                                    iLongTop[iStepNo] = -STEPSIZE.toLong()
                                }
                            }
                        }
                    }
                } else if (iNoteTop >= gdxHeight) {
                    break
                }
            }
        }

        for (iStepNo in 0 until m_iStepWidth) {
            if (bDrawLong[iStepNo]) {
                drawLongNote(iStepNo, iLongTop[iStepNo].toInt(), gdxHeight)
            }
        }

        if(m_fGauge > 1.0f){
            m_fGauge = 1.0f
        }
        if(m_fGauge < -0.5f){
            m_fGauge = 0.0f
        }

        var gaugeFind = (60000 / abs(m_fCurBPM))
        iPtnNoGauge = abs(ksf.patterns[iPtnNowNo].timePos - time).toInt()

        while(iPtnNoGauge >= gaugeFind){
            iPtnNoGauge = (iPtnNoGauge - gaugeFind).toInt()
        }
        if(time < ptnFirst.timePos){
            iPtnNoGauge = (gaugeFind - iPtnNoGauge).toInt()
        }
        gaugeFind = iPtnNoGauge / gaugeFind * 0.1f

        if(m_fGauge == 1.0f){
            gaugeFind *= 3.0f
            gaugeFind += 0.7f
            drawGauge(2.0f + gaugeFind)
        }else{
            gaugeFind += m_fGauge
            if (gaugeFind > 1.0f){
                gaugeFind = 1.0f
            }
            drawGauge(gaugeFind)
        }

        for (iStepNo in 0 until m_iStepWidth) {
            if (bDrawLongPress[iStepNo]) {
                drawLongNotePress(iStepNo)
            }

            if (flare[iStepNo].startTime == 0L) {
                continue
            }
            iLongTop[iStepNo] = ((timeCom - flare[iStepNo].startTime) shr 6)
            if (iLongTop[iStepNo] >= 6) {
                flare[iStepNo].startTime = 0
                continue
            }
            drawFlare(iStepNo, iLongTop[iStepNo].toInt())
        }
        if(m_judge.startTime == 0L){
            return
        }
        if(m_judge.startTime + 2500 < timeCom){
            m_judge.startTime = 0
            return
        }
        drawJudge(timeCom - m_judge.startTime)
    }

    fun updateStepData(time: Long) {
        var iptn: Int
        val iptnc = ksf.patterns.size
        var line_num_s: Int
        var line_num_c: Int
        var judge_time: Long
        var line_mpos: Long
        var line_num : Int
        var judge: Int
        val key = IntArray(5)

        var ptn_now : KsfProccess.Pattern
        val keyBoard = inputProcessor.getKeyBoard

        for(x in 0 until m_iStepWidth){
            key[x] = keyBoard[x]
        }

        for (x in 0 until stepWidth) {
            if (key[x] == KEY_DOWN) {
                showExpand(x)
                for (i in 0 until iptnc) {
                    ptn_now = ksf.patterns[i]
                    val timepos = ptn_now.timePos + timeToPresiscion
                    if (time < timepos - ZONE_BAD) {
                        break
                    }
                    if (time > timepos + ptn_now.timeLen + ZONE_BAD){
                        continue
                    }
                    if (ptn_now.fBPM == 0F) {
                        continue
                    }

                    line_num_s = ((time - timepos - ZONE_BAD) / 60000f * ptn_now.fBPM * ptn_now.iTick).toInt()
                    line_num_c = ptn_now.vLine.size
                    if (line_num_s < 0) {
                        line_num_s = 0
                    }

                    for (c in line_num_s until line_num_c) {
                        line_mpos = (60000 / ptn_now.fBPM * c / ptn_now.iTick).toLong() + timepos
                        judge_time = abs(line_mpos - time)
                        if (judge_time < ZONE_BAD) {
                            val nnote = ptn_now.vLine[c].step[x]
                            if (nnote and NOTE_MISS_CHK != 0.toByte()){
                                continue
                            }
                            if(nnote == NOTE_NOTE) {
                                ptn_now.vLine[c].step[x] = NOTE_NONE
                                ksf.patterns[i].vLine[c].step[x] = NOTE_NONE
                                judge = getJudgement(judge_time)
                                m_fGauge += GaugeInc[judge]
                                newJudge(judge, timeGetTime())
                                newFlare(x, timeGetTime())
                                getJudge(judge)

                                key[x] = KEY_NONE

                            }else if (nnote == NOTE_LSTART || nnote == NOTE_LNOTE) {
                                ksf.patterns[i].vLine[c].step[x] = NOTE_NONE
                                var ptnToChange: KsfProccess.Pattern?
                                var lineToChange: Int

                                if (c + 1 == line_num_c) {
                                    var next_long_ptn = i + 1
                                    while (ksf.patterns[next_long_ptn].vLine.isEmpty()) {
                                        next_long_ptn++
                                    }
                                    ptnToChange = ksf.patterns[next_long_ptn]
                                    lineToChange = 0
                                    LONGNOTE[x].ptn = next_long_ptn
                                    LONGNOTE[x].line = 0
                                } else {
                                    ptnToChange = ptn_now
                                    lineToChange = c + 1
                                    LONGNOTE[x].ptn = i
                                    LONGNOTE[x].line = c + 1
                                }

                                val stepList = ptnToChange.vLine[lineToChange].step
                                when (stepList[x]) {
                                    NOTE_LNOTE -> {
                                        stepList[x] = NOTE_LSTART_PRESS
                                        ksf.patterns[LONGNOTE[x].ptn].vLine[LONGNOTE[x].line].step[x] = NOTE_LSTART_PRESS
                                    }
                                    NOTE_LEND -> {
                                        stepList[x] = NOTE_LEND_PRESS
                                        ksf.patterns[LONGNOTE[x].ptn].vLine[LONGNOTE[x].line].step[x] = NOTE_LEND_PRESS
                                    }
                                }

                                LONGNOTE[x].pressed = true
                                LONGNOTE[x].time = judge_time
                                key[x] = KEY_NONE

                                judge_time = LONGNOTE[x].time shr 1
                                judge = getJudgement(judge_time)
                                getJudge(judge)
                                m_fGauge += GaugeInc[judge]
                                newJudge(judge, timeGetTime())
                                newFlare(x, timeGetTime())
                            }
                        } else if (line_mpos > time) {
                            key[x] = KEY_NONE
                        }
                        if(key[x] == KEY_NONE){
                            break
                        }
                    }
                    if(key[x] == KEY_NONE){
                        break
                    }
                }
            }else if (key[x] == KEY_PRESS ) {
                showExpand(x)

                /*
                if (!LONGNOTE[x].pressed) {
                    for (i in 0 until iptnc) {
                        val ptn = ksf.patterns[i]
                        val timepos = ptn.timePos + timeToPresiscion
                        if (time > timepos + ptn.timeLen + ZONE_BAD) continue
                        if (ptn.fBPM == 0f) continue

                        val lineCount = ptn.vLine.size
                        for (c in 0 until lineCount) {
                            val lineTime = (60000 / ptn.fBPM * c / ptn.iTick).toLong() + timepos
                            val judgeTime = abs(lineTime - time)
                            if (judgeTime < ZONE_BAD) {
                                val note = ptn.vLine[c].step[x]
                                if (note == NOTE_LSTART) {
                                    ptn.vLine[c].step[x] = NOTE_NONE
                                    var nextLine = c + 1
                                    var nextPtn = i
                                    if (nextLine >= lineCount) {
                                        nextPtn = i + 1
                                        while (ksf.patterns[nextPtn].vLine.isEmpty()) {
                                            nextPtn++
                                        }
                                        nextLine = 0
                                    }

                                    val stepList = ksf.patterns[nextPtn].vLine[nextLine].step
                                    when (stepList[x]) {
                                        NOTE_LNOTE -> {
                                            stepList[x] = NOTE_LSTART_PRESS
                                        }
                                        NOTE_LEND -> {
                                            stepList[x] = NOTE_LEND_PRESS
                                        }
                                    }

                                    LONGNOTE[x].ptn = nextPtn
                                    LONGNOTE[x].line = nextLine
                                    LONGNOTE[x].pressed = true
                                    LONGNOTE[x].time = judgeTime
                                    newFlare(x, timeGetTime())
                                    getJudge(0)
                                    newJudge(0, timeGetTime())
                                    m_fGauge += GaugeInc[5]
                                }
                            }
                        }
                    }
                }
                */

                if (LONGNOTE[x].pressed) {
                    line_num = LONGNOTE[x].line
                    iptn = LONGNOTE[x].ptn
                    ptn_now = ksf.patterns[iptn]
                    line_mpos = (60000 / ptn_now.fBPM * line_num / ptn_now.iTick).toLong() + ptn_now.timePos + timeToPresiscion
                    if (line_mpos <= time) {
                        ptn_now.vLine[line_num].step[x] = NOTE_NONE
                        ksf.patterns[iptn].vLine[line_num].step[x] = NOTE_NONE
                        if (line_num + 1 == ptn_now.vLine.size) {
                            var nextLongPtn = LONGNOTE[x].ptn + 1
                            while (ksf.patterns[nextLongPtn].vLine.isEmpty()) {
                                nextLongPtn++
                            }
                            LONGNOTE[x].ptn = nextLongPtn
                            LONGNOTE[x].line = 0
                        } else {
                            LONGNOTE[x].line++
                        }
                        line_num = LONGNOTE[x].line
                        iptn = LONGNOTE[x].ptn
                        ptn_now = ksf.patterns[iptn]
                        when (ptn_now.vLine[line_num].step[x]) {
                            NOTE_LEND -> {
                                ptn_now.vLine[line_num].step[x] = NOTE_LEND_PRESS
                                ksf.patterns[LONGNOTE[x].ptn].vLine[LONGNOTE[x].line].step[x] = NOTE_LEND_PRESS
                            }
                            NOTE_LNOTE, NOTE_LSTART_PRESS -> {
                                getJudge(0)
                                newJudge(0, timeGetTime())
                                m_fGauge += GaugeInc[5]
                                ptn_now.vLine[line_num].step[x] = NOTE_LSTART_PRESS
                                ksf.patterns[LONGNOTE[x].ptn].vLine[LONGNOTE[x].line].step[x] = NOTE_LSTART_PRESS
                            }
                            else -> {
                                val judgeTime = LONGNOTE[x].time shr 1
                                judge = getJudgement(judgeTime)
                                getJudge(judge)
                                m_fGauge += GaugeInc[judge]
                                newJudge(judge, timeGetTime())
                                newFlare(x, timeGetTime())
                                LONGNOTE[x].pressed = false
                            }
                        }
                    }
                }
            }else if (key[x] == KEY_UP){
                if (LONGNOTE[x].pressed){
                    LONGNOTE[x].pressed = false
                    judge_time = getLongNoteEndTime(x) - time
                    if (judge_time < ZONE_BAD){
                        judge_time = abs(judge_time)
                        judge_time += LONGNOTE[x].time
                        judge_time = judge_time shr 1
                        judge = getJudgement(judge_time)
                        getJudge(judge)
                        m_fGauge += GaugeInc[judge]
                        newJudge(judge, timeGetTime())
                        newFlare(x, timeGetTime())
                        clearLongNote(x)
                    }else{
                        line_num = LONGNOTE[x].line
                        iptn = LONGNOTE[x].ptn
                        ptn_now = ksf.patterns[iptn]
                        if(ptn_now.vLine[line_num].step[x] == NOTE_LEND_PRESS){
                            ptn_now.vLine[line_num].step[x] = NOTE_LEND_MISS
                            ksf.patterns[iptn].vLine[line_num].step[x] = NOTE_LEND_MISS
                        }else{
                            ptn_now.vLine[line_num].step[x] = NOTE_LSTART_MISS
                            ksf.patterns[iptn].vLine[line_num].step[x] = NOTE_LSTART_MISS
                        }
                        getJudge(JUDGE_MISS)
                        m_fGauge += GaugeInc[JUDGE_MISS]
                        newJudge(JUDGE_MISS, timeGetTime())
                    }
                }
            }else {
                key[x] = KEY_DOWN

                for (i in chkPtnNum[x] until iptnc) {
                    ptn_now = ksf.patterns[i]
                    if (time < ptn_now.timePos + timeToPresiscion + ZONE_BAD) {
                        break
                    }
                    line_num_c = ptn_now.vLine.size
                    val lineStart = if (i == chkPtnNum[x]) chkLineNum[x] else 0
                    for (c in lineStart until line_num_c) {
                        line_mpos = (60000 / ptn_now.fBPM * c / ptn_now.iTick).toLong() + ptn_now.timePos + timeToPresiscion
                        line_mpos += ZONE_BAD
                        if (line_mpos < time) {
                            val nnote = ptn_now.vLine[c].step[x]
                            if (nnote and NOTE_MISS_CHK != 0.toByte()) {
                                continue
                            }
                            if (nnote and NOTE_PRESS_CHK != 0.toByte()) {
                                continue
                            }
                            when(nnote){
                                NOTE_NOTE -> {
                                    ptn_now.vLine[c].step[x] = NOTE_NOTE_MISS
                                    ksf.patterns[i].vLine[c].step[x] = NOTE_NOTE_MISS
                                    getJudge(JUDGE_MISS)
                                    m_fGauge += GaugeInc[JUDGE_MISS]
                                    newJudge(JUDGE_MISS, timeGetTime())
                                }
                                NOTE_LSTART -> {
                                    ptn_now.vLine[c].step[x] = NOTE_LSTART_MISS
                                    ksf.patterns[i].vLine[c].step[x] = NOTE_LSTART_MISS
                                    getJudge(JUDGE_MISS)
                                    m_fGauge += GaugeInc[JUDGE_MISS]
                                    newJudge(JUDGE_MISS, timeGetTime())
                                }
                                NOTE_LEND -> {
                                    ptn_now.vLine[c].step[x] = NOTE_LEND_MISS
                                    ksf.patterns[i].vLine[c].step[x] = NOTE_LEND_MISS
                                }
                            }
                            chkPtnNum[x] = i
                            chkLineNum[x] = c
                        }else{
                            key[x] = KEY_NONE
                            break
                        }
                    }
                    if (key[x] == KEY_NONE) {
                        break
                    }
                }
            }
        }

        if(m_fGauge > 1.0f){
            m_fGauge = 1.0f
        }else if(m_fGauge < -0.5f){
            if(!isOnline) {
                a.breakDance()
            }
        }
    }

    private fun getLongNoteEndTime(x: Int): Long {
        val ln = LONGNOTE[x]
        for (iptn in ln.ptn until ksf.patterns.size) {
            val ptn_now = ksf.patterns[iptn]
            val lineNumC = ptn_now.vLine.size
            val lineStart = if (iptn == ln.ptn) ln.line else 0

            for (line_num in lineStart until lineNumC) {
                val nnote = ptn_now.vLine[line_num].step[x]
                if (nnote == NOTE_LEND || nnote == NOTE_LEND_PRESS) {
                    val lineMpos =(60000 / ptn_now.fBPM * line_num / ptn_now.iTick).toLong() + ptn_now.timePos + timeToPresiscion
                    return lineMpos
                }
            }
        }
        return 0
    }

    private fun showExpand(position: Int) {
        currentTimeToExpands = timeGetTime()
        batch.setColor(1f, 1f, 1f, 0.7f)
        when (position) {
            0 -> {
                batch.draw(recept0Frames[2], medidaFlechas - posX, topPos, sizeScale, sizeScale)
                if(showPadB == 2){
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(arrPadsC[position][arrowFrame], padPositionsC[position].x, padPositionsC[position].y, padPositionsC[position].size, padPositionsC[position].size)
                }
                if(showPadB == 3){
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(arrayPad4[position], padPositions[0][0], padPositions[0][1], widthBtns, heightBtns)
                }
            }
            1 -> {
                batch.draw(recept1Frames[2], (medidaFlechas * 2) - posX, topPos, sizeScale, sizeScale)
                if(showPadB == 2){
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(arrPadsC[position][arrowFrame], padPositionsC[position].x, padPositionsC[position].y, padPositionsC[position].size, padPositionsC[position].size)
                }
                if(showPadB == 3){
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(arrayPad4[position], padPositions[1][0], padPositions[1][1], widthBtns, heightBtns)
                }
            }
            2 -> {
                batch.draw(recept2Frames[2], (medidaFlechas * 3) - posX, topPos, sizeScale, sizeScale)
                if(showPadB == 2){
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(arrPadsC[position][arrowFrame], padPositionsC[position].x, padPositionsC[position].y, padPositionsC[position].size, padPositionsC[position].size)
                }
                if(showPadB == 3){
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(arrayPad4[position], padPositions[2][0], padPositions[2][1], widthBtns, heightBtns)
                }
            }
            3 -> {
                batch.draw(recept3Frames[2], (medidaFlechas * 4) - posX, topPos, sizeScale, sizeScale)
                if(showPadB == 2){
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(arrPadsC[position][arrowFrame], padPositionsC[position].x, padPositionsC[position].y, padPositionsC[position].size, padPositionsC[position].size)
                }
                if(showPadB == 3){
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(arrayPad4[position], padPositions[3][0], padPositions[3][1], widthBtns, heightBtns)
                }
            }
            4 -> {
                batch.draw(recept4Frames[2], (medidaFlechas * 5) - posX, topPos, sizeScale, sizeScale)
                if(showPadB == 2){
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(arrPadsC[position][arrowFrame], padPositionsC[position].x, padPositionsC[position].y, padPositionsC[position].size, padPositionsC[position].size)
                }
                if(showPadB == 3){
                    batch.setColor(1f, 1f, 1f, 1f)
                    batch.draw(arrayPad4[position], padPositions[4][0], padPositions[4][1], widthBtns, heightBtns)
                }
            }
        }
        batch.setColor(1f, 1f, 1f, 1f)
    }

    private fun clearLongNote(x: Int) {
        val ln = LONGNOTE[x]

        for (iptn in ln.ptn until ksf.patterns.size) {
            val ptnNow = ksf.patterns[iptn]
            val lineNumC = ptnNow.vLine.size
            val lineStart = if (iptn == ln.ptn) ln.line else 0

            for (line_num in lineStart until lineNumC) {
                val nnote = ptnNow.vLine[line_num].step[x]
                when (nnote) {
                    NOTE_LEND, NOTE_LEND_PRESS -> {
                        ptnNow.vLine[line_num].step[x] = NOTE_NONE
                        ksf.patterns[iptn].vLine[line_num].step[x] = NOTE_NONE
                        return
                    }
                    NOTE_LNOTE, NOTE_LSTART_PRESS -> {
                        ptnNow.vLine[line_num].step[x] = NOTE_NONE
                        ksf.patterns[iptn].vLine[line_num].step[x] = NOTE_NONE
                    }
                }
            }
        }
    }

    private fun drawGauge(gauge: Float) {
        //val clampedGauge = gauge.coerceIn(0f, 1f)
        val previousSrcFunc = batch.blendSrcFunc
        val previousDstFunc = batch.blendDstFunc

        val barToDraw = if (gauge <= 0.1f) barRed else barBlack
        barToDraw.setSize(maxWidth, maxlHeight)
        barToDraw.setPosition(medidaFlechas, 0f)
        barToDraw.draw(batch)

        val visibleWidth = maxWidth * gauge
        val regionWidth = (barColors.texture.width * gauge).toInt()

        if (regionWidth > 0.1 && visibleWidth > 0.1f) {
            barColors.setRegion(0, 0, regionWidth, barColors.texture.height)
            barColors.setSize(visibleWidth, maxlHeight)
            barColors.setPosition(medidaFlechas, 0f)
            barColors.draw(batch)
        }

        if (gauge >= 1.0f) {
            val currentTime = (timeGetTime() / 100L) % 2 == 0L

            if (currentTime) {
                val time = 60000 / m_fCurBPM //(System.currentTimeMillis() % 200L) / 200f
                val shine = 1f + 0.5f * Math.sin(time * Math.PI).toFloat()
                batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)

                barColors.setColor(shine, shine, shine, 1f)
                barColors.draw(batch)
                batch.setBlendFunction(previousSrcFunc, previousDstFunc )
            } else {
                barColors.setColor(1f, 1f, 1f, 1f)
                barColors.draw(batch)
            }
        }else{
            barColors.setColor(1f, 1f, 1f, 1f)
        }
        barFrame.setSize(maxWidth, maxlHeight)
        barFrame.setPosition(medidaFlechas, 0f)
        barFrame.draw(batch)

        val tipX: Float
        val tipY = 0f - (medidaFlechas * 0.05f)
        if(gauge <= 0.95f && gauge > 0f){
            tipX = visibleWidth + medidaFlechas
            barTip.setSize(tipWidth, tipHeight)
            barTip.setPosition(tipX, tipY)
            barTip.draw(batch)
        }

    }

    private fun getJudge(judgeType: Int) {
        when (judgeType) {
            0 -> {
                resultSong.perfect++
                curCombo++
                curComboMiss = 0
            }
            1 -> {
                resultSong.great++
                curCombo++
                curComboMiss = 0
            }
            2 -> {
                resultSong.good++
                curComboMiss = 0
            }
            3 -> {
                curCombo = 0
                curComboMiss = 0
                resultSong.bad++
            }
            4 -> {
                curCombo = 0
                curComboMiss++
                //resultSong.miss++
            }
        }

        if (resultSong.maxCombo < curCombo) {
            resultSong.maxCombo = curCombo
        }
    }

    private fun newJudge(judgeType: Int, time: Long){
        m_judge.judge = judgeType
        m_judge.startTime = time
    }

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

        val judgeSprite = Sprite(imgsJudge[m_judge.judge])
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

        // DIBUJAR COMBO (ACIERTO)
        if (curCombo >= 4 || curComboMiss >= 4) {
            val isMiss = curComboMiss >= 4
            val comboSprite = if (isMiss) Sprite(imgsTypeCombo[1]) else Sprite(imgsTypeCombo[0])
            val count = if (isMiss) curComboMiss else curCombo
            val numberList = if (isMiss) listNumbersMiss else listNumbers

            comboSprite.setSize(comboWidth, comboHeight)
            comboSprite.setOriginCenter()
            comboSprite.setCenter(comboX + comboWidth / 2f, comboY + comboHeight / 2f)
            comboSprite.setColor(1f, 1f, 1f, alpha)
            comboSprite.draw(batch)

            val numStr = if (count < 100) count.toString().padStart(3, '0') else count.toString()

            val totalWidth = numStr.length * digitW
            var startX = widthJudges - (totalWidth / 2)
            val digitY = comboY + comboHeight - 10f

            val digitSprite = Sprite()
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

    private fun getJudgement(judgeTime: Long): Int {
        return if(judgeTime <= ZONE_PERFECT){
            JUDGE_PERFECT
        }else if(judgeTime <= ZONE_GREAT){
            JUDGE_GREAT
        }else if(judgeTime <= ZONE_GOOD){
            JUDGE_GOOD
        }else {
            JUDGE_BAD
        }
    }

    private fun adaptValue(valorOriginal: Long): Long {
        return (valorOriginal * STEPSIZE) / 60
    }

    private fun setSpeed(type: SetSpeedType, spd: Float) {
        when (type) {
            SetSpeedType.SET -> speed = spd
            SetSpeedType.ADD -> speed += spd
            SetSpeedType.SUB -> speed -= spd
        }
        speed = speed.coerceIn(0.5f, 8f)
    }

    fun makeRandom() {
        ksf.makeRandom()
    }

    private fun newFlare(x: Int, time: Long) {
        flare[x].startTime = time
    }

    private fun drawNote(x: Int, y: Int) {
        val left = medidaFlechas * (x + 1)

        if(!noEffects){
            batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
        }else{
            if(playerSong.vanish){
                if(y > MEASUREVANISH){
                    batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                }
            }
            if(playerSong.ap){
                if(y < MEASURE){
                    batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                }
            }
        }

    }

    private fun drawLongNote(x: Int, y: Int, y2: Int) {
        val left = medidaFlechas * (x + 1)
        val posY = minOf(y, y2) + (medidaFlechas / 2f)
        var height = y2 - y - (medidaFlechas / 2f)

        if(!noEffects){
            batch.draw(arrArrowsBody[x][arrowFrame], left, posY, medidaFlechas, height)
            batch.draw(arrArrowsBottom[x][arrowFrame],left, y2.toFloat(), medidaFlechas, medidaFlechas)
            batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
        }else{
            if(playerSong.vanish){
                if(posY > MEASUREVANISH){
                    batch.draw(arrArrowsBody[x][arrowFrame], left, posY, medidaFlechas, height)
                    batch.draw(arrArrowsBottom[x][arrowFrame],left, y2.toFloat(), medidaFlechas, medidaFlechas)
                    batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                }
            }
            if(playerSong.ap){
                if(posY < MEASURE){
                    if(posY + height > MEASURE){
                        height = (MEASURE - posY).toFloat()
                    }
                    batch.draw(arrArrowsBody[x][arrowFrame], left, posY, medidaFlechas, height)
                    if(y2 < MEASURE){
                        batch.draw(arrArrowsBottom[x][arrowFrame],left, y2.toFloat(), medidaFlechas, medidaFlechas)
                    }
                    batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                }
            }
        }
    }

    private fun drawLongNotePress(x: Int) {
        val frame = (timeGetTime() % 299 * 6 / 300).toInt()
        aBatch = batch.blendSrcFunc
        bBatch = batch.blendDstFunc
        var left = 0f
        when(x){
            0->{left = medidaFlechas * (x + 1) - xFlare1}
            1->{left = medidaFlechas * (x + 1) - xFlare2}
            2->{left = medidaFlechas * (x + 1) - xFlare3}
            3->{left = medidaFlechas * (x + 1) - xFlare4}
            4->{left = medidaFlechas * (x + 1) - xFlare5}
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        batch.draw(flareArrowFrame[frame], left, yFlare, widthFlare, widthFlare)
        batch.setBlendFunction(aBatch, bBatch)

        batch.color.a = 1f
        batch.setBlendFunction(aBatch, bBatch)

    }

    private fun drawFlare(x: Int, frame: Int) {
        var left = 0f
        when(x){
            0->{left = medidaFlechas * (x + 1) - xFlare1}
            1->{left = medidaFlechas * (x + 1) - xFlare2}
            2->{left = medidaFlechas * (x + 1) - xFlare3}
            3->{left = medidaFlechas * (x + 1) - xFlare4}
            4->{left = medidaFlechas * (x + 1) - xFlare5}
        }
        val flareSprite = flareSprites[frame]
        flareSprite.setBounds(left, yFlare, widthFlare, widthFlare)
        aBatch = batch.blendSrcFunc
        bBatch = batch.blendDstFunc
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        flareSprite.draw(batch)
        batch.setBlendFunction(aBatch, bBatch)

        elapsedTimeToExpands = (timeGetTime() - currentTimeToExpands) % animationDuration

        val (alpha, zoom) = calculateAlphaAndZoom(elapsedTimeToExpands)

        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE)
        batch.color.a = alpha // Establecer transparencia (alpha)

        batch.draw(
            arrArrows[x][arrowFrame],
            (medidaFlechas * (x + 1)) - ((medidaFlechas * zoom) - medidaFlechas) / 2,
            STEPSIZE.toFloat() - ((medidaFlechas * zoom) - medidaFlechas) / 2,
            medidaFlechas * zoom,
            medidaFlechas * zoom
        )

        batch.color.a = 1f
        batch.setBlendFunction(aBatch, bBatch)

    }

    private val flareSprites = Array(flareArrowFrame.size) { i ->
        Sprite(flareArrowFrame[i]).apply { setAlpha(1f) }
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

    private fun getArrows3x2(arrow: Texture, isMirror: Boolean = false) : Array<TextureRegion> {
        val tmp = TextureRegion.split(arrow, arrow.width / 3, arrow.height / 2)

        return if(!isMirror) {
            val frames = arrayOf(
                tmp[0][0], tmp[0][1], tmp[0][2],
                tmp[1][0], tmp[1][1], tmp[1][2]
            )
            frames[0].flip(false, true)
            frames[1].flip(false, true)
            frames[2].flip(false, true)
            frames[3].flip(false, true)
            frames[4].flip(false, true)
            frames[5].flip(false, true)
            frames
        }else{
            val frames = arrayOf(
                tmp[0][0], tmp[0][1], tmp[0][2],
                tmp[1][0], tmp[1][1], tmp[1][2]
            )
            frames[0].flip(true, true)
            frames[1].flip(true, true)
            frames[2].flip(true, true)
            frames[3].flip(true, true)
            frames[4].flip(true, true)
            frames[5].flip(true, true)
            frames
        }
    }

    private fun getArrows6x1(arrow: Texture, isMirror: Boolean = false) : Array<TextureRegion> {
        arrow.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        val tmp = TextureRegion.split(arrow, arrow.width / 6, arrow.height)

        return if(!isMirror) {
            val frames = arrayOf(
                tmp[0][0], tmp[0][1], tmp[0][2],
                tmp[0][3], tmp[0][4], tmp[0][5]
            )
            frames[0].flip(false, true)
            frames[1].flip(false, true)
            frames[2].flip(false, true)
            frames[3].flip(false, true)
            frames[4].flip(false, true)
            frames[5].flip(false, true)
            frames
        }else{
            val frames = arrayOf(
                tmp[0][0], tmp[0][1], tmp[0][2],
                tmp[0][3], tmp[0][4], tmp[0][5]
            )
            frames[0].flip(true, true)
            frames[1].flip(true, true)
            frames[2].flip(true, true)
            frames[3].flip(true, true)
            frames[4].flip(true, true)
            frames[5].flip(true, true)
            frames
        }
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

        curCombo = 0
        inputProcessor.dispose()

    }
}


