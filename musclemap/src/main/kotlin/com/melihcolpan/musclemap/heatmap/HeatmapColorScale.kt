package com.melihcolpan.musclemap.heatmap

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import kotlin.math.max
import kotlin.math.min

data class HeatmapColorScale(
    val colors: List<Color>,
    var interpolation: ColorInterpolation = ColorInterpolation.Linear
) {
    fun color(intensity: Double): Color {
        if (colors.isEmpty()) return Color.Gray
        if (colors.size == 1) return colors[0]

        val curved = interpolation.apply(min(max(intensity, 0.0), 1.0))
        val scaledIndex = curved * (colors.size - 1)
        val lowerIndex = scaledIndex.toInt()
        val upperIndex = min(lowerIndex + 1, colors.size - 1)
        val fraction = scaledIndex - lowerIndex.toDouble()

        if (fraction < 0.01) {
            return colors[lowerIndex]
        }
        return lerp(colors[lowerIndex], colors[upperIndex], fraction.toFloat())
    }

    companion object {
        val MmDefaultFill = Color(0xFFEEEEEE) // Replaces placeholder color if no .mmDefaultFill defined
        
        val workout = HeatmapColorScale(
            colors = listOf(MmDefaultFill, Color.Yellow, Color(0xFFFFA500), Color.Red)
        )

        val thermal = HeatmapColorScale(
            colors = listOf(Color.Blue, Color.Green, Color.Yellow, Color.Red)
        )

        val medical = HeatmapColorScale(
            colors = listOf(Color.Green, Color.Yellow, Color.Red)
        )

        val monochrome = HeatmapColorScale(
            colors = listOf(Color(0xFFD9D9D9), Color(0xFF262626)) // 0.85 and 0.15 white
        )

        val workoutStepped = HeatmapColorScale(
            colors = listOf(MmDefaultFill, Color.Yellow, Color(0xFFFFA500), Color.Red),
            interpolation = ColorInterpolation.step(5)
        )

        val thermalSmooth = HeatmapColorScale(
            colors = listOf(Color.Blue, Color.Green, Color.Yellow, Color.Red),
            interpolation = ColorInterpolation.EaseInOut
        )
    }
}
