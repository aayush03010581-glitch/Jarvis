package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedGlow
import com.example.ui.theme.HoloBgDark
import com.example.ui.theme.HoloCyan
import com.example.ui.theme.HoloCyanBright
import com.example.ui.theme.HoloCyanDim
import com.example.ui.theme.HoloCyanGlow
import com.example.ui.theme.HoloCyanUltraGlow
import com.example.ui.theme.StarkAmber
import com.example.ui.theme.StarkGold
import com.example.ui.theme.StarkGoldBright
import com.example.ui.theme.StarkGoldGlow
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class EyeState {
    IDLE,
    LISTENING,
    COMPUTING,
    SPEAKING,
    COMBAT
}

@Composable
fun JarvisEyeView(
    state: EyeState,
    audioWaveform: List<Float> = emptyList(),
    modifier: Modifier = Modifier,
    onEyeClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_eye_anim")

    // Outer slow clockwise rotation
    val outerAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    EyeState.COMPUTING -> 4000
                    EyeState.LISTENING -> 6000
                    EyeState.COMBAT -> 3000
                    else -> 12000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rot"
    )

    // Inner fast counter-clockwise rotation
    val innerAngle by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    EyeState.COMPUTING -> 2500
                    EyeState.LISTENING -> 4500
                    EyeState.COMBAT -> 2000
                    else -> 8000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rot"
    )

    // Breathing pulse for the reactor core
    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    EyeState.SPEAKING -> 500
                    EyeState.COMPUTING -> 800
                    EyeState.COMBAT -> 600
                    else -> 1800
                },
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "core_pulse"
    )

    // Interactive Shockwave animation on tap
    val shockwaveAnim = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    val primaryColor = when (state) {
        EyeState.COMBAT -> AlertRed
        EyeState.LISTENING -> StarkGold
        EyeState.COMPUTING -> HoloCyanBright
        EyeState.SPEAKING -> HoloCyan
        EyeState.IDLE -> HoloCyan
    }

    val secondaryColor = when (state) {
        EyeState.COMBAT -> AlertRedGlow
        EyeState.LISTENING -> StarkGoldBright
        EyeState.COMPUTING -> StarkAmber
        EyeState.SPEAKING -> HoloCyanBright
        EyeState.IDLE -> HoloCyanDim
    }

    val glowColor = when (state) {
        EyeState.COMBAT -> AlertRedGlow
        EyeState.LISTENING -> StarkGoldGlow
        else -> HoloCyanGlow
    }

    Box(
        modifier = modifier
            .testTag("jarvis_eye_interactive")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                scope.launch {
                    shockwaveAnim.snapTo(0f)
                    shockwaveAnim.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(600, easing = FastOutSlowInEasing)
                    )
                }
                onEyeClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = min(size.width, size.height) / 2f * 0.92f

            // 1. Shockwave Ripple if active
            if (shockwaveAnim.value > 0f) {
                val waveRadius = maxRadius * shockwaveAnim.value
                val waveAlpha = (1f - shockwaveAnim.value).coerceIn(0f, 1f)
                drawCircle(
                    color = primaryColor.copy(alpha = waveAlpha * 0.7f),
                    radius = waveRadius,
                    center = center,
                    style = Stroke(width = 3.dp.toPx() * (1f - shockwaveAnim.value * 0.5f))
                )
            }

            // 2. Ambient Holographic Background Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        glowColor,
                        HoloCyanUltraGlow,
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius * 1.1f
                ),
                radius = maxRadius,
                center = center
            )

            // 3. Outermost Segmented HUD Orbit Ring
            rotate(outerAngle, pivot = center) {
                drawOuterSegmentedRing(
                    center = center,
                    radius = maxRadius * 0.95f,
                    color = primaryColor.copy(alpha = 0.85f),
                    strokeWidth = 2.5.dp.toPx(),
                    accentColor = secondaryColor
                )
            }

            // 4. Middle Tech Aperture / Coils
            rotate(innerAngle, pivot = center) {
                drawApertureCoils(
                    center = center,
                    radius = maxRadius * 0.72f,
                    primaryColor = primaryColor,
                    secondaryColor = secondaryColor,
                    strokeWidth = 2.dp.toPx()
                )
            }

            // 5. Audio-Reactive Waveform Equalizer Ring (when speaking or listening)
            drawAudioSpectrumRing(
                center = center,
                radius = maxRadius * 0.52f,
                audioWaveform = audioWaveform,
                state = state,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor
            )

            // 6. Glowing Inner Reactor Core & Iris
            drawReactorCore(
                center = center,
                radius = maxRadius * 0.35f * corePulse,
                primaryColor = primaryColor,
                secondaryColor = secondaryColor,
                state = state
            )

            // 7. Center Quantum Eye Pupil
            val pupilRadius = maxRadius * 0.12f * (if (state == EyeState.SPEAKING) corePulse else 1f)
            drawCircle(
                color = HoloBgDark,
                radius = pupilRadius,
                center = center
            )
            drawCircle(
                color = primaryColor,
                radius = pupilRadius * 0.65f,
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = pupilRadius * 0.28f,
                center = center
            )

            // 8. Crosshair HUD Reticle Brackets
            drawHudReticles(
                center = center,
                radius = maxRadius * 0.88f,
                color = primaryColor.copy(alpha = 0.6f)
            )
        }

        // HUD State Pill Overlay at bottom of eye
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
        ) {
            Text(
                text = "● ${state.name} // MK-LXXXV",
                color = primaryColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.5.sp
            )
        }
    }
}

