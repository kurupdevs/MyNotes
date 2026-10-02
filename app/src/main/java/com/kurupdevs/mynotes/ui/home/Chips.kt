package com.kurupdevs.mynotes.ui.home
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kurupdevs.mynotes.data.local.LabelEntity
import com.kurupdevs.mynotes.ui.theme.ChipBadgeBg
import com.kurupdevs.mynotes.ui.theme.ChipBadgeText
import com.kurupdevs.mynotes.ui.theme.ChipIdleBorder
import com.kurupdevs.mynotes.ui.theme.ChipIdleText
import com.kurupdevs.mynotes.ui.theme.ChipShape
import com.kurupdevs.mynotes.ui.theme.TitleWhite
/** Filter chips row: All (count badge), Important, To-do, then user labels. */
@Composable
fun ChipsRow(
    dark: Boolean,
    filter: HomeFilter,
    labels: List<LabelEntity>,
    totalCount: Int,
    onFilter: (HomeFilter) -> Unit,
    onLabelLongPress: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeText = if (dark) TitleWhite else Color.White
    val activeBorder = if (dark) TitleWhite else Color(0xFF141210)
    val activeBg = if (dark) Color.Transparent else Color(0xFF141210)
    val idleText = if (dark) ChipIdleText else Color(0xFF6B6257)
    val idleBorder = if (dark) ChipIdleBorder else Color(0xFFD8D0BE)
    LazyRow(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
    ) {
        item {
            Chip(
                label = "All", count = totalCount,
                active = filter is HomeFilter.All,
                activeText = activeText, activeBorder = activeBorder, activeBg = activeBg,
                idleText = idleText, idleBorder = idleBorder,
                onClick = { onFilter(HomeFilter.All) }
            )
        }
        item {
            Chip(
                label = "Important", count = null,
                active = filter is HomeFilter.Important,
                activeText = activeText, activeBorder = activeBorder, activeBg = activeBg,
                idleText = idleText, idleBorder = idleBorder,
                onClick = { onFilter(HomeFilter.Important) }
            )
        }
        item {
            Chip(
                label = "To-do", count = null,
                active = filter is HomeFilter.Todo,
                activeText = activeText, activeBorder = activeBorder, activeBg = activeBg,
                idleText = idleText, idleBorder = idleBorder,
                onClick = { onFilter(HomeFilter.Todo) }
            )
        }
        items(labels, key = { it.id }) { label ->
            val isActive = filter is HomeFilter.Label && filter.labelId == label.id
            Chip(
                label = label.name, count = null,
                active = isActive,
                activeText = activeText, activeBorder = activeBorder, activeBg = activeBg,
                idleText = idleText, idleBorder = idleBorder,
                dotColor = labelDot(label.color),
                onClick = { onFilter(HomeFilter.Label(label.id)) },
                onLongClick = onLabelLongPress
            )
        }
    }
}
@Composable
private fun Chip(
    label: String,
    count: Int?,
    active: Boolean,
    activeText: Color,
    activeBorder: Color,
    activeBg: Color,
    idleText: Color,
    idleBorder: Color,
    dotColor: Color? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val scale by animateFloatAsState(
        if (active) 1f else 0.8f,
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.6f), label = "badge"
    )
    Row(
        modifier = Modifier
            .clip(ChipShape)
            .border(if (active) 2.dp else 1.dp, if (active) activeBorder else idleBorder, ChipShape)
            .background(if (active) activeBg else Color.Transparent)
            .alpha(if (active) 1f else 0.85f)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (dotColor != null) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(dotColor))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (active) activeText else idleText
        )
        if (count != null) {
            Box(
                Modifier
                    .scale(scale)
                    .clip(CircleShape)
                    .background(ChipBadgeBg)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("$count", style = MaterialTheme.typography.labelSmall, color = ChipBadgeText)
            }
        }
    }
}
fun labelDot(color: String): Color = when (color) {
    "coral" -> Color(0xFFEA7B53)
    "yellow" -> Color(0xFFF6D44E)
    "cream" -> Color(0xFFF7EDC9)
    "green" -> Color(0xFFA9D673)
    "blue" -> Color(0xFF889DB5)
    "purple" -> Color(0xFF8E7BD8)
    else -> Color(0xFFEA7B53)
}
val LABEL_COLORS = listOf("coral", "yellow", "cream", "green", "blue", "purple")
