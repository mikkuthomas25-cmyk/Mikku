package com.fintrack.app.ui

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

/** Soft, rounded cards. */
val TileShape: Shape = RoundedCornerShape(18.dp)
val PillShape: Shape = RoundedCornerShape(50)
/** The rounded square behind icons and initials. */
val SquircleShape: Shape = RoundedCornerShape(12.dp)

/** Deep carbon canvas behind every screen, with a faint emerald glow at the top. */
@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    val c = Fin.colors
    Box(
        Modifier.fillMaxSize().background(c.background).drawBehind {
            drawRect(Brush.radialGradient(
                listOf(c.accent.copy(alpha = 0.07f), Color.Transparent),
                center = Offset(size.width * 0.2f, 0f), radius = size.width * 0.9f,
            ))
        },
        content = content,
    )
}

/** Something you can press: shrinks slightly while pressed. ([edgeRight]/[edgeBottom] are no longer drawn.) */
@Composable
fun Plunk(
    face: Color,
    edgeRight: Color,
    edgeBottom: Color,
    modifier: Modifier = Modifier,
    faceModifier: Modifier = Modifier,
    depth: Dp = 4.dp,
    border: Color? = null,
    shape: Shape = PillShape,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed && onClick != null) 0.97f else 1f, tween(90), label = "press")
    Box(
        modifier
            .scale(scale)
            .then(if (onClick != null) Modifier.clickable(interaction, indication = null, onClick = onClick) else Modifier),
    ) {
        Box(
            faceModifier.clip(shape).background(face).then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier),
            content = content,
        )
    }
}

/** Charcoal card with a razor-thin border: the container for every section. */
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
            .border(0.5.dp, c.border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** The screen's headline card: charcoal with a soft emerald glow in one corner. */
@Composable
fun HeroTile(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = Fin.colors
    Column(
        modifier
            .clip(TileShape)
            .background(c.surface)
            .drawBehind {
                drawRect(Brush.radialGradient(
                    listOf(c.accent.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width, 0f), radius = size.width * 0.8f,
                ))
            }
            .border(0.5.dp, c.accent.copy(alpha = 0.35f), TileShape)
            .padding(22.dp),
        content = content,
    )
}

/** Primary action: an emerald pill with dark text. */
@Composable
fun PopButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = Fin.colors
    Plunk(
        face = if (enabled) c.accent else c.surfaceRaised, edgeRight = c.accent, edgeBottom = c.accent,
        modifier = modifier.glow(if (enabled) c.accent else Color.Transparent),
        faceModifier = Modifier.fillMaxWidth().height(52.dp),
        onClick = if (enabled) onClick else null,
    ) {
        Text(
            text, color = if (enabled) c.background else c.inkFaint,
            style = MaterialTheme.typography.labelLarge, fontSize = 15.sp,
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 20.dp),
        )
    }
}

