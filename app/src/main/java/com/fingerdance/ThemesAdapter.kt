package com.fingerdance

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

private var selectedItem = ThemeItem("","", false)

class ThemesAdapter(private val items: List<ThemeItem>) :
    RecyclerView.Adapter<ThemesAdapter.MyViewHolder>() {
    private var lastCheckedPosition = -1

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.spinner_item_image)
        val textView: TextView = itemView.findViewById(R.id.spinner_item_text)
        val checkBox: CheckBox = itemView.findViewById(R.id.spinner_item_checkbox)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.spinner_item_layout, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = items[position]
        holder.imageView.setImageDrawable(Drawable.createFromPath(item.imageRuta))
        holder.textView.text = item.text
        holder.checkBox.isChecked = item.isChecked
/*
        if(tema == holder.textView.text){
            holder.checkBox.isChecked = true
        }
*/
        holder.textView.setOnClickListener {
            holder.checkBox.performClick()
        }
        holder.checkBox.setOnClickListener { view ->
            val checkBox = view as CheckBox
            val clickedPosition = holder.adapterPosition
            if (checkBox.isChecked) {
                if (lastCheckedPosition != -1) {
                    items[lastCheckedPosition].isChecked = false
                    notifyItemChanged(lastCheckedPosition)
                }
                lastCheckedPosition = clickedPosition
                selectedItem = item
            } else {
                lastCheckedPosition = -1
            }
            items[clickedPosition].isChecked = checkBox.isChecked
        }
    }

    override fun getItemCount(): Int = items.size

    companion object {
        fun getSelectedItem(): ThemeItem{
            return selectedItem
        }
    }


}