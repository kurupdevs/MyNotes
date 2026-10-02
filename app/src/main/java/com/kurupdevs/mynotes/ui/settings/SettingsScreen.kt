package com.kurupdevs.mynotes.ui.settings

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.kurupdevs.mynotes.data.model.NoteColor
import com.kurupdevs.mynotes.data.model.SortOrder
import com.kurupdevs.mynotes.data.repo.AccountInfo
import com.kurupdevs.mynotes.data.repo.SyncEngine
import com.kurupdevs.mynotes.export.formatBytes
import com.kurupdevs.mynotes.ui.common.DottedBackground
import com.kurupdevs.mynotes.ui.editor.shareFile
import com.kurupdevs.mynotes.ui.theme.TitleWhite
import com.kurupdevs.mynotes.ui.theme.cardColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    val c = (app as NotesApp).container
    val account = MutableStateFlow<AccountInfo?>(null)
    val storage = MutableStateFlow<com.kurupdevs.mynotes.export.ExportManager.StorageInfo?>(null)
    val busy = MutableStateFlow(false)
    val msg = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            c.auth.authState().collect { account.value = it }
        }
        refreshStorage()
    }

    fun refreshStorage() {
        viewModelScope.launch { storage.value = c.export.storageBreakdown() }
    }

    fun linkGoogle() {
        viewModelScope.launch {
            busy.value = true
            c.auth.linkGoogle()
                .onSuccess { msg.value = "Backed up with Google" }
                .onFailure { msg.value = "Couldn't link — staying local for now" }
            busy.value = false
        }
    }

    fun unlinkGoogle() {
        viewModelScope.launch {
            c.auth.unlinkGoogle()
                .onSuccess { msg.value = "Unlinked. Notes stay on this phone." }
                .onFailure { msg.value = "Couldn't unlink" }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            busy.value = true
            try {
                SyncEngine.pushAll(c)
                msg.value = "Synced"
            } catch (t: Throwable) {
                msg.value = "Sync hiccup. We'll retry — your notes are safe on this phone."
            }
            busy.value = false
        }
    }

    fun exportJson() {
        viewModelScope.launch(Dispatchers.IO) {
            val f = c.export.exportAllJson()
            launch(Dispatchers.Main) { shareFile(c.context, f, "application/json") }
        }
    }

    fun exportTxt() {
        viewModelScope.launch(Dispatchers.IO) {
            val f = c.export.exportAllTxtZip()
            launch(Dispatchers.Main) { shareFile(c.context, f, "application/zip") }
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            c.context.cacheDir.listFiles()?.forEach { it.deleteRecursively() }
            launch(Dispatchers.Main) { msg.value = "Cache cleared" }
        }
    }

    fun logout() {
        viewModelScope.launch {
            c.auth.signOut()
            c.auth.ensureSignedIn()
            msg.value = "Logged out"
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            c.auth.deleteAccount()
                .onSuccess { c.auth.ensureSignedIn() }
                .onFailure { msg.value = "Couldn't delete account" }
        }
    }

    fun clearMsg() { msg.value = null }
}

