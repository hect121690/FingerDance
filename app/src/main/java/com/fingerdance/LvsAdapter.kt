package com.fingerdance

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.fingerdance.databinding.ItemLvsBinding


class LvsAdapter(private val lvListKsf: MutableList<Ksf> = mutableListOf(), private val widthLevel: Int) : RecyclerView.Adapter<LvsAdapter.ViewHolder>() {

    private var purple = 0
    private var yellow = 0
    private var cyan = 0
    private var blue = 0
    private var pink = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemLvsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val params = RecyclerView.LayoutParams(widthLevel, ViewGroup.LayoutParams.WRAP_CONTENT)
        binding.root.layoutParams = params

        if (purple == 0) {
            purple = ContextCompat.getColor(parent.context, R.color.purple_200)
            yellow = ContextCompat.getColor(parent.context, R.color.bgButtonPaypal)
            cyan = ContextCompat.getColor(parent.context, R.color.button_background)
            blue = ContextCompat.getColor(parent.context, R.color.borde_textview_elegante)
            pink = ContextCompat.getColor(parent.context, R.color.pink_custom)
        }

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItem(lvListKsf[position], purple, yellow, cyan, blue, pink)
        val imageView = holder.itemView.findViewById<ImageView>(R.id.image_lvl)
        imageView.layoutParams.width = widthLevel

        val textView = holder.itemView.findViewById<TextView>(R.id.text_lv)
        textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, widthLevel.toFloat() / 2)
    }

    override fun getItemCount(): Int {
        return lvListKsf.size
    }

    class ViewHolder(var itemLvsBinding: ItemLvsBinding) :
        RecyclerView.ViewHolder(itemLvsBinding.root) {
        fun bindItem(lvKsf: Ksf, purple: Int, yellow: Int, cyan: Int, blue: Int, pink: Int) {
            val bitmap = BitmapCache.getBitmap(lvKsf.rutaBitActive)
            itemLvsBinding.imageLvl.setImageBitmap(bitmap)
            itemLvsBinding.textLv.text = lvKsf.level

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
                "RISE" -> {
                    itemLvsBinding.textExtra.visibility = View.VISIBLE
                    itemLvsBinding.textExtra.setTextColor(pink)
                    itemLvsBinding.textExtra.text = lvKsf.typeSteps
                }
                //else -> itemLvsBinding.textLv.text = lvKsf.level
            }
        }

        object BitmapCache {

            private val cacheSize = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()

            private val cache = LruCache<String, Bitmap>(cacheSize)

            fun getBitmap(path: String): Bitmap? {

                var bitmap = cache.get(path)

                if (bitmap == null) {

                    bitmap = BitmapFactory.decodeFile(path)

                    if (bitmap != null) {
                        cache.put(path, bitmap)
                    }
                }

                return bitmap
            }
        }

    }
}
