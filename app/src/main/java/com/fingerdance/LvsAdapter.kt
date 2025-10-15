package com.fingerdance

import android.graphics.BitmapFactory
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fingerdance.databinding.ItemLvsBinding

class LvsAdapter(private val lvList: MutableList<Lvs>? = mutableListOf(), private val lvListKsf: MutableList<Ksf>? = mutableListOf(), private val width: Int) :
    RecyclerView.Adapter<LvsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = ItemLvsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if(lvList != null){
            holder.bindItem(lvList[position], null)
        }else if (lvListKsf!= null){
            holder.bindItem(null, lvListKsf[position])
        }

        val imageView = holder.itemView.findViewById<ImageView>(R.id.image_lvl)
        imageView.layoutParams.width = width
        val textView = holder.itemView.findViewById<TextView>(R.id.text_lv)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, width.toFloat() / 2)
    }

    override fun getItemCount(): Int {
        return lvList?.size ?: lvListKsf?.size ?: 0
    }
        class ViewHolder(var itemLvsBinding: ItemLvsBinding) :
        RecyclerView.ViewHolder(itemLvsBinding.root) {
        fun bindItem(lv: Lvs? = Lvs("", ""), lvKsf: Ksf? = Ksf("","","")) {
            if(lv != null){
                itemLvsBinding.imageLvl.setImageBitmap(BitmapFactory.decodeFile(lv.rutaLvImg))
                itemLvsBinding.textLv.text = lv.lvl
            }else if (lvKsf != null){
                itemLvsBinding.imageLvl.setImageBitmap(BitmapFactory.decodeFile(lvKsf.rutaBitActive))
                itemLvsBinding.textLv.text = lvKsf.level
            }


        }
    }
}
