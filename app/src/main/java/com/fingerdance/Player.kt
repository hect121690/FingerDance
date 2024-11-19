package com.fingerdance

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.Sprite
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import java.lang.Math.abs
import kotlin.experimental.and

private val chkPtnNum = IntArray(10)
private val chkLineNum = IntArray(10)

class Player(private val batch: SpriteBatch, activity: GameScreenActivity) : GameScreenKsf(activity) {
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

        private var ZONE_PERFECT: Long = if(playerSong.hj) 20 else 50
        private var ZONE_GREAT: Long = if(playerSong.hj) 50 else 80
        private var ZONE_GOOD: Long = if(playerSong.hj) 80 else 100
        private var ZONE_BAD: Long = if(playerSong.hj) 100 else 130

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

        private val LONGNOTE = Array(10) { LongNotePress() }
    }

    data class PlayerFlare(var startTime: Long = 0)
    //data class PlayerJudge(var startTime: Long = 0, var judge: Int = 0)
    /*data class PlayerScore(
        var PERFECT: Int = 0,
        var GREAT: Int = 0,
        var GOOD: Int = 0,
        var BAD: Int = 0,
        var MISS: Int = 0,
        var MAXCOMBO: Int = 0
    )*/

    enum class SetSpeedType { SET, ADD, SUB }

    var gauge = 0f
    private var m_fCurBPM  = 0.0
    private var arrowFrame = 0
    private var iCurPtnNum = 0
    //private var iLineNo = 0
    private var m_iStepWidth = 5
    var curCombo = 0
    private var bBpmFound = false
    val m_bBtnPress = BooleanArray(10)
    //private var judge = PlayerJudge()
    private val flare = Array(10) { PlayerFlare() }
    //private var score = PlayerScore()
    private var noEffects = false

    private val widthFlare = medidaFlechas * 5f
    private var yFlare = medidaFlechas - (medidaFlechas * 2f)

    private val inputProcessor = InputProcessor()

    private var speed = playerSong.speed.replace("X", "").toFloat()

    init {
        initCommonInfo()
        setSpeed(SetSpeedType.SET, speed)
        Gdx.input.inputProcessor = inputProcessor

        for(x in 0 until 10){
            LONGNOTE[x].pressed = false
            chkPtnNum[x] = 0
            chkPtnNum[x] = 0
        }
        if(playerSong.ap || playerSong.vanish){
            noEffects = true
        }

    }
    fun initCommonInfo() {

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

        sprFlare = Texture(Gdx.files.absolute("$ruta/Flare 6x1.png"))

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

        flareArrowFrame = getArrows6x1(sprFlare)

        arrArrowsBottom = arrayOf(ldBottomArrowFrame, luBottomArrowFrame, ceBottomArrowFrame, ruBottomArrowFrame, rdBottomArrowFrame)

    }
    val timeToBpm = 360
    val timeToCalculate = 360
    var timeToPresiscion = 360

    fun render(delta: Long) {
        var time = delta
        val timeCom = System.currentTimeMillis()
        val bDrawLong = BooleanArray(10) {false}
        val bDrawLongPress = BooleanArray(10){false}
        val iLongTop = LongArray(10){0}

        var iPtnNowNo = 0

        inputProcessor.render(batch)
        inputProcessor.update()

        arrowFrame = ((timeCom % 1000) / 200).toInt()

        val ptnFirst = ksf.patterns.first()
        val ptnLast = ksf.patterns.last()

        if(time < ptnFirst.timePos + timeToBpm) {
            m_fCurBPM  = ptnFirst.fBPM
            iPtnNowNo = 0
        }else if(time > ptnLast.timePos + timeToBpm){
            m_fCurBPM  = ptnLast.fBPM
            iPtnNowNo = ksf.patterns.size - 1
        } else {
            bBpmFound = false
            for (iPtnNo in iCurPtnNum until ksf.patterns.size) {
                val pPtnCur = ksf.patterns[iPtnNo]
                if (time >= pPtnCur.timePos + timeToBpm && time < pPtnCur.timePos + timeToBpm + pPtnCur.timeLen) {
                    var currentPattern = pPtnCur
                    var iPtnNoCopy = iPtnNo

                    while (currentPattern.fBPM == 0.0) {
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
                    if(time >=pPtnCur.timePos + timeToBpm && time < pPtnNext.timePos + timeToBpm){
                        var currentPattern = pPtnCur
                        var iPtnNoCopy = iPtnNo
                        while (currentPattern.fBPM == 0.0){
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
        iCurPtnNum = iPtnNowNo

        val fGapPerStep  = (STEPSIZE * speed)
        var iPtnTop: Long
        var iPtnBottom: Long

        if(ksf.patterns[iPtnNowNo].timeDelay != 0L){
            time = ksf.patterns[iPtnNowNo].timePos + timeToBpm
        }
        iPtnTop = adaptValue(((((ksf.patterns[iPtnNowNo].timePos + timeToCalculate) - time).toFloat()) * m_fCurBPM * speed * 0.001f).toLong())

        for(iPtnNo in (iPtnNowNo - 1) downTo 0){
            if(iPtnTop < -STEPSIZE)
                break
            val pPtnCur = ksf.patterns[iPtnNo]
            val fPtnTick = pPtnCur.iTick.toFloat()
            val iLineCnt = pPtnCur.vLine.size
            iPtnBottom = iPtnTop
            iPtnTop = iPtnBottom - ((iLineCnt * fGapPerStep / fPtnTick)).toLong()

            for(iLineNo in 0 until iLineCnt){
                val iNoteTop = (iPtnTop + (iLineNo * fGapPerStep / fPtnTick) + STEPSIZE).toInt()
                if(iNoteTop > -STEPSIZE && iNoteTop < Gdx.graphics.height){
                    for(iStepNo in 0 until m_iStepWidth){
                        val nowStep = pPtnCur.vLine[iLineNo].step[iStepNo]
                        if (nowStep and NOTE_NOTE_CHK != 0.toByte()){
                            drawNote(iStepNo, iNoteTop)
                        }else if (nowStep and NOTE_LONG_CHK != 0.toByte()) {
                            if (nowStep and NOTE_START_CHK != 0.toByte()) {
                                if (nowStep and NOTE_PRESS_CHK != 0.toByte()) {
                                    bDrawLong[iStepNo] = true
                                    iLongTop[iStepNo] = STEPSIZE.toLong()
                                    bDrawLongPress[iStepNo] = true
                                } else {
                                    bDrawLong[iStepNo] = true
                                    iLongTop[iStepNo] = iNoteTop.toLong()
                                }
                                //drawNote(iStepNo, iLongTop[iStepNo].toInt())
                            } else if (nowStep and NOTE_END_CHK != 0.toByte()) {
                                if (nowStep and NOTE_PRESS_CHK != 0.toByte()) {
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
                }else if(iNoteTop >= height){
                    break
                }

            }
        }

        iPtnBottom = adaptValue((((ksf.patterns[iPtnNowNo].timePos + timeToCalculate) - time) * m_fCurBPM * speed * 0.001f).toLong())
        for (iPtnNo in iPtnNowNo until ksf.patterns.size) {
            if (iPtnBottom > Gdx.graphics.height)
                break
            val pPtnCur  = ksf.patterns[iPtnNo]
            val fPtnTick = pPtnCur.iTick
            val iLineCnt = pPtnCur.vLine.size
            iPtnTop = iPtnBottom
            iPtnBottom = iPtnTop + (iLineCnt * fGapPerStep  / fPtnTick).toLong()
            for (iLineNo in 0 until iLineCnt) {
                val iNoteTop = (iPtnTop + (iLineNo * fGapPerStep  / fPtnTick) + STEPSIZE).toInt()
                if (iNoteTop > -STEPSIZE && iNoteTop < Gdx.graphics.height) {
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
                                //drawNote(iStepNo, iLongTop[iStepNo].toInt())
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
                }else if (iNoteTop >= Gdx.graphics.height) {
                    break
                }
            }
        }

        for (iStepNo in 0 until m_iStepWidth) {
            if (bDrawLong[iStepNo]) {
                drawLongNote(iStepNo, iLongTop[iStepNo].toInt(), Gdx.graphics.height)
            }
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


    }

    private data class LongNotePress(
        var pressed: Boolean = false,
        var time: Long = 0,
        var ptn: Int = 0,
        var line: Int = 0,
    )

    private var stepWidth = 5
    private val btnPress = BooleanArray(10)
    val GaugeIncNormal = floatArrayOf(5f, 4f, 3f, -4f, -5f)
    fun updateStepData(time: Long) {
        val timeCom = System.currentTimeMillis()
        var iptn: Int
        val iptnc = ksf.patterns.size
        var line_num_s: Int
        var line_num_c: Int
        var judge_time: Long
        var line_mpos: Long
        var line_num : Int
        var judge: Int
        val key = IntArray(10)

        var ptn_now : KsfProccess.Pattern
        val keyboard = inputProcessor.getKeyBoard

        for(x in 0 until m_iStepWidth){
            key[x] = keyboard[x]
        }

        for (x in 0 until stepWidth) {
            if (key[x] == KEY_DOWN ) {
                btnPress[x] = true
                for (iptn in 0 until iptnc) {
                    val ptn_now = ksf.patterns[iptn]
                    val timepos = ptn_now.timePos + timeToPresiscion
                    if (time < timepos - ZONE_BAD) {
                        break
                    }
                    if (time > timepos + ptn_now.timeLen + ZONE_BAD){
                        continue
                    }
                    if (ptn_now.fBPM == 0.0) {
                        continue
                    }

                    line_num_s = ((time - timepos - ZONE_BAD) / 60000f * ptn_now.fBPM * ptn_now.iTick).toInt()
                    line_num_c = ptn_now.vLine.size
                    if (line_num_s < 0)
                        line_num_s = 0
                    for (line_num in line_num_s until line_num_c) {
                        line_mpos = (60000 / ptn_now.fBPM * line_num / ptn_now.iTick).toLong() + timepos
                        judge_time = abs(line_mpos - time)
                        if (judge_time < ZONE_BAD) {
                            var nnote = ptn_now.vLine[line_num].step[x]
                            if (nnote and NOTE_MISS_CHK != 0.toByte()){
                                continue
                            }
                            if(nnote == NOTE_NOTE) {
                                nnote = NOTE_NONE
                                ksf.patterns[iptn].vLine[line_num].step[x] = NOTE_NONE
                                judge = getJudgement(judge_time)
                                newFlare(x, timeCom)
                                getJudge(judge)
                                gauge = GaugeIncNormal[judge]
                                if(gauge >= 3){
                                    lifeBar.increaseLife(gauge)
                                }else if(gauge < 0){
                                    lifeBar.decreaseLife(abs(gauge))
                                }
                                key[x] = KEY_NONE

                            }else if(nnote == NOTE_LSTART){
                                var step_to_change: Byte
                                nnote = NOTE_NONE
                                ksf.patterns[iptn].vLine[line_num].step[x] = NOTE_NONE
                                if(line_num + 1 == line_num_c){
                                    var next_long_ptn = iptn + 1
                                    while(ksf.patterns[next_long_ptn].vLine.isEmpty()){
                                        ++ next_long_ptn
                                    }
                                    step_to_change = ksf.patterns[next_long_ptn].vLine.first().step[x]
                                    LONGNOTE[x].ptn = next_long_ptn
                                    LONGNOTE[x].line = 0
                                }else{
                                    step_to_change = ptn_now.vLine[line_num + 1].step[x]
                                    LONGNOTE[x].ptn = iptn;
                                    LONGNOTE[x].line = line_num + 1;
                                }
                                if(step_to_change == NOTE_LNOTE){
                                    step_to_change = NOTE_LSTART_PRESS
                                    ksf.patterns[LONGNOTE[x].ptn].vLine[LONGNOTE[x].line].step[x] = NOTE_LSTART_PRESS
                                }else if(step_to_change==NOTE_LEND){
                                    step_to_change = NOTE_LEND_PRESS;
                                    ksf.patterns[LONGNOTE[x].ptn].vLine[LONGNOTE[x].line].step[x] = NOTE_LEND_PRESS
                                }
                                LONGNOTE[x].pressed = true
                                LONGNOTE[x].time = judge_time
                                key[x] = KEY_NONE

                                judge_time = LONGNOTE[x].time shr 1
                                judge = getJudgement(judge_time)
                                getJudge(judge)
                                newFlare(x, timeCom)
                                gauge = GaugeIncNormal[judge]
                                if(gauge >= 3){
                                    lifeBar.increaseLife(gauge)
                                }else if(gauge < 0){
                                    lifeBar.decreaseLife(abs(gauge))
                                }
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
            }
            else if (key[x] == KEY_PRESS ) {
                btnPress[x] = true
                if (LONGNOTE[x].pressed) {
                    var lineNum = LONGNOTE[x].line
                    var iptn = LONGNOTE[x].ptn
                    var ptnNow = ksf.patterns[iptn]
                    val lineMpos = (60000 / ptnNow.fBPM * lineNum / ptnNow.iTick).toLong() + ptnNow.timePos + timeToPresiscion
                    if (lineMpos <= time) {
                        ptnNow.vLine[lineNum].step[x] = NOTE_NONE
                        ksf.patterns[iptn].vLine[lineNum].step[x] = NOTE_NONE
                        if (lineNum + 1 == ptnNow.vLine.size) {
                            var nextLongPtn = LONGNOTE[x].ptn + 1
                            while (ksf.patterns[nextLongPtn].vLine.isEmpty()) {
                                nextLongPtn++
                            }
                            LONGNOTE[x].ptn = nextLongPtn
                            LONGNOTE[x].line = 0
                        } else {
                            LONGNOTE[x].line++
                        }
                        lineNum = LONGNOTE[x].line
                        iptn = LONGNOTE[x].ptn
                        ptnNow = ksf.patterns[iptn]
                        when (ptnNow.vLine[lineNum].step[x]) {
                            NOTE_LEND -> {
                                ptnNow.vLine[lineNum].step[x] = NOTE_LEND_PRESS
                                ksf.patterns[LONGNOTE[x].ptn].vLine[LONGNOTE[x].line].step[x] =  NOTE_LEND_PRESS
                            }
                            NOTE_LNOTE -> {
                                ptnNow.vLine[lineNum].step[x] = NOTE_LSTART_PRESS
                                ksf.patterns[LONGNOTE[x].ptn].vLine[LONGNOTE[x].line].step[x] = NOTE_LSTART_PRESS
                            }
                            else -> {
                                val judgeTime = LONGNOTE[x].time shr 1
                                val judge = getJudgement(judgeTime)
                                getJudge(judge)
                                gauge = GaugeIncNormal[judge]
                                if(gauge >= 3){
                                    lifeBar.increaseLife(gauge)
                                }else if(gauge < 0){
                                    lifeBar.decreaseLife(abs(gauge))
                                }
                                //newJudge(judge, SystemClock.uptimeMillis())
                                newFlare(x, timeCom)
                                LONGNOTE[x].pressed = false
                            }
                        }
                    }
                }
            }
            else if (key[x] == KEY_UP){
                m_bBtnPress[x] = false
                if (LONGNOTE[x].pressed){
                    LONGNOTE[x].pressed = false
                    judge_time = getLongNoteEndTime(x) - time
                    if (judge_time < ZONE_BAD){
                        judge_time += LONGNOTE[x].time
                        judge_time = judge_time shr 1
                        judge = getJudgement(judge_time)
                        getJudge(judge)
                        newFlare(x, timeCom)
                        gauge = GaugeIncNormal[judge]
                        if(gauge >= 3){
                            lifeBar.increaseLife(gauge)
                        }else if(gauge < 0){
                            lifeBar.decreaseLife(abs(gauge))
                        }
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
                        gauge = GaugeIncNormal[JUDGE_MISS]
                        if(gauge >= 3){
                            lifeBar.increaseLife(gauge)
                        }else if(gauge < 0){
                            lifeBar.decreaseLife(abs(gauge))
                        }
                    }
                }
            }
            else{
                m_bBtnPress[x] = false
                key[x] = KEY_DOWN
                for (iptn in chkPtnNum[x] until iptnc) {
                    ptn_now = ksf.patterns[iptn]
                    val timepos = ptn_now.timePos + timeToPresiscion
                    if (time < timepos + ZONE_BAD) {
                        break
                    }
                    line_num_c = ptn_now.vLine.size
                    val lineStart = if (iptn == chkPtnNum[x]) chkLineNum[x] else 0
                    for (line_num in lineStart until line_num_c) {
                        line_mpos = (60000 / ptn_now.fBPM * line_num / ptn_now.iTick).toLong() + timepos
                        line_mpos += ZONE_BAD
                        if (line_mpos < time) {
                            var nnote = ptn_now.vLine[line_num].step[x]
                            if (nnote and NOTE_MISS_CHK != 0.toByte()) {
                                continue
                            }
                            if (nnote and NOTE_PRESS_CHK != 0.toByte()) {
                                continue
                            }
                            if (nnote == NOTE_NOTE) {
                                nnote = NOTE_NOTE_MISS
                                ksf.patterns[iptn].vLine[line_num].step[x] = NOTE_NOTE_MISS
                                getJudge(JUDGE_MISS)
                                gauge = GaugeIncNormal[JUDGE_MISS]
                                if(gauge >= 3){
                                    lifeBar.increaseLife(gauge)
                                }else if(gauge < 0){
                                    lifeBar.decreaseLife(abs(gauge))
                                }
                            }else if(nnote == NOTE_LSTART){
                                nnote = NOTE_LSTART_MISS
                                ksf.patterns[iptn].vLine[line_num].step[x] = NOTE_LSTART_MISS
                                getJudge(JUDGE_MISS)
                                gauge = GaugeIncNormal[JUDGE_MISS]
                                if(gauge >= 3){
                                    lifeBar.increaseLife(gauge)
                                }else if(gauge < 0){
                                    lifeBar.decreaseLife(abs(gauge))
                                }
                            }else if(nnote == NOTE_LEND){
                                nnote = NOTE_LEND_MISS
                                ksf.patterns[iptn].vLine[line_num].step[x] = NOTE_LEND_MISS
                            }
                            chkPtnNum[x] = iptn
                            chkLineNum[x] = line_num
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
    }

    private fun getLongNoteEndTime(x: Int): Long {
        val ln = LONGNOTE[x]
        val iPtnc = ksf.patterns.size
        for(iptn in ln.ptn until iPtnc){
            val ptn_now  = ksf.patterns[iptn]
            val line_num_c = ptn_now.vLine.size
            val line_start = if(iptn == ln.ptn) ln.line else 0
            for(line_num in line_start until line_num_c){
                val nnote = ptn_now.vLine[line_num].step[x]
                if(nnote == NOTE_LEND || nnote == NOTE_LEND_PRESS){
                    val line_mpos = (60000 / ptn_now.fBPM * line_num / ptn_now.iTick).toLong() + ptn_now.timePos + timeToCalculate
                    return line_mpos
                }
            }
        }
        return 0
    }

    private fun clearLongNote(x: Int) {
        val ln = LONGNOTE[x]
        val iPtnc = ksf.patterns.size
        for(iptn in ln.ptn until iPtnc){
            val ptn_now  = ksf.patterns[iptn]
            val line_num_c = ptn_now.vLine.size
            val line_start = if(iptn == ln.ptn) ln.line else 0
            for(line_num in line_start until line_num_c){
                var nnote = ptn_now.vLine[line_num].step[x]
                if(nnote == NOTE_LEND || nnote == NOTE_LEND_PRESS){
                    nnote = NOTE_NONE
                    ksf.patterns[iptn].vLine[line_num].step[x] = NOTE_NONE
                    return
                }else if(nnote == NOTE_LNOTE || nnote == NOTE_LSTART_PRESS){
                    nnote = NOTE_NONE
                    ksf.patterns[iptn].vLine[line_num].step[x] = NOTE_NONE
                }
            }
        }
    }

    fun getJudge(judgeType: Int) {
        a.runOnUiThread {
            when (judgeType) {
                0 -> a.getPerfect()
                1 -> a.getGreat()
                2 -> a.getGood()
                3 -> a.getBad()
                4 -> a.getMiss()
            }
        }
        when (judgeType) {
            0 -> {
                resultSong.perfect++
                curCombo++
            }
            1 -> {
                resultSong.great++
                curCombo++
            }
            2 -> {
                resultSong.good++
            }
            3 -> {
                curCombo = 0
                resultSong.bad++
            }
            4 -> {
                curCombo = 0
                resultSong.miss++
            }
        }

        if (resultSong.maxCombo < curCombo) {
            resultSong.maxCombo = curCombo
        }
    }

    private fun getJudgement(judgeTime: Long): Int {
        return if(judgeTime <= ZONE_PERFECT){
            JUDGE_PERFECT
        }else if(judgeTime <= ZONE_GREAT){
            JUDGE_GREAT
        }else if(judgeTime <= ZONE_GOOD){
            JUDGE_GOOD
        }else if(judgeTime <= ZONE_BAD){
            JUDGE_BAD
        }else{
            JUDGE_MISS
        }
    }

    fun adaptValue(valorOriginal: Long): Long {
        return (valorOriginal * STEPSIZE) / 60
    }

    fun setSpeed(type: SetSpeedType, spd: Float) {
        when (type) {
            SetSpeedType.SET -> speed = spd
            SetSpeedType.ADD -> speed += spd
            SetSpeedType.SUB -> speed -= spd
        }
        speed = speed.coerceIn(1f, 8f)
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
            batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
            if(height > 0){
                batch.draw(arrArrowsBottom[x][arrowFrame],left, y2.toFloat(), medidaFlechas, medidaFlechas)
            }
        }else{
            if(playerSong.vanish){
                if(posY > MEASUREVANISH){
                    batch.draw(arrArrowsBody[x][arrowFrame], left, posY, medidaFlechas, height)
                    batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                    if(height > 0){
                        batch.draw(arrArrowsBottom[x][arrowFrame],left, y2.toFloat(), medidaFlechas, medidaFlechas)
                    }
                }
            }
            if(playerSong.ap){
                if(posY < MEASURE){
                    if(posY + height > MEASURE){
                        height = (MEASURE - posY).toFloat()
                    }
                    batch.draw(arrArrowsBody[x][arrowFrame], left, posY, medidaFlechas, height)
                    batch.draw(arrArrows[x][arrowFrame], left, y.toFloat(), medidaFlechas, medidaFlechas)
                    if(y2 < MEASURE){
                        batch.draw(arrArrowsBottom[x][arrowFrame],left, y2.toFloat(), medidaFlechas, medidaFlechas)
                    }
                }
            }
        }
    }

    private fun drawLongNotePress(x: Int) {
        val left = medidaFlechas * (x + 1) - (medidaFlechas * 2.1f)
        val frame = (System.currentTimeMillis() % 299 * 6 / 300).toInt()
        batch.draw(flareArrowFrame[frame], left, yFlare, widthFlare, widthFlare)
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

    private fun drawFlare(x: Int, frame: Int) {
        val left = medidaFlechas * (x + 1) - (medidaFlechas * 2.1f)
        val flareSprite = flareSprites[frame]
        flareSprite.setBounds(left, yFlare, widthFlare, widthFlare)
        flareSprite.draw(batch)
    }

    private val flareSprites = Array(flareArrowFrame.size) { i ->
        Sprite(flareArrowFrame[i]).apply { setAlpha(0.8f) }
    }

    fun disposePlayer() {
        downLeftTap.dispose()
        upLeftTap.dispose()
        centerTap.dispose()
        upRightTap.dispose()
        downRightTap.dispose()
        downLeftBody.dispose()
        upLeftBody.dispose()
        centerBody.dispose()
        upRightBody.dispose()
        downRightBody.dispose()
        downLeftBottom.dispose()
        upLeftBottom.dispose()
        centerBottom.dispose()
        upRightBottom.dispose()
        downRightBottom.dispose()
        sprFlare.dispose()
        curCombo = 0
        inputProcessor.dispose()
    }
}


