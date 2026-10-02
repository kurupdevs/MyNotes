package com.kurupdevs.mynotes.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.unit.dp

private val DarkScheme = darkColorScheme(
    background = Color(0xFF000000),
    surface = Color(0xFF0B0B0B),
    onBackground = TitleWhite,
    onSurface = TitleWhite,
    primary = CardCoral,
    surfaceVariant = MenuBg,
    onSurfaceVariant = MenuDots,
    outline = ChipIdleBorder
)

private val LightScheme = lightColorScheme(
    background = LightBg,
    surface = Color(0xFFFFFFFF),
    onBackground = LightInk,
    onSurface = LightInk,
    primary = CardCoralLight,
    surfaceVariant = LightChipIdleBg,
    onSurfaceVariant = LightChipIdleText,
    outline = Color(0xFFD8D0BE)
)

val CardShape = RoundedCornerShape(28.dp)
val ChipShape = RoundedCornerShape(50)
val SheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

/** Motion tokens per spec §1.7 */
object Motion {
    val pressSpring = spring<Float>(stiffness = 400f, dampingRatio = 0.7f)
    val swipeSpring = spring<Float>(stiffness = 300f, dampingRatio = 0.65f)
    val popSpring = spring<Float>(stiffness = 500f, dampingRatio = 0.6f)
    val placementSpring = spring<Float>(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.8f)
}

/** No-op indication — spec §1.7: press feedback is scale/spring based, no ripple anywhere. */
private object NoRippleIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = NoRippleNode()
    override fun hashCode(): Int = -1
    override fun equals(other: Any?): Boolean = other === this

    private class NoRippleNode : Modifier.Node(), DrawModifierNode {
        override fun ContentDrawScope.draw() {
            drawContent()
        }
    }
}

@Composable
fun MyNotesTheme(dark: Boolean = true, content: @Composable () -> Unit) {
    // spec: no default ripple anywhere — press feedback is scale/spring based
    CompositionLocalProvider(LocalIndication provides NoRippleIndication) {
        MaterialTheme(
            colorScheme = if (dark) DarkScheme else LightScheme,
            typography = NotesTypography,
            content = content
        )
    }
}
