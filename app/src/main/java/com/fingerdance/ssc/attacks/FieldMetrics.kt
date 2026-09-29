package com.fingerdance.ssc.attacks

/**
 * Conversión entre el espacio físico de Finger Dance y el espacio lógico
 * usado por StepMania 5.1 para ArrowEffects.
 *
 * Vertical conserva la adaptación actual de Finger Dance.
 * Horizontal usa el screen lógico completo de StepMania: 640x480.
 */
data class FieldMetrics(
    val physicalReferenceWidth: Float,
    val physicalReferenceHeight: Float,
    val logicalReferenceWidth: Float,
    val arrowSizePx: Float
) {
    companion object {
        const val SM_FULL_WIDTH = 640f
        const val SM_PLAYER_WIDTH = 320f
        const val SM_HEIGHT = 480f
        const val SM_ARROW_SIZE = 64f

        fun create(
            isVertical: Boolean,
            halfDouble: Boolean,
            screenWidth: Float,
            screenHeight: Float,
            arrowSizePx: Float
        ): FieldMetrics {
            return if (isVertical) {
                FieldMetrics(
                    physicalReferenceWidth = screenWidth.coerceAtLeast(1f),
                    physicalReferenceHeight = (if (halfDouble) screenHeight * 0.575f else screenHeight * 0.50f).coerceAtLeast(1f),
                    logicalReferenceWidth = SM_PLAYER_WIDTH,
                    arrowSizePx = arrowSizePx.coerceAtLeast(1f)
                )
            } else {
                FieldMetrics(
                    physicalReferenceWidth = screenWidth,
                    physicalReferenceHeight = screenHeight,
                    logicalReferenceWidth = SM_FULL_WIDTH,
                    arrowSizePx = arrowSizePx
                )
            }
        }
    }

    val fieldScaleX: Float
        get() = physicalReferenceWidth / logicalReferenceWidth

    val fieldScaleY: Float
        get() = physicalReferenceHeight / SM_HEIGHT

    val arrowScale: Float
        get() = arrowSizePx / SM_ARROW_SIZE

    fun toStepManiaX(px: Float): Float = px / fieldScaleX
    fun toStepManiaY(px: Float): Float = px / fieldScaleY
    fun toPixelsX(sm: Float): Float = sm * fieldScaleX
    fun toPixelsY(sm: Float): Float = sm * fieldScaleY
    fun arrowUnitsToPixels(sm: Float): Float = sm * arrowScale
}
