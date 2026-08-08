package com.fingerdance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.LruCache
import android.widget.ImageView
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.max

class SongArtworkRepository(private val context: Context) {
    enum class Kind { DISC, TITLE }
    private val memory = object : LruCache<String, File>(128) {}
    private val cacheDir = File(context.cacheDir, "song_artwork_v2").apply { mkdirs() }

    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun get(path: String, kind: Kind, reqWidth: Int, reqHeight: Int): File? = withContext(Dispatchers.IO) {
        val source = File(path)
        if (!source.isFile || source.length() <= 0L || reqWidth <= 0 || reqHeight <= 0) return@withContext null
        val key = md5("${source.absolutePath}|${source.lastModified()}|${source.length()}|$kind|$reqWidth|$reqHeight")
        memory.get(key)?.takeIf { it.isFile }?.let { return@withContext it }
        val cached = File(cacheDir, "$key.webp")
        if (cached.isFile && cached.length() > 0L) {
            memory.put(key, cached)
            return@withContext cached
        }
        val decoded = decodeSampled(source.absolutePath, reqWidth, reqHeight) ?: return@withContext null
        val trimmed = trimTransparent(decoded)
        val scaled = scaleInside(trimmed, reqWidth, reqHeight)
        runCatching {
            FileOutputStream(cached).use { scaled.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, it) }
        }.onFailure {
            cached.delete()
            if (scaled !== trimmed && !scaled.isRecycled) scaled.recycle()
            if (trimmed !== decoded && !trimmed.isRecycled) trimmed.recycle()
            if (!decoded.isRecycled) decoded.recycle()
            return@withContext null
        }
        if (scaled !== trimmed && !scaled.isRecycled) scaled.recycle()
        if (trimmed !== decoded && !trimmed.isRecycled) trimmed.recycle()
        if (!decoded.isRecycled) decoded.recycle()
        memory.put(key, cached)
        cached
    }

    fun load(imageView: ImageView, file: File?, placeholder: Int = R.drawable.placeholder) {
        if (file == null || !file.isFile) {
            imageView.setImageResource(placeholder)
            return
        }
        Glide.with(imageView).load(file).dontAnimate().fitCenter().placeholder(placeholder).into(imageView)
    }

    fun clear(imageView: ImageView) {
        Glide.with(imageView).clear(imageView)
        imageView.setImageDrawable(null)
    }

    private fun decodeSampled(path: String, reqWidth: Int, reqHeight: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= reqWidth && bounds.outHeight / (sample * 2) >= reqHeight) sample *= 2
        return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply {
            inSampleSize = max(1, sample)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        })
    }

    private fun scaleInside(src: Bitmap, reqWidth: Int, reqHeight: Int): Bitmap {
        val scale = minOf(reqWidth.toFloat() / src.width, reqHeight.toFloat() / src.height, 1f)
        if (scale >= 1f) return src
        return Bitmap.createScaledBitmap(src, max(1, (src.width * scale).toInt()), max(1, (src.height * scale).toInt()), true)
    }

    private fun trimTransparent(src: Bitmap): Bitmap {
        if (!src.hasAlpha()) return src
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)
        var minX = w; var minY = h; var maxX = -1; var maxY = -1
        var i = 0
        for (y in 0 until h) for (x in 0 until w) {
            if ((pixels[i++] ushr 24) != 0) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
        if (maxX < minX || maxY < minY || (minX == 0 && minY == 0 && maxX == w - 1 && maxY == h - 1)) return src
        return Bitmap.createBitmap(src, minX, minY, maxX - minX + 1, maxY - minY + 1)
    }

    private fun md5(value: String): String = MessageDigest.getInstance("MD5").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
