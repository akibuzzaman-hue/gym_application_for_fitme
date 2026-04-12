package com.melihcolpan.musclemap.views

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.melihcolpan.musclemap.extensions.mmDefaultFill
import com.melihcolpan.musclemap.extensions.mmLighterFill
import com.melihcolpan.musclemap.extensions.mmMediumFill

data class BodyViewStyle(
    val defaultFillColor: Color = Color.mmDefaultFill,
    val strokeColor: Color = Color.Transparent,
    val strokeWidth: Float = 0f,
    val selectionColor: Color = Color.Green,
    val selectionStrokeColor: Color = Color.Green,
    val selectionStrokeWidth: Float = 2f,
    val headColor: Color = Color(0xFFBFBFBF), // white: 0.75
    val hairColor: Color = Color(0xFF404040), // white: 0.25
    val shadowColor: Color = Color.Transparent,
    val shadowRadius: Float = 0f,
    val shadowOffset: Offset = Offset.Zero
) {
    companion object {
        val default = BodyViewStyle()

        val minimal = BodyViewStyle(
            defaultFillColor = Color.mmLighterFill,
            strokeColor = Color.mmMediumFill,
            strokeWidth = 0.5f,
            selectionStrokeWidth = 1.5f
        )

        val neon = BodyViewStyle(
            defaultFillColor = Color(0xFF262626), // white: 0.15
            strokeColor = Color(0xFF4D4D4D), // white: 0.3
            strokeWidth = 0.5f,
            selectionColor = Color.Cyan,
            selectionStrokeColor = Color.Cyan,
            selectionStrokeWidth = 2f,
            headColor = Color(0xFF333333), // white: 0.2
            hairColor = Color(0xFF1A1A1A), // white: 0.1
            shadowColor = Color.Cyan.copy(alpha = 0.6f),
            shadowRadius = 8f
        )

        val medical = BodyViewStyle(
            defaultFillColor = Color(red = 0.9f, green = 0.92f, blue = 0.95f),
            strokeColor = Color(red = 0.7f, green = 0.75f, blue = 0.8f),
            strokeWidth = 0.5f,
            selectionColor = Color.Blue,
            selectionStrokeColor = Color.Blue,
            selectionStrokeWidth = 2f,
            headColor = Color(red = 0.85f, green = 0.87f, blue = 0.9f),
            hairColor = Color(red = 0.3f, green = 0.32f, blue = 0.35f)
        )
    }
}
