package com.voicelocker.app.domain.service

/**
 * Hook for AI features.
 * summarize() is a simple local fallback; translate() needs a real backend (never ship API keys in the app).
 */
object AiService {
    suspend fun summarize(text: String): String {
        val s = text.split(Regex("(?<=[.!?])\\s+")).filter { it.isNotBlank() }
        return if (s.size <= 2) text else s.take(2).joinToString(" ")
    }

    suspend fun translate(text: String, to: String): String =
        throw UnsupportedOperationException("Connect a translation backend in AiService.translate()")
}

