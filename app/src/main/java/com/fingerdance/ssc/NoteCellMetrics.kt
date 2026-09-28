data class NoteCellMetrics(
    val visibleWidthRatio: Float = 1f,
    val visibleHeightRatio: Float = 1f,
    val visibleOffsetXRatio: Float = 0f,
    val visibleOffsetYRatio: Float = 0f
) {
    fun drawWidth(logicalSize: Float): Float = logicalSize / visibleWidthRatio.coerceAtLeast(0.0001f)
    fun drawHeight(logicalSize: Float): Float = logicalSize / visibleHeightRatio.coerceAtLeast(0.0001f)

    fun drawX(logicalX: Float, logicalSize: Float): Float {
        val width = drawWidth(logicalSize)
        return logicalX - width * visibleOffsetXRatio
    }

    fun drawY(logicalY: Float, logicalSize: Float): Float {
        val height = drawHeight(logicalSize)
        return logicalY - height * visibleOffsetYRatio
    }
}