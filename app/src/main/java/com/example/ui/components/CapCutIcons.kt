package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pixel-accurate vector icons matching the reference CapCut video editor UI.
 */

@Composable
fun CapCutIconFilters(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = 1.4f * density
        val r = this.size.width * 0.28f
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f

        // 3 overlapping circles (Venn diagram)
        val topCy = cy - r * 0.42f
        val leftCx = cx - r * 0.52f
        val leftCy = cy + r * 0.44f
        val rightCx = cx + r * 0.52f
        val rightCy = cy + r * 0.44f

        drawCircle(
            color = tint,
            radius = r,
            center = Offset(cx, topCy),
            style = Stroke(width = stroke)
        )
        drawCircle(
            color = tint,
            radius = r,
            center = Offset(leftCx, leftCy),
            style = Stroke(width = stroke)
        )
        drawCircle(
            color = tint,
            radius = r,
            center = Offset(rightCx, rightCy),
            style = Stroke(width = stroke)
        )
    }
}

@Composable
fun CapCutIconEffectsStar(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = 1.5f * density
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val outerRadius = this.size.width * 0.44f
        val innerRadius = outerRadius * 0.42f

        val path = Path()
        val numPoints = 5
        val angleStep = (Math.PI * 2 / numPoints).toFloat()
        val startAngle = -Math.PI.toFloat() / 2f

        for (i in 0 until numPoints) {
            val outerAngle = startAngle + i * angleStep
            val innerAngle = outerAngle + angleStep / 2f

            val ox = cx + outerRadius * cos(outerAngle)
            val oy = cy + outerRadius * sin(outerAngle)
            if (i == 0) path.moveTo(ox, oy) else path.lineTo(ox, oy)

            val ix = cx + innerRadius * cos(innerAngle)
            val iy = cy + innerRadius * sin(innerAngle)
            path.lineTo(ix, iy)
        }
        path.close()

        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
fun CapCutIconCaptions(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = 1.4f * density
        val w = this.size.width
        val h = this.size.height
        val pad = w * 0.14f

        // Outer rounded screen
        drawRoundRect(
            color = tint,
            topLeft = Offset(pad, pad * 1.3f),
            size = Size(w - pad * 2, h - pad * 2.6f),
            cornerRadius = CornerRadius(4f * density, 4f * density),
            style = Stroke(width = stroke)
        )

        // Two horizontal caption lines
        val line1Y = h * 0.44f
        val line2Y = h * 0.58f
        drawLine(
            color = tint,
            start = Offset(w * 0.28f, line1Y),
            end = Offset(w * 0.72f, line1Y),
            strokeWidth = stroke * 1.1f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.36f, line2Y),
            end = Offset(w * 0.64f, line2Y),
            strokeWidth = stroke * 1.1f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun CapCutIconOverlay(
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: Dp = 24.dp
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = 1.4f * density
        val w = this.size.width
        val h = this.size.height

        val cardW = w * 0.62f
        val cardH = h * 0.54f

        // Back photo card
        drawRoundRect(
            color = tint.copy(alpha = 0.85f),
            topLeft = Offset(w * 0.12f, h * 0.12f),
            size = Size(cardW, cardH),
            cornerRadius = CornerRadius(3.5f * density, 3.5f * density),
            style = Stroke(width = stroke)
        )

        // Front photo card (overlapping bottom right)
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.26f, h * 0.34f),
            size = Size(cardW, cardH),
            cornerRadius = CornerRadius(3.5f * density, 3.5f * density),
            style = Stroke(width = stroke)
        )

        // Small dot / sun inside front card
        drawCircle(
            color = tint,
            radius = 1.6f * density,
            center = Offset(w * 0.40f, h * 0.49f)
        )
    }
}

@Composable
fun CapCutIconMagnetOn(
    isOn: Boolean = true,
    tint: Color = Color.White,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    val activeColor = if (isOn) Color(0xFF00F2FE) else tint.copy(alpha = 0.7f)
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = 1.3f * density
            val w = this.size.width
            val h = this.size.height

            val cardW = w * 0.66f
            val cardH = h * 0.56f

            // Back overlapping frame
            drawRoundRect(
                color = activeColor.copy(alpha = 0.5f),
                topLeft = Offset(w * 0.12f, h * 0.14f),
                size = Size(cardW, cardH),
                cornerRadius = CornerRadius(3f * density, 3f * density),
                style = Stroke(width = stroke)
            )

            // Front frame
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(w * 0.24f, h * 0.30f),
                size = Size(cardW, cardH),
                cornerRadius = CornerRadius(3f * density, 3f * density),
                style = Stroke(width = stroke)
            )
        }

        Text(
            text = "ON",
            color = activeColor,
            fontSize = 7.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.offset(x = 3.dp, y = 2.dp)
        )
    }
}

@Composable
fun CapCutIconFullscreen(
    tint: Color = Color.White,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = 1.6f * density
        val w = this.size.width
        val h = this.size.height
        val pad = w * 0.22f
        val len = w * 0.22f

        // Top-left corner
        drawLine(tint, Offset(pad, pad + len), Offset(pad, pad), stroke)
        drawLine(tint, Offset(pad, pad), Offset(pad + len, pad), stroke)

        // Top-right corner
        drawLine(tint, Offset(w - pad - len, pad), Offset(w - pad, pad), stroke)
        drawLine(tint, Offset(w - pad, pad), Offset(w - pad, pad + len), stroke)

        // Bottom-left corner
        drawLine(tint, Offset(pad, h - pad - len), Offset(pad, h - pad), stroke)
        drawLine(tint, Offset(pad, h - pad), Offset(pad + len, h - pad), stroke)

        // Bottom-right corner
        drawLine(tint, Offset(w - pad - len, h - pad), Offset(w - pad, h - pad), stroke)
        drawLine(tint, Offset(w - pad, h - pad), Offset(w - pad, h - pad - len), stroke)
    }
}

@Composable
fun CapCutIconPlayTriangle(
    tint: Color = Color.White,
    size: Dp = 26.dp,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = 1.6f * density
        val w = this.size.width
        val h = this.size.height

        val left = w * 0.30f
        val right = w * 0.74f
        val top = h * 0.24f
        val bottom = h * 0.76f

        val path = Path().apply {
            moveTo(left, top)
            lineTo(right, h / 2f)
            lineTo(left, bottom)
            close()
        }

        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

