package com.example.sketchnote.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class VoiceState { IDLE, LISTENING, ERROR }

class VoiceSearchManager(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState: StateFlow<VoiceState> = _voiceState

    private val _result = MutableStateFlow("")
    val result: StateFlow<String> = _result

    fun startListening(onResult: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            _voiceState.value = VoiceState.ERROR
            return
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p: Bundle?) {
                    _voiceState.value = VoiceState.LISTENING
                }
                override fun onResults(results: Bundle?) {
                    val matches = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull() ?: ""
                    _result.value = text
                    _voiceState.value = VoiceState.IDLE
                    onResult(text)
                }
                override fun onError(error: Int) {
                    _voiceState.value = VoiceState.ERROR
                }
                override fun onBeginningOfSpeech() {}
                override fun onBufferReceived(p: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(p: Int, p1: Bundle?) {}
                override fun onPartialResults(p: Bundle?) {}
                override fun onRmsChanged(p: Float) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "vi-VN")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        speechRecognizer?.startListening(intent)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        _voiceState.value = VoiceState.IDLE
    }
}