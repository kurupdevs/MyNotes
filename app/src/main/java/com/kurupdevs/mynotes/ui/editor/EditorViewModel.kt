package com.kurupdevs.mynotes.ui.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kurupdevs.mynotes.NotesApp
import com.kurupdevs.mynotes.data.local.NoteEntity
import com.kurupdevs.mynotes.data.model.Attachment
import com.kurupdevs.mynotes.data.model.Block
import com.kurupdevs.mynotes.data.model.BlockKind
import com.kurupdevs.mynotes.data.model.NoteColor
import com.kurupdevs.mynotes.data.model.SyncStatus
import com.kurupdevs.mynotes.data.remote.Jsons
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class EditorViewModel(app: Application, private val noteId: String) : AndroidViewModel(app) {
    private val c = (app as NotesApp).container
    private val repo = c.notes

    val note = MutableStateFlow<NoteEntity?>(null)
    val title = MutableStateFlow("")
    val blocks = MutableStateFlow<List<Block>>(emptyList())
    val saving = MutableStateFlow(false)
    val savedTick = MutableStateFlow(0L)
    val attachments = MutableStateFlow<List<Attachment>>(emptyList())
    val conflictRemote = MutableStateFlow<List<Block>?>(null)
    val locked = MutableStateFlow(false)
    val unlocked = MutableStateFlow(false)
    val canUndo = MutableStateFlow(false)
    val canRedo = MutableStateFlow(false)
    val focusedBlock = MutableStateFlow<String?>(null)

    private val undoStack = ArrayDeque<Pair<String, List<Block>>>()
    private val redoStack = ArrayDeque<Pair<String, List<Block>>>()
    private var saveJob: Job? = null
    private var loaded = false

    init {
        viewModelScope.launch {
            repo.noteById(noteId).collect { e ->
                if (e == null) return@collect
                note.value = e
                locked.value = e.locked
                if (e.syncStatus == SyncStatus.CONFLICT.name && e.conflictJson != null) {
                    conflictRemote.value = Jsons.blocks(e.conflictJson)
                } else {
                    conflictRemote.value = null
                }
                if (!loaded) {
                    loaded = true
                    title.value = e.title
                    blocks.value = Jsons.blocks(e.blocksJson).sortedBy { it.order }
                    attachments.value = Jsons.attachments(e.attachmentsJson)
                    pushUndoSnapshot()
                } else if (!editing) {
                    // external update (sync) — refresh silently when user isn't mid-edit
                    title.value = e.title
                    blocks.value = Jsons.blocks(e.blocksJson).sortedBy { it.order }
                    attachments.value = Jsons.attachments(e.attachmentsJson)
                }
            }
        }
    }

    private var editing = false

    private fun pushUndoSnapshot() {
        undoStack.addLast(title.value to blocks.value)
        if (undoStack.size > 50) undoStack.removeFirst()
        redoStack.clear()
        canUndo.value = undoStack.size > 1
        canRedo.value = false
    }

    fun undo() {
        if (undoStack.size <= 1) return
        redoStack.addLast(title.value to blocks.value)
        undoStack.removeLast()
        val (t, b) = undoStack.last()
        title.value = t; blocks.value = b
        canUndo.value = undoStack.size > 1
        canRedo.value = true
        scheduleSave()
    }

    fun redo() {
        val (t, b) = redoStack.removeLastOrNull() ?: return
        undoStack.addLast(title.value to blocks.value)
        title.value = t; blocks.value = b
        canUndo.value = true
        canRedo.value = redoStack.isNotEmpty()
        scheduleSave()
    }

    fun onTitleChange(t: String) {
        editing = true
        title.value = t
        scheduleSave()
    }

    fun onBlockText(id: String, text: String) {
        editing = true
        blocks.value = blocks.value.map { if (it.id == id) it.copy(text = text) else it }
        scheduleSave()
    }

    fun onBlockFocus(id: String?) {
        focusedBlock.value = id
    }

    fun splitBlock(id: String) {
        // Enter at end of a block -> insert new block after
        val list = blocks.value.toMutableList()
        val idx = list.indexOfFirst { it.id == id }
        if (idx == -1) return
        val cur = list[idx]
        val nb = Block(UUID.randomUUID().toString(), if (cur.kind == BlockKind.TODO) BlockKind.TODO else BlockKind.P, order = 0)
        list.add(idx + 1, nb)
        blocks.value = list.mapIndexed { i, b -> b.copy(order = i) }
        pushUndoSnapshot()
        scheduleSave()
        focusedBlock.value = nb.id
    }

    fun toggleMark(mark: String) {
        val id = focusedBlock.value ?: return
        blocks.value = blocks.value.map {
            if (it.id == id) {
                val m = it.marks.toMutableList()
                if (mark in m) m.remove(mark) else m.add(mark)
                it.copy(marks = m)
            } else it
        }
        pushUndoSnapshot()
        scheduleSave()
    }

    fun setKind(kind: BlockKind) {
        val id = focusedBlock.value ?: return
        blocks.value = blocks.value.map { if (it.id == id) it.copy(kind = kind) else it }
        pushUndoSnapshot()
        scheduleSave()
    }

    fun toggleChecklistLine() {
        val id = focusedBlock.value ?: return
        blocks.value = blocks.value.map {
            if (it.id == id) it.copy(kind = if (it.kind == BlockKind.TODO) BlockKind.P else BlockKind.TODO) else it
        }
        pushUndoSnapshot()
        scheduleSave()
    }

    fun toggleTodo(id: String) {
        blocks.value = blocks.value.map { if (it.id == id) it.copy(checked = !it.checked) else it }
        pushUndoSnapshot()
        scheduleSave()
    }

    fun deleteBlock(id: String) {
        val list = blocks.value.filter { it.id != id }
        blocks.value = if (list.isEmpty()) listOf(Block(UUID.randomUUID().toString(), BlockKind.P)) else list
        pushUndoSnapshot()
        scheduleSave()
    }

    fun addBlock(kind: BlockKind = BlockKind.P) {
        val nb = Block(UUID.randomUUID().toString(), kind, order = blocks.value.size)
        blocks.value = blocks.value + nb
        pushUndoSnapshot()
        scheduleSave()
        focusedBlock.value = nb.id
    }

    fun clearFormatting() {
        val id = focusedBlock.value ?: return
        blocks.value = blocks.value.map { if (it.id == id) it.copy(marks = emptyList(), kind = BlockKind.P) else it }
        pushUndoSnapshot()
        scheduleSave()
    }

    fun updateCaption(attachmentId: String, caption: String) {
        attachments.value = attachments.value.map {
            if (it.id == attachmentId) it.copy(caption = caption) else it
        }
        scheduleSave()
    }

    fun scheduleSave() {
        saveJob?.cancel()
        saving.value = true
        val atts = attachments.value
        saveJob = viewModelScope.launch {
            delay(800)
            repo.updateBlocks(noteId, blocks.value, title.value, atts)
            saving.value = false
            savedTick.value = System.currentTimeMillis()
            editing = false
        }
    }

    fun saveNow() {
        viewModelScope.launch {
            saveJob?.cancel()
            repo.updateBlocks(noteId, blocks.value, title.value, attachments.value)
            saving.value = false
        }
    }

    // ---- note-level ops ----
    fun setColor(color: NoteColor) = viewModelScope.launch { repo.setColor(noteId, color) }
    fun togglePin() = viewModelScope.launch { repo.togglePin(noteId) }
    fun toggleFavorite() = viewModelScope.launch { repo.toggleFavorite(noteId) }
    fun archive(arch: Boolean) = viewModelScope.launch { repo.archive(noteId, arch) }
    fun trash() = viewModelScope.launch { repo.trash(noteId) }
    fun duplicate(onDone: (String) -> Unit) = viewModelScope.launch {
        repo.duplicate(noteId)?.let { onDone(it.id) }
    }
    fun setLabels(ids: List<String>) = viewModelScope.launch { repo.setLabels(noteId, ids) }
    fun setReminder(at: Long?, repeat: String?) = viewModelScope.launch { repo.setReminder(noteId, at, repeat) }
    fun setLocked(l: Boolean) = viewModelScope.launch { repo.setLocked(noteId, l) }
    fun resolveConflict(keepLocal: Boolean) = viewModelScope.launch {
        repo.resolveConflict(noteId, keepLocal)
        conflictRemote.value = null
    }

    fun attachImageFile(file: File, onOcr: (String) -> Unit = {}) {
        viewModelScope.launch {
            val att = repo.attachImage(noteId, file) ?: return@launch
            attachments.value = attachments.value + att
            // on-device OCR in background -> searchable text
            launch {
                val text = com.kurupdevs.mynotes.ocr.OcrProcessor.extractText(file)
                if (text.isNotBlank()) {
                    repo.updateAttachment(noteId, att.copy(ocrText = text))
                    attachments.value = attachments.value.map { if (it.id == att.id) it.copy(ocrText = text) else it }
                }
            }
        }
    }

    fun attachAudioFile(file: File, durationMs: Long) {
        viewModelScope.launch {
            val att = repo.attachAudio(noteId, file, durationMs) ?: return@launch
            attachments.value = attachments.value + att
            blocks.value = Jsons.blocks(repo.getNote(noteId)?.blocksJson ?: "[]").sortedBy { it.order }
        }
    }

    /** Note cover thumbnail: saved as Attachment(kind="thumbnail"), no IMG block. Mirrors attachImageFile's upload path. */
    fun attachThumbnailFile(file: File) {
        viewModelScope.launch {
            val att = Attachment(
                id = UUID.randomUUID().toString(), kind = "thumbnail",
                localPath = file.absolutePath, mime = "image/webp", sizeBytes = file.length()
            )
            attachments.value = attachments.value + att
            scheduleSave()
            // Cloudinary upload in background (no block added for thumbnails)
            launch {
                try {
                    val res = c.uploader.uploadImage(file, repo.uid(), noteId)
                    val up = att.copy(
                        url = res.url, cloudinaryPublicId = res.publicId,
                        width = res.width, height = res.height, sizeBytes = res.sizeBytes
                    )
                    repo.updateAttachment(noteId, up)
                    attachments.value = attachments.value.map { if (it.id == att.id) up else it }
                    scheduleSave()
                } catch (_: Exception) {
                    // stays local-only; sync will retry later
                }
            }
        }
    }

    fun wordCount(): Int {
        var n = title.value.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
        blocks.value.forEach { b ->
            n += b.text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }
        }
        return n
    }

    fun exportTxt(): File {
        val dir = File(getApplication<Application>().cacheDir, "export").apply { mkdirs() }
        val f = File(dir, "note_${System.currentTimeMillis()}.txt")
        val sb = StringBuilder()
        if (title.value.isNotBlank()) sb.append(title.value).append("\n\n")
        blocks.value.forEach { b ->
            when (b.kind) {
                BlockKind.TODO -> sb.append(if (b.checked) "[x] " else "[ ] ").append(b.text).append('\n')
                BlockKind.H1 -> sb.append("# ").append(b.text).append('\n')
                BlockKind.H2 -> sb.append("## ").append(b.text).append('\n')
                BlockKind.LI -> sb.append("• ").append(b.text).append('\n')
                else -> sb.append(b.text).append('\n')
            }
        }
        f.writeText(sb.toString())
        return f
    }
}
