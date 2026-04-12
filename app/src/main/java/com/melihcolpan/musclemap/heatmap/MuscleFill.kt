package com.melihcolpan.musclemap.heatmap

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

sealed class MuscleFill {
    data class SolidColor(val color: Color) : MuscleFill()
    data class LinearGradient(val colors: List<Color>, val startPoint: Offset, val endPoint: Offset) : MuscleFill()
    data class RadialGradient(val colors: List<Color>, val center: Offset, val startRadius: Float, val endRadius: Float) : MuscleFill()

    // Note: SwiftUI UnitPoint in Swift implementation maps to offsets in a specific rect.
    // In Jetpack Compose, we can use Brush.linearGradient and rely on Compose drawing scope if start/end point bounds matter,
    // or provide normalized Offset points scaling manually if required in drawing code.
    // Standardizing on Compose Brush.
}
