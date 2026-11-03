package com.voicelocker.app

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

fun Context.toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

fun Context.findActivity(): FragmentActivity? {
    var c = this
    while (c is ContextWrapper) { if (c is FragmentActivity) return c; c = c.baseContext }
    return null
}

fun fmtTime(s: Int) = "%02d:%02d".format(s / 60, s % 60)

/** Thin wrapper around Android's SpeechRecognizer (must be used on the main thread). */
class Speech(private val ctx: Context) {
    private var rec: SpeechRecognizer? = null
    var listening by mutableStateOf(false)
        private set
    private val main = Handler(Looper.getMainLooper())

    /** onError codes: 6 = no speech, 7 = no match, -1 = recognition unavailable. */
    fun start(tag: String, onPartial: (String) -> Unit = {}, onFinal: (String) -> Unit, onError: (Int) -> Unit = {}) {
        main.post {
            cancel()
            if (!SpeechRecognizer.isRecognitionAvailable(ctx)) { onError(-1); return@post }
            val r = SpeechRecognizer.createSpeechRecognizer(ctx)
            rec = r
            r.setRecognitionListener(object : RecognitionListener {
                private fun first(b: Bundle?) = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
                override fun onResults(b: Bundle?) { listening = false; onFinal(first(b)) }
                override fun onPartialResults(b: Bundle?) { onPartial(first(b)) }
                override fun onError(e: Int) { listening = false; onError(e) }
                override fun onReadyForSpeech(p: Bundle?) {}
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(v: Float) {}
                override fun onBufferReceived(b: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onEvent(t: Int, b: Bundle?) {}
            })
            listening = true
            r.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                .putExtra(RecognizerIntent.EXTRA_LANGUAGE, tag)
                .putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true))
        }
    }

    /** Ask the recognizer to wrap up; the final result is still delivered to onFinal. */
    fun finish() { rec?.stopListening() }

    fun cancel() { rec?.let { it.cancel(); it.destroy() }; rec = null; listening = false }
}

@Composable
fun rememberSpeech(): Speech {
    val ctx = LocalContext.current
    val s = remember { Speech(ctx) }
    DisposableEffect(Unit) { onDispose { s.cancel() } }
    return s
}

fun biometricAuth(act: FragmentActivity?, title: String, onResult: (Boolean) -> Unit) {
    if (act == null) return onResult(false)
    if (BiometricManager.from(act).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_SUCCESS) return onResult(false)
    val prompt = BiometricPrompt(act, ContextCompat.getMainExecutor(act), object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onResult(true)
        override fun onAuthenticationError(code: Int, msg: CharSequence) = onResult(false)
    })
    prompt.authenticate(BiometricPrompt.PromptInfo.Builder().setTitle(title).setNegativeButtonText("Cancel")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG).build())
}
