package dev.mathi031.qrscanner.ui.scanner

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import kotlin.math.min

@Composable
fun ViewfinderOverlay(
    modifier: Modifier = Modifier,
    scanLineColor: Color = androidx.compose.material3.MaterialTheme.colorScheme.tertiary,
    frameWidthFraction: Float = 0.70f,
    cornerLengthDp: Int = 24,
    cornerStrokeDp: Int = 4,
) {
    val transition = rememberInfiniteTransition(label = "scan-line")
    val scanProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "scan-line-progress",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val frameSize = min(size.width, size.height) * frameWidthFraction
        val left = (size.width - frameSize) / 2f
        val top = (size.height - frameSize) / 2f
        val right = left + frameSize
        val bottom = top + frameSize

        val scrim = Path().apply {
            addRect(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
            addRect(androidx.compose.ui.geometry.Rect(left, top, right, bottom))
            fillType = PathFillType.EvenOdd
        }
        drawPath(scrim, color = Color.Black.copy(alpha = 0.5f))

        val cornerLenPx = cornerLengthDp.dp.toPx()
        val strokePx = cornerStrokeDp.dp.toPx()
        val cornerColor = Color.White

        // Top-left
        drawLine(cornerColor, androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Offset(left + cornerLenPx, top), strokePx, StrokeCap.Round)
        drawLine(cornerColor, androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Offset(left, top + cornerLenPx), strokePx, StrokeCap.Round)
        // Top-right
        drawLine(cornerColor, androidx.compose.ui.geometry.Offset(right - cornerLenPx, top), androidx.compose.ui.geometry.Offset(right, top), strokePx, StrokeCap.Round)
        drawLine(cornerColor, androidx.compose.ui.geometry.Offset(right, top), androidx.compose.ui.geometry.Offset(right, top + cornerLenPx), strokePx, StrokeCap.Round)
        // Bottom-left
        drawLine(cornerColor, androidx.compose.ui.geometry.Offset(left, bottom - cornerLenPx), androidx.compose.ui.geometry.Offset(left, bottom), strokePx, StrokeCap.Round)
        drawLine(cornerColor, androidx.compose.ui.geometry.Offset(left, bottom), androidx.compose.ui.geometry.Offset(left + cornerLenPx, bottom), strokePx, StrokeCap.Round)
        // Bottom-right
        drawLine(cornerColor, androidx.compose.ui.geometry.Offset(right - cornerLenPx, bottom), androidx.compose.ui.geometry.Offset(right, bottom), strokePx, StrokeCap.Round)
        drawLine(cornerColor, androidx.compose.ui.geometry.Offset(right, bottom - cornerLenPx), androidx.compose.ui.geometry.Offset(right, bottom), strokePx, StrokeCap.Round)

        val scanY = top + (bottom - top) * scanProgress
        // Glow difuso (cyan tenue) bajo la línea nítida.
        val glowBrush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                scanLineColor.copy(alpha = 0.6f),
                Color.Transparent,
            ),
            startX = left,
            endX = right,
        )
        drawLine(
            brush = glowBrush,
            start = androidx.compose.ui.geometry.Offset(left, scanY),
            end = androidx.compose.ui.geometry.Offset(right, scanY),
            strokeWidth = 8.dp.toPx(),
            cap = StrokeCap.Round,
        )
        // Línea nítida cyan.
        val scanBrush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                scanLineColor,
                Color.Transparent,
            ),
            startX = left,
            endX = right,
        )
        drawLine(
            brush = scanBrush,
            start = androidx.compose.ui.geometry.Offset(left, scanY),
            end = androidx.compose.ui.geometry.Offset(right, scanY),
            strokeWidth = 2.dp.toPx(),
        )
    }
}
