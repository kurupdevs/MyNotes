package com.kurupdevs.mynotes.ui.share

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kurupdevs.mynotes.NotesApp
import com.kurupdevs.mynotes.data.remote.Jsons
import com.kurupdevs.mynotes.ui.common.DottedBackground
import com.kurupdevs.mynotes.ui.common.EmptyState
import com.kurupdevs.mynotes.ui.theme.MenuBg
import com.kurupdevs.mynotes.ui.theme.MenuDots
import com.kurupdevs.mynotes.ui.theme.TitleWhite
import kotlinx.coroutines.launch

class ShareViewModel(app: Application, private val noteId: String) : AndroidViewModel(app) {
    private val c = (app as NotesApp).container
    private val repo = c.notes
    val note = repo.noteById(noteId)
    val isOwner = mutableStateOf(true)
    val toast = mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            repo.noteById(noteId).collect { n ->
                isOwner.value = n == null || n.ownerId == repo.uid()
            }
        }
    }

    fun invite(uidOrEmail: String, role: String) {
        viewModelScope.launch {
            // v1: invite by uid (email -> uid resolution needs a directory; uid paste works today)
            val ok = repo.shareWith(noteId, uidOrEmail.trim(), role)
            toast.value = if (ok) "Invite sent" else "Couldn't share this note"
        }
    }

    fun remove(uid: String) {
        viewModelScope.launch { repo.unshare(noteId, uid) }
    }

    fun stopSharing() {
        viewModelScope.launch { repo.stopSharing(noteId) }
    }

    fun leave() {
        viewModelScope.launch { repo.leaveShared(noteId) }
    }
}

@Composable
fun ShareScreen(noteId: String, dark: Boolean, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val vm: ShareViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            ShareViewModel(app, noteId) as T
    })
    val note by vm.note.collectAsState(initial = null)
    val ink = if (dark) TitleWhite else Color(0xFF141210)
    val clipboard = LocalClipboardManager.current
    var inviteText by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("viewer") }
    var roleMenu by remember { mutableStateOf(false) }
    var confirmStop by remember { mutableStateOf(false) }

    val collabs = note?.let { Jsons.collaborators(it.collaboratorsJson) }.orEmpty()

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
                    Text("Share note", style = MaterialTheme.typography.displayLarge, color = ink)
                }
                if (!vm.isOwner.value) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "This note was shared with you. Only the owner can manage sharing.",
                            style = MaterialTheme.typography.bodyMedium, color = ink.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Leave shared note",
                            style = MaterialTheme.typography.labelLarge, color = Color(0xFFEA7B53),
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { vm.leave(); onBack() }.padding(8.dp)
                        )
                    }
                    return@Column
                }
                Column(Modifier.padding(horizontal = 16.dp)) {
                    // presence pill
                    if (collabs.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFA9D673)))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${collabs.size} ${if (collabs.size == 1) "person" else "people"} have access",
                                style = MaterialTheme.typography.labelLarge, color = ink.copy(alpha = 0.7f)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                    }
                    // invite row
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BasicTextField(
                            value = inviteText,
                            onValueChange = { inviteText = it },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = ink),
                            cursorBrush = SolidColor(ink),
                            singleLine = true,
                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(50))
                                .background(if (dark) Color(0xFF1A1A1A) else Color(0xFFE7E1D3))
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            decorationBox = { inner ->
                                if (inviteText.isEmpty()) Text("Their user ID", color = ink.copy(alpha = 0.4f))
                                inner()
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Box {
                            Text(
                                if (role == "viewer") "Viewer" else "Editor",
                                style = MaterialTheme.typography.labelLarge, color = ink,
                                modifier = Modifier.clip(CircleShape)
                                    .background(ink.copy(alpha = 0.1f))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { roleMenu = true }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            )
                            DropdownMenu(
                                expanded = roleMenu, onDismissRequest = { roleMenu = false },
                                modifier = Modifier.background(if (dark) MenuBg else Color.White)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Viewer — read only", color = if (dark) MenuDots else ink) },
                                    onClick = { role = "viewer"; roleMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Editor — can edit", color = if (dark) MenuDots else ink) },
                                    onClick = { role = "editor"; roleMenu = false }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(16.dp))
                            .background(if (dark) TitleWhite else Color(0xFF141210))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (inviteText.isNotBlank()) {
                                    vm.invite(inviteText, role)
                                    inviteText = ""
                                }
                            }
                            .padding(14.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Send invite", style = MaterialTheme.typography.labelLarge, color = if (dark) Color.Black else Color.White)
                    }
                    Text(
                        "Labels are yours only — they never get shared.",
                        style = MaterialTheme.typography.labelMedium,
                        color = ink.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(Modifier.height(16.dp))
                    // collaborator list
                    if (collabs.isEmpty()) {
                        EmptyState(title = "Just you for now", subtitle = "Invite someone to collab on this note.")
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(collabs.entries.toList()) { (uid, c) ->
                                Row(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                        .background(if (dark) Color(0xFF141214) else Color.White)
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        Modifier.size(38.dp).clip(CircleShape)
                                            .background(Color(0xFF8E7BD8).copy(alpha = 0.35f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(uid.take(1).uppercase(), style = MaterialTheme.typography.labelLarge, color = ink)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(uid.take(12) + "…", style = MaterialTheme.typography.bodyMedium, color = ink)
                                        Text(c.role.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelMedium, color = ink.copy(alpha = 0.55f))
                                    }
                                    IconButton(onClick = { vm.remove(uid) }, modifier = Modifier.size(36.dp)) {
                                        Icon(Icons.Filled.Close, "Remove", tint = ink.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Stop sharing",
                            style = MaterialTheme.typography.labelLarge, color = Color(0xFFEA7B53),
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { confirmStop = true }.padding(8.dp)
                        )
                    }
                }
            }
        }
    }

    vm.toast.value?.let {
        // lightweight inline toast
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Text(
                it, style = MaterialTheme.typography.labelLarge, color = Color.White,
                modifier = Modifier.padding(bottom = 48.dp).clip(RoundedCornerShape(50))
                    .background(Color(0xFF222222)).padding(horizontal = 18.dp, vertical = 10.dp)
            )
        }
    }

    if (confirmStop) {
        AlertDialog(
            onDismissRequest = { confirmStop = false },
            title = { Text("Stop sharing?", color = ink) },
            text = { Text("Remove access for ${collabs.size} ${if (collabs.size == 1) "person" else "people"}? They'll lose access immediately.", color = ink.copy(alpha = 0.7f)) },
            confirmButton = {
                TextButton(onClick = { vm.stopSharing(); confirmStop = false; onBack() }) {
                    Text("Stop sharing", color = Color(0xFFEA7B53))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmStop = false }) { Text("Cancel", color = ink.copy(alpha = 0.7f)) }
            },
            containerColor = if (dark) Color(0xFF1A1A1A) else Color.White
        )
    }
}
