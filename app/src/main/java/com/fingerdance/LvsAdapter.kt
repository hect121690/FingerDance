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

private var purple = 0
private var yellow = 0
private var cyan = 0
private var blue = 0
private var pink = 0
private var red = 0
private var green = 0
private var orange = 0
class LvsAdapter(private val lvListKsf: MutableList<Ksf> = mutableListOf(), private val widthLevel: Int) : RecyclerView.Adapter<LvsAdapter.ViewHolder>() {

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
            red = ContextCompat.getColor(parent.context, R.color.negative_red)
            green = ContextCompat.getColor(parent.context, R.color.pressedColor)
            orange = ContextCompat.getColor(parent.context, com.mercadopago.android.px.R.color.ui_meli_orange)
        }

        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItem(lvListKsf[position])
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
        fun bindItem(lvKsf: Ksf) {
            val bitmap = BitmapCache.getBitmap(lvKsf.rutaBitActive)
            itemLvsBinding.imageLvl.setImageBitmap(bitmap)
            itemLvsBinding.textLv.text = lvKsf.level

            when  {
                lvKsf.typeSteps.contains("UCS", ignoreCase = true) -> {
                    itemLvsBinding.textExtra.visibility = View.VISIBLE
                    itemLvsBinding.textExtra.setTextColor(purple)
                    itemLvsBinding.textExtra.text = "UCS"
                }
                lvKsf.typeSteps.contains("NEW", ignoreCase = true) -> {
                    itemLvsBinding.textExtra.visibility = View.VISIBLE
                    itemLvsBinding.textExtra.setTextColor(yellow)
                    itemLvsBinding.textExtra.text = "NEW"
                }
                lvKsf.typeSteps.contains("ANOTHER", ignoreCase = true) -> {
                    itemLvsBinding.textExtra.visibility = View.VISIBLE
                    itemLvsBinding.textExtra.setTextColor(cyan)
                    itemLvsBinding.textExtra.text = "ANOTHER"
                }
                lvKsf.typeSteps.contains("QUEST", ignoreCase = true) -> {
                    itemLvsBinding.textExtra.visibility = View.VISIBLE
                    itemLvsBinding.textExtra.setTextColor(blue)
                    itemLvsBinding.textExtra.text = "QUEST"
                }
                lvKsf.typeSteps.contains("RISE", ignoreCase = true) -> {
                    itemLvsBinding.textExtra.visibility = View.VISIBLE
                    itemLvsBinding.textExtra.setTextColor(pink)
                    itemLvsBinding.textExtra.text = "RISE"
                }
                lvKsf.typeSteps.contains("MISSION", ignoreCase = true) -> {
                    itemLvsBinding.textExtra.visibility = View.VISIBLE
                    itemLvsBinding.textExtra.setTextColor(pink)
                    itemLvsBinding.textExtra.text = "MISSION"
                }
                lvKsf.typeSteps.contains("CHALLENGE", ignoreCase = true) -> {
                    itemLvsBinding.textExtra.visibility = View.VISIBLE
                    itemLvsBinding.textExtra.setTextColor(orange)
                    itemLvsBinding.textExtra.text = "CHALLENGE"
                }
                lvKsf.typeSteps.contains("SPECIAL", ignoreCase = true) -> {
                    itemLvsBinding.textExtra.visibility = View.VISIBLE
                    itemLvsBinding.textExtra.setTextColor(green)
                    itemLvsBinding.textExtra.text = "SPECIAL"
                }
                lvKsf.typeSteps.contains("INFINITY", ignoreCase = true) -> {
                    itemLvsBinding.textExtra.visibility = View.VISIBLE
                    itemLvsBinding.textExtra.setTextColor(cyan)
                    itemLvsBinding.textExtra.text = "INFINITY"
                }
                lvKsf.typeSteps.isNotEmpty()
                        && !lvKsf.stepmaker.equals("andamiro", ignoreCase = true) -> {
                    itemLvsBinding.textExtra.visibility = View.VISIBLE
                    itemLvsBinding.textExtra.setTextColor(green)
                    itemLvsBinding.textExtra.text = lvKsf.typeSteps.uppercase().replace(Regex("S\\d+"), "").take(3)
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
