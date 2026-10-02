package com.kurupdevs.mynotes.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.kurupdevs.mynotes.ui.theme.CardShape
import com.kurupdevs.mynotes.ui.theme.WhiteSwipeBg
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Swipe right = pin/unpin, swipe left = archive (pinned: unpin).
 * 40% width threshold, spring-back with overshoot bounce.
 */
@Composable
fun SwipeableNoteCard(
    pinned: Boolean,
    onPin: () -> Unit,
    onArchive: () -> Unit,
    card: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dark: Boolean = true
) {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .clip(CardShape)
            .pointerInput(pinned) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            val w = size.width.toFloat()
                            val threshold = w * 0.4f
                            when {
                                offsetX.value > threshold -> {
                                    offsetX.animateTo(w * 0.35f, spring(300f, Spring.DampingRatioMediumBouncy))
                                    onPin()
                                    offsetX.animateTo(0f, spring(300f, 0.65f))
                                }
                                offsetX.value < -threshold -> {
                                    offsetX.animateTo(-w * 0.35f, spring(300f, Spring.DampingRatioMediumBouncy))
                                    onArchive()
                                    offsetX.animateTo(0f, spring(300f, 0.65f))
                                }
                                else -> offsetX.animateTo(0f, spring(300f, 0.65f))
                            }
                        }
                    },
                    onHorizontalDrag = { _, dragAmount ->
                        scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                    }
                )
            }
    ) {
        // under-actions
        Row(
            Modifier.matchParentSize().background(if (dark) Color(0xFF1A1A1A) else WhiteSwipeBg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // left side (revealed on swipe right): pin
            Box(
                Modifier.weight(1f).fillMaxHeight().padding(start = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF8E7BD8).copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PushPin, "Pin", tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
            // right side (revealed on swipe left): archive / unpin
            Box(
                Modifier.weight(1f).fillMaxHeight().padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF889DB5).copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (pinned) Icons.Filled.PushPin else Icons.Filled.Archive,
                        if (pinned) "Unpin" else "Archive",
                        tint = Color.White, modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
        ) {
            card()
        }
    }
}
