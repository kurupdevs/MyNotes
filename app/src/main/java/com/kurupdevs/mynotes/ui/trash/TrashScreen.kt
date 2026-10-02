package com.kurupdevs.mynotes.ui.trash

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kurupdevs.mynotes.NotesApp
import com.kurupdevs.mynotes.ui.common.DottedBackground
import com.kurupdevs.mynotes.ui.common.EmptyState
import com.kurupdevs.mynotes.ui.theme.TitleWhite
import kotlinx.coroutines.launch

class TrashViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as NotesApp).container.notes
    val trash = repo.trash()
    fun restore(id: String) = viewModelScope.launch { repo.restore(id) }
    fun deleteForever(id: String) = viewModelScope.launch { repo.deleteForever(id) }
    fun emptyTrash() = viewModelScope.launch { repo.emptyTrash() }
}

@Composable
fun TrashScreen(dark: Boolean, onBack: () -> Unit) {
    val vm: TrashViewModel = viewModel()
    val notes by vm.trash.collectAsState(initial = emptyList())
    val ink = if (dark) TitleWhite else Color(0xFF141210)
    var confirmEmpty by remember { mutableStateOf(false) }
    var confirmOne by remember { mutableStateOf<String?>(null) }

    Scaffold(containerColor = Color.Transparent) { pad ->
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
                    Text("Trash", style = MaterialTheme.typography.displayLarge, color = ink, modifier = Modifier.weight(1f))
                    if (notes.isNotEmpty()) {
                        TextButton(onClick = { confirmEmpty = true }) {
                            Text("Empty trash", color = Color(0xFFEA7B53))
                        }
                    }
                }
                if (notes.isEmpty()) {
                    EmptyState(
                        title = "Trash is empty",
                        subtitle = "Nothing to see here.",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        Modifier.weight(1f).padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
                    ) {
                        items(notes, key = { it.id }) { n ->
                            val daysLeft = 30 - ((System.currentTimeMillis() - (n.deletedAt ?: 0)) / 86400000).toInt().coerceAtLeast(0)
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                    .background(if (dark) Color(0xFF141214) else Color.White)
                                    .alpha(0.75f)
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        n.title.ifEmpty { "Untitled" },
                                        style = MaterialTheme.typography.bodyLarge, color = ink,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "Deleted — $daysLeft days left",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = ink.copy(alpha = 0.5f)
                                    )
                                }
                                IconButton(onClick = { vm.restore(n.id) }) {
                                    Icon(Icons.Filled.Restore, "Restore", tint = ink.copy(alpha = 0.7f))
                                }
                                IconButton(onClick = { confirmOne = n.id }) {
                                    Icon(Icons.Filled.DeleteForever, "Delete forever", tint = Color(0xFFEA7B53).copy(alpha = 0.8f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmEmpty) {
        AlertDialog(
            onDismissRequest = { confirmEmpty = false },
            title = { Text("Delete ${notes.size} notes forever?", color = ink) },
            text = { Text("This can't be undone.", color = ink.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(onClick = { vm.emptyTrash(); confirmEmpty = false }) {
                    Text("Delete forever", color = Color(0xFFEA7B53))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmEmpty = false }) { Text("Cancel", color = ink.copy(alpha = 0.7f)) }
            },
            containerColor = if (dark) Color(0xFF1A1A1A) else Color.White
        )
    }
    confirmOne?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmOne = null },
            title = { Text("Delete forever?", color = ink) },
            text = { Text("No undo on this one.", color = ink.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(onClick = { vm.deleteForever(id); confirmOne = null }) {
                    Text("Delete forever", color = Color(0xFFEA7B53))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmOne = null }) { Text("Cancel", color = ink.copy(alpha = 0.7f)) }
            },
            containerColor = if (dark) Color(0xFF1A1A1A) else Color.White
        )
    }
}
