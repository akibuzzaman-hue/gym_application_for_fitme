package com.melihcolpan.musclemap.core

object SVGPathParser {

    fun parse(pathString: String): List<SVGPathCommand> {
        val commands = mutableListOf<SVGPathCommand>()
        var index = 0
        var currentCommand = 'M'

        fun skipWhitespaceAndCommas() {
            while (index < pathString.length) {
                val char = pathString[index]
                if (char == ' ' || char == ',' || char == '\n' || char == '\t' || char == '\r') {
                    index++
                } else {
                    break
                }
            }
        }

        fun parseNumber(): Float? {
            skipWhitespaceAndCommas()
            if (index >= pathString.length) return null

            val numStr = StringBuilder()
            var hasDecimal = false
            var hasExponent = false

            if (pathString[index] == '-' || pathString[index] == '+') {
                numStr.append(pathString[index])
                index++
            }

            while (index < pathString.length) {
                val char = pathString[index]

                if (char.isDigit()) {
                    numStr.append(char)
                    index++
                } else if (char == '.' && !hasDecimal && !hasExponent) {
                    hasDecimal = true
                    numStr.append(char)
                    index++
                } else if ((char == 'e' || char == 'E') && !hasExponent) {
                    hasExponent = true
                    numStr.append(char)
                    index++
                    if (index < pathString.length && (pathString[index] == '-' || pathString[index] == '+')) {
                        numStr.append(pathString[index])
                        index++
                    }
                } else {
                    break
                }
            }

            return try {
                if (numStr.isEmpty() || numStr.toString() == "-" || numStr.toString() == "+") null
                else numStr.toString().toFloat()
            } catch (e: NumberFormatException) {
                null
            }
        }

        fun parseFlag(): Boolean? {
            skipWhitespaceAndCommas()
            if (index >= pathString.length) return null
            val char = pathString[index]
            if (char == '0' || char == '1') {
                index++
                return char == '1'
            }
            return null
        }

        while (index < pathString.length) {
            skipWhitespaceAndCommas()
            if (index >= pathString.length) break

            val char = pathString[index]

            if (char.isLetter() && char != 'e' && char != 'E') {
                currentCommand = char
                index++
            }

            val isRelative = currentCommand.isLowerCase()
            val cmd = currentCommand.uppercaseChar()

            when (cmd) {
                'M' -> {
                    val x = parseNumber()
                    val y = parseNumber()
                    if (x != null && y != null) {
                        commands.add(SVGPathCommand.MoveTo(x, y, isRelative))
                        currentCommand = if (isRelative) 'l' else 'L'
                    }
                }

                'L' -> {
                    val x = parseNumber()
                    val y = parseNumber()
                    if (x != null && y != null) {
                        commands.add(SVGPathCommand.LineTo(x, y, isRelative))
                    }
                }

                'H' -> {
                    val x = parseNumber()
                    if (x != null) {
                        commands.add(SVGPathCommand.HorizontalLineTo(x, isRelative))
                    }
                }

                'V' -> {
                    val y = parseNumber()
                    if (y != null) {
                        commands.add(SVGPathCommand.VerticalLineTo(y, isRelative))
                    }
                }

                'C' -> {
                    val x1 = parseNumber()
                    val y1 = parseNumber()
                    val x2 = parseNumber()
                    val y2 = parseNumber()
                    val x = parseNumber()
                    val y = parseNumber()
                    if (x1 != null && y1 != null && x2 != null && y2 != null && x != null && y != null) {
                        commands.add(SVGPathCommand.CurveTo(x1, y1, x2, y2, x, y, isRelative))
                    }
                }

                'S' -> {
                    val x2 = parseNumber()
                    val y2 = parseNumber()
                    val x = parseNumber()
                    val y = parseNumber()
                    if (x2 != null && y2 != null && x != null && y != null) {
                        commands.add(SVGPathCommand.SmoothCurveTo(x2, y2, x, y, isRelative))
                    }
                }

                'Q' -> {
                    val x1 = parseNumber()
                    val y1 = parseNumber()
                    val x = parseNumber()
                    val y = parseNumber()
                    if (x1 != null && y1 != null && x != null && y != null) {
                        commands.add(SVGPathCommand.QuadraticCurveTo(x1, y1, x, y, isRelative))
                    }
                }

                'T' -> {
                    val x = parseNumber()
                    val y = parseNumber()
                    if (x != null && y != null) {
                        commands.add(SVGPathCommand.SmoothQuadraticCurveTo(x, y, isRelative))
                    }
                }

                'A' -> {
                    val rx = parseNumber()
                    val ry = parseNumber()
                    val angle = parseNumber()
                    val largeArc = parseFlag()
                    val sweep = parseFlag()
                    val x = parseNumber()
                    val y = parseNumber()
                    if (rx != null && ry != null && angle != null && largeArc != null && sweep != null && x != null && y != null) {
                        commands.add(SVGPathCommand.ArcTo(rx, ry, angle, largeArc, sweep, x, y, isRelative))
                    }
                }

                'Z' -> {
                    commands.add(SVGPathCommand.ClosePath)
                }

                else -> {
                    index++
                }
            }
        }

        return commands
    }
}
