package com.fintrack.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val CardShape = RoundedCornerShape(24.dp)
val PillShape = RoundedCornerShape(50)

/** App-wide backdrop: a calm base with two soft color washes that the glass cards sit on. */
@Composable
fun GlossyBackground(content: @Composable BoxScope.() -> Unit) {
    val c = Fin.colors
    Box(Modifier.fillMaxSize().background(c.background)) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                Brush.radialGradient(listOf(c.glowA, Color.Transparent), center = Offset(0f, 0f), radius = size.width * 0.9f),
                radius = size.width * 0.9f, center = Offset(0f, 0f),
            )
            drawCircle(
                Brush.radialGradient(
                    listOf(c.glowB, Color.Transparent),
                    center = Offset(size.width, size.height * 0.12f), radius = size.width * 0.8f,
                ),
                radius = size.width * 0.8f, center = Offset(size.width, size.height * 0.12f),
            )
        }
        content()
    }
}

/** Frosted card: translucent fill, bright top rim fading out, soft shadow. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardShape,
    padding: Dp = 18.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Fin.colors
    Column(
        modifier
            .shadow(elevation = 12.dp, shape = shape, ambientColor = Color(0x143B4A6B), spotColor = Color(0x143B4A6B))
            .clip(shape)
            .background(c.glass)
            .border(1.dp, Brush.verticalGradient(listOf(c.glassEdgeTop, c.glassEdgeBottom)), shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** The glossy gradient card: deep blue to violet with a light sheen across the top. */
@Composable
fun HeroCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = Fin.colors
    Column(
        modifier
            .shadow(24.dp, CardShape, ambientColor = c.heroStart, spotColor = c.heroEnd)
            .clip(CardShape)
            .background(Brush.linearGradient(listOf(c.heroStart, c.heroEnd)))
            .drawWithContent {
                drawContent()
                // Gloss: a soft white bloom top-left and a faint highlight band.
                drawCircle(
                    Brush.radialGradient(listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                        center = Offset(size.width * 0.15f, 0f), radius = size.width * 0.7f),
                    radius = size.width * 0.7f, center = Offset(size.width * 0.15f, 0f),
                )
                drawRect(Brush.verticalGradient(
                    0f to Color.White.copy(alpha = 0.10f), 0.45f to Color.Transparent,
                ))
            }
            .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.Transparent)), CardShape)
            .padding(22.dp),
        content = content,
    )
}

@Composable
fun GradientButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = Fin.colors
    val alpha by animateFloatAsState(if (enabled) 1f else 0.45f, label = "btn")
    Box(
        modifier
            .height(54.dp)
            .shadow(if (enabled) 14.dp else 0.dp, PillShape, ambientColor = c.heroStart, spotColor = c.heroStart)
            .clip(PillShape)
            .background(Brush.horizontalGradient(listOf(c.heroStart.copy(alpha = alpha), c.heroEnd.copy(alpha = alpha))))
            .drawWithContent {
                drawContent()
                drawRect(Brush.verticalGradient(0f to Color.White.copy(alpha = 0.22f), 0.5f to Color.Transparent))
            }
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge, fontSize = 16.sp)
    }
}

@Composable
fun GlassPillButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
                    leading: (@Composable () -> Unit)? = null) {
    GlassCard(modifier.height(54.dp), shape = PillShape, padding = 0.dp, onClick = if (enabled) onClick else null) {
        Row(Modifier.fillMaxSize().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center) {
            leading?.invoke()
            if (leading != null) Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.labelLarge, fontSize = 16.sp,
                color = if (enabled) Fin.colors.ink else Fin.colors.inkFaint)
        }
    }
}

