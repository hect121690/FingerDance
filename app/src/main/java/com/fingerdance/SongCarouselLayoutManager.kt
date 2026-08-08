package com.fingerdance

import android.content.Context
import android.graphics.PointF
import android.util.DisplayMetrics
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin

class SongCarouselLayoutManager(
    private val context: Context,
    val carouselItemWidth: Float
) : LinearLayoutManager(context, HORIZONTAL, false) {

    init {
        initialPrefetchItemCount = 7
    }

    override fun canScrollHorizontally(): Boolean = true
    override fun canScrollVertically(): Boolean = false

    override fun onLayoutCompleted(state: RecyclerView.State?) {
        super.onLayoutCompleted(state)
        transformChildren()
    }

    override fun scrollHorizontallyBy(
        dx: Int,
        recycler: RecyclerView.Recycler?,
        state: RecyclerView.State?
    ): Int {
        val consumed = super.scrollHorizontallyBy(dx, recycler, state)
        transformChildren()
        return consumed
    }

    override fun smoothScrollToPosition(
        recyclerView: RecyclerView,
        state: RecyclerView.State,
        position: Int
    ) {
        val smoothScroller = object : LinearSmoothScroller(context) {
            override fun calculateSpeedPerPixel(
                displayMetrics: DisplayMetrics
            ): Float {
                return 55f / displayMetrics.densityDpi
            }

            override fun calculateTimeForScrolling(dx: Int): Int {
                return super.calculateTimeForScrolling(dx)
                    .coerceAtMost(220)
            }

            override fun calculateTimeForDeceleration(dx: Int): Int {
                return super.calculateTimeForDeceleration(dx)
                    .coerceAtMost(260)
            }

            override fun calculateDxToMakeVisible(
                view: View,
                snapPreference: Int
            ): Int {
                val recyclerCenter =
                    recyclerView.paddingLeft +
                            (
                                    recyclerView.width -
                                            recyclerView.paddingLeft -
                                            recyclerView.paddingRight
                                    ) / 2

                val childCenter =
                    getDecoratedLeft(view) +
                            getDecoratedMeasuredWidth(view) / 2

                return childCenter - recyclerCenter
            }

            override fun getHorizontalSnapPreference(): Int {
                return SNAP_TO_ANY
            }
        }

        smoothScroller.targetPosition = position
        startSmoothScroll(smoothScroller)
    }

    override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
        return super.computeScrollVectorForPosition(targetPosition)
    }

    fun centerPosition(position: Int, recyclerView: RecyclerView) {
        if (recyclerView.width <= 0) {
            recyclerView.post { centerPosition(position, recyclerView) }
            return
        }

        val offset = (recyclerView.width - carouselItemWidth.toInt()) / 2
        scrollToPositionWithOffset(position, offset)
        recyclerView.post { transformChildren() }
    }

    private fun transformChildren() {
        if (width <= 0 || childCount == 0) return

        val recyclerCenter = width / 2f

        /*
         * 0.48f significa que eliminamos un 48 % de la separación física
         * entre items.
         *
         * Sube a 0.55f o 0.60f para apilarlos más.
         * Baja a 0.38f o 0.42f para separarlos.
         */
        val overlapFactor = 0.22f

        for (index in 0 until childCount) {
            val child = getChildAt(index) ?: continue

            val decoratedCenter =
                (getDecoratedLeft(child) + getDecoratedRight(child)) / 2f

            /*
             * Distancia lógica respecto del centro.
             * No usamos translationX porque produciría realimentación.
             */
            val relative =
                (decoratedCenter - recyclerCenter) / carouselItemWidth

            val distance = abs(relative)
            val normalizedDistance = (distance / 4f).coerceIn(0f, 1f)

            /*
             * Compresión horizontal:
             *
             * A la izquierda, relative es negativo y translationX positiva.
             * A la derecha, relative es positivo y translationX negativa.
             *
             * Ambos lados se acercan al centro.
             */
            val overlapOffset =
                -relative * carouselItemWidth * overlapFactor

            /*
             * Curvatura leve, sin contrarrestar el movimiento del RecyclerView.
             */
            val curveOffset =
                sin(relative * 0.52f) *
                        carouselItemWidth *
                        0.08f

            child.translationX = overlapOffset + curveOffset

            /*
             * El centro sobresale un poco hacia abajo.
             * Los lados suben ligeramente, como en la referencia.
             */
            child.translationY =
                -carouselItemWidth *
                        0.055f *
                        normalizedDistance

            /*
             * Escala progresiva.
             *
             * Centro: aproximadamente 1.22
             * Primer lateral: aproximadamente 1.03
             * Extremos: aproximadamente 0.77
             */
            val sideScale = lerp(
                start = 1.02f,
                end = 0.76f,
                fraction = normalizedDistance
            )

            val centerBoost =
                (1f - distance.coerceIn(0f, 1f)).pow(2f) * 0.20f

            val scale = sideScale + centerBoost

            child.scaleX = scale
            child.scaleY = scale

            /*
             * Perspectiva lateral.
             */
            child.rotationY =
                lerp(
                    start = 0f,
                    end = -29f,
                    fraction = normalizedDistance
                ) * sign(relative)

            /*
             * El elemento central debe dibujarse encima de los laterales.
             */
            child.translationZ =
                (100f - distance * 12f).coerceAtLeast(1f)

            child.alpha = lerp(
                start = 1f,
                end = 0.78f,
                fraction = normalizedDistance
            )

            child.pivotX = child.width / 2f
            child.pivotY = child.height / 2f
        }
    }

    private fun lerp(
        start: Float,
        end: Float,
        fraction: Float
    ): Float {
        return start + (end - start) * fraction
    }
}