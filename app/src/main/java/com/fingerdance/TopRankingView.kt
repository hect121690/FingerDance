package com.fingerdance

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.fingerdance.databinding.ItemRankingBinding

class TopRankingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var titleText: TextView
    private lateinit var iconText: TextView
    private lateinit var iconImage: ImageView

    init {
        initView()
    }

    private fun initView() {
        LayoutInflater.from(context).inflate(R.layout.view_top_ranking, this, true)
        recyclerView = findViewById(R.id.recyclerViewRanking)
        titleText = findViewById(R.id.tituloRanking)
        iconText = findViewById(R.id.iconText)
        iconImage = findViewById(R.id.rankingIcon)
        iconImage.layoutParams.width = (medidaFlechas * 1.3).toInt()
        iconImage.layoutParams.height = (medidaFlechas * 1.3).toInt()
        recyclerView.layoutManager = LinearLayoutManager(context)
        recyclerView.setOnTouchListener { _, _ -> true }
    }

    fun setNiveles(niveles: Nivel) {
        iconText.text = niveles.nivel
        recyclerView.adapter = RankingAdapter(niveles.fisrtRank)
    }

    fun setIconDrawable(drawable: Drawable) {
        iconImage.setImageDrawable(drawable)
    }
}

class RankingAdapter(private val nivelList: ArrayList<FirstRank>) : RecyclerView.Adapter<RankingAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = ItemRankingBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bindItem(nivelList[position], position + 1)
    }

    override fun getItemCount(): Int {
        return nivelList.size
    }

    class ViewHolder(private var itemBinding: ItemRankingBinding) :
        RecyclerView.ViewHolder(itemBinding.root) {
        fun bindItem(firstRank: FirstRank, i: Int) {
            itemBinding.positionText.text = i.toString()
            itemBinding.nameText.text = firstRank.nombre
            itemBinding.puntajeText.text = firstRank.puntaje
            itemBinding.gradeText.text = firstRank.grade
        }
    }
}
