    package com.fingerdance

    import android.content.Context
    import android.util.AttributeSet
    import android.view.Gravity
    import android.view.LayoutInflater
    import android.view.View
    import android.widget.FrameLayout
    import android.widget.ImageView
    import java.lang.Math.abs
    import java.lang.Math.floor
    import kotlin.math.round
    import kotlin.math.sign

    class ChannelCarouselView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
    ) : FrameLayout(context, attrs, defStyleAttr) {

        private val views = mutableListOf<View>()
        private var channels: ArrayList<Channels> = arrayListOf()
        private val offsets = listOf(-4, -3, -2, -1, 0, 1, 2, 3, 4)

        private var carouselPosition = 0f
        private var targetPosition = 0f

        private var velocity = 0f

        private var itemWidth = 0f
        private var spacing = 0f

        init {
            initLayout()
        }

        private fun initLayout() {
            val screenWidth = resources.displayMetrics.widthPixels
            itemWidth = screenWidth / 5f
            spacing = itemWidth * 0.65f

            repeat(9) {
                val view = LayoutInflater.from(context).inflate(R.layout.item_carousel, this, false)
                val params = LayoutParams(
                    itemWidth.toInt(),
                    (itemWidth * 0.8f).toInt()
                )

                params.gravity = Gravity.CENTER
                view.layoutParams = params
                addView(view)
                views.add(view)
            }
        }

        fun setChannels(list: ArrayList<Channels>) {
            channels = list
            carouselPosition = 0f
            targetPosition = 0f
            velocity = 0f

            preloadAround(0)

            updateLayout()
        }

        private fun circularIndex(index: Int): Int {

            val size = channels.size
            return ((index % size) + size) % size
        }

        private fun bind(view: View, channel: Channels) {
            val banner = view.findViewById<ImageView>(R.id.channelBanner)
            var bitmap = BannerCache.get(channel.banner)
            if (bitmap == null) {
                bitmap = BannerCache.load(channel.banner)
            }

            if (bitmap != null) {
                banner.setImageBitmap(bitmap)
            }
        }

        private fun applyTransform(view: View, relative: Float) {

            val distance = abs(relative)

            val scale = 1f - distance * 0.12f

            val rotation = when {
                distance < 0.5f -> 0f
                distance < 1.5f -> sign(relative) * -65f   // primera
                else -> sign(relative) * -55f               // resto
            }

            val alpha = when {
                distance < 1.5f -> 1f
                distance < 2.5f -> 0.75f
                distance < 3.5f -> 0.6f
                else -> 0.45f
            }

            view.translationX = relative * spacing

            view.scaleX = scale
            view.scaleY = scale

            view.rotationY = rotation

            view.alpha = alpha

            view.translationZ = (4f - distance)

            if (distance < 0.5f) {
                view.bringToFront()
            }
        }

        private fun updateLayout() {

            if (channels.isEmpty()) return

            val baseIndex = floor(carouselPosition.toDouble()).toInt()

            for (i in views.indices) {

                val offset = offsets[i]

                val index = circularIndex(baseIndex + offset)

                val channel = channels[index]

                val view = views[i]

                bind(view, channel)

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

            if (channels.isEmpty()) return -1

            val index = round(targetPosition).toInt()

            return circularIndex(index)
        }

        fun update() {

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

                val channel = channels[index]

                BannerCache.load(channel.banner)
            }
        }
    }