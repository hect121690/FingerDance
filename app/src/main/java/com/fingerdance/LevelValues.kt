package com.fingerdance

class Levels(
    //var meter: String,
    var offset: Float = 0f,
    var bpmInicial: Float = 0f,
    var tickInicial: Float = 0f,
    var speedInicial: Float = 0f,
    var scrollInicial: Float = 0f,
    var timeSignatureInicial: Float = 0f,

    var fakeInicial: Float = 0f,
    var stopInicial: Float = 0f,
    var comboinicial: Float = 0f,
    var warpInicial: Float = 0f,
    var delayInicial: Float = 0f,
    var bpms: MutableList<Pair<Float, Float>> = arrayListOf(),
    var ticks: MutableList<Pair<Float, Float>> = arrayListOf(),
    var scrolls: MutableList<Pair<Float, Float>> = arrayListOf(),
    var timeSignature: MutableList<Pair<Float, Float>> = arrayListOf(),
    var fakes: MutableList<Pair<Float, Float>> = arrayListOf(),
    var stops: MutableList<Pair<Float, Float>> = arrayListOf(),
    var combos: MutableList<Pair<Float, Float>> = arrayListOf(),
    var warps: MutableList<Pair<Float, Float>> = arrayListOf(),
    var delays: MutableList<Pair<Float, Float>> = arrayListOf(),
    var notes: MutableList<Parser.Note> = arrayListOf(),
    var stringNotes: MutableList<String> = arrayListOf())