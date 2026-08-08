package com.fingerdance

import android.view.View
import androidx.recyclerview.widget.LinearSnapHelper
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

class SongCenterSnapHelper : LinearSnapHelper() {

    fun snappedPosition(
        layoutManager: RecyclerView.LayoutManager
    ): Int {
        val view = findCenterView(layoutManager)
            ?: return RecyclerView.NO_POSITION

        return layoutManager.getPosition(view)
    }

    override fun findSnapView(
        layoutManager: RecyclerView.LayoutManager
    ): View? {
        return findCenterView(layoutManager)
    }

    private fun findCenterView(
        layoutManager: RecyclerView.LayoutManager
    ): View? {
        if (layoutManager.childCount == 0) return null

        val helper = OrientationHelper
            .createHorizontalHelper(layoutManager)

        val containerCenter = helper.startAfterPadding +
                helper.totalSpace / 2

        var closestView: View? = null
        var closestDistance = Int.MAX_VALUE

        for (index in 0 until layoutManager.childCount) {
            val child = layoutManager.getChildAt(index) ?: continue

            val childCenter = helper.getDecoratedStart(child) +
                    helper.getDecoratedMeasurement(child) / 2

            val distance = abs(childCenter - containerCenter)

            if (distance < closestDistance) {
                closestDistance = distance
                closestView = child
            }
        }

        return closestView
    }
}