package com.fingerdance

import java.io.Serializable


class PlayerSong(
    var rutaCancion: String?,
    var rutaVideo: String?,
    var rutaBanner: String?,
    var bpm: Double?,
    var tickCount: Double?,
    var scroll: Double?,
    var level: String?,
    var rutaNoteSkin: String?,
    var hj: Boolean?,
    var isBGAOff: Boolean,
    var speed: String,
    var steps: String?,
    var values: Levels,
    var rutaKsf: String?
) : Serializable