package com.voicelocker.app.util

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

private const val TAG = "BiometricHelper"

@Volatile
private var isAuthenticating = false

/**
 * Initiates biometric authentication prompt on the given FragmentActivity.
 */
fun biometricAuth(act: FragmentActivity?, title: String, onResult: (Boolean) -> Unit) {
    if (act == null || act.isFinishing || act.isDestroyed) {
        onResult(false)
        return
    }

    val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK
    val bm = BiometricManager.from(act)
    val canAuth = bm.canAuthenticate(authenticators)
    Log.d(TAG, "canAuthenticate result: $canAuth")

    when (canAuth) {
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
            act.toast("No fingerprint enrolled. Please add one in device Settings.")
            onResult(false)
            return
        }
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
            act.toast("No fingerprint sensor detected on this device.")
            onResult(false)
            return
        }
        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
            act.toast("Security update required to use fingerprint.")
            onResult(false)
            return
        }
    }

    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            super.onAuthenticationSucceeded(result)
            isAuthenticating = false
            onResult(true)
        }

        override fun onAuthenticationError(code: Int, msg: CharSequence) {
            super.onAuthenticationError(code, msg)
            isAuthenticating = false
            when (code) {
                BiometricPrompt.ERROR_USER_CANCELED,
                BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                BiometricPrompt.ERROR_CANCELED -> {
                    // User cancelled or dialog dismissed
                    onResult(false)
                }
                BiometricPrompt.ERROR_LOCKOUT,
                BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                    val text = if (msg.isNotBlank()) msg.toString() else "Too many attempts. Please try again in 30 seconds."
                    act.toast(text)
                    onResult(false)
                }
                BiometricPrompt.ERROR_NO_BIOMETRICS -> {
                    act.toast("No fingerprint enrolled. Please set up fingerprint in Settings.")
                    onResult(false)
                }
                BiometricPrompt.ERROR_HW_UNAVAILABLE -> {
                    act.toast("Fingerprint sensor is busy. Please try again.")
                    onResult(false)
                }
                else -> {
                    if (msg.isNotBlank()) {
                        act.toast(msg.toString())
                    }
                    onResult(false)
                }
            }
        }

        override fun onAuthenticationFailed() {
            super.onAuthenticationFailed()
            // Biometric presented but not recognized; system dialog prompts user to try again
        }
    }

    fun startPrompt() {
        if (act.isFinishing || act.isDestroyed) {
            isAuthenticating = false
            onResult(false)
            return
        }
        try {
            val promptInfo = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(authenticators)
                .build()

            val prompt = BiometricPrompt(
                act,
                ContextCompat.getMainExecutor(act),
                callback
            )
            isAuthenticating = true
            prompt.authenticate(promptInfo)
        } catch (e: Exception) {
            Log.e(TAG, "Biometric authenticate threw exception, scheduling retry", e)
            Handler(Looper.getMainLooper()).postDelayed({
                if (act.isFinishing || act.isDestroyed) {
                    isAuthenticating = false
                    onResult(false)
                    return@postDelayed
                }
                try {
                    val promptInfo = BiometricPrompt.PromptInfo.Builder()
                        .setTitle(title)
                        .setNegativeButtonText("Cancel")
                        .setAllowedAuthenticators(authenticators)
                        .build()
                    val prompt = BiometricPrompt(
                        act,
                        ContextCompat.getMainExecutor(act),
                        callback
                    )
                    isAuthenticating = true
                    prompt.authenticate(promptInfo)
                } catch (e2: Exception) {
                    Log.e(TAG, "Biometric authenticate retry failed", e2)
                    isAuthenticating = false
                    act.toast("Fingerprint sensor busy. Please tap Try Again.")
                    onResult(false)
                }
            }, 300)
        }
    }

    if (Looper.myLooper() == Looper.getMainLooper()) {
        startPrompt()
    } else {
        Handler(Looper.getMainLooper()).post { startPrompt() }
    }
}
