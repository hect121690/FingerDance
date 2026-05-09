package com.fingerdance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.LruCache
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.*

class SongCarouselViewVertical @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private var songs: List<Song> = emptyList()

    // 🔥 CONTINUO (NO circular)
    private var position: Float = 0f
    private var target: Float = 0f

    private val numVisible = 7
    private val views = mutableListOf<View>()
    private val offsets = (-3..3).toList()

    private var itemWidth = 0f
    private var spacing = 0f

    private val viewSongIndex = mutableMapOf<View, Int>()

    // CACHE
    private val memoryCache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 8).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    private val diskCacheDir = File(context.cacheDir, "thumbs").apply {
        if (!exists()) mkdirs()
    }

    init {
        initLayout()
    }

    private fun initLayout() {
        val screenWidth = resources.displayMetrics.widthPixels
        itemWidth = screenWidth / 4.2f
        spacing = itemWidth * 0.88f

        repeat(numVisible) {
            val view = LayoutInflater.from(context)
                .inflate(R.layout.item_carousel_song, this, false)

            val params = LayoutParams(
                itemWidth.toInt(),
                (itemWidth * 0.75f).toInt()
            )
            params.gravity = Gravity.CENTER
            view.layoutParams = params

            addView(view)
            views.add(view)
        }
    }

    fun setSongs(list: List<Song>) {
        songs = list
        position = 0f
        target = 0f
        viewSongIndex.clear()
        updateLayout()
    }

    // 🔥 SOLO para render (infinito real)
    private fun circular(index: Int): Int {
        val size = songs.size
        return ((index % size) + size) % size
    }

    // ---- INPUT LIMPIO ----

    fun moveRight() {
        if (songs.isEmpty()) return
        target += 1f
    }

    fun moveLeft() {
        if (songs.isEmpty()) return
        target -= 1f
    }

    fun getSelectedIndex(): Int {
        return circular(target.roundToInt())
    }

    // ---- UPDATE ULTRA SMOOTH ----

    fun update() {
        val diff = target - position

        // 🔥 velocidad dinámica anti-spam
        val speed = 0.22f + (abs(diff) * 0.08f).coerceAtMost(0.4f)

        position += diff * speed

        // snap final limpio
        if (abs(diff) < 0.001f) {
            position = target
        }

        updateLayout()
    }

    private fun updateLayout() {
        if (songs.isEmpty()) return

        // 🔥 evita jitter
        val base = position.toInt()

        for (i in views.indices) {
            val offset = offsets[i]
            val songIdx = circular(base + offset)

            val view = views[i]

            if (viewSongIndex[view] != songIdx) {
                bind(view, songs[songIdx])
                viewSongIndex[view] = songIdx
            }

            val rel = (base + offset) - position
            transform(view, rel)
        }
    }

    // ---- VISUAL ----

    private fun transform(view: View, relative: Float) {
        val dist = abs(relative)
        val maxDist = 3f
        val t = (dist / maxDist).coerceIn(0f, 1f)

        val baseScale = lerp(1.1f, 0.74f, t)
        // boost más notorio
        val centerBoost = (1f - dist).pow(2f)

        val scale = baseScale + (centerBoost * 0.22f)

        val yOffset = lerp(0f, -itemWidth * 0.06f, t)
        val z = lerp(7f, 1f, t)
        val rotationY = lerp(0f, -34f, t) * sign(relative)

        val radius = spacing * 2.2f // controla la curvatura

        val angle = relative * 0.55f // cuánto gira cada item

        val x = sin(angle) * radius

        view.translationX = x
        view.translationY = yOffset
        view.scaleX = scale
        view.scaleY = scale
        view.rotationY = rotationY
        view.translationZ = z

        if (dist < 0.5f) view.bringToFront()
        else view.elevation = 0f
    }

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    // ---- BIND ----

    private fun bind(view: View, song: Song) {
        val img = view.findViewById<ImageView>(R.id.banner)
        val path = song.rutaDisc

        img.tag = path
        img.setImageResource(R.drawable.placeholder)

        val key = md5(path + itemWidth)

        memoryCache.get(key)?.let {
            img.setImageBitmap(it)
            return
        }

        val file = File(diskCacheDir, "$key.webp")

        if (file.exists()) {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            if (bitmap != null) {
                memoryCache.put(key, bitmap)
                img.setImageBitmap(bitmap)
                return
            }
        }

        Glide.with(this)
            .asBitmap()
            .load(path)
            .override(itemWidth.toInt())
            .fitCenter()
            .into(object : CustomTarget<Bitmap>() {

                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                    val trimmed = trimTransparent(resource)
                    saveToDisk(trimmed, file)
                    memoryCache.put(key, trimmed)

                    img.post {
                        if (img.tag == path) {
                            img.setImageBitmap(trimmed)
                        }
                    }
                }

                override fun onLoadCleared(placeholder: Drawable?) {}
            })
    }

    private fun trimTransparent(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height

        var minX = w
        var minY = h
        var maxX = -1
        var maxY = -1

        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        var i = 0
        for (y in 0 until h) {
            for (x in 0 until w) {
                val alpha = pixels[i] ushr 24
                if (alpha != 0) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
                i++
            }
        }

        if (maxX < minX || maxY < minY) return src

        return Bitmap.createBitmap(
            src,
            minX,
            minY,
            maxX - minX + 1,
            maxY - minY + 1
        )
    }

    private fun saveToDisk(bitmap: Bitmap, file: File) {
        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, it)
        }
    }

    private fun md5(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}