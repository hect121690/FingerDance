package com.fingerdance

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView

private var selectedItem = ThemeItem("","", false)
class ListPadsAdapter(
    private val items: List<ThemeItem>,
    private val onItemSelected: (ThemeItem) -> Unit // Callback
) : RecyclerView.Adapter<ListPadsAdapter.MyViewHolderPads>() {
    private var lastCheckedPosition = -1

    class MyViewHolderPads(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.item_image_pad)
        val textView: TextView = itemView.findViewById(R.id.item_text_pad)
        val checkBox: CheckBox = itemView.findViewById(R.id.item_button_pad)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolderPads {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list_pads, parent, false)
        return MyViewHolderPads(view)
    }

    override fun onBindViewHolder(holder: MyViewHolderPads, position: Int) {
        val item = items[position]
        holder.imageView.setImageDrawable(Drawable.createFromPath(item.imageRuta))
        holder.textView.text = item.text
        holder.checkBox.isChecked = item.isChecked

        // Cuando se hace clic en el CheckBox
        holder.checkBox.setOnClickListener { view ->
            val checkBox = view as CheckBox
            val clickedPosition = holder.adapterPosition

            if (checkBox.isChecked) {
                // Desmarcar la selección previa
                if (lastCheckedPosition != -1) {
                    items[lastCheckedPosition].isChecked = false
                    notifyItemChanged(lastCheckedPosition)
                }
                lastCheckedPosition = clickedPosition
                selectedItem = item

                // Notificar al Activity/Fragment del ítem seleccionado
                onItemSelected(item)
            } else {
                lastCheckedPosition = -1
            }

            items[clickedPosition].isChecked = checkBox.isChecked
        }

        // También dispara el CheckBox si se hace clic en el texto
        holder.textView.setOnClickListener {
            holder.checkBox.performClick()
        }
    }

    override fun getItemCount(): Int = items.size
}

