package com.fingerdance

import android.media.AudioAttributes
import android.media.MediaPlayer
import java.io.File

class SongAudioController(private val startTimeMs: Int) {
    private var player: MediaPlayer? = null
    private var generation = 0L
    private var resumed = false
    private var transitionPlaying = false
    private var prepared = false
    fun prepare(path: String, selectionGeneration: Long, onError: (() -> Unit)? = null) {
        generation = selectionGeneration
        prepared = false
        releasePlayer()
        val file = File(path)
        if (!file.isFile) { onError?.invoke(); return }
        val current = MediaPlayer()
        player = current
        runCatching {
            current.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
            current.setDataSource(path)
            current.setOnPreparedListener {
                if (player !== current || generation != selectionGeneration) return@setOnPreparedListener
                prepared = true
                val seek = startTimeMs.coerceAtMost((it.duration - 1).coerceAtLeast(0))
                it.seekTo(seek)
                startWhenAllowed()
            }
            current.setOnErrorListener { _, _, _ -> onError?.invoke(); true }
            current.prepareAsync()
        }.onFailure { releasePlayer(); onError?.invoke() }
    }
    fun setTransitionPlaying(value: Boolean) { transitionPlaying = value; if (!value) startWhenAllowed() else pause() }
    fun onResume() { resumed = true; startWhenAllowed() }
    fun onPause() { resumed = false; pause() }
    fun pause() { runCatching { player?.takeIf { it.isPlaying }?.pause() } }
    fun stop() { releasePlayer() }
    fun isPlaying(): Boolean = runCatching { player?.isPlaying == true }.getOrDefault(false)
    private fun startWhenAllowed() {
        if (!resumed || transitionPlaying || !prepared) return
        runCatching { player?.start() }
    }
    private fun releasePlayer() {
        val old = player
        player = null
        if (old != null) {
            runCatching { old.setOnPreparedListener(null); old.setOnCompletionListener(null); old.setOnErrorListener(null) }
            runCatching { if (old.isPlaying) old.stop() }
            runCatching { old.reset() }
            runCatching { old.release() }
        }
    }
    fun release() { resumed = false; generation++; releasePlayer() }
}
