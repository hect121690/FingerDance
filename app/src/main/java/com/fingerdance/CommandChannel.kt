package com.fingerdance

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class   CommandChannel(private val channelList: ArrayList<Channels>, private val width: Int) :
    RecyclerView.Adapter<CommandChannel.CarouselItemViewHolder>() {

    class CarouselItemViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselItemViewHolder {
        val viewHolder = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
        return CarouselItemViewHolder(viewHolder)
    }

    override fun onBindViewHolder(holder: CarouselItemViewHolder, position: Int) {
        val imageView = holder.itemView.findViewById<ImageView>(R.id.image_channel)
        var bit = BitmapFactory.decodeFile(channelList[position].banner)
        imageView.setImageBitmap(bit)
        imageView.layoutParams.width = width

    }

    override fun getItemCount(): Int {
        return channelList.size
    }
}
