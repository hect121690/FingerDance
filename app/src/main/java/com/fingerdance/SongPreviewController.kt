package com.fingerdance

import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SongPreviewController(
    private val lifecycleOwner: LifecycleOwner,
    private val imageView: ImageView,
    private val textureView: TextureView,
    private val artwork: SongArtworkRepository,
    private val previewWidth: Int,
    private val previewHeight: Int
) : TextureView.SurfaceTextureListener {
    private var player: MediaPlayer? = null
    private var surface: Surface? = null
    private var job: Job? = null
    private var generation = 0L
    private var resumed = false
    private var transitionPlaying = false
    var isVideo = false
        private set
    init {
        textureView.surfaceTextureListener = this
        textureView.visibility = View.GONE
        imageView.visibility = View.VISIBLE
    }
    fun show(song: Song, selectionGeneration: Long) {
        generation = selectionGeneration
        job?.cancel()
        releasePlayer()
        val preview = File(song.rutaPreview)
        job = lifecycleOwner.lifecycleScope.launch {
            val type = withContext(Dispatchers.IO) { resolve(preview) }
            if (generation != selectionGeneration) return@launch
            when (type) {
                PreviewType.VIDEO -> showVideo(preview, selectionGeneration, song)
                PreviewType.IMAGE -> showImage(preview.absolutePath, SongArtworkRepository.Kind.DISC, selectionGeneration)
                PreviewType.NONE -> showImage(song.rutaDisc, SongArtworkRepository.Kind.DISC, selectionGeneration)
            }
        }
    }
    private suspend fun showImage(path: String, kind: SongArtworkRepository.Kind, selectionGeneration: Long) {
        releasePlayer()
        isVideo = false
        textureView.visibility = View.GONE
        imageView.visibility = View.VISIBLE
        val file = artwork.get(path, kind, previewWidth, previewHeight)
        if (generation == selectionGeneration) artwork.load(imageView, file)
    }
    private fun showVideo(file: File, selectionGeneration: Long, song: Song) {
        val current = MediaPlayer()
        player = current
        runCatching {
            surface?.let { current.setSurface(it) }
            current.setDataSource(file.absolutePath)
            current.isLooping = true
            current.setVolume(0f, 0f)
            current.setOnPreparedListener {
                if (player !== current || generation != selectionGeneration) return@setOnPreparedListener
                isVideo = true
                imageView.visibility = View.INVISIBLE
                textureView.visibility = View.VISIBLE
                startWhenAllowed()
            }
            current.setOnErrorListener { _, _, _ ->
                lifecycleOwner.lifecycleScope.launch { showImage(song.rutaDisc, SongArtworkRepository.Kind.DISC, selectionGeneration) }
                true
            }
            current.prepareAsync()
        }.onFailure {
            lifecycleOwner.lifecycleScope.launch { showImage(song.rutaDisc, SongArtworkRepository.Kind.DISC, selectionGeneration) }
        }
    }
    fun setTransitionPlaying(value: Boolean) { transitionPlaying = value; if (!value) startWhenAllowed() else pause() }
    fun onResume() { resumed = true; startWhenAllowed() }
    fun onPause() { resumed = false; pause() }
    fun pause() { runCatching { player?.takeIf { it.isPlaying }?.pause() } }
    private fun startWhenAllowed() {
        if (!resumed || transitionPlaying || !isVideo) return
        runCatching { player?.start() }
    }
    private fun resolve(file: File): PreviewType {
        if (!file.isFile || file.length() <= 0L) return PreviewType.NONE
        return when (file.extension.lowercase()) {
            "png", "jpg", "jpeg", "webp", "bmp" -> PreviewType.IMAGE
            "mp4", "m4v", "3gp", "webm", "mkv", "mpg", "mpeg", "avi" -> if (validVideo(file)) PreviewType.VIDEO else PreviewType.NONE
            else -> PreviewType.NONE
        }
    }
    private fun validVideo(file: File): Boolean = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.absolutePath)
            (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L) > 0
        } finally { retriever.release() }
    }.getOrDefault(false)
    private fun releasePlayer() {
        val old = player
        player = null
        isVideo = false
        if (old != null) {
            runCatching { old.setOnPreparedListener(null); old.setOnCompletionListener(null); old.setOnErrorListener(null) }
            runCatching { if (old.isPlaying) old.stop() }
            runCatching { old.reset() }
            runCatching { old.release() }
        }
    }
    fun release() {
        generation++
        job?.cancel()
        releasePlayer()
        surface?.release()
        surface = null
        textureView.surfaceTextureListener = null
    }
    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
        surface?.release()
        surface = Surface(st)
        runCatching { player?.setSurface(surface) }
    }
    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {}
    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
        runCatching { player?.setSurface(null) }
        surface?.release()
        surface = null
        return true
    }
    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
    private enum class PreviewType { VIDEO, IMAGE, NONE }
}
