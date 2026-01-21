package com.fingerdance

import android.graphics.BitmapFactory
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.fingerdance.databinding.ItemLvsBinding


class LvsAdapter(private val lvListKsf: MutableList<Ksf> = mutableListOf(),
                 private val widthLevel: Int) : RecyclerView.Adapter<LvsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = ItemLvsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItem(lvListKsf[position])
        val imageView = holder.itemView.findViewById<ImageView>(R.id.image_lvl)
        imageView.layoutParams.width = widthLevel

        //val imageViewType = holder.itemView.findViewById<ImageView>(R.id.image_type)
        //imageViewType.layoutParams.height = (widthLevel * 0.25).toInt()

        val textView = holder.itemView.findViewById<TextView>(R.id.text_lv)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, widthLevel.toFloat() / 2)
    }

    override fun getItemCount(): Int {
        return lvListKsf.size
    }

    class ViewHolder(var itemLvsBinding: ItemLvsBinding) :
        RecyclerView.ViewHolder(itemLvsBinding.root) {
        fun bindItem(lvKsf: Ksf) {
            itemLvsBinding.imageLvl.setImageBitmap(BitmapFactory.decodeFile(lvKsf.rutaBitActive))
            itemLvsBinding.textLv.text = lvKsf.level
            val purple = ContextCompat.getColor(itemView.context, R.color.purple_200)
            val yellow = ContextCompat.getColor(itemView.context, com.mercadopago.android.px.R.color.yellow_light)
            val cyan = ContextCompat.getColor(itemView.context, com.google.android.libraries.places.R.color.quantum_cyan)
            val blue = ContextCompat.getColor(itemView.context, com.mercadopago.android.px.R.color.blue)

            when (lvKsf.typeSteps) {
                "UCS" -> {
                    itemLvsBinding.textExtra.visibility = View.VISIBLE
                    itemLvsBinding.textExtra.setTextColor(purple)
                    itemLvsBinding.textExtra.text = lvKsf.typeSteps
                }
                "NEW" -> {
                    itemLvsBinding.textExtra.visibility = View.VISIBLE
                    itemLvsBinding.textExtra.setTextColor(yellow)
                    itemLvsBinding.textExtra.text = lvKsf.typeSteps
                }
                "ANOTHER" -> {
                    itemLvsBinding.textExtra.visibility = View.VISIBLE
                    itemLvsBinding.textExtra.setTextColor(cyan)
                    itemLvsBinding.textExtra.text = lvKsf.typeSteps
                }
                "QUEST" -> {
                    itemLvsBinding.textExtra.visibility = View.VISIBLE
                    itemLvsBinding.textExtra.setTextColor(blue)
                    itemLvsBinding.textExtra.text = lvKsf.typeSteps
                }
                //else -> itemLvsBinding.textLv.text = lvKsf.level
            }
        }

    }
}