// Draw Outer Segmented Arc Ring with Tick Marks
private fun DrawScope.drawOuterSegmentedRing(
    center: Offset,
    radius: Float,
    color: Color,
    strokeWidth: Float,
    accentColor: Color
) {
    // 4 Main Curved Arcs
    val gap = 16f
    val sweep = (360f - (4 * gap)) / 4f
    for (i in 0 until 4) {
        val startAngle = i * (sweep + gap)
        drawArc(
            color = if (i % 2 == 0) color else accentColor,
            startAngle = startAngle,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }

    // Outer Precision Ticks (36 ticks)
    val tickRadius = radius + 6.dp.toPx()
    for (i in 0 until 36) {
        val angleDeg = i * 10f
        val rad = angleDeg * (PI / 180f)
        val isMajor = i % 9 == 0
        val isMedium = i % 3 == 0
        val tickLen = if (isMajor) 10.dp.toPx() else if (isMedium) 6.dp.toPx() else 3.dp.toPx()
        val tickColor = if (isMajor) accentColor else color.copy(alpha = 0.5f)

        val start = Offset(
            center.x + cos(rad).toFloat() * (tickRadius - tickLen),
            center.y + sin(rad).toFloat() * (tickRadius - tickLen)
        )
        val end = Offset(
            center.x + cos(rad).toFloat() * tickRadius,
            center.y + sin(rad).toFloat() * tickRadius
        )
        drawLine(
            color = tickColor,
            start = start,
            end = end,
            strokeWidth = if (isMajor) 2.5f else 1.2f
        )
    }
}

// Draw 10-Coil Stark Arc Reactor Aperture Gear
private fun DrawScope.drawApertureCoils(
    center: Offset,
    radius: Float,
    primaryColor: Color,
    secondaryColor: Color,
    strokeWidth: Float
) {
    // Thin base guide ring
    drawCircle(
        color = primaryColor.copy(alpha = 0.35f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.dp.toPx())
    )

    // 10 Radial Reactor Coils / Copper-Gold Magnetic Blocks
    val coilCount = 10
    for (i in 0 until coilCount) {
        val angleDeg = i * (360f / coilCount)
        val rad = angleDeg * (PI / 180f)
        val innerR = radius - 10.dp.toPx()
        val outerR = radius + 4.dp.toPx()

        val start = Offset(
            center.x + cos(rad).toFloat() * innerR,
            center.y + sin(rad).toFloat() * innerR
        )
        val end = Offset(
            center.x + cos(rad).toFloat() * outerR,
            center.y + sin(rad).toFloat() * outerR
        )

        drawLine(
            color = if (i % 2 == 0) secondaryColor else primaryColor,
            start = start,
            end = end,
            strokeWidth = 3.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    // Inner bounding ring
    drawCircle(
        color = primaryColor.copy(alpha = 0.6f),
        radius = radius - 12.dp.toPx(),
        center = center,
        style = Stroke(width = strokeWidth)
    )
}

// Draw Audio-Reactive Waveform Equalizer Ring
private fun DrawScope.drawAudioSpectrumRing(
    center: Offset,
    radius: Float,
    audioWaveform: List<Float>,
    state: EyeState,
    primaryColor: Color,
    secondaryColor: Color
) {
    val barCount = 24
    for (i in 0 until barCount) {
        val angleDeg = i * (360f / barCount)
        val rad = angleDeg * (PI / 180f)

        val waveformIndex = (i % (if (audioWaveform.isNotEmpty()) audioWaveform.size else 1))
        val amplitude = if (audioWaveform.isNotEmpty() && (state == EyeState.SPEAKING || state == EyeState.LISTENING)) {
            audioWaveform.getOrElse(waveformIndex) { 0.2f }
        } else {
            0.15f + (sin(angleDeg * 0.1).toFloat() * 0.08f).coerceAtLeast(0.05f)
        }

        val barLength = 4.dp.toPx() + (amplitude * 18.dp.toPx())
        val startR = radius
        val endR = radius + barLength

        val start = Offset(
            center.x + cos(rad).toFloat() * startR,
            center.y + sin(rad).toFloat() * startR
        )
        val end = Offset(
            center.x + cos(rad).toFloat() * endR,
            center.y + sin(rad).toFloat() * endR
        )

        drawLine(
            color = if (i % 3 == 0) secondaryColor else primaryColor.copy(alpha = 0.75f + amplitude * 0.25f),
            start = start,
            end = end,
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

// Draw Glowing Multi-Layered Arc Reactor Core
private fun DrawScope.drawReactorCore(
    center: Offset,
    radius: Float,
    primaryColor: Color,
    secondaryColor: Color,
    state: EyeState
) {
    // Multi-stop Radial Glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.95f),
                primaryColor.copy(alpha = 0.85f),
                secondaryColor.copy(alpha = 0.45f),
                Color.Transparent
            ),
            center = center,
            radius = radius * 1.25f
        ),
        radius = radius,
        center = center
    )

    // Geometric Triangle Core (Iconic Mark 6 & 85 Stark Element)
    val triPath = Path().apply {
        val r = radius * 0.82f
        val angle1 = -90.0 * (PI / 180.0)
        val angle2 = 30.0 * (PI / 180.0)
        val angle3 = 150.0 * (PI / 180.0)

        moveTo(center.x + (cos(angle1) * r).toFloat(), center.y + (sin(angle1) * r).toFloat())
        lineTo(center.x + (cos(angle2) * r).toFloat(), center.y + (sin(angle2) * r).toFloat())
        lineTo(center.x + (cos(angle3) * r).toFloat(), center.y + (sin(angle3) * r).toFloat())
        close()
    }

    drawPath(
        path = triPath,
        color = primaryColor.copy(alpha = 0.9f),
        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
    )

    // Inverted Triangle for nested Hexagram Stark Core
    val invTriPath = Path().apply {
        val r = radius * 0.82f
        val angle1 = 90.0 * (PI / 180.0)
        val angle2 = 210.0 * (PI / 180.0)
        val angle3 = 330.0 * (PI / 180.0)

        moveTo(center.x + (cos(angle1) * r).toFloat(), center.y + (sin(angle1) * r).toFloat())
        lineTo(center.x + (cos(angle2) * r).toFloat(), center.y + (sin(angle2) * r).toFloat())
        lineTo(center.x + (cos(angle3) * r).toFloat(), center.y + (sin(angle3) * r).toFloat())
        close()
    }

    drawPath(
        path = invTriPath,
        color = secondaryColor.copy(alpha = 0.65f),
        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    )
}

// Draw HUD Crosshair Brackets
private fun DrawScope.drawHudReticles(
    center: Offset,
    radius: Float,
    color: Color
) {
    val bracketLen = 14.dp.toPx()
    val r = radius

    // Top-Left bracket
    drawLine(color, Offset(center.x - r, center.y - r + bracketLen), Offset(center.x - r, center.y - r), 1.5f)
    drawLine(color, Offset(center.x - r, center.y - r), Offset(center.x - r + bracketLen, center.y - r), 1.5f)

    // Top-Right bracket
    drawLine(color, Offset(center.x + r, center.y - r + bracketLen), Offset(center.x + r, center.y - r), 1.5f)
    drawLine(color, Offset(center.x + r, center.y - r), Offset(center.x + r - bracketLen, center.y - r), 1.5f)

    // Bottom-Left bracket
    drawLine(color, Offset(center.x - r, center.y + r - bracketLen), Offset(center.x - r, center.y + r), 1.5f)
    drawLine(color, Offset(center.x - r, center.y + r), Offset(center.x - r + bracketLen, center.y + r), 1.5f)

    // Bottom-Right bracket
    drawLine(color, Offset(center.x + r, center.y + r - bracketLen), Offset(center.x + r, center.y + r), 1.5f)
    drawLine(color, Offset(center.x + r, center.y + r), Offset(center.x + r - bracketLen, center.y + r), 1.5f)
}