@Composable
fun SettingsScreen(dark: Boolean, onBack: () -> Unit, onThemeChange: (String) -> Unit) {
    val context = LocalContext.current
    val vm: SettingsViewModel = viewModel()
    val c = (context.applicationContext as NotesApp).container
    val scope = rememberCoroutineScope()
    val ink = if (dark) TitleWhite else Color(0xFF141210)
    val sub = ink.copy(alpha = 0.6f)

    val account by vm.account.collectAsState()
    val storage by vm.storage.collectAsState()
    val busy by vm.busy.collectAsState()
    val msg by vm.msg.collectAsState()
    val theme by c.prefs.theme.collectAsState(initial = "dark")
    val wifiOnly by c.prefs.syncWifiOnly.collectAsState(initial = false)
    val autoSync by c.prefs.autoSync.collectAsState(initial = true)
    val lastSync by c.prefs.lastSyncAt.collectAsState(initial = 0L)
    val sortOrder by c.prefs.sortOrder.collectAsState(initial = SortOrder.EDITED)
    val defaultColor by c.prefs.defaultColor.collectAsState(initial = "default")
    val moveChecked by c.prefs.moveCheckedToBottom.collectAsState(initial = true)
    val appLock by c.prefs.appLock.collectAsState(initial = false)

    var confirmLogout by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var deleteTyped by remember { mutableStateOf("") }
    var showSort by remember { mutableStateOf(false) }

    Scaffold(containerColor = Color.Transparent) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            DottedBackground(dark)
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 34.dp, start = 4.dp, end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Filled.ArrowBack, "Back", tint = ink)
                        }
                        Text("Settings", style = MaterialTheme.typography.displayLarge, color = ink)
                    }
                }

                item {
                    SectionTitle("Account", ink)
                    val a = account
                    SettingCard(dark) {
                        Column {
                            Text(
                                if (a == null || a.isAnonymous) "Anonymous" else (a.name ?: a.email ?: "Google account"),
                                style = MaterialTheme.typography.bodyLarge, color = ink
                            )
                            if (a != null && !a.isAnonymous && a.email != null) {
                                Text(a.email, style = MaterialTheme.typography.bodySmall, color = sub)
                            } else {
                                Text("Notes live on this phone only", style = MaterialTheme.typography.bodySmall, color = sub)
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (a == null || a.isAnonymous) {
                                    ActionBtn(dark, "Link Google account", enabled = !busy) { vm.linkGoogle() }
                                } else {
                                    ActionBtn(dark, "Unlink", enabled = !busy) { vm.unlinkGoogle() }
                                }
                            }
                        }
                    }
                }

                item {
                    SectionTitle("Sync", ink)
                    SettingCard(dark) {
                        Column {
                            Text(
                                when {
                                    busy -> "Syncing…"
                                    lastSync == 0L -> "Never synced"
                                    else -> "Last synced ${com.kurupdevs.mynotes.ui.home.relTime(lastSync)}"
                                },
                                style = MaterialTheme.typography.bodyMedium, color = ink
                            )
                            Spacer(Modifier.height(8.dp))
                            ActionBtn(dark, "Sync now", enabled = !busy) { vm.syncNow() }
                            Spacer(Modifier.height(8.dp))
                            ToggleRow(dark, "Auto-sync", autoSync, ink) {
                                scope.launch(Dispatchers.IO) { c.prefs.setAutoSync(it) }
                            }
                            ToggleRow(dark, "Sync over Wi-Fi only", wifiOnly, ink) {
                                scope.launch(Dispatchers.IO) { c.prefs.setSyncWifiOnly(it) }
                            }
                        }
                    }
                }

                item {
                    SectionTitle("Appearance", ink)
                    SettingCard(dark) {
                        Column {
                            Text("Theme", style = MaterialTheme.typography.bodyLarge, color = ink)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("dark" to "Dark", "light" to "Light", "system" to "System").forEach { (v, l) ->
                                    Text(
                                        l,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (theme == v) Color.White else ink,
                                        modifier = Modifier.clip(CircleShape)
                                            .background(if (theme == v) Color(0xFFEA7B53) else ink.copy(alpha = 0.1f))
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { onThemeChange(v) }
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    SectionTitle("Notes", ink)
                    SettingCard(dark) {
                        Column {
                            Text("Sort by", style = MaterialTheme.typography.bodyLarge, color = ink)
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    SortOrder.EDITED to "Last edited",
                                    SortOrder.CREATED to "Created",
                                    SortOrder.TITLE to "Title A–Z",
                                    SortOrder.COLOR to "Color"
                                ).forEach { (v, l) ->
                                    Text(
                                        l,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (sortOrder == v) Color.White else ink,
                                        modifier = Modifier.clip(CircleShape)
                                            .background(if (sortOrder == v) Color(0xFFEA7B53) else ink.copy(alpha = 0.1f))
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { scope.launch(Dispatchers.IO) { c.prefs.setSort(v) } }
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Default note color", style = MaterialTheme.typography.bodyLarge, color = ink)
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(NoteColor.entries) { nc ->
                                    Box(
                                        Modifier.size(40.dp).clip(CircleShape)
                                            .background(cardColor(nc.key, dark))
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null
                                            ) { scope.launch(Dispatchers.IO) { c.prefs.setDefaultColor(nc.key) } },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (defaultColor == nc.key) {
                                            Icon(Icons.Filled.Check, null, tint = Color(0xFF3D1508), modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            ToggleRow(dark, "Move checked items to bottom", moveChecked, ink) {
                                scope.launch(Dispatchers.IO) { c.prefs.setMoveCheckedToBottom(it) }
                            }
                            ToggleRow(dark, "Lock app with biometrics", appLock, ink) {
                                scope.launch(Dispatchers.IO) { c.prefs.setAppLock(it) }
                            }
                        }
                    }
                }

                item {
                    SectionTitle("Storage", ink)
                    SettingCard(dark) {
                        val s = storage
                        if (s == null) {
                            Text("Calculating…", color = sub)
                        } else {
                            Column {
                                StorageRow("Notes", "${s.noteCount} notes", sub, ink)
                                StorageRow("Images", formatBytes(s.imageBytes), sub, ink)
                                StorageRow("Voice", formatBytes(s.audioBytes), sub, ink)
                                StorageRow("Trash", "${s.trashCount} notes", sub, ink)
                                Spacer(Modifier.height(8.dp))
                                ActionBtn(dark, "Clear cache") { vm.clearCache() }
                            }
                        }
                    }
                }

                item {
                    SectionTitle("Export", ink)
                    SettingCard(dark) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ActionBtn(dark, "Export JSON") { vm.exportJson() }
                            ActionBtn(dark, "Export TXT (zip)") { vm.exportTxt() }
                        }
                    }
                }

                item {
                    SectionTitle("About", ink)
                    SettingCard(dark) {
                        Column {
                            Text("My Notes 1.0", style = MaterialTheme.typography.bodyLarge, color = ink)
                            Text("Made free forever.", style = MaterialTheme.typography.bodySmall, color = sub)
                        }
                    }
                }

                item {
                    SectionTitle("Danger zone", Color(0xFFEA7B53))
                    SettingCard(dark) {
                        Column {
                            Text(
                                "Log out", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFEA7B53),
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { confirmLogout = true }.padding(vertical = 8.dp)
                            )
                            Text(
                                "Delete account", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFEA7B53),
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { confirmDelete = true; deleteTyped = "" }.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            msg?.let { m ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                    Text(
                        m, style = MaterialTheme.typography.labelLarge, color = Color.White,
                        modifier = Modifier.padding(bottom = 48.dp).clip(RoundedCornerShape(50))
                            .background(Color(0xFF222222)).padding(horizontal = 18.dp, vertical = 10.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { vm.clearMsg() }
                    )
                }
            }
        }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Log out?", color = ink) },
            text = { Text("Anonymous session ends. Your local notes stay on this phone.", color = sub) },
            confirmButton = {
                TextButton(onClick = { vm.logout(); confirmLogout = false }) { Text("Log out", color = Color(0xFFEA7B53)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Cancel", color = sub) }
            },
            containerColor = if (dark) Color(0xFF1A1A1A) else Color.White
        )
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete account?", color = ink) },
            text = {
                Column {
                    Text("Type DELETE to wipe your server data. Local notes stay unless you clear them.", color = sub)
                    Spacer(Modifier.height(8.dp))
                    BasicTextField(
                        value = deleteTyped,
                        onValueChange = { deleteTyped = it },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = ink),
                        cursorBrush = SolidColor(ink),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(if (dark) Color(0xFF222222) else Color(0xFFF4F1EA))
                            .padding(12.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { if (deleteTyped == "DELETE") { vm.deleteAccount(); confirmDelete = false } },
                    enabled = deleteTyped == "DELETE"
                ) { Text("Delete", color = Color(0xFFEA7B53)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = sub) }
            },
            containerColor = if (dark) Color(0xFF1A1A1A) else Color.White
        )
    }
}

@Composable
private fun SectionTitle(text: String, color: Color) {
    Text(
        text, style = MaterialTheme.typography.labelLarge, color = color.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingCard(dark: Boolean, content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (dark) Color(0xFF141214) else Color.White)
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
private fun ActionBtn(dark: Boolean, label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (dark) Color.Black else Color.White,
        modifier = Modifier.clip(RoundedCornerShape(50))
            .background((if (dark) TitleWhite else Color(0xFF141210)).copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, enabled = enabled, onClick = onClick
            )
            .padding(horizontal = 18.dp, vertical = 10.dp)
    )
}

@Composable
private fun ToggleRow(dark: Boolean, label: String, checked: Boolean, ink: Color, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = ink, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun StorageRow(label: String, value: String, sub: Color, ink: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = sub, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = ink)
    }
}
