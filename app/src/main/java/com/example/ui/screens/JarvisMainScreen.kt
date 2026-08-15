package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HudStatusPanels
import com.example.ui.components.JarvisEyeView
import com.example.ui.components.RadarScannerView
import com.example.ui.components.TerminalConsoleView
import com.example.ui.theme.AlertRed
import com.example.ui.theme.HoloBgDark
import com.example.ui.theme.HoloCardBorder
import com.example.ui.theme.HoloCardBorderGlow
import com.example.ui.theme.HoloCyan
import com.example.ui.theme.HoloCyanBright
import com.example.ui.theme.HoloCyanDim
import com.example.ui.theme.HoloSurfaceDark
import com.example.ui.theme.HoloSurfaceVariant
import com.example.ui.theme.HoloTextMuted
import com.example.ui.theme.HoloTextPrimary
import com.example.ui.theme.HoloTextSecondary
import com.example.ui.theme.StarkGold
import com.example.ui.theme.StarkGoldBright
import com.example.ui.theme.TechGreen
import com.example.viewmodel.JarvisViewModel

@Composable
fun JarvisMainScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val eyeState by viewModel.eyeState.collectAsState()
    val terminalLines by viewModel.terminalLines.collectAsState()
    val suitTelemetry by viewModel.suitTelemetry.collectAsState()
    val isExecuting by viewModel.isExecuting.collectAsState()
    val isVoiceMuted by viewModel.isVoiceMuted.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val audioWaveform by viewModel.audioWaveform.collectAsState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(HoloBgDark),
        containerColor = HoloBgDark,
        bottomBar = {
            JarvisBottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { viewModel.setSelectedTab(it) }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .statusBarsPadding()
        ) {
            // Top HUD Status Bar
            JarvisTopHudBar(
                isVoiceMuted = isVoiceMuted,
                onToggleVoiceMute = { viewModel.toggleVoiceMute() },
                isCombatMode = suitTelemetry.isCombatMode
            )

            // Dynamic Main Tab View
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "tab_content_anim",
                modifier = Modifier.weight(1f)
            ) { tabIndex ->
                when (tabIndex) {
                    0 -> {
                        // Eye & Core Telemetry View
                        HudEyeScreen(
                            eyeState = eyeState,
                            audioWaveform = audioWaveform,
                            suitTelemetry = suitTelemetry,
                            isVoiceMuted = isVoiceMuted,
                            onToggleVoiceMute = { viewModel.toggleVoiceMute() },
                            onEyeClick = { viewModel.triggerEyeDiagnostic() },
                            onExecuteProtocol = { viewModel.executeProtocol(it) },
                            onAdjustReactorPower = { viewModel.adjustReactorPower(it) }
                        )
                    }

                    1 -> {
                        // Full Terminal Shell View
                        TerminalConsoleView(
                            lines = terminalLines,
                            onExecuteCommand = { viewModel.executeTerminalCommand(it) },
                            onClearTerminal = { viewModel.clearTerminal() },
                            commandHistory = viewModel.getCommandHistory(),
                            isExecuting = isExecuting,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            onVoiceToggle = { viewModel.triggerEyeDiagnostic() }
                        )
                    }

                    2 -> {
                        // Radar Airspace & Threat Scan View
                        RadarScannerView(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            onTriggerScan = { viewModel.executeTerminalCommand("scan") }
                        )
                    }

                    3 -> {
                        // Protocols & Suit Systems Control View
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 6.dp)
                        ) {
                            HudStatusPanels(
                                telemetry = suitTelemetry,
                                isVoiceMuted = isVoiceMuted,
                                onToggleVoiceMute = { viewModel.toggleVoiceMute() },
                                onExecuteProtocol = { viewModel.executeProtocol(it) },
                                onAdjustReactorPower = { viewModel.adjustReactorPower(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JarvisTopHudBar(
    isVoiceMuted: Boolean,
    onToggleVoiceMute: () -> Unit,
    isCombatMode: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HoloSurfaceDark)
            .border(1.dp, HoloCardBorder)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isCombatMode) AlertRed else TechGreen)
            )
            Column {
                Text(
                    text = "J.A.R.V.I.S. // MARK LXXXV",
                    color = HoloCyanBright,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "STARK INDUSTRIES SECURE OS",
                    color = HoloTextMuted,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onToggleVoiceMute,
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(HoloSurfaceVariant)
                    .testTag("voice_mute_toggle_btn")
            ) {
                Icon(
                    imageVector = if (isVoiceMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = "Toggle Voice",
                    tint = if (isVoiceMuted) AlertRed else HoloCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun HudEyeScreen(
    eyeState: com.example.ui.components.EyeState,
    audioWaveform: List<Float>,
    suitTelemetry: com.example.data.terminal.SuitTelemetry,
    isVoiceMuted: Boolean,
    onToggleVoiceMute: () -> Unit,
    onEyeClick: () -> Unit,
    onExecuteProtocol: (String) -> Unit,
    onAdjustReactorPower: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Holographic Jarvis Arc Reactor Eye (Main Attraction!)
        Box(
            modifier = Modifier
                .size(260.dp)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            JarvisEyeView(
                state = eyeState,
                audioWaveform = audioWaveform,
                modifier = Modifier.fillMaxSize(),
                onEyeClick = onEyeClick
            )
        }

        Text(
            text = "⚡ TAP EYE TO INITIATE VOICE DIAGNOSTIC ⚡",
            color = HoloCyanDim,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        // Telemetry Panels
        HudStatusPanels(
            telemetry = suitTelemetry,
            isVoiceMuted = isVoiceMuted,
            onToggleVoiceMute = onToggleVoiceMute,
            onExecuteProtocol = onExecuteProtocol,
            onAdjustReactorPower = onAdjustReactorPower
        )
    }
}

@Composable
private fun JarvisBottomNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = HoloSurfaceDark,
        tonalElevation = 8.dp,
        modifier = Modifier
            .navigationBarsPadding()
            .border(0.8.dp, HoloCardBorder)
            .testTag("jarvis_bottom_nav")
    ) {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = {
                Icon(
                    imageVector = Icons.Default.CenterFocusStrong,
                    contentDescription = "Core Eye"
                )
            },
            label = {
                Text(
                    text = "EYE / CORE",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = HoloBgDark,
                selectedTextColor = HoloCyanBright,
                indicatorColor = HoloCyan,
                unselectedIconColor = HoloTextMuted,
                unselectedTextColor = HoloTextMuted
            ),
            modifier = Modifier.testTag("tab_core_eye")
        )

        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Terminal Shell"
                )
            },
            label = {
                Text(
                    text = "SHELL",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = HoloBgDark,
                selectedTextColor = HoloCyanBright,
                indicatorColor = HoloCyan,
                unselectedIconColor = HoloTextMuted,
                unselectedTextColor = HoloTextMuted
            ),
            modifier = Modifier.testTag("tab_terminal_shell")
        )

        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Radar,
                    contentDescription = "Radar Scan"
                )
            },
            label = {
                Text(
                    text = "RADAR",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = HoloBgDark,
                selectedTextColor = HoloCyanBright,
                indicatorColor = HoloCyan,
                unselectedIconColor = HoloTextMuted,
                unselectedTextColor = HoloTextMuted
            ),
            modifier = Modifier.testTag("tab_radar_scan")
        )

        NavigationBarItem(
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            icon = {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Suit Protocols"
                )
            },
            label = {
                Text(
                    text = "PROTOCOLS",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = HoloBgDark,
                selectedTextColor = StarkGoldBright,
                indicatorColor = StarkGold,
                unselectedIconColor = HoloTextMuted,
                unselectedTextColor = HoloTextMuted
            ),
            modifier = Modifier.testTag("tab_suit_protocols")
        )
    }
}
