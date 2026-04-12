package com.melihcolpan.musclemap.heatmap

import androidx.compose.ui.geometry.Offset

enum class GradientDirection {
    TopToBottom,
    BottomToTop,
    LeftToRight,
    RightToLeft;

    val startPoint: Offset
        get() = when (this) {
            TopToBottom -> Offset(0.5f, 0f)
            BottomToTop -> Offset(0.5f, 1f)
            LeftToRight -> Offset(0f, 0.5f)
            RightToLeft -> Offset(1f, 0.5f)
        }

    val endPoint: Offset
        get() = when (this) {
            TopToBottom -> Offset(0.5f, 1f)
            BottomToTop -> Offset(0.5f, 0f)
            LeftToRight -> Offset(1f, 0.5f)
            RightToLeft -> Offset(0f, 0.5f)
        }
}

data class HeatmapConfiguration(
    var colorScale: HeatmapColorScale = HeatmapColorScale.workout,
    var interpolation: ColorInterpolation = ColorInterpolation.Linear,
    var threshold: Double? = null,
    var isGradientFillEnabled: Boolean = false,
    var gradientDirection: GradientDirection = GradientDirection.TopToBottom,
    var gradientLowIntensityFactor: Double = 0.3
) {
    companion object {
        val default = HeatmapConfiguration()
    }
}
