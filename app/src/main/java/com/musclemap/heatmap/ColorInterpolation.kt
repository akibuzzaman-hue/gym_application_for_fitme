package com.musclemap.heatmap

import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.max
import kotlin.math.min

enum class ColorInterpolation {
    Linear,
    EaseIn,
    EaseOut,
    EaseInOut,
    Step,
    Custom;

    var stepCount: Int = 0
    var customCurve: ((Double) -> Double)? = null

    companion object {
        fun step(count: Int) = Step.apply { stepCount = count }
        fun custom(curve: (Double) -> Double) = Custom.apply { customCurve = curve }
    }

    fun apply(t: Double): Double {
        val clamped = min(max(t, 0.0), 1.0)
        return when (this) {
            Linear -> clamped
            EaseIn -> clamped * clamped
            EaseOut -> 1.0 - (1.0 - clamped) * (1.0 - clamped)
            EaseInOut -> {
                if (clamped < 0.5) {
                    2.0 * clamped * clamped
                } else {
                    1.0 - (-2.0 * clamped + 2.0).pow(2) / 2.0
                }
            }
            Step -> {
                if (stepCount <= 0) return clamped
                val stepped = kotlin.math.floor(clamped * stepCount) / stepCount.toDouble()
                min(stepped, 1.0)
            }
            Custom -> {
                customCurve?.let { min(max(it(clamped), 0.0), 1.0) } ?: clamped
            }
        }
    }
}
