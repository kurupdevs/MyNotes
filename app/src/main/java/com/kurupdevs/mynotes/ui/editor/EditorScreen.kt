package com.kurupdevs.mynotes.ui.editor

import android.app.Application
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kurupdevs.mynotes.NotesApp
import com.kurupdevs.mynotes.data.model.Block
import com.kurupdevs.mynotes.data.remote.Jsons
import com.kurupdevs.mynotes.ui.common.DottedBackground
import com.kurupdevs.mynotes.ui.home.ColorPickSheet
import com.kurupdevs.mynotes.ui.home.LabelPickSheet
import com.kurupdevs.mynotes.ui.home.relTime
import com.kurupdevs.mynotes.ui.theme.TitleWhite
import com.kurupdevs.mynotes.ui.theme.cardColor
import com.kurupdevs.mynotes.ui.theme.inkOnCard
import com.kurupdevs.mynotes.ui.voice.VoiceRecorderSheet
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val AvatarPalette = listOf(
    Color(0xFFEA7B53), Color(0xFF5B8DEF), Color(0xFF4CAF7D), Color(0xFF9B7EDE)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun EditorScreen(
    noteId: String,
    dark: Boolean,
    sharedScope: SharedTransitionScope,
    animScope: AnimatedVisibilityScope,
    onBack: () -> Unit,
    onShare: (String) -> Unit,
    onDuplicate: (String) -> Unit,
    onDrawNote: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val vm: EditorViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            EditorViewModel(app, noteId) as T
    })
    val note by vm.note.collectAsState()
    val title by vm.title.collectAsState()
    val blocks by vm.blocks.collectAsState()
    val saving by vm.saving.collectAsState()
    val atts by vm.attachments.collectAsState()
    val conflict by vm.conflictRemote.collectAsState()
    val locked by vm.locked.collectAsState()
    val unlocked by vm.unlocked.collectAsState()
    val canUndo by vm.canUndo.collectAsState()
    val canRedo by vm.canRedo.collectAsState()
    val container = (app as NotesApp).container
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    var sheetOpen by remember { mutableStateOf(false) }
    var colorOpen by remember { mutableStateOf(false) }
    var labelOpen by remember { mutableStateOf(false) }
    var reminderOpen by remember { mutableStateOf(false) }
    var voiceOpen by remember { mutableStateOf(false) }
    var attachOpen by remember { mutableStateOf(false) }
    var deleteConfirm by remember { mutableStateOf(false) }

    val labelEntities by container.notes.labelsFlow().collectAsState(initial = emptyList())
    val metaFmt = remember { SimpleDateFormat("EEE, MMM d, yyyy HH:mm", Locale.getDefault()) }

    val ink: Color
    val bg: Color
    val n = note
    if (n == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Loading…", color = if (dark) TitleWhite else Color(0xFF141210))
        }
        return
    }
    bg = if (dark) cardColor(n.color, dark) else Color.White
    ink = if (dark) inkOnCard(n.color) else Color(0xFF141210)

    val labelIds = remember(n.labelsJson) { Jsons.labels(n.labelsJson) }
    val firstLabelName = labelIds.firstNotNullOfOrNull { id ->
        labelEntities.firstOrNull { it.id == id }?.name
    }
    val collabs = remember(n.collaboratorsJson) { Jsons.collaborators(n.collaboratorsJson) }
    // presentational H1 section numbers ("1. Venue and Decor") — stored text untouched
    val h1Nums = remember(blocks) {
        var i = 0
        buildMap<String, Int> {
            blocks.forEach { b ->
                if (b.kind == com.kurupdevs.mynotes.data.model.BlockKind.H1) put(b.id, ++i)
            }
        }
    }

    // biometric gate
    LaunchedEffect(locked) {
        if (locked && !unlocked) {
            val activity = context as? FragmentActivity
            if (activity != null && container.locker.canUseBiometric()) {
                val ok = container.locker.unlock(activity)
                if (ok) vm.unlocked.value = true else onBack()
            } else {
                vm.unlocked.value = true // no biometric hardware -> allow
            }
        }
    }

    BackHandler {
        vm.saveNow()
        onBack()
    }

    // camera temp file
    val cameraFile = remember {
        File(context.cacheDir, "images").apply { mkdirs() }
            .let { File(it, "cam_${System.currentTimeMillis()}.jpg") }
    }
    val cameraUri = remember {
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", cameraFile)
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { uri ->
            scope.launch {
                val f = copyToCache(context, uri, "img")
                if (f != null) vm.attachImageFile(f)
            }
        }
    }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) vm.attachImageFile(cameraFile)
    }
    val thumbLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                val f = copyToCache(context, uri, "thumb")
                if (f != null) vm.attachThumbnailFile(f)
            }
        }
    }
    var notifPermAsked by remember { mutableStateOf(false) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(reminderOpen) {
        if (reminderOpen && !notifPermAsked &&
            android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notifPermAsked = true
            notifLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (locked && !unlocked) {
        Box(Modifier.fillMaxSize().background(if (dark) Color.Black else Color(0xFFF4F1EA)), contentAlignment = Alignment.Center) {
            Text("Locked", style = MaterialTheme.typography.headlineMedium, color = if (dark) TitleWhite else Color(0xFF141210))
        }
        return
    }

    with(sharedScope) {
        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier
                .sharedElement(sharedScope.rememberSharedContentState("note-$noteId"), animScope)
                .background(bg),
            topBar = {
                Row(
                    Modifier.fillMaxWidth().padding(top = 34.dp, start = 8.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { vm.saveNow(); onBack() }) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = ink)
                    }
                    // collaborator avatar stack, centered
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        if (collabs.isNotEmpty()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                collabs.keys.take(4).forEachIndexed { i, key ->
                                    Box(
                                        Modifier.offset(x = (-12 * i).dp)
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(AvatarPalette[i % AvatarPalette.size])
                                            .border(2.dp, bg, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            key.firstOrNull()?.uppercase() ?: "?",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    // dark circular done button
                    val doneBg = if (dark) Color.White else Color(0xFF1C1C1E)
                    Box(
                        Modifier.size(46.dp).clip(CircleShape).background(doneBg)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { vm.saveNow(); onBack() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Filled.Check, "Done",
                            tint = if (dark) Color(0xFF1C1C1E) else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            },
            bottomBar = {
                Column(Modifier.imePadding()) {
                    FormatPanel(dark = dark, ink = ink, vm = vm)
                    AttachRow(
                        ink = ink,
                        onImage = { attachOpen = true },
                        onVoice = { voiceOpen = true }
                    )
                    // meta bar
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Edited ${relTime(n.updatedAt)} · ${vm.wordCount()} words" +
                                (if (saving) " · Saving…" else " · Saved"),
                            style = MaterialTheme.typography.labelMedium,
                            color = ink.copy(alpha = 0.6f),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { sheetOpen = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.MoreVert, "Options", tint = ink.copy(alpha = 0.8f))
                        }
                        IconButton(onClick = { vm.undo() }, enabled = canUndo, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Undo, "Undo", tint = ink.copy(alpha = if (canUndo) 0.8f else 0.3f))
                        }
                        IconButton(onClick = { vm.redo() }, enabled = canRedo, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Redo, "Redo", tint = ink.copy(alpha = if (canRedo) 0.8f else 0.3f))
                        }
                    }
                }
            }
        ) { pad ->
            Box(Modifier.fillMaxSize().padding(pad)) {
                if (dark) DottedBackground(dark)
                Column(Modifier.fillMaxSize()) {
                    // title
                    BasicTextField(
                        value = title,
                        onValueChange = { vm.onTitleChange(it) },
                        textStyle = TextStyle(
                            fontFamily = MaterialTheme.typography.headlineMedium.fontFamily,
                            fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                            fontWeight = FontWeight.Bold,
                            color = ink
                        ),
                        cursorBrush = SolidColor(ink),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(onNext = {
                            vm.addBlock()
                            focusManager.clearFocus()
                        }),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                        decorationBox = { inner ->
                            if (title.isEmpty()) Text("Title", style = MaterialTheme.typography.headlineMedium, color = ink.copy(alpha = 0.35f))
                            inner()
                        }
                    )
                    // meta line: "<FirstLabel or 'Note'> | EEE, MMM d, yyyy HH:mm"
                    Text(
                        "${firstLabelName ?: "Note"} | ${metaFmt.format(Date(n.updatedAt))}",
                        style = MaterialTheme.typography.labelMedium,
                        color = ink.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 2.dp)
                    )
                    // blocks
                    LazyColumn(
                        Modifier.fillMaxSize().padding(horizontal = 20.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)
                    ) {
                        items(blocks, key = { it.id }) { block ->
                            BlockRow(
                                block = block,
                                atts = atts,
                                ink = ink,
                                dark = dark,
                                vm = vm,
                                sectionNo = h1Nums[block.id],
                                onDraw = { onDrawNote(noteId) }
                            )
                        }
                    }
                }
            }
        }
    }

    // ---- sheets & dialogs ----
    if (sheetOpen) {
        NoteOptionsSheet(
            dark = dark,
            pinned = n.pinned,
            locked = locked,
            archived = n.archived,
            favorite = n.favorite,
            firstLabelName = firstLabelName,
            updatedAt = n.updatedAt,
            onDismiss = { sheetOpen = false },
            onImage = { attachOpen = true },
            onVoice = { voiceOpen = true },
            onShare = { onShare(noteId) },
            onDraw = { onDrawNote(noteId) },
            onPin = { vm.togglePin() },
            onAddThumbnail = { thumbLauncher.launch("image/*") },
            onLabel = { labelOpen = true },
            onSend = { sendNote(context, title, blocks) },
            onDuplicate = { vm.duplicate(onDuplicate) },
            onReminder = { reminderOpen = true },
            onArchive = { vm.archive(!n.archived) },
            onColor = { colorOpen = true },
            onExport = { shareFile(context, vm.exportTxt(), "text/plain") },
            onFavorite = { vm.toggleFavorite() },
            onLock = { vm.setLocked(!locked) },
            onDelete = { deleteConfirm = true }
        )
    }
    if (colorOpen) {
        ColorPickSheet(dark = dark, onDismiss = { colorOpen = false }, onPick = {
            vm.setColor(it); colorOpen = false
        })
    }
    if (labelOpen) {
        LabelPickSheet(dark = dark, labels = labelEntities, onDismiss = { labelOpen = false }, onPick = {
            vm.setLabels(it); labelOpen = false
        })
    }
    if (reminderOpen) {
        ReminderDialog(
            dark = dark,
            current = n.reminderAt,
            repeat = n.reminderRepeat,
            onDismiss = { reminderOpen = false },
            onSet = { at, rep -> vm.setReminder(at, rep); reminderOpen = false },
            onClear = { vm.setReminder(null, null); reminderOpen = false }
        )
    }
    if (voiceOpen) {
        VoiceRecorderSheet(dark = dark, onDismiss = { voiceOpen = false }, onDone = { file, dur ->
            voiceOpen = false
            vm.attachAudioFile(file, dur)
        })
    }
    if (attachOpen) {
        AttachSheet(
            dark = dark,
            onDismiss = { attachOpen = false },
            onCamera = { attachOpen = false; cameraLauncher.launch(cameraUri) },
            onGallery = { attachOpen = false; galleryLauncher.launch("image/*") }
        )
    }
    if (deleteConfirm) {
        ConfirmDialog(
            dark = dark,
            title = "Delete this note?",
            body = "It'll sit in trash for 30 days first.",
            confirm = "Move to trash",
            onDismiss = { deleteConfirm = false },
            onConfirm = { deleteConfirm = false; vm.trash(); onBack() }
        )
    }
    conflict?.let { remote ->
        ConflictSheet(
            dark = dark,
            local = blocks,
            remote = remote,
            onDismiss = {},
            onKeepLocal = { vm.resolveConflict(true) },
            onKeepRemote = { vm.resolveConflict(false) }
        )
    }
}

fun copyToCache(context: android.content.Context, uri: android.net.Uri, prefix: String): File? {
    return try {
        val dir = File(context.cacheDir, "images").apply { mkdirs() }
        val f = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
        context.contentResolver.openInputStream(uri)?.use { ins ->
            f.outputStream().use { ins.copyTo(it) }
        }
        f
    } catch (_: Exception) {
        null
    }
}

fun shareFile(context: android.content.Context, file: File, mime: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = mime
        putExtra(android.content.Intent.EXTRA_STREAM, uri)
        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(android.content.Intent.createChooser(intent, "Share"))
}

/** ACTION_SEND with the note title + plain text of its blocks. */
fun sendNote(context: android.content.Context, title: String, blocks: List<Block>) {
    val sb = StringBuilder()
    if (title.isNotBlank()) sb.append(title).append("\n\n")
    blocks.forEach { b ->
        when (b.kind) {
            com.kurupdevs.mynotes.data.model.BlockKind.TODO ->
                sb.append(if (b.checked) "[x] " else "[ ] ")
            com.kurupdevs.mynotes.data.model.BlockKind.LI -> sb.append("• ")
            else -> {}
        }
        if (b.text.isNotBlank()) sb.append(b.text).append('\n')
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, sb.toString().trim())
    }
    context.startActivity(Intent.createChooser(intent, "Send note"))
}