/** ‹ October 2026 › */
@Composable
fun MonthSwitcher(label: String, canGoNext: Boolean, onPrev: () -> Unit, onNext: () -> Unit) {
    val c = Fin.colors
    GlassCard(shape = PillShape, padding = 4.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundIcon("‹", onPrev, enabled = true)
            Text(label, style = MaterialTheme.typography.labelLarge, color = c.ink, modifier = Modifier.padding(horizontal = 6.dp))
            RoundIcon("›", onNext, enabled = canGoNext)
        }
    }
}

@Composable
private fun RoundIcon(glyph: String, onClick: () -> Unit, enabled: Boolean) {
    Box(
        Modifier.size(34.dp).clip(CircleShape).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, fontSize = 22.sp, color = if (enabled) Fin.colors.ink else Fin.colors.inkFaint.copy(alpha = 0.5f))
    }
}

/** Circle with the first letter of a merchant or name. */
@Composable
fun Avatar(name: String, size: Dp = 42.dp, gradient: Boolean = false) {
    val c = Fin.colors
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (gradient) Brush.linearGradient(listOf(c.heroStart, c.heroEnd)) else Brush.linearGradient(listOf(c.accentSoft, c.accentSoft))),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.trim().firstOrNull()?.uppercase() ?: "•",
            color = if (gradient) Color.White else c.heroStart,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.4f).sp,
        )
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, color = Fin.colors.ink, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(action, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(PillShape).clickable(onClick = onAction).padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}

@Composable
fun Faint(text: String, modifier: Modifier = Modifier, maxLines: Int = Int.MAX_VALUE) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.bodySmall, color = Fin.colors.inkSoft,
        maxLines = maxLines, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
}

data class NavItem(val label: String, val icon: ImageVector)

/** Floating frosted pill. The selected item expands to show its label. */
@Composable
fun FloatingNavBar(items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = Fin.colors
    GlassCard(modifier, shape = PillShape, padding = 6.dp) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            items.forEachIndexed { i, item ->
                val isSel = i == selected
                val bg by animateColorAsState(if (isSel) c.heroStart else Color.Transparent, tween(250), label = "nav")
                Row(
                    Modifier
                        .clip(PillShape)
                        .background(bg)
                        .clickable(remember { MutableInteractionSource() }, indication = null) { onSelect(i) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(item.icon, contentDescription = item.label, tint = if (isSel) Color.White else c.inkSoft,
                        modifier = Modifier.size(22.dp))
                    AnimatedVisibility(isSel, enter = fadeIn() + expandHorizontally(), exit = fadeOut() + shrinkHorizontally()) {
                        Text(item.label, color = Color.White, style = MaterialTheme.typography.labelLarge,
                            modifier = Modifier.padding(start = 8.dp), maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.Stat(label: String, value: String, valueColor: Color = Color.White, labelColor: Color = Color.White.copy(alpha = 0.75f)) {
    Column(Modifier.weight(1f)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = labelColor)
        Text(value, style = MaterialTheme.typography.titleMedium.merge(MoneyStyle), color = valueColor, maxLines = 1)
    }
}

@Composable
fun FinTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    password: Boolean = false,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    enabled: Boolean = true,
) {
    val c = Fin.colors
    androidx.compose.material3.OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        visualTransformation = if (password) androidx.compose.ui.text.input.PasswordVisualTransformation()
        else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.heroStart,
            unfocusedBorderColor = c.track,
            focusedContainerColor = c.glass,
            unfocusedContainerColor = c.glass,
            disabledContainerColor = c.glass,
        ),
        modifier = modifier.fillMaxWidth(),
    )
}

/** Pill chip: gradient when selected, glass otherwise. */
@Composable
fun FinChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val c = Fin.colors
    Box(
        Modifier
            .clip(PillShape)
            .background(
                if (selected) Brush.horizontalGradient(listOf(c.heroStart, c.heroEnd))
                else Brush.horizontalGradient(listOf(c.glass, c.glass)),
            )
            .border(1.dp, if (selected) Color.Transparent else c.track, PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (selected) Color.White else c.inkSoft)
    }
}
