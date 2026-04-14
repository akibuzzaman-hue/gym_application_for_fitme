package com.musclemap.extensions

import androidx.compose.ui.graphics.Color

val Color.Companion.mmDefaultFill: Color
    get() = Color(0xFFC7C7C7) // equivalent to Color(white: 0.78)

val Color.Companion.mmLightFill: Color
    get() = Color(0xFFD9D9D9) // equivalent to Color(white: 0.85)

val Color.Companion.mmLighterFill: Color
    get() = Color(0xFFE0E0E0) // equivalent to Color(white: 0.88)

val Color.Companion.mmMediumFill: Color
    get() = Color(0xFFB3B3B3) // equivalent to Color(white: 0.70)
