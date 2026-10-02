package com.kurupdevs.mynotes.ui.draw

import android.app.Application
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kurupdevs.mynotes.NotesApp
import com.kurupdevs.mynotes.data.model.Block
import com.kurupdevs.mynotes.data.model.BlockKind
import com.kurupdevs.mynotes.data.remote.Jsons
import kotlinx.coroutines.launch
import java.util.UUID

private enum class DrawTool { PEN, PENCIL, ERASER }

private val drawColors = listOf(
    Color(0xFF111111),
    Color(0xFFE53935),
    Color(0xFF1E88E5),
    Color(0xFF43A047),
    Color(0xFF8E24AA),
    Color(0xFFFB8C00)
)

private fun widthsFor(tool: DrawTool): List<Float> = when (tool) {
    DrawTool.PEN -> listOf(3f, 6f, 12f)
    DrawTool.PENCIL -> listOf(2f, 4f, 8f)
    DrawTool.ERASER -> listOf(14f, 26f, 44f)
}

@Composable
private fun EraserIcon(modifier: Modifier = Modifier, tint: Color) {
    Canvas(modifier = modifier.size(22.dp)) {
        rotate(45f, pivot = center) {
            drawRoundRect(
                color = tint,
                topLeft = Offset(size.width * 0.18f, size.height * 0.32f),
                size = Size(size.width * 0.64f, size.height * 0.36f),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
        }
    }
}

@Composable
fun DrawScreen(noteId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val repo = (app as NotesApp).container.notes
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var strokes by remember { mutableStateOf(listOf<DrawStroke>()) }
    var undone by remember { mutableStateOf(listOf<DrawStroke>()) }
    var inProgress by remember { mutableStateOf<DrawStroke?>(null) }
    var tool by remember { mutableStateOf(DrawTool.PEN) }
    var color by remember { mutableStateOf(drawColors[0]) }
    var widthIdx by remember { mutableStateOf(1) }
    var canvasPx by remember { mutableStateOf(Size.Zero) }
    var saving by remember { mutableStateOf(false) }

    // Load existing DRAW block strokes for this note.
    LaunchedEffect(noteId) {
        val note = repo.getNote(noteId)
        if (note != null) {
            val drawBlock = Jsons.blocks(note.blocksJson).firstOrNull { it.kind == BlockKind.DRAW }
            if (drawBlock != null) strokes = decodeStrokes(drawBlock.text)
        }
    }

    BackHandler { onBack() }

    fun currentStrokeWidthPx(): Float {
        val dp = widthsFor(tool)[widthIdx.coerceIn(0, 2)]
        return with(density) { dp.dp.toPx() }
    }

    fun startStroke(at: Offset) {
        if (canvasPx.width <= 0f) return
        val c = when (tool) {
            DrawTool.PEN -> color
            DrawTool.PENCIL -> color.copy(alpha = 0.45f)
            DrawTool.ERASER -> Color.Transparent
        }
        val nx = (at.x / canvasPx.width).coerceIn(0f, 1f)
        val ny = (at.y / canvasPx.height).coerceIn(0f, 1f)
        inProgress = DrawStroke(
            points = listOf(Offset(nx, ny)),
            color = c,
            width = (currentStrokeWidthPx() / canvasPx.width).coerceIn(0f, 1f),
            isEraser = tool == DrawTool.ERASER
        )
    }

    fun addPoint(at: Offset) {
        val cur = inProgress ?: return
        val nx = (at.x / canvasPx.width).coerceIn(0f, 1f)
        val ny = (at.y / canvasPx.height).coerceIn(0f, 1f)
        inProgress = cur.copy(points = cur.points + Offset(nx, ny))
    }

    fun finishStroke() {
        inProgress?.let {
            strokes = strokes + it
            undone = emptyList()
        }
        inProgress = null
    }

    fun undo() {
        if (strokes.isNotEmpty()) {
            undone = undone + strokes.last()
            strokes = strokes.dropLast(1)
        }
    }

    fun redo() {
        if (undone.isNotEmpty()) {
            strokes = strokes + undone.last()
            undone = undone.dropLast(1)
        }
    }

    fun saveAndBack() {
        if (saving) return
        saving = true
        scope.launch {
            val note = repo.getNote(noteId)
            if (note != null) {
                val blocks = Jsons.blocks(note.blocksJson).toMutableList()
                val json = encodeStrokes(strokes)
                val idx = blocks.indexOfFirst { it.kind == BlockKind.DRAW }
                if (idx >= 0) {
                    blocks[idx] = blocks[idx].copy(text = json)
                } else {
                    val maxOrder = blocks.maxOfOrNull { it.order } ?: -1
                    blocks.add(
                        Block(
                            id = UUID.randomUUID().toString(),
                            kind = BlockKind.DRAW,
                            text = json,
                            order = maxOrder + 1
                        )
                    )
                }
                repo.updateBlocks(noteId, blocks)
            }
            saving = false
            onBack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // ---- top bar: back, undo, redo, done ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF111111))
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { undo() }, enabled = strokes.isNotEmpty()) {
                Icon(
                    Icons.Filled.Undo, contentDescription = "Undo",
                    tint = if (strokes.isNotEmpty()) Color(0xFF111111) else Color(0xFFCCCCCC)
                )
            }
            IconButton(onClick = { redo() }, enabled = undone.isNotEmpty()) {
                Icon(
                    Icons.Filled.Redo, contentDescription = "Redo",
                    tint = if (undone.isNotEmpty()) Color(0xFF111111) else Color(0xFFCCCCCC)
                )
            }
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1F1F1F))
                    .clickable { saveAndBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Check, contentDescription = "Done", tint = Color.White)
            }
        }

        // ---- canvas with dotted grid ----
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .onSizeChanged { canvasPx = Size(it.width.toFloat(), it.height.toFloat()) }
        ) {
            // dotted grid (separate layer so the eraser never eats the dots)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val step = 26.dp.toPx()
                val dotR = 1.1.dp.toPx()
                val dotColor = Color(0xFFE9E9E9)
                var y = step / 2f
                while (y < size.height) {
                    var x = step / 2f
                    while (x < size.width) {
                        drawCircle(dotColor, dotR, Offset(x, y))
                        x += step
                    }
                    y += step
                }
            }
            // strokes layer
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(tool, color, widthIdx) {
                        detectDragGestures(
                            onDragStart = { startStroke(it) },
                            onDrag = { change, _ ->
                                change.consume()
                                addPoint(change.position)
                            },
                            onDragEnd = { finishStroke() },
                            onDragCancel = { inProgress = null }
                        )
                    }
            ) {
                drawDrawStrokes(strokes)
                inProgress?.let { drawDrawStrokes(listOf(it)) }
            }
        }

        // ---- bottom toolbar ----
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(0xFFF7F7F7))
                .border(1.dp, Color(0xFFE8E8E8), RoundedCornerShape(20.dp))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // color dots
            drawColors.forEach { c ->
                val selected = tool != DrawTool.ERASER && color == c
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(c)
                        .border(
                            if (selected) 2.dp else 1.dp,
                            if (selected) Color(0xFF111111) else Color(0xFFDDDDDD),
                            CircleShape
                        )
                        .clickable {
                            color = c
                            if (tool == DrawTool.ERASER) tool = DrawTool.PEN
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) Icon(
                        Icons.Filled.Check, null, tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // tools: pen / pencil / eraser
            val toolBtn: @Composable (DrawTool, @Composable () -> Unit, String) -> Unit =
                { t, icon, desc ->
                    val selected = tool == t
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (selected) Color(0xFF1F1F1F) else Color.Transparent)
                            .clickable { tool = t },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) { icon() }
                    }
                }
            val toolTint: (DrawTool) -> Color = { t ->
                if (tool == t) Color.White else Color(0xFF555555)
            }
            toolBtn(DrawTool.PEN, { Icon(Icons.Filled.Brush, null, tint = toolTint(DrawTool.PEN)) }, "Pen")
            toolBtn(DrawTool.PENCIL, { Icon(Icons.Filled.Edit, null, tint = toolTint(DrawTool.PENCIL)) }, "Pencil")
            toolBtn(DrawTool.ERASER, { EraserIcon(tint = toolTint(DrawTool.ERASER)) }, "Eraser")

            Spacer(Modifier.width(8.dp))

            // stroke widths: 3 choices
            widthsFor(tool).forEachIndexed { i, dpVal ->
                val selected = widthIdx == i
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (selected) Color(0xFFE0E0E0) else Color.Transparent)
                        .clickable { widthIdx = i },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(dpVal.dp.coerceAtMost(22.dp))
                            .clip(CircleShape)
                            .background(Color(0xFF333333))
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // undo / redo
            IconButton(onClick = { undo() }, enabled = strokes.isNotEmpty(), modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.Undo, contentDescription = "Undo",
                    tint = if (strokes.isNotEmpty()) Color(0xFF333333) else Color(0xFFCCCCCC)
                )
            }
            IconButton(onClick = { redo() }, enabled = undone.isNotEmpty(), modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.Redo, contentDescription = "Redo",
                    tint = if (undone.isNotEmpty()) Color(0xFF333333) else Color(0xFFCCCCCC)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}
