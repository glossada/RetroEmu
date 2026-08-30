package dev.retrotv.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun CrtOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val lineH = 2.dp.toPx()
        val gap   = 2.dp.toPx()
        var y = 0f
        while (y < size.height) {
            drawRect(
                color   = Color.Black.copy(alpha = 0.18f),
                topLeft = Offset(0f, y),
                size    = Size(size.width, lineH),
            )
            y += lineH + gap
        }
    }
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.radialGradient(
                colorStops = arrayOf(
                    0.0f  to Color.Transparent,
                    0.60f to Color.Transparent,
                    1.0f  to Color.Black.copy(alpha = 0.55f),
                ),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = size.width * 0.72f,
            ),
        )
    }
}
