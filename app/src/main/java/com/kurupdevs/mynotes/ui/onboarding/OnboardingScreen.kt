package com.kurupdevs.mynotes.ui.onboarding

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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kurupdevs.mynotes.ui.common.DottedBackground
import com.kurupdevs.mynotes.ui.theme.TitleWhite
import kotlinx.coroutines.launch

private data class Slide(val title: String, val body: String, val cardColor: Color)

private val SLIDES = listOf(
    Slide(
        "Notes that look as good as your camera roll.",
        "Pastel cards, zero clutter. Your ideas finally have a home that slaps.",
        Color(0xFFEA7B53)
    ),
    Slide(
        "Checklists, voice notes, pics — all in one place.",
        "Free. Forever. No paywalls, no caps, no funny business.",
        Color(0xFFF6D44E)
    ),
    Slide(
        "Start anonymous, sync later.",
        "Your notes live on your phone first. Link Google whenever — or never.",
        Color(0xFFA9D673)
    )
)

@Composable
fun OnboardingScreen(dark: Boolean, onFinish: () -> Unit) {
    val pager = rememberPagerState(pageCount = { SLIDES.size })
    val scope = rememberCoroutineScope()
    val ink = if (dark) TitleWhite else Color(0xFF141210)

    Box(Modifier.fillMaxSize().background(if (dark) Color.Black else Color(0xFFF4F1EA))) {
        DottedBackground(dark)
        Column(Modifier.fillMaxSize().padding(28.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text(
                    "Skip",
                    style = MaterialTheme.typography.labelLarge,
                    color = ink.copy(alpha = 0.6f),
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null, onClick = onFinish
                    ).padding(8.dp)
                )
            }
            HorizontalPager(pager, Modifier.weight(1f)) { page ->
                val s = SLIDES[page]
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.Start
                ) {
                    // mockup-style card visual
                    Box(
                        Modifier.size(150.dp, 190.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(s.cardColor)
                            .padding(16.dp)
                    ) {
                        Column {
                            Box(Modifier.size(26.dp, 4.dp).clip(CircleShape).background(Color(0xFF3D1508).copy(alpha = 0.5f)).align(Alignment.CenterHorizontally))
                            Spacer(Modifier.height(10.dp))
                            Text("My Notes", style = MaterialTheme.typography.headlineMedium, color = Color(0xFF3D1508))
                            Spacer(Modifier.height(8.dp))
                            repeat(3) {
                                Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(Color(0xFF3D1508).copy(alpha = 0.18f)))
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(32.dp))
                    Text(s.title, style = MaterialTheme.typography.displayLarge, color = ink)
                    Spacer(Modifier.height(12.dp))
                    Text(s.body, style = MaterialTheme.typography.bodyLarge, color = ink.copy(alpha = 0.7f))
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    repeat(SLIDES.size) { i ->
                        Box(
                            Modifier.size(if (pager.currentPage == i) 24.dp else 8.dp, 8.dp)
                                .clip(CircleShape)
                                .background(if (pager.currentPage == i) ink else ink.copy(alpha = 0.25f))
                        )
                    }
                }
                Box(
                    Modifier.clip(RoundedCornerShape(50))
                        .background(if (dark) TitleWhite else Color(0xFF141210))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (pager.currentPage < SLIDES.size - 1) {
                                scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                            } else onFinish()
                        }
                        .padding(horizontal = 28.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (pager.currentPage < SLIDES.size - 1) "Next" else "Start writing",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (dark) Color.Black else Color.White
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
