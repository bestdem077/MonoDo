package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MonoDoThemeTokens

enum class SwissEmptyArtType {
    TASKS,
    NOTES,
    HABITS,
    BIRTHDAYS
}

/**
 * Custom Swiss minimalist geometric line-art illustrations that adapt automatically
 * to Light Paper, Dark Slate, and Pure Black AMOLED palettes.
 */
@Composable
fun SwissEmptyStateArt(
    type: SwissEmptyArtType,
    modifier: Modifier = Modifier,
    size: Dp = 88.dp
) {
    val colors = MonoDoThemeTokens.colors
    val ink = colors.primaryInk
    val muted = colors.secondaryInk.copy(alpha = 0.55f)
    val hairline = colors.border
    val accent = colors.accent
    val accentSoft = colors.accentSoft

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeThin = Stroke(width = w * 0.022f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val strokeBold = Stroke(width = w * 0.032f, cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (type) {
            SwissEmptyArtType.TASKS -> {
                // Layered Swiss paper sheets + crisp Royal Blue checkmark badge
                drawRoundRect(
                    color = hairline,
                    topLeft = Offset(w * 0.22f, h * 0.14f),
                    size = Size(w * 0.58f, h * 0.68f),
                    cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
                    style = strokeThin
                )
                drawRoundRect(
                    color = colors.surface,
                    topLeft = Offset(w * 0.15f, h * 0.20f),
                    size = Size(w * 0.58f, h * 0.68f),
                    cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
                )
                drawRoundRect(
                    color = ink,
                    topLeft = Offset(w * 0.15f, h * 0.20f),
                    size = Size(w * 0.58f, h * 0.68f),
                    cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
                    style = strokeThin
                )

                // Three minimalist checklist rows
                val rowsY = listOf(0.38f, 0.54f, 0.70f)
                rowsY.forEachIndexed { idx, yf ->
                    val y = h * yf
                    if (idx == 0) {
                        drawCircle(
                            color = accentSoft,
                            radius = w * 0.055f,
                            center = Offset(w * 0.29f, y)
                        )
                        drawCircle(
                            color = accent,
                            radius = w * 0.055f,
                            center = Offset(w * 0.29f, y),
                            style = strokeThin
                        )
                    } else {
                        drawCircle(
                            color = muted,
                            radius = w * 0.045f,
                            center = Offset(w * 0.29f, y),
                            style = strokeThin
                        )
                    }
                    drawLine(
                        color = if (idx == 0) ink else muted,
                        start = Offset(w * 0.40f, y),
                        end = Offset(w * (if (idx == 2) 0.56f else 0.63f), y),
                        strokeWidth = w * 0.026f,
                        cap = StrokeCap.Round
                    )
                }

                // Accent corner circle with crisp check
                val badgeCenter = Offset(w * 0.76f, h * 0.74f)
                drawCircle(color = accent, radius = w * 0.14f, center = badgeCenter)
                val checkStart = Offset(badgeCenter.x - w * 0.055f, badgeCenter.y + h * 0.005f)
                val checkMid = Offset(badgeCenter.x - w * 0.01f, badgeCenter.y + h * 0.05f)
                val checkEnd = Offset(badgeCenter.x + w * 0.065f, badgeCenter.y - h * 0.045f)
                drawLine(colors.surface, checkStart, checkMid, strokeWidth = w * 0.03f, cap = StrokeCap.Round)
                drawLine(colors.surface, checkMid, checkEnd, strokeWidth = w * 0.03f, cap = StrokeCap.Round)
            }

            SwissEmptyArtType.NOTES -> {
                // Swiss folded editorial notepad + ruled lines + royal blue nib mark
                drawRoundRect(
                    color = colors.surface,
                    topLeft = Offset(w * 0.18f, h * 0.14f),
                    size = Size(w * 0.64f, h * 0.74f),
                    cornerRadius = CornerRadius(w * 0.09f, w * 0.09f)
                )
                drawRoundRect(
                    color = ink,
                    topLeft = Offset(w * 0.18f, h * 0.14f),
                    size = Size(w * 0.64f, h * 0.74f),
                    cornerRadius = CornerRadius(w * 0.09f, w * 0.09f),
                    style = strokeThin
                )
                // Binding rings along top
                listOf(0.32f, 0.50f, 0.68f).forEach { xf ->
                    drawLine(
                        color = accent,
                        start = Offset(w * xf, h * 0.09f),
                        end = Offset(w * xf, h * 0.20f),
                        strokeWidth = w * 0.03f,
                        cap = StrokeCap.Round
                    )
                }
                // Ruled text lines
                val lines = listOf(0.35f to 0.70f, 0.48f to 0.68f, 0.61f to 0.60f, 0.74f to 0.48f)
                lines.forEachIndexed { idx, (yf, endX) ->
                    drawLine(
                        color = if (idx == 0) ink else muted,
                        start = Offset(w * 0.30f, h * yf),
                        end = Offset(w * endX, h * yf),
                        strokeWidth = if (idx == 0) w * 0.032f else w * 0.022f,
                        cap = StrokeCap.Round
                    )
                }
                // Accent dot bottom-right
                drawCircle(
                    color = accent,
                    radius = w * 0.045f,
                    center = Offset(w * 0.68f, h * 0.74f)
                )
            }

            SwissEmptyArtType.HABITS -> {
                // Minimalist 3x3 Swiss consistency dot matrix with diagonal active streak
                val coords = listOf(0.26f, 0.50f, 0.74f)
                coords.forEachIndexed { r, yf ->
                    coords.forEachIndexed { c, xf ->
                        val isLit = (r == 0 && c == 0) || (r == 1 && c == 1) || (r == 2 && c == 2) || (r == 1 && c == 2)
                        val center = Offset(w * xf, h * yf)
                        if (isLit) {
                            drawCircle(color = accentSoft, radius = w * 0.095f, center = center)
                            drawCircle(color = accent, radius = w * 0.055f, center = center)
                        } else {
                            drawCircle(
                                color = muted,
                                radius = w * 0.055f,
                                center = center,
                                style = strokeThin
                            )
                        }
                    }
                }
            }

            SwissEmptyArtType.BIRTHDAYS -> {
                // Minimalist architectural gift/cake emblem with subtle spark lines
                drawRoundRect(
                    color = colors.surface,
                    topLeft = Offset(w * 0.20f, h * 0.40f),
                    size = Size(w * 0.60f, h * 0.44f),
                    cornerRadius = CornerRadius(w * 0.07f, w * 0.07f)
                )
                drawRoundRect(
                    color = ink,
                    topLeft = Offset(w * 0.20f, h * 0.40f),
                    size = Size(w * 0.60f, h * 0.44f),
                    cornerRadius = CornerRadius(w * 0.07f, w * 0.07f),
                    style = strokeThin
                )
                // Lid bar
                drawRoundRect(
                    color = accentSoft,
                    topLeft = Offset(w * 0.16f, h * 0.31f),
                    size = Size(w * 0.68f, h * 0.12f),
                    cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
                )
                drawRoundRect(
                    color = ink,
                    topLeft = Offset(w * 0.16f, h * 0.31f),
                    size = Size(w * 0.68f, h * 0.12f),
                    cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
                    style = strokeThin
                )
                // Vertical ribbon
                drawLine(
                    color = accent,
                    start = Offset(w * 0.50f, h * 0.31f),
                    end = Offset(w * 0.50f, h * 0.84f),
                    strokeWidth = w * 0.036f,
                    cap = StrokeCap.Butt
                )
                // Geometric bow / candle rays on top
                drawCircle(
                    color = accent,
                    radius = w * 0.065f,
                    center = Offset(w * 0.42f, h * 0.23f),
                    style = strokeBold
                )
                drawCircle(
                    color = accent,
                    radius = w * 0.065f,
                    center = Offset(w * 0.58f, h * 0.23f),
                    style = strokeBold
                )
                // Crisp subtle spark dots
                drawCircle(color = accent, radius = w * 0.024f, center = Offset(w * 0.22f, h * 0.18f))
                drawCircle(color = muted, radius = w * 0.022f, center = Offset(w * 0.78f, h * 0.17f))
            }
        }
    }
}
