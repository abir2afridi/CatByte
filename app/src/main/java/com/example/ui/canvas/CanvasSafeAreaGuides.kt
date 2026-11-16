package com.example.ui.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.engine.SafeAreaMath

/**
 * Preview-only composition guides: action-safe (90%), title-safe (80%) and the
 * rule-of-thirds grid. They are drawn as a Compose overlay, never inside the
 * render graph, so they can never leak into an export.
 */
@Composable
internal fun CanvasSafeAreaGuides(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("safe_area_guides")
    ) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val thin = 1.dp.toPx()
        val dash = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 5.dp.toPx()))

        // Rule of thirds.
        SafeAreaMath.THIRDS.forEach { fraction ->
            drawLine(
                color = Color.White.copy(alpha = 0.22f),
                start = Offset(width * fraction, 0f),
                end = Offset(width * fraction, height),
                strokeWidth = thin,
                pathEffect = dash
            )
            drawLine(
                color = Color.White.copy(alpha = 0.22f),
                start = Offset(0f, height * fraction),
                end = Offset(width, height * fraction),
                strokeWidth = thin,
                pathEffect = dash
            )
        }

        // Title safe: text should stay inside this box.
        val title = SafeAreaMath.guideRect(SafeAreaMath.TITLE_SAFE, width, height)
        drawRect(
            color = Color.White.copy(alpha = 0.35f),
            topLeft = Offset(title.left, title.top),
            size = Size(title.width, title.height),
            style = Stroke(width = thin, pathEffect = dash)
        )

        // Action safe: important action should stay inside this box.
        val action = SafeAreaMath.guideRect(SafeAreaMath.ACTION_SAFE, width, height)
        drawRect(
            color = Color.White.copy(alpha = 0.55f),
            topLeft = Offset(action.left, action.top),
            size = Size(action.width, action.height),
            style = Stroke(width = thin)
        )

        // Centre crosshair, so an off-centre subject is easy to spot.
        val arm = 7.dp.toPx()
        val centre = Offset(width / 2f, height / 2f)
        val centreColour = Color(0xFF3FE0D0).copy(alpha = 0.85f)
        drawLine(centreColour, Offset(centre.x - arm, centre.y), Offset(centre.x + arm, centre.y), thin)
        drawLine(centreColour, Offset(centre.x, centre.y - arm), Offset(centre.x, centre.y + arm), thin)
    }
}
