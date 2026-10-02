package com.kurupdevs.mynotes.ui.labels

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kurupdevs.mynotes.NotesApp
import com.kurupdevs.mynotes.data.local.LabelEntity
import com.kurupdevs.mynotes.ui.common.DottedBackground
import com.kurupdevs.mynotes.ui.common.EmptyState
import com.kurupdevs.mynotes.ui.home.LABEL_COLORS
import com.kurupdevs.mynotes.ui.home.labelDot
import com.kurupdevs.mynotes.ui.theme.SheetShape
import com.kurupdevs.mynotes.ui.theme.TitleWhite
import kotlinx.coroutines.launch

class LabelsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as NotesApp).container.notes
    val labels = repo.labelsFlow()
    val error = mutableStateOf<String?>(null)

    fun create(name: String, color: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val ok = repo.createLabel(name, color)
            if (!ok) error.value = "Label already exists or limit reached (50 max)."
            else onDone()
        }
    }

    fun rename(id: String, name: String, onDone: () -> Unit) {
        viewModelScope.launch {
            val ok = repo.renameLabel(id, name)
            if (!ok) error.value = "Label already exists."
            else onDone()
        }
    }

    fun recolor(id: String, color: String) {
        viewModelScope.launch { repo.recolorLabel(id, color) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repo.deleteLabel(id) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelsScreen(dark: Boolean, onBack: () -> Unit, onOpenLabel: (String) -> Unit) {
    val context = LocalContext.current
    val vm: LabelsViewModel = viewModel()
    val labels by vm.labels.collectAsState(initial = emptyList())
    val ink = if (dark) TitleWhite else Color(0xFF141210)
    var sheetFor by remember { mutableStateOf<LabelEntity?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleteFor by remember { mutableStateOf<LabelEntity?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            IconButton(
                onClick = { creating = true },
                modifier = Modifier.size(56.dp).clip(CircleShape).background(if (dark) TitleWhite else Color(0xFF141210))
            ) {
                Icon(Icons.Filled.Add, "New label", tint = if (dark) Color.Black else Color.White)
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            DottedBackground(dark)
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 34.dp, start = 4.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = ink)
                    }
                    Text("Labels", style = MaterialTheme.typography.displayLarge, color = ink)
                }
                vm.error.value?.let {
                    Text(it, color = Color(0xFFEA7B53), modifier = Modifier.padding(horizontal = 16.dp))
                }
                if (labels.isEmpty()) {
                    EmptyState(
                        title = "No labels yet",
                        subtitle = "Labels keep things tidy. Make your first one.",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        Modifier.weight(1f).padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
                    ) {
                        items(labels, key = { it.id }) { l ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                    .background(if (dark) Color(0xFF141214) else Color.White)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onOpenLabel(l.id) }
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(Modifier.size(16.dp).clip(CircleShape).background(labelDot(l.color)))
                                Spacer(Modifier.width(12.dp))
                                Text(l.name, style = MaterialTheme.typography.bodyLarge, color = ink, modifier = Modifier.weight(1f))
                                IconButton(onClick = { sheetFor = l }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Filled.Edit, "Edit label", tint = ink.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { deleteFor = l }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Filled.Delete, "Delete label", tint = ink.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (creating || sheetFor != null) {
        LabelEditSheet(
            dark = dark,
            existing = sheetFor,
            onDismiss = { creating = false; sheetFor = null },
            onSave = { name, color ->
                if (sheetFor != null) {
                    vm.rename(sheetFor!!.id, name) { creating = false; sheetFor = null }
                    vm.recolor(sheetFor!!.id, color)
                } else {
                    vm.create(name, color) { creating = false }
                }
            }
        )
    }
    deleteFor?.let { l ->
        AlertDialog(
            onDismissRequest = { deleteFor = null },
            title = { Text("Delete '${l.name}'?", color = ink) },
            text = { Text("Notes keep their content, just lose the label.", color = ink.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(onClick = { vm.delete(l.id); deleteFor = null }) {
                    Text("Delete", color = Color(0xFFEA7B53))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteFor = null }) { Text("Cancel", color = ink.copy(alpha = 0.7f)) }
            },
            containerColor = if (dark) Color(0xFF1A1A1A) else Color.White
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabelEditSheet(
    dark: Boolean,
    existing: LabelEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var color by remember { mutableStateOf(existing?.color ?: "coral") }
    val ink = if (dark) TitleWhite else Color(0xFF141210)
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState, shape = SheetShape,
        containerColor = if (dark) Color(0xFF111111) else Color.White
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(if (existing == null) "New label" else "Edit label", style = MaterialTheme.typography.headlineMedium, color = ink)
            Spacer(Modifier.height(12.dp))
            BasicTextField(
                value = name,
                onValueChange = { if (it.length <= 30) name = it },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = ink),
                cursorBrush = SolidColor(ink),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (dark) Color(0xFF1E1E1E) else Color(0xFFF4F1EA))
                    .padding(14.dp),
                decorationBox = { inner ->
                    if (name.isEmpty()) Text("Label name", color = ink.copy(alpha = 0.4f))
                    inner()
                }
            )
            Spacer(Modifier.height(12.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(LABEL_COLORS) { c ->
                    Box(
                        Modifier.size(44.dp).clip(CircleShape)
                            .background(labelDot(c))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { color = c }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (color == c) Box(Modifier.size(16.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.9f)))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(if (dark) TitleWhite else Color(0xFF141210))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { if (name.isNotBlank()) onSave(name.trim(), color) }
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Save", style = MaterialTheme.typography.labelLarge, color = if (dark) Color.Black else Color.White)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
