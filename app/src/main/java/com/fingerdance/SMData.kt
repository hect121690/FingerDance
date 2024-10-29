package com.fingerdance

class SMData {
        var level = Levels()
        //private val topFade = height - medidaFlechas
        //private var allMeaseures = arrayListOf<MutableList<String>>()
        //private var stringNotes = arrayListOf<String>()

        fun proccessNotes(stringLevel: String): Levels {
            var offSet = 0f
            var bpmInicial = 120f
            var tickInicial = 4f
            var speedInicial = 1f
            var scrollInicial = 1f
            var timeSignatureInicial= 0f
            var fakeInicial = 0f
            var stopInicial = 0f
            var comboinicial = 1f
            var warpInicial = 0f
            var delayInicial = 0f

            var currentList: MutableList<Pair<Float, Float>>? = null
            var currentListSpeeds: MutableList<SpeedChange>? = null
            var currentListNotes: MutableList<String>? = null

            var currentLine = 0

            stringLevel.lines().forEach { line ->
                if (line.startsWith("#OFFSET:")) {
                    offSet = line.removePrefix("#OFFSET:").removeSuffix(";").toFloat()
                    level.offset = offSet
                } else if (line.startsWith("#BPMS:")) {
                    bpmInicial = line.removePrefix("#BPMS:").removeSuffix(";").split("=")[1].toFloat()
                    level.bpmInicial = bpmInicial
                    currentList = null
                    currentList = level.bpms
                } else if (line.startsWith("#TICKCOUNTS:")) {
                    tickInicial = line.removePrefix("#TICKCOUNTS:").removeSuffix(";").split("=")[1].toFloat()
                    level.tickInicial = tickInicial
                    currentList = null
                    currentList = level.ticks
                } else if (line.startsWith("#SCROLLS:")) {
                    scrollInicial = line.removePrefix("#SCROLLS:").removeSuffix(";").split("=")[1].toFloat()
                    level.scrollInicial = scrollInicial
                    currentList = null
                    currentList = level.scrolls
                } else if (line.startsWith("#FAKES:")) {
                    if(line.contains("=")) {
                        fakeInicial = line.removePrefix("#FAKES:").removeSuffix(";").split("=")[1].toFloat()
                        level.fakeInicial = fakeInicial
                        currentList = null
                        currentList = level.fakes
                    }else{
                        currentList = null
                        return@forEach
                    }
                } else if (line.startsWith("#WARPS:")) {
                    if(line.contains("=")) {
                        warpInicial = line.removePrefix("#WARPS:").removeSuffix(";").split("=")[1].toFloat()
                        level.warpInicial = warpInicial
                        currentList = null
                        currentList = level.warps
                    }else{
                        currentList = null
                        return@forEach
                    }
                } else if (line.startsWith("#COMBOS:")) {
                    comboinicial = line.removePrefix("#COMBOS:").removeSuffix(";").split("=")[1].toFloat()
                    level.comboinicial = comboinicial
                    currentList = null
                    currentList = level.combos
                } else if (line.startsWith("#STOPS:")) {
                    if(line.contains("=")){
                        stopInicial = line.removePrefix("#STOPS:").removeSuffix(";").split("=")[1].toFloat()
                        level.stopInicial = stopInicial
                        currentList = null
                        currentList = level.stops
                    }else{
                        currentList = null
                        return@forEach
                    }
                } else if (line.startsWith("#DELAYS:")) {
                    if(line.contains("=")) {
                        delayInicial = line.removePrefix("#DELAYS:").removeSuffix(";").split("=")[1].toFloat()
                        level.delayInicial = delayInicial
                        currentList = null
                        currentList = level.delays
                    }else{
                        currentList = null
                        return@forEach
                    }
                } else if (line.startsWith("#SPEEDS:")) {
                    speedInicial = line.removePrefix("#SPEEDS:").removeSuffix(";").split("=")[1].toFloat()
                    level.speedInicial = speedInicial
                    currentListSpeeds = null
                    currentListSpeeds = level.speeds
                } else if (line.startsWith("#TIMESIGNATURES:")) {
                    timeSignatureInicial = line.removePrefix("#TIMESIGNATURES:").removeSuffix(";").split("=")[1].toFloat()
                    level.timeSignatureInicial = timeSignatureInicial
                    currentList = null
                    currentList = level.timeSignature
                } else if (line.startsWith("#NOTES:")) {
                    currentListNotes = level.stringNotes
                } else if (line.startsWith(";")) {
                    currentList = null
                    currentListNotes = null
                } else if (currentList != null || currentListSpeeds != null) {
                    val keyValue = line.split("=")
                    if (keyValue.size == 2) {
                        try {
                            val key = keyValue[0].replace(",", "").toFloat() / tickInicial
                            val value = keyValue[1].replace(",", "").toFloat()
                            currentList!!.add(Pair(key, value))
                        } catch (e: NumberFormatException) {
                            e.printStackTrace()
                            println("Error al procesar efecto: ${e.message}")
                        }
                    } else if (keyValue.size == 4) {
                        try {
                            val time = keyValue[0].replace(",", "").toFloat() / tickInicial
                            val value = keyValue[1].toFloat()
                            val duration = keyValue[2].toFloat()
                            val w = keyValue[3].replace(",", "").toInt()
                            currentListSpeeds!!.add(SpeedChange(time, value, duration, w))
                        } catch (e: NumberFormatException) {
                            e.printStackTrace()
                            println("Error al procesar efecto: ${e.message}")
                        }
                    }
                    if (currentListNotes != null) {
                        //if(line.length != 5){
                            //return@forEach

                            if(line == "," || line.contains("measure", true) || line == ";"){
                                val last = level.stringNotes.indexOfLast { it.length < 5 }
                                level.stringNotes.add(currentLine.toString())
                                if(last != -1){
                                    level.stringNotes[last] = currentLine.toString()
                                }
                                currentLine = 0
                            }else if (line.length == 5){
                                currentLine ++
                                level.stringNotes.add(line)
                            }

                    }
                }
            }
            level.notes = processNotesSection(level)
            return level
        }

