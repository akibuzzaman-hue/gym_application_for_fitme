package com.melihcolpan.musclemap.core

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

object PathBuilder {

    fun buildPath(
        svgPath: String,
        scale: Float,
        offsetX: Float,
        offsetY: Float
    ): Path {
        val path = Path()
        val commands = SVGPathParser.parse(svgPath)

        var currentPoint = Offset.Zero
        var lastControlPoint: Offset? = null
        var startPoint = Offset.Zero

        for (command in commands) {
            when (command) {
                is SVGPathCommand.MoveTo -> {
                    val point = if (command.relative) {
                        Offset(currentPoint.x + command.x, currentPoint.y + command.y)
                    } else {
                        Offset(command.x, command.y)
                    }
                    val scaledPoint = Offset(point.x * scale + offsetX, point.y * scale + offsetY)
                    path.moveTo(scaledPoint.x, scaledPoint.y)
                    currentPoint = point
                    startPoint = point
                    lastControlPoint = null
                }

                is SVGPathCommand.LineTo -> {
                    val point = if (command.relative) {
                        Offset(currentPoint.x + command.x, currentPoint.y + command.y)
                    } else {
                        Offset(command.x, command.y)
                    }
                    val scaledPoint = Offset(point.x * scale + offsetX, point.y * scale + offsetY)
                    path.lineTo(scaledPoint.x, scaledPoint.y)
                    currentPoint = point
                    lastControlPoint = null
                }

                is SVGPathCommand.HorizontalLineTo -> {
                    val point = if (command.relative) {
                        Offset(currentPoint.x + command.x, currentPoint.y)
                    } else {
                        Offset(command.x, currentPoint.y)
                    }
                    val scaledPoint = Offset(point.x * scale + offsetX, point.y * scale + offsetY)
                    path.lineTo(scaledPoint.x, scaledPoint.y)
                    currentPoint = point
                    lastControlPoint = null
                }

                is SVGPathCommand.VerticalLineTo -> {
                    val point = if (command.relative) {
                        Offset(currentPoint.x, currentPoint.y + command.y)
                    } else {
                        Offset(currentPoint.x, command.y)
                    }
                    val scaledPoint = Offset(point.x * scale + offsetX, point.y * scale + offsetY)
                    path.lineTo(scaledPoint.x, scaledPoint.y)
                    currentPoint = point
                    lastControlPoint = null
                }

                is SVGPathCommand.CurveTo -> {
                    val control1 = if (command.relative) {
                        Offset(currentPoint.x + command.x1, currentPoint.y + command.y1)
                    } else {
                        Offset(command.x1, command.y1)
                    }
                    val control2 = if (command.relative) {
                        Offset(currentPoint.x + command.x2, currentPoint.y + command.y2)
                    } else {
                        Offset(command.x2, command.y2)
                    }
                    val end = if (command.relative) {
                        Offset(currentPoint.x + command.x, currentPoint.y + command.y)
                    } else {
                        Offset(command.x, command.y)
                    }
                    val scaledControl1 = Offset(control1.x * scale + offsetX, control1.y * scale + offsetY)
                    val scaledControl2 = Offset(control2.x * scale + offsetX, control2.y * scale + offsetY)
                    val scaledEnd = Offset(end.x * scale + offsetX, end.y * scale + offsetY)
                    
                    path.cubicTo(
                        scaledControl1.x, scaledControl1.y,
                        scaledControl2.x, scaledControl2.y,
                        scaledEnd.x, scaledEnd.y
                    )
                    currentPoint = end
                    lastControlPoint = control2
                }

                is SVGPathCommand.SmoothCurveTo -> {
                    val control1 = lastControlPoint?.let {
                        Offset(2 * currentPoint.x - it.x, 2 * currentPoint.y - it.y)
                    } ?: currentPoint
                    val control2 = if (command.relative) {
                        Offset(currentPoint.x + command.x2, currentPoint.y + command.y2)
                    } else {
                        Offset(command.x2, command.y2)
                    }
                    val end = if (command.relative) {
                        Offset(currentPoint.x + command.x, currentPoint.y + command.y)
                    } else {
                        Offset(command.x, command.y)
                    }
                    
                    val scaledControl1 = Offset(control1.x * scale + offsetX, control1.y * scale + offsetY)
                    val scaledControl2 = Offset(control2.x * scale + offsetX, control2.y * scale + offsetY)
                    val scaledEnd = Offset(end.x * scale + offsetX, end.y * scale + offsetY)
                    
                    path.cubicTo(
                        scaledControl1.x, scaledControl1.y,
                        scaledControl2.x, scaledControl2.y,
                        scaledEnd.x, scaledEnd.y
                    )
                    currentPoint = end
                    lastControlPoint = control2
                }

                is SVGPathCommand.QuadraticCurveTo -> {
                    val control = if (command.relative) {
                        Offset(currentPoint.x + command.x1, currentPoint.y + command.y1)
                    } else {
                        Offset(command.x1, command.y1)
                    }
                    val end = if (command.relative) {
                        Offset(currentPoint.x + command.x, currentPoint.y + command.y)
                    } else {
                        Offset(command.x, command.y)
                    }
                    val scaledControl = Offset(control.x * scale + offsetX, control.y * scale + offsetY)
                    val scaledEnd = Offset(end.x * scale + offsetX, end.y * scale + offsetY)
                    
                    path.quadraticTo(scaledControl.x, scaledControl.y, scaledEnd.x, scaledEnd.y)
                    currentPoint = end
                    lastControlPoint = control
                }

                is SVGPathCommand.SmoothQuadraticCurveTo -> {
                    val control = lastControlPoint?.let {
                        Offset(2 * currentPoint.x - it.x, 2 * currentPoint.y - it.y)
                    } ?: currentPoint
                    val end = if (command.relative) {
                        Offset(currentPoint.x + command.x, currentPoint.y + command.y)
                    } else {
                        Offset(command.x, command.y)
                    }
                    val scaledControl = Offset(control.x * scale + offsetX, control.y * scale + offsetY)
                    val scaledEnd = Offset(end.x * scale + offsetX, end.y * scale + offsetY)
                    
                    path.quadraticTo(scaledControl.x, scaledControl.y, scaledEnd.x, scaledEnd.y)
                    currentPoint = end
                    lastControlPoint = control
                }

                is SVGPathCommand.ArcTo -> {
                    // Compose Path arcTo takes a Rect and angles, while SVG arcs take rx, ry, rotation etc.
                    // However, looking at the Swift implementation, it just fell back to a LineTo because 
                    // translating SVG Arcs to standard Paths without proper math is complex:
                    // Swift code: `path.addLine(to: scaledEnd)`
                    // I am keeping exact parity with the Swift implementation.
                    val end = if (command.relative) {
                        Offset(currentPoint.x + command.x, currentPoint.y + command.y)
                    } else {
                        Offset(command.x, command.y)
                    }
                    val scaledEnd = Offset(end.x * scale + offsetX, end.y * scale + offsetY)
                    path.lineTo(scaledEnd.x, scaledEnd.y)
                    currentPoint = end
                    lastControlPoint = null
                }

                is SVGPathCommand.ClosePath -> {
                    path.close()
                    currentPoint = startPoint
                    lastControlPoint = null
                }
            }
        }

        return path
    }
}
