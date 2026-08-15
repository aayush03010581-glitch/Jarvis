package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AlertRed
import com.example.ui.theme.HoloBgDark
import com.example.ui.theme.HoloCardBorder
import com.example.ui.theme.HoloCardBorderGlow
import com.example.ui.theme.HoloCyan
import com.example.ui.theme.HoloCyanBright
import com.example.ui.theme.HoloCyanDim
import com.example.ui.theme.HoloSurfaceDark
import com.example.ui.theme.HoloSurfaceVariant
import com.example.ui.theme.HoloTextPrimary
import com.example.ui.theme.HoloTextSecondary
import com.example.ui.theme.StarkGold
import com.example.ui.theme.TechGreen
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

data class RadarBlip(
    val id: String,
    val callsign: String,
    val angleDeg: Float,
    val distanceRatio: Float,
    val isHostile: Boolean,
    val altitude: String,
    val speed: String
)

@Composable
fun RadarScannerView(
    modifier: Modifier = Modifier,
    onTriggerScan: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep_anim")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep"
    )

    val blips = remember {
        mutableStateListOf(
            RadarBlip("1", "VERONICA-SAT", 45f, 0.78f, false, "LEO 420km", "7.8 km/s"),
            RadarBlip("2", "CIVILIAN-FLIGHT-21", 140f, 0.45f, false, "FL340", "480 kts"),
            RadarBlip("3", "STARK-DRONE-03", 260f, 0.60f, false, "3,500 ft", "120 kts"),
            RadarBlip("4", "ATMOSPHERIC-PROBE", 315f, 0.32f, false, "12,000 ft", "0 kts")
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(HoloBgDark)
            .border(1.dp, HoloCardBorder, RoundedCornerShape(12.dp))
            .padding(12.dp)
            .testTag("radar_scanner_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Radar Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = null,
                    tint = HoloCyan,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "360° AIRSPACE & ORBITAL RADAR",
                    color = HoloTextPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            IconButton(
                onClick = {
                    blips.clear()
                    blips.addAll(
                        listOf(
                            RadarBlip("1", "VERONICA-SAT", Random.nextFloat() * 360f, 0.82f, false, "LEO 420km", "7.8 km/s"),
                            RadarBlip("2", "AIR-TRANSPORT-${Random.nextInt(10, 99)}", Random.nextFloat() * 360f, Random.nextFloat() * 0.6f + 0.2f, false, "FL360", "510 kts"),
                            RadarBlip("3", "STARK-SENTRY-${Random.nextInt(1, 9)}", Random.nextFloat() * 360f, Random.nextFloat() * 0.5f + 0.3f, false, "4,200 ft", "180 kts")
                        )
                    )
                    onTriggerScan()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Rescan",
                    tint = HoloCyanBright,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Canvas Radar Screen
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(120.dp))
                .background(HoloSurfaceDark)
                .border(1.5.dp, HoloCardBorderGlow, RoundedCornerShape(120.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = min(size.width, size.height) / 2f * 0.95f

                // Concentric Range Rings (25km, 50km, 75km, 100km)
                for (step in 1..4) {
                    val r = maxRadius * (step / 4f)
                    drawCircle(
                        color = HoloCyanDim.copy(alpha = 0.25f),
                        radius = r,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Crosshair axes
                drawLine(
                    color = HoloCyanDim.copy(alpha = 0.35f),
                    start = Offset(center.x - maxRadius, center.y),
                    end = Offset(center.x + maxRadius, center.y),
                    strokeWidth = 1.dp.toPx()
                )
                drawLine(
                    color = HoloCyanDim.copy(alpha = 0.35f),
                    start = Offset(center.x, center.y - maxRadius),
                    end = Offset(center.x, center.y + maxRadius),
                    strokeWidth = 1.dp.toPx()
                )

                // Rotating Radar Sweep Cone
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            HoloCyan.copy(alpha = 0.05f),
                            HoloCyan.copy(alpha = 0.35f)
                        ),
                        center = center
                    ),
                    startAngle = sweepAngle - 60f,
                    sweepAngle = 60f,
                    useCenter = true,
                    topLeft = Offset(center.x - maxRadius, center.y - maxRadius),
                    size = Size(maxRadius * 2f, maxRadius * 2f)
                )

                // Radar Blips
                for (blip in blips) {
                    val rad = blip.angleDeg * (PI / 180f)
                    val blipDist = maxRadius * blip.distanceRatio
                    val blipPos = Offset(
                        center.x + cos(rad).toFloat() * blipDist,
                        center.y + sin(rad).toFloat() * blipDist
                    )

                    // Blip Dot
                    drawCircle(
                        color = if (blip.isHostile) AlertRed else TechGreen,
                        radius = 4.dp.toPx(),
                        center = blipPos
                    )
                    // Blip Glow
                    drawCircle(
                        color = (if (blip.isHostile) AlertRed else TechGreen).copy(alpha = 0.4f),
                        radius = 8.dp.toPx(),
                        center = blipPos,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Target Tracking Table
        Text(
            text = "IDENTIFIED RADAR TRANSPONDERS",
            color = HoloTextSecondary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            blips.forEach { blip ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(HoloSurfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (blip.isHostile) AlertRed else TechGreen)
                        )
                        Text(
                            text = blip.callsign,
                            color = HoloTextPrimary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "${blip.altitude} // ${blip.speed}",
                        color = HoloCyanBright,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