        fun processNotesSection(level: Levels): MutableList<Note> {
            val notes = mutableListOf<Note>()
            var currentBpm = level.bpmInicial
            var currentTick = level.tickInicial
            var currentScroll = level.scrollInicial
            var currentSpeed = level.speedInicial
            var currenTimeSignature = level.timeSignatureInicial

            var bpmIndex = 0
            var tickIndex = 0
            var speedIndex = 0
            var scrollIndex = 0
            var timeSgnatureIndex = 0

            val holdStart: Note? = null

            val listLongDuration = arrayListOf(0f, 0f, 0f, 0f, 0f)
            val listIsLong = arrayListOf(false, false, false, false, false)
            val listCurrentLong = arrayListOf(holdStart, holdStart, holdStart, holdStart, holdStart)
            var currentMeasure: Int
            var currentTime = 0f
            var accumulatedTime = 0f

            if(level.stringNotes.last().length < 5){
                level.stringNotes.removeAt(level.stringNotes.size - 1)
            }

            if(level.stringNotes.first().length < 5){
                currentMeasure = level.stringNotes.first().toInt()
                level.stringNotes.removeAt(0)
            }else{
                currentMeasure = level.stringNotes.find { it.length < 5 }!!.toInt()
            }

            level.stringNotes.forEachIndexed { _index, line ->

                if (bpmIndex < level.bpms.size && accumulatedTime >= level.bpms[bpmIndex].first) {
                    currentBpm = level.bpms[bpmIndex].second
                    bpmIndex++
                }

                if (tickIndex < level.ticks.size && accumulatedTime >= level.ticks[tickIndex].first) {
                    currentTick = level.ticks[tickIndex].second
                    tickIndex++
                }
                /*
                if (scrollIndex < level.scrolls.size && accumulatedTime >= level.scrolls[scrollIndex].first) {
                    currentScroll = level.scrolls[scrollIndex].second
                    scrollIndex++
                }

                if (speedIndex < level.speeds.size && accumulatedTime >= level.speeds[speedIndex].time) {
                    currentSpeed = level.speeds[speedIndex].speed
                    speedIndex++
                }


                while (timeSgnatureIndex < level.timeSignature.size && accumulatedTime >= level.timeSignature[timeSgnatureIndex].first) {
                    currenTimeSignature = level.timeSignature[timeSgnatureIndex].second
                    timeSgnatureIndex++
                }
                */


                if(line.length < 5){
                    currentMeasure = line.toInt()
                }else {
                    val time =  ((60 / currentBpm) / currentMeasure)
                    currentTime = level.offset + time
                    accumulatedTime -= currentTime

                    line.forEachIndexed { colIndex, char ->
                        val noteType = when (char) {
                            '0' -> NoteType.EMPTY
                            '1' -> NoteType.TAP
                            '2' -> NoteType.HOLD_START
                            '3' -> NoteType.HOLD_END
                            else -> NoteType.EMPTY
                        }

                        when (noteType) {
                            NoteType.TAP -> {
                                notes.add(Note(colIndex + 1, noteType, accumulatedTime, 0f))
                            }
                            NoteType.HOLD_START -> {
                                listIsLong[colIndex] = true
                                listLongDuration[colIndex] -= currentTime
                                listCurrentLong[colIndex] = Note(colIndex + 1, noteType, accumulatedTime, 0f, listLongDuration[colIndex]
                                )
                            }
                            NoteType.EMPTY -> {
                                if (listIsLong[colIndex]) {
                                    listLongDuration[colIndex] -= currentTime
                                    listCurrentLong[colIndex]?.let { startNote ->
                                        startNote.duration = listLongDuration[colIndex]
                                        //notes.add(startNote)
                                    }
                                }
                            }
                            NoteType.HOLD_END -> {
                                //listLongDuration[colIndex] += beatDuration
                                notes.add(listCurrentLong[colIndex]!!)

                                listIsLong[colIndex] = false
                                listLongDuration[colIndex] = 0f
                                listCurrentLong[colIndex] = holdStart

                                notes.add(Note(colIndex + 1, noteType, accumulatedTime, 0f))
                            }
                        }
                    }
                }
            }

            return notes
        }

        data class SpeedChange(val time: Float, val speed: Float, val duration: Float, val mode: Int)

        data class Note(
            val column: Int,
            val type: NoteType,
            var time: Float,
            var positionY: Float,
            var duration: Float = 0f,
        )

        enum class NoteType {
            EMPTY, TAP, HOLD_START, HOLD_END
        }
    }




