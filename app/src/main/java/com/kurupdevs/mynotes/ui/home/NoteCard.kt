package com.kurupdevs.mynotes.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.kurupdevs.mynotes.data.local.NoteEntity
import com.kurupdevs.mynotes.data.model.Attachment
import com.kurupdevs.mynotes.data.model.BlockKind
import com.kurupdevs.mynotes.data.remote.Jsons
import com.kurupdevs.mynotes.ui.theme.CardShape
import com.kurupdevs.mynotes.ui.theme.CheckFill
import com.kurupdevs.mynotes.ui.theme.CheckMark
import com.kurupdevs.mynotes.ui.theme.HeartFavFill
import com.kurupdevs.mynotes.ui.theme.cardColor
import com.kurupdevs.mynotes.ui.theme.inkOnCard
import com.kurupdevs.mynotes.ui.theme.subOnCard
import com.kurupdevs.mynotes.voice.AudioPlayer

@Composable
fun NoteCard(
    note: NoteEntity,
    dark: Boolean,
    selected: Boolean,
    onOpen: () -> Unit,
    onToggleTodo: (String) -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = cardColor(note.color, dark)
    val ink = inkOnCard(note.color)
    val sub = subOnCard(note.color)
    val blocks = Jsons.blocks(note.blocksJson)
    val atts = Jsons.attachments(note.attachmentsJson)
    val todos = blocks.filter { it.kind == BlockKind.TODO }
    val firstImage = atts.firstOrNull { it.kind == "image" }
    val firstAudio = atts.firstOrNull { it.kind == "audio" }

    Box(
        modifier = modifier
            .clip(CardShape)
            .background(bg)
            .clickable(onClick = onOpen)
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 18.dp)
    ) {
        if (selected) {
            Box(
                Modifier.matchParentSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )
        }
        Column {
            // drag handle
            Box(
                Modifier.align(Alignment.CenterHorizontally)
                    .size(26.dp, 4.dp)
                    .clip(CircleShape)
                    .background(ink.copy(alpha = 0.5f))
            )
            Spacer(Modifier.height(8.dp))
            // title row + heart
            Row(verticalAlignment = Alignment.Top) {
                val title = note.title.ifEmpty {
                    blocks.firstOrNull { it.text.isNotBlank() }?.text ?: "Untitled"
                }
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(28.dp)) {
                    Icon(
                        if (note.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (note.favorite) "Remove from Important" else "Mark as Important",
                        tint = if (note.favorite) HeartFavFill else ink.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            when {
                note.locked -> LockedBody(ink)
                todos.isNotEmpty() -> ChecklistBody(note, todos, ink, onToggleTodo)
                firstImage != null -> ImageBody(firstImage, note, sub, dark)
                firstAudio != null -> VoiceBody(firstAudio, ink, sub)
                else -> TextBody(blocks, note, ink, sub)
            }
        }
    }
}

@Composable
private fun LockedBody(ink: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp)
    ) {
        Icon(Icons.Filled.Lock, "Locked note", tint = ink.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Locked", style = MaterialTheme.typography.bodyMedium, color = ink.copy(alpha = 0.6f))
    }
}

@Composable
private fun ChecklistBody(
    note: NoteEntity,
    todos: List<com.kurupdevs.mynotes.data.model.Block>,
    ink: Color,
    onToggleTodo: (String) -> Unit
) {
    val done = todos.count { it.checked }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
        todos.take(4).forEach { t ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(40.dp)
                    .fillMaxWidth()
                    .clickable { onToggleTodo(t.id) }
            ) {
                CheckboxCircle(checked = t.checked, ink = ink)
                Spacer(Modifier.width(10.dp))
                Text(
                    t.text.ifEmpty { "Todo" },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (t.checked) ink.copy(alpha = 0.55f) else ink,
                        textDecoration = if (t.checked) TextDecoration.LineThrough else null
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        if (todos.size > 4) {
            Text(
                "+${todos.size - 4} more",
                style = MaterialTheme.typography.labelMedium,
                color = ink.copy(alpha = 0.55f)
            )
        }
        Text(
            "$done/${todos.size} done",
            style = MaterialTheme.typography.labelMedium,
            color = ink.copy(alpha = 0.55f)
        )
    }
}

@Composable
fun CheckboxCircle(checked: Boolean, ink: Color, modifier: Modifier = Modifier) {
    Box(modifier.size(20.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(20.dp)) {
            if (checked) {
                drawCircle(CheckFill, radius = 10.dp.toPx())
            } else {
                drawCircle(ink.copy(alpha = 0.65f), radius = 10.dp.toPx(), style = Stroke(2.dp.toPx()))
            }
        }
        if (checked) {
            Icon(Icons.Filled.Check, null, tint = CheckMark, modifier = Modifier.size(13.dp))
        }
    }
}

@Composable
private fun ImageBody(att: Attachment, note: NoteEntity, sub: Color, dark: Boolean) {
    Column {
        Text(
            "Updated ${relTime(note.updatedAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = sub,
            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
        )
        val model: Any = att.url.ifEmpty { att.localPath.ifEmpty { "" } }
        AsyncImage(
            model = model,
            contentDescription = "Note image",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .clip(CardShape)
        )
    }
}

@Composable
private fun VoiceBody(att: Attachment, ink: Color, sub: Color) {
    val ctx = LocalContext.current
    val player = AudioPlayer.get(ctx)
    val playingId by player.playingId.collectAsState()
    val isPlaying by player.isPlaying.collectAsState()
    val active = playingId == att.id && isPlaying
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
            .clickable { player.toggle(att.id, att.url.ifEmpty { att.localPath }) }
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(ink.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.PlayArrow, "Play voice note",
                tint = ink, modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        WaveformStrip(active, ink)
        Spacer(Modifier.width(10.dp))
        Text(fmtDur(att.durationMs), style = MaterialTheme.typography.labelMedium, color = sub)
    }
}

@Composable
fun WaveformStrip(active: Boolean, ink: Color, modifier: Modifier = Modifier) {
    val bars = listOf(6, 12, 18, 10, 22, 14, 8, 20, 12, 16, 7, 19, 11, 15, 9, 21, 13, 17, 8, 14)
    Canvas(modifier.height(28.dp).fillMaxWidth()) {
        val bw = size.width / bars.size
        bars.forEachIndexed { i, h ->
            val bh = (h / 22f) * size.height
            drawRoundRect(
                color = ink.copy(alpha = if (active) 0.9f else 0.45f),
                topLeft = androidx.compose.ui.geometry.Offset(i * bw + bw * 0.25f, (size.height - bh) / 2),
                size = androidx.compose.ui.geometry.Size(bw * 0.5f, bh),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx())
            )
        }
    }
}

@Composable
private fun TextBody(
    blocks: List<com.kurupdevs.mynotes.data.model.Block>,
    note: NoteEntity,
    ink: Color,
    sub: Color
) {
    val preview = blocks.firstOrNull { it.text.isNotBlank() && it.kind != BlockKind.IMG && it.kind != BlockKind.AUDIO }
        ?.text.orEmpty()
    if (preview.isNotBlank() && preview != note.title) {
        Text(
            preview,
            style = MaterialTheme.typography.bodyMedium,
            color = ink.copy(alpha = 0.85f),
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
    Text(
        "Updated ${relTime(note.updatedAt)}",
        style = MaterialTheme.typography.bodySmall,
        color = sub,
        modifier = Modifier.padding(top = 8.dp)
    )
}

fun relTime(ms: Long): String {
    val d = System.currentTimeMillis() - ms
    val m = d / 60000
    return when {
        m < 1 -> "just now"
        m < 60 -> "${m}m ago"
        m < 1440 -> "${m / 60}h ago"
        m < 10080 -> "${m / 1440}d ago"
        else -> "${m / 10080}w ago"
    }
}

fun fmtDur(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
