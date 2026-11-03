package com.voicelocker.app.util

import java.security.MessageDigest

/**
 * Normalizes input text by removing punctuation, converting to lowercase, and trimming whitespace.
 */
fun normalize(s: String): String =
    s.lowercase()
        .replace(Regex("[^\\p{L}\\p{N}\\s]"), "")
        .replace(Regex("\\s+"), " ")
        .trim()

/**
 * Computes a salted SHA-256 hash for secure credential storage.
 */
fun hashSecret(s: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest("vsl::$s".toByteArray())
        .joinToString("") { "%02x".format(it) }

