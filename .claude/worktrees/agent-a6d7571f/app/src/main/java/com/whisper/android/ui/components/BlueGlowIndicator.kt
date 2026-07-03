package com.whisper.android.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val BlueGlowColor = Color(0xFF90CAF9)

@Composable
fun BlueGlowIndicator(active: Boolean, onClick: () -> Unit) {
    val alpha by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(durationMillis = 1500),
        label = "glow_alpha",
    )

    if (alpha > 0f) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .glowEffect(color = BlueGlowColor.copy(alpha = alpha), radius = 16.dp)
                .background(BlueGlowColor.copy(alpha = alpha), CircleShape)
                .clickable(onClick = onClick),
        )
    }
}

private fun Modifier.glowEffect(color: Color, radius: Dp): Modifier =
    drawBehind {
        drawIntoCanvas { canvas ->
            val paint = Paint().apply {
                asFrameworkPaint().apply {
                    isAntiAlias = true
                    this.color = android.graphics.Color.TRANSPARENT
                    setShadowLayer(
                        radius.toPx(),
                        0f,
                        0f,
                        color.copy(alpha = color.alpha * 0.8f).toArgb(),
                    )
                }
            }
            canvas.drawCircle(
                center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f),
                radius = size.minDimension / 2f,
                paint = paint,
            )
        }
    }
