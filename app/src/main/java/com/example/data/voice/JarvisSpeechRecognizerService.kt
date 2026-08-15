package com.example.data.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class JarvisSpeechRecognizerService(
    private val context: Context,
    private val onCommandRecognized: (String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isHandsFreeActive = false
    private val handler = Handler(Looper.getMainLooper())

    private val _isListeningFlow = MutableStateFlow(false)
    val isListeningFlow: StateFlow<Boolean> = _isListeningFlow.asStateFlow()

    private val _handsFreeFlow = MutableStateFlow(false)
    val handsFreeFlow: StateFlow<Boolean> = _handsFreeFlow.asStateFlow()

    init {
        initRecognizer()
    }

    private fun initRecognizer() {
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            _isListeningFlow.value = true
                        }
                        override fun onBeginningOfSpeech() {}
                        override fun onRmsChanged(rmsdB: Float) {}
                        override fun onBufferReceived(buffer: ByteArray?) {}
                        override fun onEndOfSpeech() {
                            _isListeningFlow.value = false
                        }
                        override fun onError(error: Int) {
                            _isListeningFlow.value = false
                            isListening = false
                            if (isHandsFreeActive) {
                                handler.postDelayed({
                                    startListening()
                                }, 1200)
                            }
                        }
                        override fun onResults(results: Bundle?) {
                            _isListeningFlow.value = false
                            isListening = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                val spokenText = matches[0]
                                processSpokenCommand(spokenText)
                            }
                            if (isHandsFreeActive) {
                                handler.postDelayed({
                                    startListening()
                                }, 800)
                            }
                        }
                        override fun onPartialResults(partialResults: Bundle?) {}
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
            }
        } catch (e: Exception) {
            Log.e("JarvisSpeech", "Error initializing SpeechRecognizer: ${e.message}")
        }
    }

    fun startListening() {
        if (isListening) return
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }
            handler.post {
                try {
                    speechRecognizer?.startListening(intent)
                    isListening = true
                    _isListeningFlow.value = true
                } catch (e: Exception) {
                    isListening = false
                    _isListeningFlow.value = false
                }
            }
        } catch (e: Exception) {
            isListening = false
            _isListeningFlow.value = false
        }
    }

    fun stopListening() {
        try {
            handler.post {
                speechRecognizer?.stopListening()
            }
        } catch (e: Exception) {}
        isListening = false
        _isListeningFlow.value = false
    }

    fun toggleHandsFree(): Boolean {
        isHandsFreeActive = !isHandsFreeActive
        _handsFreeFlow.value = isHandsFreeActive
        if (isHandsFreeActive) {
            startListening()
        } else {
            stopListening()
        }
        return isHandsFreeActive
    }

    private fun processSpokenCommand(text: String) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        val lower = trimmed.lowercase()
        val cleanedCommand = when {
            lower.startsWith("jarvis ") -> trimmed.removePrefix("jarvis ").removePrefix("Jarvis ").trim()
            lower.startsWith("hey jarvis ") -> trimmed.removePrefix("hey jarvis ").removePrefix("Hey Jarvis ").trim()
            else -> trimmed
        }

        onCommandRecognized(cleanedCommand)
    }

    fun destroy() {
        stopListening()
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {}
        speechRecognizer = null
    }
}
