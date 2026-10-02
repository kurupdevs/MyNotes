package com.kurupdevs.mynotes.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.kurupdevs.mynotes.ui.theme.SheetShape
import com.kurupdevs.mynotes.ui.theme.TitleWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Note options bottom sheet (reference: options sheet with Image/Voice/Share
 * quick actions, menu rows, and a "Last Edited … By …" footer).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteOptionsSheet(
    dark: Boolean,
    pinned: Boolean,
    locked: Boolean,
    archived: Boolean,
    favorite: Boolean,
    firstLabelName: String?,
    updatedAt: Long,
    onDismiss: () -> Unit,
    onImage: () -> Unit,
    onVoice: () -> Unit,
    onShare: () -> Unit,
    onDraw: () -> Unit,
    onPin: () -> Unit,
    onAddThumbnail: () -> Unit,
    onLabel: () -> Unit,
    onSend: () -> Unit,
    onDuplicate: () -> Unit,
    onReminder: () -> Unit,
    onArchive: () -> Unit,
    onColor: () -> Unit,
    onExport: () -> Unit,
    onFavorite: () -> Unit,
    onLock: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val ink = if (dark) TitleWhite else Color(0xFF141210)
    val sub = ink.copy(alpha = 0.55f)
    val danger = Color(0xFFE5484D)
    val footerFmt = remember { SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()) }
    fun act(fn: () -> Unit): () -> Unit = { onDismiss(); fn() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = SheetShape,
        containerColor = if (dark) Color(0xFF1A1A1A) else Color.White
    ) {
        Column(
            Modifier.fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 28.dp)
        ) {
            // top quick actions
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SheetTopBtn(Icons.Filled.Image, "Image", ink, act(onImage))
                SheetTopBtn(Icons.Filled.GraphicEq, "Voice", ink, act(onVoice))
                SheetTopBtn(Icons.Filled.Share, "Share", ink, act(onShare))
            }
            SheetRow(Icons.Filled.PushPin, if (pinned) "Unpin" else "Pin", ink, act(onPin))
            SheetRow(
                if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                if (favorite) "Unmark Important" else "Important", ink, act(onFavorite)
            )
            SheetRow(Icons.Filled.Brush, "Draw", ink, act(onDraw))
            SheetRow(Icons.Filled.Image, "Add Thumbnail", ink, act(onAddThumbnail))
            SheetRow(Icons.Filled.Label, "Label", ink, act(onLabel)) {
                Text(firstLabelName ?: "None", style = MaterialTheme.typography.bodyMedium, color = sub)
                Spacer(Modifier.width(2.dp))
                Icon(Icons.Filled.ChevronRight, "Open", tint = sub, modifier = Modifier.size(20.dp))
            }
            SheetRow(Icons.Filled.Send, "Send", ink, act(onSend)) {
                Icon(Icons.Filled.ChevronRight, "Open", tint = sub, modifier = Modifier.size(20.dp))
            }
            SheetRow(Icons.Filled.ContentCopy, "Make a Copy", ink, act(onDuplicate))
            SheetRow(Icons.Filled.Notifications, "Reminder", ink, act(onReminder))
            SheetRow(Icons.Filled.Archive, if (archived) "Unarchive" else "Archive", ink, act(onArchive))
            SheetRow(Icons.Filled.Palette, "Note color", ink, act(onColor))
            SheetRow(Icons.Filled.Upload, "Export as TXT", ink, act(onExport))
            SheetRow(
                if (locked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                if (locked) "Unlock Note" else "Lock Note", ink, act(onLock)
            )
            SheetRow(Icons.Filled.Delete, "Delete Note", danger, act(onDelete), iconTint = danger)
            Spacer(Modifier.height(18.dp))
            Text(
                "Last Edited ${footerFmt.format(Date(updatedAt))} By you",
                style = MaterialTheme.typography.labelMedium,
                color = sub,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun SheetTopBtn(icon: ImageVector, label: String, ink: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(64.dp)
                .clip(CircleShape)
                .border(1.dp, ink.copy(alpha = 0.25f), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, tint = ink, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = ink)
    }
}

@Composable
private fun SheetRow(
    icon: ImageVector,
    label: String,
    labelColor: Color,
    onClick: () -> Unit,
    iconTint: Color = labelColor.copy(alpha = 0.75f),
    trailing: @Composable (() -> Unit)? = null
) {
    Row(
        Modifier.fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onClick
            )
            .padding(horizontal = 22.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, label, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(16.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = labelColor,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}
