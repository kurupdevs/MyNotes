package com.kurupdevs.mynotes.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.kurupdevs.mynotes.R

// Quicksand ships as a variable font — weights are selected via variation
// settings on each Font entry, so fontWeight in TextStyle just works.
@OptIn(ExperimentalTextApi::class)
private val QuicksandFamily = FontFamily(
    Font(R.font.quicksand_variable, FontWeight.Light, variationSettings = FontVariation.Settings(FontVariation.weight(300))),
    Font(R.font.quicksand_variable, FontWeight.Normal, variationSettings = FontVariation.Settings(FontVariation.weight(400))),
    Font(R.font.quicksand_variable, FontWeight.Medium, variationSettings = FontVariation.Settings(FontVariation.weight(500))),
    Font(R.font.quicksand_variable, FontWeight.SemiBold, variationSettings = FontVariation.Settings(FontVariation.weight(600))),
    Font(R.font.quicksand_variable, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700)))
)

private fun qs(weight: FontWeight, size: Int, line: Int) = TextStyle(
    fontFamily = QuicksandFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp
)

val NotesTypography = Typography(
    displayLarge = qs(FontWeight.Light, 32, 40),   // "My Notes" header
    headlineMedium = qs(FontWeight.Bold, 18, 24),  // card title
    bodyLarge = qs(FontWeight.Medium, 16, 22),
    bodyMedium = qs(FontWeight.Medium, 14, 20),    // card body
    bodySmall = qs(FontWeight.Normal, 13, 16),
    labelLarge = qs(FontWeight.SemiBold, 14, 20),  // chip label
    labelMedium = qs(FontWeight.Medium, 12, 16),
    labelSmall = qs(FontWeight.Medium, 11, 14)     // badge count
)
