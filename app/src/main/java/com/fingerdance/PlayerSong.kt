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
    var hj: Boolean = false,
    var isBGAOff: Boolean,
    var speed: String,
    var steps: String?,
    var values: Levels,
    var rutaKsf: String,
    var ap: Boolean = false,
    var fd: Boolean = false,
    var vanish: Boolean = false,
    var pathImgHJ: String = "",
    var stepMaker: String = "",
    var rs: Boolean = false,
    var mirror: Boolean = false,
    var isBAGDark: Boolean = false,
    var pathImgRS: String = "",
    var pathImgMirror: String = "",
    var snake: Boolean = false
) : Serializable
