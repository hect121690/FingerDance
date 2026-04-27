package com.fingerdance

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.util.LruCache
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.fingerdance.databinding.ItemBinding
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.concurrent.Executors

class CustomAdapter(
    private val context: Context,
    private val songListKsf: ArrayList<Song>,
    private val heightBanners: Int,
    private val widthBanners: Int
) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    // 🔥 Thread pool
    private val executor = Executors.newFixedThreadPool(2)

    // 🔥 Memory cache
    private val imageCache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 8).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
    }

    // 🔥 Disk cache dir
    private val diskCacheDir = File(context.cacheDir, "thumbs").apply {
        if (!exists()) mkdirs()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding, heightBanners, widthBanners, executor)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val realPosition = getRealPosition(position)
        holder.bindItem(
            context,
            songListKsf[realPosition],
            imageCache,
            diskCacheDir
        )
    }

    override fun getItemCount(): Int = Int.MAX_VALUE

    fun getRealPosition(position: Int): Int {
        val size = songListKsf.size
        return ((position % size) + size) % size
    }

    class ViewHolder(
        private val binding: ItemBinding,
        private val heightB: Int,
        private val widthB: Int,
        private val executor: java.util.concurrent.ExecutorService
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bindItem(
            context: Context,
            song: Song,
            memoryCache: LruCache<String, Bitmap>,
            diskCacheDir: File
        ) {
            val path = song.rutaDisc
            binding.image.tag = path

            binding.image.layoutParams.apply {
                height = heightB
                width = widthB
            }

            val key = md5(path + widthB + heightB)

            // 🔥 1. MEMORY CACHE
            memoryCache.get(key)?.let {
                binding.image.setImageBitmap(it)
                return
            }

            // 🔥 2. DISK CACHE
            val file = File(diskCacheDir, "$key.webp")
            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                if (bitmap != null) {
                    memoryCache.put(key, bitmap)
                    binding.image.setImageBitmap(bitmap)
                    return
                }
            }

            // 🔥 3. PLACEHOLDER
            binding.image.setImageResource(R.drawable.placeholder)

            // 🔥 4. GENERAR (solo una vez en vida del cache)
            Glide.with(binding.image)
                .asBitmap()
                .load(path)
                .override(widthB, heightB)
                .fitCenter()
                .into(object : CustomTarget<Bitmap>() {

                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {

                        executor.execute {
                            val trimmed = trimTransparent(resource)

                            // 🔥 guardar en disco
                            saveToDisk(trimmed, file)

                            // 🔥 guardar en memoria
                            memoryCache.put(key, trimmed)

                            binding.image.post {
                                if (binding.image.tag == path) {
                                    binding.image.setImageBitmap(trimmed)
                                }
                            }
                        }
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                })
        }

        // 🔥 Trim optimizado
        private fun trimTransparent(src: Bitmap): Bitmap {
            val width = src.width
            val height = src.height

            var minX = width
            var minY = height
            var maxX = -1
            var maxY = -1

            val pixels = IntArray(width * height)
            src.getPixels(pixels, 0, width, 0, 0, width, height)

            var index = 0
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val alpha = pixels[index] ushr 24
                    if (alpha != 0) {
                        if (x < minX) minX = x
                        if (x > maxX) maxX = x
                        if (y < minY) minY = y
                        if (y > maxY) maxY = y
                    }
                    index++
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

        // 🔥 Guardar WEBP (rápido + liviano)
        private fun saveToDisk(bitmap: Bitmap, file: File) {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 80, out)
            }
        }

        companion object {
            // 🔥 Hash robusto
            fun md5(input: String): String {
                val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
                return bytes.joinToString("") { "%02x".format(it) }
            }
        }
    }
}