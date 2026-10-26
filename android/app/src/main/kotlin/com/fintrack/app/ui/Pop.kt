package com.fintrack.app.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** NeoPOP is built from sharp rectangles. */
val TileShape: Shape = RectangleShape

/** Plain black canvas behind every screen. */
@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Fin.colors.background), content = content)
}

/**
 * The NeoPOP "plunk": a flat face with hard right and bottom edges that make it look raised.
 * When pressed, the face slides into the edges and sits flat. Static when [onClick] is null.
 */
@Composable
fun Plunk(
    face: Color,
    edgeRight: Color,
    edgeBottom: Color,
    modifier: Modifier = Modifier,
    faceModifier: Modifier = Modifier,
    depth: Dp = 4.dp,
    border: Color? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val shift by animateDpAsState(if (pressed && onClick != null) depth else 0.dp, tween(70), label = "plunk")
    Box(
        modifier
            .drawBehind {
                val d = depth.toPx()
                val s = shift.toPx()
                val w = size.width - d
                val h = size.height - d
                if (s >= d) return@drawBehind
                // Right edge: from the face's right side down to where the face sits when pressed.
                drawPath(Path().apply {
                    moveTo(s + w, s); lineTo(d + w, d); lineTo(d + w, d + h); lineTo(s + w, s + h); close()
                }, edgeRight)
                drawPath(Path().apply {
                    moveTo(s, s + h); lineTo(s + w, s + h); lineTo(d + w, d + h); lineTo(d, d + h); close()
                }, edgeBottom)
            }
            .then(if (onClick != null) Modifier.clickable(interaction, indication = null, onClick = onClick) else Modifier)
            .padding(end = depth, bottom = depth),
    ) {
        Box(
            faceModifier
                .offset(shift, shift)
                .background(face)
                .then(if (border != null) Modifier.border(1.dp, border) else Modifier),
            content = content,
        )
    }
}

/** Flat dark tile with a hairline border; the default container for every section. */
@Composable
fun Tile(
    modifier: Modifier = Modifier,
    shape: Shape = TileShape,
    padding: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Fin.colors
    Column(
        modifier
            .clip(shape)
            .background(c.surface)
            .border(1.dp, c.border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** The one loud tile per screen: accent face with deep 3D edges. */
@Composable
fun HeroTile(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = Fin.colors
    Plunk(c.accent, c.accentEdgeRight, c.accentEdgeBottom, modifier, faceModifier = Modifier.fillMaxWidth(), depth = 6.dp) {
        Column(Modifier.fillMaxWidth().padding(22.dp), content = content)
    }
}

/** Primary action: white plunk button, black bold uppercase text. */
@Composable
fun PopButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = Fin.colors
    Plunk(
        face = if (enabled) Color.White else c.surfaceRaised,
        edgeRight = if (enabled) c.buttonEdgeRight else c.border,
        edgeBottom = if (enabled) c.buttonEdgeBottom else c.border,
        modifier = modifier,
        faceModifier = Modifier.fillMaxWidth().height(52.dp),
        onClick = if (enabled) onClick else null,
    ) {
        Text(
            text.uppercase(), color = if (enabled) Color.Black else c.inkFaint,
            style = MaterialTheme.typography.labelLarge, fontSize = 14.sp,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 20.dp),
        )
    }
}

/** Secondary action: dark plunk with a white outline. */
@Composable
fun PopOutlineButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    val c = Fin.colors
    Plunk(
        face = c.background, edgeRight = c.inkFaint, edgeBottom = c.border, modifier = modifier,
        faceModifier = Modifier.fillMaxWidth().height(52.dp), border = if (enabled) c.ink else c.border,
        onClick = if (enabled) onClick else null,
    ) {
        Row(Modifier.align(Alignment.Center).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            leading?.invoke()
            if (leading != null) Spacer(Modifier.width(10.dp))
            Text(text.uppercase(), style = MaterialTheme.typography.labelLarge, fontSize = 14.sp,
                color = if (enabled) c.ink else c.inkFaint)
        }
    }
}

