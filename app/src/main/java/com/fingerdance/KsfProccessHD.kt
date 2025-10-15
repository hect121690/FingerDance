package com.fingerdance

import java.io.File
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.math.round

class KsfProccessHD {
    companion object {
        const val STEPINFO_STEP = 0
        const val STEPINFO_BPM = 1
        const val STEPINFO_TICK = 2
        const val STEPINFO_DELAY = 3
        const val STEPINFO_DELAYBEAT = 4
        const val STEPINFO_UNKNOWN = 5
        const val STEPINFO_SPEED = 6

        const val NOTE_NONE: Byte = 0
        const val NOTE_NOTE: Byte = 1
        const val NOTE_LSTART: Byte = 6
        const val NOTE_LNOTE: Byte = 2
        const val NOTE_LEND: Byte = 10

        const val NOTE_START_CHK: Byte = 4
        const val NOTE_END_CHK: Byte = 8
    }

    data class Line(val step: ByteArray = ByteArray(10))

    data class Pattern(
        var timePos: Long = 0,
        var timeLen: Long = 0,
        var timeDelay: Long = 0,
        var iTick: Int = 0,
        var iLastMissCheck: Int = 0,
        var fBPM: Float = 0f,
        val vLine: MutableList<Line> = mutableListOf(),
        var iSpeed: Float = 0f
    )

    data class LoadingInfo(val tag: String, val value: String)
    data class StepInfo(val step: String, val type: Int)
    data class LongNoteInfo(var bUsed: Boolean = false, var iPrevPtn: Int = 0, var iPrevPos: Int = 0)
    val patterns = mutableListOf<Pattern>()

