package com.musclemap.heatmap

import androidx.compose.ui.graphics.Color
import com.musclemap.data.Muscle
import com.musclemap.data.MuscleSide
import kotlin.math.max
import kotlin.math.min

data class MuscleIntensity(
    val muscle: Muscle,
    val intensity: Double,
    val side: MuscleSide = MuscleSide.Both,
    val color: Color? = null
) {
    init {
        // Enforce 0.0 to 1.0 bounds in Kotlin initialization since we can't cleanly overwrite val
        require(intensity in 0.0..1.0) { "Intensity must be between 0.0 and 1.0" }
    }
}

data class MuscleHighlight(
    val muscle: Muscle,
    val fill: MuscleFill,
    val opacity: Double = 1.0
) {
    val color: Color
        get() = when (fill) {
            is MuscleFill.SolidColor -> fill.color
            is MuscleFill.LinearGradient -> fill.colors.firstOrNull() ?: Color.Transparent
            is MuscleFill.RadialGradient -> fill.colors.firstOrNull() ?: Color.Transparent
        }

    constructor(muscle: Muscle, color: Color, opacity: Double = 1.0) : this(
        muscle = muscle,
        fill = MuscleFill.SolidColor(color),
        opacity = opacity
    )
}
