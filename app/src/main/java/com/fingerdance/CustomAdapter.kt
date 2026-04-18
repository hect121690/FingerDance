package com.fingerdance

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.LruCache
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.fingerdance.databinding.ItemBinding
import java.util.concurrent.Executors

class CustomAdapter(
    private val songListKsf: ArrayList<Song>,
    private val heightBanners: Int,
    private val widthBanners: Int
) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    // 🔥 thread pool para trim (no bloquear UI)
    private val executor = Executors.newFixedThreadPool(2)

    private val imageCache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 8).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.byteCount
        }
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
        holder.bindItem(songListKsf[realPosition], imageCache)
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

        fun bindItem(song: Song, cache: LruCache<String, Bitmap>) {

            binding.image.layoutParams.apply {
                height = heightB
                width = widthB
            }

            val path = song.rutaDisc
            binding.image.tag = path

            // 🔥 1. cache primero (instantáneo)
            val cached = cache.get(path)
            if (cached != null) {
                binding.image.setImageBitmap(cached)
                return
            }

            // 🔥 2. placeholder inmediato (evita pantalla vacía)
            binding.image.setImageResource(R.drawable.placeholder)

            // 🔥 3. Glide → decode + resize
            Glide.with(binding.image)
                .asBitmap()
                .load(path)
                .override(widthB, heightB)
                .fitCenter()
                .into(object : CustomTarget<Bitmap>() {

                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {

                        if (binding.image.tag != path) return

                        // 🔥 mostrar rápido SIN trim
                        binding.image.setImageBitmap(resource)

                        // 🔥 trim en background (NO bloquea UI)
                        executor.execute {
                            val trimmed = trimTransparent(resource)
                            cache.put(path, trimmed)

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

        // 🔥 trim optimizado (ahora sobre imagen pequeña)
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
    }
}