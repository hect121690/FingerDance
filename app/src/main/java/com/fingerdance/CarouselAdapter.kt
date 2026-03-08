package com.fingerdance

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class CarouselAdapter(
    private val songListKsf: ArrayList<SongKsf>
) : RecyclerView.Adapter<CarouselAdapter.SongViewHolder>() {

    inner class SongViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val banner: ImageView = view.findViewById(R.id.imgBanner)
        val texturePreview: TextureView = view.findViewById(R.id.texturePreview)
        val title: TextView = view.findViewById(R.id.txtTitle)

    }

    override fun getItemCount(): Int {
        return Int.MAX_VALUE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_carousel, parent, false)

        return SongViewHolder(view)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {

        val realIndex = position % songListKsf.size
        val song = songListKsf[realIndex]

        holder.title.text = song.title

        // cargar banner
        val bitmap = BitmapFactory.decodeFile(song.rutaDisc)
        holder.banner.setImageBitmap(bitmap)


    }

    fun getRealItem(position: Int): SongKsf {
        return songListKsf[position % songListKsf.size]
    }
}