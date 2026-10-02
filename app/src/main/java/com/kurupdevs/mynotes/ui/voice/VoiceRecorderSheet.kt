package com.kurupdevs.mynotes.ui.voice

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.kurupdevs.mynotes.ui.home.fmtDur
import com.kurupdevs.mynotes.ui.theme.SheetShape
import com.kurupdevs.mynotes.ui.theme.TitleWhite
import com.kurupdevs.mynotes.voice.VoiceRecordService
import kotlinx.coroutines.delay
import java.io.File
import kotlin.random.Random

/** Bottom-sheet recorder: waveform, timer, pause/resume, discard, done. Max 10 min. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceRecorderSheet(
    dark: Boolean,
    onDismiss: () -> Unit,
    onDone: (File, Long) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val recState by VoiceRecordService.state.collectAsState()
    var elapsed by mutableLongStateOf(0L)
    var warned by remember { mutableStateOf(false) }
    var denied by remember { mutableStateOf(false) }

    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) VoiceRecordService.start(context) else denied = true
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            VoiceRecordService.start(context)
        } else {
            permLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // live ticker
    LaunchedEffect(recState.recording, recState.paused) {
        var base = System.currentTimeMillis() - elapsed
        while (recState.recording && !recState.paused) {
            delay(250)
            elapsed = System.currentTimeMillis() - base
            if (elapsed >= 9 * 60 * 1000 && !warned) warned = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // if dismissed mid-record without done, stop service
            if (VoiceRecordService.state.value.recording) {
                VoiceRecordService.stop(context)
            }
        }
    }

    val ink = if (dark) TitleWhite else Color(0xFF141210)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = SheetShape,
        containerColor = if (dark) Color(0xFF111111) else Color.White
    ) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (denied) {
                Text("Mic access was denied — voice notes need it.", style = MaterialTheme.typography.bodyMedium, color = ink)
                Spacer(Modifier.height(16.dp))
                return@Column
            }
            Text(
                if (recState.paused) "Paused" else "Recording",
                style = MaterialTheme.typography.headlineMedium, color = ink
            )
            Spacer(Modifier.height(4.dp))
            Text(fmtDur(elapsed) + " / 10:00", style = MaterialTheme.typography.bodyLarge, color = ink.copy(alpha = 0.6f))
            if (warned) {
                Text("1 min left", style = MaterialTheme.typography.labelLarge, color = Color(0xFFEA7B53))
            }
            Spacer(Modifier.height(16.dp))
            LiveWaveform(active = recState.recording && !recState.paused, ink = ink)
            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                // discard
                IconButton(
                    onClick = {
                        val f = recState.file
                        VoiceRecordService.stop(context)
                        f?.delete()
                        onDismiss()
                    },
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(ink.copy(alpha = 0.08f))
                ) {
                    Icon(Icons.Filled.Close, "Discard", tint = ink)
                }
                // pause/resume
                IconButton(
                    onClick = {
                        if (recState.paused) VoiceRecordService.resume(context)
                        else VoiceRecordService.pause(context)
                    },
                    modifier = Modifier.size(72.dp).clip(CircleShape).background(Color(0xFFEA7B53))
                ) {
                    Icon(
                        if (recState.paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        if (recState.paused) "Resume" else "Pause",
                        tint = Color.White, modifier = Modifier.size(32.dp)
                    )
                }
                // done
                IconButton(
                    onClick = {
                        val f = recState.file
                        val dur = if (elapsed > 0) elapsed else recState.elapsedMs
                        VoiceRecordService.stop(context)
                        if (f != null && f.exists() && dur > 1000) onDone(f, dur)
                        else { f?.delete(); onDismiss() }
                    },
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(ink.copy(alpha = 0.08f))
                ) {
                    Icon(Icons.Filled.Check, "Done", tint = ink)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LiveWaveform(active: Boolean, ink: Color) {
    var tick by remember { mutableLongStateOf(0L) }
    LaunchedEffect(active) {
        while (active) {
            delay(180)
            tick++
        }
    }
    val bars = remember(tick) {
        List(32) { Random.nextInt(6, 30) }
    }
    Canvas(Modifier.fillMaxWidth().height(72.dp)) {
        val bw = size.width / bars.size
        bars.forEachIndexed { i, h ->
            val bh = (h / 30f) * size.height
            drawRoundRect(
                color = ink.copy(alpha = if (active) 0.85f else 0.3f),
                topLeft = Offset(i * bw + bw * 0.22f, (size.height - bh) / 2),
                size = Size(bw * 0.56f, bh),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
        }
    }
    if (!active) {
        Box(Modifier.size(0.dp)) {
            Icon(Icons.Filled.Mic, null, tint = Color.Transparent)
        }
    }
}
