package com.fingerdance

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.round
import kotlin.math.sign

class SongsCarouselView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val views = mutableListOf<View>()
    private var songs: ArrayList<Song> = arrayListOf()

    // 9 visibles
    private val offsets = listOf(-4,-3,-2,-1,0,1,2,3,4)

    private var carouselPosition = 0f
    private var targetPosition = 0f

    private var itemWidth = 0f

    var carouselVisible = true

    init {
        initLayout()
    }

    private fun initLayout() {

        val screenWidth = resources.displayMetrics.widthPixels

        itemWidth = screenWidth / 7f
        minimumHeight = (itemWidth * 1.8f).toInt()

        repeat(9) {

            val view = LayoutInflater.from(context).inflate(R.layout.item_carousel, this, false)

            val params = LayoutParams(
                (itemWidth.toInt() * 0.98).toInt(),
                (itemWidth * 0.55).toInt()
            )

            params.gravity = Gravity.CENTER

            view.layoutParams = params

            // perspectiva correcta
            view.cameraDistance = 120000f

            addView(view)

            views.add(view)
        }
    }

    fun setSongs(list: ArrayList<Song>) {

        songs = list

        carouselPosition = 0f
        targetPosition = 0f

        preloadAround(0)

        updateLayout()
    }

    private fun circularIndex(index: Int): Int {

        val size = songs.size

        return ((index % size) + size) % size
    }

    private fun bind(view: View, song: Song) {
        view.rotationY = 0f
        view.animate().cancel()
        val banner = view.findViewById<ImageView>(R.id.banner)
        val frame = view.findViewById<ImageView>(R.id.frame_disc)

        var bitmap = BannerCacheSelectSong.get(song.rutaDisc)
        AppResources.frameDisc?.let {
            frame.visibility = VISIBLE
            frame.setImageBitmap(it)
        }

        if (bitmap == null) {
            bitmap = BannerCacheSelectSong.load(song.rutaDisc)
        }

        bitmap?.let {
            banner.setImageBitmap(it)
        }
    }

    /**
     * Transformación estilo Pump It Up
     */
    private fun applyTransform(view: View, relative: Float) {

        val d = abs(relative)

        val spacing = itemWidth * 0.97f

        // mover por GPU (no relayout)
        view.translationX = relative * spacing

        view.pivotX = view.width / 2f
        view.pivotY = view.height.toFloat()

        view.rotation = relative.coerceIn(-2.1f, 2.1f) * 4f

        view.translationY = (d * d * itemWidth * 0.026f) + (d * itemWidth * 0.01f)

        view.scaleX = 1f
        view.scaleY = 1f

        if (d < 0.5f) view.bringToFront()
    }

    private fun updateLayout() {

        if (songs.isEmpty()) return

        val baseIndex = floor(carouselPosition.toDouble()).toInt()

        for (i in views.indices) {

            val offset = offsets[i]

            val index = circularIndex(baseIndex + offset)

            val song = songs[index]

            val view = views[i]

            if (view.tag != index) {
                bind(view, song)
                view.tag = index
            }

            val relative = (baseIndex + offset) - carouselPosition

            applyTransform(view, relative)
        }
    }

    fun moveRight() {
        targetPosition += 1f
        preloadAround(targetPosition.toInt())
    }

    fun moveLeft() {
        targetPosition -= 1f
        preloadAround(targetPosition.toInt())
    }

    fun getFocusedIndex(): Int {

        if (songs.isEmpty()) return -1

        val index = round(targetPosition).toInt()

        return circularIndex(index)
    }

    fun update() {
        if(!carouselVisible) return
        val speed = 0.22f

        carouselPosition += (targetPosition - carouselPosition) * speed

        if (abs(targetPosition - carouselPosition) < 0.001f) {
            carouselPosition = targetPosition
        }

        updateLayout()
    }

    private fun preloadAround(center: Int) {

        val radius = 4

        for (i in -radius..radius) {

            val index = circularIndex(center + i)

            val song = songs[index]

            BannerCacheSelectSong.load(song.rutaDisc)
        }
    }

    fun showCarousel(){
        val centerIndex = offsets.indexOf(0)
        val baseDelay = 60L
        for(i in views.indices){
            val view = views[i]
            view.animate().cancel()
            val distance = abs(i-centerIndex)
            val dir = sign((i-centerIndex).toFloat())
            val delay = distance * baseDelay
            if(i == centerIndex){

                // estado inicial
                view.scaleX = 0f
                view.scaleY = 0f
                view.alpha = 0f
                view.animate()
                    .scaleX(1.3f)
                    .scaleY(1.3f)
                    .alpha(1f)
                    .setDuration(250)
                    .withEndAction {
                        view.animate()
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(120)
                            .withEndAction {
                                carouselVisible = true
                                updateLayout()
                            }
                            .start()
                    }
                    .start()
            }else{
                // estado inicial fuera de pantalla
                view.translationX = dir * width * 1.6f
                view.scaleX = 0.5f
                view.scaleY = 0.5f
                view.alpha = 0f
                view.animate()
                    .translationX(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .setStartDelay(delay)
                    .setDuration(350)
                    .start()
            }
        }
    }

    fun hideCarousel(){
        carouselVisible = false
        val centerIndex = offsets.indexOf(0)
        val baseDelay = 60L
        for(i in views.indices){
            val view = views[i]
            view.animate().cancel()
            if(i == centerIndex){
                view.animate()
                    .scaleX(1.3f)
                    .scaleY(1.3f)
                    .setDuration(150)
                    .withEndAction {
                        view.animate()
                            .scaleX(0f)
                            .scaleY(0f)
                            .alpha(0f)
                            .setDuration(350)
                            .start()
                    }
                    .start()
            }else{
                val distance = abs(i-centerIndex)
                val dir = sign((i-centerIndex).toFloat())
                val delay = distance*baseDelay
                view.animate()
                    .translationXBy(dir * width * 1.6f)
                    .scaleX(0.5f)
                    .scaleY(0.5f)
                    .alpha(0f)
                    .setStartDelay(delay)
                    .setDuration(350)
                    .start()
            }
        }
    }
}