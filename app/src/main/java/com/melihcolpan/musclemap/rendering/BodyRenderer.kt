package com.melihcolpan.musclemap.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.withTransform
import com.melihcolpan.musclemap.data.BodyGender
import com.melihcolpan.musclemap.data.BodyPathProvider
import com.melihcolpan.musclemap.data.BodySide
import com.melihcolpan.musclemap.data.BodySlug
import com.melihcolpan.musclemap.data.Muscle
import com.melihcolpan.musclemap.data.MuscleSide
import com.melihcolpan.musclemap.heatmap.MuscleFill
import com.melihcolpan.musclemap.heatmap.MuscleHighlight
import com.melihcolpan.musclemap.views.BodyViewStyle
import kotlin.math.min

class BodyRenderer(
    val gender: BodyGender,
    val side: BodySide,
    val highlights: Map<Muscle, MuscleHighlight>,
    val style: BodyViewStyle,
    val selectedMuscles: Set<Muscle>,
    var selectionPulseFactor: Double = 1.0,
    val hideSubGroups: Boolean = true
) {
    private val pathCache = PathCache()

    fun render(drawScope: DrawScope) {
        val size = drawScope.size
        val viewBox = BodyPathProvider.viewBox(gender, side)
        val scale = min(
            size.width / viewBox.size.width,
            size.height / viewBox.size.height
        )
        val offsetX = (size.width - viewBox.size.width * scale) / 2 - viewBox.origin.x * scale
        val offsetY = (size.height - viewBox.size.height * scale) / 2 - viewBox.origin.y * scale

        val bodyParts = BodyPathProvider.paths(gender, side)
        val hasShadow = style.shadowRadius > 0f

        for (bodyPart in bodyParts) {
            val m = bodyPart.slug.muscle
            if (hideSubGroups && m != null && m.isSubGroup && !m.isAlwaysVisibleSubGroup) continue

            val muscle = bodyPart.slug.muscle
            val highlight = muscle?.let { highlights[it] }
            
            val isSelected = muscle?.let {
                if (selectedMuscles.contains(it)) return@let true
                if (hideSubGroups && it.isAlwaysVisibleSubGroup) {
                    val parent = it.parentGroup
                    if (parent != null && selectedMuscles.contains(parent)) return@let true
                }
                false
            } ?: false

            val fill = resolveFill(bodyPart.slug, highlight, isSelected)
            val highlightOpacity = highlight?.opacity?.toFloat() ?: 1.0f
            val needsOpacityLayer = highlightOpacity < 1.0f && highlight != null
            val needsShadow = hasShadow && highlight != null

            val allPaths = mutableListOf<Pair<String, MuscleSide>>()
            bodyPart.common.forEach { allPaths.add(it to MuscleSide.Both) }
            bodyPart.left.forEach { allPaths.add(it to MuscleSide.Left) }
            bodyPart.right.forEach { allPaths.add(it to MuscleSide.Right) }

            for ((pathString, _) in allPaths) {
                val path = pathCache.path(pathString, scale, offsetX, offsetY)
                val boundingRect = path.getBounds()

                val brush = when (fill) {
                    is MuscleFill.SolidColor -> SolidColor(fill.color)
                    is MuscleFill.LinearGradient -> {
                        val start = Offset(
                            boundingRect.left + boundingRect.width * fill.startPoint.x,
                            boundingRect.top + boundingRect.height * fill.startPoint.y
                        )
                        val end = Offset(
                            boundingRect.left + boundingRect.width * fill.endPoint.x,
                            boundingRect.top + boundingRect.height * fill.endPoint.y
                        )
                        Brush.linearGradient(fill.colors, start, end)
                    }
                    is MuscleFill.RadialGradient -> {
                        val center = Offset(
                            boundingRect.left + boundingRect.width * fill.center.x,
                            boundingRect.top + boundingRect.height * fill.center.y
                        )
                        val calculatedRadius = kotlin.math.max(boundingRect.width, boundingRect.height) * fill.endRadius
                        if (calculatedRadius > 0) {
                            Brush.radialGradient(fill.colors, center, calculatedRadius)
                        } else {
                            SolidColor(fill.colors.firstOrNull() ?: Color.Transparent)
                        }
                    }
                }

                // In Compose DrawScope, shadow can be applied using modifiers on a Canvas, 
                // but drawing individual path shadows requires Paint() with Shadow layer which is more complex.
                // We simplify drawing here by using global opacity/brush.
                val finalOpacity = if (isSelected && selectionPulseFactor != 1.0) {
                    highlightOpacity * selectionPulseFactor.toFloat()
                } else if (needsOpacityLayer) {
                    highlightOpacity
                } else {
                    1.0f
                }

                val isAbs = muscle == Muscle.Abs || muscle?.parentGroup == Muscle.Abs
                val isObliques = muscle == Muscle.Obliques || muscle?.parentGroup == Muscle.Obliques
                
                val drawBlock: DrawScope.() -> Unit = {
                    drawPath(
                        path = path,
                        brush = brush,
                        alpha = finalOpacity,
                        style = Fill
                    )

                    if (style.strokeWidth > 0f) {
                        drawPath(
                            path = path,
                            color = style.strokeColor,
                            style = Stroke(width = style.strokeWidth)
                        )
                    }

                    if (isSelected) {
                        drawPath(
                            path = path,
                            color = style.selectionStrokeColor,
                            style = Stroke(width = style.selectionStrokeWidth)
                        )
                    }
                }

                if (isAbs) {
                    // Compute a common pivot for the Abs muscle group to prevent disjoint scaling
                    val absPivot = boundingRect(Muscle.Abs, size)?.center ?: boundingRect.center
                    drawScope.withTransform({
                        scale(0.72f, 0.72f, pivot = absPivot)
                    }) {
                        drawBlock()
                    }
                } else if (isObliques) {
                    val obliquesPivot = boundingRect(Muscle.Obliques, size)?.center ?: boundingRect.center
                    drawScope.withTransform({
                        scale(0.85f, 0.85f, pivot = obliquesPivot)
                    }) {
                        drawBlock()
                    }
                } else {
                    drawScope.drawBlock()
                }
            }
        }
    }

    fun hitTest(point: Offset, size: Size): Pair<Muscle, MuscleSide>? {
        val viewBox = BodyPathProvider.viewBox(gender, side)
        val scale = min(
            size.width / viewBox.size.width,
            size.height / viewBox.size.height
        )
        val offsetX = (size.width - viewBox.size.width * scale) / 2 - viewBox.origin.x * scale
        val offsetY = (size.height - viewBox.size.height * scale) / 2 - viewBox.origin.y * scale

        val bodyParts = BodyPathProvider.paths(gender, side)

        // Test sub-groups first
        val sortedParts = bodyParts.sortedBy { 
            val isSub = it.slug.muscle?.isSubGroup ?: false
            if (isSub) 0 else 1
        }

        for (bodyPart in sortedParts) {
            val muscle = bodyPart.slug.muscle ?: continue
            if (hideSubGroups && muscle.isSubGroup && !muscle.isAlwaysVisibleSubGroup) continue

            val resolvedMuscle = if (hideSubGroups && muscle.isAlwaysVisibleSubGroup) {
                muscle.parentGroup ?: muscle
            } else {
                muscle
            }

            for (pathString in bodyPart.left) {
               // A simplified contains check via Android Path could be using Path.intersects or region operations
               // In Jetpack Compose, doing a true contains requires creating an Android Region or similar.
               // For this translation, assuming a theoretical path.contains implementation (which usually means path.getBounds().contains(point))
               // Android's Path lacks a contains(x,y). We use a simpler bounding box hit test for now, 
               // but a real implementation would use Android Region.
               val path = pathCache.path(pathString, scale, offsetX, offsetY)
               if (containsPoint(path, point)) return resolvedMuscle to MuscleSide.Left
            }

            for (pathString in bodyPart.right) {
               val path = pathCache.path(pathString, scale, offsetX, offsetY)
               if (containsPoint(path, point)) return resolvedMuscle to MuscleSide.Right
            }

            for (pathString in bodyPart.common) {
               val path = pathCache.path(pathString, scale, offsetX, offsetY)
               if (containsPoint(path, point)) return resolvedMuscle to MuscleSide.Both
            }
        }

        return null
    }
    
    // In Android Graphics, Path does not easily support hit testing point by point.
    // It's typically done by turning Path into Region:
    private fun containsPoint(path: Path, point: Offset): Boolean {
        val androidPath = path.asAndroidPath()
        val rectF = android.graphics.RectF()
        androidPath.computeBounds(rectF, true)
        val region = android.graphics.Region()
        region.setPath(
            androidPath, 
            android.graphics.Region(rectF.left.toInt(), rectF.top.toInt(), rectF.right.toInt(), rectF.bottom.toInt())
        )
        return region.contains(point.x.toInt(), point.y.toInt())
    }

    fun boundingRect(muscle: Muscle, size: Size): Rect? {
        val viewBox = BodyPathProvider.viewBox(gender, side)
        val scale = min(
            size.width / viewBox.size.width,
            size.height / viewBox.size.height
        )
        val offsetX = (size.width - viewBox.size.width * scale) / 2 - viewBox.origin.x * scale
        val offsetY = (size.height - viewBox.size.height * scale) / 2 - viewBox.origin.y * scale

        val bodyParts = BodyPathProvider.paths(gender, side)
        var combinedRect: Rect? = null

        for (bodyPart in bodyParts) {
            if (bodyPart.slug.muscle != muscle) continue
            for (pathString in bodyPart.allPaths) {
                val path = pathCache.path(pathString, scale, offsetX, offsetY)
                val rect = path.getBounds()
                if (rect.isEmpty) continue
                combinedRect = combinedRect?.intersect(rect) ?: rect
            }
        }
        return combinedRect
    }

    private fun resolveFill(
        slug: BodySlug,
        highlight: MuscleHighlight?,
        isSelected: Boolean
    ): MuscleFill {
        if (slug == BodySlug.Hair) return MuscleFill.SolidColor(style.hairColor)
        if (slug == BodySlug.Head) return MuscleFill.SolidColor(style.headColor)
        if (isSelected) return MuscleFill.SolidColor(style.selectionColor)
        if (highlight != null) return highlight.fill
        
        val muscle = slug.muscle
        if (muscle != null) {
            val parent = muscle.parentGroup
            if (parent != null) {
                val parentHighlight = highlights[parent]
                if (parentHighlight != null) return parentHighlight.fill
            }
        }
        
        return MuscleFill.SolidColor(style.defaultFillColor)
    }
}
