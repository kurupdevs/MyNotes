package com.kurupdevs.mynotes.ui.home

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kurupdevs.mynotes.data.model.SyncStatus
import com.kurupdevs.mynotes.ui.common.DottedBackground
import com.kurupdevs.mynotes.ui.common.EmptyState
import com.kurupdevs.mynotes.ui.theme.ChipBadgeBg
import com.kurupdevs.mynotes.ui.theme.ChipBadgeText
import com.kurupdevs.mynotes.ui.theme.ChipIdleBorder
import com.kurupdevs.mynotes.ui.theme.ChipIdleText
import com.kurupdevs.mynotes.ui.theme.ChipShape
import com.kurupdevs.mynotes.ui.theme.MenuBg
import com.kurupdevs.mynotes.ui.theme.MenuDots
import com.kurupdevs.mynotes.ui.theme.TitleWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalSharedTransitionApi::class)
@Composable
fun HomeScreen(
    dark: Boolean,
    sharedScope: SharedTransitionScope,
    animScope: AnimatedVisibilityScope,
    onOpenNote: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLabels: () -> Unit,
    onOpenReminders: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenArchive: () -> Unit,
    onNewNote: (String) -> Unit,
    onQuickVoice: () -> Unit,
    onLinkGoogle: () -> Unit,
    vm: HomeViewModel = viewModel()
) {
    val notes by vm.notes.collectAsState()
    val labels by vm.labels.collectAsState()
    val filter by vm.filter.collectAsState()
    val selection by vm.selection.collectAsState()
    val inSelection by vm.inSelection.collectAsState()
    val syncing by vm.syncing.collectAsState()
    val syncError by vm.syncError.collectAsState()
    val isAnonymous by vm.isAnonymous.collectAsState()
    val backupNudge by vm.backupNudge.collectAsState()
    val toastMsg by vm.toast.collectAsState()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val gridState = rememberLazyStaggeredGridState()
    val ptrState = rememberPullToRefreshState()
    var menuOpen by remember { mutableStateOf(false) }
    var newSheetOpen by remember { mutableStateOf(false) }
    var colorSheetFor by remember { mutableStateOf<Set<String>?>(null) }
    var labelSheetFor by remember { mutableStateOf<Set<String>?>(null) }
    val dockVisible by remember {
        androidx.compose.runtime.derivedStateOf {
            val first = gridState.firstVisibleItemIndex
            val offset = gridState.firstVisibleItemScrollOffset
            first == 0 && offset < 120
        }
    }

    LaunchedEffect(toastMsg) {
        toastMsg?.let { t ->
            vm.toastShown()
            val res = snackbar.showSnackbar(t.text, actionLabel = if (t.undo != null) "Undo" else null, duration = SnackbarDuration.Short)
            if (res == SnackbarResult.ActionPerformed) t.undo?.invoke()
        }
    }
    LaunchedEffect(syncError) {
        syncError?.let { snackbar.showSnackbar(it, duration = SnackbarDuration.Short) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = Color.Transparent,
        content = { pad ->
            Box(Modifier.fillMaxSize().padding(pad)) {
                DottedBackground(dark)
                PullToRefreshBox(
                    state = ptrState,
                    isRefreshing = syncing,
                    onRefresh = { vm.syncNow() },
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            state = ptrState,
                            isRefreshing = syncing,
                            containerColor = Color.Transparent,
                            color = if (dark) TitleWhite else Color(0xFF141210),
                            modifier = Modifier.align(Alignment.TopCenter)
                        )
                    }
                ) {
                    Column(Modifier.fillMaxSize()) {
                        // ---- top bar ----
                        Row(
                            Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 34.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (inSelection) {
                                IconButton(onClick = { vm.clearSelection() }) {
                                    Icon(Icons.Filled.Close, "Cancel selection", tint = if (dark) TitleWhite else Color(0xFF141210))
                                }
                                Text(
                                    "${selection.size} selected",
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = if (dark) TitleWhite else Color(0xFF141210)
                                )
                            } else {
                                Text(
                                    "My Notes",
                                    style = MaterialTheme.typography.displayLarge,
                                    color = if (dark) TitleWhite else Color(0xFF141210),
                                    modifier = Modifier.weight(1f)
                                )
                                SyncPill(syncing = syncing, anonymous = isAnonymous, dark = dark, onClick = { vm.syncNow() })
                                Spacer(Modifier.width(8.dp))
                                IconButton(onClick = onOpenSearch) {
                                    Icon(Icons.Filled.Search, "Search notes", tint = if (dark) TitleWhite else Color(0xFF141210), modifier = Modifier.size(26.dp))
                                }
                                // avatar dot -> settings
                                Box(
                                    Modifier.size(40.dp).clip(CircleShape)
                                        .background(if (dark) MenuBg else Color(0xFFE7E1D3))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = onOpenSettings
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "A",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (dark) MenuDots else Color(0xFF6B6257)
                                    )
                                }
                                Box {
                                    IconButton(onClick = { menuOpen = true }) {
                                        Icon(Icons.Filled.MoreVert, "Menu", tint = if (dark) MenuDots else Color(0xFF6B6257))
                                    }
                                    DropdownMenu(
                                        expanded = menuOpen,
                                        onDismissRequest = { menuOpen = false },
                                        modifier = Modifier.background(if (dark) MenuBg else Color.White)
                                    ) {
                                        listOf(
                                            "Reminders" to onOpenReminders,
                                            "Labels" to onOpenLabels,
                                            "Archive" to onOpenArchive,
                                            "Trash" to onOpenTrash,
                                            "Settings" to onOpenSettings
                                        ).forEach { (label, fn) ->
                                            DropdownMenuItem(
                                                text = { Text(label, color = if (dark) MenuDots else Color(0xFF141210)) },
                                                onClick = { menuOpen = false; fn() }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (inSelection) {
                            SelectionActions(
                                dark = dark,
                                onPin = { vm.selectionPin() },
                                onLabel = { labelSheetFor = selection },
                                onColor = { colorSheetFor = selection },
                                onArchive = { vm.selectionArchive(); vm.toast("Archived") },
                                onDelete = { vm.selectionDelete(); vm.toast("Moved to trash") },
                                onSelectAll = { vm.selectAll() }
                            )
                        } else {
                            // ---- chips ----
                            ChipsRow(
                                dark = dark,
                                filter = filter,
                                labels = labels,
                                totalCount = vm.noteCountForChip(),
                                onFilter = { vm.setFilter(it) },
                                onLabelLongPress = onOpenLabels
                            )
                        }

                        // ---- backup nudge ----
                        if (backupNudge && !inSelection) {
                            BackupNudge(dark = dark, onLink = onLinkGoogle, onDismiss = { vm.dismissBackupNudge() })
                        }

                        // ---- grid ----
                        if (notes.isEmpty()) {
                            EmptyState(
                                title = "Nothing here yet",
                                subtitle = "Tap + and start dumping ideas.",
                                actionLabel = "Make my first note",
                                onAction = { newSheetOpen = true },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            LazyVerticalStaggeredGrid(
                                columns = StaggeredGridCells.Fixed(2),
                                state = gridState,
                                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 170.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalItemSpacing = 12.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                itemsIndexed(notes, key = { _, n -> n.id }) { index, note ->
                                    EntranceCard(index = index) {
                                        val pressScale = remember { androidx.compose.animation.core.Animatable(1f) }
                                        val cscope = rememberCoroutineScope()
                                        with(sharedScope) {
                                            SwipeableNoteCard(
                                                pinned = note.pinned,
                                                onPin = { vm.swipePin(note.id) },
                                                onArchive = { vm.swipeArchive(note.id, note.pinned) },
                                                modifier = Modifier
                                                    .sharedElement(
                                                        sharedScope.rememberSharedContentState("note-${note.id}"),
                                                        animScope
                                                    )
                                                    .scale(pressScale.value)
                                                    .combinedClickable(
                                                        interactionSource = remember { MutableInteractionSource() },
                                                        onClick = {
                                                            if (inSelection) vm.toggleSelect(note.id)
                                                            else {
                                                                cscope.launch {
                                                                    pressScale.animateTo(0.96f, tween(80))
                                                                    pressScale.animateTo(1f, tween(120))
                                                                }
                                                                onOpenNote(note.id)
                                                            }
                                                        },
                                                        onLongClick = {
                                                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                                            vm.toggleSelect(note.id)
                                                        }
                                                    ),
                                                card = {
                                                    Box {
                                                        NoteCard(
                                                            note = note,
                                                            dark = dark,
                                                            selected = note.id in selection,
                                                            onOpen = { onOpenNote(note.id) },
                                                            onToggleTodo = { vm.quickToggleTodo(note.id, it) },
                                                            onToggleFavorite = { vm.toggleFavorite(note.id) },
                                                            modifier = Modifier.fillMaxWidth()
                                                        )
                                                        if (note.pinned) {
                                                            Icon(
                                                                Icons.Filled.PushPin, "Pinned",
                                                                tint = Color.White.copy(alpha = 0.85f),
                                                                modifier = Modifier.align(Alignment.TopStart).padding(10.dp).size(16.dp)
                                                            )
                                                        }
                                                        if (note.syncStatus == SyncStatus.CONFLICT.name) {
                                                            Box(
                                                                Modifier.align(Alignment.BottomCenter)
                                                                    .fillMaxWidth()
                                                                    .background(Color.Black.copy(alpha = 0.55f))
                                                                    .clickable { onOpenNote(note.id) }
                                                                    .padding(6.dp),
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    "Edited elsewhere — tap to merge",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = Color.White
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ---- dock ----
                androidx.compose.animation.AnimatedVisibility(
                    visible = dockVisible && !inSelection,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInVertically { it / 2 },
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.slideOutVertically { it / 2 }
                ) {
                    HomeDock(dark = dark, onAdd = { newSheetOpen = true }, onMic = onQuickVoice)
                }
            }
        }
    )

    if (newSheetOpen) {
        NewNoteSheet(
            dark = dark,
            onDismiss = { newSheetOpen = false },
            onPick = { type -> newSheetOpen = false; onNewNote(type) }
        )
    }
    colorSheetFor?.let { ids ->
        ColorPickSheet(dark = dark, onDismiss = { colorSheetFor = null }, onPick = {
            vm.selectionColor(it); colorSheetFor = null
        })
    }
    labelSheetFor?.let { ids ->
        LabelPickSheet(dark = dark, labels = labels, onDismiss = { labelSheetFor = null }, onPick = {
            vm.selectionLabels(it); labelSheetFor = null
        })
    }
}

@Composable
private fun EntranceCard(index: Int, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay((index * 50).coerceAtMost(600).toLong())
        visible = true
    }
    val alpha by animateFloatAsState(
        if (visible) 1f else 0f,
        animationSpec = tween(350, easing = FastOutSlowInEasing), label = "fade"
    )
    val dy by animateFloatAsState(
        if (visible) 0f else 40f,
        animationSpec = tween(350, easing = FastOutSlowInEasing), label = "slide"
    )
    Box(Modifier.graphicsLayer { this.alpha = alpha; translationY = dy }) {
        content()
    }
}

@Composable
private fun SyncPill(syncing: Boolean, anonymous: Boolean, dark: Boolean, onClick: () -> Unit) {
    val tint = if (dark) TitleWhite.copy(alpha = 0.7f) else Color(0xFF6B6257)
    Box(
        Modifier.clip(CircleShape).clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null, onClick = onClick
        ).padding(8.dp)
    ) {
        if (syncing) {
            SpinningSync(tint)
        } else {
            Icon(
                Icons.Filled.Sync, if (anonymous) "Local only" else "Synced",
                tint = tint.copy(alpha = if (anonymous) 0.4f else 1f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SpinningSync(tint: Color) {
    var angle by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(16)
            angle = (angle + 8) % 360
        }
    }
    Icon(Icons.Filled.Sync, "Syncing", tint = tint, modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = angle })
}

@Composable
private fun BackupNudge(dark: Boolean, onLink: () -> Unit, onDismiss: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .background(if (dark) Color(0xFF1A1A1A) else Color(0xFFE7E1D3))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Back up with Google? Your notes sync everywhere.",
            style = MaterialTheme.typography.bodyMedium,
            color = if (dark) TitleWhite else Color(0xFF141210),
            modifier = Modifier.weight(1f)
        )
        Text(
            "Back up",
            style = MaterialTheme.typography.labelLarge,
            color = if (dark) TitleWhite else Color(0xFF141210),
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onLink
            ).padding(8.dp)
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.Close, "Dismiss", tint = if (dark) TitleWhite.copy(alpha = 0.6f) else Color(0xFF6B6257), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun SelectionActions(
    dark: Boolean,
    onPin: () -> Unit,
    onLabel: () -> Unit,
    onColor: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onSelectAll: () -> Unit
) {
    val ink = if (dark) TitleWhite else Color(0xFF141210)
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("Pin" to onPin, "Label" to onLabel, "Color" to onColor, "Archive" to onArchive, "Delete" to onDelete).forEach { (l, fn) ->
            Text(
                l, style = MaterialTheme.typography.labelLarge, color = ink,
                modifier = Modifier.clip(CircleShape).clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null, onClick = fn
                ).padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            "Select all", style = MaterialTheme.typography.labelLarge,
            color = ink.copy(alpha = 0.6f),
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null, onClick = onSelectAll
            ).padding(8.dp)
        )
    }
}
