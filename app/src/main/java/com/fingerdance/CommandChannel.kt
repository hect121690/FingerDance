package com.fingerdance

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class CommandChannel(
    private val channelList: List<Channels>,
    private val width: Int,
    private val context: Context
) : RecyclerView.Adapter<CommandChannel.ChannelViewHolder>() {

    class ChannelViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageViewChannel: ImageView = view.findViewById(R.id.image_channel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
        return ChannelViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {

        val realPosition = position % channelList.size

        val item = channelList[realPosition]

        Glide.with(context)
            .load(item.banner)
            .override(width, width) // importante
            .fitCenter()
            .diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL)
            .into(holder.imageViewChannel)
        holder.imageViewChannel.layoutParams.width = width
    }

    override fun getItemCount(): Int {
        // 🔥 infinito fake
        return Int.MAX_VALUE
    }

    fun getRealPosition(position: Int): Int {
        return position % channelList.size
    }
}