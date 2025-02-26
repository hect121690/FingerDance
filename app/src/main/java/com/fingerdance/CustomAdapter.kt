package com.fingerdance

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.fingerdance.databinding.ItemBinding

class CustomAdapter(private val songList: ArrayList<Song>?,
                    private val songListKsf: ArrayList<SongKsf>?,
                    private val heightBanners : Int, private val widthBanners: Int) : RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = ItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(v, heightBanners, widthBanners)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if(songList != null){
            holder.bindItem(songList[position], null)
        }else if(songListKsf != null){
            holder.bindItem(null, songListKsf[position])
        }

    }

    override fun getItemCount(): Int {
        return songList?.size ?: songListKsf?.size ?: 0
    }

    class ViewHolder(var itemBinding: ItemBinding, heightBanners : Int, widthBanners: Int) :
        RecyclerView.ViewHolder(itemBinding.root) {
        val height = heightBanners
        val width = widthBanners
        fun bindItem(song: Song?, songKsf: SongKsf?) {
            itemBinding.image.layoutParams.height = height
            itemBinding.image.layoutParams.width = width
            if(song != null){
                itemBinding.image.setImageBitmap(BitmapFactory.decodeFile(song.rutaBanner))
            }else if(songKsf != null){
                itemBinding.image.setImageBitmap(BitmapFactory.decodeFile(songKsf.rutaDisc))
            }
        }
    }
}
