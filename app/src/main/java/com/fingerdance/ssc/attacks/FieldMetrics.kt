package com.fingerdance.ssc.attacks

/**
 * Conversión entre el espacio físico de Finger Dance y el espacio lógico
 * usado por StepMania 5.1 para ArrowEffects.
 *
 * IMPORTANTE: hay dos escalas diferentes:
 *
 * 1) FIELD SCALE
 *    Traduce el sistema lógico 320/640 x 480 de StepMania al campo de Finger Dance.
 *    Se usa para posiciones/offsets que StepMania expresa respecto al campo.
 *
 * 2) ARROW SCALE
 *    Traduce ARROW_SIZE = 64 de StepMania al tamaño real de nuestras flechas.
 *    Se usa para magnitudes que StepMania deriva del tamaño de la flecha.
 *
 * Vertical:
 *   - representamos sólo el lado lógico de P1: 320 x 480.
 *   - modo normal: 480 -> la mitad superior útil de Finger Dance (height * 0.5).
 *     El origen lógico Y=0 está en el receptor (GameScreenSsc.targetTop).
 *   - halfDouble conserva su referencia previa (height * 0.575) y no forma parte
 *     de la adaptación vertical normal de ATTACKS.
 *
 * IMPORTANTE: esta altura sólo define la ESCALA matemática de ArrowEffects.
 * No recorta notas a 480: offsets > 480 siguen siendo válidos para notas que
 * todavía se encuentran debajo del área útil superior.
 *
 * Horizontal:
 *   - 640 -> 80% del ancho físico.
 *   - 480 -> toda la altura física.
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
                    physicalReferenceHeight = (
                        if (halfDouble) screenHeight * 0.575f else screenHeight * 0.50f
                    ).coerceAtLeast(1f),
                    logicalReferenceWidth = SM_PLAYER_WIDTH,
                    arrowSizePx = arrowSizePx.coerceAtLeast(1f)
                )
            } else {
                FieldMetrics(
                    physicalReferenceWidth = (screenWidth * 0.80f).coerceAtLeast(1f),
                    physicalReferenceHeight = screenHeight.coerceAtLeast(1f),
                    logicalReferenceWidth = SM_FULL_WIDTH,
                    arrowSizePx = arrowSizePx.coerceAtLeast(1f)
                )
            }
        }
    }

    /** 320/640 StepMania -> ancho físico del campo actual. */
    val fieldScaleX: Float
        get() = physicalReferenceWidth / logicalReferenceWidth

    /** 480 StepMania -> altura física del campo actual. */
    val fieldScaleY: Float
        get() = physicalReferenceHeight / SM_HEIGHT

    /** 64 StepMania (ARROW_SIZE) -> medidaFlechas de Finger Dance. */
    val arrowScale: Float
        get() = arrowSizePx / SM_ARROW_SIZE

    fun toStepManiaX(px: Float): Float =
        px / fieldScaleX.coerceAtLeast(0.0001f)

    fun toStepManiaY(px: Float): Float =
        px / fieldScaleY.coerceAtLeast(0.0001f)

    fun toPixelsX(sm: Float): Float =
        sm * fieldScaleX

    fun toPixelsY(sm: Float): Float =
        sm * fieldScaleY

    /** Convierte una magnitud basada en ARROW_SIZE a píxeles Finger Dance. */
    fun arrowUnitsToPixels(sm: Float): Float =
        sm * arrowScale
}
