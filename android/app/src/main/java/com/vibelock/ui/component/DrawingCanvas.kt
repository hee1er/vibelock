package com.vibelock.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.vibelock.data.model.DrawPath
import com.vibelock.data.model.DrawPoint

@Composable
fun DrawingCanvas(
    localPaths: List<DrawPath>,
    remotePaths: List<DrawPath>,
    currentLocalPath: DrawPath?,
    currentRemotePath: DrawPath?,
    isDrawingEnabled: Boolean,
    onDrawStart: (Float, Float) -> Unit,
    onDrawMove: (Float, Float) -> Unit,
    onDrawEnd: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF111114),
) {
    Box(
        modifier = modifier
            .background(backgroundColor)
            .pointerInput(isDrawingEnabled) {
                if (!isDrawingEnabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        onDrawStart(offset.x / size.width, offset.y / size.height)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        onDrawMove(change.position.x / size.width, change.position.y / size.height)
                    },
                    onDragEnd = { onDrawEnd() },
                    onDragCancel = { onDrawEnd() },
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Remote paths (partner) — slightly dimmer
            remotePaths.forEach { path -> drawPath(path, alpha = 0.7f) }
            currentRemotePath?.let { drawPath(it, alpha = 0.7f) }

            // Local paths
            localPaths.forEach { path -> drawPath(path) }
            currentLocalPath?.let { drawPath(it) }
        }
    }
}

private fun DrawScope.drawPath(path: DrawPath, alpha: Float = 1f) {
    if (path.points.size < 2) {
        // Single dot
        path.points.firstOrNull()?.let { p ->
            drawCircle(
                color = Color(path.color).copy(alpha = alpha),
                radius = path.strokeWidth / 2f,
                center = Offset(p.x * size.width, p.y * size.height),
            )
        }
        return
    }

    val stroke = Stroke(
        width = path.strokeWidth,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
    )

    val androidPath = android.graphics.Path()
    path.points.forEachIndexed { index, point ->
        val x = point.x * size.width
        val y = point.y * size.height
        if (index == 0) androidPath.moveTo(x, y)
        else {
            // Smooth curves using quadratic bezier
            val prev = path.points[index - 1]
            val midX = (prev.x * size.width + x) / 2f
            val midY = (prev.y * size.height + y) / 2f
            androidPath.quadTo(prev.x * size.width, prev.y * size.height, midX, midY)
        }
    }

    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            this.color = Color(path.color).copy(alpha = alpha)
            this.strokeWidth = path.strokeWidth
            this.strokeCap = StrokeCap.Round
            this.strokeJoin = StrokeJoin.Round
            this.style = androidx.compose.ui.graphics.PaintingStyle.Stroke
            this.isAntiAlias = true
        }
        canvas.drawPath(
            androidx.compose.ui.graphics.Path().apply {
                path.points.forEachIndexed { index, point ->
                    val x = point.x * size.width
                    val y = point.y * size.height
                    if (index == 0) moveTo(x, y)
                    else {
                        val prev = path.points[index - 1]
                        val midX = (prev.x * size.width + x) / 2f
                        val midY = (prev.y * size.height + y) / 2f
                        quadraticBezierTo(prev.x * size.width, prev.y * size.height, midX, midY)
                    }
                }
            },
            paint,
        )
    }
}
