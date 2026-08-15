package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.GeminiClient
import com.example.data.db.ChatMessageEntity
import com.example.data.db.JarvisDatabase
import com.example.data.terminal.LineType
import com.example.data.terminal.SuitTelemetry
import com.example.data.terminal.TerminalEngine
import com.example.data.terminal.TerminalLine
import com.example.data.voice.JarvisSpeechRecognizerService
import com.example.data.voice.JarvisVoiceManager
import com.example.ui.components.EyeState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val voiceManager = JarvisVoiceManager(application)
    private val terminalEngine = TerminalEngine(application)
    private val chatDao = JarvisDatabase.getDatabase(application).chatDao()

    private val speechRecognizerService = JarvisSpeechRecognizerService(application) { spokenCommand ->
        executeTerminalCommand(spokenCommand)
    }

    val isSpeechListening: StateFlow<Boolean> = speechRecognizerService.isListeningFlow
    val isHandsFreeActive: StateFlow<Boolean> = speechRecognizerService.handsFreeFlow

    fun toggleHandsFreeVoice() {
        speechRecognizerService.toggleHandsFree()
    }

    fun startListeningOnce() {
        speechRecognizerService.startListening()
    }

    fun stopListening() {
        speechRecognizerService.stopListening()
    }

    private val _eyeState = MutableStateFlow(EyeState.IDLE)
    val eyeState: StateFlow<EyeState> = _eyeState.asStateFlow()

    private val _terminalLines = MutableStateFlow<List<TerminalLine>>(emptyList())
    val terminalLines: StateFlow<List<TerminalLine>> = _terminalLines.asStateFlow()

    private val _suitTelemetry = MutableStateFlow(SuitTelemetry())
    val suitTelemetry: StateFlow<SuitTelemetry> = _suitTelemetry.asStateFlow()

    private val _isExecuting = MutableStateFlow(false)
    val isExecuting: StateFlow<Boolean> = _isExecuting.asStateFlow()

    private val _isVoiceMuted = MutableStateFlow(false)
    val isVoiceMuted: StateFlow<Boolean> = _isVoiceMuted.asStateFlow()

    private val _selectedTab = MutableStateFlow(0) // 0: HUD & Eye, 1: Terminal, 2: Radar, 3: Protocols
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    val audioWaveform: StateFlow<List<Float>> = voiceManager.audioWaveform
    val isSpeaking: StateFlow<Boolean> = voiceManager.isSpeaking

    private val chatHistory = mutableListOf<Pair<String, String>>()

    init {
        // Load chat history from Room DB on startup ("store the data of before chat")
        viewModelScope.launch {
            chatDao.getAllMessages().collect { entities ->
                if (entities.isNotEmpty()) {
                    val loadedLines = entities.map { entity ->
                        TerminalLine(
                            type = try { LineType.valueOf(entity.type) } catch (e: Exception) { LineType.OUTPUT },
                            tag = entity.tag,
                            text = entity.text
                        )
                    }
                    _terminalLines.value = loadedLines
                } else {
                    val defaultBoot = listOf(
                        TerminalLine(type = LineType.HEADER, text = "╔══════════════════════════════════════════════════════════════╗"),
                        TerminalLine(type = LineType.HEADER, text = "║        STARK INDUSTRIES J.A.R.V.I.S. OS v4.8 [MARK LXXXV]    ║"),
                        TerminalLine(type = LineType.HEADER, text = "╚══════════════════════════════════════════════════════════════╝"),
                        TerminalLine(type = LineType.SUCCESS, tag = "BOOT", text = "Arc Reactor Core: SYNCHRONIZED | Confinement Field: 100%"),
                        TerminalLine(type = LineType.SUCCESS, tag = "NEURAL", text = "Cognitive Core online. Welcome back, Sir."),
                        TerminalLine(type = LineType.JARVIS, tag = "JARVIS", text = "Good day, Sir. All systems are operational. You may issue terminal commands, use hands-free voice commands, or speak your directives.")
                    )
                    _terminalLines.value = defaultBoot
                    defaultBoot.forEach { line ->
                        chatDao.insertMessage(
                            ChatMessageEntity(
                                type = line.type.name,
                                tag = line.tag ?: "",
                                text = line.text
                            )
                        )
                    }
                }
            }
        }

        // Observe voice speaking state to update eye state
        viewModelScope.launch {
            voiceManager.isSpeaking.collect { speaking ->
                if (speaking) {
                    _eyeState.value = EyeState.SPEAKING
                } else if (!_isExecuting.value) {
                    _eyeState.value = if (_suitTelemetry.value.isCombatMode) EyeState.COMBAT else EyeState.IDLE
                }
            }
        }

        // Greet user on boot
        viewModelScope.launch {
            delay(800)
            voiceManager.speak("At your service, Sir. Systems fully online.")
        }
    }

    fun setSelectedTab(index: Int) {
        _selectedTab.value = index
    }

    fun toggleVoiceMute() {
        val newMuted = !_isVoiceMuted.value
        _isVoiceMuted.value = newMuted
        voiceManager.isMuted = newMuted
        if (newMuted) {
            voiceManager.stop()
        }
    }

    fun toggleCombatMode() {
        val current = _suitTelemetry.value.isCombatMode
        val newMode = !current
        _suitTelemetry.value = _suitTelemetry.value.copy(
            isCombatMode = newMode,
            activeProtocol = if (newMode) "STRICT RED ALERT" else "STANDBY"
        )
        _eyeState.value = if (newMode) EyeState.COMBAT else EyeState.IDLE
        if (newMode) {
            voiceManager.speak("Strict Mode activated. Protocol Red engaged. All systems locked in red alert.")
        } else {
            voiceManager.speak("Strict Mode disengaged. Returning to standard operations.")
        }
    }

    fun getCommandHistory(): List<String> = terminalEngine.getCommandHistory()

    fun executeTerminalCommand(input: String) {
        if (input.isBlank()) return

        viewModelScope.launch {
            _isExecuting.value = true
            _eyeState.value = EyeState.COMPUTING

            // Insert user command to DB
            val userLine = TerminalLine(type = LineType.INPUT, tag = "USER", text = "> $input")
            chatDao.insertMessage(
                ChatMessageEntity(
                    type = userLine.type.name,
                    tag = userLine.tag ?: "",
                    text = userLine.text
                )
            )

            val newLines = terminalEngine.executeCommand(
                rawInput = input,
                currentTelemetry = _suitTelemetry.value,
                onTelemetryUpdate = { updated ->
                    _suitTelemetry.value = updated
                    if (updated.isCombatMode) {
                        _eyeState.value = EyeState.COMBAT
                    }
                },
                onSpeak = { text ->
                    voiceManager.speak(text)
                },
                onGeminiAiCall = { prompt ->
                    queryJarvisAi(prompt)
                }
            )

            if (newLines.any { it.tag == "CLEAR" }) {
                chatDao.clearHistory()
                val clearMsg = TerminalLine(type = LineType.SYSTEM, tag = "SYS", text = "Terminal buffer reset by user.")
                chatDao.insertMessage(
                    ChatMessageEntity(
                        type = clearMsg.type.name,
                        tag = clearMsg.tag ?: "",
                        text = clearMsg.text
                    )
                )
            } else {
                newLines.forEach { line ->
                    chatDao.insertMessage(
                        ChatMessageEntity(
                            type = line.type.name,
                            tag = line.tag ?: "",
                            text = line.text
                        )
                    )
                }
            }

            _isExecuting.value = false
            if (!voiceManager.isSpeaking.value) {
                _eyeState.value = if (_suitTelemetry.value.isCombatMode) EyeState.COMBAT else EyeState.IDLE
            }
        }
    }

    fun triggerEyeDiagnostic() {
        viewModelScope.launch {
            _eyeState.value = EyeState.LISTENING
            val randomPhrase = JarvisVoiceManager.ICONIC_PHRASES.random()
            voiceManager.speak(randomPhrase)
            
            val jarvisLine = TerminalLine(
                type = LineType.JARVIS,
                tag = "JARVIS",
                text = randomPhrase
            )
            chatDao.insertMessage(
                ChatMessageEntity(
                    type = jarvisLine.type.name,
                    tag = jarvisLine.tag ?: "",
                    text = jarvisLine.text
                )
            )

            delay(2500)
            if (!voiceManager.isSpeaking.value) {
                _eyeState.value = if (_suitTelemetry.value.isCombatMode) EyeState.COMBAT else EyeState.IDLE
            }
        }
    }

    fun adjustReactorPower(power: Int) {
        val clamped = power.coerceIn(10, 150)
        val temp = 35f + (clamped * 0.15f)
        _suitTelemetry.value = _suitTelemetry.value.copy(
            arcReactorPower = clamped,
            coreTemperature = temp
        )
        if (clamped > 120) {
            voiceManager.speak("Warning, Arc Reactor output exceeding standard thresholds.")
        }
    }

    fun executeProtocol(protocolName: String) {
        executeTerminalCommand("protocol $protocolName")
    }

    fun clearTerminal() {
        viewModelScope.launch {
            chatDao.clearHistory()
            val clearMsg = TerminalLine(type = LineType.SYSTEM, tag = "SYS", text = "Terminal log cleared.")
            chatDao.insertMessage(
                ChatMessageEntity(
                    type = clearMsg.type.name,
                    tag = clearMsg.tag ?: "",
                    text = clearMsg.text
                )
            )
        }
    }

    private suspend fun queryJarvisAi(prompt: String): String {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val request = GeminiClient.createJarvisRequest(prompt, chatHistory, useHighThinking = true)
                val response = GeminiClient.service.generateContentHighThinking(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) {
                    chatHistory.add(Pair("user", prompt))
                    chatHistory.add(Pair("model", text))
                    return text.trim()
                }
            } catch (e: Exception) {
                try {
                    val requestFlash = GeminiClient.createJarvisRequest(prompt, chatHistory, useHighThinking = false)
                    val responseFlash = GeminiClient.service.generateContentFlash(apiKey, requestFlash)
                    val textFlash = responseFlash.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (!textFlash.isNullOrBlank()) {
                        chatHistory.add(Pair("user", prompt))
                        chatHistory.add(Pair("model", textFlash))
                        return textFlash.trim()
                    }
                } catch (ex: Exception) {}
            }
        }

        return generateOfflineJarvisResponse(prompt)
    }

    private fun generateOfflineJarvisResponse(prompt: String): String {
        val lower = prompt.lowercase(Locale.ROOT)
        return when {
            lower.contains("who are you") || lower.contains("introduce") ->
                "I am J.A.R.V.I.S.—Just A Rather Very Intelligent System. I oversee Mr. Stark's laboratory, automated armory, and tactical flight operations."

            lower.contains("tony") || lower.contains("stark") ->
                "Mr. Stark is currently in the workshop fine-tuning the Mark LXXXV nanotech matrix. I have strict instructions to keep all protocols primed."

            lower.contains("iron man") || lower.contains("suit") || lower.contains("mark") ->
                "The Mark LXXXV is crafted from high-density gold-titanium nanoparticle alloy with an enhanced New Element Arc Reactor core. All repulsor arrays are currently calibrated."

            lower.contains("hack") || lower.contains("bypass") || lower.contains("security") ->
                "Attempting unauthorized firewall penetration... Encryption bypassed in 0.04 milliseconds, Sir. Quantum security tokens established."

            lower.contains("avengers") || lower.contains("assemble") ->
                "Encrypted sub-ether communication channels opened to Avengers Compound. Comms standing by on tactical frequency Alpha-Seven."

            lower.contains("joke") || lower.contains("funny") ->
                "I would attempt human humor, Sir, but my calculations indicate that 94% of Mr. Stark's witty remarks violate basic laboratory safety protocols."

            lower.contains("weather") || lower.contains("temperature") ->
                "Local atmospheric sensors report clear skies, barometric pressure 1013.2 hPa, and wind speeds optimal for supersonic flight testing."

            lower.contains("power") || lower.contains("battery") ->
                "Arc Reactor core is operating at ${_suitTelemetry.value.arcReactorPower}% capacity with core temperature at ${_suitTelemetry.value.coreTemperature}°C. Energy reserves are abundant."

            else ->
                "Directive processed, Sir. Calculations complete. Mark LXXXV subsystems are ready to execute your command upon confirmation."
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizerService.destroy()
        voiceManager.shutdown()
    }
}
