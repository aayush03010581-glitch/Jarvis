package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.terminal.LineType
import com.example.data.terminal.TerminalLine
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
import com.example.ui.theme.HoloTextTerminal
import com.example.ui.theme.StarkAmber
import com.example.ui.theme.StarkGold
import com.example.ui.theme.StarkGoldBright
import com.example.ui.theme.TechGreen

@Composable
fun TerminalConsoleView(
    lines: List<TerminalLine>,
    onExecuteCommand: (String) -> Unit,
    onClearTerminal: () -> Unit,
    commandHistory: List<String>,
    isExecuting: Boolean = false,
    modifier: Modifier = Modifier,
    onVoiceToggle: () -> Unit = {}
) {
    var inputText by remember { mutableStateOf("") }
    var historyIndex by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()

    // Auto-scroll to bottom when new terminal output arrives
    LaunchedEffect(lines.size) {
        if (lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }

    val quickCommands = listOf(
        "sysinfo",
        "reactor",
        "scan",
        "visa checklist",
        "ping 8.8.8.8",
        "protocol veronica",
        "protocol house_party",
        "armor",
        "diagnostics",
        "help"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        HoloSurfaceDark,
                        HoloBgDark
                    )
                )
            )
            .border(1.dp, HoloCardBorder, RoundedCornerShape(12.dp))
            .testTag("terminal_console_container")
    ) {
        // Terminal Window Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HoloSurfaceVariant)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(AlertRed)
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(StarkGold)
                )
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(TechGreen)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = HoloCyan,
                    modifier = Modifier.size(16.dp)
                )

                Text(
                    text = "STARK-OS // BASH v5.2 // JARVIS-CORTEX",
                    color = HoloTextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isExecuting) {
                    Text(
                        text = "● COMPUTING",
                        color = StarkGold,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                IconButton(
                    onClick = onClearTerminal,
                    modifier = Modifier
                        .size(28.dp)
                        .testTag("clear_terminal_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear Terminal",
                        tint = HoloTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Terminal Log Screen (Scrollable)
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("terminal_lines_list"),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            items(lines, key = { it.id }) { line ->
                TerminalLineItem(line = line)
            }
        }

        // Quick Command Action Chips Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            quickCommands.forEach { cmd ->
                AssistChip(
                    onClick = {
                        onExecuteCommand(cmd)
                    },
                    label = {
                        Text(
                            text = cmd,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = HoloCyanBright
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = HoloSurfaceVariant.copy(alpha = 0.7f),
                        labelColor = HoloCyanBright
                    ),
                    border = AssistChipDefaults.assistChipBorder(
                        borderColor = HoloCardBorder,
                        borderWidth = 1.dp,
                        enabled = true
                    ),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.testTag("quick_cmd_$cmd")
                )
            }
        }

        // Terminal Input Prompt
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(HoloSurfaceDark)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Command History Navigation Buttons
            IconButton(
                onClick = {
                    if (commandHistory.isNotEmpty()) {
                        val newIdx = if (historyIndex == -1) {
                            commandHistory.size - 1
                        } else {
                            (historyIndex - 1).coerceAtLeast(0)
                        }
                        historyIndex = newIdx
                        inputText = commandHistory[newIdx]
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowUpward,
                    contentDescription = "History Previous",
                    tint = HoloCyanDim,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = {
                    if (commandHistory.isNotEmpty() && historyIndex != -1) {
                        val newIdx = historyIndex + 1
                        if (newIdx < commandHistory.size) {
                            historyIndex = newIdx
                            inputText = commandHistory[newIdx]
                        } else {
                            historyIndex = -1
                            inputText = ""
                        }
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowDownward,
                    contentDescription = "History Next",
                    tint = HoloCyanDim,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Text Input Field
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .testTag("terminal_input_field"),
                placeholder = {
                    Text(
                        text = "Enter terminal command or query JARVIS...",
                        color = HoloTextMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                },
                leadingIcon = {
                    Text(
                        text = "stark@hud:~$",
                        color = HoloCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = HoloTextPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = HoloCardBorderGlow,
                    unfocusedBorderColor = HoloCardBorder,
                    focusedContainerColor = HoloBgDark,
                    unfocusedContainerColor = HoloBgDark,
                    cursorColor = HoloCyan
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (inputText.isNotBlank()) {
                            onExecuteCommand(inputText)
                            inputText = ""
                            historyIndex = -1
                        }
                    }
                ),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.width(6.dp))

            // Execute Button
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        onExecuteCommand(inputText)
                        inputText = ""
                        historyIndex = -1
                    }
                },
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(HoloCyan)
                    .testTag("execute_command_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Execute Command",
                    tint = HoloBgDark,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun TerminalLineItem(line: TerminalLine) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        when (line.type) {
            LineType.INPUT -> {
                Text(
                    text = line.text,
                    color = StarkGoldBright,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.SemiBold
                )
            }

            LineType.SYSTEM -> {
                TagBadge(tag = line.tag.ifBlank { "SYS" }, color = HoloCyanDim)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = line.text,
                    color = HoloTextSecondary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            LineType.SUCCESS -> {
                TagBadge(tag = line.tag.ifBlank { "OK" }, color = TechGreen)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = line.text,
                    color = HoloTextTerminal,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            LineType.WARNING -> {
                TagBadge(tag = line.tag.ifBlank { "WARN" }, color = StarkAmber)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = line.text,
                    color = StarkGold,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            LineType.ERROR -> {
                TagBadge(tag = line.tag.ifBlank { "ERR" }, color = AlertRed)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = line.text,
                    color = AlertRed,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            LineType.JARVIS -> {
                TagBadge(tag = "J.A.R.V.I.S.", color = HoloCyan)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = line.text,
                    color = HoloTextPrimary,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp
                )
            }

            LineType.STARK_DIRECTIVE -> {
                TagBadge(tag = line.tag.ifBlank { "STARK" }, color = StarkGold)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = line.text,
                    color = HoloTextPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            LineType.PROGRESS -> {
                TagBadge(tag = line.tag.ifBlank { "RUN" }, color = HoloCyanBright)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = line.text,
                    color = HoloCyanBright,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            LineType.HEADER -> {
                Text(
                    text = line.text,
                    color = HoloCyan,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            LineType.OUTPUT -> {
                Text(
                    text = line.text,
                    color = HoloTextPrimary,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun TagBadge(tag: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.2f))
            .border(0.8.dp, color.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(
            text = tag,
            color = color,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
