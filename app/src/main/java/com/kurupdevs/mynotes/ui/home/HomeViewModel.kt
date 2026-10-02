package com.kurupdevs.mynotes.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kurupdevs.mynotes.NotesApp
import com.kurupdevs.mynotes.data.local.LabelEntity
import com.kurupdevs.mynotes.data.local.NoteEntity
import com.kurupdevs.mynotes.data.model.NoteColor
import com.kurupdevs.mynotes.data.model.SortOrder
import com.kurupdevs.mynotes.data.remote.Jsons
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HomeFilter {
    data object All : HomeFilter
    data object Important : HomeFilter
    data object Todo : HomeFilter
    data class Label(val labelId: String) : HomeFilter
}

class HomeViewModel(app: Application) : AndroidViewModel(app) {
    private val c = (app as NotesApp).container
    private val repo = c.notes

    val filter = MutableStateFlow<HomeFilter>(HomeFilter.All)
    val selection = MutableStateFlow<Set<String>>(emptySet())
    val syncing = MutableStateFlow(false)
    val syncError = MutableStateFlow<String?>(null)
    val lastSync = c.prefs.lastSyncAt
    val isAnonymous = MutableStateFlow(true)

    private val notesFlow = repo.home()
    private val sortFlow = c.prefs.sortOrder
    private val pinnedFirstFlow = c.prefs.pinnedFirst

    val labels: StateFlow<List<LabelEntity>> = repo.labelsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<NoteEntity>> = combine(
        notesFlow, filter, sortFlow, pinnedFirstFlow, labels
    ) { notes, f, sort, pinnedFirst, _ ->
        var list = when (f) {
            is HomeFilter.All -> notes
            is HomeFilter.Important -> notes.filter { it.favorite }
            is HomeFilter.Todo -> notes.filter { n ->
                Jsons.blocks(n.blocksJson).any { it.kind.name == "TODO" }
            }
            is HomeFilter.Label -> notes.filter { n ->
                f.labelId in Jsons.labels(n.labelsJson)
            }
        }
        list = when (sort) {
            SortOrder.EDITED -> list.sortedByDescending { it.updatedAt }
            SortOrder.CREATED -> list.sortedByDescending { it.createdAt }
            SortOrder.TITLE -> list.sortedBy { it.title.lowercase() }
            SortOrder.COLOR -> list.sortedBy { it.color }
        }
        if (pinnedFirst) list = list.sortedByDescending { it.pinned } // stable: keeps order within groups
        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inSelection: StateFlow<Boolean> = selection
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        viewModelScope.launch {
            c.auth.authState().collect { isAnonymous.value = it?.isAnonymous ?: true }
        }
    }

    fun setFilter(f: HomeFilter) {
        filter.value = f
        selection.value = emptySet()
    }

    fun toggleSelect(id: String) {
        val s = selection.value.toMutableSet()
        if (id in s) s.remove(id) else s.add(id)
        selection.value = s
    }

    fun selectAll() {
        selection.value = notes.value.map { it.id }.toSet()
    }

    fun clearSelection() {
        selection.value = emptySet()
    }

    fun quickToggleTodo(noteId: String, blockId: String) {
        viewModelScope.launch { repo.toggleTodo(noteId, blockId) }
    }

    fun toggleFavorite(noteId: String) {
        viewModelScope.launch { repo.toggleFavorite(noteId) }
    }

    fun swipePin(noteId: String) {
        viewModelScope.launch {
            val ok = repo.togglePin(noteId)
            if (!ok) toast("Unpin something first — 20 max")
        }
    }

    fun swipeArchive(noteId: String, pinned: Boolean) {
        viewModelScope.launch {
            if (pinned) repo.togglePin(noteId) else {
                repo.archive(noteId, true)
                toast("Archived — Undo", undo = { viewModelScope.launch { repo.archive(noteId, false) } })
            }
        }
    }

    fun selectionPin() = act { repo.togglePin(it) }
    fun selectionArchive() = act { repo.archive(it, true) }
    fun selectionDelete() = act { repo.trash(it) }
    fun selectionColor(color: NoteColor) = act { repo.setColor(it, color) }
    fun selectionLabels(labelIds: List<String>) = act { repo.setLabels(it, labelIds) }

    private fun act(fn: suspend (String) -> Unit) {
        val ids = selection.value.toList()
        selection.value = emptySet()
        viewModelScope.launch { ids.forEach { fn(it) } }
    }

    private var syncJob: Job? = null
    fun syncNow() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            syncing.value = true
            syncError.value = null
            try {
                com.kurupdevs.mynotes.data.repo.SyncEngine.pushAll(c)
            } catch (t: Throwable) {
                syncError.value = "Sync hiccup. We'll retry — your notes are safe on this phone."
            } finally {
                syncing.value = false
            }
        }
    }

    // ---- one-shot UI events ----
    private val _toast = MutableStateFlow<ToastMsg?>(null)
    val toast: StateFlow<ToastMsg?> = _toast

    data class ToastMsg(val text: String, val undo: (() -> Unit)? = null, val id: Long = System.nanoTime())

    fun toast(text: String, undo: (() -> Unit)? = null) {
        _toast.value = ToastMsg(text, undo)
    }

    fun toastShown() {
        _toast.value = null
    }

    fun noteCountForChip(): Int = notesFlowValue.size

    private var notesFlowValue: List<NoteEntity> = emptyList()

    private val _backupNudge = MutableStateFlow(false)
    val backupNudge: StateFlow<Boolean> = _backupNudge

    init {
        viewModelScope.launch { notesFlow.collect { notesFlowValue = it } }
        // gentle backup nudge: anonymous + 3+ notes + asked fewer than 3 times
        viewModelScope.launch {
            delay(2000)
            val count = c.db.noteDao().countLive(repo.uid())
            val asks = c.prefs.backupAsks.first()
            val anon = c.auth.isAnonymous()
            if (anon && count >= 3 && asks < 3) {
                _backupNudge.value = true
                c.prefs.bumpBackupAsks()
            }
        }
    }

    fun dismissBackupNudge() {
        _backupNudge.value = false
    }
}
