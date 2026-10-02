package com.kurupdevs.mynotes.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.kurupdevs.mynotes.data.model.BlockKind

private val HlYellow = Color(0xFFFFF176)
private val HlGreen = Color(0xFFAED581)
private val HlPurple = Color(0xFFCE93D8)
private val HlPink = Color(0xFFF48FB1)
private val HlMarks = listOf("hl-yellow", "hl-green", "hl-purple", "hl-pink")
private val HlColors = listOf(HlYellow, HlGreen, HlPurple, HlPink)

/**
 * Formatting panel (reference: rich editor formatting panel).
 * Slim always-visible row; expands to full panel with Highlighter dots,
 * B/I/S/U, H1/H2/H3, bullet, quote and clear-formatting.
 * Wires into EditorViewModel.toggleMark / setKind / toggleChecklistLine / clearFormatting.
 */
@Composable
fun FormatPanel(dark: Boolean, ink: Color, vm: EditorViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val focused by vm.focusedBlock.collectAsState()
    val blocks by vm.blocks.collectAsState()
    val cur = blocks.firstOrNull { it.id == focused }
    fun has(mark: String) = cur?.marks?.contains(mark) == true

    Column(
        Modifier.fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(ink.copy(alpha = 0.06f))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // slim row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            PanelIconBtn(Icons.Filled.TextFields, "Formatting", expanded, ink) { expanded = !expanded }
            PanelTextBtn("B", has("bold"), ink, bold = true) { vm.toggleMark("bold") }
            PanelTextBtn("I", has("italic"), ink, italic = true) { vm.toggleMark("italic") }
            PanelIconBtn(
                Icons.Filled.FormatListBulleted, "Bullet list",
                cur?.kind == BlockKind.LI, ink
            ) { vm.setKind(if (cur?.kind == BlockKind.LI) BlockKind.P else BlockKind.LI) }
            PanelIconBtn(
                Icons.Filled.Checklist, "Checklist",
                cur?.kind == BlockKind.TODO, ink
            ) { vm.toggleChecklistLine() }
            PanelIconBtn(Icons.Filled.FormatQuote, "Quote", has("quote"), ink) { vm.toggleMark("quote") }
            Spacer(Modifier.weight(1f))
            HlColors.take(2).forEachIndexed { i, c ->
                HlDot(c, HlMarks[i], has(HlMarks[i]), ink) { vm.toggleMark(HlMarks[i]) }
            }
        }
        if (expanded) {
            Spacer(Modifier.height(2.dp))
            // Highlighter row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Icon(
                    Icons.Filled.Brush, "Highlighter",
                    tint = ink.copy(alpha = 0.7f), modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Highlighter", style = MaterialTheme.typography.labelLarge, color = ink)
                Spacer(Modifier.weight(1f))
                HlColors.forEachIndexed { i, c ->
                    HlDot(c, HlMarks[i], has(HlMarks[i]), ink) { vm.toggleMark(HlMarks[i]) }
                }
                // none
                Box(
                    Modifier.size(30.dp).padding(3.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, ink.copy(alpha = 0.4f), CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { HlMarks.filter { has(it) }.forEach { vm.toggleMark(it) } },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.Close, "No highlight",
                        tint = ink.copy(alpha = 0.5f), modifier = Modifier.size(13.dp)
                    )
                }
                Spacer(Modifier.width(2.dp))
            }
            // B I S U
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                PanelTextBtn("B", has("bold"), ink, bold = true) { vm.toggleMark("bold") }
                PanelTextBtn("I", has("italic"), ink, italic = true) { vm.toggleMark("italic") }
                PanelTextBtn("S", has("strike"), ink, strike = true) { vm.toggleMark("strike") }
                PanelTextBtn("U", has("underline"), ink, underline = true) { vm.toggleMark("underline") }
            }
            // H1 H2 H3, bullet, quote, clear
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                PanelTextBtn("H1", cur?.kind == BlockKind.H1, ink, heading = true) {
                    vm.setKind(if (cur?.kind == BlockKind.H1) BlockKind.P else BlockKind.H1)
                }
                PanelTextBtn("H2", cur?.kind == BlockKind.H2, ink, heading = true) {
                    vm.setKind(if (cur?.kind == BlockKind.H2) BlockKind.P else BlockKind.H2)
                }
                PanelTextBtn("H3", has("h3"), ink, heading = true) { vm.toggleMark("h3") }
                PanelIconBtn(
                    Icons.Filled.FormatListBulleted, "Bullet list",
                    cur?.kind == BlockKind.LI, ink
                ) { vm.setKind(if (cur?.kind == BlockKind.LI) BlockKind.P else BlockKind.LI) }
                PanelIconBtn(Icons.Filled.FormatQuote, "Quote", has("quote"), ink) { vm.toggleMark("quote") }
                PanelIconBtn(Icons.Filled.FormatClear, "Clear formatting", false, ink) { vm.clearFormatting() }
            }
        }
    }
}

@Composable
private fun PanelIconBtn(
    icon: ImageVector,
    desc: String,
    active: Boolean,
    ink: Color,
    onClick: () -> Unit
) {
    Box(
        Modifier.clip(CircleShape)
            .background(if (active) ink.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon, desc,
            tint = if (active) ink else ink.copy(alpha = 0.6f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun PanelTextBtn(
    label: String,
    active: Boolean,
    ink: Color,
    bold: Boolean = false,
    italic: Boolean = false,
    underline: Boolean = false,
    strike: Boolean = false,
    heading: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        Modifier.clip(CircleShape)
            .background(if (active) ink.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (bold || heading) FontWeight.Bold else FontWeight.SemiBold,
                fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
                textDecoration = when {
                    underline -> TextDecoration.Underline
                    strike -> TextDecoration.LineThrough
                    else -> TextDecoration.None
                }
            ),
            color = if (active) ink else ink.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun HlDot(color: Color, mark: String, active: Boolean, ink: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(32.dp).padding(4.dp)
            .clip(CircleShape)
            .background(color)
            .then(if (active) Modifier.border(2.dp, ink, CircleShape) else Modifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            )
    )
}
