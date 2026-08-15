package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.terminal.SuitTelemetry
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedGlow
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
import com.example.ui.theme.StarkAmber
import com.example.ui.theme.StarkGold
import com.example.ui.theme.StarkGoldBright
import com.example.ui.theme.TechGreen

@Composable
fun HudStatusPanels(
    telemetry: SuitTelemetry,
    isVoiceMuted: Boolean,
    onToggleVoiceMute: () -> Unit,
    onExecuteProtocol: (String) -> Unit,
    onAdjustReactorPower: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Active Protocol Alert Banner
        ActiveProtocolBanner(activeProtocol = telemetry.activeProtocol, isCombatMode = telemetry.isCombatMode)

        // Telemetry Grid (4 Gauge Cards)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TelemetryGaugeCard(
                title = "ARC CORE",
                value = "${telemetry.arcReactorPower}%",
                progress = telemetry.arcReactorPower / 100f,
                icon = Icons.Default.Bolt,
                color = HoloCyan,
                modifier = Modifier.weight(1f)
            )
            TelemetryGaugeCard(
                title = "ARMOR",
                value = "${telemetry.suitIntegrity}%",
                progress = telemetry.suitIntegrity / 100f,
                icon = Icons.Default.Shield,
                color = if (telemetry.suitIntegrity > 50) TechGreen else AlertRed,
                modifier = Modifier.weight(1f)
            )
            TelemetryGaugeCard(
                title = "REPULSOR",
                value = "${telemetry.repulsorCharge}%",
                progress = telemetry.repulsorCharge / 100f,
                icon = Icons.Default.Security,
                color = StarkGold,
                modifier = Modifier.weight(1f)
            )
            TelemetryGaugeCard(
                title = "FLIGHT",
                value = "${telemetry.thrusterEfficiency}%",
                progress = telemetry.thrusterEfficiency / 100f,
                icon = Icons.Default.Flight,
                color = HoloCyanBright,
                modifier = Modifier.weight(1f)
            )
        }

        // Arc Reactor Power Regulator Slider
        ReactorPowerControlCard(
            powerLevel = telemetry.arcReactorPower,
            temperature = telemetry.coreTemperature,
            onPowerChanged = onAdjustReactorPower
        )

        // Protocol Directives Bar
        StarkProtocolsCard(
            activeProtocol = telemetry.activeProtocol,
            onExecuteProtocol = onExecuteProtocol
        )
    }
}

@Composable
private fun ActiveProtocolBanner(activeProtocol: String, isCombatMode: Boolean) {
    val borderColor by animateColorAsState(
        targetValue = if (isCombatMode) AlertRed else HoloCardBorderGlow,
        animationSpec = tween(500),
        label = "banner_border"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (isCombatMode) AlertRedGlow else HoloSurfaceVariant)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = if (isCombatMode) Icons.Default.Warning else Icons.Default.Security,
                contentDescription = null,
                tint = if (isCombatMode) AlertRed else HoloCyan,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "PROTOCOL: $activeProtocol",
                color = if (isCombatMode) AlertRed else HoloCyanBright,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.2.sp
            )
        }

        Text(
            text = if (isCombatMode) "COMBAT STATUS" else "STABLE",
            color = if (isCombatMode) AlertRed else TechGreen,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
private fun TelemetryGaugeCard(
    title: String,
    value: String,
    progress: Float,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "gauge_prog"
    )

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(HoloSurfaceDark)
            .border(0.8.dp, HoloCardBorder, RoundedCornerShape(8.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = title,
                color = HoloTextSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            color = color,
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(4.dp))

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = color,
            trackColor = HoloSurfaceVariant
        )
    }
}

@Composable
private fun ReactorPowerControlCard(
    powerLevel: Int,
    temperature: Float,
    onPowerChanged: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(HoloSurfaceDark)
            .border(0.8.dp, HoloCardBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = HoloCyan,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "ARC REACTOR POWER REGULATOR",
                    color = HoloTextPrimary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "${powerLevel}% (${temperature}°C)",
                color = if (powerLevel > 90) HoloCyanBright else StarkAmber,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }

        Slider(
            value = powerLevel.toFloat(),
            onValueChange = { onPowerChanged(it.toInt()) },
            valueRange = 10f..150f,
            steps = 13,
            colors = SliderDefaults.colors(
                thumbColor = HoloCyanBright,
                activeTrackColor = HoloCyan,
                inactiveTrackColor = HoloSurfaceVariant
            ),
            modifier = Modifier.testTag("reactor_power_slider")
        )
    }
}

@Composable
private fun StarkProtocolsCard(
    activeProtocol: String,
    onExecuteProtocol: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(HoloSurfaceDark)
            .border(0.8.dp, HoloCardBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Text(
            text = "TACTICAL PROTOCOL DIRECTIVES",
            color = HoloTextSecondary,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(
                onClick = { onExecuteProtocol("veronica") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("protocol_veronica_btn"),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = HoloSurfaceVariant.copy(alpha = 0.5f),
                    contentColor = HoloCyanBright
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(listOf(HoloCardBorder, HoloCardBorderGlow))
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "VERONICA",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = { onExecuteProtocol("house_party") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("protocol_house_party_btn"),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = HoloSurfaceVariant.copy(alpha = 0.5f),
                    contentColor = StarkGoldBright
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(listOf(HoloCardBorder, StarkGold))
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "HOUSE PARTY",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = { onExecuteProtocol("nano_repair") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("protocol_nano_repair_btn"),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = HoloSurfaceVariant.copy(alpha = 0.5f),
                    contentColor = TechGreen
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Brush.horizontalGradient(listOf(HoloCardBorder, TechGreen))
                ),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "NANO REPAIR",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
