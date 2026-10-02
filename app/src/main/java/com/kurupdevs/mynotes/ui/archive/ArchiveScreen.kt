package com.kurupdevs.mynotes.ui.archive

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kurupdevs.mynotes.NotesApp
import com.kurupdevs.mynotes.ui.common.DottedBackground
import com.kurupdevs.mynotes.ui.common.EmptyState
import com.kurupdevs.mynotes.ui.home.NoteCard
import com.kurupdevs.mynotes.ui.theme.TitleWhite
import kotlinx.coroutines.launch

class ArchiveViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as NotesApp).container.notes
    val notes = repo.archived()
    fun unarchive(id: String) = viewModelScope.launch { repo.archive(id, false) }
}

@Composable
fun ArchiveScreen(dark: Boolean, onBack: () -> Unit, onOpenNote: (String) -> Unit) {
    val vm: ArchiveViewModel = viewModel()
    val notes by vm.notes.collectAsState(initial = emptyList())
    val ink = if (dark) TitleWhite else Color(0xFF141210)

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
                    Text("Archived", style = MaterialTheme.typography.displayLarge, color = ink)
                }
                if (notes.isEmpty()) {
                    EmptyState(
                        title = "Nothing archived",
                        subtitle = "Swipe left on a note to park it here.",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier.weight(1f).alpha(0.85f),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalItemSpacing = 12.dp
                    ) {
                        items(notes, key = { it.id }) { n ->
                            Box {
                                NoteCard(
                                    note = n, dark = dark, selected = false,
                                    onOpen = { onOpenNote(n.id) },
                                    onToggleTodo = {},
                                    onToggleFavorite = {},
                                    modifier = Modifier.fillMaxWidth().clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onOpenNote(n.id) }
                                )
                                IconButton(
                                    onClick = { vm.unarchive(n.id) },
                                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                                ) {
                                    Icon(Icons.Filled.Unarchive, "Unarchive", tint = Color.White.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
