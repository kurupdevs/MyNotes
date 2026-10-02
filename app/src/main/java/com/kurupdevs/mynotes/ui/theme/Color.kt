package com.kurupdevs.mynotes.ui.theme

import androidx.compose.ui.graphics.Color

// ---- card pastels (dark default) ----
val CardCoral = Color(0xFFEA7B53)
val CardYellow = Color(0xFFF6D44E)
val CardCream = Color(0xFFF7EDC9)
val CardGreen = Color(0xFFA9D673)
val CardBlue = Color(0xFF889DB5)
val CardPurple = Color(0xFF8E7BD8)

// light-mode deepened pastels (~8%)
val CardCoralLight = Color(0xFFE56F45)
val CardYellowLight = Color(0xFFF0CD3F)
val CardCreamLight = Color(0xFFF2E4B8)
val CardGreenLight = Color(0xFF9CCC5F)
val CardBlueLight = Color(0xFF7A93AC)
val CardPurpleLight = Color(0xFF836DD2)

// ---- inks ----
val InkOnCoral = Color(0xFF3D1508)
val InkOnYellow = Color(0xFF241C0D)
val SubOnYellow = Color(0xFF6B5312)
val InkOnCream = Color(0xFF201A12)
val SubOnCream = Color(0xFF8A7A5E)
val CheckFill = Color(0xFF7E3B1F)
val CheckMark = Color(0xFFF7EDC9)
val HeartFavFill = Color(0xFF2E0F04)

// ---- chrome ----
val TitleWhite = Color(0xFFFFFFFF)
val ChipBadgeBg = Color(0xFF2A2A2A)
val ChipBadgeText = Color(0xFFA8A8A8)
val ChipIdleBorder = Color(0xFF3F3F3F)
val ChipIdleText = Color(0xFF7A7A7A)
val DockFab = Color(0xFF0B0B0B)
val DockFabIcon = Color(0xFFFFFFFF)
val DockMicGlass = Color(0x66FFFFFF)
val DockMicIcon = Color(0xFFF2F2F2)
val MenuBg = Color(0xFF171717)
val MenuDots = Color(0xFFCFCFCF)
val GlowGreen = Color(0xFFA9D673)
val GlowBlue = Color(0xFF889DB5)
val GlowPurple = Color(0xFF8E7BD8)

// light mode chrome
val LightBg = Color(0xFFF4F1EA)
val LightInk = Color(0xFF141210)
val LightChipActive = Color(0xFF141210)
val LightChipIdleBg = Color(0xFFE7E1D3)
val LightChipIdleText = Color(0xFF6B6257)

/** Card background for a color key + dark/light. */
fun cardColor(key: String, dark: Boolean): Color = when (key) {
    "coral" -> if (dark) CardCoral else CardCoralLight
    "yellow" -> if (dark) CardYellow else CardYellowLight
    "cream" -> if (dark) CardCream else CardCreamLight
    "green" -> if (dark) CardGreen else CardGreenLight
    "blue" -> if (dark) CardBlue else CardBlueLight
    "purple" -> if (dark) CardPurple else CardPurpleLight
    else -> if (dark) CardCream else CardCreamLight // default reads as cream
}

/** Primary ink (title/body) on a given card color. */
fun inkOnCard(key: String): Color = when (key) {
    "coral", "green", "blue", "purple" -> InkOnCoral
    "yellow" -> InkOnYellow
    else -> InkOnCream // cream + default
}

/** Muted subtitle ink on a given card color. */
fun subOnCard(key: String): Color = when (key) {
    "yellow" -> SubOnYellow
    "coral" -> InkOnCoral.copy(alpha = 0.55f)
    "green", "blue", "purple" -> InkOnCoral.copy(alpha = 0.6f)
    else -> SubOnCream
}

// ---- white theme (v2 home redesign) ----
val WhiteScreenBg = Color(0xFFFFFFFF)
val WhiteCardBg = Color(0xFFFFFFFF)
val WhiteSearchField = Color(0xFFF1EEE8)
val WhiteHint = Color(0xFF9A938A)
val WhitePreview = Color(0xFF6E6A63)
val WhiteDate = Color(0xFFA3A099)
val WhiteTitle = Color(0xFF141210)
val WhiteLabelPill = Color(0xFF262626)
val WhiteLabelPillText = Color(0xFFFFFFFF)
val WhiteAvatarBg = Color(0xFFF5A623)
val WhiteCheckFill = Color(0xFF1A1A1A)
val WhiteCheckMark = Color(0xFFFFFFFF)
val WhiteMicBg = Color(0xFFF1EEE8)
val WhiteSwipeBg = Color(0xFFF1EEE8)
