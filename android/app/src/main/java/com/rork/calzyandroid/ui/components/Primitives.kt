package com.rork.calzyandroid.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rork.calzyandroid.ui.theme.CalzyColors
import com.rork.calzyandroid.ui.theme.MetricFontFamily

/** Ambient app background — soft lavender / peach mist over off-white. */
@Composable
fun CalzyBackdrop(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CalzyColors.background)
            .drawBehind {
                val w = size.width
                val h = size.height
                fun mist(color: Color, alpha: Float, center: Offset, radius: Float) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                        center = center,
                    )
                }
                mist(CalzyColors.mistLavender, 0.95f, Offset(w * 0.02f, -h * 0.02f), w * 0.72f)
                mist(CalzyColors.mistPeach, 0.9f, Offset(w * 1.02f, h * 0.04f), w * 0.66f)
                mist(CalzyColors.mistSky, 0.75f, Offset(-w * 0.14f, h * 0.46f), w * 0.62f)
                mist(CalzyColors.mistApricot, 0.65f, Offset(w * 1.14f, h * 0.6f), w * 0.62f)
                mist(CalzyColors.mistLavender, 0.7f, Offset(w * 0.32f, h * 1.04f), w * 0.7f)
                mist(CalzyColors.mistSage, 0.45f, Offset(w * 0.96f, h * 0.98f), w * 0.5f)
            },
    ) {
        content()
    }
}

/** Frosted card container — CardBackground on iOS, `.calzy-card` on web. */
@Composable
fun CalzyCard(
    modifier: Modifier = Modifier,
    radius: Dp = 22.dp,
    padding: PaddingValues = PaddingValues(0.dp),
    fill: Color = CalzyColors.cardFill,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .border(1.dp, CalzyColors.cardBorder, shape)
            .padding(padding),
    ) {
        content()
    }
}

/** Scale-on-press wrapper matching the web `.pressable` affordance. */
@Composable
fun Pressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    haptic: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(120),
        label = "pressScale",
    )
    val haptics = LocalHapticFeedback.current
    Box(
        modifier = modifier
            .scale(scale)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
            ) {
                if (haptic) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Big rounded number — Nunito ExtraBold, the app's signature metric style. */
@Composable
fun MetricText(
    text: String,
    size: Int,
    modifier: Modifier = Modifier,
    color: Color = CalzyColors.ink,
    align: TextAlign? = null,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = size.sp,
        fontFamily = MetricFontFamily,
        fontWeight = FontWeight.ExtraBold,
        textAlign = align,
        lineHeight = (size * 1.08f).sp,
    )
}

/** Circular progress ring with gradient sweep and rounded caps. */
@Composable
fun RingProgress(
    progress: Float,
    size: Dp,
    lineWidth: Dp,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = color.copy(alpha = 0.12f),
    content: @Composable () -> Unit = {},
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(750),
        label = "ring",
    )
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = lineWidth.toPx()
            val inset = strokePx / 2
            val arcSize = androidx.compose.ui.geometry.Size(
                this.size.width - strokePx,
                this.size.height - strokePx,
            )
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round),
            )
            if (animated > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round),
                )
            }
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            content()
        }
    }
}

/** Full-width black pill CTA. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    Button(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp),
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = CalzyColors.ink,
            contentColor = Color.White,
            disabledContainerColor = CalzyColors.ink.copy(alpha = 0.35f),
            disabledContentColor = Color.White,
        ),
    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Two-or-more option segmented control on a well background. */
@Composable
fun <T> SegmentedControl(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(CalzyColors.well)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        options.forEach { (value, label) ->
            val active = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (active) Color.White else Color.Transparent)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelect(value)
                    }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) CalzyColors.ink else CalzyColors.inkSoft,
                )
            }
        }
    }
}

/** Styled Material slider matching the app tint. */
@Composable
fun CalzySlider(
    value: Float,
    onChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    tint: Color = CalzyColors.ink,
) {
    Slider(
        value = value,
        onValueChange = onChange,
        valueRange = range,
        steps = steps,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = tint,
            inactiveTrackColor = tint.copy(alpha = 0.14f),
        ),
    )
}

/** App-tinted switch. */
@Composable
fun CalzyToggle(
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Switch(
        checked = checked,
        onCheckedChange = onChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedTrackColor = CalzyColors.mint,
            checkedThumbColor = Color.White,
            uncheckedTrackColor = CalzyColors.inkFaint.copy(alpha = 0.4f),
            uncheckedThumbColor = Color.White,
            uncheckedBorderColor = Color.Transparent,
        ),
    )
}

/** Thin divider matching the web `.calzy-hairline`. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(1.dp)
            .background(CalzyColors.ink.copy(alpha = 0.06f)),
    )
}

/** Vertical hairline used between inline stats. */
@Composable
fun RowScope.VHairline(height: Dp = 30.dp) {
    Box(
        modifier = Modifier
            .align(Alignment.CenterVertically)
            .size(width = 1.dp, height = height)
            .background(CalzyColors.ink.copy(alpha = 0.06f)),
    )
}
