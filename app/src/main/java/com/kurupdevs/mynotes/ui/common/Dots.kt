package com.kurupdevs.mynotes.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Subtle dotted grid texture over the background (spec §1.1 bg_dot). */
@Composable
fun DottedBackground(dark: Boolean, modifier: Modifier = Modifier) {
    Box(modifier) {
        val density = LocalDensity.current
        val dot = if (dark) Color(0x0AFFFFFF) else Color(0x0A000000)
        Canvas(Modifier.fillMaxSize()) {
            with(density) {
                val step = 26.dp.toPx()
                val r = 1.1.dp.toPx()
                var y = step / 2
                while (y < size.height) {
                    var x = step / 2
                    while (x < size.width) {
                        drawCircle(dot, r, Offset(x, y))
                        x += step
                    }
                    y += step
                }
            }
        }
    }
}
