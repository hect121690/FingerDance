package com.fingerdance

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View

class CustomBackgroundView(context: Context, attrs: AttributeSet) : View(context, attrs) {

    private val imgRects = Array(5) { RectF() }
    private val imgDrawables = arrayOfNulls<Drawable>(5)

    init {
        val ruta = "/FingerDance/Themes/$tema/GraphicsStatics/game_play"
        val drawablePaths = arrayOf(
            "$ruta/left_down.png",
            "$ruta/left_up.png",
            "$ruta/center.png",
            "$ruta/right_up.png",
            "$ruta/right_down.png"
        )

        for (i in 0 until imgDrawables.size) {
            val drawable = Drawable.createFromPath(context.getExternalFilesDir(drawablePaths[i]).toString())
            imgDrawables[i] = drawable
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val buttonWidth = width
        val buttonHeight = height / 2f

        imgRects[0].set(0f, buttonHeight, buttonWidth / 3f, height.toFloat())
        imgRects[1].set(0f, 0f, buttonWidth / 3f, buttonHeight)
        imgRects[2].set(buttonWidth / 3f, buttonHeight / 2f, (buttonWidth / 3f) * 2, buttonHeight * 1.5f)
        imgRects[3].set((buttonWidth / 3f) * 2, 0f, width.toFloat(), buttonHeight)
        imgRects[4].set((buttonWidth / 3f) * 2, buttonHeight, width.toFloat(), height.toFloat())
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        for (i in 0 until imgDrawables.size) {
            imgDrawables[i]?.let { drawable ->
                drawable.bounds = imgRects[i].toRect()
                drawable.draw(canvas)
            }
        }
    }

    private fun RectF.toRect(): android.graphics.Rect {
        return android.graphics.Rect(left.toInt(), top.toInt(), right.toInt(), bottom.toInt())
    }
}
