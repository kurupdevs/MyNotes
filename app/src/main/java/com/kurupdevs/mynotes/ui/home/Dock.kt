package com.kurupdevs.mynotes.ui.home
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.kurupdevs.mynotes.ui.theme.DockFab
import com.kurupdevs.mynotes.ui.theme.DockFabIcon
import com.kurupdevs.mynotes.ui.theme.DockMicGlass
import com.kurupdevs.mynotes.ui.theme.DockMicIcon
import com.kurupdevs.mynotes.ui.theme.GlowBlue
import com.kurupdevs.mynotes.ui.theme.GlowGreen
import com.kurupdevs.mynotes.ui.theme.GlowPurple
/** Bottom dock: + FAB overlapping a frosted-glass mic on three glow orbs (spec §1.6). */
@Composable
fun HomeDock(
    dark: Boolean,
    onAdd: () -> Unit,
    onMic: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier.size(190.dp, 120.dp), contentAlignment = Alignment.BottomCenter) {
        // glow orbs behind
        GlowOrbs(dark)
        // mic button (trailing, tucked ~12dp behind FAB)
        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-28).dp, y = (-14).dp)
                .size(56.dp)
                .clip(CircleShape)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onMic,
                    onLongClick = onMic
                ),
            contentAlignment = Alignment.Center
        ) {
            // frosted glass: blurred orb colors clipped inside + translucent white + rim
            Box(
                Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .blur(18.dp)
                    .graphicsLayer { alpha = 0.85f }
            ) {
                Canvas(Modifier.matchParentSize()) {
                    drawCircle(GlowPurple, radius = size.minDimension * 0.5f, center = Offset(size.width * 0.3f, size.height * 0.7f))
                    drawCircle(GlowBlue, radius = size.minDimension * 0.5f, center = Offset(size.width * 0.7f, size.height * 0.3f))
                }
            }
            Box(
                Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .background(DockMicGlass)
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            )
            Icon(Icons.Filled.Mic, "Quick voice note", tint = DockMicIcon, modifier = Modifier.size(24.dp))
        }
        // FAB (on top, centered)
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-16).dp)
                .size(62.dp)
                .clip(CircleShape)
                .background(DockFab)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onAdd
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Add, "New note", tint = DockFabIcon, modifier = Modifier.size(28.dp))
        }
    }
}
@Composable
private fun GlowOrbs(dark: Boolean) {
    val alpha = if (dark) 0.55f else 0.3f
    Canvas(Modifier.size(190.dp, 120.dp)) {
        fun orb(color: Color, cx: Float, cy: Float) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
                    center = Offset(cx, cy),
                    radius = 70.dp.toPx()
                ),
                radius = 70.dp.toPx(),
                center = Offset(cx, cy)
            )
        }
        orb(GlowGreen, size.width * 0.22f, size.height * 0.78f)
        orb(GlowBlue, size.width * 0.78f, size.height * 0.78f)
        orb(GlowPurple, size.width * 0.5f, size.height * 0.66f)
    }
}
