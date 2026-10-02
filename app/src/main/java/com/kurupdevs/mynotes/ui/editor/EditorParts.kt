package com.kurupdevs.mynotes.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatClear
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.kurupdevs.mynotes.data.model.Attachment
import com.kurupdevs.mynotes.data.model.Block
import com.kurupdevs.mynotes.data.model.BlockKind
import com.kurupdevs.mynotes.ui.home.CheckboxCircle
import com.kurupdevs.mynotes.ui.home.WaveformStrip
import com.kurupdevs.mynotes.ui.home.fmtDur
import com.kurupdevs.mynotes.ui.theme.SheetShape
import com.kurupdevs.mynotes.ui.theme.TitleWhite
import com.kurupdevs.mynotes.voice.AudioPlayer
import kotlinx.coroutines.delay
import java.util.Calendar

fun styledText(block: Block, baseSize: Int, ink: Color): AnnotatedString {
    return buildAnnotatedString {
        withStyle(
            SpanStyle(
                fontWeight = if ("bold" in block.marks || block.kind == BlockKind.H1 || block.kind == BlockKind.H2) FontWeight.Bold else FontWeight.Medium,
                fontStyle = if ("italic" in block.marks) FontStyle.Italic else FontStyle.Normal,
                textDecoration = buildList {
                    if ("underline" in block.marks) add(TextDecoration.Underline)
                    if ("strike" in block.marks || (block.kind == BlockKind.TODO && block.checked)) add(TextDecoration.LineThrough)
                }.let { if (it.isEmpty()) TextDecoration.None else TextDecoration.combine(it) },
                fontSize = baseSize.sp,
                color = if (block.kind == BlockKind.TODO && block.checked) ink.copy(alpha = 0.55f) else ink
            )
        ) {
            append(block.text)
        }
    }
}