    fun load(filePath: String): Boolean {
        val loadInfo = mutableListOf<LoadingInfo>()
        val stepInfo = mutableListOf<StepInfo>()

        try {
            File(filePath).bufferedReader().use { reader ->
                var line: String?
                var readingSteps = false
                while (reader.readLine().also { line = it } != null) {
                    if (line!!.startsWith("#") && !readingSteps) {
                        val parts = line!!.split(":", limit = 2)
                        if (parts.size == 2) {
                            val tag = parts[0].substring(1)
                            var value = parts[1]
                            if (value.endsWith(";")) {
                                value = value.substring(0, value.length - 1)
                            }
                            if (tag == "STEP" && line!!.length == 6) {
                                readingSteps = true
                            } else {
                                loadInfo.add(LoadingInfo(tag, value))
                            }
                        }
                    } else if (readingSteps) {
                        when {
                            line!!.startsWith("|") && line!!.endsWith("|") -> {
                                val type = when (line!![1]) {
                                    'B' -> STEPINFO_BPM
                                    'T' -> STEPINFO_TICK
                                    'D' -> STEPINFO_DELAY
                                    'E' -> STEPINFO_DELAYBEAT
                                    'S' -> STEPINFO_SPEED
                                    else -> STEPINFO_UNKNOWN
                                }
                                stepInfo.add(StepInfo(line!!.substring(2, line!!.length - 1), type))
                            }
                            line!!.startsWith("#") -> {
                                val parts = line!!.split(":", limit = 2)
                                if (parts.size == 2) {
                                    val tag = parts[0].substring(1)
                                    var value = parts[1]
                                    if (value.endsWith(";")) {
                                        value = value.substring(0, value.length - 1)
                                    }
                                    val type = when (tag) {
                                        "BPM" -> STEPINFO_BPM
                                        "TICKCOUNT" -> STEPINFO_TICK
                                        "DELAY" -> STEPINFO_DELAY
                                        "DELAYBEAT" -> STEPINFO_DELAYBEAT
                                        else -> STEPINFO_UNKNOWN
                                    }
                                    stepInfo.add(StepInfo(value, type))
                                }
                            }
                            line!!.length == 13 -> {
                                stepInfo.add(StepInfo(line!!, STEPINFO_STEP))
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        var tickCount = 4
        var bpm = 0f
        var bpm2 = 0f
        var bpm3 = 0f
        var bunki = 0L
        var bunki2 = 0L
        var startTime = 0L
        var startTime2 = 0L
        var startTime3 = 0L
        var bUseOldBPM = false

        loadInfo.forEach { info ->
            when (info.tag) {
                "TICKCOUNT" -> tickCount = info.value.toInt()
                "BPM" -> bpm = info.value.toFloat()
                "BPM2" -> bpm2 = info.value.toFloat()
                "BPM3" -> bpm3 = info.value.toFloat()
                "BUNKI" -> bunki = info.value.toLong() * 10
                "BUNKI2" -> bunki2 = info.value.toLong() * 10
                "STARTTIME" -> startTime = ((round(info.value.toDouble()) + TIME_ADJUST + valueOffset) * 10).toLong()
                "STARTTIME2" -> startTime2 = ((round(info.value.toDouble()) + TIME_ADJUST + valueOffset) * 10).toLong()
                "STARTTIME3" -> startTime3 = ((round(info.value.toDouble()) + TIME_ADJUST + valueOffset) * 10).toLong()
            }
        }

        //if (bpm < 0) bpm = 0f
        //if (bpm2 < 0) bpm2 = 0f
        //if (bpm3 < 0) bpm3 = 0f
        //if (startTime < 0) startTime = 0
        //if (startTime2 < 0) startTime2 = 0
        //if (startTime3 < 0) startTime3 = 0
        if (bunki < 0) bunki = 0
        if (bunki2 < 0) bunki2 = 0

        if (bpm2 != 0F) {
            bUseOldBPM = true
            if (bunki == 0L) {
                bpm = bpm2
                bpm2 = bpm3
                bpm3 = 0f
                bunki = bunki2
                bunki2 = 0
                startTime = startTime2
                startTime2 = startTime3
                startTime3 = 0
            }
            if (bunki == bunki2) {
                bpm2 = bpm3
                bpm3 = 0f
                bunki2 = 0
                startTime2 = startTime3
                startTime3 = 0
            }
            if (bpm2 == 0f) {
                bUseOldBPM = false
            }
        }

        val curLongNote = Array(10) { LongNoteInfo() }
        var curTick = tickCount
        var curBPM = bpm
        val buttonCount = 10

        var curPtn = Pattern(fBPM = bpm, iTick = tickCount)
        patterns.add(curPtn)

        stepInfo.forEach { info ->
            when (info.type) {
                STEPINFO_STEP -> {
                    val line = Line()
                    for (iStep in 0 until buttonCount) {
                        when (info.step[iStep]) {
                            '1' -> {
                                line.step[iStep] = NOTE_NOTE
                                if (curLongNote[iStep].bUsed) {
                                    val prevStep = patterns[curLongNote[iStep].iPrevPtn].vLine[curLongNote[iStep].iPrevPos].step[iStep]
                                    curLongNote[iStep].bUsed = false
                                    patterns[curLongNote[iStep].iPrevPtn].vLine[curLongNote[iStep].iPrevPos].step[iStep] = (prevStep or NOTE_END_CHK)
                                    if ((prevStep and (NOTE_START_CHK or NOTE_END_CHK)) == (NOTE_START_CHK or NOTE_END_CHK)) {
                                        patterns[curLongNote[iStep].iPrevPtn].vLine[curLongNote[iStep].iPrevPos].step[iStep] = NOTE_NOTE
                                    }
                                }
                            }
                            '4' -> {
                                line.step[iStep] = NOTE_LNOTE
                                if (!curLongNote[iStep].bUsed) {
                                    line.step[iStep] = (line.step[iStep] or NOTE_START_CHK)
                                }
                                curLongNote[iStep].bUsed = true
                                curLongNote[iStep].iPrevPtn = patterns.size - 1
                                curLongNote[iStep].iPrevPos = patterns.last().vLine.size
                            }
                            else -> {
                                if (curLongNote[iStep].bUsed) {
                                    val prevStep = patterns[curLongNote[iStep].iPrevPtn].vLine[curLongNote[iStep].iPrevPos].step[iStep]
                                    curLongNote[iStep].bUsed = false
                                    patterns[curLongNote[iStep].iPrevPtn].vLine[curLongNote[iStep].iPrevPos].step[iStep] = (prevStep or NOTE_END_CHK)
                                    if ((prevStep and (NOTE_START_CHK or NOTE_END_CHK)) == (NOTE_START_CHK or NOTE_END_CHK)) {
                                        patterns[curLongNote[iStep].iPrevPtn].vLine[curLongNote[iStep].iPrevPos].step[iStep] = NOTE_NOTE
                                    }
                                }
                                line.step[iStep] = NOTE_NONE
                            }
                        }
                    }
                    patterns.last().vLine.add(line)
                }
                STEPINFO_BPM -> {
                    if (patterns.last().vLine.isNotEmpty()) {
                        curPtn = Pattern(fBPM = info.step.toFloat(), iTick = curTick)
                        patterns.add(curPtn)
                    } else {
                        curPtn.fBPM = info.step.toFloat()
                    }
                    curBPM = curPtn.fBPM
                    if(curBPM < -999){
                        curBPM = -999F
                    }
                    if(curBPM > 999){
                        curBPM = 999F
                    }
                }
                STEPINFO_TICK -> {
                    if (patterns.last().vLine.isNotEmpty()) {
                        curPtn = Pattern(fBPM = curBPM, iTick = info.step.toInt())
                        patterns.add(curPtn)
                    } else {
                        curPtn.iTick = info.step.toInt()
                    }
                    curTick = curPtn.iTick
                }
                STEPINFO_DELAY -> {
                    val delayPtn = Pattern(fBPM = curBPM, iTick = curTick, timeDelay = info.step.toDouble().toLong())
                    if (patterns.last().vLine.isNotEmpty()) {
                        curPtn = Pattern(fBPM = curBPM, iTick = curTick)
                    } else {
                        patterns.removeAt(patterns.lastIndex)
                    }
                    patterns.add(delayPtn)
                    patterns.add(curPtn)
                }
                STEPINFO_DELAYBEAT -> {
                    val delayPtn = Pattern(fBPM = curBPM, iTick = curTick)
                    if (patterns.last().vLine.isNotEmpty()) {
                        curPtn = Pattern(fBPM = curBPM, iTick = curTick)
                    } else {
                        patterns.removeAt(patterns.lastIndex)
                    }
                    delayPtn.timeDelay = ((60000 / curBPM / curTick) * info.step.toFloat()).toLong()
                    //delayPtn.timeDelay = (60000 / curBPM * info.step.toFloat() / curTick).toLong()
                    patterns.add(delayPtn)
                    patterns.add(curPtn)
                }

                STEPINFO_SPEED -> {
                    if (patterns.last().vLine.isNotEmpty()) {
                        curPtn = Pattern(fBPM = curBPM, iTick = curTick)
                    } else {
                        patterns.removeAt(patterns.lastIndex)
                    }
                    curPtn.iSpeed = info.step.trim().toFloat() //Pair(pair[0].trim().toFloat(), pair[1].trim().toLong())
                    patterns.add(curPtn)
                }

            }
        }

        if (bUseOldBPM) {
            val ptn0 = patterns.first()
            val ptn1: Pattern?
            var ptn2: Pattern? = null
            ptn0.timePos = startTime

            if (bunki != 0L) {
                var cuttingPos = getCuttingPos(bpm, startTime, bunki, ptn0.vLine.size.toLong(), tickCount)
                if (cuttingPos >= 0) {
                    ptn1 = Pattern(fBPM = bpm2, timePos = startTime2, iTick = ptn0.iTick)
                    patterns.add(ptn1)
                    while (ptn0.vLine.size > cuttingPos) {
                        val newLine = ptn0.vLine[cuttingPos]
                        ptn1.vLine.add(newLine)
                        ptn0.vLine.removeAt(cuttingPos)
                    }
                    if (bunki2 != 0L) {
                        val cuttingPos2 = getCuttingPos(bpm2, startTime2, bunki2, (ptn0.vLine.size + ptn1.vLine.size).toLong(), tickCount)
                        if (cuttingPos < cuttingPos2) {
                            ptn2 = Pattern(fBPM = bpm3, timePos = startTime3, iTick = ptn1.iTick)
                            patterns.add(ptn2)
                            //val newCuttingPos = cuttingPos2 - cuttingPos
                            cuttingPos = cuttingPos2 - cuttingPos
                            while  (ptn1.vLine.size > cuttingPos) {
                                val newLine = ptn1.vLine[cuttingPos]
                                ptn2.vLine.add(newLine)
                                ptn1.vLine.removeAt(cuttingPos)
                            }
                        } else if (cuttingPos == cuttingPos2) {
                            ptn1.fBPM = bpm3
                        } else {
                            ptn2 = Pattern(fBPM = bpm3, timePos = startTime3, iTick = ptn1.iTick)
                            patterns.add(ptn2)
                            cuttingPos -= cuttingPos2
                            cuttingPos = ptn0.vLine.size - cuttingPos
                            //val newCuttingPos = ptn0.vLine.size - (cuttingPos2 - cuttingPos)
                            while (ptn0.vLine.size > cuttingPos) {
                                val newLine = ptn0.vLine[cuttingPos]
                                ptn2.vLine.add(newLine)
                                ptn0.vLine.removeAt(cuttingPos)
                            }
                            while (ptn1.vLine.isNotEmpty()) {
                                val newLine = ptn0.vLine[cuttingPos]
                                ptn2.vLine.add(newLine)
                                ptn1.vLine.removeAt(0)
                            }
                            patterns.removeAt(1)
                        }
                    }
                    ptn1.timePos = getPtnTimePos(bpm2, startTime2, bunki, tickCount)
                    cuttingPos = getCuttingPos(bpm, startTime, bunki, Long.MAX_VALUE, tickCount)
                    var tcuttingPos = getCuttingPos(bpm2, startTime2, bunki, Long.MAX_VALUE, tickCount)
                    while (cuttingPos < tcuttingPos) {
                        tcuttingPos--
                        ptn1.vLine.removeAt(0)
                    }
                    if (cuttingPos > tcuttingPos) {
                        val temp = mutableListOf<Line>()
                        while (cuttingPos > tcuttingPos) {
                            val newLine = Line()
                            tcuttingPos++
                            temp.add(newLine)
                        }
                        ptn1.vLine.addAll(0, temp)
                        temp.clear()
                    }
                    if (patterns.size == 3) {
                        ptn2!!.timePos = getPtnTimePos(bpm3, startTime3, bunki2, tickCount)
                        val cuttingPos2 = getCuttingPos(bpm2, startTime2, bunki2, Long.MAX_VALUE, tickCount)
                        var tcuttingPos2 = getCuttingPos(bpm3, startTime3, bunki2, Long.MAX_VALUE, tickCount)
                        while (cuttingPos2 < tcuttingPos2) {
                            tcuttingPos2--
                            ptn2.vLine.removeAt(0)
                        }
                        if (cuttingPos2 > tcuttingPos2) {
                            val temp = mutableListOf<Line>()
                            while (cuttingPos2 > tcuttingPos2) {
                                val newLine = Line()
                                tcuttingPos2++
                                temp.add(newLine)
                            }
                            ptn2.vLine.addAll(0, temp)
                            temp.clear()
                        }
                    }
                }
            }
        } else {
            var lastTick = startTime.toFloat()
            for (i in 0 until patterns.size) {
                val nowptn = patterns[i]
                nowptn.timePos = lastTick.toLong()
                if (nowptn.timeDelay != 0L) {
                    lastTick += nowptn.timeDelay
                } else {
                    if (nowptn.fBPM != 0f) {
                        lastTick += (60000f / nowptn.fBPM * (if(nowptn.vLine.size == 0) 1 else nowptn.vLine.size) / nowptn.iTick.toFloat()) + nowptn.timeDelay
                    }
                }
            }
        }

        for(i in 0 until patterns.size){
                val ptn = patterns[i]
            if(ptn.timeDelay != 0L){
                ptn.timeLen = ptn.timeDelay
            }else{
                if (ptn.fBPM != 0F) {
                    ptn.timeLen = ((60000 / ptn.fBPM * (if(ptn.vLine.size == 0) 1 else ptn.vLine.size) / ptn.iTick).toLong()) + ptn.timeDelay
                }else{
                    ptn.timeLen = 0
                }
            }
        }

        /*
        patterns.forEach { ptn ->
            ptn.timeLen = if (ptn.timeDelay != 0L) {
                ptn.timeDelay
            } else {
                if (ptn.fBPM != 0F) {
                    (60000 / ptn.fBPM * ptn.vLine.size / ptn.iTick).toLong()
                } else {
                    0
                }
            }
        }
        */

        for (i in 0 until patterns.size - 1) {
            val mpos = patterns[i].timePos
            val mlen = patterns[i].timeLen
            val npos = patterns[i + 1].timePos
            if (mpos + mlen < npos) {
                patterns[i].timeLen = npos - mpos
            }
            if (bUseOldBPM) {
                patterns[i].timeLen = npos - mpos
            }
        }

        return true
    }

    private fun getPtnTimePos(bpm: Float, start: Long, bunki: Long, tick: Int): Long {
        val lastTick = start.toFloat()
        val ticks = 60000 / bpm
        val dest = bunki.toFloat()

        var num = 0
        while (true) {
            val now = lastTick + (ticks * num / tick)
            if (now >= dest) {
                return now.toLong()
            }
            num++
        }
    }

    private fun getCuttingPos(bpm: Float, start: Long, bunki: Long, max: Long, tick: Int): Int {
        val lastTick = start.toFloat()
        val ticks = 60000 / bpm
        val dest = bunki.toFloat()

        for (num in 0 until max.toInt()) {
            val now = lastTick + (ticks * num / tick)
            if (now >= dest) {
                return num
            }
        }
        return -1
    }

    fun makeMirror() {
        val mirrorMap = intArrayOf(7, 3, 2, 5, 4, 6)
        val stepWidth = 10

        patterns.forEach { ptn ->
            val nowLong = ByteArray(10) { 255.toByte() }
            val newLong = ByteArray(10) { 255.toByte() }

            ptn.vLine.forEach { line ->
                val newLine = ByteArray(stepWidth) { NOTE_NONE }

                for (i in 0 until stepWidth) {
                    when (line.step[i]) {
                        NOTE_NOTE -> {
                            val newPos = mirrorMap[i]
                            newLine[newPos] = NOTE_NOTE
                        }
                        NOTE_LSTART -> {
                            val newPos = mirrorMap[i]
                            newLine[newPos] = NOTE_LSTART
                            nowLong[i] = i.toByte()
                            newLong[i] = newPos.toByte()
                        }
                        NOTE_LNOTE -> {
                            if (nowLong[i] != 255.toByte()) {
                                val newPos = newLong[i].toInt()
                                newLine[newPos] = NOTE_LNOTE
                            }
                        }
                        NOTE_LEND -> {
                            if (nowLong[i] != 255.toByte()) {
                                val newPos = newLong[i].toInt()
                                newLine[newPos] = NOTE_LEND
                                nowLong[i] = 255.toByte()
                                newLong[i] = 255.toByte()
                            }
                        }
                    }
                }

                for (i in 0 until stepWidth) {
                    line.step[i] = newLine[i]
                }
            }
        }
    }

    fun makeRandom() {
        val mirrorMap1 = intArrayOf(4, 7, 2, 3, 5, 6)
        val mirrorMap2 = intArrayOf(2, 4, 6, 5, 3, 7)
        val mirrorMap3 = intArrayOf(3, 5, 7, 4, 6, 2)

        val mirrorMaps = listOf(mirrorMap1, mirrorMap2, mirrorMap3)
        val mirrorMap = mirrorMaps.random()
        val stepWidth = 10

        patterns.forEach { ptn ->
            val nowLong = ByteArray(stepWidth) { 255.toByte() }
            val newLong = ByteArray(stepWidth) { 255.toByte() }

            ptn.vLine.forEach { line ->
                val newLine = ByteArray(stepWidth) { NOTE_NONE }

                for (i in 0 until stepWidth) {
                    when (line.step[i]) {
                        NOTE_NOTE -> {
                            val newPos = mirrorMap[i]
                            newLine[newPos] = NOTE_NOTE
                        }
                        NOTE_LSTART -> {
                            val newPos = mirrorMap[i]
                            newLine[newPos] = NOTE_LSTART
                            nowLong[i] = i.toByte()
                            newLong[i] = newPos.toByte()
                        }
                        NOTE_LNOTE -> {
                            if (nowLong[i] != 255.toByte()) {
                                val newPos = newLong[i].toInt()
                                newLine[newPos] = NOTE_LNOTE
                            }
                        }
                        NOTE_LEND -> {
                            if (nowLong[i] != 255.toByte()) {
                                val newPos = newLong[i].toInt()
                                newLine[newPos] = NOTE_LEND
                                nowLong[i] = 255.toByte()
                                newLong[i] = 255.toByte()
                            }
                        }
                    }
                }

                for (i in 0 until stepWidth) {
                    line.step[i] = newLine[i]
                }
            }
        }
    }

}
