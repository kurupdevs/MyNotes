package com.kurupdevs.mynotes.ui.draw

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kurupdevs.mynotes.data.model.Block
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Compact wire format for one freehand stroke, stored as JSON in Block.text
 * for blocks with kind == BlockKind.DRAW.
 *
 * c = color as ARGB bits (Long), w = stroke width as a fraction of the
 * canvas width (device-independent so thumbnails and re-edits match at any
 * size), p = normalized 0..1 [x, y] points, e = true for eraser strokes.
 */
@Serializable
internal data class StrokeDto(
    val c: Long,
    val w: Float,
    val p: List<List<Float>>,
    val e: Boolean = false
)

/** In-memory stroke: points are normalized 0..1, width is a canvas-width fraction. */
data class DrawStroke(
    val points: List<Offset>,
    val color: Color,
    val width: Float,
    val isEraser: Boolean = false
)

private val strokeJson = Json { ignoreUnknownKeys = true }

fun encodeStrokes(strokes: List<DrawStroke>): String =
    strokeJson.encodeToString(
        strokes.filter { it.points.isNotEmpty() }.map {
            StrokeDto(
                c = it.color.toArgb().toLong() and 0xFFFFFFFFL,
                w = it.width,
                p = it.points.map { pt -> listOf(pt.x, pt.y) },
                e = it.isEraser
            )
        }
    )

fun decodeStrokes(json: String): List<DrawStroke> {
    if (json.isBlank()) return emptyList()
    return try {
        strokeJson.decodeFromString<List<StrokeDto>>(json).mapNotNull { dto ->
            val pts = dto.p.mapNotNull { pair ->
                if (pair.size >= 2) Offset(pair[0].coerceIn(0f, 1f), pair[1].coerceIn(0f, 1f))
                else null
            }
            if (pts.isEmpty()) null
            else DrawStroke(
                points = pts,
                color = Color(dto.c.toInt()),
                width = dto.w.coerceIn(0f, 1f),
                isEraser = dto.e
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

/** Renders strokes scaled to the current draw scope size. Eraser strokes clear. */
fun DrawScope.drawDrawStrokes(strokes: List<DrawStroke>) {
    strokes.forEach { s ->
        val erase = s.isEraser
        val wPx = (s.width * size.width).coerceAtLeast(1f)
        if (s.points.size == 1) {
            val c = Offset(s.points[0].x * size.width, s.points[0].y * size.height)
            drawCircle(
                color = if (erase) Color.Transparent else s.color,
                radius = wPx / 2f,
                center = c,
                blendMode = if (erase) BlendMode.Clear else BlendMode.SrcOver
            )
        } else if (s.points.size >= 2) {
            val path = Path().apply {
                moveTo(s.points[0].x * size.width, s.points[0].y * size.height)
                for (i in 1 until s.points.size) {
                    lineTo(s.points[i].x * size.width, s.points[i].y * size.height)
                }
            }
            drawPath(
                path = path,
                color = if (erase) Color.Transparent else s.color,
                style = Stroke(width = wPx, cap = StrokeCap.Round, join = StrokeJoin.Round),
                blendMode = if (erase) BlendMode.Clear else BlendMode.SrcOver
            )
        }
    }
}

/**
 * Thumbnail of a DRAW block for the note editor: 4:3, white card with a
 * light border and rounded corners. Tap opens the drawing screen.
 */
@Composable
fun DrawBlockView(block: Block, modifier: Modifier = Modifier, onEdit: () -> Unit) {
    val strokes = remember(block.text) { decodeStrokes(block.text) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(1.dp, Color(0xFFE2E2E2), RoundedCornerShape(12.dp))
            .clickable(onClick = onEdit)
    ) {
        if (strokes.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Draw",
                    tint = Color(0xFF9E9E9E),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text("Tap to draw", color = Color(0xFF9E9E9E), fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawDrawStrokes(strokes)
            }
        }
    }
}
