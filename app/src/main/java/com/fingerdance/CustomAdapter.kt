package com.fingerdance

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fingerdance.databinding.ItemBinding

class CustomAdapter(
    private val songListKsf: ArrayList<SongKsf>,
    private val heightBanners: Int,
    private val widthBanners: Int
) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

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
        return ViewHolder(binding, heightBanners, widthBanners)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItem(songListKsf[position], imageCache)
    }

    override fun getItemCount(): Int = songListKsf.size

    class ViewHolder(
        private val itemBinding: ItemBinding,
        heightBanners: Int,
        widthBanners: Int
    ) : RecyclerView.ViewHolder(itemBinding.root) {

        private val heightB = heightBanners
        private val widthB = widthBanners

        fun bindItem(songKsf: SongKsf, cache: LruCache<String, Bitmap>) {
            itemBinding.image.layoutParams.apply {
                height = heightB
                width = widthB
            }

            val path = songKsf.rutaDisc
            itemBinding.image.tag = path

            val cached = cache.get(path)
            if (cached != null) {
                itemBinding.image.setImageBitmap(cached)
                return
            }

            itemBinding.image.setImageDrawable(null)

            itemBinding.image.post {
                if (adapterPosition == RecyclerView.NO_POSITION) return@post
                if (itemBinding.image.tag != path) return@post

                val original = BitmapFactory.decodeFile(path) ?: return@post
                val trimmed = trimTransparent(original)

                cache.put(path, trimmed)

                if (itemBinding.image.tag == path) {
                    itemBinding.image.setImageBitmap(trimmed)
                }
            }
        }


        fun trimTransparent(src: Bitmap): Bitmap {
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

            if (maxX < minX || maxY < minY) {
                return src
            }

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
