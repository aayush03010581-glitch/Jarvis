package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.api.GeminiClient
import com.example.data.terminal.LineType
import com.example.data.terminal.SuitTelemetry
import com.example.data.terminal.TerminalEngine
import com.example.data.terminal.TerminalLine
import com.example.data.voice.JarvisVoiceManager
import com.example.ui.components.EyeState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val voiceManager = JarvisVoiceManager(application)
    private val terminalEngine = TerminalEngine(application)

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
    private var stateResetJob: Job? = null

    init {
        // Initial boot lines
        _terminalLines.value = listOf(
            TerminalLine(
                type = LineType.HEADER,
                text = "╔══════════════════════════════════════════════════════════════╗"
            ),
            TerminalLine(
                type = LineType.HEADER,
                text = "║        STARK INDUSTRIES J.A.R.V.I.S. OS v4.8 [MARK LXXXV]    ║"
            ),
            TerminalLine(
                type = LineType.HEADER,
                text = "╚══════════════════════════════════════════════════════════════╝"
            ),
            TerminalLine(
                type = LineType.SUCCESS,
                tag = "BOOT",
                text = "Arc Reactor Core: SYNCHRONIZED | Confinement Field: 100%"
            ),
            TerminalLine(
                type = LineType.SUCCESS,
                tag = "NEURAL",
                text = "Cognitive Core online. Welcome back, Sir."
            ),
            TerminalLine(
                type = LineType.JARVIS,
                tag = "JARVIS",
                text = "Good day, Sir. All systems are operational. You may issue terminal commands or speak your directives."
            )
        )

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

    fun getCommandHistory(): List<String> = terminalEngine.getCommandHistory()

    fun executeTerminalCommand(input: String) {
        if (input.isBlank()) return

        viewModelScope.launch {
            _isExecuting.value = true
            _eyeState.value = EyeState.COMPUTING

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
                _terminalLines.value = listOf(
                    TerminalLine(type = LineType.SYSTEM, tag = "SYS", text = "Terminal buffer reset by user.")
                )
            } else {
                _terminalLines.value = _terminalLines.value + newLines
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
            
            _terminalLines.value = _terminalLines.value + TerminalLine(
                type = LineType.JARVIS,
                tag = "JARVIS",
                text = randomPhrase
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
        _terminalLines.value = listOf(
            TerminalLine(type = LineType.SYSTEM, tag = "SYS", text = "Terminal log cleared.")
        )
    }

    private suspend fun queryJarvisAi(prompt: String): String {
        val apiKey = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                // High thinking mode with gemini-3.1-pro-preview
                val request = GeminiClient.createJarvisRequest(prompt, chatHistory, useHighThinking = true)
                val response = GeminiClient.service.generateContentHighThinking(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) {
                    chatHistory.add(Pair("user", prompt))
                    chatHistory.add(Pair("model", text))
                    return text.trim()
                }
            } catch (e: Exception) {
                // Fallback to flash if pro preview rate-limited or error
                try {
                    val requestFlash = GeminiClient.createJarvisRequest(prompt, chatHistory, useHighThinking = false)
                    val responseFlash = GeminiClient.service.generateContentFlash(apiKey, requestFlash)
                    val textFlash = responseFlash.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    if (!textFlash.isNullOrBlank()) {
                        chatHistory.add(Pair("user", prompt))
                        chatHistory.add(Pair("model", textFlash))
                        return textFlash.trim()
                    }
                } catch (ex: Exception) {
                    // Fallback to intelligent in-character engine
                }
            }
        }

        // Sophisticated In-Character Intelligence Engine
        return generateOfflineJarvisResponse(prompt)
    }

    private fun generateOfflineJarvisResponse(prompt: String): String {
        val lower = prompt.lowercase(Locale.ROOT)
        return when {
            lower.contains("visa") || lower.contains("student visa") || lower.contains("study abroad") || lower.contains("f1") || lower.contains("i-20") ->
                "Analyzing International Student Visa Protocols, Sir:\n\n" +
                "1. Core Documents: Valid Passport (>6 mo. validity), Form I-20 / CAS / LOA from accredited institution, DS-160 confirmation, SEVIS I-901 fee receipt.\n" +
                "2. Financial Solvency: Liquid bank balances covering 1-2 years tuition + living expenses, affidavit of sponsorship, taxation records.\n" +
                "3. Consular Interview Strategy: Clearly articulate your academic curriculum, career trajectory in your home country, and strong non-immigrant ties.\n" +
                "4. Pre-Departure: Schedule biometric appointments, medical clearance, and secure health insurance. All Stark neural channels are standing by for your interview rehearsal, Sir."

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
        voiceManager.shutdown()
    }
}
