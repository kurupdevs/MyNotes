package com.kurupdevs.mynotes.ui.reminders

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kurupdevs.mynotes.NotesApp
import com.kurupdevs.mynotes.data.local.NoteEntity
import com.kurupdevs.mynotes.ui.common.DottedBackground
import com.kurupdevs.mynotes.ui.common.EmptyState
import com.kurupdevs.mynotes.ui.theme.TitleWhite
import com.kurupdevs.mynotes.ui.theme.cardColor
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

class RemindersViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = (app as NotesApp).container.notes
    val reminders = repo.allRemindersFlow()

    fun complete(id: String) = viewModelScope.launch { repo.completeReminder(id) }
    fun snooze(id: String, at: Long) = viewModelScope.launch { repo.snoozeReminder(id, at) }
    fun dismiss(id: String) = viewModelScope.launch { repo.setReminder(id, null, null) }
}

@Composable
fun RemindersScreen(dark: Boolean, onBack: () -> Unit, onOpenNote: (String) -> Unit) {
    val vm: RemindersViewModel = viewModel()
    val all by vm.reminders.collectAsState(initial = emptyList())
    val ink = if (dark) TitleWhite else Color(0xFF141210)
    val now = System.currentTimeMillis()
    val upcoming = all.filter { !it.reminderDone && (it.reminderAt ?: 0) >= now }
    val missed = all.filter { !it.reminderDone && (it.reminderAt ?: Long.MAX_VALUE) < now }
    val done = all.filter { it.reminderDone }

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
                    Text("Reminders", style = MaterialTheme.typography.displayLarge, color = ink)
                }
                if (all.isEmpty()) {
                    EmptyState(
                        title = "No reminders",
                        subtitle = "Set one from any note and future-you says thanks.",
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        Modifier.weight(1f).padding(horizontal = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
                    ) {
                        if (missed.isNotEmpty()) {
                            item { SectionHeader("Missed", Color(0xFFEA7B53)) }
                            items(missed, key = { it.id }) { n ->
                                ReminderRow(n, dark, missed = true,
                                    onOpen = { onOpenNote(n.id) },
                                    onComplete = { vm.complete(n.id) },
                                    onSnooze = { vm.snooze(n.id, System.currentTimeMillis() + 3600_000) },
                                    onDismiss = { vm.dismiss(n.id) })
                            }
                        }
                        if (upcoming.isNotEmpty()) {
                            item { SectionHeader("Upcoming", ink.copy(alpha = 0.6f)) }
                            items(upcoming, key = { it.id }) { n ->
                                ReminderRow(n, dark, missed = false,
                                    onOpen = { onOpenNote(n.id) },
                                    onComplete = { vm.complete(n.id) },
                                    onSnooze = { vm.snooze(n.id, System.currentTimeMillis() + 3600_000) },
                                    onDismiss = { vm.dismiss(n.id) })
                            }
                        }
                        if (done.isNotEmpty()) {
                            item { SectionHeader("Done", ink.copy(alpha = 0.4f)) }
                            items(done, key = { it.id }) { n ->
                                ReminderRow(n, dark, missed = false, dimmed = true,
                                    onOpen = { onOpenNote(n.id) },
                                    onComplete = {}, onSnooze = {}, onDismiss = { vm.dismiss(n.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String, color: Color) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = color, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun ReminderRow(
    note: NoteEntity,
    dark: Boolean,
    missed: Boolean,
    dimmed: Boolean = false,
    onOpen: () -> Unit,
    onComplete: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    val ink = if (dark) TitleWhite else Color(0xFF141210)
    var offsetX by remember { androidx.compose.runtime.mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(if (dark) Color(0xFF141214) else Color.White)
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            when {
                                offsetX < -120 -> { onComplete(); }
                                offsetX > 120 -> { onSnooze(); }
                            }
                            // snap back (animate omitted for brevity)
                        }
                    },
                    onHorizontalDrag = { _, d -> offsetX += d }
                )
            }
            .offset { IntOffset(offsetX.roundToInt().coerceIn(-240, 240), 0) }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onOpen
            )
            .padding(14.dp)
            .then(if (dimmed) Modifier else Modifier)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(12.dp).clip(CircleShape).background(cardColor(note.color, dark)))
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    note.title.ifEmpty { "Untitled" },
                    style = MaterialTheme.typography.bodyLarge, color = ink.copy(alpha = if (dimmed) 0.5f else 1f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    fmtReminder(note.reminderAt ?: 0),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (missed) Color(0xFFEA7B53) else ink.copy(alpha = 0.55f)
                )
            }
            if (!dimmed) {
                Icon(
                    Icons.Filled.Notifications, null,
                    tint = ink.copy(alpha = 0.4f), modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

fun fmtReminder(ms: Long): String {
    val now = System.currentTimeMillis()
    val cal = java.util.Calendar.getInstance()
    val today = cal.get(java.util.Calendar.DAY_OF_YEAR)
    cal.timeInMillis = ms
    val day = cal.get(java.util.Calendar.DAY_OF_YEAR)
    val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(ms))
    return when {
        ms < now -> "Missed · " + SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(ms))
        day == today -> "Today · $time"
        day == today + 1 -> "Tomorrow · $time"
        else -> SimpleDateFormat("EEE, MMM d · h:mm a", Locale.getDefault()).format(Date(ms))
    }
}
