package com.fingerdance.ssc

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fingerdance.R
import com.fingerdance.SscChart

class ChartOffsetAdapter(
    private val charts: MutableList<SscChart>
) : RecyclerView.Adapter<ChartOffsetAdapter.ChartVH>() {

    inner class ChartVH(view: View) : RecyclerView.ViewHolder(view) {
        val check: CheckBox = view.findViewById(R.id.checkChart)
        val info: TextView = view.findViewById(R.id.txtInfo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChartVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chart_offset, parent, false)
        return ChartVH(view)
    }

    override fun getItemCount() = charts.size

    override fun onBindViewHolder(holder: ChartVH, position: Int) {
        val item = charts[position]
        holder.check.text = "Lv ${item.level} (${item.stepType.removePrefix("pump-").removeSuffix("double")})"
        holder.check.isChecked = item.checked
        holder.info.text = buildString {
            if (item.chartName.isNotEmpty())
                append("Chart: ${item.chartName}\n")
            if (item.difficulty.isNotEmpty())
                append("Difficulty: ${item.difficulty}\n")
            if (item.description.isNotEmpty())
                append("Description: ${item.description}\n")
            if (item.credit.isNotEmpty())
                append("Credit: ${item.credit}\n")
                append("Offset: ${item.offset}")
        }

        holder.check.setOnCheckedChangeListener { _, checked ->
            item.checked = checked
        }
        holder.itemView.setOnClickListener {
            holder.check.isChecked = !holder.check.isChecked
        }
    }
}