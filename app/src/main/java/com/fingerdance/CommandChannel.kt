package com.fingerdance

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class CommandChannel(private val channelList: ArrayList<Channels>, private val width: Int, private val context: Context) :
    RecyclerView.Adapter<CommandChannel.CarouselItemViewHolder>() {

    class CarouselItemViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselItemViewHolder {
        val viewHolder = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
        return CarouselItemViewHolder(viewHolder)
    }

    override fun onBindViewHolder(holder: CarouselItemViewHolder, position: Int) {
        val imageViewChannel = holder.itemView.findViewById<ImageView>(R.id.image_channel)
        val bitChannel = BitmapFactory.decodeFile(channelList[position].banner)
        imageViewChannel.setImageBitmap(bitChannel)
        imageViewChannel.layoutParams.width = width
    }

    override fun getItemCount(): Int {
        return channelList.size
    }
}