/** ‹ OCTOBER 2026 › */
@Composable
fun MonthSwitcher(label: String, canGoNext: Boolean, onPrev: () -> Unit, onNext: () -> Unit) {
    val c = Fin.colors
    Row(Modifier.border(1.dp, c.border).background(c.surface), verticalAlignment = Alignment.CenterVertically) {
        StepButton("‹", onPrev, enabled = true)
        Text(label.uppercase(), style = CapsLabel, color = c.ink, modifier = Modifier.padding(horizontal = 4.dp))
        StepButton("›", onNext, enabled = canGoNext)
    }
}

@Composable
private fun StepButton(glyph: String, onClick: () -> Unit, enabled: Boolean) {
    Box(Modifier.size(38.dp).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(glyph, fontSize = 22.sp, color = if (enabled) Fin.colors.ink else Fin.colors.track)
    }
}

/** Square-cornered initial badge for a merchant or person; [gradient] = accent fill. */
@Composable
fun Avatar(name: String, size: Dp = 42.dp, gradient: Boolean = false) {
    val c = Fin.colors
    Box(
        Modifier.size(size).background(if (gradient) c.accent else c.surfaceRaised)
            .then(if (gradient) Modifier else Modifier.border(1.dp, c.border)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().firstOrNull()?.uppercase() ?: "•",
            color = c.ink, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.42f).sp,
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    val c = Fin.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = CapsLabel, color = c.inkSoft, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                "${action.uppercase()}  →", style = CapsLabel, color = c.ink,
                modifier = Modifier.clickable(onClick = onAction).padding(horizontal = 4.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
fun Faint(text: String, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.bodySmall, color = Fin.colors.inkSoft,
        maxLines = maxLines, overflow = TextOverflow.Ellipsis)
}

data class NavItem(val label: String, val icon: ImageVector)

/** Full-width black bar; the selected destination is white with an accent bar above it. */
@Composable
fun PopNavBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Fin.colors
    Column(modifier.fillMaxWidth().background(c.background)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
        Row(Modifier.fillMaxWidth().navigationBarsPadding()) {
            items.forEachIndexed { i, item ->
                val isSel = i == selected
                Column(
                    Modifier.weight(1f).clickable(remember { MutableInteractionSource() }, indication = null) { onSelect(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.width(28.dp).height(3.dp).background(if (isSel) c.accent else Color.Transparent))
                    Spacer(Modifier.height(9.dp))
                    Icon(item.icon, contentDescription = item.label, tint = if (isSel) c.ink else c.inkFaint, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.height(4.dp))
                    Text(item.label.uppercase(), style = CapsLabel, fontSize = 9.sp, color = if (isSel) c.ink else c.inkFaint)
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
fun RowScope.Stat(label: String, value: String, valueColor: Color = Color.White, labelColor: Color = Color.White.copy(alpha = 0.75f)) {
    Column(Modifier.weight(1f)) {
        Text(label.uppercase(), style = CapsLabel, fontSize = 9.sp, color = labelColor, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium.merge(MoneyStyle), color = valueColor, maxLines = 1)
    }
}

/** Underline-free rectangular input with a caps label above it; white border when focused. */
@Composable
fun PopTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    enabled: Boolean = true,
) {
    val c = Fin.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    Column(modifier.fillMaxWidth()) {
        Text(label.uppercase(), style = CapsLabel, fontSize = 10.sp, color = c.inkSoft)
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value, onValueChange = onValueChange, enabled = enabled, singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = if (enabled) c.ink else c.inkFaint),
            cursorBrush = SolidColor(c.ink),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            interactionSource = interaction,
            modifier = Modifier.fillMaxWidth().background(c.surfaceRaised)
                .border(1.dp, if (focused) c.ink else c.border).padding(horizontal = 14.dp, vertical = 15.dp),
        )
    }
}

/** Chip: selected = white plunk with black text; otherwise an outlined label. */
@Composable
fun PopChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = Fin.colors
    if (selected) {
        Plunk(Color.White, c.buttonEdgeRight, c.buttonEdgeBottom, depth = 3.dp, onClick = onClick) {
            Text(text.uppercase(), style = CapsLabel, color = Color.Black, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
        }
    } else {
        Box(Modifier.padding(end = 3.dp, bottom = 3.dp).border(1.dp, c.border).clickable(onClick = onClick)) {
            Text(text.uppercase(), style = CapsLabel, color = c.inkSoft, modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp))
        }
    }
}
