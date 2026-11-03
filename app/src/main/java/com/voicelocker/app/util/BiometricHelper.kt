package com.voicelocker.app.util

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Initiates biometric authentication prompt on the given FragmentActivity.
 */
fun biometricAuth(act: FragmentActivity?, title: String, onResult: (Boolean) -> Unit) {
    if (act == null) return onResult(false)
    if (BiometricManager.from(act).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) != BiometricManager.BIOMETRIC_SUCCESS) {
        return onResult(false)
    }
    val prompt = BiometricPrompt(
        act,
        ContextCompat.getMainExecutor(act),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onResult(true)
            override fun onAuthenticationError(code: Int, msg: CharSequence) = onResult(false)
        }
    )
    prompt.authenticate(
        BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
    )
}

