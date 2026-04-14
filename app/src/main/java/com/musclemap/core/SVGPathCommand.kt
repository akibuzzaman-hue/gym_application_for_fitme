package com.musclemap.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

sealed class SVGPathCommand {
    data class MoveTo(val x: Float, val y: Float, val relative: Boolean) : SVGPathCommand()
    data class LineTo(val x: Float, val y: Float, val relative: Boolean) : SVGPathCommand()
    data class HorizontalLineTo(val x: Float, val relative: Boolean) : SVGPathCommand()
    data class VerticalLineTo(val y: Float, val relative: Boolean) : SVGPathCommand()
    data class CurveTo(val x1: Float, val y1: Float, val x2: Float, val y2: Float, val x: Float, val y: Float, val relative: Boolean) : SVGPathCommand()
    data class SmoothCurveTo(val x2: Float, val y2: Float, val x: Float, val y: Float, val relative: Boolean) : SVGPathCommand()
    data class QuadraticCurveTo(val x1: Float, val y1: Float, val x: Float, val y: Float, val relative: Boolean) : SVGPathCommand()
    data class SmoothQuadraticCurveTo(val x: Float, val y: Float, val relative: Boolean) : SVGPathCommand()
    data class ArcTo(val rx: Float, val ry: Float, val angle: Float, val largeArc: Boolean, val sweep: Boolean, val x: Float, val y: Float, val relative: Boolean) : SVGPathCommand()
    object ClosePath : SVGPathCommand()
}
