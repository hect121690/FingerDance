package com.fingerdance

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.MotionEvent
import android.view.Window
import android.widget.*
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity

class PadEditorActivity : AppCompatActivity() {

    private lateinit var root: FrameLayout

    private val leftPads = mutableListOf<ImageView>()
    private val rightPads = mutableListOf<ImageView>()

    private var rutaBtnOff = ""
    private lateinit var padBitmap: Bitmap
    private lateinit var bgFrame: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_pad_editor)

        root = findViewById(android.R.id.content)
        bgFrame = findViewById(R.id.bgFrame)
        bgFrame.setBackgroundResource(R.drawable.background_for_pads)

        rutaBtnOff = "$rutaBase/FingerDance/Themes/$tema/GraphicsStatics/game_play/btn_off.png"
        padBitmap = BitmapFactory.decodeFile(rutaBtnOff)

        buildPads()
        setupSeekbars()
        setupDrag()
        setupMirror()

        updatePositions()
    }

    private fun buildPads() {
        repeat(10) { i ->
            val img = ImageView(this).apply {
                setImageBitmap(padBitmap)
            }

            root.addView(img)

            img.layoutParams = FrameLayout.LayoutParams(
                widthBtnsHorizontal.toInt(),
                heightBtnsHorizontal.toInt()
            )

            if (i < 5) leftPads.add(img)
            else rightPads.add(img)
        }
    }

    // 🔥 POSICIÓN USANDO CENTRO (fix real)
    private fun updatePositions() {

        leftPads.forEachIndexed { i, img ->
            val pos = padPositionsHorizontal[i]

            img.x = pos[0] - (widthBtnsHorizontal / 2f)
            img.y = pos[1] - (heightBtnsHorizontal / 2f)
        }

        rightPads.forEachIndexed { i, img ->
            val pos = padPositionsHorizontal[5 + i]

            img.x = pos[0] - (widthBtnsHorizontal / 2f)
            img.y = pos[1] - (heightBtnsHorizontal / 2f)
        }
    }

    private fun updateWidth() {
        (leftPads + rightPads).forEach { img ->
            val params = img.layoutParams as FrameLayout.LayoutParams
            params.width = widthBtnsHorizontal.toInt()
            img.layoutParams = params
        }
    }

    private fun updateHeight() {
        (leftPads + rightPads).forEach { img ->
            val params = img.layoutParams as FrameLayout.LayoutParams
            params.height = heightBtnsHorizontal.toInt()
            img.layoutParams = params
        }
    }

    private fun setupSeekbars() {
        val seekWidthGlobal = findViewById<SeekBar>(R.id.seekWidthLeft)
        val seekHeightGlobal = findViewById<SeekBar>(R.id.seekHeightLeft)

        val originalWidth = widthBtnsHorizontal
        val originalHeight = heightBtnsHorizontal

        seekWidthGlobal.progress = 100
        seekHeightGlobal.progress = 100

        // ✅ SOLO cambia ancho
        seekWidthGlobal.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) {
                widthBtnsHorizontal = originalWidth * (p / 100f)
                updateWidth()
                updatePositions()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })

        // ✅ SOLO cambia alto
        seekHeightGlobal.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) {
                heightBtnsHorizontal = originalHeight * (p / 100f)
                updateHeight()
                updatePositions()
            }
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        })
    }

    private fun setupDrag() {
        var dragStartX = 0f
        var dragStartY = 0f
        var isDraggingLeft = false
        var draggedPads = listOf<Int>()

        root.setOnTouchListener { _, e ->
            when (e.action) {

                MotionEvent.ACTION_DOWN -> {
                    dragStartX = e.x
                    dragStartY = e.y
                    isDraggingLeft = e.x < root.width / 2f
                    draggedPads = if (isDraggingLeft) (0..4).toList() else (5..9).toList()
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = e.x - dragStartX
                    val deltaY = e.y - dragStartY

                    draggedPads.forEach { i ->
                        padPositionsHorizontal[i][0] += deltaX
                        padPositionsHorizontal[i][1] += deltaY
                    }

                    dragStartX = e.x
                    dragStartY = e.y

                    updatePositions()
                }
            }
            true
        }
    }

    // 🔥 MIRROR CORREGIDO (basado en centro)
    private fun setupMirror() {

        findViewById<Button>(R.id.btnMirrorRight).setOnClickListener {

            for (i in 5..9) {
                val leftIndex = i - 5

                val centerX = padPositionsHorizontal[leftIndex][0]
                val centerY = padPositionsHorizontal[leftIndex][1]

                padPositionsHorizontal[i][0] = root.width - centerX
                padPositionsHorizontal[i][1] = centerY
            }

            updatePositions()
        }

        findViewById<Button>(R.id.btnMirrorLeft).setOnClickListener {

            for (i in 0..4) {
                val rightIndex = i + 5

                val centerX = padPositionsHorizontal[rightIndex][0]
                val centerY = padPositionsHorizontal[rightIndex][1]

                padPositionsHorizontal[i][0] = root.width - centerX
                padPositionsHorizontal[i][1] = centerY
            }

            updatePositions()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (this::padBitmap.isInitialized) {
            padBitmap.recycle()
        }
    }
}