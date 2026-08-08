package com.fingerdance

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import android.view.View
import java.io.File

class SongTransitionController(
    private val previousView: TextureView,
    private val nextView: TextureView,
    previousPath: String,
    nextPath: String,
    private val onPlayingChanged: (Boolean) -> Unit
) {
    private val previous = TransitionPlayer(previousView, previousPath)
    private val next = TransitionPlayer(nextView, nextPath)
    private var generation = 0L
    fun play(direction: SongCarouselController.Direction, onFinished: () -> Unit = {}) {
        val currentGeneration = ++generation
        previous.stop()
        next.stop()
        val selected = when (direction) {
            SongCarouselController.Direction.PREVIOUS -> previous
            SongCarouselController.Direction.NEXT -> next
            else -> null
        }
        if (selected == null) { onPlayingChanged(false); onFinished(); return }
        onPlayingChanged(true)
        selected.play {
            if (generation != currentGeneration) return@play
            onPlayingChanged(false)
            onFinished()
        }
    }
    fun stop() { generation++; previous.stop(); next.stop(); onPlayingChanged(false) }
    fun release() { generation++; previous.release(); next.release(); onPlayingChanged(false) }

    private class TransitionPlayer(private val view: TextureView, path: String) : TextureView.SurfaceTextureListener {
        private val player = MediaPlayer()
        private var surface: Surface? = null
        private var prepared = false
        private var pending: (() -> Unit)? = null
        init {
            view.visibility = View.GONE
            view.surfaceTextureListener = this
            runCatching {
                val file = File(path)
                if (file.isFile) {
                    player.setDataSource(path)
                    player.isLooping = false
                    player.setVolume(0f, 0f)
                    player.setOnPreparedListener { prepared = true }
                    player.prepareAsync()
                }
            }
        }
        fun play(done: () -> Unit) {
            pending = done
            if (!prepared) { done(); return }
            runCatching {
                view.visibility = View.VISIBLE
                player.setOnCompletionListener {
                    view.visibility = View.GONE
                    pending?.invoke()
                    pending = null
                }
                player.seekTo(0)
                player.start()
            }.onFailure {
                view.visibility = View.GONE
                done()
            }
        }
        fun stop() {
            pending = null
            view.visibility = View.GONE
            runCatching { if (player.isPlaying) player.pause() }
            runCatching { player.seekTo(0) }
        }
        fun release() {
            stop()
            view.surfaceTextureListener = null
            runCatching { player.setOnPreparedListener(null); player.setOnCompletionListener(null) }
            runCatching { player.release() }
            surface?.release()
            surface = null
        }
        override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
            surface?.release()
            surface = Surface(st)
            runCatching { player.setSurface(surface) }
        }
        override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {}
        override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
            runCatching { player.setSurface(null) }
            surface?.release()
            surface = null
            return true
        }
        override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
    }
}