@Composable
fun TextBlockRow(block: Block, ink: Color, vm: EditorViewModel) {
    val focusRequester = remember { FocusRequester() }
    val focusedId by vm.focusedBlock.collectAsState()
    var value by remember(block.id) {
        mutableStateOf(TextFieldValue(styledText(block, if (block.kind == BlockKind.H1) 22 else if (block.kind == BlockKind.H2) 19 else 16, ink), TextRange(block.text.length)))
    }
    // keep external updates (undo) in sync
    LaunchedEffect(block.text, block.marks, block.kind) {
        val styled = styledText(block, if (block.kind == BlockKind.H1) 22 else if (block.kind == BlockKind.H2) 19 else 16, ink)
        if (value.annotatedString.text != block.text) {
            value = TextFieldValue(styled, TextRange(block.text.length))
        }
    }
    LaunchedEffect(focusedId) {
        if (focusedId == block.id) {
            focusRequester.requestFocus()
            vm.onBlockFocus(null)
        }
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (block.kind == BlockKind.LI) {
            Text("• ", color = ink, fontSize = 16.sp, modifier = Modifier.padding(end = 4.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = {
                value = it
                vm.onBlockText(block.id, it.text)
            },
            textStyle = androidx.compose.ui.text.TextStyle(color = ink, fontSize = 16.sp),
            cursorBrush = SolidColor(ink),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { vm.splitBlock(block.id) }),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { if (it.isFocused) vm.onBlockFocus(block.id) }
                .padding(vertical = if (block.kind == BlockKind.H1 || block.kind == BlockKind.H2) 6.dp else 3.dp),
            decorationBox = { inner ->
                if (block.text.isEmpty()) Text(
                    when (block.kind) {
                        BlockKind.H1 -> "Heading"
                        BlockKind.H2 -> "Subheading"
                        BlockKind.LI -> "List item"
                        else -> "Write something…"
                    },
                    color = ink.copy(alpha = 0.35f), fontSize = 16.sp
                )
                inner()
            }
        )
    }
}

@Composable
fun TodoBlockRow(block: Block, ink: Color, vm: EditorViewModel, dark: Boolean) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { vm.toggleTodo(block.id) }) {
            CheckboxCircle(checked = block.checked, ink = ink)
        }
        Spacer(Modifier.width(10.dp))
        var value by remember(block.id) { mutableStateOf(TextFieldValue(block.text, TextRange(block.text.length))) }
        LaunchedEffect(block.text) {
            if (value.text != block.text) value = TextFieldValue(block.text, TextRange(block.text.length))
        }
        BasicTextField(
            value = value,
            onValueChange = { value = it; vm.onBlockText(block.id, it.text) },
            textStyle = androidx.compose.ui.text.TextStyle(
                color = if (block.checked) ink.copy(alpha = 0.55f) else ink,
                fontSize = 16.sp,
                textDecoration = if (block.checked) TextDecoration.LineThrough else null
            ),
            cursorBrush = SolidColor(ink),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { vm.splitBlock(block.id) }),
            modifier = Modifier.weight(1f).onFocusChanged { if (it.isFocused) vm.onBlockFocus(block.id) }
        )
        IconButton(onClick = { vm.deleteBlock(block.id) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Close, "Delete line", tint = ink.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun ImageBlockRow(block: Block, atts: List<Attachment>, ink: Color, vm: EditorViewModel) {
    val att = atts.firstOrNull { it.id == block.attachmentId } ?: return
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        AsyncImage(
            model = att.url.ifEmpty { att.localPath },
            contentDescription = "Attached image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp))
        )
        var caption by remember(att.id) { mutableStateOf(att.caption) }
        BasicTextField(
            value = caption,
            onValueChange = {
                caption = it
                vm.updateCaption(att.id, it)
            },
            textStyle = androidx.compose.ui.text.TextStyle(color = ink.copy(alpha = 0.7f), fontSize = 13.sp),
            cursorBrush = SolidColor(ink),
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            decorationBox = { inner ->
                if (caption.isEmpty()) Text("Add a caption…", color = ink.copy(alpha = 0.35f), fontSize = 13.sp)
                inner()
            }
        )
        if (att.ocrText.isNotBlank()) {
            Text(
                "Text found in pic: ${att.ocrText.take(80)}",
                style = MaterialTheme.typography.labelSmall,
                color = ink.copy(alpha = 0.45f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { vm.deleteBlock(block.id) }) {
                Text("Remove", color = ink.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun AudioBlockRow(block: Block, atts: List<Attachment>, ink: Color, dark: Boolean) {
    val context = LocalContext.current
    val att = atts.firstOrNull { it.id == block.attachmentId } ?: return
    val player = AudioPlayer.get(context)
    val playingId by player.playingId.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val active = playingId == att.id
    var speed by remember { mutableFloatStateOf(1f) }
    var pos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(active, isPlaying) {
        while (active && isPlaying) {
            delay(500)
            pos = player.position()
        }
    }
    val dur = if (att.durationMs > 0) att.durationMs else player.duration().takeIf { it > 0 } ?: 0L
    Column(
        Modifier.fillMaxWidth().padding(vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(ink.copy(alpha = 0.08f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { player.toggle(att.id, att.url.ifEmpty { att.localPath }) },
                modifier = Modifier.size(44.dp).clip(CircleShape).background(ink.copy(alpha = 0.15f))
            ) {
                Icon(
                    if (active && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    "Play", tint = ink, modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            WaveformStrip(active && isPlaying, ink, Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Text(fmtDur(dur), style = MaterialTheme.typography.labelMedium, color = ink.copy(alpha = 0.7f))
        }
        if (dur > 0) {
            Slider(
                value = pos.toFloat().coerceIn(0f, dur.toFloat()),
                onValueChange = { player.seekTo(it.toLong()) },
                valueRange = 0f..dur.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            listOf(1f, 1.25f, 1.5f, 2f).forEach { s ->
                Text(
                    "${s}x",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (speed == s) ink else ink.copy(alpha = 0.45f),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { speed = s; player.setSpeed(s) }.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun FormatToolbar(dark: Boolean, ink: Color, vm: EditorViewModel) {
    val focused by vm.focusedBlock.collectAsState()
    val blocks by vm.blocks.collectAsState()
    val cur = blocks.firstOrNull { it.id == focused }
    fun has(mark: String) = cur?.marks?.contains(mark) == true
    Row(
        Modifier.fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        FormatBtn("B", active = has("bold"), ink = ink, bold = true) { vm.toggleMark("bold") }
        FormatBtn("I", active = has("italic"), ink = ink, italic = true) { vm.toggleMark("italic") }
        FormatBtn("U", active = has("underline"), ink = ink, underline = true) { vm.toggleMark("underline") }
        FormatBtn("S", active = has("strike"), ink = ink, strike = true) { vm.toggleMark("strike") }
        FormatBtn("H1", active = cur?.kind == BlockKind.H1, ink = ink) {
            vm.setKind(if (cur?.kind == BlockKind.H1) BlockKind.P else BlockKind.H1)
        }
        FormatBtn("H2", active = cur?.kind == BlockKind.H2, ink = ink) {
            vm.setKind(if (cur?.kind == BlockKind.H2) BlockKind.P else BlockKind.H2)
        }
        FormatBtn("•", active = cur?.kind == BlockKind.LI, ink = ink) {
            vm.setKind(if (cur?.kind == BlockKind.LI) BlockKind.P else BlockKind.LI)
        }
        FormatBtn(null, active = cur?.kind == BlockKind.TODO, ink = ink, icon = Icons.Filled.Checklist) { vm.toggleChecklistLine() }
        FormatBtn(null, active = false, ink = ink, icon = Icons.Filled.FormatClear) { vm.clearFormatting() }
    }
}

@Composable
private fun FormatBtn(
    label: String?,
    active: Boolean,
    ink: Color,
    bold: Boolean = false,
    italic: Boolean = false,
    underline: Boolean = false,
    strike: Boolean = false,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    onClick: () -> Unit
) {
    Box(
        Modifier.clip(CircleShape)
            .background(if (active) ink.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        if (icon != null) {
            Icon(
                icon, contentDescription = label ?: "Format",
                tint = if (active) ink else ink.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        } else {
            Text(
                label ?: "",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
                    fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
                    textDecoration = when {
                        underline -> TextDecoration.Underline
                        strike -> TextDecoration.LineThrough
                        else -> TextDecoration.None
                    }
                ),
                color = if (active) ink else ink.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun AttachRow(ink: Color, onImage: () -> Unit, onVoice: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AttachChip(ink, Icons.Filled.Image, "Photo", onImage)
        AttachChip(ink, Icons.Filled.Mic, "Voice", onVoice)
    }
}

@Composable
private fun AttachChip(ink: Color, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(50))
            .background(ink.copy(alpha = 0.1f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, label, tint = ink, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = ink)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachSheet(dark: Boolean, onDismiss: () -> Unit, onCamera: () -> Unit, onGallery: () -> Unit) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss, sheetState = sheetState, shape = SheetShape,
        containerColor = if (dark) Color(0xFF111111) else Color.White
    ) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            val ink = if (dark) TitleWhite else Color(0xFF141210)
            Row(
                Modifier.fillMaxWidth().clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onCamera
                ).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.CameraAlt, "Camera", tint = ink)
                Spacer(Modifier.width(12.dp))
                Text("Take a photo", style = MaterialTheme.typography.bodyLarge, color = ink)
            }
            Row(
                Modifier.fillMaxWidth().clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onGallery
                ).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Image, "Gallery", tint = ink)
                Spacer(Modifier.width(12.dp))
                Text("Pick from gallery", style = MaterialTheme.typography.bodyLarge, color = ink)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDialog(
    dark: Boolean,
    current: Long?,
    repeat: String?,
    onDismiss: () -> Unit,
    onSet: (Long, String?) -> Unit,
    onClear: () -> Unit
) {
    var step by remember { mutableStateOf(0) } // 0 date, 1 time
    var rep by remember { mutableStateOf(repeat) }
    val dateState = rememberDatePickerState(initialSelectedDateMillis = current ?: System.currentTimeMillis())
    val timeState = rememberTimePickerState(
        initialHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        initialMinute = 0, is24Hour = false
    )
    val ink = if (dark) TitleWhite else Color(0xFF141210)
    if (step == 0) {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = { step = 1 }) { Text("Next") }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        ) {
            DatePicker(dateState)
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Pick a time", color = ink) },
            text = {
                Column {
                    TimePicker(timeState)
                    Spacer(Modifier.height(8.dp))
                    Text("Repeat", style = MaterialTheme.typography.labelLarge, color = ink)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(null to "Once", "daily" to "Daily", "weekly" to "Weekly").forEach { (v, l) ->
                            Text(
                                l,
                                color = if (rep == v) Color.White else ink,
                                modifier = Modifier.clip(CircleShape)
                                    .background(if (rep == v) Color(0xFFEA7B53) else ink.copy(alpha = 0.1f))
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { rep = v }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = dateState.selectedDateMillis ?: System.currentTimeMillis()
                        set(Calendar.HOUR_OF_DAY, timeState.hour)
                        set(Calendar.MINUTE, timeState.minute)
                        set(Calendar.SECOND, 0)
                    }
                    onSet(cal.timeInMillis, rep)
                }) { Text("Set reminder") }
            },
            dismissButton = {
                TextButton(onClick = onClear) { Text("Clear") }
            },
            containerColor = if (dark) Color(0xFF111111) else Color.White
        )
    }
}

@Composable
fun ConfirmDialog(
    dark: Boolean,
    title: String,
    body: String,
    confirm: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val ink = if (dark) TitleWhite else Color(0xFF141210)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = ink) },
        text = { Text(body, color = ink.copy(alpha = 0.7f)) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirm, color = Color(0xFFEA7B53)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = ink.copy(alpha = 0.7f)) }
        },
        containerColor = if (dark) Color(0xFF1A1A1A) else Color.White
    )
}

@Composable
fun ConflictSheet(
    dark: Boolean,
    local: List<Block>,
    remote: List<Block>,
    onDismiss: () -> Unit,
    onKeepLocal: () -> Unit,
    onKeepRemote: () -> Unit
) {
    val ink = if (dark) TitleWhite else Color(0xFF141210)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edited elsewhere", color = ink) },
        text = {
            Column {
                Text("This note changed on another device. Pick which version to keep — nothing gets merged silently.", color = ink.copy(alpha = 0.7f))
                Spacer(Modifier.height(8.dp))
                Text("Yours: ${local.joinToString(" ") { it.text }.take(120)}", style = MaterialTheme.typography.bodySmall, color = ink)
                Spacer(Modifier.height(4.dp))
                Text("Theirs: ${remote.joinToString(" ") { it.text }.take(120)}", style = MaterialTheme.typography.bodySmall, color = ink.copy(alpha = 0.7f))
            }
        },
        confirmButton = {
            TextButton(onClick = onKeepLocal) { Text("Keep mine") }
        },
        dismissButton = {
            TextButton(onClick = onKeepRemote) { Text("Keep theirs") }
        },
        containerColor = if (dark) Color(0xFF1A1A1A) else Color.White
    )
}
