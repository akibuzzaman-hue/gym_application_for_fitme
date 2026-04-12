package com.melihcolpan.musclemap.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.melihcolpan.musclemap.data.BodyGender
import com.melihcolpan.musclemap.data.BodySide
import com.melihcolpan.musclemap.data.Muscle
import com.melihcolpan.musclemap.data.MuscleSide
import com.melihcolpan.musclemap.heatmap.HeatmapColorScale
import com.melihcolpan.musclemap.heatmap.HeatmapConfiguration
import com.melihcolpan.musclemap.heatmap.MuscleHighlight
import com.melihcolpan.musclemap.heatmap.MuscleIntensity
import com.melihcolpan.musclemap.rendering.BodyRenderer

@Composable
fun BodyView(
    modifier: Modifier = Modifier,
    gender: BodyGender = BodyGender.Male,
    side: BodySide = BodySide.Front,
    style: BodyViewStyle = BodyViewStyle.default,
    highlights: Map<Muscle, MuscleHighlight> = emptyMap(),
    selectedMuscles: Set<Muscle> = emptySet(),
    hideSubGroups: Boolean = true,
    selectionPulseFactor: Double = 1.0,
    heatmapConfig: HeatmapConfiguration? = null,
    heatmapData: List<MuscleIntensity> = emptyList(),
    onMuscleSelected: ((Muscle, MuscleSide) -> Unit)? = null,
    onMuscleLongPressed: ((Muscle, MuscleSide) -> Unit)? = null,
    tooltipContent: @Composable ((Muscle, MuscleSide) -> Unit)? = null
) {
    // Resolve heatmap highlights if provided
    val resolvedHighlights = remember(highlights, heatmapConfig, heatmapData) {
        val finalMap = highlights.toMutableMap()
        if (heatmapData.isNotEmpty()) {
            val config = heatmapConfig ?: HeatmapConfiguration.default
            val effectiveScale = HeatmapColorScale(
                colors = config.colorScale.colors,
                interpolation = config.colorScale.interpolation
            )
            for (entry in heatmapData) {
                if (config.threshold != null && entry.intensity < config.threshold!!) continue
                
                // Similar fallback to Swift:
                val color = entry.color ?: effectiveScale.color(entry.intensity)
                finalMap[entry.muscle] = MuscleHighlight(
                    muscle = entry.muscle,
                    color = color,
                    opacity = 1.0
                )
            }
        }
        finalMap
    }

    val renderer = remember(gender, side, resolvedHighlights, style, selectedMuscles, hideSubGroups, selectionPulseFactor) {
        BodyRenderer(
            gender = gender,
            side = side,
            highlights = resolvedHighlights,
            style = style,
            selectedMuscles = selectedMuscles,
            selectionPulseFactor = selectionPulseFactor,
            hideSubGroups = hideSubGroups
        )
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(renderer) {
                    detectTapGestures(
                        onTap = { offset ->
                            val hit = renderer.hitTest(offset, size)
                            if (hit != null) {
                                onMuscleSelected?.invoke(hit.first, hit.second)
                            }
                        },
                        onLongPress = { offset ->
                            val hit = renderer.hitTest(offset, size)
                            if (hit != null) {
                                onMuscleLongPressed?.invoke(hit.first, hit.second)
                            }
                        }
                    )
                }
        ) {
            renderer.render(this)
        }
        
        // Advanced features like Zoom and Animations would require Compose Animation/Transform wrappers.
        // I have consolidated the standard body into this component as per the common translation strategy.
    }
}