/** Secondary action: a pill with a thin border. */
@Composable
fun PopOutlineButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    val c = Fin.colors
    Plunk(
        face = c.surface, edgeRight = c.border, edgeBottom = c.border, modifier = modifier,
        faceModifier = Modifier.fillMaxWidth().height(50.dp), border = c.border,
        onClick = if (enabled) onClick else null,
    ) {
        Row(Modifier.align(Alignment.Center).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            leading?.invoke()
            if (leading != null) Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, fontSize = 15.sp, color = if (enabled) c.ink else c.inkFaint,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** A soft glow under emerald elements. */
fun Modifier.glow(color: Color, radius: Dp = 18.dp): Modifier = drawBehind {
    if (color.alpha == 0f) return@drawBehind
    val r = radius.toPx()
    drawRoundRect(
        Brush.radialGradient(listOf(color.copy(alpha = 0.28f), Color.Transparent), center = center, radius = size.maxDimension * 0.7f),
        topLeft = Offset(-r, -r * 0.6f),
        size = androidx.compose.ui.geometry.Size(size.width + 2 * r, size.height + 1.2f * r),
        cornerRadius = CornerRadius(size.height),
    )
}

/** ‹ OCTOBER 2026 › */
@Composable
fun MonthSwitcher(label: String, canGoNext: Boolean, onPrev: () -> Unit, onNext: () -> Unit) {
    val c = Fin.colors
    Row(Modifier.clip(PillShape).border(0.5.dp, c.border, PillShape).background(c.surface), verticalAlignment = Alignment.CenterVertically) {
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

/** Rounded-square badge: the payee's initial, or [symbol] when given. [gradient] = emerald tint. */
@Composable
fun Avatar(name: String, size: Dp = 42.dp, gradient: Boolean = false, symbol: String? = null, tint: Color? = null) {
    val c = Fin.colors
    val bg = when {
        tint != null -> tint.copy(alpha = 0.14f)
        gradient -> c.accent.copy(alpha = 0.16f)
        else -> c.surfaceRaised
    }
    Box(
        Modifier.size(size).clip(SquircleShape).background(bg).border(0.5.dp, (tint ?: c.border).copy(alpha = if (tint != null) 0.35f else 1f), SquircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            symbol ?: (name.trim().firstOrNull()?.uppercase() ?: "•"),
            color = tint ?: if (gradient) c.accent else c.ink, fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * if (symbol != null) 0.44f else 0.4f).sp,
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
                action, style = MaterialTheme.typography.labelMedium, color = c.accent,
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

/**
 * Floating frosted tab bar: the tabs either side of a prominent "+ Add" button. [items] has an even
 * count; the add button sits in the middle. The active tab glows emerald.
 */
@Composable
fun PopNavBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, onAdd: (() -> Unit)? = null) {
    val c = Fin.colors
    val half = items.size / 2
    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            Modifier.fillMaxWidth().height(64.dp).clip(RoundedCornerShape(26.dp))
                .background(c.surface.copy(alpha = 0.92f)).border(0.5.dp, c.border, RoundedCornerShape(26.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEachIndexed { i, item ->
                if (onAdd != null && i == half) AddFab(onAdd)
                val isSel = i == selected
                Column(
                    Modifier.weight(1f).clickable(remember { MutableInteractionSource() }, indication = null) { onSelect(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(item.icon, contentDescription = item.label, tint = if (isSel) c.accent else c.inkFaint,
                        modifier = Modifier.size(22.dp).then(if (isSel) Modifier.glow(c.accent, 10.dp) else Modifier))
                    Spacer(Modifier.height(4.dp))
                    Text(item.label, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp,
                        color = if (isSel) c.accent else c.inkFaint, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun AddFab(onClick: () -> Unit) {
    val c = Fin.colors
    Row(
        Modifier.padding(horizontal = 4.dp).height(40.dp).clip(PillShape).background(c.surfaceRaised)
            .border(0.5.dp, c.accent.copy(alpha = 0.5f), PillShape).clickable(onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Add, null, tint = c.ink, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text("Add", style = MaterialTheme.typography.labelLarge, color = c.ink)
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

/** Rounded input with a small label above it; emerald border when focused. */
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
    val shape = RoundedCornerShape(14.dp)
    Column(modifier.fillMaxWidth()) {
        Text(label.uppercase(), style = CapsLabel, fontSize = 10.sp, color = c.inkSoft)
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = value, onValueChange = onValueChange, enabled = enabled, singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = if (enabled) c.ink else c.inkFaint),
            cursorBrush = SolidColor(c.accent),
            visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            interactionSource = interaction,
            modifier = Modifier.fillMaxWidth().clip(shape).background(c.surfaceRaised)
                .border(if (focused) 1.dp else 0.5.dp, if (focused) c.accent else c.border, shape).padding(horizontal = 14.dp, vertical = 15.dp),
        )
    }
}

/** Pill chip: selected = soft emerald fill and text; otherwise a thin outline. */
@Composable
fun PopChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = Fin.colors
    Box(
        Modifier.clip(PillShape)
            .background(if (selected) c.accent.copy(alpha = 0.16f) else Color.Transparent)
            .border(0.5.dp, if (selected) c.accent.copy(alpha = 0.6f) else c.border, PillShape)
            .clickable(onClick = onClick),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = if (selected) c.accent else c.inkSoft,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
    }
}
