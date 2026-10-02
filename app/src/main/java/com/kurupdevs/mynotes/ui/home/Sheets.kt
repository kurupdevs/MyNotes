package com.kurupdevs.mynotes.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kurupdevs.mynotes.data.local.LabelEntity
import com.kurupdevs.mynotes.data.model.NoteColor
import com.kurupdevs.mynotes.ui.theme.CardShape
import com.kurupdevs.mynotes.ui.theme.SheetShape
import com.kurupdevs.mynotes.ui.theme.TitleWhite
import com.kurupdevs.mynotes.ui.theme.cardColor

/** New-note type picker sheet (FAB tap). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewNoteSheet(dark: Boolean, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = SheetShape,
        containerColor = if (dark) Color(0xFF111111) else Color.White
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(
                "New note",
                style = MaterialTheme.typography.headlineMedium,
                color = if (dark) TitleWhite else Color(0xFF141210)
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TypeTile(dark, "Text", Icons.Filled.TextFields, Color(0xFFF7EDC9)) { onPick("text") }
                TypeTile(dark, "Checklist", Icons.Filled.Checklist, Color(0xFFEA7B53)) { onPick("checklist") }
                TypeTile(dark, "Photo", Icons.Filled.Image, Color(0xFFF6D44E)) { onPick("photo") }
                TypeTile(dark, "Voice", Icons.Filled.Mic, Color(0xFFA9D673)) { onPick("voice") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun TypeTile(dark: Boolean, label: String, icon: ImageVector, bg: Color, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null, onClick = onClick
        )
    ) {
        Box(
            Modifier.size(68.dp).clip(CardShape.copy()).background(bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, tint = Color(0xFF3D1508), modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (dark) TitleWhite else Color(0xFF141210))
    }
}

/** Color picker sheet (5 pastels + default). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickSheet(dark: Boolean, onDismiss: () -> Unit, onPick: (NoteColor) -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState, shape = SheetShape,
        containerColor = if (dark) Color(0xFF111111) else Color.White
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Pick a color", style = MaterialTheme.typography.headlineMedium,
                color = if (dark) TitleWhite else Color(0xFF141210))
            Spacer(Modifier.height(16.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(NoteColor.entries) { c ->
                    val bg = cardColor(c.key, dark)
                    Box(
                        Modifier.size(52.dp).clip(CircleShape)
                            .background(bg)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onPick(c) }
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Label picker sheet (multi handled by caller via single picks loop). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelPickSheet(
    dark: Boolean,
    labels: List<LabelEntity>,
    onDismiss: () -> Unit,
    onPick: (List<String>) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val selected = remember { androidx.compose.runtime.mutableStateListOf<String>() }
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState, shape = SheetShape,
        containerColor = if (dark) Color(0xFF111111) else Color.White
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Add labels", style = MaterialTheme.typography.headlineMedium,
                color = if (dark) TitleWhite else Color(0xFF141210))
            Spacer(Modifier.height(12.dp))
            if (labels.isEmpty()) {
                Text("No labels yet — make some from the Labels screen.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (dark) TitleWhite.copy(alpha = 0.6f) else Color(0xFF6B6257))
            }
            labels.forEach { l ->
                val isSel = l.id in selected
                Row(
                    Modifier.fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (isSel) selected.remove(l.id) else selected.add(l.id)
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(14.dp).clip(CircleShape).background(labelDot(l.color)))
                    Spacer(Modifier.size(12.dp))
                    Text(l.name, style = MaterialTheme.typography.bodyLarge,
                        color = if (dark) TitleWhite else Color(0xFF141210),
                        modifier = Modifier.weight(1f))
                    if (isSel) Icon(
                        Icons.Filled.Check, "Selected",
                        tint = if (dark) TitleWhite else Color(0xFF141210),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.fillMaxWidth()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                    .background(if (dark) TitleWhite else Color(0xFF141210))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onPick(selected.toList()) }
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Done", style = MaterialTheme.typography.labelLarge,
                    color = if (dark) Color.Black else Color.White)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
