package com.fingerdance

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.AttributeSet
import android.util.TypedValue
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.withStyledAttributes

class OptionStepperView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val titleTextView: TextView
    private val leftButton: TextView
    private val optionTextView: TextView
    private val rightButton: TextView
    private val optionContainer: LinearLayout

    private var options: List<String> = emptyList()
    private var selectedIndex: Int = 0

    private var onOptionChanged: ((index: Int, value: String) -> Unit)? = null

    init {
        orientation = HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )

        titleTextView = TextView(context).apply {
            text = "Opción"
            gravity = Gravity.CENTER_VERTICAL
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTypeface(typeface, Typeface.BOLD)
            includeFontPadding = false
        }

        leftButton = TextView(context).apply {
            text = "<"
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTypeface(typeface, Typeface.BOLD)
            includeFontPadding = false
            setPadding(7.dp(), 4.dp(), 7.dp(), 4.dp())
            isClickable = true
            isFocusable = true
        }

        optionTextView = TextView(context).apply {
            text = ""
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
            setTypeface(typeface, Typeface.BOLD)
            includeFontPadding = false
            minWidth = 90.dp()
        }

        rightButton = TextView(context).apply {
            text = ">"
            gravity = Gravity.CENTER
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTypeface(typeface, Typeface.BOLD)
            includeFontPadding = false
            setPadding(7.dp(), 4.dp(), 7.dp(), 4.dp())
            isClickable = true
            isFocusable = true
        }

        optionContainer = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            addView(leftButton)
            addView(optionTextView)
            addView(rightButton)
        }

        addView(
            titleTextView,
            LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        )

        addView(
            optionContainer,
            LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        )

        context.withStyledAttributes(attrs, R.styleable.OptionStepperView) {
            val title = getString(R.styleable.OptionStepperView_optionTitle) ?: "Opción"
            val defaultIndex = getInt(R.styleable.OptionStepperView_optionDefaultIndex, 0)

            val titleColor = getColor(
                R.styleable.OptionStepperView_optionTitleColor,
                Color.WHITE
            )

            val titleSize = getDimensionPixelSize(
                R.styleable.OptionStepperView_optionTitleSize,
                16.spToPx()
            )

            val titleStyle = getInt(
                R.styleable.OptionStepperView_optionTitleStyle,
                Typeface.BOLD
            )

            val optionColor = getColor(
                R.styleable.OptionStepperView_optionTextColor,
                Color.WHITE
            )

            val optionSize = getDimensionPixelSize(
                R.styleable.OptionStepperView_optionTextSize,
                11.spToPx()
            )

            val optionStyle = getInt(
                R.styleable.OptionStepperView_optionTextStyle,
                Typeface.BOLD
            )

            titleTextView.text = title
            titleTextView.setTextColor(titleColor)
            titleTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleSize.toFloat())
            titleTextView.setTypeface(titleTextView.typeface, titleStyle.toTypefaceStyle())

            optionTextView.setTextColor(optionColor)
            optionTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, optionSize.toFloat())
            optionTextView.setTypeface(optionTextView.typeface, optionStyle.toTypefaceStyle())

            leftButton.setTextColor(optionColor)
            leftButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, optionSize.toFloat() + 6f)
            leftButton.setTypeface(leftButton.typeface, optionStyle.toTypefaceStyle())

            rightButton.setTextColor(optionColor)
            rightButton.setTextSize(TypedValue.COMPLEX_UNIT_PX, optionSize.toFloat() + 6f)
            rightButton.setTypeface(rightButton.typeface, optionStyle.toTypefaceStyle())

            selectedIndex = defaultIndex
        }

        leftButton.setOnClickListener {
            movePrevious()
        }

        rightButton.setOnClickListener {
            moveNext()
        }
    }

    fun setTitle(title: String) {
        titleTextView.text = title
    }

    fun setOptions(
        newOptions: List<String>,
        defaultIndex: Int = 0
    ) {
        options = newOptions

        selectedIndex = when {
            options.isEmpty() -> 0
            defaultIndex < 0 -> 0
            defaultIndex > options.lastIndex -> options.lastIndex
            else -> defaultIndex
        }

        updateOptionText(notify = false)
    }

    fun setSelectedIndex(index: Int, notify: Boolean = true) {
        if (options.isEmpty()) return

        selectedIndex = when {
            index < 0 -> 0
            index > options.lastIndex -> options.lastIndex
            else -> index
        }

        updateOptionText(notify)
    }

    fun getSelectedIndex(): Int {
        return selectedIndex
    }

    fun getSelectedValue(): String {
        return options.getOrNull(selectedIndex).orEmpty()
    }

    fun setOnOptionChangedListener(listener: (index: Int, value: String) -> Unit) {
        onOptionChanged = listener
    }

    private fun movePrevious() {
        if (options.isEmpty()) return

        selectedIndex--

        if (selectedIndex < 0) {
            selectedIndex = options.lastIndex
        }

        updateOptionText(notify = true)
    }

    private fun moveNext() {
        if (options.isEmpty()) return

        selectedIndex++

        if (selectedIndex > options.lastIndex) {
            selectedIndex = 0
        }

        updateOptionText(notify = true)
    }

    private fun updateOptionText(notify: Boolean) {
        val value = options.getOrNull(selectedIndex).orEmpty()

        optionTextView.text = value

        if (notify) {
            onOptionChanged?.invoke(selectedIndex, value)
        }
    }

    private fun Int.dp(): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            this.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun Int.spToPx(): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_SP,
            this.toFloat(),
            resources.displayMetrics
        ).toInt()
    }

    private fun Int.toTypefaceStyle(): Int {
        return when (this) {
            0 -> Typeface.NORMAL
            1 -> Typeface.BOLD
            2 -> Typeface.ITALIC
            3 -> Typeface.BOLD_ITALIC
            else -> Typeface.BOLD
        }
    }
}