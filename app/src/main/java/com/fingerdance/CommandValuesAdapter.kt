package com.fingerdance

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.Executors

class CommandValuesAdapter(
    private val commandList: ArrayList<CommandValues>
) : RecyclerView.Adapter<CommandValuesAdapter.CarouselItemViewHolder>() {

    private val executor = Executors.newFixedThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val bitmapCache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    ) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }

    class CarouselItemViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.image_command)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CarouselItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_command, parent, false)

        return CarouselItemViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: CarouselItemViewHolder,
        position: Int
    ) {
        val path = commandList[position].rutaCommandImg

        holder.imageView.tag = path

        val cachedBitmap = bitmapCache.get(path)

        if (cachedBitmap != null) {
            holder.imageView.setImageBitmap(cachedBitmap)
            return
        }

        holder.imageView.setImageDrawable(null)

        executor.execute {
            val bitmap = BitmapFactory.decodeFile(path)

            if (bitmap != null) {
                bitmapCache.put(path, bitmap)

                mainHandler.post {
                    if (holder.imageView.tag == path) {
                        holder.imageView.setImageBitmap(bitmap)
                    }
                }
            }
        }
    }

    override fun onViewRecycled(holder: CarouselItemViewHolder) {
        holder.imageView.tag = null
        holder.imageView.setImageDrawable(null)
        super.onViewRecycled(holder)
    }

    override fun getItemCount(): Int {
        return commandList.size
    }
}