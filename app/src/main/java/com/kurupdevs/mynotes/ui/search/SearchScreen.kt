package com.kurupdevs.mynotes.ui.search

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kurupdevs.mynotes.NotesApp
import com.kurupdevs.mynotes.data.local.NoteEntity
import com.kurupdevs.mynotes.data.model.BlockKind
import com.kurupdevs.mynotes.data.remote.Jsons
import com.kurupdevs.mynotes.ui.common.DottedBackground
import com.kurupdevs.mynotes.ui.common.EmptyState
import com.kurupdevs.mynotes.ui.theme.TitleWhite
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SearchViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as NotesApp).container.notes
    val query = mutableStateOf("")
    val results = mutableStateOf<List<NoteEntity>>(emptyList())
    val searching = mutableStateOf(false)
    private var job: Job? = null

    fun onQuery(q: String) {
        query.value = q
        job?.cancel()
        job = viewModelScope.launch {
            delay(250)
            if (q.trim().length < 2) {
                results.value = emptyList(); searching.value = false; return@launch
            }
            searching.value = true
            results.value = repo.search(q)
            searching.value = false
        }
    }
}

@Composable
fun SearchScreen(dark: Boolean, onBack: () -> Unit, onOpenNote: (String) -> Unit) {
    val context = LocalContext.current
    val vm: SearchViewModel = viewModel()
    val focusRequester = remember { FocusRequester() }
    val ink = if (dark) TitleWhite else Color(0xFF141210)

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

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
                    Box(
                        Modifier.weight(1f).clip(RoundedCornerShape(50))
                            .background(if (dark) Color(0xFF1A1A1A) else Color(0xFFE7E1D3))
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BasicTextField(
                                value = vm.query.value,
                                onValueChange = { vm.onQuery(it) },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = ink),
                                cursorBrush = SolidColor(ink),
                                singleLine = true,
                                modifier = Modifier.weight(1f).focusRequester(focusRequester),
                                decorationBox = { inner ->
                                    if (vm.query.value.isEmpty()) Text(
                                        "Search notes…", color = ink.copy(alpha = 0.4f),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    inner()
                                }
                            )
                            if (vm.query.value.isNotEmpty()) {
                                Icon(
                                    Icons.Filled.Close, "Clear",
                                    tint = ink.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp).clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { vm.onQuery("") }
                                )
                            }
                        }
                    }
                }
                val results = vm.results.value
                if (vm.query.value.trim().length < 2) {
                    EmptyState(
                        title = "Find anything",
                        subtitle = "Titles, checklists, even text inside your pics.",
                        modifier = Modifier.weight(1f)
                    )
                } else if (results.isEmpty() && !vm.searching.value) {
                    EmptyState(
                        title = "No matches for '${vm.query.value}'",
                        subtitle = "Try fewer words or check spelling.",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        Modifier.weight(1f).padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
                    ) {
                        items(results, key = { it.id }) { n ->
                            SearchRow(note = n, query = vm.query.value, dark = dark, onClick = { onOpenNote(n.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchRow(note: NoteEntity, query: String, dark: Boolean, onClick: () -> Unit) {
    val ink = if (dark) TitleWhite else Color(0xFF141210)
    val blocks = Jsons.blocks(note.blocksJson)
    val snippet = blocks.firstOrNull { it.text.contains(query, ignoreCase = true) }?.text
        ?: blocks.firstOrNull { it.text.isNotBlank() }?.text.orEmpty()
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(if (dark) Color(0xFF141214) else Color.White)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            )
            .padding(14.dp)
    ) {
        Text(
            note.title.ifEmpty { "Untitled" },
            style = MaterialTheme.typography.headlineMedium, color = ink,
            maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        if (snippet.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                snippet, style = MaterialTheme.typography.bodyMedium,
                color = ink.copy(alpha = 0.65f), maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
    }
}
