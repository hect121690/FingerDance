package com.fingerdance.ssc

import com.badlogic.gdx.audio.Music

class AudioEngine(private val music: Music) {

    private var offsetMs = 0.0
    private var playing = false

    // smoothing
    private var lastAudioMs = 0.0
    private var lastSampleTime = 0.0

    fun load(offsetMs: Double) {
        this.offsetMs = offsetMs
    }

    fun play() {
        if (playing) return

        val offsetSec = offsetMs / 1000.0

        if (offsetSec < 0) {
            // offset negativo → adelantar audio
            music.position = (-offsetSec).toFloat()
        } else {
            music.position = 0f
        }

        music.play()
        playing = true

        // reset smoothing
        lastAudioMs = music.position * 1000.0
        lastSampleTime = nowMs()
    }

    fun pause() {
        if (!playing) return
        music.pause()
        playing = false
    }

    fun stop() {
        music.stop()
        playing = false
    }

    fun isPlaying(): Boolean = music.isPlaying

    fun currentTimeMs(): Double {
        if (!playing) return lastAudioMs

        val audioMs = music.position * 1000.0
        val now = nowMs()

        // smoothing simple para evitar jitter
        val delta = now - lastSampleTime
        val smoothed = audioMs + delta

        lastAudioMs = audioMs
        lastSampleTime = now

        // aplicar offset (gameplay offset)
        return smoothed - offsetMs
    }

    private fun nowMs(): Double {
        return System.nanoTime() / 1_000_000.0
    }
}