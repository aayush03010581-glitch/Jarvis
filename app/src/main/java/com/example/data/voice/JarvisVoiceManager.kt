package com.example.data.voice

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

class JarvisVoiceManager(context: Context) {
    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private val scope = CoroutineScope(Dispatchers.Main)
    private var amplitudeJob: Job? = null

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _audioWaveform = MutableStateFlow(List(16) { 0.1f })
    val audioWaveform: StateFlow<List<Float>> = _audioWaveform.asStateFlow()

    var isMuted = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                // Attempt UK English first for authentic Jarvis tone, otherwise US or default
                val ukLocale = Locale.UK
                if (tts?.isLanguageAvailable(ukLocale) == TextToSpeech.LANG_AVAILABLE) {
                    tts?.language = ukLocale
                } else {
                    tts?.language = Locale.getDefault()
                }
                // Slightly deeper pitch and steady sophisticated rate
                tts?.setPitch(0.92f)
                tts?.setSpeechRate(0.98f)

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                        startWaveformSimulation()
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        stopWaveformSimulation()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        stopWaveformSimulation()
                    }
                })
            }
        }
    }

    fun speak(text: String, utteranceId: String = "jarvis_msg_${System.currentTimeMillis()}") {
        if (isMuted || !isTtsInitialized || text.isBlank()) return
        
        // Clean out terminal markdown or bracket codes before speaking
        val cleanText = text
            .replace(Regex("\\[[A-Z0-9_-]+\\]"), "")
            .replace(Regex("[-*#`_>|]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        if (cleanText.isBlank()) return

        val params = Bundle()
        tts?.speak(cleanText, TextToSpeech.QUEUE_ADD, params, utteranceId)
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
        stopWaveformSimulation()
    }

    private fun startWaveformSimulation() {
        amplitudeJob?.cancel()
        amplitudeJob = scope.launch {
            while (_isSpeaking.value) {
                // Generate dynamic realistic audio frequency bands
                val newBars = List(16) { index ->
                    val centerFactor = 1f - (Math.abs(index - 7.5f) / 8f) * 0.4f
                    val randomFluctuation = Random.nextFloat() * 0.8f + 0.2f
                    (centerFactor * randomFluctuation).coerceIn(0.1f, 1.0f)
                }
                _audioWaveform.value = newBars
                delay(60)
            }
            _audioWaveform.value = List(16) { 0.1f }
        }
    }

    private fun stopWaveformSimulation() {
        amplitudeJob?.cancel()
        _audioWaveform.value = List(16) { 0.08f }
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    companion object {
        val ICONIC_PHRASES = listOf(
            "At your service, Sir.",
            "All systems operating at peak efficiency.",
            "Arc Reactor output holding steady at one hundred percent.",
            "Mark eighty-five nanotech subroutines calibrated and ready.",
            "Veronica orbital defense satellite standing by.",
            "Running full diagnostic telemetry on all repulsor arrays.",
            "Security firewall established. Awaiting your directive, Sir.",
            "Energy reserves optimal. Flight stabilizers engaged."
        )
    }
}
