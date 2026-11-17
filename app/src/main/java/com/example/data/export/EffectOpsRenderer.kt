package com.example.data.export

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.LinearGradient
import android.graphics.Shader
import com.example.engine.EffectOp
import com.example.engine.EffectOpKind

/**
 * Draws [EffectOp] lists (from [com.example.engine.EffectOps]) onto an
 * android.graphics canvas. This is the exported-file renderer; the Compose
 * preview consumes the same ops, so both match pixel for pixel.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object EffectOpsRenderer {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun draw(canvas: Canvas, ops: List<EffectOp>) {
        ops.forEach { op ->
            when (op.kind) {
                EffectOpKind.RECT -> {
                    paint.style = Paint.Style.FILL
                    paint.shader = null
                    paint.color = op.colorA
                    canvas.drawRect(op.left, op.top, op.right, op.bottom, paint)
                }
                EffectOpKind.GRADIENT_H -> {
                    paint.style = Paint.Style.FILL
                    paint.shader = LinearGradient(
                        op.left, op.top, op.right, op.top,
                        op.colorA, op.colorB, Shader.TileMode.CLAMP
                    )
                    canvas.drawRect(op.left, op.top, op.right, op.bottom, paint)
                    paint.shader = null
                }
                EffectOpKind.RADIAL -> {
                    paint.style = Paint.Style.FILL
                    val cx = (op.left + op.right) / 2f
                    val cy = (op.top + op.bottom) / 2f
                    val radius = maxOf(op.right - op.left, op.bottom - op.top) / 2f
                    paint.shader = RadialGradient(
                        cx, cy, radius.coerceAtLeast(1f),
                        op.colorA, op.colorB, Shader.TileMode.CLAMP
                    )
                    canvas.drawRect(op.left, op.top, op.right, op.bottom, paint)
                    paint.shader = null
                }
                EffectOpKind.OVAL_FILLED -> {
                    paint.style = Paint.Style.FILL
                    paint.shader = null
                    paint.color = op.colorA
                    canvas.drawCircle(op.cx, op.cy, op.radius.coerceAtLeast(0.5f), paint)
                }
                EffectOpKind.CIRCLE_STROKE -> {
                    paint.style = Paint.Style.STROKE
                    paint.shader = null
                    paint.color = op.colorA
                    paint.strokeWidth = op.strokeWidth.coerceAtLeast(1f)
                    canvas.drawCircle(op.cx, op.cy, op.radius.coerceAtLeast(1f), paint)
                }
                EffectOpKind.RECT_STROKE -> {
                    paint.style = Paint.Style.STROKE
                    paint.shader = null
                    paint.color = op.colorA
                    paint.strokeWidth = op.strokeWidth.coerceAtLeast(1f)
                    canvas.drawRect(op.left, op.top, op.right, op.bottom, paint)
                }
                EffectOpKind.LINE -> {
                    paint.style = Paint.Style.STROKE
                    paint.shader = null
                    paint.color = op.colorA
                    paint.strokeWidth = op.strokeWidth.coerceAtLeast(1f)
                    paint.strokeCap = Paint.Cap.ROUND
                    canvas.drawLine(op.cx, op.cy, op.left, op.top, paint)
                }
            }
        }
        paint.style = Paint.Style.FILL
    }
}
