package com.fingerdance

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

class SongCarouselAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val artwork: SongArtworkRepository,
    private val discWidth: Int,
    private val discHeight: Int,
) : RecyclerView.Adapter<SongCarouselAdapter.Holder>() {
    private var songs: List<Song> = emptyList()
    fun submitSongs(value: List<Song>) { songs = value; notifyDataSetChanged() }
    fun realIndex(position: Int): Int = if (songs.isEmpty()) 0 else Math.floorMod(position, songs.size)
    fun songAt(position: Int): Song? = songs.getOrNull(realIndex(position))
    override fun getItemCount(): Int = if (songs.isEmpty()) 0 else Int.MAX_VALUE
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_carousel_song_recycler, parent, false)
        view.layoutParams = RecyclerView.LayoutParams(discWidth, discHeight)
        return Holder(view)
    }
    override fun onBindViewHolder(holder: Holder, position: Int) { holder.bind(songAt(position) ?: return) }
    override fun onViewRecycled(holder: Holder) { holder.recycle() }
    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val disc = view.findViewById<ImageView>(R.id.banner)
        private var job: Job? = null
        private var bindToken = 0L
        fun bind(song: Song) {
            recycle()
            val token = ++bindToken
            disc.setImageResource(R.drawable.placeholder)
            job = lifecycleOwner.lifecycleScope.launch {
                val discFile = artwork.get(song.rutaDisc, SongArtworkRepository.Kind.DISC, discWidth, discHeight)
                if (token != bindToken) return@launch
                artwork.load(disc, discFile)
            }
        }
        fun recycle() {
            bindToken++
            job?.cancel()
            job = null
            artwork.clear(disc)
        }
    }
}
