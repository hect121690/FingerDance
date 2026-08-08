package com.fingerdance

import android.animation.ValueAnimator
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs
import kotlin.math.roundToInt

class SongCarouselController(
    private val recyclerView: RecyclerView,
    private val adapter: SongCarouselAdapter,
    private val layoutManager: SongCarouselLayoutManager,
    private val snapHelper: SongCenterSnapHelper
) {
    enum class Direction { PREVIOUS, NEXT, NONE }

    interface Listener {
        fun onTargetChanged(realIndex: Int, direction: Direction, newSequence: Boolean)
        fun onSettled(realIndex: Int, direction: Direction)
    }

    var listener: Listener? = null

    private var currentPosition = RecyclerView.NO_POSITION
    private var targetPosition = RecyclerView.NO_POSITION
    private var currentDirection = Direction.NONE
    private var initialized = false
    private var animator: ValueAnimator? = null
    private var lastAnimationValue = 0
    private var navigationSequenceActive = false
    private val itemStepPx: Int
        get() = layoutManager.carouselItemWidth.roundToInt().coerceAtLeast(1)

    init {
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = null
        recyclerView.overScrollMode = RecyclerView.OVER_SCROLL_NEVER
        recyclerView.clipChildren = false
        recyclerView.clipToPadding = false
        recyclerView.setHasFixedSize(true)
        recyclerView.isNestedScrollingEnabled = false

        /*
         * Conservamos SnapHelper para comprobar el centro al terminar,
         * pero no dejamos que lance correcciones durante la animación.
         */
        snapHelper.attachToRecyclerView(recyclerView)
    }

    fun setSongs(songs: List<Song>, initialRealIndex: Int = 0) {
        stop()
        adapter.submitSongs(songs)

        if (songs.isEmpty()) {
            initialized = false
            currentPosition = RecyclerView.NO_POSITION
            targetPosition = RecyclerView.NO_POSITION
            return
        }

        val middle = Int.MAX_VALUE / 2
        val normalizedMiddle = middle - Math.floorMod(middle, songs.size)

        currentPosition = normalizedMiddle + Math.floorMod(initialRealIndex, songs.size)
        targetPosition = currentPosition

        recyclerView.post {
            layoutManager.centerPosition(currentPosition, recyclerView)

            recyclerView.post {
                initialized = true
                listener?.onSettled(adapter.realIndex(currentPosition), Direction.NONE)
            }
        }
    }

    /*
     * Conservamos la dirección visual que ya comprobaste:
     * izquierda = posición +1
     * derecha = posición -1
     */
    fun moveLeft() {
        moveBy(-1, Direction.PREVIOUS)
    }

    fun moveRight() {
        moveBy(1, Direction.NEXT)
    }

    private fun moveBy(delta: Int, direction: Direction) {
        if (!initialized || adapter.itemCount == 0) return
        if (targetPosition == RecyclerView.NO_POSITION) return

        val wasNewSequence = !navigationSequenceActive
        navigationSequenceActive = true
        currentDirection = direction

        targetPosition = (targetPosition + delta).coerceIn(0, adapter.itemCount - 1)

        listener?.onTargetChanged(
            adapter.realIndex(targetPosition),
            direction,
            wasNewSequence
        )

        animateToTarget()
    }

    private fun animateToTarget() {
        if (currentPosition == RecyclerView.NO_POSITION) return
        if (targetPosition == RecyclerView.NO_POSITION) return

        animator?.cancel()
        recyclerView.stopScroll()

        /*
         * Primero identificamos qué elemento está físicamente centrado.
         * Esto evita calcular el siguiente recorrido desde un índice viejo.
         */
        val snapped = snapHelper.snappedPosition(layoutManager)
        if (snapped != RecyclerView.NO_POSITION) currentPosition = snapped

        val positionDistance = targetPosition - currentPosition

        if (positionDistance == 0) {
            finishNavigation()
            return
        }

        /*
         * En tu orientación:
         * posición mayor -> contenido se desplaza hacia la izquierda.
         */
        val totalDx = positionDistance * itemStepPx

        lastAnimationValue = 0

        animator = ValueAnimator.ofInt(0, totalDx).apply {
            duration = (90L + abs(positionDistance) * 22L).coerceAtMost(190L)
            interpolator = DecelerateInterpolator(1.4f)

            addUpdateListener { valueAnimator ->
                val value = valueAnimator.animatedValue as Int
                val deltaPx = value - lastAnimationValue
                lastAnimationValue = value

                if (deltaPx != 0) recyclerView.scrollBy(deltaPx, 0)
            }

            addListener(object : android.animation.AnimatorListenerAdapter() {
                private var cancelled = false

                override fun onAnimationCancel(animation: android.animation.Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: android.animation.Animator) {
                    if (cancelled) return
                    layoutManager.centerPosition(targetPosition, recyclerView)
                    currentPosition = targetPosition
                    recyclerView.post { finishNavigation() }
                }
            })

            start()
        }
    }

    private fun finishNavigation() {
        animator = null
        currentPosition = targetPosition
        navigationSequenceActive = false

        val finishedDirection = currentDirection
        currentDirection = Direction.NONE

        listener?.onSettled(
            adapter.realIndex(currentPosition),
            finishedDirection
        )
    }

    fun selectedIndex(): Int {
        val position = if (targetPosition != RecyclerView.NO_POSITION) {
            targetPosition
        } else {
            currentPosition
        }

        return if (position == RecyclerView.NO_POSITION) 0 else adapter.realIndex(position)
    }

    fun settledIndex(): Int {
        return if (currentPosition == RecyclerView.NO_POSITION) {
            selectedIndex()
        } else {
            adapter.realIndex(currentPosition)
        }
    }

    fun selectedSong(): Song? {
        val position = if (targetPosition != RecyclerView.NO_POSITION) {
            targetPosition
        } else {
            currentPosition
        }

        return if (position == RecyclerView.NO_POSITION) null else adapter.songAt(position)
    }

    fun isMoving(): Boolean = animator?.isRunning == true

    fun stop() {
        animator?.cancel()
        animator = null
        recyclerView.stopScroll()

        val snapped = snapHelper.snappedPosition(layoutManager)
        if (snapped != RecyclerView.NO_POSITION) {
            currentPosition = snapped
            targetPosition = snapped
            layoutManager.centerPosition(snapped, recyclerView)
        }

        navigationSequenceActive = false
        currentDirection = Direction.NONE
    }

    fun release() {
        stop()
        listener = null
    }
}