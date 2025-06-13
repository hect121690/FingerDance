package com.fingerdance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

// MyAdapter.kt
class ListChannelsAdapter(private val btnDownload: Button, private val items: ArrayList<Pair<String, String>>, private val onItemSelected: (String) -> Unit) :
    RecyclerView.Adapter<ListChannelsAdapter.ListChannelsViewHolder>() {

    private var selectedPosition: Int = -1

    inner class ListChannelsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.item_text)
        val radioButton: RadioButton = itemView.findViewById(R.id.item_radio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListChannelsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_list_channels, parent, false)
        return ListChannelsViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListChannelsViewHolder, position: Int) {
        val item = items[position]
        fileName = item.first
        holder.textView.text = item.first
        holder.radioButton.isChecked = position == selectedPosition

        holder.itemView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.absoluteAdapterPosition
            notifyItemChanged(previousPosition)
            notifyItemChanged(selectedPosition)
            onItemSelected(item.second)
            btnDownload.isEnabled = selectedPosition != -1
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}
