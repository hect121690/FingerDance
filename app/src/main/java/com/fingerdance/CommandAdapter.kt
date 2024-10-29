package com.fingerdance

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class CommandAdapter(private val commandList: ArrayList<Command>) :
    RecyclerView.Adapter<CommandAdapter.CarouselItemViewHolder>() {

    class CarouselItemViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CarouselItemViewHolder {
        val viewHolder = LayoutInflater.from(parent.context).inflate(R.layout.item_command, parent, false)
        return CarouselItemViewHolder(viewHolder)
    }

    override fun onBindViewHolder(holder: CarouselItemViewHolder, position: Int) {
        val imageView = holder.itemView.findViewById<ImageView>(R.id.image_channel)
        val bit = BitmapFactory.decodeFile(commandList[position].rutaCommandImg + ".png")
        imageView.setImageBitmap(bit)
    }

    override fun getItemCount(): Int {
        return commandList.size
    }
}
