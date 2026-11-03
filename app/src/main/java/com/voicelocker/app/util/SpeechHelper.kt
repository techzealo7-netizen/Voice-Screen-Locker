package com.voicelocker.app.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Thin wrapper around Android's SpeechRecognizer (must be used on the main thread).
 */
class Speech(private val ctx: Context) {
    private var rec: SpeechRecognizer? = null
    var listening by mutableStateOf(false)
        private set
    private val main = Handler(Looper.getMainLooper())

    /** onError codes: 6 = no speech, 7 = no match, -1 = recognition unavailable. */
    fun start(
        tag: String,
        onPartial: (String) -> Unit = {},
        onFinal: (String) -> Unit,
        onError: (Int) -> Unit = {}
    ) {
        main.post {
            cancel()
            if (!SpeechRecognizer.isRecognitionAvailable(ctx)) {
                onError(-1)
                return@post
            }
            val r = SpeechRecognizer.createSpeechRecognizer(ctx)
            rec = r
            r.setRecognitionListener(object : RecognitionListener {
                private fun first(b: Bundle?) =
                    b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""

                override fun onResults(b: Bundle?) {
                    listening = false
                    onFinal(first(b))
                }

                override fun onPartialResults(b: Bundle?) {
                    onPartial(first(b))
                }

                override fun onError(e: Int) {
                    listening = false
                    onError(e)
                }

                override fun onReadyForSpeech(p: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(v: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(t: Int, b: Bundle?) {}
            })
            listening = true
            r.startListening(
                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    .putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag)
                    .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            )
        }
    }

    /** Ask the recognizer to wrap up; the final result is still delivered to onFinal. */
    fun finish() {
        rec?.stopListening()
    }

    fun cancel() {
        rec?.let {
            it.cancel()
            it.destroy()
        }
        rec = null
        listening = false
    }
}

@Composable
fun rememberSpeech(): Speech {
    val ctx = LocalContext.current
    val s = remember { Speech(ctx) }
    DisposableEffect(Unit) {
        onDispose { s.cancel() }
    }
    return s
}

