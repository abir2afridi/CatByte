package com.example.ui.speed

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.LinearScale
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BezierPoint
import com.example.model.SpeedCurve
import com.example.model.SpeedCurvePreset
import com.example.ui.theme.PencilTheme

fun buildCurvePath(points: List<BezierPoint>, width: Float, height: Float): Path {
    if (points.size < 2) return Path()

    val sortedPoints = points.sortedBy { it.x }
    val path = Path()

    val screenPoints = sortedPoints.map { p ->
        Offset(
            p.x * width,
            (1f - ((p.speedMultiplier.coerceIn(0.1f, 3f) - 0.1f) / 2.9f)) * height
        )
    }

    path.moveTo(screenPoints[0].x, screenPoints[0].y)

    if (sortedPoints.size == 2) {
        path.lineTo(screenPoints[1].x, screenPoints[1].y)
    } else {
        for (i in 0 until screenPoints.size - 1) {
            val p0 = if (i > 0) screenPoints[i - 1] else screenPoints[0]
            val p1 = screenPoints[i]
            val p2 = screenPoints[i + 1]
            val p3 = if (i + 2 < screenPoints.size) screenPoints[i + 2] else screenPoints.last()

            val cp1x = p1.x + (p2.x - p0.x) / 6f
            val cp1y = p1.y + (p2.y - p0.y) / 6f
            val cp2x = p2.x - (p3.x - p1.x) / 6f
            val cp2y = p2.y - (p3.y - p1.y) / 6f

            path.cubicTo(cp1x, cp1y, cp2x, cp2y, p2.x, p2.y)
        }
    }

    return path
}

fun generatePresetPoints(preset: SpeedCurvePreset): List<BezierPoint> = when (preset) {
    SpeedCurvePreset.STANDARD -> listOf(
        BezierPoint(0.0f, 1.0f),
        BezierPoint(1.0f, 1.0f)
    )
    SpeedCurvePreset.MONTAGE -> listOf(
        BezierPoint(0.0f, 1.0f),
        BezierPoint(0.2f, 2.5f),
        BezierPoint(0.4f, 0.5f),
        BezierPoint(0.6f, 2.0f),
        BezierPoint(0.8f, 0.7f),
        BezierPoint(1.0f, 1.0f)
    )
    SpeedCurvePreset.HERO_MOMENT -> listOf(
        BezierPoint(0.0f, 1.0f),
        BezierPoint(0.3f, 1.0f),
        BezierPoint(0.4f, 0.2f),
        BezierPoint(0.5f, 3.0f),
        BezierPoint(0.6f, 0.3f),
        BezierPoint(0.7f, 1.0f),
        BezierPoint(1.0f, 1.0f)
    )
    SpeedCurvePreset.BULLET_TIME -> listOf(
        BezierPoint(0.0f, 1.0f),
        BezierPoint(0.25f, 1.0f),
        BezierPoint(0.35f, 0.15f),
        BezierPoint(0.65f, 0.15f),
        BezierPoint(0.75f, 1.0f),
        BezierPoint(1.0f, 1.0f)
    )
    SpeedCurvePreset.JUMP_CUT -> listOf(
        BezierPoint(0.0f, 2.0f),
        BezierPoint(0.2f, 0.5f),
        BezierPoint(0.4f, 2.5f),
        BezierPoint(0.6f, 0.4f),
        BezierPoint(0.8f, 2.0f),
        BezierPoint(1.0f, 1.0f)
    )
    SpeedCurvePreset.CUSTOM -> listOf(
        BezierPoint(0.0f, 1.0f),
        BezierPoint(1.0f, 1.0f)
    )
}
