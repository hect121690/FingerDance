package com.fingerdance

import java.io.Serializable

class ResultSong: Serializable {
    var perfect: Int = 0
    var great: Int = 0
    var good: Int = 0
    var bad: Int = 0
    var miss: Int = 0
    var maxCombo: Int = 0
    var level: String = ""
    var banner: String? = null
    var listEfects: MutableList<String>? = null

    constructor(
        perfect: Int = 0,
        great: Int = 0,
        good: Int = 0,
        bad: Int = 0,
        miss: Int = 0,
        maxCombo: Int = 0,
        level: String = "",
        banner: String = "",
        listEfects: MutableList<String> = mutableListOf()
    ){

        this.perfect = perfect
        this.great = great
        this.good = good
        this.bad = bad
        this.miss = miss
        this.maxCombo = maxCombo
        this.level = level
        this.banner = banner
        this.listEfects = listEfects
    }
}