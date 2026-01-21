package com.fingerdance

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fingerdance.databinding.ItemBinding

class CustomAdapter(private val songListKsf: ArrayList<SongKsf>,
                    private val heightBanners : Int,
                    private val widthBanners: Int) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = ItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(v, heightBanners, widthBanners)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItem(songListKsf[position])
    }

    override fun getItemCount(): Int {
        return songListKsf.size
    }

    class ViewHolder(var itemBinding: ItemBinding, heightBanners : Int, widthBanners: Int) :
        RecyclerView.ViewHolder(itemBinding.root) {
        val heightB = heightBanners
        val widthB = widthBanners
        fun bindItem(songKsf: SongKsf) {
            itemBinding.image.layoutParams.height = heightB
            itemBinding.image.layoutParams.width = widthB
            itemBinding.image.setImageBitmap(BitmapFactory.decodeFile(songKsf.rutaDisc))
        }
    }
}
