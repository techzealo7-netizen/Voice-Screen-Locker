package com.voicelocker.app.data.repository

import com.voicelocker.app.data.local.Store

interface LockRepository {
    var isOnboarded: Boolean
    var isVoiceEnabled: Boolean
    var isFingerprintEnabled: Boolean
    var isSystemLockOff: Boolean
    var currentLanguage: String
    var currentThemeIndex: Int
    var themeMode: Int
    val isLockEnabled: Boolean

    val hasVoice: Boolean
    fun setVoicePhrase(phrase: String)
    fun checkVoice(phrase: String): Boolean

    val hasPin: Boolean
    fun setPin(pin: String)
    fun checkPin(pin: String): Boolean

    val hasPattern: Boolean
    fun setPattern(pattern: List<Int>)
    fun checkPattern(pattern: List<Int>): Boolean

    val hasSecurity: Boolean
    val securityQuestion: String?
    fun setSecurity(question: String, answer: String)
    fun checkAnswer(answer: String): Boolean
}

class StoreLockRepository : LockRepository {
    override var isOnboarded: Boolean
        get() = Store.onboarded
        set(value) { Store.onboarded = value }

    override var isVoiceEnabled: Boolean
        get() = Store.voiceEnabled
        set(value) { Store.voiceEnabled = value }

    override var isFingerprintEnabled: Boolean
        get() = Store.fingerprintEnabled
        set(value) { Store.fingerprintEnabled = value }

    override var isSystemLockOff: Boolean
        get() = Store.systemLockOff
        set(value) { Store.systemLockOff = value }

    override var currentLanguage: String
        get() = Store.language
        set(value) { Store.language = value }

    override var currentThemeIndex: Int
        get() = Store.themeIndex
        set(value) { Store.themeIndex = value }

    override var themeMode: Int
        get() = Store.themeMode
        set(value) { Store.themeMode = value }

    override val isLockEnabled: Boolean
        get() = Store.lockEnabled

    override val hasVoice: Boolean
        get() = Store.hasVoice

    override fun setVoicePhrase(phrase: String) = Store.setVoicePhrase(phrase)
    override fun checkVoice(phrase: String): Boolean = Store.checkVoice(phrase)

    override val hasPin: Boolean
        get() = Store.hasPin

    override fun setPin(pin: String) = Store.setPin(pin)
    override fun checkPin(pin: String): Boolean = Store.checkPin(pin)

    override val hasPattern: Boolean
        get() = Store.hasPattern

    override fun setPattern(pattern: List<Int>) = Store.setPattern(pattern)
    override fun checkPattern(pattern: List<Int>): Boolean = Store.checkPattern(pattern)

    override val hasSecurity: Boolean
        get() = Store.hasSecurity

    override val securityQuestion: String?
        get() = Store.securityQuestion

    override fun setSecurity(question: String, answer: String) = Store.setSecurity(question, answer)
    override fun checkAnswer(answer: String): Boolean = Store.checkAnswer(answer)
}

